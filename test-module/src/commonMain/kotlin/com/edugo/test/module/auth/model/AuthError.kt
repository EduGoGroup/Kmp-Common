package com.edugo.test.module.auth.model

import com.edugo.test.module.core.ErrorCode

/**
 * Sealed class que representa los diferentes tipos de errores de autenticación.
 *
 * Esta clase proporciona un catálogo type-safe de todos los errores posibles
 * durante el flujo de autenticación, cada uno mapeado a su correspondiente
 * [ErrorCode] para consistencia con el sistema de errores del proyecto.
 *
 * ## Diseño y Arquitectura
 *
 * Esta sealed class sigue el patrón de modelado de errores de dominio específico:
 * - **Type-safe**: El compilador garantiza que todos los casos están manejados
 * - **Semántico**: Cada error tiene un significado claro en el dominio de autenticación
 * - **Integrado**: Se mapea a ErrorCode existente para logging y tracking
 *
 * ## Categorías de Errores
 *
 * 1. **Errores de Credenciales** (InvalidCredentials, UserNotFound)
 * 2. **Errores de Cuenta** (AccountLocked, UserInactive)
 * 3. **Errores de Red** (NetworkError)
 * 4. **Errores Desconocidos** (UnknownError)
 *
 * ## Ejemplo de Uso en Repository
 *
 * ```kotlin
 * suspend fun login(credentials: LoginCredentials): Result<LoginResponse> {
 *     return try {
 *         val response = httpClient.post("/v1/auth/login", credentials)
 *         Result.Success(response)
 *     } catch (e: ClientRequestException) {
 *         val authError = when (e.response.status.value) {
 *             401 -> AuthError.InvalidCredentials
 *             404 -> AuthError.UserNotFound
 *             423 -> AuthError.AccountLocked
 *             403 -> AuthError.UserInactive
 *             else -> AuthError.UnknownError(e.message ?: "Authentication failed")
 *         }
 *         Result.Failure(authError.errorCode.description)
 *     } catch (e: IOException) {
 *         Result.Failure(AuthError.NetworkError(e.message ?: "Network error").errorCode.description)
 *     }
 * }
 * ```
 *
 * ## Ejemplo de Uso en UI
 *
 * ```kotlin
 * when (val result = authService.login(credentials)) {
 *     is Result.Success -> navigateToHome()
 *     is Result.Failure -> {
 *         // Parsear mensaje de error para determinar tipo
 *         val error = AuthError.fromMessage(result.error)
 *         val userMessage = when (error) {
 *             is AuthError.InvalidCredentials ->
 *                 "Usuario o contraseña incorrectos"
 *             is AuthError.AccountLocked ->
 *                 "Tu cuenta ha sido bloqueada. Contacta soporte."
 *             is AuthError.NetworkError ->
 *                 "Sin conexión a internet. Intenta de nuevo."
 *             else ->
 *                 "Error de autenticación. Intenta de nuevo."
 *         }
 *         showError(userMessage)
 *     }
 * }
 * ```
 *
 * ## Factory Method para API Responses
 *
 * ```kotlin
 * // En AuthRepositoryImpl, al recibir error del backend
 * val apiErrorCode = errorResponse.getString("code") // "AUTH_INVALID_CREDENTIALS"
 * val apiMessage = errorResponse.getString("message")
 *
 * val authError = AuthError.fromErrorResponse(apiErrorCode, apiMessage)
 * ```
 */
public sealed class AuthError {
    /**
     * Código de error asociado del sistema.
     */
    public abstract val errorCode: ErrorCode

    /**
     * Credenciales inválidas.
     *
     * El usuario o contraseña proporcionados no son correctos.
     * Este es el error más común en flujos de login.
     *
     * **ErrorCode asociado**: [ErrorCode.AUTH_INVALID_CREDENTIALS] (2002)
     * **HTTP Status típico**: 401 Unauthorized
     */
    public object InvalidCredentials : AuthError() {
        override val errorCode: ErrorCode = ErrorCode.AUTH_INVALID_CREDENTIALS
    }

