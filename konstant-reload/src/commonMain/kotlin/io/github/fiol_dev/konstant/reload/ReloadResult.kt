package io.github.fiol_dev.konstant.reload

/** The outcome of [ReloadableConfig.reload]. */
public sealed class ReloadResult<out T : Any> {
    /** The config that is current after the reload. */
    public abstract val config: T

    /** The reload produced a different valid config, which is now published. */
    public data class Updated<T : Any>(override val config: T) : ReloadResult<T>()

    /** The reload produced a valid config equal to the current one; nothing was published. */
    public data class Unchanged<T : Any>(override val config: T) : ReloadResult<T>()

    /**
     * The reload failed and the last good [config] was kept. [error] is a
     * [io.github.fiol_dev.konstant.core.ConfigException] for invalid values, or whatever a
     * source threw (a missing file, say).
     */
    public data class Failed<T : Any>(override val config: T, val error: Throwable) : ReloadResult<T>()
}
