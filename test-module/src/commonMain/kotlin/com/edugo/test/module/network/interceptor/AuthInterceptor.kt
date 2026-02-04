package com.edugo.test.module.network.interceptor

import io.ktor.client.request.*
import io.ktor.http.*

/**
 * Proveedor de tokens de autenticación.
 */
interface TokenProvider {
    /**
     * Obtiene el token actual. Retorna null si no hay sesión.
     */
    suspend fun getToken(): String?

    /**
     * Refresca el token. Retorna nuevo token o null si falla.
     */
    suspend fun refreshToken(): String?

    /**
     * Indica si el token actual ha expirado.
     */
    suspend fun isTokenExpired(): Boolean
}

/**
 * Interceptor que agrega header Authorization con Bearer token.
 *
 * Ejemplo:
 * ```kotlin
 * val authInterceptor = AuthInterceptor(
 *     tokenProvider = myTokenProvider,
 *     autoRefresh = true
 * )
 * ```
 */
class AuthInterceptor(
    private val tokenProvider: TokenProvider,
    private val autoRefresh: Boolean = true,
    private val headerName: String = HttpHeaders.Authorization,
    private val tokenPrefix: String = "Bearer "
) : Interceptor {

    override val order: Int = 20 // Después de HeadersInterceptor

    override suspend fun interceptRequest(request: HttpRequestBuilder) {
        // Skip si ya tiene Authorization
        if (request.headers.contains(headerName)) {
            return
        }

        var token = tokenProvider.getToken()

        // Auto-refresh si está expirado
        if (autoRefresh && token != null && tokenProvider.isTokenExpired()) {
            token = tokenProvider.refreshToken()
        }

        token?.let {
            request.header(headerName, "$tokenPrefix$it")
        }
    }

    companion object {
        /**
         * Crea AuthInterceptor con token estático (para testing).
         */
        fun withStaticToken(token: String): AuthInterceptor {
            return AuthInterceptor(
                tokenProvider = object : TokenProvider {
                    override suspend fun getToken(): String = token
                    override suspend fun refreshToken(): String = token
                    override suspend fun isTokenExpired(): Boolean = false
                },
                autoRefresh = false
            )
        }
    }
}
