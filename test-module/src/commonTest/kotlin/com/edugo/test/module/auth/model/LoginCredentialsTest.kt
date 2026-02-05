package com.edugo.test.module.auth.model

import com.edugo.test.module.core.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests unitarios para LoginCredentials.
 *
 * Verifica:
 * - Validación de email (formato, vacío)
 * - Validación de password (longitud mínima, vacío)
 * - Serialización JSON round-trip
 * - Factory methods para testing
 */
class LoginCredentialsTest {

    // ==================== TESTS DE VALIDACIÓN DE EMAIL ====================

    @Test
    fun `validate returns success for valid email and password`() {
        // Given
        val credentials = LoginCredentials(
            email = "user@edugo.com",
            password = "password123"
        )

        // When
        val result = credentials.validate()

        // Then
        assertIs<Result.Success<Unit>>(result)
    }

    @Test
    fun `validate fails when email is blank`() {
        // Given
        val credentials = LoginCredentials(
            email = "",
            password = "password123"
        )

        // When
        val result = credentials.validate()

        // Then
        assertIs<Result.Failure>(result)
        assertEquals("Email cannot be blank", result.error)
    }

    @Test
    fun `validate fails when email has no at symbol`() {
        // Given
        val credentials = LoginCredentials(
            email = "notanemail.com",
            password = "password123"
        )

        // When
        val result = credentials.validate()

        // Then
        assertIs<Result.Failure>(result)
        assertEquals("Email must be a valid email address", result.error)
    }

    @Test
    fun `validate fails when email has multiple at symbols`() {
        // Given
        val credentials = LoginCredentials(
            email = "user@@edugo.com",
            password = "password123"
        )

        // When
        val result = credentials.validate()

        // Then
        assertIs<Result.Failure>(result)
        assertEquals("Email must be a valid email address", result.error)
    }

    @Test
    fun `validate fails when email has no local part`() {
        // Given
        val credentials = LoginCredentials(
            email = "@edugo.com",
            password = "password123"
        )

        // When
        val result = credentials.validate()

        // Then
        assertIs<Result.Failure>(result)
        assertEquals("Email must be a valid email address", result.error)
    }

    @Test
    fun `validate fails when email has no domain`() {
        // Given
        val credentials = LoginCredentials(
            email = "user@",
            password = "password123"
        )

        // When
        val result = credentials.validate()

        // Then
        assertIs<Result.Failure>(result)
        assertEquals("Email must be a valid email address", result.error)
    }

    @Test
    fun `validate fails when email domain has no dot`() {
        // Given
        val credentials = LoginCredentials(
            email = "user@edugo",
            password = "password123"
        )

        // When
        val result = credentials.validate()

        // Then
        assertIs<Result.Failure>(result)
        assertEquals("Email must be a valid email address", result.error)
    }

    @Test
    fun `validate succeeds with valid email formats`() {
        // Given
        val validEmails = listOf(
            "user@edugo.com",
            "test.user@edugo.com",
            "test+tag@edugo.com",
            "user123@subdomain.edugo.com",
            "a@b.co"
        )

        validEmails.forEach { email ->
            // When
            val credentials = LoginCredentials(email = email, password = "password123")
            val result = credentials.validate()

            // Then
            assertIs<Result.Success<Unit>>(result, "Email $email should be valid")
        }
    }

    // ==================== TESTS DE VALIDACIÓN DE PASSWORD ====================

    @Test
    fun `validate fails when password is blank`() {
        // Given
        val credentials = LoginCredentials(
            email = "user@edugo.com",
            password = ""
        )

        // When
        val result = credentials.validate()

        // Then
        assertIs<Result.Failure>(result)
        assertEquals("Password cannot be blank", result.error)
    }

    @Test
    fun `validate fails when password is too short`() {
        // Given
        val credentials = LoginCredentials(
            email = "user@edugo.com",
            password = "short"  // 5 caracteres, menos de 8
        )

        // When
        val result = credentials.validate()

        // Then
        assertIs<Result.Failure>(result)
        assertEquals("Password must be at least 8 characters long", result.error)
    }

    @Test
    fun `validate succeeds when password has exactly minimum length`() {
        // Given
        val credentials = LoginCredentials(
            email = "user@edugo.com",
            password = "12345678"  // Exactamente 8 caracteres
        )

        // When
        val result = credentials.validate()

        // Then
        assertIs<Result.Success<Unit>>(result)
    }

