package io.github.fiol_dev.konfigure.core

class ConfigLoader(block: ConfigLoaderBuilder.() -> Unit) {
    val sources: List<ConfigSource>

    init {
        val builder = ConfigLoaderBuilder()
        builder.block()
        sources = builder.buildSources()
    }

    fun <T> resolve(field: FieldDescriptor<T>, prefix: String?): ResolveResult<T> {
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
                    return try {
                        ResolveResult.Success(field.convert(raw))
                    } catch (e: Exception) {
                        val displayValue = if (field.secret) "***" else raw
                        ResolveResult.Error(
                            ConfigError.ConversionFailed(
                                key = key,
                                rawValue = displayValue,
                                targetType = field.typeName,
                                cause = e.message ?: "unknown"
                            )
                        )
                    }
                }
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
}

class ConfigLoaderBuilder {
    private val sourceList = mutableListOf<ConfigSource>()

    fun sources(block: SourcesBuilder.() -> Unit) {
        val builder = SourcesBuilder()
        builder.block()
        sourceList.addAll(builder.sources)
    }

    fun buildSources(): List<ConfigSource> = sourceList.toList()
}

class SourcesBuilder {
    internal val sources = mutableListOf<ConfigSource>()

    operator fun ConfigSource.unaryPlus() {
        sources += this
    }
}
