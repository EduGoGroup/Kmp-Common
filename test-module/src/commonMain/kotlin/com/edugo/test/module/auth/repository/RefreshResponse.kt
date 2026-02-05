package com.edugo.test.module.auth.repository

import com.edugo.test.module.data.models.AuthToken
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

/**
 * Modelo que representa la respuesta del endpoint de refresh de token.
 *
 * Este modelo encapsula los datos que retorna el backend después de
 * renovar un access token usando un refresh token. A diferencia de
 * [LoginResponse], esta respuesta NO incluye un nuevo refresh token
 * ni información del usuario.
 *
 * ## Serialización y Compatibilidad con Backend
 *
 * Este modelo usa `@SerialName` para mapear entre convenciones de nomenclatura:
 * - Kotlin: camelCase (`accessToken`, `expiresIn`, `tokenType`)
 * - Backend/JSON: snake_case (`access_token`, `expires_in`, `token_type`)
 *
 * ### Formato JSON del Backend
 *
 * ```json
 * {
 *   "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
 *   "expires_in": 3600,
 *   "token_type": "Bearer"
 * }
 * ```
 *
 * **IMPORTANTE**: El backend NO retorna un nuevo `refresh_token`.
 * El refresh token original debe mantenerse para futuras renovaciones.
 *
 * ## Diferencias con LoginResponse
 *
 * | Campo | LoginResponse | RefreshResponse |
 * |-------|---------------|-----------------|
 * | access_token | ✅ | ✅ |
 * | expires_in | ✅ | ✅ |
 * | token_type | ✅ | ✅ |
 * | refresh_token | ✅ | ❌ |
 * | user | ✅ | ❌ |
 *
 * ## Conversión a AuthToken
 *
 * Este modelo se puede convertir a [AuthToken], pero requiere el
 * refresh token existente como parámetro:
 *
 * ```kotlin
 * val refreshResponse: RefreshResponse = repository.refresh(oldRefreshToken)
 * val newAuthToken = refreshResponse.toAuthToken(oldRefreshToken)
 *
 * // newAuthToken tendrá:
 * // - token: nuevo access token del backend
 * // - expiresAt: calculado desde expires_in
 * // - refreshToken: el MISMO refresh token original
 * ```
 *
 * ## Ejemplo de Uso Completo
 *
 * ```kotlin
 * // En AuthService
 * override suspend fun refreshToken(): Result<AuthToken> {
 *     // 1. Obtener refresh token actual del storage
 *     val currentTokenJson = storage.getStringOrNull(AUTH_TOKEN_KEY)
 *         ?: return Result.Failure("No token found")
 *
 *     val currentToken = Json.decodeFromString<AuthToken>(currentTokenJson)
 *     val refreshToken = currentToken.refreshToken
 *         ?: return Result.Failure("No refresh token available")
 *
 *     // 2. Llamar al repository para renovar
 *     return when (val result = repository.refresh(refreshToken)) {
 *         is Result.Success -> {
 *             // 3. Convertir a AuthToken manteniendo el refresh token original
 *             val newAuthToken = result.data.toAuthToken(refreshToken)
 *
 *             // 4. Guardar nuevo token
 *             storage.putString(
 *                 AUTH_TOKEN_KEY,
 *                 Json.encodeToString(newAuthToken)
 *             )
 *
 *             // 5. Actualizar estado
 *             _authState.value = when (val current = _authState.value) {
 *                 is AuthState.Authenticated -> current.copy(token = newAuthToken)
 *                 else -> AuthState.Unauthenticated
 *             }
 *
 *             Result.Success(newAuthToken)
 *         }
 *         is Result.Failure -> {
 *             // Refresh falló, limpiar sesión
 *             logout()
 *             result
 *         }
 *         is Result.Loading -> Result.Loading
 *     }
 * }
 * ```
 *
 * ## Flujo de Renovación Automática
 *
 * ```kotlin
 * // En AuthInterceptor con TokenProvider
 * class AuthServiceImpl : TokenProvider {
 *     override suspend fun isTokenExpired(): Boolean {
 *         val token = getCurrentToken() ?: return true
 *         return token.isExpired()
 *     }
 *
 *     override suspend fun refreshToken(): String? {
 *         return when (val result = refreshToken()) {
 *             is Result.Success -> result.data.token
 *             else -> null
 *         }
 *     }
 * }
 * ```
 *
 * @property accessToken Nuevo token de acceso JWT
 * @property expiresIn Tiempo de expiración en segundos desde ahora
 * @property tokenType Tipo de token (típicamente "Bearer")
 */
@Serializable
public data class RefreshResponse(
    @SerialName("access_token")
    val accessToken: String,

    @SerialName("expires_in")
    val expiresIn: Int,

    @SerialName("token_type")
    val tokenType: String
) {

    /**
     * Convierte este RefreshResponse a un AuthToken.
     *
     * Esta conversión requiere el refresh token existente como parámetro
     * porque el backend NO retorna un nuevo refresh token en la respuesta.
     *
     * La fecha de expiración se calcula basándose en el timestamp actual
     * más el valor de `expiresIn` (en segundos).
     *
     * **IMPORTANTE**: Siempre usar el refresh token original que se usó
     * para hacer la petición de refresh.
     *
     * ## Ejemplo
     *
     * ```kotlin
     * // Obtener token actual
     * val currentToken = getCurrentAuthToken()
     * val oldRefreshToken = currentToken.refreshToken!!
     *
     * // Renovar
     * val refreshResponse = repository.refresh(oldRefreshToken).data
     *
     * // Convertir manteniendo el refresh token
     * val newAuthToken = refreshResponse.toAuthToken(oldRefreshToken)
     *
     * // Comparación:
     * println(currentToken.token)         // "old_access_token"
     * println(newAuthToken.token)         // "new_access_token"
     * println(currentToken.refreshToken)  // "refresh_123"
     * println(newAuthToken.refreshToken)  // "refresh_123" (MISMO)
     * ```
     *
     * @param existingRefreshToken El refresh token original que se debe mantener
     * @return AuthToken con el nuevo access token y el refresh token existente
     */
    public fun toAuthToken(existingRefreshToken: String): AuthToken {
        val now = Clock.System.now()
        val expiresAt = now + expiresIn.seconds

        return AuthToken(
            token = accessToken,
            expiresAt = expiresAt,
            refreshToken = existingRefreshToken
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
     * val authHeader = refreshResponse.getAuthorizationHeader()
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
     * Ejemplo: "RefreshResponse(tokenType=Bearer, expiresIn=3600, token=eyJh...J9)"
     */
    public fun toLogString(): String {
        val tokenPreview = if (accessToken.length > 10) {
            "${accessToken.take(4)}...${accessToken.takeLast(2)}"
        } else {
            "***"
        }
        return "RefreshResponse(tokenType=$tokenType, expiresIn=$expiresIn, token=$tokenPreview)"
    }

    companion object {
        /**
         * Crea una respuesta de ejemplo para tests.
         *
         * **IMPORTANTE**: Solo usar en tests, nunca en producción.
         *
         * @param accessToken Token de acceso (default: test_access_token_<timestamp>)
         * @param expiresIn Segundos hasta expiración (default: 3600)
         * @param tokenType Tipo de token (default: Bearer)
         */
        public fun createTestResponse(
            accessToken: String = "test_access_token_${Clock.System.now().toEpochMilliseconds()}",
            expiresIn: Int = 3600,
            tokenType: String = "Bearer"
        ): RefreshResponse {
            return RefreshResponse(
                accessToken = accessToken,
                expiresIn = expiresIn,
                tokenType = tokenType
            )
        }
    }
}
