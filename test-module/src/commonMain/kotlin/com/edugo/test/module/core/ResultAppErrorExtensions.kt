package com.edugo.test.module.core

/**
 * Extensiones de integración entre Result<T> y AppError.
 *
 * Este archivo implementa el puente entre el sistema de Result<T> existente
 * (que usa String para errores) y el sistema AppError (que proporciona errores
 * tipados con código, causa y metadatos).
 *
 * ## Estrategia de Integración (Opción B)
 *
 * En lugar de modificar Result.Failure para usar AppError directamente (lo cual
 * sería un breaking change masivo), estas extensiones proporcionan conversiones
 * bidireccionales que mantienen la compatibilidad con el código existente mientras
 * permiten trabajar con AppError cuando se necesita.
 *
 * ## Uso Típico
 *
 * ### Conversión de AppError a Result.Failure
 * ```kotlin
 * val appError = AppError.fromCode(ErrorCode.NETWORK_TIMEOUT, "Request timed out")
 * val result: Result<User> = appError.toResult()
 * // result es Result.Failure("Request timed out")
 * ```
 *
 * ### Conversión de Result.Failure a AppError
 * ```kotlin
 * val result: Result<User> = Result.Failure("User not found")
 * val appError = result.toAppError(ErrorCode.BUSINESS_RESOURCE_NOT_FOUND)
 * // appError contiene code, message, y puede tener cause/details
 * ```
 *
 * ### Creación directa de Result con AppError
 * ```kotlin
 * fun fetchUser(id: String): Result<User> {
 *     return try {
 *         val user = repository.find(id) ?: return resultFailure(
 *             ErrorCode.BUSINESS_RESOURCE_NOT_FOUND,
 *             "User with id $id not found",
 *             mapOf("userId" to id)
 *         )
 *         Result.Success(user)
 *     } catch (e: Exception) {
 *         resultFailureFrom(e, ErrorCode.SYSTEM_DATABASE_ERROR)
 *     }
 * }
 * ```
 *
 * ## Trade-offs
 *
 * **Ventajas:**
 * - ✅ No rompe código existente
 * - ✅ Permite adopción gradual de AppError
 * - ✅ Mantiene simplicidad de Result.Failure para casos simples
 *
 * **Desventajas:**
 * - ⚠️ Pierde información rica de AppError al convertir a Result.Failure
 * - ⚠️ Requiere conversión explícita cuando se necesita AppError
 * - ⚠️ Dos formas de representar errores pueden causar confusión
 */

// ============================================================================
// EXTENSIONES: AppError -> Result
// ============================================================================

/**
 * Convierte un AppError en Result.Failure.
 *
 * Esta conversión extrae solo el mensaje del AppError, perdiendo el código,
 * causa y detalles. Úsala cuando necesites interoperar con código que espera
 * Result<T> y no necesita la información rica del error.
 *
 * **Información perdida:**
 * - ErrorCode
 * - Throwable cause
 * - Map<String, String> details
 * - timestamp
 *
 * Si necesitas preservar esta información, considera mantener el AppError
 * separado o usar un wrapper.
 *
 * Ejemplo:
 * ```kotlin
 * val appError = AppError.notFound("User not found", mapOf("userId" to "123"))
 * val result: Result<User> = appError.toResult()
 * // result.error == "User not found"
 * // (se pierden userId y ErrorCode.BUSINESS_RESOURCE_NOT_FOUND)
 * ```
 *
 * @return Result.Failure con el mensaje del AppError
 */
fun <T> AppError.toResult(): Result<T> = Result.Failure(this.message)

/**
 * Convierte un AppError en Result.Failure con mensaje enriquecido.
 *
 * Esta variante incluye el código de error en el mensaje para preservar
 * algo de contexto al convertir a Result.Failure.
 *
 * Formato del mensaje: `"[ERROR_CODE] mensaje original"`
 *
 * Ejemplo:
 * ```kotlin
 * val appError = AppError.timeout("Request timed out")
 * val result: Result<User> = appError.toResultWithCode()
 * // result.error == "[NETWORK_TIMEOUT] Request timed out"
 * ```
 *
 * @return Result.Failure con mensaje que incluye el código de error
 */
fun <T> AppError.toResultWithCode(): Result<T> =
    Result.Failure("[${this.code.name}] ${this.message}")

