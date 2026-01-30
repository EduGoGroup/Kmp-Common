package com.edugo.test.module.core

/**
 * Sealed class for handling operation results with different states.
 *
 * This type provides a type-safe way to represent the outcome of an operation
 * that can succeed, fail, or be in progress.
 *
 * Example usage:
 * ```kotlin
 * suspend fun fetchUser(): Result<User> {
 *     return try {
 *         val user = api.getUser()
 *         Result.Success(user)
 *     } catch (e: Exception) {
 *         Result.Error(e)
 *     }
 * }
 * ```
 */
sealed class Result<out T> {
    /**
     * Represents a successful operation with resulting data.
     *
     * @property data The successful result value
     */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * Represents a failed operation with error information.
     *
     * **⚠️ Security Note**: The `exception` field may contain sensitive information
     * in stack traces and messages. Use [getSafeMessage] when displaying errors to users
     * or transmitting error information over the network.
     *
     * @property exception The underlying exception (may contain sensitive data)
     */
    data class Error(val exception: Throwable) : Result<Nothing>() {
        /**
         * Returns a user-safe error message without sensitive details.
         *
         * This method extracts only the exception message, which is safer to display
         * than the full exception with stack traces and internal details.
         *
         * @return A sanitized error message, or a generic message if none is available
         */
        fun getSafeMessage(): String {
            return exception.message ?: "An error occurred"
        }
    }

    /**
     * Represents an operation that is currently in progress.
     *
     * Use this state to show loading indicators in the UI or to prevent
     * duplicate operations.
     */
    object Loading : Result<Nothing>()
}

/**
 * Extension function to transform the data of a successful result.
 *
 * This function applies the given transformation only if the result is [Result.Success],
 * and preserves [Result.Error] and [Result.Loading] states unchanged.
 *
 * Example:
 * ```kotlin
 * val userResult: Result<User> = fetchUser()
 * val nameResult: Result<String> = userResult.map { it.name }
 * ```
 *
 * @param transform Function to transform the success data
 * @return A new Result with the transformed data, or the original Error/Loading state
 */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
    is Result.Loading -> this
}
