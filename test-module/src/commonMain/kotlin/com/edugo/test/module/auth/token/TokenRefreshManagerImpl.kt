package com.edugo.test.module.auth.token

import com.edugo.test.module.auth.repository.AuthRepository
import com.edugo.test.module.core.Result
import com.edugo.test.module.core.failure
import com.edugo.test.module.core.success
import com.edugo.test.module.data.models.AuthToken
import com.edugo.test.module.storage.SafeEduGoStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

/**
 * Implementación de [TokenRefreshManager] con sincronización thread-safe y retry inteligente.
 *
 * @property repository Repositorio para llamadas al backend
 * @property storage Storage seguro para persistencia de tokens
 * @property config Configuración de refresh y retry
 * @property scope CoroutineScope para operaciones asíncronas
 * @property json Instancia de Json para serialización
 */
public class TokenRefreshManagerImpl(
    private val repository: AuthRepository,
    private val storage: SafeEduGoStorage,
    private val config: TokenRefreshConfig = TokenRefreshConfig.DEFAULT,
    private val scope: CoroutineScope,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : TokenRefreshManager {

    // Mutex para sincronización de operaciones de refresh
    private val refreshMutex = Mutex()

    // Job del refresh en progreso (null si no hay refresh activo)
    private var refreshJob: Deferred<Result<AuthToken>>? = null

    // SharedFlow para emitir fallos de refresh
    private val _onRefreshFailed = MutableSharedFlow<RefreshFailureReason>(replay = 0)
    override val onRefreshFailed: Flow<RefreshFailureReason> = _onRefreshFailed.asSharedFlow()

    private companion object {
        private const val AUTH_TOKEN_KEY = "auth_token"
    }

    override suspend fun refreshIfNeeded(): Result<AuthToken> {
        return refreshMutex.withLock {
            // Si ya hay un refresh en progreso, esperar ese resultado
            refreshJob?.let { return@withLock it.await() }

            // Obtener token actual
            val currentToken = getCurrentToken()
                ?: return@withLock failure("No token available")

            // Verificar si necesita refresh
            if (!shouldRefresh(currentToken)) {
                return@withLock success(currentToken)
            }

            // Iniciar nuevo refresh
            refreshJob = scope.async { performRefresh() }
            val result = refreshJob!!.await()
            refreshJob = null

            result
        }
    }

    override suspend fun forceRefresh(): Result<AuthToken> {
        return refreshMutex.withLock {
            // Si ya hay un refresh en progreso, esperar ese resultado
            refreshJob?.let { return@withLock it.await() }

            // Iniciar nuevo refresh forzado
            refreshJob = scope.async { performRefresh() }
            val result = refreshJob!!.await()
            refreshJob = null

            result
        }
    }

    override fun shouldRefresh(token: AuthToken): Boolean {
        if (token.isExpired()) {
            return true
        }

        val now = Clock.System.now()
        val timeUntilExpiration = token.expiresAt - now
        val threshold = config.refreshThresholdSeconds.seconds

        return timeUntilExpiration <= threshold
    }

    /**
     * Ejecuta el refresh con retry y exponential backoff.
     */
    private suspend fun performRefresh(): Result<AuthToken> {
        val refreshToken = getRefreshToken()
            ?: return handleRefreshFailure(RefreshFailureReason.NoRefreshToken)

        var lastError: Throwable? = null

        repeat(config.maxRetryAttempts + 1) { attempt ->
            // Aplicar delay con exponential backoff (excepto en primer intento)
            if (attempt > 0) {
                val delayMs = config.calculateRetryDelay(attempt)
                delay(delayMs)
            }

            // Intentar refresh
            when (val result = repository.refresh(refreshToken)) {
                is Result.Success -> {
                    val refreshResponse = result.data
                    val newToken = refreshResponse.toAuthToken(refreshToken)

                    // Manejar token rotation si está habilitado
                    handleSuccessfulRefresh(newToken, refreshResponse.tokenType)

                    return success(newToken)
                }
                is Result.Failure -> {
                    lastError = extractThrowableFromError(result.error)

                    // Si no es retryable, fallar inmediatamente
                    if (!isRetryableError(result.error)) {
                        val reason = mapErrorToFailureReason(result.error)
                        return handleRefreshFailure(reason)
                    }
                }
                is Result.Loading -> {
                    // No debería ocurrir en suspend function
                }
            }
        }

        // Todos los reintentos fallaron
        val reason = RefreshFailureReason.NetworkError(
            lastError?.message ?: "Network error after ${config.maxRetryAttempts} retries"
        )
        return handleRefreshFailure(reason)
    }

    /**
     * Maneja un refresh exitoso, guardando el nuevo token.
     */
    private suspend fun handleSuccessfulRefresh(newToken: AuthToken, tokenType: String) {
        // Guardar nuevo access token
        saveToken(newToken)

        // Token rotation: Si el backend envió nuevo refresh token, ya está en newToken
        // porque toAuthToken() lo maneja automáticamente
    }

    /**
     * Maneja una falla de refresh, emitiendo el evento y retornando error.
     */
    private suspend fun handleRefreshFailure(reason: RefreshFailureReason): Result<AuthToken> {
        _onRefreshFailed.emit(reason)
        return failure(reason.toAuthError().errorCode.description)
    }

    /**
     * Mapea un mensaje de error a RefreshFailureReason.
     */
    private fun mapErrorToFailureReason(errorMessage: String): RefreshFailureReason {
        return when {
            errorMessage.contains("expired", ignoreCase = true) ->
                RefreshFailureReason.TokenExpired

            errorMessage.contains("revoked", ignoreCase = true) ->
                RefreshFailureReason.TokenRevoked

            errorMessage.contains("invalid", ignoreCase = true) ->
                RefreshFailureReason.TokenExpired

            errorMessage.contains("401") ->
                RefreshFailureReason.TokenExpired

            errorMessage.contains("network", ignoreCase = true) ||
            errorMessage.contains("timeout", ignoreCase = true) ||
            errorMessage.contains("connection", ignoreCase = true) ->
                RefreshFailureReason.NetworkError(errorMessage)

            errorMessage.matches(Regex(".*5\\d{2}.*")) -> {
                // Extraer código de servidor si es posible
                val code = Regex("(5\\d{2})").find(errorMessage)?.value?.toIntOrNull() ?: 500
                RefreshFailureReason.ServerError(code, errorMessage)
            }

            else ->
                RefreshFailureReason.ServerError(0, errorMessage)
        }
    }

    /**
     * Verifica si un error es retryable.
     */
    private fun isRetryableError(errorMessage: String): Boolean {
        // Errores de red son retryables
        if (errorMessage.contains("network", ignoreCase = true) ||
            errorMessage.contains("timeout", ignoreCase = true) ||
            errorMessage.contains("connection", ignoreCase = true)) {
            return true
        }

        // Errores 5xx son retryables
        if (errorMessage.matches(Regex(".*5\\d{2}.*"))) {
            return true
        }

        // Token expirado/inválido/revocado NO son retryables
        if (errorMessage.contains("expired", ignoreCase = true) ||
            errorMessage.contains("invalid", ignoreCase = true) ||
            errorMessage.contains("revoked", ignoreCase = true) ||
            errorMessage.contains("401")) {
            return false
        }

        // Por defecto, no reintentar
        return false
    }

    /**
     * Extrae un Throwable del mensaje de error (best-effort).
     */
    private fun extractThrowableFromError(errorMessage: String): Throwable {
        return Exception(errorMessage)
    }

    /**
     * Obtiene el token actual del storage.
     */
    private fun getCurrentToken(): AuthToken? {
        val tokenJson = storage.getStringSafe(AUTH_TOKEN_KEY, "")
        if (tokenJson.isBlank()) return null

        return try {
            json.decodeFromString<AuthToken>(tokenJson)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Obtiene el refresh token del token actual.
     */
    private fun getRefreshToken(): String? {
        return getCurrentToken()?.refreshToken
    }

    /**
     * Guarda el token en storage.
     */
    private fun saveToken(token: AuthToken) {
        val tokenJson = json.encodeToString(token)
        storage.putStringSafe(AUTH_TOKEN_KEY, tokenJson)
    }
}
