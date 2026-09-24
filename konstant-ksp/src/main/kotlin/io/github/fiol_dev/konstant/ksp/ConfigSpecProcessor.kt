package io.github.fiol_dev.konstant.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
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
        private val SUPPORTED_TYPES = setOf(
            "kotlin.String",
            "kotlin.Int",
            "kotlin.Long",
            "kotlin.Double",
            "kotlin.Float",
            "kotlin.Boolean",
        )
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(ConfigSpec::class.qualifiedName!!)
        val deferred = mutableListOf<KSAnnotated>()

        for (symbol in symbols) {
            if (!symbol.validate()) {
                deferred += symbol
                continue
            }
            if (symbol !is KSClassDeclaration) continue
            if (!validateClass(symbol)) continue
            generateCode(symbol)
        }
        return deferred
    }

    private fun validateClass(decl: KSClassDeclaration): Boolean {
        if (decl.classKind != ClassKind.CLASS || Modifier.DATA !in decl.modifiers) {
            logger.error("@ConfigSpec can only be applied to data classes", decl)
            return false
        }

        var valid = true
        for (prop in decl.getDeclaredProperties()) {
            val type = prop.type.resolve()
            val qualifiedName = type.declaration.qualifiedName?.asString() ?: ""

            if (qualifiedName in SUPPORTED_TYPES) continue

            val typeDecl = type.declaration
            if (typeDecl is KSClassDeclaration && typeDecl.isAnnotationPresent(ConfigSpec::class)) {
                continue
            }

            logger.error(
                "Unsupported field type '$qualifiedName' for property '${prop.simpleName.asString()}'. " +
                    "Supported types: String, Int, Long, Double, Float, Boolean, and @ConfigSpec data classes.",
                prop
            )
            valid = false
        }
        return valid
    }

    private fun generateCode(decl: KSClassDeclaration) {
        val className = decl.simpleName.asString()
        val packageName = decl.packageName.asString()
        val constructorParams = decl.primaryConstructor?.parameters ?: return
        val properties = decl.getDeclaredProperties().toList()

        // Extract default value expressions from source
        val defaultValues = DefaultValueExtractor.extract(decl)

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(true, decl.containingFile!!),
            packageName = packageName,
            fileName = "${className}Generated",
        )

        val code = buildString {
            appendLine("@file:Suppress(\"UNCHECKED_CAST\")")
            appendLine()
            appendLine("package $packageName")
            appendLine()
            appendLine("import io.github.fiol_dev.konstant.core.ConfigError")
            appendLine("import io.github.fiol_dev.konstant.core.ConfigLoader")
            appendLine("import io.github.fiol_dev.konstant.core.ConfigResult")
            appendLine("import io.github.fiol_dev.konstant.core.FieldDescriptor")
            appendLine("import io.github.fiol_dev.konstant.core.KeyUtils")
            appendLine("import io.github.fiol_dev.konstant.core.ResolveResult")
            appendLine()

            generateSchema(className, constructorParams, properties, defaultValues)
            appendLine()
            generateLoader(className, constructorParams, properties, defaultValues)
        }

        file.write(code.toByteArray())
        file.close()
    }

    private fun StringBuilder.generateSchema(
        className: String,
        constructorParams: List<KSValueParameter>,
        properties: List<com.google.devtools.ksp.symbol.KSPropertyDeclaration>,
        defaultValues: Map<String, String>,
    ) {
        appendLine("object ${className}Schema {")
        for (param in constructorParams) {
            val propName = param.name?.asString() ?: continue
            val type = param.type.resolve()
            val qualifiedType = type.declaration.qualifiedName?.asString() ?: ""

            if (isNestedConfigSpec(type)) continue

            val prop = properties.firstOrNull { it.simpleName.asString() == propName }
            val isSecret = prop?.isAnnotationPresent(Secret::class) == true
            val customKey = prop?.let { getCustomKey(it) }
            val hasDefault = param.hasDefault
            val envKey = camelToScreamingSnake(propName)
            val typeStr = mapTypeToKotlin(qualifiedType)
            val converterStr = converterForType(qualifiedType)
            val defaultExpr = if (hasDefault) defaultValues[propName] else null
            val required = !hasDefault

            appendLine("    val $propName = FieldDescriptor<$typeStr>(")
            appendLine("        propertyName = \"$propName\",")
            appendLine("        envKey = \"$envKey\",")
            appendLine("        typeName = \"$typeStr\",")
            appendLine("        required = $required,")
            appendLine("        secret = $isSecret,")
            if (defaultExpr != null) {
                appendLine("        default = $defaultExpr,")
            } else {
                appendLine("        default = null,")
            }
            appendLine("        hasDefault = $hasDefault,")
            if (customKey != null) {
                appendLine("        customKey = \"$customKey\",")
            }
            appendLine("        convert = $converterStr,")
            appendLine("    )")
        }
        appendLine("}")
    }

    private fun StringBuilder.generateLoader(
        className: String,
        constructorParams: List<KSValueParameter>,
        properties: List<com.google.devtools.ksp.symbol.KSPropertyDeclaration>,
        defaultValues: Map<String, String>,
    ) {
        val funName = "load$className"
        appendLine("fun ConfigLoader.$funName(prefix: String? = null): ConfigResult<$className> {")
        appendLine("    val errors = mutableListOf<ConfigError>()")
        appendLine()

        // Resolve each field
        for (param in constructorParams) {
            val propName = param.name?.asString() ?: continue
            val type = param.type.resolve()
            val qualifiedType = type.declaration.qualifiedName?.asString() ?: ""
            val isNested = isNestedConfigSpec(type)

            if (isNested) {
                val nestedClassName = type.declaration.simpleName.asString()
                val nestedLoader = "load$nestedClassName"
                val capName = propName.replaceFirstChar { it.uppercase() }
                appendLine("    val nestedPrefix$capName = KeyUtils.resolvePrefix(\"$propName\", prefix)")
                appendLine("    val ${propName}Result = $nestedLoader(nestedPrefix$capName)")
                appendLine("    val $propName: $nestedClassName? = when (${propName}Result) {")
                appendLine("        is ConfigResult.Success -> ${propName}Result.value")
                appendLine("        is ConfigResult.Failure -> { errors += ${propName}Result.errors; null }")
                appendLine("    }")
            } else {
                val typeStr = mapTypeToKotlin(qualifiedType)

                appendLine("    val ${propName}Result = resolve(${className}Schema.$propName, prefix)")
                appendLine("    val $propName: $typeStr? = when (${propName}Result) {")
                appendLine("        is ResolveResult.Success -> ${propName}Result.value")
                appendLine("        is ResolveResult.Error -> { errors += ${propName}Result.error; null }")
                appendLine("    }")
            }
            appendLine()
        }

        appendLine("    if (errors.isNotEmpty()) return ConfigResult.Failure(errors)")
        appendLine()

        // Construct the object
        appendLine("    return ConfigResult.Success($className(")
        for ((i, param) in constructorParams.withIndex()) {
            val propName = param.name?.asString() ?: continue
            val type = param.type.resolve()
            val isNested = isNestedConfigSpec(type)
            val comma = ","

            if (isNested || !param.hasDefault) {
                appendLine("        $propName = $propName!!$comma")
            } else {
                // Has default — resolve already returns the default from FieldDescriptor
                appendLine("        $propName = $propName!!$comma")
            }
        }
        appendLine("    ))")
        appendLine("}")
    }

    private fun isNestedConfigSpec(type: KSType): Boolean {
        val decl = type.declaration
        return decl is KSClassDeclaration && decl.isAnnotationPresent(ConfigSpec::class)
    }

    private fun getCustomKey(prop: com.google.devtools.ksp.symbol.KSPropertyDeclaration): String? {
        val keyAnnotation = prop.annotations.firstOrNull {
            it.shortName.asString() == "Key" &&
                it.annotationType.resolve().declaration.qualifiedName?.asString() == Key::class.qualifiedName
        } ?: return null
        return keyAnnotation.arguments.firstOrNull()?.value as? String
    }

    private fun mapTypeToKotlin(qualifiedName: String): String = when (qualifiedName) {
        "kotlin.String" -> "String"
        "kotlin.Int" -> "Int"
        "kotlin.Long" -> "Long"
        "kotlin.Double" -> "Double"
        "kotlin.Float" -> "Float"
        "kotlin.Boolean" -> "Boolean"
        else -> qualifiedName.substringAfterLast('.')
    }

    private fun converterForType(qualifiedName: String): String = when (qualifiedName) {
        "kotlin.String" -> "{ it }"
        "kotlin.Int" -> "String::toInt"
        "kotlin.Long" -> "String::toLong"
        "kotlin.Double" -> "String::toDouble"
        "kotlin.Float" -> "String::toFloat"
        "kotlin.Boolean" -> "String::toBoolean"
        else -> "{ it }"
    }

    private fun camelToScreamingSnake(name: String): String = buildString {
        for ((i, ch) in name.withIndex()) {
            if (ch.isUpperCase() && i > 0) append('_')
            append(ch.uppercaseChar())
        }
    }
}
