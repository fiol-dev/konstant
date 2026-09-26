@file:OptIn(InternalKonstantApi::class)

package io.github.fiol_dev.konstant.core

/**
 * Reads config values from a list of sources. The KSP processor adds a `load<Spec>()`
 * extension for each `@ConfigSpec` class, which returns a [ConfigResult]:
 *
 * ```kotlin
 * val loader = ConfigLoader {
 *     sources {
 *         +EnvSource()
 *         +TomlSource.fromFile("config.toml")
 *     }
 * }
 * val config = loader.loadAppConfig().getOrThrow()
 * ```
 */
public class ConfigLoader private constructor(
    /**
     * The sources in priority order: for each field, the first source that has a value wins,
     * and later sources are only asked when earlier ones have none.
     */
    public val sources: List<ConfigSource>,
    // Set only on the copy made by explain, so ordinary loads record nothing
    private val recorder: MutableList<ConfigReport.Entry>?,
) {
    /** Creates a loader with the sources declared in [block]'s [ConfigLoaderBuilder.sources]. */
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

    /** Resolves one field from the sources in priority order. Called by generated code. */
    @InternalKonstantApi
    public fun <T> resolve(field: FieldDescriptor<T>, prefix: String?): ResolveResult<T> {
        // Reports name every field the same way, whichever source format supplied it
        val reportKey = if (recorder == null) "" else KeyUtils.resolveKey(
            propertyName = field.propertyName,
            prefix = prefix,
            format = KeyFormat.DOT_NOTATION,
            customKey = field.customKey,
        )
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
                    record(field, reportKey, raw, "#${index + 1} ${source.name}", key)
                    return convert(field, key, raw) { field.convert(raw) }
                }
                val convertChildren = field.convertChildren ?: continue
                val children = source.children(key) ?: continue
                val shown = children.entries.joinToString(prefix = "{", postfix = "}") { "${it.key}=${it.value}" }
                record(field, reportKey, shown, "#${index + 1} ${source.name}", key)
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
            if (recorder != null) record(field, reportKey, field.default?.toString(), ConfigReport.DEFAULT)
            @Suppress("UNCHECKED_CAST")
            return ResolveResult.Success(field.default as T)
        }
        record(field, reportKey, null, ConfigReport.MISSING)
        return ResolveResult.Error(ConfigError.MissingRequired(resolvedKey))
    }

    private fun record(
        field: FieldDescriptor<*>,
        key: String,
        value: String?,
        origin: String,
        sourceKey: String? = null,
    ) {
        val recorder = recorder ?: return
        val shown = if (field.secret && value != null) "***" else value
        recorder.add(ConfigReport.Entry(key, shown, origin, sourceKey?.takeIf { it != key }))
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

/** Configures a [ConfigLoader]. */
public class ConfigLoaderBuilder {
    private val sourceList = mutableListOf<ConfigSource>()

    /**
     * Adds sources with `+source`, highest priority first. Calling it again appends
     * after the sources already added.
     */
    public fun sources(block: SourcesBuilder.() -> Unit) {
        val builder = SourcesBuilder()
        builder.block()
        sourceList.addAll(builder.sources)
    }

    internal fun buildSources(): List<ConfigSource> = sourceList.toList()
}

/** Collects sources inside [ConfigLoaderBuilder.sources], in the order they are added. */
public class SourcesBuilder {
    internal val sources = mutableListOf<ConfigSource>()

    /** Adds this source after the ones already added, so it has lower priority. */
    public operator fun ConfigSource.unaryPlus() {
        sources += this
    }

    /** Adds several sources in order, e.g. `+KonstantBaked.sources` from the Gradle plugin. */
    public operator fun List<ConfigSource>.unaryPlus() {
        sources += this
    }
}
