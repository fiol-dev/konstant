package io.github.fiol_dev.konfigure.core

sealed class ConfigResult<out T> {
    data class Success<T>(val value: T) : ConfigResult<T>()
    data class Failure(val errors: List<ConfigError>) : ConfigResult<Nothing>()

    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw ConfigException(errors)
    }

    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    fun getOrElse(fallback: @UnsafeVariance T): @UnsafeVariance T = when (this) {
        is Success -> value
        is Failure -> fallback
    }

    fun <R> map(transform: (T) -> R): ConfigResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    fun onFailure(action: (List<ConfigError>) -> Unit): ConfigResult<T> {
        if (this is Failure) action(errors)
        return this
    }
}
