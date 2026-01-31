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
 * Chains multiple Result-returning operations together.
 *
 * This function allows composing operations that each return a Result, flattening
 * the nested Result structure. Only executes the transform if this is a Success.
 *
 * Example:
 * ```kotlin
 * val userResult: Result<User> = fetchUser()
 * val postsResult: Result<List<Post>> = userResult.flatMap { user ->
 *     fetchUserPosts(user.id)
 * }
 * ```
 *
 * @param transform Function that transforms the success data into another Result
 * @return The result from the transform, or the original Failure/Loading state
 */
inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
    is Result.Success -> transform(data)
    is Result.Failure -> this
    is Result.Loading -> this
}

/**
 * Transforms the error message of a Failure result.
 *
 * This function allows modifying the error message without affecting Success or Loading states.
 * Useful for adding context or translating error messages.
 *
 * Example:
 * ```kotlin
 * val result: Result<User> = fetchUser()
 *     .mapError { error -> "Failed to fetch user: $error" }
 * ```
 *
 * @param transform Function to transform the error message
 * @return A new Result with the transformed error, or the original Success/Loading state
 */
inline fun <T> Result<T>.mapError(transform: (String) -> String): Result<T> = when (this) {
    is Result.Success -> this
    is Result.Failure -> Result.Failure(transform(error))
    is Result.Loading -> this
}

/**
 * Folds the Result into a single value by applying the appropriate function.
 *
 * This provides a functional pattern-matching approach to handle both success and failure cases.
 *
 * Example:
 * ```kotlin
 * val message: String = userResult.fold(
 *     onSuccess = { user -> "Welcome, ${user.name}!" },
 *     onFailure = { error -> "Error: $error" }
 * )
 * ```
 *
 * @param onSuccess Function to apply if this is a Success
 * @param onFailure Function to apply if this is a Failure
 * @return The result of applying the appropriate function, or null if Loading
 */
inline fun <T, R> Result<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (String) -> R
): R? = when (this) {
    is Result.Success -> onSuccess(data)
    is Result.Failure -> onFailure(error)
    is Result.Loading -> null
}

/**
 * Returns the success value or a default value if this is a Failure or Loading.
 *
 * Example:
 * ```kotlin
 * val userName: String = userResult.getOrElse { "Guest" }
 * ```
 *
 * @param default Function that provides the default value
 * @return The success data or the default value
 */
inline fun <T> Result<T>.getOrElse(default: () -> T): T = when (this) {
    is Result.Success -> data
    is Result.Failure -> default()
    is Result.Loading -> default()
}

/**
 * Returns the success value or null if this is a Failure or Loading.
 *
 * Example:
 * ```kotlin
 * val user: User? = userResult.getOrNull()
 * ```
 *
 * @return The success data or null
 */
fun <T> Result<T>.getOrNull(): T? = when (this) {
    is Result.Success -> data
    is Result.Failure -> null
    is Result.Loading -> null
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
