package io.github.fiol_dev.konstant.core

public class ConfigLoader(block: ConfigLoaderBuilder.() -> Unit) {
    public val sources: List<ConfigSource>

    init {
        val builder = ConfigLoaderBuilder()
        builder.block()
        sources = builder.buildSources()
    }

    public fun <T> resolve(field: FieldDescriptor<T>, prefix: String?): ResolveResult<T> {
        for (source in sources) {
            val formats = listOf(source.keyFormat) + source.fallbackKeyFormats
            for (format in formats) {
                val key = KeyUtils.resolveKey(
                    propertyName = field.propertyName,
                    prefix = prefix,
                    format = format,
                    customKey = field.customKey,
                )
                val raw = source.get(key)
                if (raw != null) {
                    return convert(field, key, raw) { field.convert(raw) }
                }
                val convertChildren = field.convertChildren ?: continue
                val children = source.children(key) ?: continue
                val shown = children.entries.joinToString(prefix = "{", postfix = "}") { "${it.key}=${it.value}" }
                return convert(field, key, shown) { convertChildren(children) }
            }
        }
        // Not found in any source
        if (field.hasDefault) {
            @Suppress("UNCHECKED_CAST")
            return ResolveResult.Success(field.default as T)
        }
        val resolvedKey = KeyUtils.resolveKey(
            propertyName = field.propertyName,
            prefix = prefix,
            format = KeyFormat.SCREAMING_SNAKE,
            customKey = field.customKey,
        )
        return ResolveResult.Error(ConfigError.MissingRequired(resolvedKey))
    }

    private inline fun <T> convert(
        field: FieldDescriptor<T>,
        key: String,
        raw: String,
        block: () -> T,
    ): ResolveResult<T> = try {
        val value = block()
        val problem = field.validate?.invoke(value)
        if (problem == null) {
            ResolveResult.Success(value)
        } else {
            ResolveResult.Error(
                ConfigError.ValidationFailed(key, if (field.secret) "***" else raw, problem)
            )
        }
    } catch (e: Exception) {
        // Parser messages often echo the input, so a secret's cause is redacted too
        ResolveResult.Error(
            ConfigError.ConversionFailed(
                key = key,
                rawValue = if (field.secret) "***" else raw,
                targetType = field.typeName,
                cause = if (field.secret) "invalid value" else e.message ?: "unknown",
            )
        )
    }
}

public class ConfigLoaderBuilder {
    private val sourceList = mutableListOf<ConfigSource>()

    public fun sources(block: SourcesBuilder.() -> Unit) {
        val builder = SourcesBuilder()
        builder.block()
        sourceList.addAll(builder.sources)
    }

    public fun buildSources(): List<ConfigSource> = sourceList.toList()
}

public class SourcesBuilder {
    internal val sources = mutableListOf<ConfigSource>()

    public operator fun ConfigSource.unaryPlus() {
        sources += this
    }
}