    /**
     * Usuario no encontrado.
     *
     * El email proporcionado no existe en el sistema.
     * Por razones de seguridad, en producción se suele retornar el mismo
     * error que InvalidCredentials para evitar enumerar usuarios.
     *
     * **ErrorCode asociado**: [ErrorCode.BUSINESS_RESOURCE_NOT_FOUND] (4000)
     * **HTTP Status típico**: 404 Not Found
     */
    public object UserNotFound : AuthError() {
        override val errorCode: ErrorCode = ErrorCode.BUSINESS_RESOURCE_NOT_FOUND
    }

    /**
     * Cuenta bloqueada.
     *
     * La cuenta del usuario ha sido bloqueada por razones de seguridad
     * (intentos fallidos múltiples, suspensión administrativa, etc.).
     *
     * **ErrorCode asociado**: [ErrorCode.AUTH_ACCOUNT_LOCKED] (2004)
     * **HTTP Status típico**: 423 Locked
     */
    public object AccountLocked : AuthError() {
        override val errorCode: ErrorCode = ErrorCode.AUTH_ACCOUNT_LOCKED
    }

    /**
     * Usuario inactivo.
     *
     * La cuenta existe pero está inactiva o deshabilitada.
     * Diferente de AccountLocked: este es un estado del usuario,
     * no un bloqueo de seguridad.
     *
     * **ErrorCode asociado**: [ErrorCode.AUTH_FORBIDDEN] (2003)
     * **HTTP Status típico**: 403 Forbidden
     */
    public object UserInactive : AuthError() {
        override val errorCode: ErrorCode = ErrorCode.AUTH_FORBIDDEN
    }

    /**
     * Error de red.
     *
     * Ocurrió un problema de conectividad durante la autenticación.
     * Puede ser timeout, falta de conexión, DNS failure, etc.
     *
     * **ErrorCode asociado**: Varía según el tipo de error de red
     * - [ErrorCode.NETWORK_TIMEOUT] (1000)
     * - [ErrorCode.NETWORK_NO_CONNECTION] (1001)
     * - [ErrorCode.NETWORK_DNS_FAILURE] (1003)
     *
     * @property cause Descripción del error de red
     */
    public data class NetworkError(val cause: String) : AuthError() {
        override val errorCode: ErrorCode = when {
            cause.contains("timeout", ignoreCase = true) -> ErrorCode.NETWORK_TIMEOUT
            cause.contains("connection", ignoreCase = true) -> ErrorCode.NETWORK_NO_CONNECTION
            cause.contains("dns", ignoreCase = true) -> ErrorCode.NETWORK_DNS_FAILURE
            else -> ErrorCode.NETWORK_SERVER_ERROR
        }
    }

    /**
     * Error desconocido.
     *
     * Un error que no se pudo categorizar en ninguno de los tipos anteriores.
     * Útil para casos inesperados o errores del servidor sin mapear.
     *
     * **ErrorCode asociado**: [ErrorCode.SYSTEM_UNKNOWN_ERROR] (5000)
     *
     * @property message Descripción del error
     */
    public data class UnknownError(val message: String) : AuthError() {
        override val errorCode: ErrorCode = ErrorCode.SYSTEM_UNKNOWN_ERROR
    }

    /**
     * Obtiene un mensaje amigable para el usuario.
     *
     * Convierte el error técnico en un mensaje comprensible para mostrar en UI.
     *
     * @return Mensaje en español para el usuario final
     */
    public fun getUserFriendlyMessage(): String = ERROR_MESSAGES[this::class] ?:
        "Ocurrió un error inesperado. Por favor intenta de nuevo más tarde."

