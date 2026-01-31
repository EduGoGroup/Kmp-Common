package com.edugo.test.module.core

/**
 * Sealed class for handling operation results with different states.
 *
 * This type provides a type-safe way to represent the outcome of an operation
 * that can succeed, fail, or be in progress.
 *
 * Example usage:
 * ```kotlin
 * suspend fun fetchUser(): Result<User> = catching {
 *     val user = api.getUser()
 *     Result.Success(user)
 * }
 *
 * // Or manually:
 * suspend fun fetchUserManual(): Result<User> {
 *     return try {
 *         val user = api.getUser()
 *         Result.Success(user)
 *     } catch (e: Exception) {
 *         Result.Failure(e.message ?: "Failed to fetch user")
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
     * **Design Note**: This class uses String as the error type for simplicity
     * and to avoid coupling with platform-specific exception types. Use the [catching]
     * helper function to automatically convert exceptions to error messages.
     *
     * @property error A human-readable error message describing what went wrong
     */
    data class Failure(val error: String) : Result<Nothing>() {
        /**
         * Returns a user-safe error message.
         *
         * Since error is already a String, this simply returns it.
         * This method is kept for API compatibility and future extensibility.
         *
         * @return The error message
         */
        fun getSafeMessage(): String = error
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
    is Result.Failure -> this
    is Result.Loading -> this
}

/**
 * Executes the given block and wraps any thrown exception into a [Result.Failure].
 *
 * This helper function automatically catches exceptions and converts them to
 * Result.Failure with the exception message. If the exception has no message,
 * a generic error message is used.
 *
 * Example:
 * ```kotlin
 * suspend fun fetchData(): Result<Data> = catching {
 *     val data = api.fetch() // might throw
 *     Result.Success(data)
 * }
 * ```
 *
 * @param block The block of code to execute that may throw an exception
 * @return The result from the block, or Result.Failure if an exception was thrown
 */
inline fun <T> catching(block: () -> Result<T>): Result<T> {
    return try {
        block()
    } catch (e: Throwable) {
        Result.Failure(e.message ?: "An error occurred")
    }
}
