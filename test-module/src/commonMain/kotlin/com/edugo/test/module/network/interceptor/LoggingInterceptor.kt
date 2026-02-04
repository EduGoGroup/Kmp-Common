package com.edugo.test.module.network.interceptor

import com.edugo.test.module.network.NetworkLogger
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * Interceptor que loguea requests/responses usando NetworkLogger.
 *
 * Permite registrar métricas de tiempo y estado de requests para
 * debugging y monitoreo.
 *
 * Ejemplo:
 * ```kotlin
 * val client = EduGoHttpClientBuilder()
 *     .interceptor(LoggingInterceptor.Verbose)
 *     .build()
 * ```
 *
 * @param logger Logger a usar (default: NetworkLogger.Default)
 * @param logHeaders Si se deben loguear los headers (default: false)
 * @param logBody Si se debe loguear el body (default: false)
 */
class LoggingInterceptor(
    private val logger: NetworkLogger = NetworkLogger.Default,
    private val logHeaders: Boolean = false,
    private val logBody: Boolean = false
) : Interceptor {

    override val order: Int = 100 // Ejecutar último en request, primero en response

    override suspend fun interceptRequest(request: HttpRequestBuilder) {
        val headers = if (logHeaders) {
            request.headers.entries()
                .associate { (key, values) -> key to (values.firstOrNull() ?: "") }
        } else {
            emptyMap()
        }

        logger.logRequest(
            method = request.method,
            url = request.url.buildString(),
            headers = headers
        )
    }

    override suspend fun interceptResponse(response: HttpResponse) {
        val headers = if (logHeaders) {
            response.headers.entries()
                .associate { (key, values) -> key to (values.firstOrNull() ?: "") }
        } else {
            emptyMap()
        }

        logger.logResponse(
            statusCode = response.status.value,
            url = response.request.url.toString(),
            headers = headers
        )
    }

    override suspend fun onError(request: HttpRequestBuilder, exception: Throwable) {
        logger.logError(request.url.buildString(), exception)
    }

    companion object {
        /** Logging mínimo (solo URL y status) */
        val Minimal = LoggingInterceptor(logHeaders = false, logBody = false)

        /** Logging completo (headers incluidos) */
        val Verbose = LoggingInterceptor(logHeaders = true, logBody = true)
    }
}
