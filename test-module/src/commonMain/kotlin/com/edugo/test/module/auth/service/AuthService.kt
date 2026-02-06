package com.edugo.test.module.auth.service

import com.edugo.test.module.auth.model.*
import com.edugo.test.module.core.Result
import com.edugo.test.module.data.models.AuthToken
import com.edugo.test.module.network.interceptor.TokenProvider
import kotlinx.coroutines.flow.StateFlow

/**
 * Servicio principal de autenticación que gestiona el estado de sesión,
 * login/logout, y renovación de tokens.
 *
 * Este servicio actúa como la capa de aplicación en Clean Architecture,
 * orquestando operaciones entre el repository (datos), storage (persistencia)
 * y UI (estado reactivo).
 *
 * ## Responsabilidades
 *
 * 1. **Gestión de Estado**: Mantiene el estado reactivo de autenticación
 * 2. **Login/Logout**: Coordina autenticación y limpieza de sesión
 * 3. **Token Refresh**: Renueva tokens automáticamente cuando expiran
 * 4. **Persistencia**: Guarda/recupera sesión del storage local
 * 5. **Validación**: Valida credenciales antes de enviar al backend
 *
 * ## Estado Reactivo
 *
 * El servicio expone un [StateFlow] que emite cambios de estado:
 * - [AuthState.Unauthenticated]: Sin sesión activa
 * - [AuthState.Loading]: Operación de autenticación en progreso
 * - [AuthState.Authenticated]: Sesión activa con usuario y token
 *
 * ```kotlin
 * // En ViewModel
 * authService.authState.collect { state ->
 *     when (state) {
 *         is AuthState.Authenticated -> navigateToHome()
 *         is AuthState.Unauthenticated -> showLoginScreen()
 *         is AuthState.Loading -> showLoadingIndicator()
 *     }
 * }
 * ```
 *
 * ## Ejemplo de Uso Completo
 *
 * ```kotlin
 * // Crear servicio (típicamente en DI container)
 * val authService = AuthServiceFactory.create(
 *     httpClient = httpClient,
 *     storage = storage,
 *     baseUrl = "https://api.edugo.com"
 * )
 *
 * // Restaurar sesión al iniciar app
 * authService.restoreSession()
 *
 * // Login
 * val credentials = LoginCredentials(
 *     email = "user@edugo.com",
 *     password = "password123"
 * )
 * when (val result = authService.login(credentials)) {
 *     is LoginResult.Success -> {
 *         println("Logged in: ${result.response.user.fullName}")
 *     }
 *     is LoginResult.Error -> {
 *         println("Error: ${result.error.getUserFriendlyMessage()}")
 *     }
 * }
 *
 * // Logout
 * authService.logout()
 *
 * // Observar estado
 * authService.authState.collect { state ->
 *     if (state.isAuthenticated) {
 *         println("User: ${state.currentUser?.fullName}")
 *     }
 * }
 * ```
 *
 * ## Integración con HTTP Client
 *
 * El servicio implementa [TokenProvider] para integrarse directamente con
 * interceptores HTTP:
 *
 * ```kotlin
 * val authInterceptor = AuthInterceptor(
 *     tokenProvider = authService,  // AuthService IS-A TokenProvider
 *     autoRefresh = true
 * )
 * ```
 */
public interface AuthService : TokenProvider {
    /**
     * Estado reactivo de autenticación.
     *
     * Observa cambios en: Authenticated, Unauthenticated, Loading.
     *
     * Este StateFlow NUNCA emite null y siempre tiene un valor inicial
     * (Unauthenticated por defecto).
     */
    public val authState: StateFlow<AuthState>

    /**
     * Autentica un usuario con credenciales.
     *
     * Este método:
     * 1. Valida las credenciales localmente
     * 2. Emite [AuthState.Loading]
     * 3. Llama al repository para autenticar
     * 4. Guarda tokens y usuario en storage
     * 5. Emite [AuthState.Authenticated] o vuelve a [AuthState.Unauthenticated]
     *
     * ## Validaciones
     *
     * - Email no vacío y formato válido
     * - Password no vacío y mínimo 8 caracteres
     *
     * ## Errores Posibles
     *
     * - [AuthError.InvalidCredentials]: Credenciales incorrectas (401)
     * - [AuthError.UserNotFound]: Usuario no existe (404)
     * - [AuthError.AccountLocked]: Cuenta bloqueada (423)
     * - [AuthError.UserInactive]: Cuenta inactiva (403)
     * - [AuthError.NetworkError]: Error de red
     * - [AuthError.UnknownError]: Error inesperado
     *
     * @param credentials Credenciales del usuario (email + password)
     * @return [LoginResult.Success] con [LoginResponse] o [LoginResult.Error]
     */
    public suspend fun login(credentials: LoginCredentials): LoginResult

    /**
     * Cierra la sesión del usuario actual.
     *
     * Este método:
     * 1. Obtiene el token actual (si existe)
     * 2. Notifica al backend para invalidar la sesión (ignora errores)
     * 3. Limpia tokens y usuario del storage local
     * 4. Emite [AuthState.Unauthenticated]
     *
     * **IMPORTANTE**: El storage local se limpia SIEMPRE, incluso si
     * la llamada al backend falla. Esto garantiza que el usuario pueda
     * cerrar sesión incluso sin conexión a internet.
     *
     * @return [Result.Success] siempre (errores de backend se ignoran)
     */
    public suspend fun logout(): Result<Unit>

    /**
     * Renueva el access token usando el refresh token.
     *
     * Este método:
     * 1. Obtiene el refresh token del storage
     * 2. Llama al backend para renovar
     * 3. Guarda el nuevo access token
     * 4. Actualiza el estado si está autenticado
     *
     * Si el refresh falla (token expirado o inválido), se limpia
     * la sesión automáticamente.
     *
     * @return [Result.Success] con nuevo [AuthToken] o [Result.Failure]
     */
    public suspend fun refreshAuthToken(): Result<AuthToken>

    /**
     * Verifica si hay un usuario autenticado actualmente.
     *
     * Esta es una verificación rápida del estado actual sin I/O.
     *
     * @return true si el estado es [AuthState.Authenticated]
     */
    public fun isAuthenticated(): Boolean

    /**
     * Obtiene el token actual si existe.
     *
     * Si el token está expirado, intenta renovarlo automáticamente.
     *
     * Retorna null si:
     * - No hay sesión activa
     * - El token expiró y el refresh falló
     *
     * @return Token de acceso válido o null
     */
    override suspend fun getToken(): String?

    /**
     * Verifica si el token actual está expirado.
     *
     * @return true si el token está expirado o no existe
     */
    override suspend fun isTokenExpired(): Boolean

    /**
     * Obtiene el usuario actual si está autenticado.
     *
     * @return Información del usuario o null si no está autenticado
     */
    public fun getCurrentUser(): AuthUserInfo?

    /**
     * Obtiene el token completo actual si está autenticado.
     *
     * @return Token completo con access y refresh token, o null
     */
    public fun getCurrentAuthToken(): AuthToken?

    /**
     * Restaura la sesión desde storage si existe.
     *
     * Este método debe llamarse al iniciar la aplicación para
     * recuperar sesiones previas.
     *
     * Si el token guardado está expirado, intenta renovarlo
     * automáticamente. Si el refresh falla, limpia la sesión.
     *
     * ```kotlin
     * // En Application.onCreate() o equivalente
     * lifecycleScope.launch {
     *     authService.restoreSession()
     * }
     * ```
     */
    public suspend fun restoreSession()
}
