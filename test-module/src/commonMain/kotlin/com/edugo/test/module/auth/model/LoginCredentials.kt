package com.edugo.test.module.auth.model

import com.edugo.test.module.core.Result
import com.edugo.test.module.core.failure
import com.edugo.test.module.core.success
import com.edugo.test.module.data.models.base.ValidatableModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo que representa las credenciales de inicio de sesión.
 *
 * Este modelo encapsula las credenciales necesarias para autenticar a un usuario
 * en el sistema, incluyendo validaciones de formato y seguridad.
 *
 * ## Composición de Interfaces
 *
 * Este modelo implementa:
 * - [ValidatableModel]: Proporciona validación con Result<Unit>
 *
 * ## Serialización y Compatibilidad con Backend
 *
 * Este modelo usa `@SerialName` para mapear entre convenciones de nomenclatura:
 * - Kotlin: camelCase (`email`, `password`)
 * - Backend/JSON: snake_case (en este caso coinciden)
 *
 * ### Formato JSON Esperado
 *
 * ```json
 * {
 *   "email": "user@edugo.com",
 *   "password": "securePassword123"
 * }
 * ```
 *
 * ## Ejemplo de Uso Básico
 *
 * ```kotlin
 * // Crear credenciales
 * val credentials = LoginCredentials(
 *     email = "user@edugo.com",
 *     password = "securePassword123"
 * )
 *
 * // Validar antes de enviar
 * when (val result = credentials.validate()) {
 *     is Result.Success -> {
 *         // Credenciales válidas, proceder con login
 *         authRepository.login(credentials)
 *     }
 *     is Result.Failure -> {
 *         // Mostrar error al usuario
 *         showError(result.error)
 *     }
 * }
 * ```
 *
 * ## Uso con Formularios de UI
 *
 * ```kotlin
 * // En ViewModel o Presenter
 * fun onLoginClick(email: String, password: String) {
 *     val credentials = LoginCredentials(
 *         email = email.trim(),
 *         password = password
 *     )
 *
 *     when (val validation = credentials.validate()) {
 *         is Result.Success -> {
 *             // Continuar con el login
 *             viewModelScope.launch {
 *                 val result = authService.login(credentials)
 *                 // Manejar resultado
 *             }
 *         }
 *         is Result.Failure -> {
 *             // Mostrar error en UI
 *             _errorState.value = validation.error
 *         }
 *     }
 * }
 * ```
 *
 * ## Reglas de Validación
 *
 * 1. **Email**:
 *    - No puede estar vacío
 *    - Debe tener formato válido (contiene @)
 *    - Debe tener dominio (algo después de @)
 *
 * 2. **Password**:
 *    - No puede estar vacío
 *    - Debe tener al menos 8 caracteres
 *
 * @property email Correo electrónico del usuario
 * @property password Contraseña del usuario
 */
@Serializable
public data class LoginCredentials(
    @SerialName("email")
    val email: String,

    @SerialName("password")
    val password: String
) : ValidatableModel {

    /**
     * Valida la consistencia e integridad de las credenciales.
     *
     * ## Reglas de Validación
     *
     * 1. **Email**: No puede estar vacío y debe tener formato válido
     * 2. **Password**: No puede estar vacío y debe tener al menos 8 caracteres
     *
     * ## Ejemplos
     *
     * ```kotlin
     * // Credenciales válidas
     * val valid = LoginCredentials(
     *     email = "user@edugo.com",
     *     password = "password123"
     * )
     * valid.validate() // Result.Success
     *
     * // Email inválido
     * val invalidEmail = LoginCredentials(
     *     email = "notanemail",
     *     password = "password123"
     * )
     * invalidEmail.validate() // Result.Failure("Email must be a valid email address")
     *
     * // Password muy corta
     * val shortPassword = LoginCredentials(
     *     email = "user@edugo.com",
     *     password = "pass"
     * )
     * shortPassword.validate() // Result.Failure("Password must be at least 8 characters long")
     * ```
     *
     * @return [Result.Success] si todas las validaciones pasan,
     *         [Result.Failure] con mensaje descriptivo si alguna falla
     */
    override fun validate(): Result<Unit> {
        return when {
            email.isBlank() ->
                failure("Email cannot be blank")

            !isValidEmail(email) ->
                failure("Email must be a valid email address")

            password.isBlank() ->
                failure("Password cannot be blank")

            password.length < MIN_PASSWORD_LENGTH ->
                failure("Password must be at least $MIN_PASSWORD_LENGTH characters long")

            else ->
                success(Unit)
        }
    }

    /**
     * Valida que el email tenga un formato básico correcto.
     *
     * Esta es una validación básica que verifica:
     * - Contiene exactamente un @
     * - Tiene contenido antes del @
     * - Tiene contenido después del @ (dominio)
     * - El dominio contiene al menos un punto
     *
     * @param email Email a validar
     * @return true si el formato es válido
     */
    private fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return false

        // Verificar que contiene exactamente un @
        val atCount = trimmed.count { it == '@' }
        if (atCount != 1) return false

        // Separar en partes
        val parts = trimmed.split("@")
        if (parts.size != 2) return false

        val localPart = parts[0]
        val domainPart = parts[1]

        // Validar parte local (antes del @)
        if (localPart.isEmpty()) return false

        // Validar dominio (después del @)
        if (domainPart.isEmpty()) return false
        if (!domainPart.contains(".")) return false

        // Verificar que el dominio tiene contenido después del último punto
        val domainParts = domainPart.split(".")
        if (domainParts.any { it.isEmpty() }) return false

        return true
    }

    /**
     * Obtiene una representación segura para logging.
     *
     * **IMPORTANTE**: NO loguear passwords en producción.
     * Esta función solo muestra el email y oculta completamente la contraseña.
     *
     * Ejemplo: "LoginCredentials(email=user@edugo.com, password=***)"
     */
    public fun toLogString(): String {
        return "LoginCredentials(email=$email, password=***)"
    }

    companion object {
        /**
         * Longitud mínima requerida para la contraseña.
         */
        public const val MIN_PASSWORD_LENGTH: Int = 8

        /**
         * Crea credenciales de ejemplo para tests.
         *
         * **IMPORTANTE**: Solo usar en tests, nunca en producción.
         *
         * @param email Email de prueba (default: test@edugo.com)
         * @param password Password de prueba (default: password123)
         */
        public fun createTestCredentials(
            email: String = "test@edugo.com",
            password: String = "password123"
        ): LoginCredentials {
            return LoginCredentials(
                email = email,
                password = password
            )
        }
    }
}
