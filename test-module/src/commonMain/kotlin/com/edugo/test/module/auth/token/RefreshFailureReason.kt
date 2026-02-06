package com.edugo.test.module.auth.token

import com.edugo.test.module.auth.model.AuthError
import com.edugo.test.module.core.ErrorCode

/**
 * Sealed class que representa las razones por las cuales un refresh de token puede fallar.
 *
 * Esta clase modela los diferentes escenarios de falla en el proceso de renovación
 * de tokens, permitiendo que la UI y otros componentes reaccionen apropiadamente
 * a cada tipo de falla.
 *
 * ## Casos de Uso
 *
 * ```kotlin
 * tokenRefreshManager.onRefreshFailed.collect { reason ->
 *     when (reason) {
 *         is RefreshFailureReason.TokenExpired -> {
 *             // Token expiró, navegar a login
 *             navigateToLogin()
 *         }
 *         is RefreshFailureReason.TokenRevoked -> {
 *             // Token revocado, mostrar mensaje y logout
 *             showMessage("Tu sesión fue cerrada por seguridad")
 *             logout()
 *         }
 *         is RefreshFailureReason.NetworkError -> {
 *             // Error de red, permitir retry
 *             showRetryDialog(reason.cause)
 *         }
 *         // ... otros casos
 *     }
 * }
 * ```
 *
 * ## Conversión a AuthError
 *
 * Cada razón puede convertirse a un [AuthError] para consistencia con el resto
 * del sistema de errores:
 *
 * ```kotlin
 * val authError = refreshFailureReason.toAuthError()
 * val userMessage = authError.getUserFriendlyMessage()
 * ```
 */
public sealed class RefreshFailureReason {

    /**
     * El refresh token ha expirado.
     *
     * El token de renovación alcanzó su tiempo de vida máximo y ya no es válido.
     * El usuario debe volver a autenticarse con sus credenciales.
     *
     * **Acción recomendada**: Navegar a pantalla de login.
     */
    public object TokenExpired : RefreshFailureReason()

    /**
     * El refresh token fue revocado por el servidor.
     *
     * El token fue explícitamente invalidado, posiblemente por:
     * - Logout desde otro dispositivo
     * - Acción administrativa
     * - Cambio de password
     * - Detección de uso sospechoso
     *
     * **Acción recomendada**: Limpiar sesión y navegar a login con mensaje explicativo.
     */
    public object TokenRevoked : RefreshFailureReason()

    /**
     * No hay refresh token disponible en storage.
     *
     * No se encontró un refresh token guardado, posiblemente porque:
     * - El usuario nunca hizo login
     * - El storage fue limpiado
     * - El login inicial no proporcionó refresh token
     *
     * **Acción recomendada**: Navegar a login sin mensaje de error.
     */
    public object NoRefreshToken : RefreshFailureReason()

    /**
     * Error de red después de agotar todos los reintentos.
     *
     * Se intentó renovar el token múltiples veces pero todos los intentos
     * fallaron por problemas de conectividad.
     *
     * **Acción recomendada**: Mostrar mensaje de error de red y permitir retry manual.
     *
     * @property cause Descripción del error de red (timeout, no connection, etc.)
     */
    public data class NetworkError(val cause: String) : RefreshFailureReason()

    /**
     * El servidor retornó un error inesperado.
     *
     * Error HTTP 5xx o respuesta no manejada del servidor durante el refresh.
     *
     * **Acción recomendada**: Loguear error y mostrar mensaje genérico al usuario.
     *
     * @property code Código HTTP del error
     * @property message Mensaje de error del servidor
     */
    public data class ServerError(val code: Int, val message: String) : RefreshFailureReason()

    /**
     * Convierte esta razón de falla a un [AuthError] equivalente.
     *
     * Permite integración con el sistema de errores existente y obtener
     * mensajes amigables para el usuario.
     *
     * ## Ejemplo
     *
     * ```kotlin
     * val failureReason = RefreshFailureReason.TokenExpired
     * val authError = failureReason.toAuthError()
     *
     * // authError es AuthError.TokenExpired con ErrorCode.AUTH_TOKEN_EXPIRED
     * val userMessage = authError.getUserFriendlyMessage()
     * // "Tu sesión ha expirado. Por favor inicia sesión nuevamente."
     * ```
     *
     * ## Mapeo
     *
     * - [TokenExpired] → [AuthError.TokenExpired]
     * - [TokenRevoked] → [AuthError] personalizado con ErrorCode.AUTH_TOKEN_REVOKED
     * - [NoRefreshToken] → [AuthError.InvalidCredentials] (sin credenciales válidas)
     * - [NetworkError] → [AuthError.NetworkError]
     * - [ServerError] → [AuthError.UnknownError]
     *
     * @return [AuthError] correspondiente a esta razón de falla
     */
    public fun toAuthError(): AuthError {
        return when (this) {
            is TokenExpired -> AuthError.fromErrorResponse(
                code = ErrorCode.AUTH_TOKEN_EXPIRED.name,
                message = ErrorCode.AUTH_TOKEN_EXPIRED.description
            )

            is TokenRevoked -> AuthError.fromErrorResponse(
                code = ErrorCode.AUTH_TOKEN_REVOKED.name,
                message = ErrorCode.AUTH_TOKEN_REVOKED.description
            )

            is NoRefreshToken -> AuthError.fromErrorResponse(
                code = ErrorCode.AUTH_REFRESH_TOKEN_INVALID.name,
                message = "No refresh token available"
            )

            is NetworkError -> AuthError.NetworkError(cause)

            is ServerError -> AuthError.UnknownError("Server error: $code - $message")
        }
    }

    /**
     * Indica si esta falla es recuperable con un retry.
     *
     * Algunas fallas son temporales (red) mientras que otras son permanentes
     * (token expirado, revocado).
     *
     * ## Ejemplo
     *
     * ```kotlin
     * if (failureReason.isRetryable()) {
     *     showRetryButton()
     * } else {
     *     navigateToLogin()
     * }
     * ```
     *
     * @return true si la operación puede intentarse nuevamente, false si es falla permanente
     */
    public fun isRetryable(): Boolean {
        return when (this) {
            is NetworkError -> true  // Error temporal de red
            is ServerError -> code >= 500  // Errores 5xx son potencialmente temporales
            is TokenExpired,
            is TokenRevoked,
            is NoRefreshToken -> false  // Fallas permanentes
        }
    }

    /**
     * Obtiene una representación legible para logging.
     *
     * **IMPORTANTE**: No incluye información sensible como tokens completos.
     *
     * @return Descripción del error para logs
     */
    public fun toLogString(): String {
        return when (this) {
            is TokenExpired -> "RefreshFailureReason.TokenExpired"
            is TokenRevoked -> "RefreshFailureReason.TokenRevoked"
            is NoRefreshToken -> "RefreshFailureReason.NoRefreshToken"
            is NetworkError -> "RefreshFailureReason.NetworkError(cause=$cause)"
            is ServerError -> "RefreshFailureReason.ServerError(code=$code, message=$message)"
        }
    }
}
