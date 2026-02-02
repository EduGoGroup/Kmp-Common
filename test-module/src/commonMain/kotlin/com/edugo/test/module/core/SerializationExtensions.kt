package com.edugo.test.module.core

import kotlinx.serialization.SerializationException

/**
 * Extensiones para manejo de serialización con Result<T> y AppError.
 *
 * Este archivo proporciona helpers para trabajar con kotlinx.serialization
 * de forma segura, capturando excepciones y convirtiéndolas automáticamente
 * en Result<T> con AppError tipado.
 *
 * ## Excepciones Capturadas
 *
 * Las funciones en este archivo capturan las siguientes excepciones de kotlinx.serialization:
 * - `SerializationException`: Error general de serialización/deserialización
 * - `MissingFieldException`: Campo requerido faltante en JSON
 * - `UnknownKeyException`: Clave desconocida en JSON (modo strict)
 * - Cualquier otra subclase de SerializationException
 *
 * ## Uso Típico
 *
 * ### Deserialización segura
 * ```kotlin
 * @Serializable
 * data class User(val id: String, val name: String)
 *
 * val jsonString = """{"id": "123", "name": "John"}"""
 * val result: Result<User> = catchSerialization {
 *     Json.decodeFromString<User>(jsonString)
 * }
 *
 * when (result) {
 *     is Result.Success -> println("User: ${result.data}")
 *     is Result.Failure -> println("Error: ${result.error}")
 * }
 * ```
 *
 * ### Serialización segura
 * ```kotlin
 * val user = User(id = "123", name = "John")
 * val result: Result<String> = catchSerialization {
 *     Json.encodeToString(user)
 * }
 * ```
 *
 * ### Con detalles adicionales
 * ```kotlin
 * val result = catchSerializationWithDetails(
 *     details = mapOf("source" to "api", "endpoint" to "/users")
 * ) {
 *     Json.decodeFromString<User>(jsonString)
 * }
 * ```
 */

/**
 * Ejecuta un bloque de código que puede lanzar SerializationException
 * y lo envuelve en un Result<T>.
 *
 * Esta función captura todas las excepciones de kotlinx.serialization y las
 * convierte automáticamente en Result.Failure con un mensaje descriptivo.
 *
 * **Excepciones capturadas:**
 * - SerializationException y todas sus subclases
 * - MissingFieldException (campo requerido faltante)
 * - UnknownKeyException (clave no esperada en modo strict)
 *
 * **Mapeo de errores:**
 * - Las excepciones se convierten en Result.Failure con el mensaje de la excepción
 * - Si se necesita AppError con código tipado, usar [catchSerializationAsAppError]
 *
 * Ejemplo:
 * ```kotlin
 * val result: Result<User> = catchSerialization {
 *     Json.decodeFromString<User>("""{"id": "1", "name": "John"}""")
 * }
 * // Si el JSON es inválido o falta un campo, result será Result.Failure
 * ```
 *
 * @param T El tipo esperado del resultado de la deserialización
 * @param block El bloque de código que realiza la serialización/deserialización
 * @return Result.Success<T> si la operación fue exitosa, Result.Failure si hubo error
 */
inline fun <T> catchSerialization(block: () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: SerializationException) {
        Result.Failure(e.message ?: "Serialization error occurred")
    } catch (e: Exception) {
        // Captura cualquier otra excepción inesperada
        Result.Failure("Unexpected error during serialization: ${e.message}")
    }
}

