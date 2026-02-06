package com.edugo.test.module.auth.service

import com.edugo.test.module.auth.service.AuthState
import com.edugo.test.module.auth.model.LoginCredentials
import com.edugo.test.module.auth.model.LoginResult
import com.edugo.test.module.core.Result
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests para [AuthServiceImpl].
 *
 * Verifica:
 * - Estado inicial
 * - Login exitoso y fallido
 * - Logout
 * - Refresh de tokens
 * - Obtención de tokens
 * - StateFlow emissions
 * - Persistencia de sesión
 */
class AuthServiceImplTest {

    private fun createTestService(): AuthService {
        return AuthServiceFactory.createForTesting()
    }

    @Test
    fun testInitialStateIsUnauthenticated() = runTest {
        val service = createTestService()

        val state = service.authState.value
        assertTrue(state is AuthState.Unauthenticated)
        assertFalse(service.isAuthenticated())
        assertNull(service.getCurrentUser())
        assertNull(service.getCurrentAuthToken())
    }

    @Test
    fun testLoginSuccess() = runTest {
        val service = createTestService()
        val credentials = LoginCredentials(
            email = "test@edugo.com",
            password = "password123"
        )

        val result = service.login(credentials)

        assertTrue(result is LoginResult.Success)
        assertTrue(service.isAuthenticated())
        assertNotNull(service.getCurrentUser())
        assertNotNull(service.getCurrentAuthToken())

        val state = service.authState.value
        assertTrue(state is AuthState.Authenticated)
        assertEquals("test@edugo.com", state.currentUser?.email)
    }

    @Test
    fun testLoginFailureWithInvalidCredentials() = runTest {
        val service = createTestService()
        val credentials = LoginCredentials(
            email = "wrong@edugo.com",
            password = "wrongpassword"
        )

        val result = service.login(credentials)

        assertTrue(result is LoginResult.Error)
        assertFalse(service.isAuthenticated())

        val state = service.authState.value
        assertTrue(state is AuthState.Unauthenticated)
    }

    @Test
    fun testLoginFailureWithInvalidEmail() = runTest {
        val service = createTestService()
        val credentials = LoginCredentials(
            email = "notanemail",
            password = "password123"
        )

        val result = service.login(credentials)

        assertTrue(result is LoginResult.Error)
        assertFalse(service.isAuthenticated())
    }

    @Test
    fun testLoginFailureWithShortPassword() = runTest {
        val service = createTestService()
        val credentials = LoginCredentials(
            email = "test@edugo.com",
            password = "short"
        )

        val result = service.login(credentials)

        assertTrue(result is LoginResult.Error)
        assertFalse(service.isAuthenticated())
    }

    @Test
    fun testLogoutClearsSession() = runTest {
        val service = createTestService()

        // Primero login
        service.login(LoginCredentials("test@edugo.com", "password123"))
        assertTrue(service.isAuthenticated())

        // Luego logout
        val result = service.logout()

        assertTrue(result is Result.Success)
        assertFalse(service.isAuthenticated())
        assertNull(service.getCurrentUser())
        assertNull(service.getCurrentAuthToken())

        val state = service.authState.value
        assertTrue(state is AuthState.Unauthenticated)
    }

    @Test
    fun testLogoutWhenNotAuthenticatedSucceeds() = runTest {
        val service = createTestService()

        // Logout sin login previo
        val result = service.logout()

        assertTrue(result is Result.Success)
        assertFalse(service.isAuthenticated())
    }

    @Test
    fun testGetTokenReturnsValidToken() = runTest {
        val service = createTestService()

        service.login(LoginCredentials("test@edugo.com", "password123"))

        val token = service.getToken()
        assertNotNull(token)
        assertTrue(token.isNotBlank())
    }

    @Test
    fun testGetTokenReturnsNullWhenNotAuthenticated() = runTest {
        val service = createTestService()

        val token = service.getToken()
        assertNull(token)
    }

    @Test
    fun testIsTokenExpiredReturnsTrueWhenNotAuthenticated() = runTest {
        val service = createTestService()

        val isExpired = service.isTokenExpired()
        assertTrue(isExpired)
    }

