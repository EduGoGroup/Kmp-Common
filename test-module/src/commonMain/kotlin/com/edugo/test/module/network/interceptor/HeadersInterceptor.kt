package com.edugo.test.module.network.interceptor

import io.ktor.client.request.*
import io.ktor.http.*

/**
 * Interceptor que agrega headers comunes a todos los requests.
 *
 * Ejemplo:
 * ```kotlin
 * val headersInterceptor = HeadersInterceptor.builder()
 *     .userAgent("EduGo-App/1.0.0")
 *     .acceptLanguage("es-ES")
 *     .custom("X-Client-Version", "1.0.0")
 *     .build()
 * ```
 */
class HeadersInterceptor private constructor(
    private val headers: Map<String, String>
) : Interceptor {

    override val order: Int = 10 // Ejecutar temprano

    override suspend fun interceptRequest(request: HttpRequestBuilder) {
        headers.forEach { (key, value) ->
            // No sobrescribir si ya existe
            if (!request.headers.contains(key)) {
                request.header(key, value)
            }
        }
    }

    class Builder {
        private val headers = mutableMapOf<String, String>()

        fun userAgent(value: String) = apply {
            headers[HttpHeaders.UserAgent] = value
        }

        fun acceptLanguage(value: String) = apply {
            headers[HttpHeaders.AcceptLanguage] = value
        }

        fun accept(value: String) = apply {
            headers[HttpHeaders.Accept] = value
        }

        fun contentType(value: String) = apply {
            headers[HttpHeaders.ContentType] = value
        }

        fun custom(key: String, value: String) = apply {
            headers[key] = value
        }

        fun headers(map: Map<String, String>) = apply {
            headers.putAll(map)
        }

        fun build() = HeadersInterceptor(headers.toMap())
    }

    companion object {
        fun builder() = Builder()

        /**
         * Interceptor con headers por defecto para API JSON.
         */
        fun jsonDefaults(userAgent: String = "EduGo-KMP/1.0"): HeadersInterceptor {
            return builder()
                .userAgent(userAgent)
                .accept("application/json")
                .contentType("application/json; charset=utf-8")
                .build()
        }
    }
}
