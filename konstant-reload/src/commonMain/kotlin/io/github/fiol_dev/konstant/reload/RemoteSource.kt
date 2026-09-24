package io.github.fiol_dev.konstant.reload

import io.github.fiol_dev.konstant.core.KeyFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map

/**
 * A source whose values are pushed in by the app, typically from a remote config service.
 * It works with any provider: fetch the values, then call [update]. Each change is reported on
 * [changes], so [ReloadableConfig.watch] reloads the config:
 *
 * ```kotlin
 * val remote = RemoteSource(name = "Firebase")
 * val appConfig = ReloadableConfig.load(load = { loadAppConfig() }) {
 *     sources {
 *         +remote              // first, so remote values override the local defaults
 *         +TomlSource.fromResource("config.toml")
 *     }
 * }
 * appConfig.watch(scope, remote)
 *
 * // Whenever the provider has new values
 * remote.update(fetchedValues)
 * ```
 *
 * Keys use [keyFormat] (dot notation by default, e.g. `server.port`) and are matched
 * case-insensitively, like the file sources.
 */
public class RemoteSource(
    initial: Map<String, String> = emptyMap(),
    override val keyFormat: KeyFormat = KeyFormat.DOT_NOTATION,
    override val fallbackKeyFormats: List<KeyFormat> = listOf(KeyFormat.SCREAMING_SNAKE),
    override val name: String = "RemoteSource",
) : ReloadableSource {
    private val state = MutableStateFlow(initial.toMap())

    /** The current values. */
    public val values: Map<String, String> get() = state.value

    /** Emits after [update] changed the values; updating to equal values emits nothing. */
    override val changes: Flow<Unit> = state.drop(1).map { }

    /** Replaces all values. Keys missing from [values] fall through to the next source. */
    public fun update(values: Map<String, String>) {
        state.value = values.toMap()
    }

    override fun get(key: String): String? {
        val entries = state.value
        return entries[key] ?: entries.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value
    }

    override fun children(key: String): Map<String, String>? {
        val prefix = "$key."
        return state.value
            .filterKeys { it.length > prefix.length && it.startsWith(prefix, ignoreCase = true) }
            .mapKeys { it.key.substring(prefix.length) }
            .takeIf { it.isNotEmpty() }
    }
}
