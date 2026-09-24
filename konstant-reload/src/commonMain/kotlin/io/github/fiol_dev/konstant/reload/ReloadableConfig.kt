package io.github.fiol_dev.konstant.reload

import io.github.fiol_dev.konstant.core.ConfigException
import io.github.fiol_dev.konstant.core.ConfigLoader
import io.github.fiol_dev.konstant.core.ConfigLoaderBuilder
import io.github.fiol_dev.konstant.core.ConfigResult
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
import kotlin.time.Duration

/**
 * A config that can be reloaded while the app runs. Every reload builds the sources again (so
 * files are read again), loads and validates the config, and publishes it to [config] only if it
 * is valid. A failed reload keeps the last good config and emits the error to [failures].
 *
 * ```kotlin
 * val appConfig = ReloadableConfig(load = { loadAppConfig() }) {
 *     sources {
 *         +EnvSource()
 *         +TomlSource.fromFile("config.toml")
 *     }
 * }
 *
 * appConfig.reloadEvery(scope, 30.seconds)   // or reloadOn(scope, fileChanges), or watch(scope)
 * appConfig.config.collect { config -> applyLogLevel(config.logLevel) }
 * ```
 */
public class ReloadableConfig<T : Any> private constructor(
    initial: T,
    private val sources: ConfigLoaderBuilder.() -> Unit,
    private val load: ConfigLoader.() -> ConfigResult<T>,
    private val initialLoader: ConfigLoader,
) {
    private val state = MutableStateFlow(initial)
    private val failureEvents = MutableSharedFlow<Throwable>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** The latest valid config. Collect it to react to updates. */
    public val config: StateFlow<T> = state.asStateFlow()

    /** The latest valid config, the same as `config.value`. */
    public val current: T get() = state.value

    /**
     * Errors from reloads that failed and were skipped: a [ConfigException] listing the
     * [ConfigException.errors] for invalid values, or whatever a source threw (a missing file,
     * say). Nothing is replayed to late collectors.
     */
    public val failures: SharedFlow<Throwable> = failureEvents.asSharedFlow()

    /**
     * Loads the config again. Publishes it and returns success if it is valid; otherwise keeps
     * the current config, emits the error to [failures] and returns it as a failure.
     */
    public fun reload(): Result<T> {
        val config = try {
            ConfigLoader(sources).load().getOrThrow()
        } catch (e: Exception) {
            failureEvents.tryEmit(e)
            return Result.failure(e)
        }
        state.value = config
        return Result.success(config)
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
     * Reloads each time one of the [ReloadableSource]s used for the first load reports a change.
     * Keep such sources in a variable and add that same instance in the `sources` block, so the
     * watched source is the one that is read.
     */
    public fun watch(scope: CoroutineScope): Job =
        reloadOn(scope, initialLoader.sources.filterIsInstance<ReloadableSource>().map { it.changes }.merge())

    public companion object {
        /**
         * Loads the config for the first time.
         *
         * @param load reads the config from a loader, usually the generated `load<Spec>()`.
         * @param sources declares the sources, and runs again on every reload.
         * @throws ConfigException if the first load fails, since there is no last good config yet.
         */
        public operator fun <T : Any> invoke(
            load: ConfigLoader.() -> ConfigResult<T>,
            sources: ConfigLoaderBuilder.() -> Unit,
        ): ReloadableConfig<T> {
            val loader = ConfigLoader(sources)
            return ReloadableConfig(loader.load().getOrThrow(), sources, load, loader)
        }
    }
}
