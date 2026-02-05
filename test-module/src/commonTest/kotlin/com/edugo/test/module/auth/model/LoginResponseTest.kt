package com.edugo.test.module.auth.model

import com.edugo.test.module.data.models.AuthToken
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Tests unitarios para LoginResponse.
 *
 * Verifica:
 * - Deserialización desde JSON del backend
 * - Conversión a AuthToken con cálculo correcto de expiresAt
 * - Serialización/Deserialización round-trip
 * - Métodos utilitarios (getAuthorizationHeader, isBearerToken, etc.)
 */
class LoginResponseTest {

    // ==================== TESTS DE DESERIALIZACIÓN ====================

    @Test
    fun `deserialization from backend JSON works correctly`() {
        // Given - JSON tal como lo retorna el backend
        val json = """
            {
              "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test",
              "expires_in": 3600,
              "refresh_token": "refresh_token_123",
              "token_type": "Bearer",
              "user": {
                "id": "user-123",
                "email": "test@edugo.com",
                "first_name": "Test",
                "last_name": "User",
                "full_name": "Test User",
                "role": "student",
                "school_id": "school-456"
              }
            }
        """.trimIndent()

        // When
        val response = kotlinx.serialization.json.Json.decodeFromString(
            LoginResponse.serializer(),
            json
        )

        // Then
        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test", response.accessToken)
        assertEquals(3600, response.expiresIn)
        assertEquals("refresh_token_123", response.refreshToken)
        assertEquals("Bearer", response.tokenType)
        assertEquals("user-123", response.user.id)
        assertEquals("test@edugo.com", response.user.email)
        assertEquals("student", response.user.role)
    }

    @Test
    fun `deserialization handles user without school_id`() {
        // Given
        val json = """
            {
              "access_token": "token123",
              "expires_in": 3600,
              "refresh_token": "refresh123",
              "token_type": "Bearer",
              "user": {
                "id": "user-123",
                "email": "test@edugo.com",
                "first_name": "Test",
                "last_name": "User",
                "full_name": "Test User",
                "role": "admin"
              }
            }
        """.trimIndent()

        // When
        val response = kotlinx.serialization.json.Json.decodeFromString(
            LoginResponse.serializer(),
            json
        )

        // Then
        assertEquals(null, response.user.schoolId)
    }

    // ==================== TESTS DE CONVERSIÓN A AUTHTOKEN ====================

    @Test
    fun `toAuthToken creates valid AuthToken`() {
        // Given
        val now = Clock.System.now()
        val response = LoginResponse(
            accessToken = "test_token",
            expiresIn = 3600,
            refreshToken = "refresh_token",
            tokenType = "Bearer",
            user = AuthUserInfo.createTestUser()
        )

        // When
        val authToken = response.toAuthToken()

        // Then
        assertEquals("test_token", authToken.token)
        assertEquals("refresh_token", authToken.refreshToken)

        // Verificar que expiresAt está en el futuro
        assertTrue(authToken.expiresAt > now)

        // Verificar que expiresAt es aproximadamente now + expiresIn
        val expectedExpiration = now + 3600.seconds
        val timeDifference = (authToken.expiresAt - expectedExpiration).inWholeSeconds
        assertTrue(timeDifference < 2, "expiresAt should be within 2 seconds of expected")
    }

    @Test
    fun `toAuthToken calculates expiration correctly for different durations`() {
        // Given
        val testCases = listOf(
            1800,   // 30 minutos
            3600,   // 1 hora
            7200,   // 2 horas
            86400   // 24 horas
        )

        testCases.forEach { expiresIn ->
            // When
            val now = Clock.System.now()
            val response = LoginResponse.createTestResponse(expiresIn = expiresIn)
            val authToken = response.toAuthToken()

            // Then
            val expectedExpiration = now + expiresIn.seconds
            val timeDifference = (authToken.expiresAt - expectedExpiration).inWholeSeconds
            assertTrue(
                timeDifference < 2,
                "expiresAt should be within 2 seconds of expected for $expiresIn seconds"
            )
        }
    }

