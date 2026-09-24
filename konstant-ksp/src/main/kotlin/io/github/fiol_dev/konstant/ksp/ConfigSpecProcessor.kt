package io.github.fiol_dev.konstant.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate
import io.github.fiol_dev.konstant.annotations.ConfigSpec
import io.github.fiol_dev.konstant.annotations.Key
import io.github.fiol_dev.konstant.annotations.Secret

@OptIn(KspExperimental::class)
class ConfigSpecProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    companion object {
        private const val CONVERTERS = "io.github.fiol_dev.konstant.core.Converters"

        private const val SUPPORTED_TYPES_HINT =
            "Supported types: String, Int, Long, Double, Float, Boolean, Duration, enums, " +
                "List/Set of those, Map<String, T> of those, nullable versions of all of them, " +
                "and @ConfigSpec data classes."
    }

    /** How a single value is written in generated code. */
    private data class ValueType(
        /** Fully qualified Kotlin type, safe to use without imports. */
        val code: String,
        /** Short form shown in error messages, e.g. `List<Int>`. */
        val display: String,
        /** Expression of type `(String) -> T`. */
        val converter: String,
    )

    private sealed interface FieldKind {
        data class Value(val type: ValueType, val nullable: Boolean) : FieldKind
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
            val kind = fieldKind(type)
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
            fields += Field(param, name, kind, properties[name])
        }
        return if (valid) fields else null
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
                )
            }
            "kotlin.collections.Set" -> {
                val element = scalarType(args.single()) ?: return null
                ValueType(
                    code = "kotlin.collections.Set<${element.code}>",
                    display = "Set<${element.display}>",
                    converter = "{ $CONVERTERS.list(it, ${element.converter}).toSet() }",
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
                )
            }
            else -> null
        }
    }

    private fun scalarType(type: KSType): ValueType? {
        val decl = type.declaration
        val qualified = decl.qualifiedName?.asString() ?: return null
        return when (qualified) {
            "kotlin.String" -> ValueType(qualified, "String", "{ it }")
            "kotlin.Int" -> ValueType(qualified, "Int", "{ it.trim().toInt() }")
            "kotlin.Long" -> ValueType(qualified, "Long", "{ it.trim().toLong() }")
            "kotlin.Double" -> ValueType(qualified, "Double", "{ it.trim().toDouble() }")
            "kotlin.Float" -> ValueType(qualified, "Float", "{ it.trim().toFloat() }")
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
            "io.github.fiol_dev.konstant.core.KeyUtils",
            "io.github.fiol_dev.konstant.core.ResolveResult",
        )
        imports += DefaultValueExtractor.extractImports(decl)
        for (field in fields) {
            val kind = field.kind
            if (kind is FieldKind.Nested) {
                val nestedPackage = kind.decl.packageName.asString()
                if (nestedPackage != packageName) {
                    imports += "$nestedPackage.load${kind.decl.simpleName.asString()}"
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

        appendLine("    return ConfigResult.Success($className(")
        for (field in fields) {
            val nullable = (field.kind as? FieldKind.Value)?.nullable == true
            // Every non-null field is set once errors is empty
            appendLine("        ${field.name} = ${field.name}${if (nullable) "" else "!!"},")
        }
        appendLine("    ))")
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
