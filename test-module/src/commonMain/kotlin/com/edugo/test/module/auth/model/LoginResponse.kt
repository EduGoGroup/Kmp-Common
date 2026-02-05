package com.edugo.test.module.auth.model

import com.edugo.test.module.data.models.AuthToken
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

/**
 * Modelo que representa la respuesta exitosa del endpoint de login.
 *
 * Este modelo encapsula todos los datos que retorna el backend después de
 * una autenticación exitosa, incluyendo tokens JWT y información del usuario.
 *
 * ## Serialización y Compatibilidad con Backend
 *
 * Este modelo usa `@SerialName` para mapear entre convenciones de nomenclatura:
 * - Kotlin: camelCase (`accessToken`, `expiresIn`, `refreshToken`, `tokenType`)
 * - Backend/JSON: snake_case (`access_token`, `expires_in`, `refresh_token`, `token_type`)
 *
 * ### Formato JSON del Backend
 *
 * ```json
 * {
 *   "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
 *   "expires_in": 3600,
 *   "refresh_token": "refresh_eyJhbGciOiJIUzI1NiI...",
 *   "token_type": "Bearer",
 *   "user": {
 *     "id": "user-123",
 *     "email": "john.doe@edugo.com",
 *     "first_name": "John",
 *     "last_name": "Doe",
 *     "full_name": "John Doe",
 *     "role": "student",
 *     "school_id": "school-456"
 *   }
 * }
 * ```
 *
 * ## Conversión a AuthToken
 *
 * Este modelo se puede convertir a [AuthToken] para usar con el sistema
 * de autenticación existente:
 *
 * ```kotlin
 * val loginResponse: LoginResponse = authRepository.login(credentials)
 * val authToken: AuthToken = loginResponse.toAuthToken()
 *
 * // Guardar token en storage
 * storage.putString("auth_token", Json.encodeToString(authToken))
 * ```
 *
 * ## Ejemplo de Uso Completo
 *
 * ```kotlin
 * // En AuthService
 * suspend fun login(credentials: LoginCredentials): Result<LoginResponse> {
 *     val result = repository.login(credentials)
 *
 *     return when (result) {
 *         is Result.Success -> {
 *             val response = result.data
 *
 *             // Convertir y guardar token
 *             val authToken = response.toAuthToken()
 *             storage.putString(AUTH_TOKEN_KEY, Json.encodeToString(authToken))
 *
 *             // Guardar información del usuario
 *             storage.putString(AUTH_USER_KEY, Json.encodeToString(response.user))
 *
 *             // Actualizar estado
 *             _authState.value = AuthState.Authenticated(response.user, authToken)
 *
 *             Result.Success(response)
 *         }
 *         is Result.Failure -> result
 *         is Result.Loading -> result
 *     }
 * }
 * ```
 *
 * @property accessToken Token de acceso JWT
 * @property expiresIn Tiempo de expiración en segundos desde ahora
 * @property refreshToken Token para renovar la sesión
 * @property tokenType Tipo de token (típicamente "Bearer")
 * @property user Información del usuario autenticado
 */
@Serializable
public data class LoginResponse(
    @SerialName("access_token")
    val accessToken: String,

    @SerialName("expires_in")
    val expiresIn: Int,

    @SerialName("refresh_token")
    val refreshToken: String,

    @SerialName("token_type")
    val tokenType: String,

    @SerialName("user")
    val user: AuthUserInfo
) {

    /**
     * Convierte este LoginResponse a un AuthToken.
     *
     * Esta conversión calcula la fecha de expiración basándose en el timestamp
     * actual más el valor de `expiresIn` (en segundos).
     *
     * **IMPORTANTE**: La fecha de expiración se calcula en el momento de la
     * conversión, por lo que se recomienda llamar este método inmediatamente
     * después de recibir la respuesta del backend.
     *
     * ## Ejemplo
     *
     * ```kotlin
     * val loginResponse = LoginResponse(
     *     accessToken = "eyJhbGciOiJIUzI1NiI...",
     *     expiresIn = 3600,  // 1 hora
     *     refreshToken = "refresh_token...",
     *     tokenType = "Bearer",
     *     user = userInfo
     * )
     *
     * val authToken = loginResponse.toAuthToken()
     * // authToken.expiresAt será Clock.System.now() + 3600 segundos
     * ```
     *
     * @return AuthToken con los datos convertidos
     */
    public fun toAuthToken(): AuthToken {
        val now = Clock.System.now()
        val expiresAt = now + expiresIn.seconds

        return AuthToken(
            token = accessToken,
            expiresAt = expiresAt,
            refreshToken = refreshToken
        )
    }

    /**
     * Verifica si el token es de tipo Bearer.
     *
     * @return true si tokenType es "Bearer" (case-insensitive)
     */
    public fun isBearerToken(): Boolean {
        return tokenType.equals("Bearer", ignoreCase = true)
    }

    /**
     * Obtiene el header de autorización formateado.
     *
     * Útil para configurar headers HTTP manualmente.
     *
     * Ejemplo:
     * ```kotlin
     * val authHeader = loginResponse.getAuthorizationHeader()
     * // "Bearer eyJhbGciOiJIUzI1NiI..."
     *
     * httpClient.config {
     *     header("Authorization", authHeader)
     * }
     * ```
     *
     * @return String con formato "Bearer <token>"
     */
    public fun getAuthorizationHeader(): String {
        return "$tokenType $accessToken"
    }

    /**
     * Calcula cuándo expirará el token.
     *
     * Retorna un Instant calculado desde el momento actual.
     *
     * @return Instant de expiración
     */
    public fun calculateExpirationTime(): kotlinx.datetime.Instant {
        return Clock.System.now() + expiresIn.seconds
    }

    /**
     * Obtiene una representación segura para logging.
     *
     * **IMPORTANTE**: NO loguear tokens completos en producción.
     * Esta función solo muestra información no sensible.
     *
     * Ejemplo: "LoginResponse(tokenType=Bearer, expiresIn=3600, userId=user-123, userRole=student)"
     */
    public fun toLogString(): String {
        val tokenPreview = if (accessToken.length > 10) {
            "${accessToken.take(4)}...${accessToken.takeLast(2)}"
        } else {
            "***"
        }
        return "LoginResponse(tokenType=$tokenType, expiresIn=$expiresIn, " +
                "token=$tokenPreview, userId=${user.id}, userRole=${user.role})"
    }

    companion object {
        /**
         * Crea una respuesta de ejemplo para tests.
         *
         * **IMPORTANTE**: Solo usar en tests, nunca en producción.
         *
         * @param accessToken Token de acceso (default: test_access_token)
         * @param expiresIn Segundos hasta expiración (default: 3600)
         * @param refreshToken Token de refresh (default: test_refresh_token)
         * @param user Información del usuario (default: usuario de prueba)
         */
        public fun createTestResponse(
            accessToken: String = "test_access_token_${Clock.System.now().toEpochMilliseconds()}",
            expiresIn: Int = 3600,
            refreshToken: String = "test_refresh_token_${Clock.System.now().toEpochMilliseconds()}",
            user: AuthUserInfo = AuthUserInfo.createTestUser()
        ): LoginResponse {
            return LoginResponse(
                accessToken = accessToken,
                expiresIn = expiresIn,
                refreshToken = refreshToken,
                tokenType = "Bearer",
                user = user
            )
        }
    }
}
