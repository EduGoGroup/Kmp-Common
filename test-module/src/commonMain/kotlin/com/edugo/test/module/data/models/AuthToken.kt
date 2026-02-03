package com.edugo.test.module.data.models

import com.edugo.test.module.core.Result
import com.edugo.test.module.core.failure
import com.edugo.test.module.core.success
import com.edugo.test.module.data.models.base.ValidatableModel
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo que representa un token de autenticación.
 *
 * Este modelo encapsula la información de autenticación JWT o similar,
 * incluyendo el token principal, su fecha de expiración, y opcionalmente
 * un refresh token para renovar la sesión.
 *
 * ## Composición de Interfaces
 *
 * Este modelo implementa:
 * - [ValidatableModel]: Proporciona validación con Result<Unit>
 *
 * **DECISIÓN DE DISEÑO**: A diferencia de [User] y [Role], AuthToken NO implementa
 * [EntityBase] porque:
 * 1. Los tokens no tienen un ciclo de vida persistente en BD
 * 2. No requieren timestamps de creación/actualización (solo expiración)
 * 3. Son efímeros y generados por el sistema de autenticación
 *
 * ## Serialización y Compatibilidad con Backend
 *
 * Este modelo usa `@SerialName` para mapear entre convenciones de nomenclatura:
 * - Kotlin: camelCase (`expiresAt`, `refreshToken`)
 * - Backend/JSON: snake_case (`expires_at`, `refresh_token`)
 *
 * ### Formato de Instant en JSON
 *
 * kotlinx.datetime.Instant se serializa automáticamente a formato ISO-8601:
 *
 * ```json
 * {
 *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
 *   "expires_at": "2024-01-01T12:00:00Z",
 *   "refresh_token": "refresh_eyJhbGciOiJIUzI1NiI..."
 * }
 * ```
 *
 * ## Ejemplo de Uso Básico
 *
 * ```kotlin
 * // Crear token de autenticación
 * val authToken = AuthToken(
 *     token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
 *     expiresAt = Clock.System.now() + 3600.seconds, // Expira en 1 hora
 *     refreshToken = "refresh_eyJhbGciOiJIUzI1NiI..."
 * )
 *
 * // Validar token
 * when (val result = authToken.validate()) {
 *     is Result.Success -> {
 *         if (authToken.isValid()) {
 *             // Token válido y no expirado
 *         }
 *     }
 *     is Result.Failure -> println("Invalid token: ${result.error}")
 * }
 * ```
 *
 * ## Verificación de Expiración
 *
 * ```kotlin
 * // Verificar si el token ha expirado
 * if (authToken.isExpired()) {
 *     // Intentar renovar con refresh token
 *     authToken.refreshToken?.let { refresh ->
 *         val newToken = authService.refreshToken(refresh)
 *     }
 * }
 * ```
 *
 * ## Uso con Almacenamiento Seguro
 *
 * ```kotlin
 * // Guardar token de forma segura
 * secureStorage.save("auth_token", Json.encodeToString(authToken))
 *
 * // Recuperar token
 * val stored = secureStorage.get("auth_token")
 * val authToken = Json.decodeFromString<AuthToken>(stored)
 *
 * // Verificar validez antes de usar
 * if (authToken.isValid()) {
 *     apiClient.setAuthToken(authToken.token)
 * }
 * ```
 *
 * @property token Token de autenticación (típicamente JWT)
 * @property expiresAt Timestamp de expiración del token (ISO-8601)
 * @property refreshToken Token opcional para renovar la autenticación (default: null)
 */
