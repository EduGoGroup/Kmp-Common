package com.edugo.test.module.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class ErrorCodeTest {

    @Test
    fun errorCode_hasDefaultMessage() {
        assertEquals("An unexpected error occurred", ErrorCode.UNKNOWN.defaultMessage)
        assertEquals("Validation failed", ErrorCode.VALIDATION.defaultMessage)
        assertEquals("Network error occurred", ErrorCode.NETWORK.defaultMessage)
        assertEquals("Resource not found", ErrorCode.NOT_FOUND.defaultMessage)
        assertEquals("Unauthorized access", ErrorCode.UNAUTHORIZED.defaultMessage)
        assertEquals("Access forbidden", ErrorCode.FORBIDDEN.defaultMessage)
        assertEquals("Server error occurred", ErrorCode.SERVER_ERROR.defaultMessage)
    }

    @Test
    fun errorCode_hasCorrectHttpStatusCode() {
        assertEquals(null, ErrorCode.UNKNOWN.httpStatusCode) // UNKNOWN has no HTTP code, SERVER_ERROR is primary 500
        assertEquals(400, ErrorCode.VALIDATION.httpStatusCode)
        assertEquals(null, ErrorCode.NETWORK.httpStatusCode)
        assertEquals(404, ErrorCode.NOT_FOUND.httpStatusCode)
        assertEquals(401, ErrorCode.UNAUTHORIZED.httpStatusCode)
        assertEquals(403, ErrorCode.FORBIDDEN.httpStatusCode)
        assertEquals(500, ErrorCode.SERVER_ERROR.httpStatusCode)
        assertEquals(409, ErrorCode.CONFLICT.httpStatusCode)
        assertEquals(429, ErrorCode.RATE_LIMIT_EXCEEDED.httpStatusCode)
        assertEquals(503, ErrorCode.SERVICE_UNAVAILABLE.httpStatusCode)
        assertEquals(408, ErrorCode.TIMEOUT.httpStatusCode)
        assertEquals(400, ErrorCode.VALIDATION.httpStatusCode) // VALIDATION is the primary 400 error
        assertEquals(null, ErrorCode.BAD_REQUEST.httpStatusCode) // BAD_REQUEST has no HTTP code
        assertEquals(422, ErrorCode.BUSINESS_LOGIC.httpStatusCode)
    }

    @Test
    fun errorCode_networkHasNoHttpStatus() {
        assertEquals(null, ErrorCode.NETWORK.httpStatusCode)
        assertEquals(null, ErrorCode.SERIALIZATION.httpStatusCode)
        assertEquals(null, ErrorCode.PERSISTENCE.httpStatusCode)
    }

    @Test
    fun isClientError_identifiesClientErrors() {
        assertTrue(ErrorCode.VALIDATION.isClientError())
        assertTrue(ErrorCode.NOT_FOUND.isClientError())
        assertTrue(ErrorCode.UNAUTHORIZED.isClientError())
        assertTrue(ErrorCode.FORBIDDEN.isClientError())
        assertTrue(ErrorCode.CONFLICT.isClientError())
        assertTrue(ErrorCode.RATE_LIMIT_EXCEEDED.isClientError())
        assertTrue(ErrorCode.TIMEOUT.isClientError())
        assertFalse(ErrorCode.BAD_REQUEST.isClientError()) // BAD_REQUEST has no HTTP code
        assertTrue(ErrorCode.BUSINESS_LOGIC.isClientError())
    }

    @Test
    fun isClientError_returnsFalseForServerErrors() {
        assertFalse(ErrorCode.UNKNOWN.isClientError())
        assertFalse(ErrorCode.SERVER_ERROR.isClientError())
        assertFalse(ErrorCode.SERVICE_UNAVAILABLE.isClientError())
    }

    @Test
    fun isClientError_returnsFalseForNonHttpErrors() {
        assertFalse(ErrorCode.NETWORK.isClientError())
        assertFalse(ErrorCode.SERIALIZATION.isClientError())
        assertFalse(ErrorCode.PERSISTENCE.isClientError())
    }

    @Test
    fun isServerError_identifiesServerErrors() {
        assertFalse(ErrorCode.UNKNOWN.isServerError()) // UNKNOWN has no HTTP code
        assertTrue(ErrorCode.SERVER_ERROR.isServerError())
        assertTrue(ErrorCode.SERVICE_UNAVAILABLE.isServerError())
    }

    @Test
    fun isServerError_returnsFalseForClientErrors() {
        assertFalse(ErrorCode.VALIDATION.isServerError())
        assertFalse(ErrorCode.NOT_FOUND.isServerError())
        assertFalse(ErrorCode.UNAUTHORIZED.isServerError())
        assertFalse(ErrorCode.FORBIDDEN.isServerError())
        assertFalse(ErrorCode.CONFLICT.isServerError())
        assertFalse(ErrorCode.RATE_LIMIT_EXCEEDED.isServerError())
    }

    @Test
    fun isServerError_returnsFalseForNonHttpErrors() {
        assertFalse(ErrorCode.NETWORK.isServerError())
        assertFalse(ErrorCode.SERIALIZATION.isServerError())
        assertFalse(ErrorCode.PERSISTENCE.isServerError())
    }

    @Test
    fun isRetryable_identifiesRetryableErrors() {
        assertTrue(ErrorCode.NETWORK.isRetryable())
        assertTrue(ErrorCode.TIMEOUT.isRetryable())
        assertTrue(ErrorCode.SERVICE_UNAVAILABLE.isRetryable())
        assertTrue(ErrorCode.RATE_LIMIT_EXCEEDED.isRetryable())
    }

    @Test
    fun isRetryable_returnsFalseForNonRetryableErrors() {
        assertFalse(ErrorCode.UNKNOWN.isRetryable())
        assertFalse(ErrorCode.VALIDATION.isRetryable())
        assertFalse(ErrorCode.NOT_FOUND.isRetryable())
        assertFalse(ErrorCode.UNAUTHORIZED.isRetryable())
        assertFalse(ErrorCode.FORBIDDEN.isRetryable())
        assertFalse(ErrorCode.SERVER_ERROR.isRetryable())
        assertFalse(ErrorCode.CONFLICT.isRetryable())
        assertFalse(ErrorCode.BAD_REQUEST.isRetryable())
        assertFalse(ErrorCode.SERIALIZATION.isRetryable())
        assertFalse(ErrorCode.PERSISTENCE.isRetryable())
        assertFalse(ErrorCode.BUSINESS_LOGIC.isRetryable())
    }

    @Test
    fun fromHttpStatus_mapsCorrectly() {
        // 400 maps to VALIDATION (primary 400 error code)
        assertEquals(ErrorCode.VALIDATION, ErrorCode.fromHttpStatus(400))
        assertEquals(ErrorCode.UNAUTHORIZED, ErrorCode.fromHttpStatus(401))
        assertEquals(ErrorCode.FORBIDDEN, ErrorCode.fromHttpStatus(403))
        assertEquals(ErrorCode.NOT_FOUND, ErrorCode.fromHttpStatus(404))
        assertEquals(ErrorCode.TIMEOUT, ErrorCode.fromHttpStatus(408))
        assertEquals(ErrorCode.CONFLICT, ErrorCode.fromHttpStatus(409))
        assertEquals(ErrorCode.BUSINESS_LOGIC, ErrorCode.fromHttpStatus(422))
        assertEquals(ErrorCode.RATE_LIMIT_EXCEEDED, ErrorCode.fromHttpStatus(429))
        assertEquals(ErrorCode.SERVER_ERROR, ErrorCode.fromHttpStatus(500))
        assertEquals(ErrorCode.SERVICE_UNAVAILABLE, ErrorCode.fromHttpStatus(503))
    }

    @Test
    fun fromHttpStatus_returnsUnknownForUnmappedStatus() {
        assertEquals(ErrorCode.UNKNOWN, ErrorCode.fromHttpStatus(418)) // I'm a teapot
        assertEquals(ErrorCode.UNKNOWN, ErrorCode.fromHttpStatus(505))
        assertEquals(ErrorCode.UNKNOWN, ErrorCode.fromHttpStatus(999))
        assertEquals(ErrorCode.UNKNOWN, ErrorCode.fromHttpStatus(200)) // Success status
        assertEquals(ErrorCode.UNKNOWN, ErrorCode.fromHttpStatus(0))
    }

    @Test
    fun allErrorCodes_haveNonBlankDefaultMessage() {
        ErrorCode.entries.forEach { code ->
            assertTrue(
                code.defaultMessage.isNotBlank(),
                "ErrorCode.$code should have a non-blank default message"
            )
        }
    }

    @Test
    fun allErrorCodes_exist() {
        val expectedCodes = setOf(
            ErrorCode.UNKNOWN,
            ErrorCode.VALIDATION,
            ErrorCode.NETWORK,
            ErrorCode.NOT_FOUND,
            ErrorCode.UNAUTHORIZED,
            ErrorCode.FORBIDDEN,
            ErrorCode.SERVER_ERROR,
            ErrorCode.CONFLICT,
            ErrorCode.RATE_LIMIT_EXCEEDED,
            ErrorCode.SERVICE_UNAVAILABLE,
            ErrorCode.TIMEOUT,
            ErrorCode.BAD_REQUEST,
            ErrorCode.SERIALIZATION,
            ErrorCode.PERSISTENCE,
            ErrorCode.BUSINESS_LOGIC
        )

        assertEquals(expectedCodes, ErrorCode.entries.toSet())
    }

    @Test
    fun errorCode_canBeUsedInWhenExpression() {
        val code = ErrorCode.VALIDATION
        val message = when (code) {
            ErrorCode.VALIDATION -> "Validation error"
            ErrorCode.NETWORK -> "Network error"
            ErrorCode.NOT_FOUND -> "Not found"
            else -> "Other error"
        }
        assertEquals("Validation error", message)
    }

    @Test
    fun errorCode_hasCorrectToString() {
        // Enum toString() returns the name
        assertEquals("VALIDATION", ErrorCode.VALIDATION.toString())
        assertEquals("NETWORK", ErrorCode.NETWORK.toString())
        assertEquals("NOT_FOUND", ErrorCode.NOT_FOUND.toString())
    }

    @Test
    fun errorCode_comparison() {
        // Enum equality works correctly
        val code1 = ErrorCode.VALIDATION
        val code2 = ErrorCode.VALIDATION
        val code3 = ErrorCode.NETWORK

        assertEquals(code1, code2)
        assertTrue(code1 != code3)
    }
}
