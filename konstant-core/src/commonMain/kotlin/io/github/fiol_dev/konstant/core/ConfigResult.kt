package io.github.fiol_dev.konstant.core

public sealed class ConfigResult<out T> {
    public data class Success<T>(val value: T) : ConfigResult<T>()
    public data class Failure(val errors: List<ConfigError>) : ConfigResult<Nothing>()

    public fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw ConfigException(errors)
    }

    public fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    public fun getOrElse(fallback: @UnsafeVariance T): @UnsafeVariance T = when (this) {
        is Success -> value
        is Failure -> fallback
    }

    public fun <R> map(transform: (T) -> R): ConfigResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    public fun onFailure(action: (List<ConfigError>) -> Unit): ConfigResult<T> {
        if (this is Failure) action(errors)
        return this
    }
}