@Serializable
public data class AuthToken(
    @SerialName("token")
    val token: String,

    @SerialName("expires_at")
    val expiresAt: Instant = Clock.System.now(),

    @SerialName("refresh_token")
    val refreshToken: String? = null
) : ValidatableModel {

    /**
     * Valida la consistencia e integridad de los datos del token.
     *
     * ## Reglas de Validación
     *
     * 1. **Token**: No puede estar vacío
     * 2. **RefreshToken**: Si está presente, no puede estar vacío
     *
     * **NOTA**: Esta validación NO verifica si el token ha expirado.
     * Use [isExpired] o [isValid] para verificar expiración.
     *
     * ## Ejemplos
     *
     * ```kotlin
     * // Token válido
     * val valid = AuthToken(
     *     token = "eyJhbGciOiJIUzI1NiI...",
     *     expiresAt = Clock.System.now() + 3600.seconds
     * )
     * valid.validate() // Result.Success
     *
     * // Token inválido - token vacío
     * val invalid = AuthToken(
     *     token = "",
     *     expiresAt = Clock.System.now()
     * )
     * invalid.validate() // Result.Failure("Token cannot be blank")
     * ```
     *
     * @return [Result.Success] si todas las validaciones pasan,
     *         [Result.Failure] con mensaje descriptivo si alguna falla
     */
    override fun validate(): Result<Unit> {
        return when {
            token.isBlank() ->
                failure("Token cannot be blank")

            refreshToken != null && refreshToken.isBlank() ->
                failure("Refresh token cannot be blank if provided")

            else ->
                success(Unit)
        }
    }

    /**
     * Verifica si el token ha expirado.
     *
     * Compara la fecha de expiración con el momento actual.
     *
     * Ejemplo:
     * ```kotlin
     * if (authToken.isExpired()) {
     *     println("Token expirado, necesita renovación")
     * }
     * ```
     *
     * @return true si el token ha expirado, false si aún es válido
     */
    public fun isExpired(): Boolean {
        return Clock.System.now() >= expiresAt
    }

    /**
     * Verifica si el token es válido (no vacío y no expirado).
     *
     * Esta es una verificación combinada de validación estructural
     * y estado de expiración.
     *
     * Ejemplo:
     * ```kotlin
     * if (authToken.isValid()) {
     *     apiClient.setAuthToken(authToken.token)
     * } else {
     *     // Re-autenticar o renovar token
     * }
     * ```
     *
     * @return true si el token es válido y no ha expirado
     */
    public fun isValid(): Boolean {
        return validate() is Result.Success && !isExpired()
    }

    /**
     * Verifica si hay un refresh token disponible.
     *
     * Útil para determinar si se puede intentar renovar la sesión.
     *
     * Ejemplo:
     * ```kotlin
     * if (authToken.isExpired() && authToken.hasRefreshToken()) {
     *     val newToken = authService.refresh(authToken.refreshToken!!)
     * }
     * ```
     *
     * @return true si refreshToken no es null y no está vacío
     */
    public fun hasRefreshToken(): Boolean {
        return !refreshToken.isNullOrBlank()
    }

    /**
     * Calcula el tiempo restante hasta la expiración.
     *
     * Útil para mostrar advertencias al usuario antes de que expire.
     *
     * Ejemplo:
     * ```kotlin
     * val remaining = authToken.timeUntilExpiration()
     * if (remaining.inWholeMinutes < 5) {
     *     showWarning("Su sesión expira en ${remaining.inWholeMinutes} minutos")
     * }
     * ```
     *
     * @return Duration hasta la expiración (puede ser negativo si ya expiró)
     */
    public fun timeUntilExpiration(): kotlin.time.Duration {
        return expiresAt - Clock.System.now()
    }

    /**
     * Obtiene un resumen corto del token para logging.
     *
     * **IMPORTANTE**: Solo muestra los primeros y últimos caracteres del token
     * por seguridad. NUNCA loguear tokens completos.
     *
     * Ejemplo: "AuthToken(token=eyJh...J9, expires=2024-01-01T12:00:00Z)"
     */
    public fun toLogString(): String {
        val tokenPreview = if (token.length > 10) {
            "${token.take(4)}...${token.takeLast(2)}"
        } else {
            "***"
        }
        return "AuthToken(token=$tokenPreview, expires=$expiresAt, hasRefresh=${hasRefreshToken()})"
    }

    companion object {
        /**
         * Crea un token de ejemplo para tests.
         *
         * DECISIÓN DE DISEÑO: Este factory method es útil para tests
         * pero NO debe usarse en producción (los tokens reales vienen del backend).
         *
         * @param durationSeconds Duración del token en segundos (default: 3600 = 1 hora)
         * @param includeRefresh Si debe incluir refresh token (default: true)
         */
        public fun createTestToken(
            durationSeconds: Long = 3600,
            includeRefresh: Boolean = true
        ): AuthToken {
            val now = Clock.System.now()
            return AuthToken(
                token = "test_token_${now.toEpochMilliseconds()}",
                expiresAt = now + kotlin.time.Duration.parse("${durationSeconds}s"),
                refreshToken = if (includeRefresh) "refresh_token_${now.toEpochMilliseconds()}" else null
            )
        }
    }
}
