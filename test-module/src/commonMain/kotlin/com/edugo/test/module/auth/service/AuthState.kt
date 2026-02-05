package com.edugo.test.module.auth.service

import com.edugo.test.module.auth.model.AuthUserInfo
import com.edugo.test.module.data.models.AuthToken

/**
 * Sealed class que representa el estado de autenticación del usuario.
 *
 * Esta clase modela los diferentes estados posibles durante el ciclo de vida
 * de la autenticación, permitiendo que la UI reaccione a cambios de estado
 * de manera type-safe.
 *
 * ## Estados Posibles
 *
 * 1. **Authenticated**: Usuario autenticado con token válido
 * 2. **Unauthenticated**: Usuario no autenticado o sesión expirada
 * 3. **Loading**: Operación de autenticación en progreso
 *
 * ## Flujo de Estados
 *
 * ```
 * Inicio → Unauthenticated
 *            ↓ login()
 *          Loading
 *            ↓ success
 *       Authenticated ←──┐
 *            ↓ logout    │ refreshToken()
 *       Unauthenticated  │
 *            ↓ login     │
 *          Loading ──────┘
 * ```
 *
 * ## Ejemplo de Uso en ViewModel
 *
 * ```kotlin
 * class AuthViewModel(private val authService: AuthService) : ViewModel() {
 *     val authState: StateFlow<AuthState> = authService.authState
 *
 *     init {
 *         viewModelScope.launch {
 *             authState.collect { state ->
 *                 when (state) {
 *                     is AuthState.Authenticated -> {
 *                         navigateToHome()
 *                         showWelcome(state.user.fullName)
 *                     }
 *                     is AuthState.Unauthenticated -> {
 *                         navigateToLogin()
 *                     }
 *                     is AuthState.Loading -> {
 *                         showLoadingIndicator()
 *                     }
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Ejemplo de Uso en Composable
 *
 * ```kotlin
 * @Composable
 * fun AuthScreen(authService: AuthService) {
 *     val authState by authService.authState.collectAsState()
 *
 *     when (val state = authState) {
 *         is AuthState.Authenticated -> {
 *             HomeScreen(user = state.user)
 *         }
 *         is AuthState.Unauthenticated -> {
 *             LoginScreen()
 *         }
 *         is AuthState.Loading -> {
 *             LoadingScreen()
 *         }
 *     }
 * }
 * ```
 *
 * ## Extension Properties
 *
 * Para facilitar el trabajo con estados, se proveen extension properties:
 *
 * ```kotlin
 * val isLoggedIn = authState.isAuthenticated
 * val currentUserName = authState.currentUser?.fullName
 * val accessToken = authState.currentToken?.token
 * ```
 */
public sealed class AuthState {

    /**
     * Estado autenticado con información del usuario y token.
     *
     * Este estado indica que el usuario ha iniciado sesión exitosamente
     * y tiene un token de acceso válido.
     *
     * @property user Información del usuario autenticado
     * @property token Token de acceso y refresh
     */
    public data class Authenticated(
        val user: AuthUserInfo,
        val token: AuthToken
    ) : AuthState() {
        /**
         * Verifica si el token ha expirado.
         *
         * Útil para determinar si se necesita renovar el token.
         *
         * @return true si el token ha expirado
         */
        public fun isTokenExpired(): Boolean = token.isExpired()

        /**
         * Verifica si hay refresh token disponible.
         *
         * @return true si hay refresh token
         */
        public fun canRefresh(): Boolean = token.hasRefreshToken()
    }

    /**
     * Estado no autenticado.
     *
     * Este estado indica que el usuario no ha iniciado sesión o
     * su sesión ha expirado/sido cerrada.
     */
    public object Unauthenticated : AuthState()

    /**
     * Estado de carga durante operaciones de autenticación.
     *
     * Este estado se activa durante operaciones asíncronas como
     * login, logout o refresh de token.
     */
    public object Loading : AuthState()
}

/**
 * Verifica si el estado actual es autenticado.
 *
 * ```kotlin
 * if (authState.isAuthenticated) {
 *     // Usuario tiene sesión activa
 * }
 * ```
 *
 * @return true si el estado es Authenticated
 */
public val AuthState.isAuthenticated: Boolean
    get() = this is AuthState.Authenticated

/**
 * Verifica si el estado actual es no autenticado.
 *
 * @return true si el estado es Unauthenticated
 */
public val AuthState.isUnauthenticated: Boolean
    get() = this is AuthState.Unauthenticated

