package com.sage.app.data.remote

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(
        val message: String,
        val statusCode: Int? = null,
        val isInvalidKey: Boolean = false,
        val isQuotaExceeded: Boolean = false,
        val isNetworkError: Boolean = false
    ) : ApiResult<Nothing>()
}
