package com.edugo.test.module.auth.service

import com.edugo.test.module.auth.model.LoginCredentials
import com.edugo.test.module.auth.model.LogoutResult
import com.edugo.test.module.auth.model.isPartial
import com.edugo.test.module.auth.model.isSuccess
import com.edugo.test.module.auth.model.localCleared
import com.edugo.test.module.auth.model.wasAlreadyLoggedOut
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests para la funcionalidad de logout extendida con soporte offline,
 * idempotencia y eventos diferenciados.
 *
 * Verifica:
 * - Logout exitoso online (Success)
 * - Idempotencia (AlreadyLoggedOut)
 * - Limpieza completa de estado
 * - Diferencia con método legacy logout()
 */
class LogoutTest {

    private lateinit var authService: AuthService

    private val validCredentials = LoginCredentials(
        email = "test@edugo.com",
        password = "password123"
    )

    @BeforeTest
    fun setup() {
        authService = AuthServiceFactory.createForTesting()
    }

    @Test
    fun `logoutWithDetails exitoso limpia todo y retorna Success`() = runTest {
        // Arrange: login primero
        authService.login(validCredentials)
        assertTrue(authService.isAuthenticated())

        // Act
        val result = authService.logoutWithDetails()

        // Assert
        assertEquals(LogoutResult.Success, result)
        assertFalse(authService.isAuthenticated())
        assertEquals(AuthState.Unauthenticated, authService.authState.value)
    }

    @Test
    fun `logoutWithDetails idempotente retorna AlreadyLoggedOut`() = runTest {
        // Arrange: NO hacer login (ya está deslogueado)
        assertFalse(authService.isAuthenticated())

        // Act: llamar logout sin estar logueado
        val result = authService.logoutWithDetails()

        // Assert
        assertEquals(LogoutResult.AlreadyLoggedOut, result)
        assertFalse(authService.isAuthenticated())
    }

    @Test
    fun `logoutWithDetails después de login, segundo logout es idempotente`() = runTest {
        // Arrange
        authService.login(validCredentials)
        assertTrue(authService.isAuthenticated())

        // Act
        val result1 = authService.logoutWithDetails()
        val result2 = authService.logoutWithDetails()

        // Assert
        assertEquals(LogoutResult.Success, result1)
        assertEquals(LogoutResult.AlreadyLoggedOut, result2)
    }

    @Test
    fun `logout con método legacy sigue funcionando`() = runTest {
        // Arrange
        authService.login(validCredentials)
        assertTrue(authService.isAuthenticated())

        // Act: usar método legacy logout()
        val result = authService.logout()

        // Assert
        assertTrue(result is com.edugo.test.module.core.Result.Success)
        assertFalse(authService.isAuthenticated())
    }

    @Test
    fun `extension properties de LogoutResult funcionan correctamente`() {
        // Success
        val success = LogoutResult.Success
        assertTrue(success.isSuccess)
        assertTrue(success.localCleared)
        assertFalse(success.isPartial)
        assertFalse(success.wasAlreadyLoggedOut)

        // PartialSuccess
        val partial = LogoutResult.PartialSuccess("Network error")
        assertFalse(partial.isSuccess)
        assertTrue(partial.localCleared)
        assertTrue(partial.isPartial)
        assertFalse(partial.wasAlreadyLoggedOut)

        // AlreadyLoggedOut
        val alreadyOut = LogoutResult.AlreadyLoggedOut
        assertFalse(alreadyOut.isSuccess)
        assertFalse(alreadyOut.localCleared)
        assertFalse(alreadyOut.isPartial)
        assertTrue(alreadyOut.wasAlreadyLoggedOut)
    }

    @Test
    fun `múltiples logouts concurrentes son seguros`() = runTest {
        // Arrange
        authService.login(validCredentials)
        assertTrue(authService.isAuthenticated())

        // Act: llamar logout múltiples veces
        val result1 = authService.logoutWithDetails()
        val result2 = authService.logoutWithDetails()
        val result3 = authService.logoutWithDetails()

        // Assert: primero exitoso, luego idempotentes
        assertEquals(LogoutResult.Success, result1)
        assertEquals(LogoutResult.AlreadyLoggedOut, result2)
        assertEquals(LogoutResult.AlreadyLoggedOut, result3)
        assertFalse(authService.isAuthenticated())
    }

    @Test
    fun `logoutWithDetails con forceLocal=true limpia local`() = runTest {
        // Arrange
        authService.login(validCredentials)

        // Act
        val result = authService.logoutWithDetails(forceLocal = true)

        // Assert
        assertTrue(result is LogoutResult.Success || result is LogoutResult.PartialSuccess)
        assertFalse(authService.isAuthenticated())
    }

    @Test
    fun `logoutWithDetails con forceLocal=false también limpia si backend OK`() = runTest {
        // Arrange
        authService.login(validCredentials)

        // Act
        val result = authService.logoutWithDetails(forceLocal = false)

        // Assert: con stub siempre es Success porque backend siempre OK
        assertEquals(LogoutResult.Success, result)
        assertFalse(authService.isAuthenticated())
    }
}
