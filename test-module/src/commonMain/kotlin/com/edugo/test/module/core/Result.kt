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
 * Creates a successful Result with the given value.
 *
 * This is a convenience factory function that's cleaner than using Result.Success directly.
 *
 * Example:
 * ```kotlin
 * fun getDefaultUser(): Result<User> = success(User(id = 0, name = "Guest"))
 * ```
 *
 * @param value The success value
 * @return A Result.Success containing the value
 */
fun <T> success(value: T): Result<T> = Result.Success(value)

/**
 * Creates a failed Result with the given error message.
 *
 * This is a convenience factory function that's cleaner than using Result.Failure directly.
 *
 * Example:
 * ```kotlin
 * fun validateEmail(email: String): Result<String> {
 *     return if (email.contains("@")) success(email)
 *     else failure("Invalid email format")
 * }
 * ```
 *
 * @param error The error message
 * @return A Result.Failure with the error message
 */
fun <T> failure(error: String): Result<T> = Result.Failure(error)

/**
 * Combines two Results by applying a transformation function to both success values.
 *
 * If both Results are Success, applies the transform. If either is Failure, returns the first Failure.
 * If either is Loading, returns Loading.
 *
 * Example:
 * ```kotlin
 * val userResult: Result<User> = fetchUser()
 * val settingsResult: Result<Settings> = fetchSettings()
 * val combined: Result<UserProfile> = userResult.zip(settingsResult) { user, settings ->
 *     UserProfile(user, settings)
 * }
 * ```
 *
 * @param other The other Result to combine with
 * @param transform Function to combine both success values
 * @return A Result with the combined value, or the first error/loading encountered
 */
inline fun <A, B, R> Result<A>.zip(
    other: Result<B>,
    transform: (A, B) -> R
): Result<R> = when {
    this is Result.Loading || other is Result.Loading -> Result.Loading
    this is Result.Failure -> this
    other is Result.Failure -> other
    this is Result.Success && other is Result.Success -> Result.Success(transform(this.data, other.data))
    else -> Result.Failure("Unexpected state in zip")
}

/**
 * Combines multiple Results into a single Result containing a list of all success values.
 *
 * Returns Success with a list only if ALL results are Success.
 * Returns the first Failure encountered if any result is Failure.
 * Returns Loading if any result is Loading and none are Failure.
 *
 * Example:
 * ```kotlin
 * val results: List<Result<User>> = listOf(fetchUser1(), fetchUser2(), fetchUser3())
 * val combined: Result<List<User>> = combine(*results.toTypedArray())
 * ```
 *
 * @param results Variable number of Results to combine
 * @return A Result containing a list of all values, or the first error/loading encountered
 */
fun <T> combine(vararg results: Result<T>): Result<List<T>> {
    // Check for Failure first (highest priority)
    results.forEach { result ->
        if (result is Result.Failure) return result
    }

    // Check for Loading second
    results.forEach { result ->
        if (result is Result.Loading) return Result.Loading
    }

    // All must be Success at this point
    val values = results.map { result ->
        when (result) {
            is Result.Success -> result.data
            else -> return Result.Failure("Unexpected state in combine")
        }
    }

    return Result.Success(values)
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

/**
 * Recupera de un error aplicando una función de transformación al mensaje de error.
 *
 * Esta función permite intentar recuperarse de un Result.Failure convirtiéndolo
 * en un Result.Success con un valor por defecto o alternativo.
 *
 * **Comportamiento:**
 * - Si es Success: devuelve el mismo Success
 * - Si es Failure: aplica la función de recuperación y devuelve su resultado
 * - Si es Loading: devuelve Loading
 *
 * Ejemplo:
 * ```kotlin
 * val result: Result<User> = fetchUser()
 * val recovered = result.recover { error ->
 *     // Intentar obtener usuario de caché local
 *     getCachedUser() ?: User.guest()
 * }
 * ```
 *
 * @param recovery Función que recibe el mensaje de error y retorna un valor de recuperación
 * @return Result con el valor original si es Success, o el valor recuperado si es Failure
 */
inline fun <T> Result<T>.recover(recovery: (String) -> T): Result<T> = when (this) {
    is Result.Success -> this
    is Result.Failure -> Result.Success(recovery(this.error))
    is Result.Loading -> this
}

/**
 * Recupera de un error devolviendo un Result alternativo.
 *
 * Similar a [recover], pero la función de recuperación devuelve un Result<T>
 * en lugar de un valor directo, permitiendo que la recuperación también pueda fallar.
 *
 * Ejemplo:
 * ```kotlin
 * val result: Result<User> = fetchUserFromApi()
 * val recovered = result.recoverWith { error ->
 *     // Si falla la API, intentar desde caché (que también puede fallar)
 *     fetchUserFromCache()
 * }
 * ```
 *
 * @param recovery Función que recibe el mensaje de error y retorna un Result<T>
 * @return El Result original si es Success, o el Result de recuperación si es Failure
 */
inline fun <T> Result<T>.recoverWith(recovery: (String) -> Result<T>): Result<T> = when (this) {
    is Result.Success -> this
    is Result.Failure -> recovery(this.error)
    is Result.Loading -> this
}

/**
 * Convierte un valor nullable en un Result.
 *
 * Esta extensión simplifica el patrón común de convertir valores opcionales
 * en Result, proporcionando un mensaje de error personalizado cuando el valor es null.
 *
 * Ejemplo:
 * ```kotlin
 * val user: User? = repository.findById(id)
 * val result: Result<User> = user.toResult("User not found")
 *
 * // Equivale a:
 * val result = if (user != null) {
 *     Result.Success(user)
 * } else {
 *     Result.Failure("User not found")
 * }
 * ```
 *
 * @param errorMessage Mensaje de error a usar si el valor es null
 * @return Result.Success si el valor no es null, Result.Failure si es null
 */
fun <T : Any> T?.toResult(errorMessage: String): Result<T> {
    return if (this != null) {
        Result.Success(this)
    } else {
        Result.Failure(errorMessage)
    }
}

/**
 * Convierte un valor nullable en un Result usando un lambda para generar el error.
 *
 * Variante de [toResult] que permite crear el mensaje de error de forma lazy,
 * útil cuando el mensaje es costoso de construir.
 *
 * Ejemplo:
 * ```kotlin
 * val user: User? = repository.findById(userId)
 * val result = user.toResultOrElse {
 *     "User with ID $userId not found in database"
 * }
 * ```
 *
 * @param errorProvider Función que genera el mensaje de error si el valor es null
 * @return Result.Success si el valor no es null, Result.Failure si es null
 */
inline fun <T : Any> T?.toResultOrElse(errorProvider: () -> String): Result<T> {
    return if (this != null) {
        Result.Success(this)
    } else {
        Result.Failure(errorProvider())
    }
}

/**
 * Transforma un Result<Result<T>> (nested) en Result<T> (flattened).
 *
 * Esta función es útil cuando tienes operaciones que retornan Result<Result<T>>
 * y quieres aplanar la estructura.
 *
 * Ejemplo:
 * ```kotlin
 * val nested: Result<Result<User>> = Result.Success(fetchUser())
 * val flattened: Result<User> = nested.flatten()
 * ```
 *
 * @return Result<T> aplanado
 */
fun <T> Result<Result<T>>.flatten(): Result<T> = when (this) {
    is Result.Success -> this.data
    is Result.Failure -> this
    is Result.Loading -> this
}
