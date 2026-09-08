package com.screeniq.core.common

/**
 * Standard typed result monad used across ScreenIQ pipeline stages when
 * extended status / metadata is required beyond Kotlin's standard Result.
 */
sealed class ScreenIqResult<out T> {
    data class Success<out T>(val data: T) : ScreenIqResult<T>()
    data class Failure(val throwable: Throwable, val message: String? = null) : ScreenIqResult<Nothing>()
    data object Loading : ScreenIqResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Failure -> throw throwable
        is Loading -> throw IllegalStateException("Cannot get value while in Loading state")
    }

    inline fun onSuccess(action: (T) -> Unit): ScreenIqResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onFailure(action: (throwable: Throwable, message: String?) -> Unit): ScreenIqResult<T> {
        if (this is Failure) action(throwable, message)
        return this
    }
}
