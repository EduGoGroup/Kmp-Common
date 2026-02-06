package com.edugo.test.module.auth.service

import com.edugo.test.module.auth.model.*
import com.edugo.test.module.auth.repository.AuthRepository
import com.edugo.test.module.auth.token.RefreshFailureReason
import com.edugo.test.module.auth.token.TokenRefreshConfig
import com.edugo.test.module.auth.token.TokenRefreshManager
import com.edugo.test.module.auth.token.TokenRefreshManagerImpl
import com.edugo.test.module.core.Result
import com.edugo.test.module.data.models.AuthToken
import com.edugo.test.module.storage.SafeEduGoStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Implementación de [AuthService] con gestión de estado reactivo y persistencia.
 *
 * Esta implementación es thread-safe usando [Mutex] de kotlinx.coroutines,
 * compatible con todas las plataformas KMP (Android, iOS, Desktop, Web).
 *
 * ## Thread-Safety
 *
 * Todas las operaciones que modifican el estado están protegidas con un [Mutex]
 * para garantizar consistencia en entornos multi-threaded.
 *
 * **IMPORTANTE**: NO usar `synchronized`, `AtomicReference`, u otras APIs
 * específicas de JVM que no son multiplataforma.
 *
 * ## Persistencia
 *
 * El servicio guarda automáticamente:
 * - AuthToken en "auth_token" (JSON serializado)
 * - AuthUserInfo en "auth_user" (JSON serializado)
 *
 * ## Seguridad
 *
 * - NO loguea tokens completos (solo previews)
 * - Limpia storage automáticamente en logout
 * - Invalida sesión si el refresh falla
 *
 * @property repository Repositorio para operaciones de red
 * @property storage Storage seguro para persistencia
 * @property json Instancia de Json para serialización
 * @property scope CoroutineScope para operaciones asíncronas (observar eventos, etc.)
 * @property refreshConfig Configuración para el TokenRefreshManager
 */
