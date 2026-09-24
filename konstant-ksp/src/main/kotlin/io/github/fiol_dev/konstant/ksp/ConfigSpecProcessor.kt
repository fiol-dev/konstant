package io.github.fiol_dev.konstant.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate
import io.github.fiol_dev.konstant.annotations.ConfigSpec
import io.github.fiol_dev.konstant.annotations.Convert
import io.github.fiol_dev.konstant.annotations.Key
import io.github.fiol_dev.konstant.annotations.NotBlank
import io.github.fiol_dev.konstant.annotations.Pattern
import io.github.fiol_dev.konstant.annotations.Range
import io.github.fiol_dev.konstant.annotations.Secret
import io.github.fiol_dev.konstant.annotations.Size

@OptIn(KspExperimental::class)
class ConfigSpecProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    companion object {
        private const val CONVERTERS = "io.github.fiol_dev.konstant.core.Converters"
        private const val VALIDATORS = "io.github.fiol_dev.konstant.core.Validators"
        private const val VALUE_CONVERTER = "io.github.fiol_dev.konstant.core.ValueConverter"

        /** Sentinel from [validationExpression] meaning an error was already logged. */
        private const val INVALID = "<invalid>"

        private const val SUPPORTED_TYPES_HINT =
            "Supported types: String, Int, Long, Double, Float, Boolean, Duration, enums, " +
                "List/Set of those, Map<String, T> of those, nullable versions of all of them, " +
                "and @ConfigSpec data classes."
    }

    /** What validation annotations a value type accepts. */
    private enum class Shape { NUMBER, STRING, COLLECTION, OTHER }

    /** How a single value is written in generated code. */
    private data class ValueType(
        /** Fully qualified Kotlin type, safe to use without imports. */
        val code: String,
        /** Short form shown in error messages, e.g. `List<Int>`. */
        val display: String,
        /** Expression of type `(String) -> T`. */
        val converter: String,
        /** Expression of type `(Map<String, String>) -> T` for values read from nested entries. */
        val childrenConverter: String? = null,
        val shape: Shape = Shape.OTHER,
    )

    private sealed interface FieldKind {
        data class Value(val type: ValueType, val nullable: Boolean, val validate: String? = null) : FieldKind
        data class Nested(val decl: KSClassDeclaration) : FieldKind
    }

    private data class Field(
        val param: KSValueParameter,
        val name: String,
        val kind: FieldKind,
        val property: KSPropertyDeclaration?,
    )

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(ConfigSpec::class.qualifiedName!!)
        val deferred = mutableListOf<KSAnnotated>()

        for (symbol in symbols) {
            if (!symbol.validate()) {
                deferred += symbol
                continue
            }
            if (symbol !is KSClassDeclaration) continue
            val fields = collectFields(symbol) ?: continue
            generateCode(symbol, fields)
        }
        return deferred
    }

    /** Returns the class's fields, or null after logging errors if the class can't be generated. */
    private fun collectFields(decl: KSClassDeclaration): List<Field>? {
        if (decl.classKind != ClassKind.CLASS || Modifier.DATA !in decl.modifiers) {
            logger.error("@ConfigSpec can only be applied to data classes", decl)
            return null
        }
        val params = decl.primaryConstructor?.parameters ?: return null
        val properties = decl.getAllProperties().associateBy { it.simpleName.asString() }

        var valid = true
        val fields = mutableListOf<Field>()
        for (param in params) {
            val name = param.name?.asString() ?: continue
            val type = param.type.resolve()
            val property = properties[name]
            val convertWith = property?.let { findAnnotation(it, Convert::class.qualifiedName!!) }
            var kind = if (convertWith != null) {
                customConverterKind(name, type, convertWith, param)
            } else {
                fieldKind(type)
            }
            if (convertWith != null && kind == null) {
                // customConverterKind already logged why
                valid = false
                continue
            }
            if (kind is FieldKind.Value && property != null) {
                val validate = validationExpression(name, kind, property)
                if (validate == INVALID) {
                    valid = false
                    continue
                }
                kind = kind.copy(validate = validate)
            }
            if (kind == null) {
                val shown = type.declaration.qualifiedName?.asString() ?: type.toString()
                val nullableNested = type.isMarkedNullable && isConfigSpec(type)
                val reason = if (nullableNested) {
                    "Nullable @ConfigSpec field '$name' is not supported; give it a default instead."
                } else {
                    "Unsupported field type '$shown${if (type.isMarkedNullable) "?" else ""}' " +
                        "for property '$name'. $SUPPORTED_TYPES_HINT"
                }
                logger.error(reason, param)
                valid = false
                continue
            }
            fields += Field(param, name, kind, property)
        }
        return if (valid) fields else null
    }

    private fun findAnnotation(prop: KSPropertyDeclaration, qualifiedName: String): KSAnnotation? =
        prop.annotations.firstOrNull {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == qualifiedName
        }

    private fun KSAnnotation.argument(name: String): Any? =
        arguments.firstOrNull { it.name?.asString() == name }?.value
            ?: defaultArguments.firstOrNull { it.name?.asString() == name }?.value

    /** `@Convert(with = X::class)`: X must be an object implementing ValueConverter of the field's type. */
    private fun customConverterKind(
        name: String,
        type: KSType,
        annotation: KSAnnotation,
        param: KSValueParameter,
    ): FieldKind? {
        val converterType = annotation.argument("with") as? KSType
        val converter = converterType?.declaration as? KSClassDeclaration
        if (converter == null || converter.classKind != ClassKind.OBJECT) {
            logger.error("@Convert on '$name' must name an object implementing ValueConverter", param)
            return null
        }
        val target = converter.getAllSuperTypes()
            .firstOrNull { it.declaration.qualifiedName?.asString() == VALUE_CONVERTER }
            ?.arguments?.firstOrNull()?.type?.resolve()
        if (target == null) {
            logger.error("${converter.simpleName.asString()} used by @Convert on '$name' does not implement ValueConverter", param)
            return null
        }
        val fieldType = type.makeNotNullable()
        val nullabilityOk = !target.isMarkedNullable || type.isMarkedNullable
        if (renderType(target.makeNotNullable()) != renderType(fieldType) || !nullabilityOk) {
            logger.error(
                "${converter.simpleName.asString()} converts to ${renderType(target)}, " +
                    "but '$name' is ${renderType(fieldType)}",
                param,
            )
            return null
        }
        val value = ValueType(
            code = renderType(fieldType),
            display = fieldType.declaration.simpleName.asString(),
            converter = "{ ${converter.qualifiedName!!.asString()}.convert(it) }",
            // Validation annotations work as for the built-in type, e.g. @Range on a converted Int
            shape = valueType(fieldType)?.shape ?: Shape.OTHER,
        )
        return FieldKind.Value(value, type.isMarkedNullable)
    }

    private fun renderType(type: KSType): String {
        val base = type.declaration.qualifiedName?.asString() ?: type.declaration.simpleName.asString()
        val args = type.arguments.map { it.type?.resolve()?.let(::renderType) ?: "*" }
        return base + (if (args.isEmpty()) "" else args.joinToString(prefix = "<", postfix = ">")) +
            if (type.isMarkedNullable) "?" else ""
    }

    /**
     * Builds the `validate` lambda from validation annotations, null when there are none, or
     * [INVALID] after logging an error for an annotation that doesn't fit the field's type.
     */
    private fun validationExpression(name: String, kind: FieldKind.Value, prop: KSPropertyDeclaration): String? {
        val shape = kind.type.shape
        val checks = mutableListOf<String>()
        var ok = true
        fun misuse(annotation: String, allowed: String) {
            logger.error("@$annotation on '$name' needs $allowed, but '$name' is ${kind.type.display}", prop)
            ok = false
        }

        findAnnotation(prop, Range::class.qualifiedName!!)?.let {
            if (shape != Shape.NUMBER) return@let misuse("Range", "an Int, Long, Double or Float")
            val min = (it.argument("min") as? Double) ?: Double.NEGATIVE_INFINITY
            val max = (it.argument("max") as? Double) ?: Double.POSITIVE_INFINITY
            // Via toString so a Float like 0.1f compares as 0.1, not 0.10000000149
            val asDouble = if (kind.type.code == "kotlin.Float") "v.toString().toDouble()" else "v.toDouble()"
            checks += "$VALIDATORS.range($asDouble, ${doubleLiteral(min)}, ${doubleLiteral(max)})"
        }
        findAnnotation(prop, Size::class.qualifiedName!!)?.let {
            if (shape != Shape.STRING && shape != Shape.COLLECTION) {
                return@let misuse("Size", "a String, List, Set or Map")
            }
            val min = (it.argument("min") as? Int) ?: 0
            val max = (it.argument("max") as? Int) ?: Int.MAX_VALUE
            val size = if (shape == Shape.STRING) "v.length" else "v.size"
            checks += "$VALIDATORS.size($size, $min, $max)"
        }
        findAnnotation(prop, NotBlank::class.qualifiedName!!)?.let {
            if (shape != Shape.STRING) return@let misuse("NotBlank", "a String")
            checks += "$VALIDATORS.notBlank(v)"
        }
        findAnnotation(prop, Pattern::class.qualifiedName!!)?.let {
            if (shape != Shape.STRING) return@let misuse("Pattern", "a String")
            val regex = it.argument("regex") as? String ?: ""
            try {
                Regex(regex)
            } catch (e: Exception) {
                logger.error("@Pattern on '$name' is not a valid regex: ${e.message}", prop)
                ok = false
                return@let
            }
            checks += "$VALIDATORS.pattern(v, ${kotlinString(regex)})"
        }

        if (!ok) return INVALID
        if (checks.isEmpty()) return null
        val chain = checks.joinToString(" ?: ")
        return if (kind.nullable) "{ v -> if (v == null) null else $chain }" else "{ v -> $chain }"
    }

    private fun doubleLiteral(d: Double): String = when (d) {
        Double.NEGATIVE_INFINITY -> "Double.NEGATIVE_INFINITY"
        Double.POSITIVE_INFINITY -> "Double.POSITIVE_INFINITY"
        else -> d.toString()
    }

    private fun kotlinString(s: String): String = buildString {
        append('"')
        for (ch in s) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '$' -> append("\\$")
                '\n' -> append("\\n")
                else -> append(ch)
            }
        }
        append('"')
    }

    private fun fieldKind(type: KSType): FieldKind? {
        if (isConfigSpec(type)) {
            return if (type.isMarkedNullable) null else FieldKind.Nested(type.declaration as KSClassDeclaration)
        }
        val value = valueType(type.makeNotNullable()) ?: return null
        return FieldKind.Value(value, type.isMarkedNullable)
    }

    private fun valueType(type: KSType): ValueType? {
        scalarType(type)?.let { return it }

        val qualified = type.declaration.qualifiedName?.asString() ?: return null
        val args = type.arguments.map { arg ->
            val resolved = arg.type?.resolve() ?: return null
            if (resolved.isMarkedNullable) return null
            resolved
        }
        return when (qualified) {
            "kotlin.collections.List" -> {
                val element = scalarType(args.single()) ?: return null
                ValueType(
                    code = "kotlin.collections.List<${element.code}>",
                    display = "List<${element.display}>",
                    converter = "{ $CONVERTERS.list(it, ${element.converter}) }",
                    shape = Shape.COLLECTION,
                )
            }
            "kotlin.collections.Set" -> {
                val element = scalarType(args.single()) ?: return null
                ValueType(
                    code = "kotlin.collections.Set<${element.code}>",
                    display = "Set<${element.display}>",
                    converter = "{ $CONVERTERS.list(it, ${element.converter}).toSet() }",
                    shape = Shape.COLLECTION,
                )
            }
            "kotlin.collections.Map" -> {
                val (key, value) = args
                if (key.declaration.qualifiedName?.asString() != "kotlin.String") return null
                val element = scalarType(value) ?: return null
                ValueType(
                    code = "kotlin.collections.Map<kotlin.String, ${element.code}>",
                    display = "Map<String, ${element.display}>",
                    converter = "{ $CONVERTERS.map(it, ${element.converter}) }",
                    childrenConverter = "{ $CONVERTERS.mapEntries(it, ${element.converter}) }",
                    shape = Shape.COLLECTION,
                )
            }
            else -> null
        }
    }

    private fun scalarType(type: KSType): ValueType? {
        val decl = type.declaration
        val qualified = decl.qualifiedName?.asString() ?: return null
        return when (qualified) {
            "kotlin.String" -> ValueType(qualified, "String", "{ it }", shape = Shape.STRING)
            "kotlin.Int" -> ValueType(qualified, "Int", "{ it.trim().toInt() }", shape = Shape.NUMBER)
            "kotlin.Long" -> ValueType(qualified, "Long", "{ it.trim().toLong() }", shape = Shape.NUMBER)
            "kotlin.Double" -> ValueType(qualified, "Double", "{ it.trim().toDouble() }", shape = Shape.NUMBER)
            "kotlin.Float" -> ValueType(qualified, "Float", "{ it.trim().toFloat() }", shape = Shape.NUMBER)
            "kotlin.Boolean" -> ValueType(qualified, "Boolean", "$CONVERTERS::boolean")
            "kotlin.time.Duration" -> ValueType(qualified, "Duration", "$CONVERTERS::duration")
            else -> if (decl is KSClassDeclaration && decl.classKind == ClassKind.ENUM_CLASS) {
                ValueType(qualified, decl.simpleName.asString(), "{ $CONVERTERS.enumOf($qualified.entries, it) }")
            } else {
                null
            }
        }
    }

    private fun isConfigSpec(type: KSType): Boolean {
        val decl = type.declaration
        return decl is KSClassDeclaration && decl.isAnnotationPresent(ConfigSpec::class)
    }

    private fun generateCode(decl: KSClassDeclaration, fields: List<Field>) {
        val className = decl.simpleName.asString()
        val packageName = decl.packageName.asString()
        val sourceFile = decl.containingFile!!

        // Default expressions are copied from source, so they need the source file's imports too
        val defaultValues = DefaultValueExtractor.extract(decl)
        val imports = sortedSetOf(
            "io.github.fiol_dev.konstant.core.ConfigError",
            "io.github.fiol_dev.konstant.core.ConfigLoader",
            "io.github.fiol_dev.konstant.core.ConfigResult",
            "io.github.fiol_dev.konstant.core.FieldDescriptor",
            "io.github.fiol_dev.konstant.core.ConfigLoaderBuilder",
            "io.github.fiol_dev.konstant.core.KeyUtils",
            "io.github.fiol_dev.konstant.core.Konstant",
            "io.github.fiol_dev.konstant.core.ResolveResult",
        )
        imports += DefaultValueExtractor.extractImports(decl)
        for (field in fields) {
            val kind = field.kind
            if (kind is FieldKind.Nested) {
                val nestedPackage = kind.decl.packageName.asString()
                if (nestedPackage != packageName) {
                    imports += "$nestedPackage.load${kind.decl.simpleName.asString()}"
                    imports += "$nestedPackage.konstantNestedConfigs"
                }
            }
        }

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(true, sourceFile),
            packageName = packageName,
            fileName = "${className}Generated",
        )

        val code = buildString {
            appendLine("@file:Suppress(\"UNCHECKED_CAST\", \"RedundantSuppression\", \"UnusedImport\")")
            appendLine()
            if (packageName.isNotEmpty()) {
                appendLine("package $packageName")
                appendLine()
            }
            imports.forEach { appendLine("import $it") }
            appendLine()

            generateSchema(className, fields, defaultValues)
            appendLine()
            generateLoader(className, fields)
            appendLine()
            generateHolder(className, fields)
        }

        file.write(code.toByteArray())
        file.close()
    }

    private fun StringBuilder.generateSchema(
        className: String,
        fields: List<Field>,
        defaultValues: Map<String, String>,
    ) {
        appendLine("object ${className}Schema {")
        for (field in fields) {
            val kind = field.kind as? FieldKind.Value ?: continue
            val propName = field.name
            val isSecret = field.property?.isAnnotationPresent(Secret::class) == true
            val customKey = field.property?.let { getCustomKey(it) }
            val codeType = kind.type.code + if (kind.nullable) "?" else ""
            // A nullable field without a default is optional and defaults to null
            val hasDefault = field.param.hasDefault || kind.nullable
            val defaultExpr = if (field.param.hasDefault) defaultValues[propName] else null

            appendLine("    val $propName = FieldDescriptor<$codeType>(")
            appendLine("        propertyName = \"$propName\",")
            appendLine("        envKey = \"${camelToScreamingSnake(propName)}\",")
            appendLine("        typeName = \"${kind.type.display}\",")
            appendLine("        required = ${!hasDefault},")
            appendLine("        secret = $isSecret,")
            appendLine("        default = ${defaultExpr ?: "null"},")
            appendLine("        hasDefault = $hasDefault,")
            if (customKey != null) {
                appendLine("        customKey = \"$customKey\",")
            }
            appendLine("        convert = ${kind.type.converter},")
            kind.type.childrenConverter?.let { appendLine("        convertChildren = $it,") }
            kind.validate?.let { appendLine("        validate = $it,") }
            appendLine("    )")
        }
        appendLine("}")
    }

    private fun StringBuilder.generateLoader(className: String, fields: List<Field>) {
        appendLine("fun ConfigLoader.load$className(prefix: String? = null): ConfigResult<$className> {")
        appendLine("    val errors = mutableListOf<ConfigError>()")
        appendLine()

        for (field in fields) {
            val propName = field.name
            when (val kind = field.kind) {
                is FieldKind.Nested -> {
                    val nestedQualified = kind.decl.qualifiedName!!.asString()
                    val capName = propName.replaceFirstChar { it.uppercase() }
                    appendLine("    val nestedPrefix$capName = KeyUtils.resolvePrefix(\"$propName\", prefix)")
                    appendLine("    val ${propName}Result = load${kind.decl.simpleName.asString()}(nestedPrefix$capName)")
                    appendLine("    val $propName: $nestedQualified? = when (${propName}Result) {")
                    appendLine("        is ConfigResult.Success -> ${propName}Result.value")
                    appendLine("        is ConfigResult.Failure -> { errors += ${propName}Result.errors; null }")
                    appendLine("    }")
                }
                is FieldKind.Value -> {
                    appendLine("    val ${propName}Result = resolve(${className}Schema.$propName, prefix)")
                    appendLine("    val $propName: ${kind.type.code}? = when (${propName}Result) {")
                    appendLine("        is ResolveResult.Success -> ${propName}Result.value")
                    appendLine("        is ResolveResult.Error -> { errors += ${propName}Result.error; null }")
                    appendLine("    }")
                }
            }
            appendLine()
        }

        appendLine("    if (errors.isNotEmpty()) return ConfigResult.Failure(errors)")
        appendLine()

        // require(...) in the class's init block becomes a ValidationFailed error
        appendLine("    return try {")
        appendLine("        ConfigResult.Success($className(")
        for (field in fields) {
            val nullable = (field.kind as? FieldKind.Value)?.nullable == true
            // Every non-null field is set once errors is empty
            appendLine("            ${field.name} = ${field.name}${if (nullable) "" else "!!"},")
        }
        appendLine("        ))")
        appendLine("    } catch (e: IllegalArgumentException) {")
        appendLine("        ConfigResult.Failure(listOf(ConfigError.ValidationFailed(")
        appendLine("            key = prefix ?: \"$className\",")
        appendLine("            rawValue = null,")
        appendLine("            reason = e.message ?: \"rejected by $className\",")
        appendLine("        )))")
        appendLine("    }")
        appendLine("}")
    }

    /** Loads the spec into the global [Konstant] holder, together with every nested spec. */
    private fun StringBuilder.generateHolder(className: String, fields: List<Field>) {
        appendLine("/** Every nested @ConfigSpec inside this config, at any depth. Used by [Konstant.init$className]. */")
        appendLine("fun $className.konstantNestedConfigs(): List<Any> = buildList {")
        for (field in fields) {
            if (field.kind !is FieldKind.Nested) continue
            // Qualified, so a field named like a list member (size, indices) still means the config's
            val ref = "this@konstantNestedConfigs.${field.name}"
            appendLine("    add($ref)")
            appendLine("    addAll($ref.konstantNestedConfigs())")
        }
        appendLine("}")
        appendLine()
        appendLine("/**")
        appendLine(" * Loads [$className] from the sources in [block] and makes it and its nested specs")
        appendLine(" * available through Konstant.get. Throws ConfigException if loading fails.")
        appendLine(" */")
        appendLine("fun Konstant.init$className(prefix: String? = null, block: ConfigLoaderBuilder.() -> Unit): $className =")
        appendLine("    init$className(ConfigLoader(block), prefix)")
        appendLine()
        appendLine("/** Like the builder overload, with an existing [loader]. */")
        appendLine("fun Konstant.init$className(loader: ConfigLoader, prefix: String? = null): $className {")
        appendLine("    val config = loader.load$className(prefix).getOrThrow()")
        appendLine("    install(config, config.konstantNestedConfigs())")
        appendLine("    return config")
        appendLine("}")
    }

    private fun getCustomKey(prop: KSPropertyDeclaration): String? {
        val keyAnnotation = prop.annotations.firstOrNull {
            it.shortName.asString() == "Key" &&
                it.annotationType.resolve().declaration.qualifiedName?.asString() == Key::class.qualifiedName
        } ?: return null
        return keyAnnotation.arguments.firstOrNull()?.value as? String
    }

    private fun camelToScreamingSnake(name: String): String = buildString {
        for ((i, ch) in name.withIndex()) {
            if (ch.isUpperCase() && i > 0) append('_')
            append(ch.uppercaseChar())
        }
    }
}
