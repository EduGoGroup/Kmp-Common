package com.edugo.test.module.auth.repository

import com.edugo.test.module.auth.model.AuthUserInfo
import com.edugo.test.module.auth.model.LoginCredentials
import com.edugo.test.module.core.Result
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Tests unitarios para StubAuthRepository.
 *
 * Verifica:
 * - Login con credenciales válidas e inválidas
 * - Logout siempre exitoso
 * - Refresh con tokens válidos e inválidos
 * - Simulación de errores de red
 * - Simulación de delay/latencia
 * - Reset de configuración
 */
class StubAuthRepositoryTest {

    private lateinit var repository: StubAuthRepository

    @BeforeTest
    fun setup() {
        repository = StubAuthRepository()
    }

    @AfterTest
    fun cleanup() {
        repository.reset()
    }

    // ==================== LOGIN TESTS ====================

    @Test
    fun `login with valid credentials returns success`() = runTest {
        // Given
        val credentials = StubAuthRepository.VALID_CREDENTIALS

        // When
        val result = repository.login(credentials)

        // Then
        assertIs<Result.Success<*>>(result)
        val response = (result as Result.Success).data
        assertEquals("test@edugo.com", response.user.email)
        assertNotNull(response.accessToken)
        assertNotNull(response.refreshToken)
    }

    @Test
    fun `login with invalid credentials returns failure`() = runTest {
        // Given
        val credentials = StubAuthRepository.INVALID_CREDENTIALS

        // When
        val result = repository.login(credentials)

        // Then
        assertIs<Result.Failure>(result)
        assertTrue(result.error.contains("credentials", ignoreCase = true))
    }

    @Test
    fun `login with invalid email returns failure`() = runTest {
        // Given
        val credentials = LoginCredentials("wrong@edugo.com", "password123")

        // When
        val result = repository.login(credentials)

        // Then
        assertIs<Result.Failure>(result)
    }

    @Test
    fun `login with invalid password returns failure`() = runTest {
        // Given
        val credentials = LoginCredentials("test@edugo.com", "wrongpassword")

        // When
        val result = repository.login(credentials)

        // Then
        assertIs<Result.Failure>(result)
    }

    // ==================== LOGOUT TESTS ====================

    @Test
    fun `logout always returns success`() = runTest {
        // Given
        val accessToken = "any_token"

        // When
        val result = repository.logout(accessToken)

        // Then
        assertIs<Result.Success<Unit>>(result)
    }

    @Test
    fun `logout with empty token returns success`() = runTest {
        // Given
        val accessToken = ""

        // When
        val result = repository.logout(accessToken)

        // Then
        assertIs<Result.Success<Unit>>(result)
    }

    // ==================== REFRESH TESTS ====================

    @Test
    fun `refresh with valid token returns success`() = runTest {
        // Given
        val refreshToken = StubAuthRepository.VALID_REFRESH_TOKEN

        // When
        val result = repository.refresh(refreshToken)

        // Then
        assertIs<Result.Success<*>>(result)
        val response = (result as Result.Success).data
        assertNotNull(response.accessToken)
        assertTrue(response.expiresIn > 0)
    }

    @Test
    fun `refresh with blank token returns failure`() = runTest {
        // Given
        val refreshToken = ""

        // When
        val result = repository.refresh(refreshToken)

        // Then
        assertIs<Result.Failure>(result)
        assertTrue(result.error.contains("refresh", ignoreCase = true))
    }

    // ==================== NETWORK ERROR SIMULATION ====================

    @Test
    fun `login with network error simulation returns failure`() = runTest {
        // Given
        repository.simulateNetworkError = true
        val credentials = StubAuthRepository.VALID_CREDENTIALS

        // When
        val result = repository.login(credentials)

        // Then
        assertIs<Result.Failure>(result)
        assertTrue(result.error.contains("timeout", ignoreCase = true) ||
                   result.error.contains("network", ignoreCase = true))
    }

