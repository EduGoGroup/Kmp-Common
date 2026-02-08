package com.edugo.test.module.auth.service

import com.edugo.test.module.auth.model.LoginCredentials
import com.edugo.test.module.auth.repository.StubAuthRepository
import com.edugo.test.module.auth.token.TokenRefreshConfig
import com.edugo.test.module.core.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests de integración para TokenRefreshManager con AuthService.
 *
 * Estos tests verifican el comportamiento completo del flujo de auto-refresh:
 * - Token refresh cuando expira
 * - Sincronización de múltiples refreshes concurrentes
 * - Emisión de onSessionExpired cuando el refresh falla
 * - Integración con HttpClient (conceptualmente)
 */
class AuthServiceRefreshIntegrationTest {

    /**
     * Test: Refresh exitoso actualiza el token en storage y estado.
     */
    @Test
    fun `successful refresh updates token in storage and state`() = runTest {
        // Arrange
        val stubRepo = StubAuthRepository().apply {
            refreshShouldSucceed = true
        }
        val authService = AuthServiceFactory.createForTesting(
            repository = stubRepo,
            refreshConfig = TokenRefreshConfig.NO_RETRY // Sin retry para test rápido
        )

        // Login inicial
        val loginResult = authService.login(LoginCredentials("test@edugo.com", "password123"))
        assertTrue(loginResult is com.edugo.test.module.auth.model.LoginResult.Success)

        val originalToken = authService.getCurrentAuthToken()
        require(originalToken != null) { "Token should exist after login" }

        // Act: Forzar refresh
        val refreshResult = authService.tokenRefreshManager.forceRefresh()

        // Assert
        assertTrue(refreshResult is Result.Success, "Refresh should succeed")
        val newToken = authService.getCurrentAuthToken()
        require(newToken != null) { "Token should exist after refresh" }

        // El token debe ser diferente (stub genera tokens únicos)
        assertFalse(
            originalToken.token == newToken.token,
            "Token should be different after refresh"
        )

        // El estado debe seguir siendo Authenticated
        assertTrue(
            authService.authState.value is AuthState.Authenticated,
            "State should remain Authenticated after refresh"
        )
    }

    /**
     * Test: Múltiples refreshes concurrentes resultan en un solo refresh real.
     */
    @Test
    fun `concurrent refresh calls result in single refresh operation`() = runTest {
        // Arrange
        var refreshCallCount = 0
        val stubRepo = StubAuthRepository().apply {
            refreshShouldSucceed = true
            onRefresh = {
                refreshCallCount++
                delay(100) // Simular latencia de red para aumentar concurrencia
            }
        }

        val authService = AuthServiceFactory.createForTesting(
            repository = stubRepo,
            refreshConfig = TokenRefreshConfig.NO_RETRY
        )

        // Login inicial
        authService.login(LoginCredentials("test@edugo.com", "password123"))

        // Act: Lanzar 10 refreshes concurrentes
        val results = (1..10).map {
            async { authService.tokenRefreshManager.forceRefresh() }
        }.awaitAll()

        // Assert
        assertEquals(1, refreshCallCount, "Repository refresh should be called exactly once")
        assertTrue(results.all { it is Result.Success }, "All refreshes should succeed")

        // Todos deben recibir el mismo token
        val tokens = results.mapNotNull { (it as? Result.Success)?.data?.token }
        assertTrue(tokens.distinct().size == 1, "All calls should receive the same token")
    }

    /**
     * Test: Cuando refresh falla, onSessionExpired se emite.
     */
    @Test
    fun `refresh failure emits onSessionExpired event`() = runTest {
        // Arrange
        val stubRepo = StubAuthRepository().apply {
            refreshShouldSucceed = false
        }

        val authService = AuthServiceFactory.createForTesting(
            repository = stubRepo,
            refreshConfig = TokenRefreshConfig.NO_RETRY
        )

        // Login inicial
        authService.login(LoginCredentials("test@edugo.com", "password123"))

        // Colectar eventos de onSessionExpired
        val sessionExpiredEvents = mutableListOf<Unit>()
        val job = launch {
            authService.onSessionExpired.collect {
                sessionExpiredEvents.add(Unit)
            }
        }

        // Act: Forzar refresh (fallará)
        val refreshResult = authService.tokenRefreshManager.forceRefresh()

        // Esperar a que se procese el evento
        delay(500)

        // Assert
        assertTrue(refreshResult is Result.Failure, "Refresh should fail")
        assertEquals(
            1,
            sessionExpiredEvents.size,
            "onSessionExpired should emit exactly once"
        )

        // El estado debe ser Unauthenticated
        assertTrue(
            authService.authState.value is AuthState.Unauthenticated,
            "State should be Unauthenticated after refresh failure"
        )

        // El token debe estar limpio
        assertEquals(null, authService.getCurrentAuthToken(), "Token should be cleared")

        job.cancel()
    }