/**
 * Convierte un AppError en Result.Failure con mensaje completo.
 *
 * Esta variante incluye toda la información disponible en el mensaje
 * para minimizar pérdida de contexto.
 *
 * Formato: `"[ERROR_CODE] mensaje (code: 1000, details: {...})"`
 *
 * Ejemplo:
 * ```kotlin
 * val appError = AppError.network(
 *     cause = SocketTimeoutException("timeout"),
 *     mapOf("endpoint" to "/api/users")
 * )
 * val result: Result<User> = appError.toResultWithDetails()
 * // result.error incluye code, detalles, y causa
 * ```
 *
 * @return Result.Failure con mensaje enriquecido con toda la información
 */
fun <T> AppError.toResultWithDetails(): Result<T> {
    val detailsStr = if (details.isNotEmpty()) {
        ", details: ${details.entries.joinToString { "${it.key}=${it.value}" }}"
    } else {
        ""
    }

    val causeStr = cause?.let { ", cause: ${it::class.simpleName}: ${it.message}" } ?: ""

    return Result.Failure(
        "[${code.name}] $message (code: ${code.code}$detailsStr$causeStr)"
    )
}

// ============================================================================
// EXTENSIONES: Result.Failure -> AppError
// ============================================================================

/**
 * Convierte un Result.Failure en AppError.
 *
 * Dado que Result.Failure solo contiene un String, necesitas proporcionar
 * el ErrorCode apropiado. Esta función crea un AppError sin causa ni detalles.
 *
 * Ejemplo:
 * ```kotlin
 * val result: Result<User> = Result.Failure("User not found")
 * val appError = result.toAppError(ErrorCode.BUSINESS_RESOURCE_NOT_FOUND)
 * // appError.code == ErrorCode.BUSINESS_RESOURCE_NOT_FOUND
 * // appError.message == "User not found"
 * // appError.cause == null
 * // appError.details == emptyMap()
 * ```
 *
 * @param code El código de error apropiado para categorizar este error
 * @param details Detalles adicionales opcionales para enriquecer el error
 * @return AppError con el código especificado y el mensaje del Failure
 */
fun Result.Failure.toAppError(
    code: ErrorCode = ErrorCode.SYSTEM_UNKNOWN_ERROR,
    details: Map<String, String> = emptyMap()
): AppError {
    return AppError.fromCode(
        code = code,
        customMessage = this.error,
        details = details
    )
}

/**
 * Convierte un Result.Failure en AppError con causa.
 *
 * Úsala cuando tengas acceso al Throwable original que causó el error.
 *
 * Ejemplo:
 * ```kotlin
 * fun processData(): Result<Data> {
 *     return try {
 *         // ... operación que puede fallar
 *         Result.Success(data)
 *     } catch (e: Exception) {
 *         val failure = Result.Failure(e.message ?: "Processing failed")
 *         // Más tarde, si necesitas AppError:
 *         val appError = failure.toAppErrorWithCause(
 *             code = ErrorCode.SYSTEM_INTERNAL_ERROR,
 *             cause = e
 *         )
 *         failure
 *     }
 * }
 * ```
 *
 * @param code El código de error apropiado
 * @param cause La excepción original que causó este error
 * @param details Detalles adicionales opcionales
 * @return AppError con código, mensaje, causa y detalles
 */
fun Result.Failure.toAppErrorWithCause(
    code: ErrorCode,
    cause: Throwable,
    details: Map<String, String> = emptyMap()
): AppError {
    return AppError(
        code = code,
        message = this.error,
        detailsInternal = details,
        cause = cause
    )
}

// ============================================================================
// FACTORY FUNCTIONS: Crear Result.Failure desde AppError directamente
// ============================================================================

/**
 * Crea un Result.Failure desde un ErrorCode y mensaje.
 *
 * Esta es una función de conveniencia que internamente crea un AppError
 * y lo convierte a Result.Failure, pero con una sintaxis más concisa.
 *
 * Ejemplo:
 * ```kotlin
 * fun findUser(id: String): Result<User> {
 *     val user = repository.find(id)
 *     return if (user != null) {
 *         Result.Success(user)
 *     } else {
 *         resultFailure(
 *             ErrorCode.BUSINESS_RESOURCE_NOT_FOUND,
 *             "User not found",
 *             mapOf("userId" to id)
 *         )
 *     }
 * }
 * ```
 *
 * @param code Código de error
 * @param message Mensaje descriptivo del error
 * @param details Detalles adicionales opcionales
 * @return Result.Failure con el mensaje del error
 */
fun <T> resultFailure(
    code: ErrorCode,
    message: String,
    details: Map<String, String> = emptyMap()
): Result<T> {
    val appError = AppError.fromCode(code, message, details)
    return appError.toResult()
}

