package io.github.fiol_dev.konstant.core

/** The outcome of resolving one field. Used by generated code. */
@InternalKonstantApi
public sealed class ResolveResult<out T> {
    /** The field resolved to [value]. */
    public class Success<T>(public val value: T) : ResolveResult<T>()

    /** The field failed with [error]. */
    public class Error(public val error: ConfigError) : ResolveResult<Nothing>()

    /** Runs [action] with the error if this is an [Error], then returns this result. */
    public fun onError(action: (ConfigError) -> Unit): ResolveResult<T> {
        if (this is Error) action(error)
        return this
    }

    /** The value, or null on error. */
    public fun getOrNull(): T? = when (this) {
        is Success -> value
        is Error -> null
    }

    /** The value, or [fallback] on error. */
    public fun getOrElse(fallback: @UnsafeVariance T): @UnsafeVariance T = when (this) {
        is Success -> value
        is Error -> fallback
    }
}
