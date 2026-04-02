package io.github.fioldev.konfigure.core

sealed class ResolveResult<out T> {
    data class Success<T>(val value: T) : ResolveResult<T>()
    data class Error(val error: ConfigError) : ResolveResult<Nothing>()

    fun onError(action: (ConfigError) -> Unit): ResolveResult<T> {
        if (this is Error) action(error)
        return this
    }

    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Error -> null
    }

    fun getOrElse(fallback: @UnsafeVariance T): @UnsafeVariance T = when (this) {
        is Success -> value
        is Error -> fallback
    }
}