    @Test
    fun `toAuthToken creates non-expired token for positive expiresIn`() {
        // Given
        val response = LoginResponse.createTestResponse(expiresIn = 3600)

        // When
        val authToken = response.toAuthToken()

        // Then
        assertFalse(authToken.isExpired(), "Token should not be expired immediately after creation")
        assertTrue(authToken.isValid(), "Token should be valid immediately after creation")
    }

    // ==================== TESTS DE SERIALIZACIÓN ROUND-TRIP ====================

    @Test
    fun `serialization round-trip preserves all data`() {
        // Given
        val original = LoginResponse(
            accessToken = "test_token_123",
            expiresIn = 3600,
            refreshToken = "refresh_token_123",
            tokenType = "Bearer",
            user = AuthUserInfo(
                id = "user-123",
                email = "test@edugo.com",
                firstName = "Test",
                lastName = "User",
                fullName = "Test User",
                role = "student",
                schoolId = "school-456"
            )
        )

        // When
        val json = kotlinx.serialization.json.Json.encodeToString(
            LoginResponse.serializer(),
            original
        )
        val deserialized = kotlinx.serialization.json.Json.decodeFromString(
            LoginResponse.serializer(),
            json
        )

        // Then
        assertEquals(original.accessToken, deserialized.accessToken)
        assertEquals(original.expiresIn, deserialized.expiresIn)
        assertEquals(original.refreshToken, deserialized.refreshToken)
        assertEquals(original.tokenType, deserialized.tokenType)
        assertEquals(original.user.id, deserialized.user.id)
        assertEquals(original.user.email, deserialized.user.email)
        assertEquals(original.user.role, deserialized.user.role)
    }

    // ==================== TESTS DE MÉTODOS UTILITARIOS ====================

    @Test
    fun `isBearerToken returns true for Bearer token`() {
        // Given
        val response = LoginResponse.createTestResponse()

        // When - Then
        assertTrue(response.isBearerToken())
    }

    @Test
    fun `isBearerToken is case insensitive`() {
        // Given
        val testCases = listOf("Bearer", "bearer", "BEARER", "BeArEr")

        testCases.forEach { tokenType ->
            // When
            val response = LoginResponse(
                accessToken = "token",
                expiresIn = 3600,
                refreshToken = "refresh",
                tokenType = tokenType,
                user = AuthUserInfo.createTestUser()
            )

            // Then
            assertTrue(
                response.isBearerToken(),
                "isBearerToken should return true for '$tokenType'"
            )
        }
    }

    @Test
    fun `isBearerToken returns false for non-Bearer token`() {
        // Given
        val response = LoginResponse(
            accessToken = "token",
            expiresIn = 3600,
            refreshToken = "refresh",
            tokenType = "Basic",
            user = AuthUserInfo.createTestUser()
        )

        // When - Then
        assertFalse(response.isBearerToken())
    }

    @Test
    fun `getAuthorizationHeader returns correctly formatted header`() {
        // Given
        val response = LoginResponse(
            accessToken = "abc123xyz",
            expiresIn = 3600,
            refreshToken = "refresh",
            tokenType = "Bearer",
            user = AuthUserInfo.createTestUser()
        )

        // When
        val header = response.getAuthorizationHeader()

        // Then
        assertEquals("Bearer abc123xyz", header)
    }

    @Test
    fun `calculateExpirationTime returns future instant`() {
        // Given
        val now = Clock.System.now()
        val response = LoginResponse.createTestResponse(expiresIn = 3600)

        // When
        val expirationTime = response.calculateExpirationTime()

        // Then
        assertTrue(expirationTime > now, "Expiration time should be in the future")
    }

