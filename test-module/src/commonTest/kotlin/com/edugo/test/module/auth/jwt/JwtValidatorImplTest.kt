package com.edugo.test.module.auth.jwt

import com.edugo.test.module.auth.repository.StubAuthRepository
import com.edugo.test.module.auth.repository.TokenVerificationResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class JwtValidatorImplTest {
    private lateinit var validator: JwtValidator
    private lateinit var stubRepository: StubAuthRepository

    // Token válido con exp en el futuro (2030)
    // Payload: {"sub":"user-123","exp":1893456000}
    private val validToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyLTEyMyIsImV4cCI6MTg5MzQ1NjAwMH0.sig"

    // Token expirado (exp en el pasado - 2021)
    // Payload: {"sub":"user-123","exp":1604000000}
    private val expiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyLTEyMyIsImV4cCI6MTYwNDAwMDAwMH0.sig"

    @BeforeTest
    fun setup() {
        stubRepository = StubAuthRepository()
        validator = JwtValidatorImpl(stubRepository)
    }

    @Test
    fun `validate returns Valid when backend confirms`() = runTest {
        stubRepository.verifyTokenShouldSucceed = true
        stubRepository.verifyTokenResponse = TokenVerificationResponse(
            valid = true,
            userId = "user-123",
            email = "test@edugo.com",
            role = "admin",
            schoolId = "school-456",
            expiresAt = "2030-01-01T00:00:00Z"
        )

        val result = validator.validate(validToken)

        assertTrue(result is JwtValidationResult.Valid)
        assertEquals("user-123", (result as JwtValidationResult.Valid).userId)
        assertEquals("admin", result.role)
    }

    @Test
    fun `validate returns Invalid Expired for expired token without calling backend`() = runTest {
        // Configurar para que falle si se llama
        stubRepository.verifyTokenShouldSucceed = false

        val result = validator.validate(expiredToken)

        assertTrue(result is JwtValidationResult.Invalid)
        assertEquals(
            JwtValidationResult.InvalidReason.Expired,
            (result as JwtValidationResult.Invalid).reason
        )
    }

    @Test
    fun `validate returns Invalid Revoked when backend says revoked`() = runTest {
        stubRepository.verifyTokenResponse = TokenVerificationResponse(
            valid = false,
            error = "Token has been revoked"
        )

        val result = validator.validate(validToken)

        assertTrue(result is JwtValidationResult.Invalid)
        assertEquals(
            JwtValidationResult.InvalidReason.Revoked,
            (result as JwtValidationResult.Invalid).reason
        )
    }

    @Test
    fun `validate returns NetworkError when backend unavailable`() = runTest {
        stubRepository.verifyTokenShouldSucceed = false
        stubRepository.simulateNetworkError = true

        val result = validator.validate(validToken)

        assertTrue(result is JwtValidationResult.NetworkError)
    }

    @Test
    fun `validate returns Invalid Malformed for invalid token structure`() = runTest {
        val result = validator.validate("not-a-jwt")

        assertTrue(result is JwtValidationResult.Invalid)
        assertEquals(
            JwtValidationResult.InvalidReason.Malformed,
            (result as JwtValidationResult.Invalid).reason
        )
    }

    @Test
    fun `validate returns Invalid Malformed for empty token`() = runTest {
        val result = validator.validate("")

        assertTrue(result is JwtValidationResult.Invalid)
        assertEquals(
            JwtValidationResult.InvalidReason.Malformed,
            (result as JwtValidationResult.Invalid).reason
        )
    }

    @Test
    fun `validate returns Invalid UserInactive when backend says inactive`() = runTest {
        stubRepository.verifyTokenResponse = TokenVerificationResponse(
            valid = false,
            error = "User is inactive"
        )

        val result = validator.validate(validToken)

        assertTrue(result is JwtValidationResult.Invalid)
        assertEquals(
            JwtValidationResult.InvalidReason.UserInactive,
            (result as JwtValidationResult.Invalid).reason
        )
    }

    @Test
    fun `quickValidate works offline and returns parsed claims`() {
        // quickValidate es síncrono y no llama al backend
        val result = validator.quickValidate(validToken)

        assertTrue(result is JwtParseResult.Success)
        assertEquals("user-123", (result as JwtParseResult.Success).claims.userId)
    }

    @Test
    fun `quickValidate returns error for invalid token`() {
        val result = validator.quickValidate("invalid")

        assertTrue(result is JwtParseResult.InvalidFormat)
    }

    @Test
    fun `extension isValid returns true for Valid result`() = runTest {
        stubRepository.verifyTokenResponse = TokenVerificationResponse(
            valid = true,
            userId = "user-123",
            email = "test@edugo.com",
            role = "user",
            expiresAt = "2030-01-01T00:00:00Z"
        )

        val result = validator.validate(validToken)

        assertTrue(result.isValid)
        assertFalse(result.isInvalid)
        assertFalse(result.isNetworkError)
    }

    @Test
    fun `extension isInvalid returns true for Invalid result`() = runTest {
        val result = validator.validate("not-a-jwt")

        assertFalse(result.isValid)
        assertTrue(result.isInvalid)
        assertFalse(result.isNetworkError)
    }

    @Test
    fun `extension getValidOrNull returns Valid when successful`() = runTest {
        stubRepository.verifyTokenResponse = TokenVerificationResponse(
            valid = true,
            userId = "user-123",
            email = "test@edugo.com",
            role = "user",
            expiresAt = "2030-01-01T00:00:00Z"
        )

        val result = validator.validate(validToken)
        val validResult = result.getValidOrNull()

        assertNotNull(validResult)
        assertEquals("user-123", validResult.userId)
    }

    @Test
    fun `extension getValidOrNull returns null when invalid`() = runTest {
        val result = validator.validate("invalid")
        val validResult = result.getValidOrNull()

        assertNull(validResult)
    }
}