    @Test
    fun testIsTokenExpiredReturnsFalseForValidToken() = runTest {
        val service = createTestService()

        service.login(LoginCredentials("test@edugo.com", "password123"))

        val isExpired = service.isTokenExpired()
        assertFalse(isExpired)
    }

    @Test
    fun testRefreshTokenSucceeds() = runTest {
        val service = createTestService()

        // Login primero
        service.login(LoginCredentials("test@edugo.com", "password123"))
        val oldToken = service.getCurrentAuthToken()
        assertNotNull(oldToken)

        // Refresh
        val result = service.refreshAuthToken()

        assertTrue(result is Result.Success)
        val newToken = service.getCurrentAuthToken()
        assertNotNull(newToken)
    }

    @Test
    fun testRefreshTokenFailsWhenNotAuthenticated() = runTest {
        val service = createTestService()

        val result = service.refreshAuthToken()

        assertTrue(result is Result.Failure)
    }

    @Test
    fun testStateFlowEmissions() = runTest {
        val service = createTestService()

        // Estado inicial
        var state = service.authState.first()
        assertTrue(state is AuthState.Unauthenticated)

        // Login
        service.login(LoginCredentials("test@edugo.com", "password123"))
        state = service.authState.value
        assertTrue(state is AuthState.Authenticated)

        // Logout
        service.logout()
        state = service.authState.value
        assertTrue(state is AuthState.Unauthenticated)
    }

    @Test
    fun testGetCurrentUserReturnsUserWhenAuthenticated() = runTest {
        val service = createTestService()

        service.login(LoginCredentials("test@edugo.com", "password123"))

        val user = service.getCurrentUser()
        assertNotNull(user)
        assertEquals("test@edugo.com", user.email)
    }

    @Test
    fun testGetCurrentUserReturnsNullWhenNotAuthenticated() = runTest {
        val service = createTestService()

        val user = service.getCurrentUser()
        assertNull(user)
    }

    @Test
    fun testGetCurrentAuthTokenReturnsTokenWhenAuthenticated() = runTest {
        val service = createTestService()

        service.login(LoginCredentials("test@edugo.com", "password123"))

        val token = service.getCurrentAuthToken()
        assertNotNull(token)
        assertNotNull(token.token)
    }

    @Test
    fun testGetCurrentAuthTokenReturnsNullWhenNotAuthenticated() = runTest {
        val service = createTestService()

        val token = service.getCurrentAuthToken()
        assertNull(token)
    }

    @Test
    fun testIsAuthenticatedReturnsFalseInitially() = runTest {
        val service = createTestService()

        assertFalse(service.isAuthenticated())
    }

    @Test
    fun testIsAuthenticatedReturnsTrueAfterLogin() = runTest {
        val service = createTestService()

        service.login(LoginCredentials("test@edugo.com", "password123"))

        assertTrue(service.isAuthenticated())
    }

    @Test
    fun testIsAuthenticatedReturnsFalseAfterLogout() = runTest {
        val service = createTestService()

        service.login(LoginCredentials("test@edugo.com", "password123"))
        service.logout()

        assertFalse(service.isAuthenticated())
    }

    @Test
    fun testRestoreSessionWithNoDataDoesNothing() = runTest {
        // Cada test tiene storage único (AuthServiceFactory genera nombre único)
        // Esto garantiza aislamiento completo entre tests
        val service = createTestService()

        service.restoreSession()

        assertFalse(service.isAuthenticated())
        val state = service.authState.value
        assertTrue(state is AuthState.Unauthenticated)
    }

    @Test
    fun testMultipleLoginsUpdateState() = runTest {
        val service = createTestService()

        // First login
        service.login(LoginCredentials("test@edugo.com", "password123"))
        assertTrue(service.isAuthenticated())

        // Logout
        service.logout()
        assertFalse(service.isAuthenticated())

        // Second login
        service.login(LoginCredentials("test@edugo.com", "password123"))
        assertTrue(service.isAuthenticated())
    }

    @Test
    fun testLoginWithNetworkErrorFails() = runTest {
        val service = AuthServiceFactory.createForTestingWithNetworkError()

        val result = service.login(LoginCredentials("test@edugo.com", "password123"))

        assertTrue(result is LoginResult.Error)
        assertFalse(service.isAuthenticated())
    }
}