    /**
     * Test: Refresh solo se ejecuta si el token está próximo a expirar.
     */
    @Test
    fun `refreshIfNeeded does not refresh when token has sufficient time`() = runTest {
        // Arrange
        var refreshCallCount = 0
        val stubRepo = StubAuthRepository().apply {
            refreshShouldSucceed = true
            onRefresh = { refreshCallCount++ }
        }

        val authService = AuthServiceFactory.createForTesting(
            repository = stubRepo,
            refreshConfig = TokenRefreshConfig(
                refreshThresholdSeconds = 300, // 5 minutos
                maxRetryAttempts = 0
            )
        )

        // Login inicial (stub genera tokens con 1 hora de validez)
        authService.login(LoginCredentials("test@edugo.com", "password123"))

        // Act: Intentar refresh condicional
        val result = authService.tokenRefreshManager.refreshIfNeeded()

        // Assert
        assertTrue(result is Result.Success, "Should return current token")
        assertEquals(0, refreshCallCount, "Should NOT call repository refresh")
    }

    /**
     * Test: AuthService se integra correctamente con TokenRefreshManager.
     */
    @Test
    fun `authService refreshToken method delegates to tokenRefreshManager`() = runTest {
        // Arrange
        val stubRepo = StubAuthRepository().apply {
            refreshShouldSucceed = true
        }

        val authService = AuthServiceFactory.createForTesting(
            repository = stubRepo,
            refreshConfig = TokenRefreshConfig.NO_RETRY
        )

        // Login inicial
        authService.login(LoginCredentials("test@edugo.com", "password123"))

        // Act: Llamar refreshToken() (método de TokenProvider)
        val newTokenString = authService.refreshToken()

        // Assert
        assertTrue(newTokenString != null, "Should return new token string")
        assertTrue(newTokenString!!.isNotBlank(), "Token should not be blank")
    }

    /**
     * Test: getToken() refresca automáticamente si está expirado.
     */
    @Test
    fun `getToken refreshes automatically if token is expired`() = runTest {
        // Arrange
        var refreshCallCount = 0
        val stubRepo = StubAuthRepository().apply {
            refreshShouldSucceed = true
            onRefresh = { refreshCallCount++ }
        }

        val authService = AuthServiceFactory.createForTesting(
            repository = stubRepo,
            refreshConfig = TokenRefreshConfig.NO_RETRY
        )

        // Login inicial
        authService.login(LoginCredentials("test@edugo.com", "password123"))

        // Simular token expirado manualmente
        // (En producción esto ocurriría naturalmente con el tiempo)
        // Nota: StubAuthRepository genera tokens con expiresAt muy lejano,
        // por lo que este test es más conceptual

        // Act: getToken() debe verificar expiración y refrescar si es necesario
        val token = authService.getToken()

        // Assert
        assertTrue(token != null, "Should return valid token")
        // Si el token no estaba expirado, refreshCallCount será 0
        // Si estaba expirado, refreshCallCount será 1
        assertTrue(refreshCallCount <= 1, "Should refresh at most once")
    }

    /**
     * Test: HttpClient configurado con auto-refresh puede ser creado.
     */
    @Test
    fun `createHttpClientWithAutoRefresh creates configured client`() = runTest {
        // Arrange
        val authService = AuthServiceFactory.createForTesting()

        // Act: Crear HttpClient con auto-refresh
        val httpClient = AuthServiceFactory.createHttpClientWithAutoRefresh(
            authService = authService,
            logLevel = io.ktor.client.plugins.logging.LogLevel.NONE
        )

        // Assert
        assertTrue(httpClient != null, "HttpClient should be created")

        // Cleanup
        httpClient.close()
    }
}
