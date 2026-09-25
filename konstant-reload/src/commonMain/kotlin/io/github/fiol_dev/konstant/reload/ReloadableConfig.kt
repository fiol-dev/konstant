package io.github.fiol_dev.konstant.reload

import io.github.fiol_dev.konstant.core.ConfigException
import io.github.fiol_dev.konstant.core.ConfigLoader
import io.github.fiol_dev.konstant.core.ConfigLoaderBuilder
import io.github.fiol_dev.konstant.core.ConfigResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

/**
 * A config that can be reloaded while the app runs. Every reload builds the sources again (so
 * files are read again), loads and validates the config, and publishes it to [config] only if it
 * is valid. A failed reload keeps the last good config and emits the error to [failures].
 * Reloads run one at a time, so a slow reload never overwrites a newer one.
 *
 * ```kotlin
 * val appConfig = ReloadableConfig.load(load = { loadAppConfig() }) {
 *     sources {
 *         +EnvSource()
 *         +TomlSource.fromFile("config.toml")
 *     }
 * }
 *
 * scope.launch { appConfig.failures.collect { log.warn("Config reload skipped", it) } }
 * appConfig.reloadEvery(scope, 30.seconds)
 * appConfig.config.collect { config -> applyLogLevel(config.logLevel) }
 * ```
 */
public class ReloadableConfig<T : Any> private constructor(
    initial: T,
    private val sources: ConfigLoaderBuilder.() -> Unit,
    private val load: ConfigLoader.() -> ConfigResult<T>,
    private val context: CoroutineContext,
) {
    private val state = MutableStateFlow(initial)
    private val mutex = Mutex()
    private val failureEvents = MutableSharedFlow<Throwable>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** The latest valid config. Collect it to react to updates. */
    public val config: StateFlow<T> = state.asStateFlow()

    /** The latest valid config, the same as `config.value`. */
    public val current: T get() = state.value

    /**
     * Errors from reloads that failed and were skipped, see [ReloadResult.Failed.error].
     * Nothing is replayed, so start collecting before starting reloads.
     */
    public val failures: SharedFlow<Throwable> = failureEvents.asSharedFlow()

    /**
     * Loads the config again and publishes it if it is valid and different. Never throws
     * except for cancellation: a failure keeps the current config and is emitted to [failures].
     */
    public suspend fun reload(): ReloadResult<T> = mutex.withLock {
        val next = try {
            withContext(context) { ConfigLoader(sources).load().getOrThrow() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // Throwable, not Exception: JavaScript errors from Node's fs are not Kotlin Exceptions
            failureEvents.tryEmit(e)
            return ReloadResult.Failed(state.value, e)
        }
        if (next == state.value) return ReloadResult.Unchanged(next)
        state.value = next
        ReloadResult.Updated(next)
    }

    /** Reloads each time [triggers] emits, until [scope] or the returned job is cancelled. */
    public fun reloadOn(scope: CoroutineScope, triggers: Flow<*>): Job = scope.launch {
        triggers.collect { reload() }
    }

    /** Reloads every [period], until [scope] or the returned job is cancelled. */
    public fun reloadEvery(scope: CoroutineScope, period: Duration): Job {
        require(period.isPositive()) { "period must be positive, was $period" }
        return scope.launch {
            while (isActive) {
                delay(period)
                reload()
            }
        }
    }

    /**
     * Reloads each time one of [sources] reports a change. Pass the same instances that the
     * `sources` block adds, so the watched source is the one that is read:
     *
     * ```kotlin
     * val remote = RemoteSource()
     * val appConfig = ReloadableConfig.load(load = { loadAppConfig() }) { sources { +remote } }
     * appConfig.watch(scope, remote)
     * ```
     */
    public fun watch(scope: CoroutineScope, vararg sources: ReloadableSource): Job {
        require(sources.isNotEmpty()) { "watch needs at least one ReloadableSource" }
        return reloadOn(scope, sources.map { it.changes }.merge())
    }

    /** Creates a [ReloadableConfig] with [load]. */
    public companion object {
        /**
         * Loads the config for the first time.
         *
         * @param load reads the config from a loader, usually the generated `load<Spec>()`.
         * @param context where later reloads read their sources, e.g. `Dispatchers.IO` so a
         *   reload started from the main thread does not read files on it.
         * @param sources declares the sources, and runs again on every reload.
         * @throws ConfigException if the first load fails, since there is no last good config yet.
         */
        public fun <T : Any> load(
            load: ConfigLoader.() -> ConfigResult<T>,
            context: CoroutineContext = EmptyCoroutineContext,
            sources: ConfigLoaderBuilder.() -> Unit,
        ): ReloadableConfig<T> =
            ReloadableConfig(ConfigLoader(sources).load().getOrThrow(), sources, load, context)
    }
}