/**
 * Verifica si el estado actual es loading.
 *
 * @return true si el estado es Loading
 */
public val AuthState.isLoading: Boolean
    get() = this is AuthState.Loading

/**
 * Obtiene el usuario actual si está autenticado.
 *
 * ```kotlin
 * val userName = authState.currentUser?.fullName ?: "Guest"
 * ```
 *
 * @return AuthUserInfo si está autenticado, null en caso contrario
 */
public val AuthState.currentUser: AuthUserInfo?
    get() = (this as? AuthState.Authenticated)?.user

/**
 * Obtiene el token actual si está autenticado.
 *
 * ```kotlin
 * val accessToken = authState.currentToken?.token
 * ```
 *
 * @return AuthToken si está autenticado, null en caso contrario
 */
public val AuthState.currentToken: AuthToken?
    get() = (this as? AuthState.Authenticated)?.token

/**
 * Obtiene el ID del usuario actual si está autenticado.
 *
 * @return ID del usuario o null
 */
public val AuthState.currentUserId: String?
    get() = currentUser?.id

/**
 * Obtiene el email del usuario actual si está autenticado.
 *
 * @return Email del usuario o null
 */
public val AuthState.currentUserEmail: String?
    get() = currentUser?.email

/**
 * Obtiene el rol del usuario actual si está autenticado.
 *
 * @return Rol del usuario o null
 */
public val AuthState.currentUserRole: String?
    get() = currentUser?.role

/**
 * Verifica si el token actual ha expirado.
 *
 * ```kotlin
 * if (authState.isTokenExpired) {
 *     authService.refreshToken()
 * }
 * ```
 *
 * @return true si está autenticado y el token ha expirado, false en caso contrario
 */
public val AuthState.isTokenExpired: Boolean
    get() = (this as? AuthState.Authenticated)?.isTokenExpired() ?: false

/**
 * Verifica si se puede refrescar el token.
 *
 * @return true si está autenticado y tiene refresh token disponible
 */
public val AuthState.canRefreshToken: Boolean
    get() = (this as? AuthState.Authenticated)?.canRefresh() ?: false

/**
 * Aplica una acción solo si el estado es Authenticated.
 *
 * Útil para operaciones que requieren usuario autenticado.
 *
 * ```kotlin
 * authState.ifAuthenticated { user, token ->
 *     println("User ${user.email} is authenticated")
 *     apiClient.setAuthToken(token.token)
 * }
 * ```
 *
 * @param action Acción a ejecutar con user y token
 */
public inline fun AuthState.ifAuthenticated(action: (AuthUserInfo, AuthToken) -> Unit) {
    if (this is AuthState.Authenticated) {
        action(user, token)
    }
}

/**
 * Aplica una acción solo si el estado es Unauthenticated.
 *
 * ```kotlin
 * authState.ifUnauthenticated {
 *     navigateToLogin()
 * }
 * ```
 *
 * @param action Acción a ejecutar
 */
public inline fun AuthState.ifUnauthenticated(action: () -> Unit) {
    if (this is AuthState.Unauthenticated) {
        action()
    }
}

/**
 * Aplica una acción solo si el estado es Loading.
 *
 * ```kotlin
 * authState.ifLoading {
 *     showProgressBar()
 * }
 * ```
 *
 * @param action Acción a ejecutar
 */
public inline fun AuthState.ifLoading(action: () -> Unit) {
    if (this is AuthState.Loading) {
        action()
    }
}

/**
 * Pattern matching sobre el estado de autenticación.
 *
 * Permite manejar todos los casos de forma exhaustiva.
 *
 * ```kotlin
 * val message = authState.fold(
 *     onAuthenticated = { user, token -> "Welcome ${user.fullName}" },
 *     onUnauthenticated = { "Please login" },
 *     onLoading = { "Loading..." }
 * )
 * ```
 *
 * @param onAuthenticated Acción para estado Authenticated
 * @param onUnauthenticated Acción para estado Unauthenticated
 * @param onLoading Acción para estado Loading
 * @return Resultado de aplicar la acción correspondiente
 */
public inline fun <R> AuthState.fold(
    onAuthenticated: (AuthUserInfo, AuthToken) -> R,
    onUnauthenticated: () -> R,
    onLoading: () -> R
): R {
    return when (this) {
        is AuthState.Authenticated -> onAuthenticated(user, token)
        is AuthState.Unauthenticated -> onUnauthenticated()
        is AuthState.Loading -> onLoading()
    }
}
