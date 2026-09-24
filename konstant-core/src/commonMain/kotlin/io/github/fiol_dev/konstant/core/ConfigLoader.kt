package io.github.fiol_dev.konstant.core

public class ConfigLoader private constructor(
    public val sources: List<ConfigSource>,
    // Set only on the copy made by explain, so ordinary loads record nothing
    private val recorder: MutableList<ConfigReport.Entry>?,
) {
    public constructor(block: ConfigLoaderBuilder.() -> Unit) :
        this(ConfigLoaderBuilder().apply(block).buildSources(), null)

    /**
     * Runs [load] and reports, for every field, the value used and which source it came from.
     * Secrets are shown as `***`. A value that fails conversion or validation is still listed
     * with its source; the error itself is in [ConfigReport.result]. Useful for debugging which
     * file or variable won:
     *
     * ```kotlin
     * println(loader.explain { loadAppConfig() })
     * ```
     */
    public fun <T> explain(load: ConfigLoader.() -> ConfigResult<T>): ConfigReport<T> {
        val entries = mutableListOf<ConfigReport.Entry>()
        val result = ConfigLoader(sources, entries).load()
        return ConfigReport(result, entries)
    }

    public fun <T> resolve(field: FieldDescriptor<T>, prefix: String?): ResolveResult<T> {
        for ((index, source) in sources.withIndex()) {
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
                    record(field, key, raw, "#${index + 1} ${source.name}")
                    return convert(field, key, raw) { field.convert(raw) }
                }
                val convertChildren = field.convertChildren ?: continue
                val children = source.children(key) ?: continue
                val shown = children.entries.joinToString(prefix = "{", postfix = "}") { "${it.key}=${it.value}" }
                record(field, key, shown, "#${index + 1} ${source.name}")
                return convert(field, key, shown) { convertChildren(children) }
            }
        }
        // Not found in any source
        val resolvedKey = KeyUtils.resolveKey(
            propertyName = field.propertyName,
            prefix = prefix,
            format = KeyFormat.SCREAMING_SNAKE,
            customKey = field.customKey,
        )
        if (field.hasDefault) {
            if (recorder != null) record(field, resolvedKey, field.default?.toString(), ConfigReport.DEFAULT)
            @Suppress("UNCHECKED_CAST")
            return ResolveResult.Success(field.default as T)
        }
        record(field, resolvedKey, null, ConfigReport.MISSING)
        return ResolveResult.Error(ConfigError.MissingRequired(resolvedKey))
    }

    private fun record(field: FieldDescriptor<*>, key: String, value: String?, origin: String) {
        recorder?.add(ConfigReport.Entry(key, if (field.secret && value != null) "***" else value, origin))
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

    /** Adds several sources in order, e.g. `+KonstantBaked.sources` from the Gradle plugin. */
    public operator fun List<ConfigSource>.unaryPlus() {
        sources += this
    }
}
