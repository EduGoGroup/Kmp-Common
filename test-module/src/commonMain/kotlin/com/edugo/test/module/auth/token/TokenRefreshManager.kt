package com.edugo.test.module.auth.token

import com.edugo.test.module.core.Result
import com.edugo.test.module.data.models.AuthToken
import kotlinx.coroutines.flow.Flow

/**
 * Manager que orquestra el refresh de tokens con sincronización, retry y manejo de errores robusto.
 *
 * Este manager coordina la renovación de tokens de autenticación, garantizando que:
 * 1. **Thread-Safety**: Múltiples llamadas concurrentes resultan en un solo refresh
 * 2. **Retry inteligente**: Reintenta automáticamente en errores de red con exponential backoff
 * 3. **Notificación de fallos**: Emite eventos cuando el refresh falla definitivamente
 * 4. **Token rotation**: Soporta actualización de refresh tokens si el backend lo provee
 *
 * ## Ejemplo de Uso Básico
 *
 * ```kotlin
 * val manager = TokenRefreshManagerImpl(
 *     repository = authRepository,
 *     storage = storage,
 *     config = TokenRefreshConfig.DEFAULT,
 *     scope = coroutineScope
 * )
 *
 * // Refrescar si es necesario (verifica expiración primero)
 * val result = manager.refreshIfNeeded()
 * when (result) {
 *     is Result.Success -> println("Token refrescado: ${result.data.token}")
 *     is Result.Failure -> println("Refresh falló: ${result.error}")
 * }
 *
 * // Forzar refresh independiente del estado
 * val forceResult = manager.forceRefresh()
 * ```
 *
 * ## Thread-Safety y Concurrencia
 *
 * El manager garantiza que múltiples llamadas concurrentes a refresh resultan
 * en una sola llamada al backend:
 *
 * ```kotlin
 * // Lanzar 10 refreshes concurrentes
 * val results = (1..10).map {
 *     async { manager.refreshIfNeeded() }
 * }.awaitAll()
 *
 * // Resultado: Solo 1 llamada al backend, todas reciben el mismo token
 * ```
 *
 * ## Manejo de Fallos
 *
 * Cuando el refresh falla definitivamente, el manager emite un evento:
 *
 * ```kotlin
 * // En el ViewModel o UseCase
 * manager.onRefreshFailed.collect { reason ->
 *     when (reason) {
 *         is RefreshFailureReason.TokenExpired -> navigateToLogin()
 *         is RefreshFailureReason.NetworkError -> showRetryDialog()
 *         // ... otros casos
 *     }
 * }
 * ```
 *
 * ## Integración con AuthService
 *
 * ```kotlin
 * class AuthServiceImpl(
 *     private val repository: AuthRepository,
 *     private val storage: SafeEduGoStorage,
 *     private val scope: CoroutineScope
 * ) : AuthService {
 *
 *     val tokenRefreshManager: TokenRefreshManager = TokenRefreshManagerImpl(
 *         repository = repository,
 *         storage = storage,
 *         config = TokenRefreshConfig.DEFAULT,
 *         scope = scope
 *     )
 *
 *     // Implementación de TokenProvider
 *     override suspend fun refreshToken(): String? {
 *         return tokenRefreshManager.forceRefresh().getOrNull()?.token
 *     }
 * }
 * ```
 */
public interface TokenRefreshManager {

    /**
     * Refresca el token solo si está próximo a expirar.
     *
     * Este método verifica primero si el token actual necesita renovación
     * (basándose en [TokenRefreshConfig.refreshThresholdSeconds]). Si no
     * necesita refresh, retorna el token actual sin hacer llamada al backend.
     *
     * **Thread-safe**: Múltiples llamadas concurrentes resultan en un solo
     * refresh real. Todas las llamadas esperan y reciben el mismo resultado.
     *
     * ## Flujo de Ejecución
     *
     * 1. Obtiene el token actual del storage
     * 2. Verifica si necesita refresh usando [shouldRefresh]
     * 3. Si NO necesita: retorna token actual (sin llamada al backend)
     * 4. Si SÍ necesita:
     *    - Verifica si ya hay un refresh en progreso
     *    - Si hay refresh en progreso: espera su resultado
     *    - Si no hay refresh: inicia nuevo refresh con retry y exponential backoff
     * 5. Guarda nuevo token en storage
     * 6. Retorna nuevo token
     *
     * ## Ejemplo
     *
     * ```kotlin
     * // En AuthInterceptor, antes de cada request
     * override suspend fun interceptRequest(request: HttpRequestBuilder) {
     *     val result = tokenRefreshManager.refreshIfNeeded()
     *     when (result) {
     *         is Result.Success -> {
     *             request.header("Authorization", "Bearer ${result.data.token}")
     *         }
     *         is Result.Failure -> {
     *             // Token no se pudo refrescar, el request fallará con 401
     *             println("Warning: Could not refresh token: ${result.error}")
     *         }
     *     }
     * }
     * ```
     *
     * @return [Result.Success] con [AuthToken] si el token es válido o se refrescó exitosamente,
     *         [Result.Failure] si no hay token o el refresh falló
     */
    public suspend fun refreshIfNeeded(): Result<AuthToken>

