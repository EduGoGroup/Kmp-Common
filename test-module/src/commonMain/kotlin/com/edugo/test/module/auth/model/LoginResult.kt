package com.edugo.test.module.auth.model

/**
 * Sealed class que representa el resultado de una operación de login.
 *
 * Esta clase proporciona un tipo específico del dominio de autenticación
 * para representar el resultado de un intento de login, encapsulando tanto
 * el caso de éxito con [LoginResponse] como el caso de error con [AuthError].
 *
 * ## Diseño y Arquitectura
 *
 * **DECISIÓN DE DISEÑO**: LoginResult es complementario a Result<T>:
 * - **Result<T>**: Tipo genérico usado internamente en repositorios y servicios
 * - **LoginResult**: Tipo específico del dominio expuesto en la API pública de AuthService
 *
 * Esta separación permite:
 * 1. **Type-safety**: El compilador garantiza que solo LoginResponse o AuthError son posibles
 * 2. **Expresividad**: El nombre LoginResult comunica claramente su propósito
 * 3. **Documentación viva**: El código documenta los posibles resultados de login
 *
 * ## Ejemplo de Uso en AuthService
 *
 * ```kotlin
 * class AuthServiceImpl : AuthService {
 *     override suspend fun login(credentials: LoginCredentials): LoginResult {
 *         // Validar credenciales
 *         when (val validation = credentials.validate()) {
 *             is Result.Failure ->
 *                 return LoginResult.Error(AuthError.UnknownError(validation.error))
 *             else -> Unit
 *         }
 *
 *         // Llamar repository
 *         return when (val result = repository.login(credentials)) {
 *             is Result.Success -> {
 *                 val response = result.data
 *
 *                 // Guardar tokens y user
 *                 saveAuthData(response)
 *
 *                 // Actualizar estado
 *                 _authState.value = AuthState.Authenticated(
 *                     user = response.user,
 *                     token = response.toAuthToken()
 *                 )
 *
 *                 LoginResult.Success(response)
 *             }
 *             is Result.Failure -> {
 *                 val error = AuthError.fromMessage(result.error)
 *                 LoginResult.Error(error)
 *             }
 *             is Result.Loading -> {
 *                 // No debería ocurrir en suspend function
 *                 LoginResult.Error(AuthError.UnknownError("Unexpected loading state"))
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Ejemplo de Uso en UI/ViewModel
 *
 * ```kotlin
 * // En ViewModel
 * fun onLoginClick(email: String, password: String) {
 *     viewModelScope.launch {
 *         _uiState.value = UiState.Loading
 *
 *         val credentials = LoginCredentials(email, password)
 *
 *         when (val result = authService.login(credentials)) {
 *             is LoginResult.Success -> {
 *                 _uiState.value = UiState.Success
 *                 navigateToHome()
 *             }
 *             is LoginResult.Error -> {
 *                 val message = result.error.getUserFriendlyMessage()
 *                 _uiState.value = UiState.Error(message)
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Pattern Matching con when
 *
 * ```kotlin
 * val message = when (val result = authService.login(credentials)) {
 *     is LoginResult.Success ->
 *         "Bienvenido, ${result.response.user.fullName}"
 *     is LoginResult.Error ->
 *         result.error.getUserFriendlyMessage()
 * }
 * ```
 */
public sealed class LoginResult {
    /**
     * Representa un login exitoso.
     *
     * Contiene la respuesta completa del backend incluyendo tokens
     * e información del usuario.
     *
     * @property response Respuesta del backend con tokens y datos del usuario
     */
    public data class Success(val response: LoginResponse) : LoginResult()

    /**
     * Representa un login fallido.
     *
     * Contiene el error específico que causó la falla.
     *
     * @property error Error de autenticación que describe la causa de la falla
     */
    public data class Error(val error: AuthError) : LoginResult()

    /**
     * Verifica si el resultado es exitoso.
     *
     * @return true si es Success, false si es Error
     */
    public fun isSuccess(): Boolean = this is Success

    /**
     * Verifica si el resultado es un error.
     *
     * @return true si es Error, false si es Success
     */
    public fun isError(): Boolean = this is Error

    /**
     * Obtiene la respuesta si es Success, o null si es Error.
     *
     * Útil para extraer el valor sin pattern matching explícito.
     *
     * ```kotlin
     * val response = loginResult.getOrNull()
     * response?.let {
     *     println("Login exitoso: ${it.user.email}")
     * }
     * ```
     *
     * @return LoginResponse si es Success, null si es Error
     */
    public fun getOrNull(): LoginResponse? = when (this) {
        is Success -> response
        is Error -> null
    }

    /**
     * Obtiene el error si es Error, o null si es Success.
     *
     * ```kotlin
     * val error = loginResult.getErrorOrNull()
     * error?.let {
     *     println("Login falló: ${it.getUserFriendlyMessage()}")
     * }
     * ```
     *
     * @return AuthError si es Error, null si es Success
     */
    public fun getErrorOrNull(): AuthError? = when (this) {
        is Success -> null
        is Error -> error
    }

