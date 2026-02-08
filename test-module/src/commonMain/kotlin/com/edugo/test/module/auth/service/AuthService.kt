package com.edugo.test.module.auth.service

import com.edugo.test.module.auth.model.AuthError
import com.edugo.test.module.auth.model.AuthUserInfo
import com.edugo.test.module.auth.model.LoginCredentials
import com.edugo.test.module.auth.model.LoginResult
import com.edugo.test.module.auth.model.LogoutResult
import com.edugo.test.module.auth.token.TokenRefreshManager
import com.edugo.test.module.core.Result
import com.edugo.test.module.data.models.AuthToken
import com.edugo.test.module.network.interceptor.TokenProvider
import kotlinx.coroutines.flow.Flow
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
     * Manager para refresh de tokens con sincronización y retry.
     *
     * Proporciona acceso al [TokenRefreshManager] para casos avanzados
     * como refresh manual, verificación de estado, u observación de fallos.
     *
     * ## Ejemplo de Uso
     *
     * ```kotlin
     * // Verificar si un token necesita refresh
     * val token = authService.getCurrentAuthToken()
     * if (token != null && authService.tokenRefreshManager.shouldRefresh(token)) {
     *     println("Token necesita renovación")
     * }
     *
     * // Observar fallos de refresh
     * authService.tokenRefreshManager.onRefreshFailed.collect { reason ->
     *     when (reason) {
     *         is RefreshFailureReason.TokenExpired -> navigateToLogin()
     *         is RefreshFailureReason.NetworkError -> showRetryDialog()
     *         // ... otros casos
     *     }
     * }
     * ```
     */
    public val tokenRefreshManager: TokenRefreshManager

    /**
     * Flow que emite cuando la sesión expira y no se puede renovar.
     *
     * Este flow emite Unit cuando:
     * - El refresh token ha expirado
     * - El refresh token fue revocado
     * - No hay refresh token disponible
     *
     * **La UI debe observar esto para navegar a login.**
     *
     * **IMPORTANTE**: Este flow NO emite en errores de red temporales.
     * Solo emite cuando la sesión está definitivamente expirada y el
     * usuario debe volver a autenticarse.
     *
     * ## Ejemplo en ViewModel
     *
     * ```kotlin
     * class MainViewModel(
     *     private val authService: AuthService
     * ) : ViewModel() {
     *
     *     init {
     *         viewModelScope.launch {
     *             authService.onSessionExpired.collect {
     *                 // Sesión expiró, navegar a login
     *                 _navigationEvent.emit(NavigationEvent.ToLogin)
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * ## Ejemplo en Compose
     *
     * ```kotlin
     * @Composable
     * fun MainScreen(authService: AuthService) {
     *     val navigator = rememberNavigator()
     *
     *     LaunchedEffect(Unit) {
     *         authService.onSessionExpired.collect {
     *             navigator.navigate(Screen.Login) {
     *                 popUpTo(Screen.Main) { inclusive = true }
     *             }
     *         }
     *     }
     *
     *     // ... resto de la UI
     * }
     * ```
     */
    public val onSessionExpired: Flow<Unit>

    /**
     * Flow que emite cuando el usuario hace logout EXPLÍCITO.
     *
     * Este flow es diferente de [onSessionExpired]:
     * - **onLogout**: Usuario hace logout manualmente (botón "Cerrar sesión")
     * - **onSessionExpired**: Sesión expira automáticamente (token expirado/revocado)
     *
     * Emite el resultado del logout, que puede ser:
     * - [LogoutResult.Success]: Logout completo (local + remoto)
     * - [LogoutResult.PartialSuccess]: Local limpiado, pero backend falló
     * - [LogoutResult.AlreadyLoggedOut]: Ya estaba deslogueado (idempotente)
     *
     * La UI puede usar este flow para mostrar mensajes diferenciados:
     *
     * **Ejemplo**:
     * ```kotlin
     * authService.onLogout.collect { result ->
     *     when (result) {
     *         is LogoutResult.Success -> {
     *             showMessage("Sesión cerrada exitosamente")
     *             navigateToLogin()
     *         }
     *         is LogoutResult.PartialSuccess -> {
     *             showWarning("Sesión cerrada localmente. ${result.remoteError}")
     *             navigateToLogin()
     *         }
     *         is LogoutResult.AlreadyLoggedOut -> {
     *             // No hacer nada, ya estaba deslogueado
     *         }
     *     }
     * }
     * ```
     */
    public val onLogout: Flow<LogoutResult>

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
     * **DEPRECATED**: Usa [logoutWithDetails] para obtener información
     * detallada sobre el resultado del logout.
     *
     * @return [Result.Success] siempre (errores de backend se ignoran)
     */
    public suspend fun logout(): Result<Unit>

    /**
     * Cierra la sesión del usuario con información detallada del resultado.
     *
     * Este método extiende el [logout] básico con:
     * - **Idempotencia**: Múltiples llamadas son seguras
     * - **Soporte offline**: Logout local aunque el backend falle
     * - **Cancelación de refreshes**: Cancela operaciones pendientes
     * - **Resultado detallado**: Distingue éxito total, parcial e idempotente
     *
     * ## Flujo de Ejecución
     *
     * 1. **Verificar idempotencia**: Si ya está en [AuthState.Unauthenticated],
     *    retorna [LogoutResult.AlreadyLoggedOut] sin hacer nada
     * 2. **Intentar logout remoto**: Llama al backend (POST /v1/auth/logout)
     * 3. **Decidir limpieza local**: Basado en [forceLocal] y resultado remoto
     * 4. **Cancelar refreshes**: Cancela [TokenRefreshManager.cancelPendingRefresh]
     * 5. **Limpiar storage**: Elimina tokens y usuario del storage
     * 6. **Emitir estado**: Cambia a [AuthState.Unauthenticated]
     * 7. **Emitir evento**: Emite resultado en [onLogout]
     *
     * ## Parámetros
     *
     * @param forceLocal Si `true` (default), limpia datos locales aunque el
     *                   logout remoto falle. Esto garantiza logout offline.
     *                   Si `false`, solo limpia local si remoto es exitoso.
     *
     * ## Retorno
     *
     * - [LogoutResult.Success]: Logout completo (local + remoto exitosos)
     * - [LogoutResult.PartialSuccess]: Local limpiado, pero backend falló (offline)
     * - [LogoutResult.AlreadyLoggedOut]: Ya estaba deslogueado (idempotente)
     *
     * ## Ejemplos
     *
     * ```kotlin
     * // Logout normal (default: forceLocal=true)
     * val result = authService.logoutWithDetails()
     * when (result) {
     *     is LogoutResult.Success -> {
     *         showMessage("Sesión cerrada exitosamente")
     *         navigateToLogin()
     *     }
     *     is LogoutResult.PartialSuccess -> {
     *         showWarning("Sin conexión. Sesión cerrada localmente.")
     *         navigateToLogin()
     *     }
     *     is LogoutResult.AlreadyLoggedOut -> {
     *         // Ya estaba deslogueado, no hacer nada
     *     }
     * }
     *
     * // Logout que requiere confirmación del servidor
     * val result = authService.logoutWithDetails(forceLocal = false)
     * if (result is LogoutResult.PartialSuccess) {
     *     // Backend falló, usuario sigue "autenticado" localmente
     *     showError("No se pudo cerrar sesión. Verifica tu conexión.")
     * }
     * ```
     *
     * ## Diferencia con logout()
     *
     * - **logout()**: Retorna `Result<Unit>`, siempre `Success`
     * - **logoutWithDetails()**: Retorna `LogoutResult` con información detallada
     *
     * ## Thread-Safety
     *
     * Este método es thread-safe. Múltiples llamadas concurrentes son seguras
     * gracias al uso de Mutex interno.
     */
    public suspend fun logoutWithDetails(forceLocal: Boolean = true): LogoutResult

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
