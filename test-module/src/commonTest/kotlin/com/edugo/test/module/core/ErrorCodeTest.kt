package com.edugo.test.module.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class ErrorCodeTest {

    // ============================================================================
    // Numeric code and description tests
    // ============================================================================

    @Test
    fun errorCode_hasUniqueNumericCodes() {
        val codes = ErrorCode.entries.map { it.code }
        assertEquals(codes.size, codes.distinct().size, "All error codes must have unique numeric values")
    }

    @Test
    fun errorCode_hasNonBlankDescriptions() {
        ErrorCode.entries.forEach { code ->
            assertTrue(
                code.description.isNotBlank(),
                "ErrorCode.$code should have a non-blank description"
            )
        }
    }

    @Test
    fun errorCode_defaultMessageAliasWorks() {
        ErrorCode.entries.forEach { code ->
            assertEquals(
                code.description,
                code.defaultMessage,
                "defaultMessage should be an alias for description"
            )
        }
    }

    // ============================================================================
    // Category range tests
    // ============================================================================

    @Test
    fun networkErrors_areInCorrectRange() {
        val networkCodes = ErrorCode.entries.filter { it.name.startsWith("NETWORK_") }
        assertTrue(networkCodes.isNotEmpty(), "Should have network error codes")
        networkCodes.forEach { code ->
            assertTrue(
                code.code in 1000..1999,
                "Network error ${code.name} (${code.code}) should be in range 1000-1999"
            )
        }
    }

    @Test
    fun authErrors_areInCorrectRange() {
        val authCodes = ErrorCode.entries.filter { it.name.startsWith("AUTH_") }
        assertTrue(authCodes.isNotEmpty(), "Should have auth error codes")
        authCodes.forEach { code ->
            assertTrue(
                code.code in 2000..2999,
                "Auth error ${code.name} (${code.code}) should be in range 2000-2999"
            )
        }
    }

    @Test
    fun validationErrors_areInCorrectRange() {
        val validationCodes = ErrorCode.entries.filter { it.name.startsWith("VALIDATION_") }
        assertTrue(validationCodes.isNotEmpty(), "Should have validation error codes")
        validationCodes.forEach { code ->
            assertTrue(
                code.code in 3000..3999,
                "Validation error ${code.name} (${code.code}) should be in range 3000-3999"
            )
        }
    }

    @Test
    fun businessErrors_areInCorrectRange() {
        val businessCodes = ErrorCode.entries.filter { it.name.startsWith("BUSINESS_") }
        assertTrue(businessCodes.isNotEmpty(), "Should have business error codes")
        businessCodes.forEach { code ->
            assertTrue(
                code.code in 4000..4999,
                "Business error ${code.name} (${code.code}) should be in range 4000-4999"
            )
        }
    }

    @Test
    fun systemErrors_areInCorrectRange() {
        val systemCodes = ErrorCode.entries.filter { it.name.startsWith("SYSTEM_") }
        assertTrue(systemCodes.isNotEmpty(), "Should have system error codes")
        systemCodes.forEach { code ->
            assertTrue(
                code.code in 5000..5999,
                "System error ${code.name} (${code.code}) should be in range 5000-5999"
            )
        }
    }

    @Test
    fun allCategories_haveMinimumCodes() {
        assertTrue(ErrorCode.networkErrors().size >= 4, "Should have at least 4 network errors")
        assertTrue(ErrorCode.authErrors().size >= 4, "Should have at least 4 auth errors")
        assertTrue(ErrorCode.validationErrors().size >= 4, "Should have at least 4 validation errors")
        assertTrue(ErrorCode.businessErrors().size >= 4, "Should have at least 4 business errors")
        assertTrue(ErrorCode.systemErrors().size >= 4, "Should have at least 4 system errors")
    }

    @Test
    fun totalErrorCodes_meetsMinimumRequirement() {
        assertTrue(
            ErrorCode.entries.size >= 20,
            "Should have at least 20 error codes, found ${ErrorCode.entries.size}"
        )
    }

    // ============================================================================
    // Category detection method tests
    // ============================================================================

    @Test
    fun isNetworkError_identifiesNetworkErrors() {
        assertTrue(ErrorCode.NETWORK_TIMEOUT.isNetworkError())
        assertTrue(ErrorCode.NETWORK_NO_CONNECTION.isNetworkError())
        assertTrue(ErrorCode.NETWORK_SERVER_ERROR.isNetworkError())
        assertFalse(ErrorCode.AUTH_UNAUTHORIZED.isNetworkError())
        assertFalse(ErrorCode.VALIDATION_INVALID_INPUT.isNetworkError())
    }

    @Test
    fun isAuthError_identifiesAuthErrors() {
        assertTrue(ErrorCode.AUTH_UNAUTHORIZED.isAuthError())
        assertTrue(ErrorCode.AUTH_TOKEN_EXPIRED.isAuthError())
        assertTrue(ErrorCode.AUTH_FORBIDDEN.isAuthError())
        assertFalse(ErrorCode.NETWORK_TIMEOUT.isAuthError())
        assertFalse(ErrorCode.VALIDATION_INVALID_INPUT.isAuthError())
    }

    @Test
    fun isValidationError_identifiesValidationErrors() {
        assertTrue(ErrorCode.VALIDATION_INVALID_INPUT.isValidationError())
        assertTrue(ErrorCode.VALIDATION_MISSING_FIELD.isValidationError())
        assertTrue(ErrorCode.VALIDATION_FORMAT_ERROR.isValidationError())
        assertFalse(ErrorCode.NETWORK_TIMEOUT.isValidationError())
        assertFalse(ErrorCode.AUTH_UNAUTHORIZED.isValidationError())
    }

    @Test
    fun isBusinessError_identifiesBusinessErrors() {
        assertTrue(ErrorCode.BUSINESS_RESOURCE_NOT_FOUND.isBusinessError())
        assertTrue(ErrorCode.BUSINESS_OPERATION_NOT_ALLOWED.isBusinessError())
        assertTrue(ErrorCode.BUSINESS_RATE_LIMIT_EXCEEDED.isBusinessError())
        assertFalse(ErrorCode.NETWORK_TIMEOUT.isBusinessError())
        assertFalse(ErrorCode.VALIDATION_INVALID_INPUT.isBusinessError())
    }

    @Test
    fun isSystemError_identifiesSystemErrors() {
        assertTrue(ErrorCode.SYSTEM_UNKNOWN_ERROR.isSystemError())
        assertTrue(ErrorCode.SYSTEM_INTERNAL_ERROR.isSystemError())
        assertTrue(ErrorCode.SYSTEM_SERVICE_UNAVAILABLE.isSystemError())
        assertFalse(ErrorCode.NETWORK_TIMEOUT.isSystemError())
        assertFalse(ErrorCode.AUTH_UNAUTHORIZED.isSystemError())
    }

    // ============================================================================
    // HTTP status code mapping tests
    // ============================================================================

    @Test
    fun httpStatusCode_mapsCorrectlyForAuthErrors() {
        assertEquals(401, ErrorCode.AUTH_UNAUTHORIZED.httpStatusCode)
        assertEquals(401, ErrorCode.AUTH_TOKEN_EXPIRED.httpStatusCode)
        assertEquals(401, ErrorCode.AUTH_INVALID_CREDENTIALS.httpStatusCode)
        assertEquals(403, ErrorCode.AUTH_FORBIDDEN.httpStatusCode)
        assertEquals(423, ErrorCode.AUTH_ACCOUNT_LOCKED.httpStatusCode)
    }

    @Test
    fun httpStatusCode_mapsCorrectlyForValidationErrors() {
        assertEquals(400, ErrorCode.VALIDATION_INVALID_INPUT.httpStatusCode)
        assertEquals(400, ErrorCode.VALIDATION_MISSING_FIELD.httpStatusCode)
        assertEquals(400, ErrorCode.VALIDATION_FORMAT_ERROR.httpStatusCode)
        assertEquals(409, ErrorCode.VALIDATION_DUPLICATE_VALUE.httpStatusCode)
    }

    @Test
    fun httpStatusCode_mapsCorrectlyForBusinessErrors() {
        assertEquals(404, ErrorCode.BUSINESS_RESOURCE_NOT_FOUND.httpStatusCode)
        assertEquals(403, ErrorCode.BUSINESS_OPERATION_NOT_ALLOWED.httpStatusCode)
        assertEquals(409, ErrorCode.BUSINESS_RESOURCE_CONFLICT.httpStatusCode)
        assertEquals(429, ErrorCode.BUSINESS_RATE_LIMIT_EXCEEDED.httpStatusCode)
        assertEquals(410, ErrorCode.BUSINESS_OPERATION_EXPIRED.httpStatusCode)
    }

    @Test
    fun httpStatusCode_mapsCorrectlyForSystemErrors() {
        assertEquals(500, ErrorCode.SYSTEM_UNKNOWN_ERROR.httpStatusCode)
        assertEquals(500, ErrorCode.SYSTEM_INTERNAL_ERROR.httpStatusCode)
        assertEquals(503, ErrorCode.SYSTEM_SERVICE_UNAVAILABLE.httpStatusCode)
    }

    @Test
    fun httpStatusCode_returnsNullForNonHttpNetworkErrors() {
        assertEquals(null, ErrorCode.NETWORK_NO_CONNECTION.httpStatusCode)
        assertEquals(null, ErrorCode.NETWORK_DNS_FAILURE.httpStatusCode)
        assertEquals(null, ErrorCode.NETWORK_SSL_ERROR.httpStatusCode)
    }

    @Test
    fun httpStatusCode_returnsMappedForHttpNetworkErrors() {
        assertEquals(408, ErrorCode.NETWORK_TIMEOUT.httpStatusCode)
        assertEquals(502, ErrorCode.NETWORK_SERVER_ERROR.httpStatusCode)
    }

    // ============================================================================
    // isClientError and isServerError tests
    // ============================================================================

    @Test
    fun isClientError_identifiesClientErrors() {
        assertTrue(ErrorCode.VALIDATION_INVALID_INPUT.isClientError())
        assertTrue(ErrorCode.AUTH_UNAUTHORIZED.isClientError())
        assertTrue(ErrorCode.AUTH_FORBIDDEN.isClientError())
        assertTrue(ErrorCode.BUSINESS_RESOURCE_NOT_FOUND.isClientError())
        assertTrue(ErrorCode.BUSINESS_RATE_LIMIT_EXCEEDED.isClientError())
        assertTrue(ErrorCode.NETWORK_TIMEOUT.isClientError())
    }

    @Test
    fun isClientError_returnsFalseForServerErrors() {
        assertFalse(ErrorCode.SYSTEM_UNKNOWN_ERROR.isClientError())
        assertFalse(ErrorCode.SYSTEM_INTERNAL_ERROR.isClientError())
        assertFalse(ErrorCode.SYSTEM_SERVICE_UNAVAILABLE.isClientError())
    }

    @Test
    fun isClientError_returnsFalseForNonHttpErrors() {
        assertFalse(ErrorCode.NETWORK_NO_CONNECTION.isClientError())
        assertFalse(ErrorCode.NETWORK_DNS_FAILURE.isClientError())
        assertFalse(ErrorCode.NETWORK_SSL_ERROR.isClientError())
    }

    @Test
    fun isServerError_identifiesServerErrors() {
        assertTrue(ErrorCode.SYSTEM_UNKNOWN_ERROR.isServerError())
        assertTrue(ErrorCode.SYSTEM_INTERNAL_ERROR.isServerError())
        assertTrue(ErrorCode.SYSTEM_SERVICE_UNAVAILABLE.isServerError())
        assertTrue(ErrorCode.NETWORK_SERVER_ERROR.isServerError())
    }

    @Test
    fun isServerError_returnsFalseForClientErrors() {
        assertFalse(ErrorCode.VALIDATION_INVALID_INPUT.isServerError())
        assertFalse(ErrorCode.AUTH_UNAUTHORIZED.isServerError())
        assertFalse(ErrorCode.BUSINESS_RESOURCE_NOT_FOUND.isServerError())
    }

    // ============================================================================
    // isRetryable tests
    // ============================================================================

    @Test
    fun isRetryable_identifiesRetryableErrors() {
        assertTrue(ErrorCode.NETWORK_TIMEOUT.isRetryable())
        assertTrue(ErrorCode.NETWORK_NO_CONNECTION.isRetryable())
        assertTrue(ErrorCode.NETWORK_SERVER_ERROR.isRetryable())
        assertTrue(ErrorCode.NETWORK_DNS_FAILURE.isRetryable())
        assertTrue(ErrorCode.NETWORK_CONNECTION_RESET.isRetryable())
        assertTrue(ErrorCode.BUSINESS_RATE_LIMIT_EXCEEDED.isRetryable())
        assertTrue(ErrorCode.SYSTEM_SERVICE_UNAVAILABLE.isRetryable())
        assertTrue(ErrorCode.SYSTEM_EXTERNAL_SERVICE_ERROR.isRetryable())
    }

    @Test
    fun isRetryable_returnsFalseForNonRetryableErrors() {
        assertFalse(ErrorCode.AUTH_UNAUTHORIZED.isRetryable())
        assertFalse(ErrorCode.AUTH_INVALID_CREDENTIALS.isRetryable())
        assertFalse(ErrorCode.AUTH_FORBIDDEN.isRetryable())
        assertFalse(ErrorCode.VALIDATION_INVALID_INPUT.isRetryable())
        assertFalse(ErrorCode.VALIDATION_MISSING_FIELD.isRetryable())
        assertFalse(ErrorCode.BUSINESS_RESOURCE_NOT_FOUND.isRetryable())
        assertFalse(ErrorCode.BUSINESS_OPERATION_NOT_ALLOWED.isRetryable())
        assertFalse(ErrorCode.SYSTEM_UNKNOWN_ERROR.isRetryable())
        assertFalse(ErrorCode.SYSTEM_INTERNAL_ERROR.isRetryable())
    }

    // ============================================================================
    // fromCode tests
    // ============================================================================

    @Test
    fun fromCode_findsExistingCode() {
        assertEquals(ErrorCode.NETWORK_TIMEOUT, ErrorCode.fromCode(1000))
        assertEquals(ErrorCode.AUTH_UNAUTHORIZED, ErrorCode.fromCode(2000))
        assertEquals(ErrorCode.VALIDATION_INVALID_INPUT, ErrorCode.fromCode(3000))
        assertEquals(ErrorCode.BUSINESS_RESOURCE_NOT_FOUND, ErrorCode.fromCode(4000))
        assertEquals(ErrorCode.SYSTEM_UNKNOWN_ERROR, ErrorCode.fromCode(5000))
    }

    @Test
    fun fromCode_returnsUnknownForInvalidCode() {
        assertEquals(ErrorCode.SYSTEM_UNKNOWN_ERROR, ErrorCode.fromCode(-1))
        assertEquals(ErrorCode.SYSTEM_UNKNOWN_ERROR, ErrorCode.fromCode(0))
        assertEquals(ErrorCode.SYSTEM_UNKNOWN_ERROR, ErrorCode.fromCode(999))
        assertEquals(ErrorCode.SYSTEM_UNKNOWN_ERROR, ErrorCode.fromCode(6000))
        assertEquals(ErrorCode.SYSTEM_UNKNOWN_ERROR, ErrorCode.fromCode(9999))
    }

    // ============================================================================
    // fromHttpStatus tests
    // ============================================================================

    @Test
    fun fromHttpStatus_mapsCommonStatusCodes() {
        assertEquals(ErrorCode.VALIDATION_INVALID_INPUT, ErrorCode.fromHttpStatus(400))
        assertEquals(ErrorCode.AUTH_UNAUTHORIZED, ErrorCode.fromHttpStatus(401))
        assertEquals(ErrorCode.AUTH_FORBIDDEN, ErrorCode.fromHttpStatus(403))
        assertEquals(ErrorCode.BUSINESS_RESOURCE_NOT_FOUND, ErrorCode.fromHttpStatus(404))
        assertEquals(ErrorCode.NETWORK_TIMEOUT, ErrorCode.fromHttpStatus(408))
        assertEquals(ErrorCode.BUSINESS_RESOURCE_CONFLICT, ErrorCode.fromHttpStatus(409))
        assertEquals(ErrorCode.BUSINESS_RATE_LIMIT_EXCEEDED, ErrorCode.fromHttpStatus(429))
        assertEquals(ErrorCode.SYSTEM_INTERNAL_ERROR, ErrorCode.fromHttpStatus(500))
        assertEquals(ErrorCode.NETWORK_SERVER_ERROR, ErrorCode.fromHttpStatus(502))
        assertEquals(ErrorCode.SYSTEM_SERVICE_UNAVAILABLE, ErrorCode.fromHttpStatus(503))
    }

    @Test
    fun fromHttpStatus_returnsUnknownForUnmappedStatus() {
        assertEquals(ErrorCode.SYSTEM_UNKNOWN_ERROR, ErrorCode.fromHttpStatus(418)) // I'm a teapot
        assertEquals(ErrorCode.SYSTEM_UNKNOWN_ERROR, ErrorCode.fromHttpStatus(505))
        assertEquals(ErrorCode.SYSTEM_UNKNOWN_ERROR, ErrorCode.fromHttpStatus(999))
        assertEquals(ErrorCode.SYSTEM_UNKNOWN_ERROR, ErrorCode.fromHttpStatus(200)) // Success status
        assertEquals(ErrorCode.SYSTEM_UNKNOWN_ERROR, ErrorCode.fromHttpStatus(0))
    }

    // ============================================================================
    // getByCategory tests
    // ============================================================================

    @Test
    fun getByCategory_returnsCorrectCodes() {
        val networkCodes = ErrorCode.getByCategory("NETWORK")
        assertTrue(networkCodes.contains(ErrorCode.NETWORK_TIMEOUT))
        assertTrue(networkCodes.contains(ErrorCode.NETWORK_NO_CONNECTION))
        assertTrue(networkCodes.all { it.name.startsWith("NETWORK_") })

        val authCodes = ErrorCode.getByCategory("AUTH")
        assertTrue(authCodes.contains(ErrorCode.AUTH_UNAUTHORIZED))
        assertTrue(authCodes.contains(ErrorCode.AUTH_TOKEN_EXPIRED))
        assertTrue(authCodes.all { it.name.startsWith("AUTH_") })
    }

    @Test
    fun getByCategory_isCaseInsensitive() {
        val upperCase = ErrorCode.getByCategory("NETWORK")
        val lowerCase = ErrorCode.getByCategory("network")
        val mixedCase = ErrorCode.getByCategory("Network")

        assertEquals(upperCase, lowerCase)
        assertEquals(upperCase, mixedCase)
    }

    @Test
    fun getByCategory_returnsEmptyForUnknownCategory() {
        val unknownCategory = ErrorCode.getByCategory("UNKNOWN_CATEGORY")
        assertTrue(unknownCategory.isEmpty())
    }

    // ============================================================================
    // Convenience category list methods tests
    // ============================================================================

    @Test
    fun categoryListMethods_returnCorrectCodes() {
        assertEquals(
            ErrorCode.entries.filter { it.isNetworkError() },
            ErrorCode.networkErrors()
        )
        assertEquals(
            ErrorCode.entries.filter { it.isAuthError() },
            ErrorCode.authErrors()
        )
        assertEquals(
            ErrorCode.entries.filter { it.isValidationError() },
            ErrorCode.validationErrors()
        )
        assertEquals(
            ErrorCode.entries.filter { it.isBusinessError() },
            ErrorCode.businessErrors()
        )
        assertEquals(
            ErrorCode.entries.filter { it.isSystemError() },
            ErrorCode.systemErrors()
        )
    }

    // ============================================================================
    // Specific error codes existence tests
    // ============================================================================

    @Test
    fun networkCategory_hasExpectedCodes() {
        val networkCodes = ErrorCode.networkErrors()
        assertTrue(networkCodes.any { it.name.contains("TIMEOUT") })
        assertTrue(networkCodes.any { it.name.contains("NO_CONNECTION") || it.name.contains("CONNECTION") })
        assertTrue(networkCodes.any { it.name.contains("SERVER_ERROR") })
    }

    @Test
    fun authCategory_hasExpectedCodes() {
        val authCodes = ErrorCode.authErrors()
        assertTrue(authCodes.any { it.name.contains("UNAUTHORIZED") })
        assertTrue(authCodes.any { it.name.contains("TOKEN_EXPIRED") || it.name.contains("EXPIRED") })
        assertTrue(authCodes.any { it.name.contains("INVALID_CREDENTIALS") || it.name.contains("CREDENTIALS") })
        assertTrue(authCodes.any { it.name.contains("FORBIDDEN") })
    }

    @Test
    fun validationCategory_hasExpectedCodes() {
        val validationCodes = ErrorCode.validationErrors()
        assertTrue(validationCodes.any { it.name.contains("INVALID_INPUT") || it.name.contains("INPUT") })
        assertTrue(validationCodes.any { it.name.contains("MISSING_FIELD") || it.name.contains("MISSING") })
        assertTrue(validationCodes.any { it.name.contains("FORMAT") })
    }

    @Test
    fun businessCategory_hasExpectedCodes() {
        val businessCodes = ErrorCode.businessErrors()
        assertTrue(businessCodes.any { it.name.contains("NOT_FOUND") || it.name.contains("RESOURCE_NOT_FOUND") })
        assertTrue(businessCodes.any { it.name.contains("NOT_ALLOWED") || it.name.contains("OPERATION") })
    }

    @Test
    fun systemCategory_hasExpectedCodes() {
        val systemCodes = ErrorCode.systemErrors()
        assertTrue(systemCodes.any { it.name.contains("UNKNOWN") })
        assertTrue(systemCodes.any { it.name.contains("CONFIGURATION") || it.name.contains("CONFIG") })
        assertTrue(systemCodes.any { it.name.contains("UNAVAILABLE") || it.name.contains("SERVICE") })
    }

    // ============================================================================
    // General enum behavior tests
    // ============================================================================

    @Test
    fun errorCode_canBeUsedInWhenExpression() {
        val code = ErrorCode.VALIDATION_INVALID_INPUT
        val message = when {
            code.isValidationError() -> "Validation error"
            code.isNetworkError() -> "Network error"
            code.isAuthError() -> "Auth error"
            else -> "Other error"
        }
        assertEquals("Validation error", message)
    }

    @Test
    fun errorCode_hasCorrectToString() {
        assertEquals("VALIDATION_INVALID_INPUT", ErrorCode.VALIDATION_INVALID_INPUT.toString())
        assertEquals("NETWORK_TIMEOUT", ErrorCode.NETWORK_TIMEOUT.toString())
        assertEquals("AUTH_UNAUTHORIZED", ErrorCode.AUTH_UNAUTHORIZED.toString())
    }

    @Test
    fun errorCode_comparison() {
        val code1 = ErrorCode.VALIDATION_INVALID_INPUT
        val code2 = ErrorCode.VALIDATION_INVALID_INPUT
        val code3 = ErrorCode.NETWORK_TIMEOUT

        assertEquals(code1, code2)
        assertTrue(code1 != code3)
    }

    @Test
    fun errorCode_namingConventionFollowed() {
        ErrorCode.entries.forEach { code ->
            val parts = code.name.split("_")
            assertTrue(
                parts.size >= 2,
                "ErrorCode ${code.name} should follow CATEGORY_SPECIFIC_ERROR naming convention"
            )
            val validCategories = listOf("NETWORK", "AUTH", "VALIDATION", "BUSINESS", "SYSTEM")
            assertTrue(
                validCategories.contains(parts[0]),
                "ErrorCode ${code.name} should start with a valid category prefix"
            )
        }
    }
}
