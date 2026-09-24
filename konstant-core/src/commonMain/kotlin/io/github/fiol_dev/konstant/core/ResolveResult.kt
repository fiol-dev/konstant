package io.github.fiol_dev.konstant.core

public sealed class ResolveResult<out T> {
    public data class Success<T>(val value: T) : ResolveResult<T>()
    public data class Error(val error: ConfigError) : ResolveResult<Nothing>()

    public fun onError(action: (ConfigError) -> Unit): ResolveResult<T> {
        if (this is Error) action(error)
        return this
    }

    public fun getOrNull(): T? = when (this) {
        is Success -> value
        is Error -> null
    }

    public fun getOrElse(fallback: @UnsafeVariance T): @UnsafeVariance T = when (this) {
        is Success -> value
        is Error -> fallback
    }
}