    @Test
    fun `logout with network error simulation returns failure`() = runTest {
        // Given
        repository.simulateNetworkError = true

        // When
        val result = repository.logout("token")

        // Then
        assertIs<Result.Failure>(result)
    }

    @Test
    fun `refresh with network error simulation returns failure`() = runTest {
        // Given
        repository.simulateNetworkError = true

        // When
        val result = repository.refresh("refresh_token")

        // Then
        assertIs<Result.Failure>(result)
    }

    // ==================== DELAY SIMULATION ====================

    @Test
    fun `login with delay configuration works`() = runTest {
        // Given
        repository.simulateDelay = 50 // 50ms
        val credentials = StubAuthRepository.VALID_CREDENTIALS

        // When
        val result = repository.login(credentials)

        // Then
        // Verificar que el delay está configurado y la operación aún funciona
        assertEquals(50, repository.simulateDelay)
        assertIs<Result.Success<*>>(result)
    }

    // ==================== CONFIGURATION TESTS ====================

    @Test
    fun `custom valid email and password work`() = runTest {
        // Given
        repository.validEmail = "custom@test.com"
        repository.validPassword = "custompass"
        val credentials = LoginCredentials("custom@test.com", "custompass")

        // When
        val result = repository.login(credentials)

        // Then
        assertIs<Result.Success<*>>(result)
    }

    @Test
    fun `custom test user is returned on login`() = runTest {
        // Given
        val customUser = AuthUserInfo.createTestUser(
            id = "custom-id",
            email = "custom@test.com",
            firstName = "Custom",
            role = "teacher"
        )
        repository.testUser = customUser
        repository.validEmail = "custom@test.com"

        val credentials = LoginCredentials("custom@test.com", "password123")

        // When
        val result = repository.login(credentials)

        // Then
        assertIs<Result.Success<*>>(result)
        val response = (result as Result.Success).data
        assertEquals("custom-id", response.user.id)
        assertEquals("Custom", response.user.firstName)
        assertEquals("teacher", response.user.role)
    }

    @Test
    fun `reset restores default configuration`() = runTest {
        // Given - Modificar configuración
        repository.simulateNetworkError = true
        repository.simulateDelay = 1000
        repository.validEmail = "changed@test.com"

        // When - Reset
        repository.reset()

        // Then - Verificar valores por defecto
        assertFalse(repository.simulateNetworkError)
        assertEquals(0, repository.simulateDelay)
        assertEquals("test@edugo.com", repository.validEmail)
        assertEquals("password123", repository.validPassword)

        // Verificar que login funciona con credenciales por defecto
        val result = repository.login(StubAuthRepository.VALID_CREDENTIALS)
        assertIs<Result.Success<*>>(result)
    }

    // ==================== FACTORY METHODS TESTS ====================

    @Test
    fun `create factory method returns working instance`() = runTest {
        // When
        val stub = StubAuthRepository.create()
        val result = stub.login(StubAuthRepository.VALID_CREDENTIALS)

        // Then
        assertIs<Result.Success<*>>(result)
    }

    @Test
    fun `createWithNetworkError factory method simulates network error`() = runTest {
        // When
        val stub = StubAuthRepository.createWithNetworkError()
        val result = stub.login(StubAuthRepository.VALID_CREDENTIALS)

        // Then
        assertIs<Result.Failure>(result)
        assertTrue(stub.simulateNetworkError)
    }

    @Test
    fun `createWithDelay factory method configures delay`() = runTest {
        // When
        val stub = StubAuthRepository.createWithDelay(50)

        // Then
        assertEquals(50, stub.simulateDelay)
    }

    @Test
    fun `createWithUser factory method uses custom user`() = runTest {
        // Given
        val customUser = AuthUserInfo.createTestUser(id = "custom", email = "custom@test.com")

        // When
        val stub = StubAuthRepository.createWithUser(customUser)
        val result = stub.login(LoginCredentials("custom@test.com", "password123"))

        // Then
        assertIs<Result.Success<*>>(result)
        val response = (result as Result.Success).data
        assertEquals("custom", response.user.id)
    }
}