/**
 * Ejecuta un bloque de serialización y retorna Result<T> con AppError tipado.
 *
 * Esta variante convierte SerializationException en AppError con el código
 * ErrorCode.SYSTEM_SERIALIZATION_ERROR, preservando información adicional
 * como causa y detalles.
 *
 * **Ventaja sobre catchSerialization:**
 * - Retorna AppError con código tipado
 * - Preserva la excepción original como `cause`
 * - Permite agregar detalles de contexto
 *
 * Ejemplo:
 * ```kotlin
 * val result: Result<User> = catchSerializationAsAppError(
 *     details = mapOf("endpoint" to "/api/users", "method" to "GET")
 * ) {
 *     Json.decodeFromString<User>(responseBody)
 * }
 *
 * when (result) {
 *     is Result.Success -> handleUser(result.data)
 *     is Result.Failure -> {
 *         // Convertir a AppError para análisis detallado
 *         val appError = result.toAppError(ErrorCode.SYSTEM_SERIALIZATION_ERROR)
 *         logger.error("Serialization failed", appError)
 *     }
 * }
 * ```
 *
 * @param T El tipo esperado del resultado
 * @param details Detalles adicionales para enriquecer el error
 * @param block El bloque de código que realiza la serialización
 * @return Result<T> envuelto, con error tipado si falla
 */
inline fun <T> catchSerializationAsAppError(
    details: Map<String, String> = emptyMap(),
    block: () -> T
): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: SerializationException) {
        val appError = AppError.fromException(
            exception = e,
            code = ErrorCode.SYSTEM_SERIALIZATION_ERROR,
            details = details
        )
        appError.toResult()
    } catch (e: Exception) {
        val appError = AppError.fromException(
            exception = e,
            code = ErrorCode.SYSTEM_UNKNOWN_ERROR,
            details = details + ("context" to "serialization")
        )
        appError.toResult()
    }
}

/**
 * Variante de catchSerialization con detalles adicionales.
 *
 * Permite agregar contexto al error sin necesidad de crear un AppError explícito.
 * El mensaje de error incluirá los detalles proporcionados.
 *
 * Ejemplo:
 * ```kotlin
 * val result = catchSerializationWithDetails(
 *     mapOf("file" to "config.json", "line" to "45")
 * ) {
 *     Json.decodeFromString<Config>(jsonContent)
 * }
 * // Si falla, el error incluirá: "Serialization error: [details: file=config.json, line=45]"
 * ```
 *
 * @param details Map de contexto para incluir en el mensaje de error
 * @param block Bloque de código de serialización
 * @return Result<T>
 */
inline fun <T> catchSerializationWithDetails(
    details: Map<String, String>,
    block: () -> T
): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: SerializationException) {
        val detailsStr = details.entries.joinToString(", ") { "${it.key}=${it.value}" }
        Result.Failure("${e.message ?: "Serialization error"} [details: $detailsStr]")
    } catch (e: Exception) {
        val detailsStr = details.entries.joinToString(", ") { "${it.key}=${it.value}" }
        Result.Failure("Unexpected error: ${e.message} [details: $detailsStr]")
    }
}

/**
 * Helper para deserializar JSON de forma segura con un deserializador específico.
 *
 * Esta es una función de conveniencia para el caso común de deserializar JSON.
 *
 * Ejemplo:
 * ```kotlin
 * import kotlinx.serialization.json.Json
 *
 * val result: Result<User> = safeDecodeFromString(
 *     json = """{"id": "1", "name": "John"}""",
 *     deserializer = User.serializer()
 * )
 * ```
 *
 * @param json String JSON a deserializar
 * @param deserializer El deserializador de kotlinx.serialization
 * @return Result<T> con el objeto deserializado o error
 */
inline fun <reified T> safeDecodeFromString(json: String): Result<T> {
    return catchSerialization {
        kotlinx.serialization.json.Json.decodeFromString<T>(json)
    }
}

/**
 * Helper para serializar un objeto a JSON de forma segura.
 *
 * Ejemplo:
 * ```kotlin
 * val user = User(id = "1", name = "John")
 * val result: Result<String> = safeEncodeToString(user)
 *
 * when (result) {
 *     is Result.Success -> sendToApi(result.data)
 *     is Result.Failure -> handleError(result.error)
 * }
 * ```
 *
 * @param value El objeto a serializar
 * @return Result<String> con el JSON o error
 */
inline fun <reified T> safeEncodeToString(value: T): Result<String> {
    return catchSerialization {
        kotlinx.serialization.json.Json.encodeToString(value)
    }
}