public class AuthServiceImpl(
    private val repository: AuthRepository,
    private val storage: SafeEduGoStorage,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    refreshConfig: TokenRefreshConfig = TokenRefreshConfig.DEFAULT
) : AuthService {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Mutex para thread-safety en operaciones de estado (KMP-compatible)
    private val stateMutex = Mutex()

    // TokenRefreshManager para refresh automático
    override val tokenRefreshManager: TokenRefreshManager = TokenRefreshManagerImpl(
        repository = repository,
        storage = storage,
        config = refreshConfig,
        scope = scope,
        json = json
    )

    // Flow para notificar expiración de sesión
    private val _onSessionExpired = MutableSharedFlow<Unit>(replay = 0)
    override val onSessionExpired: Flow<Unit> = _onSessionExpired.asSharedFlow()

    init {
        // Observar fallos de refresh para limpiar sesión cuando sea necesario
        scope.launch {
            tokenRefreshManager.onRefreshFailed.collect { reason ->
                when (reason) {
                    is RefreshFailureReason.TokenExpired,
                    is RefreshFailureReason.TokenRevoked,
                    is RefreshFailureReason.NoRefreshToken -> {
                        // Sesión irrecuperable: limpiar y notificar
                        clearSession()
                        _onSessionExpired.emit(Unit)
                    }
                    is RefreshFailureReason.NetworkError -> {
                        // Error de red: no limpiar sesión, UI puede reintentar
                        // Solo loguear
                        println("TokenRefresh: Network error during refresh: ${reason.cause}")
                    }
                    is RefreshFailureReason.ServerError -> {
                        // Error de servidor: loguear pero no limpiar sesión
                        println("TokenRefresh: Server error during refresh: ${reason.code} - ${reason.message}")
                    }
                }
            }
        }
    }

    /**
     * Limpia la sesión localmente sin notificar al backend.
     */
    private suspend fun clearSession() {
        stateMutex.withLock {
            clearAuthData()
            _authState.value = AuthState.Unauthenticated
        }
    }

    public companion object {
        private const val AUTH_TOKEN_KEY = "auth_token"
        private const val AUTH_USER_KEY = "auth_user"
    }

    override suspend fun login(credentials: LoginCredentials): LoginResult {
        return stateMutex.withLock {
            // 1. Emitir Loading
            _authState.value = AuthState.Loading

            // 2. Validar credenciales localmente
            val validationResult = credentials.validate()
            if (validationResult is Result.Failure) {
                _authState.value = AuthState.Unauthenticated
                return LoginResult.Error(AuthError.InvalidCredentials)
            }

            // 3. Llamar al repository
            when (val result = repository.login(credentials)) {
                is Result.Success -> {
                    val loginResponse = result.data
                    val authToken = loginResponse.toAuthToken()

                    // 4. Guardar en storage (NO loguear tokens!)
                    saveAuthData(authToken, loginResponse.user)

                    // 5. Actualizar estado
                    _authState.value = AuthState.Authenticated(
                        user = loginResponse.user,
                        token = authToken
                    )

                    LoginResult.Success(loginResponse)
                }
                is Result.Failure -> {
                    _authState.value = AuthState.Unauthenticated
                    val authError = mapErrorToAuthError(result.error)
                    LoginResult.Error(authError)
                }
                is Result.Loading -> {
                    // No debería ocurrir en suspend function
                    _authState.value = AuthState.Unauthenticated
                    LoginResult.Error(AuthError.UnknownError("Unexpected loading state"))
                }
            }
        }
    }

    override suspend fun logout(): Result<Unit> {
        return stateMutex.withLock {
            // Obtener token para llamada al backend
            val token = getCurrentAuthToken()?.token

            // Llamar al backend (ignorar fallos de red)
            if (token != null) {
                repository.logout(token) // Ignoramos el resultado
            }

            // Limpiar storage local SIEMPRE (incluso si backend falla)
            clearAuthData()

            // Actualizar estado
            _authState.value = AuthState.Unauthenticated

            Result.Success(Unit)
        }
    }

    override suspend fun refreshAuthToken(): Result<AuthToken> {
        // Delegar al TokenRefreshManager que maneja sincronización y retry
        val result = tokenRefreshManager.forceRefresh()

        // Actualizar estado si estamos autenticados y el refresh fue exitoso
        if (result is Result.Success) {
            stateMutex.withLock {
                val currentState = _authState.value
                if (currentState is AuthState.Authenticated) {
                    _authState.value = currentState.copy(token = result.data)
                }
            }
        }

        return result
    }

    // === Implementación de TokenProvider ===

    /**
     * Implementación de [TokenProvider.refreshToken] que delega a [refreshAuthToken].
     *
     * Retorna el nuevo access token como String o null si falla.
     */
    override suspend fun refreshToken(): String? {
        return when (val result = refreshAuthToken()) {
            is Result.Success -> result.data.token
            is Result.Failure -> null
            is Result.Loading -> null
        }
    }

    override fun isAuthenticated(): Boolean {
        return _authState.value is AuthState.Authenticated
    }

    override suspend fun getToken(): String? {
        val token = getCurrentAuthToken() ?: return null

        // Si está expirado, intentar refresh
        if (token.isExpired()) {
            return refreshToken() // Usa la implementación de TokenProvider
        }

        return token.token
    }

    override suspend fun isTokenExpired(): Boolean {
        return getCurrentAuthToken()?.isExpired() ?: true
    }

    override fun getCurrentUser(): AuthUserInfo? {
        return when (val state = _authState.value) {
            is AuthState.Authenticated -> state.user
            else -> null
        }
    }

    override fun getCurrentAuthToken(): AuthToken? {
        return when (val state = _authState.value) {
            is AuthState.Authenticated -> state.token
            else -> null
        }
    }

    override suspend fun restoreSession() {
        stateMutex.withLock {
            val tokenJson = storage.getStringSafe(AUTH_TOKEN_KEY, "")
            val userJson = storage.getStringSafe(AUTH_USER_KEY, "")

            if (tokenJson.isNotBlank() && userJson.isNotBlank()) {
                try {
                    val token = json.decodeFromString<AuthToken>(tokenJson)
                    val user = json.decodeFromString<AuthUserInfo>(userJson)

                    // Verificar si token está expirado
                    if (!token.isExpired()) {
                        _authState.value = AuthState.Authenticated(user, token)
                    } else {
                        // Intentar refresh si está expirado y hay refresh token
                        if (token.hasRefreshToken()) {
                            val refreshResult = tokenRefreshManager.forceRefresh()
                            if (refreshResult is Result.Success) {
                                val newToken = refreshResult.data
                                _authState.value = AuthState.Authenticated(user, newToken)
                            } else {
                                // Refresh falló, limpiar (onRefreshFailed se emitirá automáticamente)
                                clearAuthData()
                                _authState.value = AuthState.Unauthenticated
                            }
                        } else {
                            // No hay refresh token, limpiar
                            clearAuthData()
                            _authState.value = AuthState.Unauthenticated
                        }
                    }
                } catch (e: Exception) {
                    // Error deserializando, limpiar storage corrupto
                    println("Error restoring auth session: ${e.message}")
                    e.printStackTrace()
                    clearAuthData()
                    _authState.value = AuthState.Unauthenticated
                }
            }
        }
    }

    // === Private helper methods ===

    private fun saveAuthData(token: AuthToken, user: AuthUserInfo) {
        saveToken(token)
        saveUser(user)
    }

    private fun saveToken(token: AuthToken) {
        val tokenJson = json.encodeToString(token)
        storage.putStringSafe(AUTH_TOKEN_KEY, tokenJson)
    }

    private fun saveUser(user: AuthUserInfo) {
        val userJson = json.encodeToString(user)
        storage.putStringSafe(AUTH_USER_KEY, userJson)
    }

    private fun clearAuthData() {
        storage.removeSafe(AUTH_TOKEN_KEY)
        storage.removeSafe(AUTH_USER_KEY)
    }

    private fun mapErrorToAuthError(error: String): AuthError {
        // Mapear errores del repository a AuthError
        return when {
            error.contains("401") || error.contains("invalid credentials", ignoreCase = true) ->
                AuthError.InvalidCredentials
            error.contains("404") || error.contains("not found", ignoreCase = true) ->
                AuthError.UserNotFound
            error.contains("423") || error.contains("locked", ignoreCase = true) ->
                AuthError.AccountLocked
            error.contains("403") || error.contains("forbidden", ignoreCase = true) ||
                    error.contains("inactive", ignoreCase = true) ->
                AuthError.UserInactive
            error.contains("network", ignoreCase = true) ||
                    error.contains("timeout", ignoreCase = true) ||
                    error.contains("connection", ignoreCase = true) ->
                AuthError.NetworkError(error)
            else ->
                AuthError.UnknownError(error)
        }
    }
}