    @Test
    fun `calculateExpirationTime matches expiresIn duration`() {
        // Given
        val now = Clock.System.now()
        val expiresInSeconds = 1800
        val response = LoginResponse.createTestResponse(expiresIn = expiresInSeconds)

        // When
        val expirationTime = response.calculateExpirationTime()

        // Then
        val expectedExpiration = now + expiresInSeconds.seconds
        val timeDifference = (expirationTime - expectedExpiration).inWholeSeconds
        assertTrue(
            timeDifference < 2,
            "Expiration time should be within 2 seconds of expected"
        )
    }

    // ==================== TESTS DE LOGGING ====================

    @Test
    fun `toLogString does not expose full token`() {
        // Given
        val response = LoginResponse(
            accessToken = "very_long_secret_access_token_that_should_not_be_logged",
            expiresIn = 3600,
            refreshToken = "refresh_token",
            tokenType = "Bearer",
            user = AuthUserInfo.createTestUser()
        )

        // When
        val logString = response.toLogString()

        // Then
        assertFalse(
            logString.contains("very_long_secret_access_token_that_should_not_be_logged"),
            "Log string should not contain full token"
        )
        assertTrue(
            logString.contains("..."),
            "Log string should contain truncation indicator"
        )
    }

    @Test
    fun `toLogString includes useful debug information`() {
        // Given
        val response = LoginResponse.createTestResponse()

        // When
        val logString = response.toLogString()

        // Then
        assertTrue(logString.contains("Bearer"), "Should include token type")
        assertTrue(logString.contains("3600"), "Should include expiresIn")
        assertTrue(logString.contains("test-user-123"), "Should include user ID")
        assertTrue(logString.contains("student"), "Should include user role")
    }

    @Test
    fun `toLogString handles short tokens`() {
        // Given
        val response = LoginResponse(
            accessToken = "short",
            expiresIn = 3600,
            refreshToken = "refresh",
            tokenType = "Bearer",
            user = AuthUserInfo.createTestUser()
        )

        // When
        val logString = response.toLogString()

        // Then
        assertTrue(logString.contains("***"), "Short tokens should be fully masked")
        assertFalse(logString.contains("short"), "Short token should not be exposed")
    }

    // ==================== TESTS DE FACTORY METHOD ====================

    @Test
    fun `createTestResponse returns valid response`() {
        // When
        val response = LoginResponse.createTestResponse()

        // Then
        assertNotNull(response.accessToken)
        assertTrue(response.accessToken.startsWith("test_access_token_"))
        assertEquals(3600, response.expiresIn)
        assertNotNull(response.refreshToken)
        assertTrue(response.refreshToken.startsWith("test_refresh_token_"))
        assertEquals("Bearer", response.tokenType)
        assertEquals("test-user-123", response.user.id)
    }

    @Test
    fun `createTestResponse accepts custom parameters`() {
        // Given
        val customUser = AuthUserInfo.createTestUser(
            id = "custom-id",
            email = "custom@test.com"
        )

        // When
        val response = LoginResponse.createTestResponse(
            accessToken = "custom_token",
            expiresIn = 7200,
            refreshToken = "custom_refresh",
            user = customUser
        )

        // Then
        assertEquals("custom_token", response.accessToken)
        assertEquals(7200, response.expiresIn)
        assertEquals("custom_refresh", response.refreshToken)
        assertEquals("custom-id", response.user.id)
        assertEquals("custom@test.com", response.user.email)
    }

    @Test
    fun `createTestResponse generates unique tokens on each call`() {
        // When
        val response1 = LoginResponse.createTestResponse()

        // Pequeño delay para garantizar timestamp diferente
        Thread.sleep(2)

        val response2 = LoginResponse.createTestResponse()

        // Then
        // Los tokens deberían ser diferentes porque incluyen timestamp
        assertTrue(
            response1.accessToken != response2.accessToken,
            "Access tokens should be different: ${response1.accessToken} vs ${response2.accessToken}"
        )
        assertTrue(
            response1.refreshToken != response2.refreshToken,
            "Refresh tokens should be different: ${response1.refreshToken} vs ${response2.refreshToken}"
        )
    }
}