    companion object {
        /**
         * Mapa estático de mensajes amigables por tipo de error.
         */
        private val ERROR_MESSAGES = mapOf(
            InvalidCredentials::class to
                "Usuario o contraseña incorrectos. Por favor verifica tus credenciales.",
            UserNotFound::class to
                "No encontramos una cuenta con ese correo electrónico.",
            AccountLocked::class to
                "Tu cuenta ha sido bloqueada. Por favor contacta al soporte.",
            UserInactive::class to
                "Tu cuenta está inactiva. Por favor contacta al administrador.",
            NetworkError::class to
                "Problema de conexión. Verifica tu internet e intenta de nuevo.",
            UnknownError::class to
                "Ocurrió un error inesperado. Por favor intenta de nuevo más tarde."
        )

        /**
         * Crea un AuthError desde un código de error del backend.
         *
         * El backend puede retornar códigos de error como strings:
         * - "AUTH_INVALID_CREDENTIALS"
         * - "BUSINESS_RESOURCE_NOT_FOUND"
         * - etc.
         *
         * Esta función mapea esos códigos a instancias de AuthError.
         *
         * Ejemplo:
         * ```kotlin
         * // Response del backend en JSON
         * {
         *   "error": {
         *     "code": "AUTH_INVALID_CREDENTIALS",
         *     "message": "Invalid email or password"
         *   }
         * }
         *
         * val authError = AuthError.fromErrorResponse(
         *     code = "AUTH_INVALID_CREDENTIALS",
         *     message = "Invalid email or password"
         * )
         * // Retorna: AuthError.InvalidCredentials
         * ```
         *
         * @param code Código de error del backend
         * @param message Mensaje de error del backend
         * @return Instancia apropiada de AuthError
         */
        public fun fromErrorResponse(code: String, message: String): AuthError {
            return when (code.uppercase()) {
                "AUTH_INVALID_CREDENTIALS" -> InvalidCredentials
                "BUSINESS_RESOURCE_NOT_FOUND" -> UserNotFound
                "AUTH_ACCOUNT_LOCKED" -> AccountLocked
                "AUTH_FORBIDDEN", "AUTH_USER_INACTIVE" -> UserInactive
                "NETWORK_TIMEOUT", "NETWORK_NO_CONNECTION", "NETWORK_DNS_FAILURE",
                "NETWORK_CONNECTION_RESET", "NETWORK_SERVER_ERROR" -> NetworkError(message)
                else -> UnknownError(message)
            }
        }

        /**
         * Crea un AuthError desde un HTTP status code.
         *
         * Útil cuando solo se tiene el código de status HTTP sin mensaje estructurado.
         *
         * Ejemplo:
         * ```kotlin
         * catch (e: ClientRequestException) {
         *     val authError = AuthError.fromHttpStatus(
         *         statusCode = e.response.status.value,
         *         message = e.message
         *     )
         * }
         * ```
         *
         * @param statusCode Código de status HTTP
         * @param message Mensaje de error opcional
         * @return Instancia apropiada de AuthError
         */
        public fun fromHttpStatus(statusCode: Int, message: String? = null): AuthError {
            return when (statusCode) {
                401 -> InvalidCredentials
                404 -> UserNotFound
                423 -> AccountLocked
                403 -> UserInactive
                in 500..599 -> UnknownError(message ?: "Server error")
                else -> UnknownError(message ?: "Authentication error")
            }
        }

        /**
         * Intenta parsear un AuthError desde un mensaje de error.
         *
         * Útil cuando se tiene un mensaje de error como string y se quiere
         * determinar el tipo de error.
         *
         * @param message Mensaje de error
         * @return AuthError más apropiado basado en el mensaje
         */
        public fun fromMessage(message: String): AuthError {
            return when {
                message.contains("invalid credentials", ignoreCase = true) ||
                message.contains("incorrect", ignoreCase = true) ||
                message.contains("wrong password", ignoreCase = true) -> InvalidCredentials

                message.contains("not found", ignoreCase = true) ||
                message.contains("does not exist", ignoreCase = true) -> UserNotFound

                message.contains("locked", ignoreCase = true) ||
                message.contains("blocked", ignoreCase = true) -> AccountLocked

                message.contains("inactive", ignoreCase = true) ||
                message.contains("disabled", ignoreCase = true) -> UserInactive

                message.contains("network", ignoreCase = true) ||
                message.contains("connection", ignoreCase = true) ||
                message.contains("timeout", ignoreCase = true) -> NetworkError(message)

                else -> UnknownError(message)
            }
        }
    }
}