/**
 * Crea un Result.Failure desde una excepción.
 *
 * Esta función crea un AppError desde la excepción y lo convierte a Result.Failure.
 * Útil para manejo de errores en bloques try-catch.
 *
 * Ejemplo:
 * ```kotlin
 * fun fetchData(): Result<Data> {
 *     return try {
 *         val data = api.fetch()
 *         Result.Success(data)
 *     } catch (e: IOException) {
 *         resultFailureFrom(e, ErrorCode.NETWORK_NO_CONNECTION)
 *     } catch (e: Exception) {
 *         resultFailureFrom(e, ErrorCode.SYSTEM_UNKNOWN_ERROR)
 *     }
 * }
 * ```
 *
 * @param exception La excepción capturada
 * @param code Código de error apropiado para categorizar el error
 * @param details Detalles adicionales opcionales
 * @return Result.Failure con el mensaje de la excepción
 */
fun <T> resultFailureFrom(
    exception: Throwable,
    code: ErrorCode = ErrorCode.SYSTEM_UNKNOWN_ERROR,
    details: Map<String, String> = emptyMap()
): Result<T> {
    val appError = AppError.fromException(exception, code, details)
    return appError.toResult()
}

/**
 * Crea un Result.Failure desde una excepción con mensaje enriquecido.
 *
 * Similar a [resultFailureFrom] pero incluye el ErrorCode en el mensaje.
 *
 * @param exception La excepción capturada
 * @param code Código de error apropiado
 * @param details Detalles adicionales opcionales
 * @return Result.Failure con mensaje enriquecido
 */
fun <T> resultFailureFromWithCode(
    exception: Throwable,
    code: ErrorCode = ErrorCode.SYSTEM_UNKNOWN_ERROR,
    details: Map<String, String> = emptyMap()
): Result<T> {
    val appError = AppError.fromException(exception, code, details)
    return appError.toResultWithCode()
}

// ============================================================================
// EXTENSIONES AVANZADAS: Trabajar con Result preservando AppError
// ============================================================================

/**
 * Ejecuta una operación que retorna Result<T> y captura errores como AppError.
 *
 * Esta función es útil cuando quieres trabajar con Result<T> pero necesitas
 * acceso a AppError para logging o tracking.
 *
 * Ejemplo:
 * ```kotlin
 * val result = catchingWithAppError(ErrorCode.SYSTEM_DATABASE_ERROR) {
 *     val data = database.query()
 *     Result.Success(data)
 * }
 * // Si falla, puedes obtener el AppError para logging
 * ```
 *
 * @param defaultCode Código de error a usar si la operación lanza una excepción
 * @param details Detalles adicionales para el error
 * @param block Operación que retorna Result<T>
 * @return Result<T> de la operación, o Result.Failure si lanza excepción
 */
inline fun <T> catchingWithAppError(
    defaultCode: ErrorCode = ErrorCode.SYSTEM_UNKNOWN_ERROR,
    details: Map<String, String> = emptyMap(),
    block: () -> Result<T>
): Result<T> {
    return try {
        block()
    } catch (e: Throwable) {
        resultFailureFrom(e, defaultCode, details)
    }
}

/**
 * Transforma un Result<T> aplicando una función que puede lanzar excepciones.
 *
 * Similar a flatMap, pero captura excepciones y las convierte en AppError.
 *
 * Ejemplo:
 * ```kotlin
 * val userResult: Result<User> = fetchUser()
 * val postsResult = userResult.flatMapCatching(ErrorCode.SYSTEM_DATABASE_ERROR) { user ->
 *     val posts = repository.getPostsByUser(user.id) // puede lanzar excepción
 *     Result.Success(posts)
 * }
 * ```
 *
 * @param errorCode Código de error si la transformación falla
 * @param details Detalles adicionales para el error
 * @param transform Función de transformación que puede lanzar excepciones
 * @return Result transformado o Result.Failure si hay error
 */
inline fun <T, R> Result<T>.flatMapCatching(
    errorCode: ErrorCode = ErrorCode.SYSTEM_UNKNOWN_ERROR,
    details: Map<String, String> = emptyMap(),
    transform: (T) -> Result<R>
): Result<R> {
    return when (this) {
        is Result.Success -> {
            try {
                transform(this.data)
            } catch (e: Throwable) {
                resultFailureFrom(e, errorCode, details)
            }
        }
        is Result.Failure -> this
        is Result.Loading -> this
    }
}
