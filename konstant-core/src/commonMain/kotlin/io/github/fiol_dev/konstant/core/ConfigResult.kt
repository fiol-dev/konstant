package io.github.fiol_dev.konstant.core

/**
 * The outcome of loading a config: [Success] with the value, or [Failure] with every error
 * found. Loading does not stop at the first bad key, so all problems are reported at once.
 */
public sealed class ConfigResult<out T> {
    /** The config loaded and passed validation. */
    public data class Success<T>(val value: T) : ConfigResult<T>()

    /** The config could not be loaded; [errors] lists each failed key. */
    public data class Failure(val errors: List<ConfigError>) : ConfigResult<Nothing>()

    /** The value, or throws [ConfigException] with all the errors. */
    public fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw ConfigException(errors)
    }

    /** The value, or null on failure. */
    public fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    /** The value, or [fallback] on failure. */
    public fun getOrElse(fallback: @UnsafeVariance T): @UnsafeVariance T = when (this) {
        is Success -> value
        is Failure -> fallback
    }

    /** Transforms the value of a [Success]; a [Failure] is returned unchanged. */
    public fun <R> map(transform: (T) -> R): ConfigResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    /** Runs [action] with the errors if this is a [Failure], then returns this result. */
    public fun onFailure(action: (List<ConfigError>) -> Unit): ConfigResult<T> {
        if (this is Failure) action(errors)
        return this
    }
}