    /**
     * Aplica una función si es Success, retornando un nuevo LoginResult.
     *
     * Similar a map en Result<T>.
     *
     * ```kotlin
     * val result: LoginResult = authService.login(credentials)
     * val transformed = result.map { response ->
     *     // Transformar o procesar response
     *     response.copy(user = response.user.copy(email = response.user.email.lowercase()))
     * }
     * ```
     *
     * @param transform Función para transformar la respuesta
     * @return Nuevo LoginResult con la respuesta transformada, o el mismo Error
     */
    public inline fun map(transform: (LoginResponse) -> LoginResponse): LoginResult {
        return when (this) {
            is Success -> Success(transform(response))
            is Error -> this
        }
    }

    /**
     * Aplica una función a ambos casos (Success y Error).
     *
     * Permite manejar ambos casos en una sola expresión.
     *
     * ```kotlin
     * val message = loginResult.fold(
     *     onSuccess = { response -> "Bienvenido ${response.user.fullName}" },
     *     onError = { error -> error.getUserFriendlyMessage() }
     * )
     * ```
     *
     * @param onSuccess Función para el caso Success
     * @param onError Función para el caso Error
     * @return Resultado de aplicar la función correspondiente
     */
    public inline fun <R> fold(
        onSuccess: (LoginResponse) -> R,
        onError: (AuthError) -> R
    ): R {
        return when (this) {
            is Success -> onSuccess(response)
            is Error -> onError(error)
        }
    }

    /**
     * Ejecuta una acción solo si es Success.
     *
     * Útil para efectos secundarios (logging, analytics, etc.).
     *
     * ```kotlin
     * loginResult.onSuccess { response ->
     *     analytics.logEvent("login_success", mapOf("userId" to response.user.id))
     * }
     * ```
     *
     * @param action Acción a ejecutar con la respuesta
     * @return El mismo LoginResult para encadenar llamadas
     */
    public inline fun onSuccess(action: (LoginResponse) -> Unit): LoginResult {
        if (this is Success) {
            action(response)
        }
        return this
    }

    /**
     * Ejecuta una acción solo si es Error.
     *
     * Útil para logging de errores, analytics, etc.
     *
     * ```kotlin
     * loginResult.onError { error ->
     *     analytics.logEvent("login_error", mapOf("errorCode" to error.errorCode.code))
     * }
     * ```
     *
     * @param action Acción a ejecutar con el error
     * @return El mismo LoginResult para encadenar llamadas
     */
    public inline fun onError(action: (AuthError) -> Unit): LoginResult {
        if (this is Error) {
            action(error)
        }
        return this
    }

    /**
     * Convierte este LoginResult a Result<LoginResponse>.
     *
     * Útil para interoperabilidad con APIs que usan Result<T> genérico.
     *
     * ```kotlin
     * val loginResult: LoginResult = authService.login(credentials)
     * val genericResult: Result<LoginResponse> = loginResult.toResult()
     * ```
     *
     * @return Result.Success si es Success, Result.Failure si es Error
     */
    public fun toResult(): com.edugo.test.module.core.Result<LoginResponse> {
        return when (this) {
            is Success -> com.edugo.test.module.core.Result.Success(response)
            is Error -> com.edugo.test.module.core.Result.Failure(error.getUserFriendlyMessage())
        }
    }

    companion object {
        /**
         * Crea un LoginResult.Success desde una LoginResponse.
         *
         * Factory method para claridad en el código.
         *
         * ```kotlin
         * val result = LoginResult.success(loginResponse)
         * ```
         */
        public fun success(response: LoginResponse): LoginResult = Success(response)

        /**
         * Crea un LoginResult.Error desde un AuthError.
         *
         * Factory method para claridad en el código.
         *
         * ```kotlin
         * val result = LoginResult.error(AuthError.InvalidCredentials)
         * ```
         */
        public fun error(error: AuthError): LoginResult = Error(error)

        /**
         * Convierte un Result<LoginResponse> a LoginResult.
         *
         * Útil para convertir resultados internos de repository a la API pública.
         *
         * ```kotlin
         * val repositoryResult: Result<LoginResponse> = repository.login(credentials)
         * val loginResult: LoginResult = LoginResult.fromResult(repositoryResult)
         * ```
         *
         * @param result Result genérico a convertir
         * @return LoginResult correspondiente
         */
        public fun fromResult(result: com.edugo.test.module.core.Result<LoginResponse>): LoginResult {
            return when (result) {
                is com.edugo.test.module.core.Result.Success -> Success(result.data)
                is com.edugo.test.module.core.Result.Failure -> {
                    val error = AuthError.fromMessage(result.error)
                    Error(error)
                }
                is com.edugo.test.module.core.Result.Loading -> {
                    // Loading no tiene equivalente en LoginResult
                    Error(AuthError.UnknownError("Unexpected loading state"))
                }
            }
        }
    }
}
