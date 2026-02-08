package com.edugo.test.module.auth.repository

import com.edugo.test.module.auth.model.LoginCredentials
import com.edugo.test.module.auth.model.LoginResponse
import com.edugo.test.module.core.ErrorCode
import com.edugo.test.module.core.Result
import com.edugo.test.module.network.EduGoHttpClient
import com.edugo.test.module.network.ExceptionMapper
import io.ktor.client.plugins.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Implementación del repositorio de autenticación usando EduGoHttpClient.
 *
 * Esta implementación realiza llamadas HTTP reales al backend de autenticación
 * usando Ktor Client envuelto en [EduGoHttpClient]. Maneja automáticamente
 * la serialización/deserialización JSON y el mapeo de errores HTTP a Result.
 *
 * ## Arquitectura
 *
 * ```
 * AuthService
 *     ↓
 * AuthRepositoryImpl (esta clase)
 *     ↓
 * EduGoHttpClient
 *     ↓
 * Ktor HttpClient
 *     ↓
 * Backend API (edu-admin)
 * ```
 *
 * ## Endpoints del Backend
 *
 * - `POST {baseUrl}/v1/auth/login` - Autenticación
 * - `POST {baseUrl}/v1/auth/logout` - Cerrar sesión
 * - `POST {baseUrl}/v1/auth/refresh` - Renovar token
 *
 * ## Manejo de Errores
 *
 * Los errores HTTP se mapean automáticamente a Result.Failure:
 *
 * | HTTP Status | ErrorCode | Descripción |
 * |-------------|-----------|-------------|
 * | 400 | VALIDATION_INVALID_INPUT | Datos inválidos |
 * | 401 | AUTH_INVALID_CREDENTIALS | Credenciales incorrectas |
 * | 403 | AUTH_FORBIDDEN | Usuario inactivo/sin permisos |
 * | 404 | BUSINESS_RESOURCE_NOT_FOUND | Usuario no encontrado |
 * | 423 | AUTH_ACCOUNT_LOCKED | Cuenta bloqueada |
 * | 500 | SYSTEM_INTERNAL_ERROR | Error del servidor |
 * | 502 | NETWORK_SERVER_ERROR | Backend no disponible |
 * | 503 | SYSTEM_SERVICE_UNAVAILABLE | Servicio temporalmente no disponible |
 *
 * ## Ejemplo de Uso
 *
 * ```kotlin
 * // Crear instancia
 * val httpClient = EduGoHttpClient.create()
 * val repository = AuthRepositoryImpl(
 *     httpClient = httpClient,
 *     baseUrl = "https://api.edugo.com"
 * )
 *
 * // Login
 * val credentials = LoginCredentials("user@edugo.com", "password123")
 * when (val result = repository.login(credentials)) {
 *     is Result.Success -> println("Logged in: ${result.data.user.email}")
 *     is Result.Failure -> println("Error: ${result.error}")
 * }
 *
 * // Refresh
 * when (val result = repository.refresh("refresh_token_123")) {
 *     is Result.Success -> println("Token refreshed: ${result.data.accessToken}")
 *     is Result.Failure -> println("Refresh failed: ${result.error}")
 * }
 *
 * // Logout
 * repository.logout("access_token_123")
 * ```
 *
 * ## Testing
 *
 * Para testing, usar [StubAuthRepository] en lugar de esta implementación
 * para evitar llamadas de red reales.
 *
 * @property httpClient Cliente HTTP configurado (EduGoHttpClient)
 * @property baseUrl URL base del backend (ej: "https://api.edugo.com" o "http://localhost:8081")
 */
