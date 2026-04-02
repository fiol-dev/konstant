package io.github.fioldev.konfigure.core

sealed class ConfigResult<out T> {
    data class Success<T>(val value: T) : ConfigResult<T>()
    data class Failure(val errors: List<ConfigError>) : ConfigResult<Nothing>()

    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw ConfigException(errors)
    }

    fun <R> map(transform: (T) -> R): ConfigResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }
}