    @Test
    fun `validate succeeds when password is longer than minimum`() {
        // Given
        val credentials = LoginCredentials(
            email = "user@edugo.com",
            password = "verylongpassword123"
        )

        // When
        val result = credentials.validate()

        // Then
        assertIs<Result.Success<Unit>>(result)
    }

    // ==================== TESTS DE SERIALIZACIÓN ====================

    @Test
    fun `serialization preserves email and password fields`() {
        // Given
        val credentials = LoginCredentials(
            email = "user@edugo.com",
            password = "password123"
        )

        // When
        val json = kotlinx.serialization.json.Json.encodeToString(
            LoginCredentials.serializer(),
            credentials
        )
        val deserialized = kotlinx.serialization.json.Json.decodeFromString(
            LoginCredentials.serializer(),
            json
        )

        // Then
        assertEquals(credentials.email, deserialized.email)
        assertEquals(credentials.password, deserialized.password)
    }

    @Test
    fun `serialization uses snake_case or camelCase as expected`() {
        // Given
        val credentials = LoginCredentials(
            email = "user@edugo.com",
            password = "password123"
        )

        // When
        val json = kotlinx.serialization.json.Json.encodeToString(
            LoginCredentials.serializer(),
            credentials
        )

        // Then - Verificar que el JSON contiene los campos esperados
        assertTrue(json.contains("\"email\""))
        assertTrue(json.contains("\"password\""))
        assertTrue(json.contains("user@edugo.com"))
        assertTrue(json.contains("password123"))
    }

    @Test
    fun `deserialization from JSON works correctly`() {
        // Given
        val json = """{"email":"test@edugo.com","password":"testpass123"}"""

        // When
        val credentials = kotlinx.serialization.json.Json.decodeFromString(
            LoginCredentials.serializer(),
            json
        )

        // Then
        assertEquals("test@edugo.com", credentials.email)
        assertEquals("testpass123", credentials.password)
    }

    // ==================== TESTS DE UTILIDADES ====================

    @Test
    fun `toLogString does not expose password`() {
        // Given
        val credentials = LoginCredentials(
            email = "user@edugo.com",
            password = "supersecretpassword"
        )

        // When
        val logString = credentials.toLogString()

        // Then
        assertTrue(logString.contains("user@edugo.com"))
        assertFalse(logString.contains("supersecretpassword"))
        assertTrue(logString.contains("***"))
    }

    @Test
    fun `createTestCredentials returns valid credentials`() {
        // When
        val credentials = LoginCredentials.createTestCredentials()

        // Then
        assertIs<Result.Success<Unit>>(credentials.validate())
        assertEquals("test@edugo.com", credentials.email)
        assertEquals("password123", credentials.password)
    }

    @Test
    fun `createTestCredentials accepts custom parameters`() {
        // When
        val credentials = LoginCredentials.createTestCredentials(
            email = "custom@test.com",
            password = "custompass"
        )

        // Then
        assertEquals("custom@test.com", credentials.email)
        assertEquals("custompass", credentials.password)
    }

    @Test
    fun `MIN_PASSWORD_LENGTH constant has expected value`() {
        // Then
        assertEquals(8, LoginCredentials.MIN_PASSWORD_LENGTH)
    }

    // ==================== TESTS DE CASOS EDGE ====================

    @Test
    fun `validate trims whitespace from email before validation`() {
        // Given
        val credentials = LoginCredentials(
            email = "  user@edugo.com  ",
            password = "password123"
        )

        // When
        val result = credentials.validate()

        // Then
        // El email con espacios debería ser válido porque se hace trim internamente
        assertIs<Result.Success<Unit>>(result)
    }

    @Test
    fun `validate handles email with only whitespace`() {
        // Given
        val credentials = LoginCredentials(
            email = "   ",
            password = "password123"
        )

        // When
        val result = credentials.validate()

        // Then
        assertIs<Result.Failure>(result)
        // El email vacío falla en isBlank() antes de llegar a isValidEmail()
        assertEquals("Email cannot be blank", result.error)
    }

    @Test
    fun `validate handles password with only whitespace`() {
        // Given
        val credentials = LoginCredentials(
            email = "user@edugo.com",
            password = "        "  // 8 espacios
        )

        // When
        val result = credentials.validate()

        // Then
        assertIs<Result.Failure>(result)
        assertEquals("Password cannot be blank", result.error)
    }
}
