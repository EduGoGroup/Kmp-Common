package com.edugo.test.module.network

import com.edugo.test.module.core.AppError
import com.edugo.test.module.core.ErrorCode
import com.edugo.test.module.core.Result
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * Convierte HttpResponse a Result<T> con deserialización automática.
 * Mapea errores HTTP a AppError apropiados.
 *
 * @return Result.Success con datos deserializados o Result.Failure con error
 */
public suspend inline fun <reified T> HttpResponse.toResult(): Result<T> {
    return when {
        status.isSuccess() -> {
            try {
                Result.Success(body<T>())
            } catch (e: Exception) {
                Result.Failure("[${ErrorCode.SYSTEM_SERIALIZATION_ERROR}] Failed to deserialize response: ${e.message}")
            }
        }
        else -> {
            val errorCode = ErrorCode.fromHttpStatus(status.value)
            val errorBody = tryReadErrorBody()
            Result.Failure("[$errorCode] ${status.description}: $errorBody")
        }
    }
}

/**
 * Convierte HttpResponse a Result<T> con AppError estructurado.
 */
public suspend inline fun <reified T> HttpResponse.toResultWithAppError(): Result<T> {
    return when {
        status.isSuccess() -> {
            try {
                Result.Success(body<T>())
            } catch (e: Exception) {
                val appError = AppError.fromException(
                    exception = e,
                    code = ErrorCode.SYSTEM_SERIALIZATION_ERROR
                )
                Result.Failure(appError.toString())
            }
        }
        else -> {
            val errorCode = ErrorCode.fromHttpStatus(status.value)
            val errorBody = tryReadErrorBody()
            val appError = AppError.fromCode(
                code = errorCode,
                customMessage = errorBody ?: status.description
            )
            Result.Failure(appError.toString())
        }
    }
}

/**
 * Intenta leer el body de error de forma segura.
 */
public suspend fun HttpResponse.tryReadErrorBody(): String? {
    return try {
        bodyAsText().take(500) // Limitar tamaño
    } catch (e: Exception) {
        null
    }
}

/**
 * Verifica si el status code indica éxito (2xx).
 */
public fun HttpStatusCode.isSuccess(): Boolean = value in 200..299

/**
 * Verifica si el status code indica error del cliente (4xx).
 */
public fun HttpStatusCode.isClientError(): Boolean = value in 400..499

/**
 * Verifica si el status code indica error del servidor (5xx).
 */
public fun HttpStatusCode.isServerError(): Boolean = value in 500..599