public class AuthRepositoryImpl(
    private val httpClient: EduGoHttpClient,
    private val baseUrl: String
) : AuthRepository {

    /**
     * Request body para el endpoint de refresh.
     */
    @Serializable
    private data class RefreshRequest(
        @SerialName("refresh_token")
        val refreshToken: String
    )

    override suspend fun login(credentials: LoginCredentials): Result<LoginResponse> {
        return try {
            val url = "$baseUrl/v1/auth/login"

            // Usar postSafe que retorna Result<T> automáticamente
            val result = httpClient.postSafe<LoginCredentials, LoginResponse>(
                url = url,
                body = credentials
            )

            // Mapear a errores más específicos si es necesario
            when (result) {
                is Result.Success -> result
                is Result.Failure -> {
                    // Aquí podrías parsear el mensaje de error para dar más contexto
                    // Por ahora lo retornamos tal cual
                    result
                }
                is Result.Loading -> Result.Failure("Unexpected loading state")
            }
        } catch (e: ClientRequestException) {
            // Mapeo explícito de errores HTTP 4xx
            println("AuthRepository: Client error on login - Status: ${e.response.status.value}, Message: ${e.message}")
            val errorMessage = when (e.response.status.value) {
                400 -> ErrorCode.VALIDATION_INVALID_INPUT.description
                401 -> ErrorCode.AUTH_INVALID_CREDENTIALS.description
                403 -> ErrorCode.AUTH_FORBIDDEN.description
                404 -> ErrorCode.BUSINESS_RESOURCE_NOT_FOUND.description
                423 -> ErrorCode.AUTH_ACCOUNT_LOCKED.description
                else -> e.message ?: "Request failed"
            }
            Result.Failure(errorMessage)
        } catch (e: ServerResponseException) {
            // Mapeo explícito de errores HTTP 5xx
            println("AuthRepository: Server error on login - Status: ${e.response.status.value}, Message: ${e.message}")
            val errorMessage = when (e.response.status.value) {
                500 -> ErrorCode.SYSTEM_INTERNAL_ERROR.description
                502 -> ErrorCode.NETWORK_SERVER_ERROR.description
                503 -> ErrorCode.SYSTEM_SERVICE_UNAVAILABLE.description
                else -> e.message ?: "Server error"
            }
            Result.Failure(errorMessage)
        } catch (e: Throwable) {
            // Mapeo de otros errores (network, timeout, etc.)
            println("AuthRepository: Unexpected error on login - ${e.message}")
            val networkException = ExceptionMapper.map(e)
            Result.Failure(networkException.toAppError().toString())
        }
    }

    override suspend fun logout(accessToken: String): Result<Unit> {
        return try {
            val url = "$baseUrl/v1/auth/logout"

            // Configurar header de autorización
            val config = com.edugo.test.module.network.HttpRequestConfig.builder()
                .header("Authorization", "Bearer $accessToken")
                .build()

            // POST sin body y sin respuesta esperada
            httpClient.postNoResponse(url, Unit, config)

            Result.Success(Unit)
        } catch (e: ClientRequestException) {
            // En logout, típicamente ignoramos errores 401 (token ya inválido)
            println("AuthRepository: Client error on logout - Status: ${e.response.status.value}, Message: ${e.message}")
            if (e.response.status.value == 401) {
                // Token ya inválido, considerar como éxito
                Result.Success(Unit)
            } else {
                val errorMessage = when (e.response.status.value) {
                    500 -> ErrorCode.SYSTEM_INTERNAL_ERROR.description
                    else -> e.message ?: "Logout failed"
                }
                Result.Failure(errorMessage)
            }
        } catch (e: ServerResponseException) {
            // Errores del servidor en logout no son críticos
            println("AuthRepository: Server error on logout - Status: ${e.response.status.value}, Message: ${e.message}")
            val errorMessage = ErrorCode.SYSTEM_INTERNAL_ERROR.description
            Result.Failure(errorMessage)
        } catch (e: Throwable) {
            // Errores de red en logout no son críticos
            // Podríamos considerar retornar Success de todos modos
            println("AuthRepository: Unexpected error on logout - ${e.message}")
            val networkException = ExceptionMapper.map(e)
            Result.Failure(networkException.toAppError().toString())
        }
    }

    override suspend fun refresh(refreshToken: String): Result<RefreshResponse> {
        return try {
            val url = "$baseUrl/v1/auth/refresh"

            // Crear body con el refresh token
            val requestBody = RefreshRequest(refreshToken = refreshToken)

            // Usar postSafe que retorna Result<T> automáticamente
            val result = httpClient.postSafe<RefreshRequest, RefreshResponse>(
                url = url,
                body = requestBody
            )

            when (result) {
                is Result.Success -> result
                is Result.Failure -> {
                    // El refresh falló, típicamente significa que debe re-autenticarse
                    result
                }
                is Result.Loading -> Result.Failure("Unexpected loading state")
            }
        } catch (e: ClientRequestException) {
            // Mapeo explícito de errores HTTP 4xx en refresh
            println("AuthRepository: Client error on refresh - Status: ${e.response.status.value}, Message: ${e.message}")
            val errorMessage = when (e.response.status.value) {
                401 -> ErrorCode.AUTH_REFRESH_TOKEN_INVALID.description
                403 -> ErrorCode.AUTH_FORBIDDEN.description
                else -> e.message ?: "Refresh failed"
            }
            Result.Failure(errorMessage)
        } catch (e: ServerResponseException) {
            // Mapeo explícito de errores HTTP 5xx
            println("AuthRepository: Server error on refresh - Status: ${e.response.status.value}, Message: ${e.message}")
            val errorMessage = when (e.response.status.value) {
                500 -> ErrorCode.SYSTEM_INTERNAL_ERROR.description
                502 -> ErrorCode.NETWORK_SERVER_ERROR.description
                503 -> ErrorCode.SYSTEM_SERVICE_UNAVAILABLE.description
                else -> e.message ?: "Server error"
            }
            Result.Failure(errorMessage)
        } catch (e: Throwable) {
            // Mapeo de otros errores (network, timeout, etc.)
            println("AuthRepository: Unexpected error on refresh - ${e.message}")
            val networkException = ExceptionMapper.map(e)
            Result.Failure(networkException.toAppError().toString())
        }
    }

    override suspend fun verifyToken(token: String): Result<TokenVerificationResponse> {
        return try {
            val url = "$baseUrl/v1/auth/verify"

            // Crear body con el token
            val requestBody = TokenVerificationRequest(token = token)

            // Usar postSafe que retorna Result<T> automáticamente
            val result = httpClient.postSafe<TokenVerificationRequest, TokenVerificationResponse>(
                url = url,
                body = requestBody
            )

            when (result) {
                is Result.Success -> result
                is Result.Failure -> result
                is Result.Loading -> Result.Failure("Unexpected loading state")
            }
        } catch (e: ClientRequestException) {
            // Mapeo explícito de errores HTTP 4xx en verify
            println("AuthRepository: Client error on verify - Status: ${e.response.status.value}, Message: ${e.message}")
            val errorMessage = when (e.response.status.value) {
                400 -> ErrorCode.VALIDATION_INVALID_INPUT.description
                429 -> "Rate limit exceeded"
                else -> e.message ?: "Verification failed"
            }
            Result.Failure(errorMessage)
        } catch (e: ServerResponseException) {
            // Mapeo explícito de errores HTTP 5xx
            println("AuthRepository: Server error on verify - Status: ${e.response.status.value}, Message: ${e.message}")
            val errorMessage = when (e.response.status.value) {
                500 -> ErrorCode.SYSTEM_INTERNAL_ERROR.description
                502 -> ErrorCode.NETWORK_SERVER_ERROR.description
                503 -> ErrorCode.SYSTEM_SERVICE_UNAVAILABLE.description
                else -> e.message ?: "Server error"
            }
            Result.Failure(errorMessage)
        } catch (e: Throwable) {
            // Mapeo de otros errores (network, timeout, etc.)
            println("AuthRepository: Unexpected error on verify - ${e.message}")
            val networkException = ExceptionMapper.map(e)
            Result.Failure(networkException.toAppError().toString())
        }
    }

    companion object {
        /**
         * URLs típicas del backend por entorno.
         */
        public object BaseUrls {
            /** URL para desarrollo local */
            public const val LOCAL: String = "http://localhost:8081"

            /** URL para entorno de desarrollo */
            public const val DEVELOPMENT: String = "https://dev-api.edugo.com"

            /** URL para entorno de staging */
            public const val STAGING: String = "https://staging-api.edugo.com"

            /** URL para producción */
            public const val PRODUCTION: String = "https://api.edugo.com"
        }

        /**
         * Factory method para crear instancia con configuración por defecto.
         *
         * @param baseUrl URL base del backend
         * @return Nueva instancia de AuthRepositoryImpl
         */
        public fun create(baseUrl: String = BaseUrls.LOCAL): AuthRepositoryImpl {
            val httpClient = EduGoHttpClient.create()
            return AuthRepositoryImpl(httpClient, baseUrl)
        }

        /**
         * Factory method para testing con HttpClient personalizado.
         *
         * Útil para inyectar MockEngine en tests.
         *
         * @param httpClient Cliente HTTP configurado (puede ser mock)
         * @param baseUrl URL base del backend
         * @return Nueva instancia de AuthRepositoryImpl
         */
        public fun withHttpClient(
            httpClient: EduGoHttpClient,
            baseUrl: String = BaseUrls.LOCAL
        ): AuthRepositoryImpl {
            return AuthRepositoryImpl(httpClient, baseUrl)
        }
    }
}