    /**
     * Fuerza refresh independiente del estado actual del token.
     *
     * Este método ignora el estado de expiración del token y siempre
     * intenta renovarlo. Útil para:
     * - Responder a 401 en requests HTTP
     * - Refresh manual iniciado por el usuario
     * - Testing
     *
     * **Thread-safe**: Al igual que [refreshIfNeeded], múltiples llamadas
     * concurrentes resultan en un solo refresh real.
     *
     * ## Diferencia con refreshIfNeeded
     *
     * - **refreshIfNeeded**: Verifica expiración primero, puede no hacer nada
     * - **forceRefresh**: SIEMPRE intenta refrescar, ignora estado de expiración
     *
     * ## Ejemplo
     *
     * ```kotlin
     * // En HttpCallValidator, al recibir 401
     * install(HttpCallValidator) {
     *     handleResponseExceptionWithRequest { exception, request ->
     *         if (exception is ClientRequestException &&
     *             exception.response.status == HttpStatusCode.Unauthorized) {
     *
     *             // Intentar refresh forzado
     *             val result = tokenRefreshManager.forceRefresh()
     *             if (result.isSuccess) {
     *                 // Retry request con nuevo token
     *                 return@handleResponseExceptionWithRequest
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * @return [Result.Success] con [AuthToken] si el refresh fue exitoso,
     *         [Result.Failure] si no hay refresh token o el refresh falló
     */
    public suspend fun forceRefresh(): Result<AuthToken>

    /**
     * Evalúa si el token debería refrescarse.
     *
     * Determina si un token está lo suficientemente cerca de su expiración
     * como para necesitar renovación, basándose en el threshold configurado.
     *
     * ## Lógica de Evaluación
     *
     * Un token debe refrescarse si:
     * ```
     * tiempoRestante <= refreshThresholdSeconds
     * ```
     *
     * ## Ejemplo
     *
     * ```kotlin
     * val config = TokenRefreshConfig(refreshThresholdSeconds = 300) // 5 min
     * val manager = TokenRefreshManagerImpl(..., config)
     * val token = getCurrentToken()
     *
     * // Token expira en 10 minutos
     * manager.shouldRefresh(token)  // false (aún tiene tiempo)
     *
     * // Token expira en 3 minutos
     * manager.shouldRefresh(token)  // true (< 5 min threshold)
     *
     * // Token ya expiró
     * manager.shouldRefresh(token)  // true (expirado)
     * ```
     *
     * @param token Token a evaluar
     * @return true si el token debería refrescarse, false si aún tiene tiempo suficiente
     */
    public fun shouldRefresh(token: AuthToken): Boolean

    /**
     * Flow que emite cuando el refresh falla definitivamente.
     *
     * Este flow emite [RefreshFailureReason] cuando:
     * - Se agotaron todos los reintentos (errores de red)
     * - El token está expirado o revocado (no retriable)
     * - No hay refresh token disponible
     *
     * **IMPORTANTE**: Este flow NO emite en fallos recuperables que
     * serán reintentados automáticamente.
     *
     * ## Ejemplo de Observación en ViewModel
     *
     * ```kotlin
     * class MainViewModel(
     *     private val authService: AuthService
     * ) : ViewModel() {
     *
     *     init {
     *         viewModelScope.launch {
     *             authService.tokenRefreshManager.onRefreshFailed.collect { reason ->
     *                 when (reason) {
     *                     is RefreshFailureReason.TokenExpired,
     *                     is RefreshFailureReason.TokenRevoked -> {
     *                         // Sesión expiró, navegar a login
     *                         _navigationEvent.emit(NavigationEvent.ToLogin)
     *                     }
     *                     is RefreshFailureReason.NetworkError -> {
     *                         // Error de red, mostrar snackbar
     *                         _snackbarMessage.emit("Error de conexión. Intenta nuevamente.")
     *                     }
     *                     // ... otros casos
     *                 }
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * ## Ejemplo de Integración con AuthService
     *
     * ```kotlin
     * class AuthServiceImpl(...) : AuthService {
     *
     *     val tokenRefreshManager = TokenRefreshManagerImpl(...)
     *
     *     private val _onSessionExpired = MutableSharedFlow<Unit>()
     *     val onSessionExpired: Flow<Unit> = _onSessionExpired.asSharedFlow()
     *
     *     init {
     *         scope.launch {
     *             tokenRefreshManager.onRefreshFailed.collect { reason ->
     *                 when (reason) {
     *                     is RefreshFailureReason.TokenExpired,
     *                     is RefreshFailureReason.TokenRevoked,
     *                     is RefreshFailureReason.NoRefreshToken -> {
     *                         clearSession()
     *                         _onSessionExpired.emit(Unit)
     *                     }
     *                     else -> {
     *                         // No limpiar sesión en errores temporales
     *                     }
     *                 }
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * @return Flow que emite razones de fallo definitivo
     */
    public val onRefreshFailed: Flow<RefreshFailureReason>
}
