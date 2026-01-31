package com.edugo.test.module.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertFailsWith

class AppErrorTest {

    // Basic construction tests
    @Test
    fun appError_canBeConstructedWithAllParameters() {
        val error = AppError(
            code = ErrorCode.VALIDATION,
            message = "Test error",
            inputDetails = mapOf("field" to "email"),
            cause = IllegalArgumentException("Invalid email")
        )

        assertEquals(ErrorCode.VALIDATION, error.code)
        assertEquals("Test error", error.message)
        assertEquals(mapOf("field" to "email"), error.details)
        assertNotNull(error.cause)
        assertTrue(error.timestamp > 0)
    }

    @Test
    fun appError_canBeConstructedWithMinimalParameters() {
        val error = AppError(
            code = ErrorCode.NOT_FOUND,
            message = "Resource not found"
        )

        assertEquals(ErrorCode.NOT_FOUND, error.code)
        assertEquals("Resource not found", error.message)
        assertTrue(error.details.isEmpty())
        assertNull(error.cause)
    }

    @Test
    fun appError_requiresNonBlankMessage() {
        assertFailsWith<IllegalArgumentException> {
            AppError(
                code = ErrorCode.UNKNOWN,
                message = ""
            )
        }

        assertFailsWith<IllegalArgumentException> {
            AppError(
                code = ErrorCode.UNKNOWN,
                message = "   "
            )
        }
    }

    // fromException tests
    @Test
    fun fromException_createsErrorFromException() {
        val exception = RuntimeException("Something went wrong")
        val error = AppError.fromException(exception)

        assertEquals(ErrorCode.UNKNOWN, error.code)
        assertEquals("Something went wrong", error.message)
        assertEquals(exception, error.cause)
    }

    @Test
    fun fromException_usesProvidedCode() {
        val exception = RuntimeException("Network failed")
        val error = AppError.fromException(
            exception = exception,
            code = ErrorCode.NETWORK
        )

        assertEquals(ErrorCode.NETWORK, error.code)
        assertEquals("Network failed", error.message)
    }

    @Test
    fun fromException_includesDetails() {
        val exception = RuntimeException("Error")
        val error = AppError.fromException(
            exception = exception,
            code = ErrorCode.SERVER_ERROR,
            inputDetails = mapOf("endpoint" to "/api/users", "status" to 500)
        )

        assertEquals(mapOf("endpoint" to "/api/users", "status" to 500), error.details)
    }

    @Test
    fun fromException_usesDefaultMessageWhenExceptionMessageIsNull() {
        val exception = RuntimeException()
        val error = AppError.fromException(
            exception = exception,
            code = ErrorCode.NETWORK
        )

        assertEquals(ErrorCode.NETWORK.defaultMessage, error.message)
    }

    @Test
    fun fromException_usesDefaultMessageWhenExceptionMessageIsBlank() {
        val exception = RuntimeException("   ")
        val error = AppError.fromException(
            exception = exception,
            code = ErrorCode.TIMEOUT
        )

        assertEquals(ErrorCode.TIMEOUT.defaultMessage, error.message)
    }

    // fromCode tests
    @Test
    fun fromCode_createsErrorFromCode() {
        val error = AppError.fromCode(ErrorCode.VALIDATION)

        assertEquals(ErrorCode.VALIDATION, error.code)
        assertEquals(ErrorCode.VALIDATION.defaultMessage, error.message)
        assertNull(error.cause)
    }

    @Test
    fun fromCode_usesCustomMessage() {
        val error = AppError.fromCode(
            code = ErrorCode.NOT_FOUND,
            customMessage = "User with ID 123 not found"
        )

        assertEquals(ErrorCode.NOT_FOUND, error.code)
        assertEquals("User with ID 123 not found", error.message)
    }

    @Test
    fun fromCode_includesDetails() {
        val error = AppError.fromCode(
            code = ErrorCode.CONFLICT,
            customMessage = "Resource already exists",
            inputDetails = mapOf("resourceId" to 456)
        )

        assertEquals(mapOf("resourceId" to 456), error.details)
    }

    // validation tests
    @Test
    fun validation_createsValidationError() {
        val error = AppError.validation(
            message = "Email is required"
        )

        assertEquals(ErrorCode.VALIDATION, error.code)
        assertEquals("Email is required", error.message)
    }

    @Test
    fun validation_includesFieldInDetails() {
        val error = AppError.validation(
            message = "Invalid format",
            field = "phoneNumber"
        )

        assertEquals(ErrorCode.VALIDATION, error.code)
        assertEquals("Invalid format", error.message)
        assertEquals(mapOf("field" to "phoneNumber"), error.details)
    }

    @Test
    fun validation_mergesFieldWithAdditionalDetails() {
        val error = AppError.validation(
            message = "Value out of range",
            field = "age",
            inputDetails = mapOf("min" to 18, "max" to 120, "actual" to 150)
        )

        assertEquals(
            mapOf("field" to "age", "min" to 18, "max" to 120, "actual" to 150),
            error.details
        )
    }

    @Test
    fun validation_worksWithoutField() {
        val error = AppError.validation(
            message = "General validation error",
            inputDetails = mapOf("errors" to 5)
        )

        assertEquals(mapOf("errors" to 5), error.details)
    }

    // network tests
    @Test
    fun network_createsNetworkError() {
        val exception = RuntimeException("Connection refused")
        val error = AppError.network(exception)

        assertEquals(ErrorCode.NETWORK, error.code)
        assertEquals("Connection refused", error.message)
        assertEquals(exception, error.cause)
    }

    @Test
    fun network_includesDetails() {
        val exception = RuntimeException("Timeout")
        val error = AppError.network(
            cause = exception,
            inputDetails = mapOf("url" to "https://api.example.com", "timeout" to 30)
        )

        assertEquals(mapOf("url" to "https://api.example.com", "timeout" to 30), error.details)
    }

    // timeout tests
    @Test
    fun timeout_createsTimeoutError() {
        val error = AppError.timeout()

        assertEquals(ErrorCode.TIMEOUT, error.code)
        assertEquals(ErrorCode.TIMEOUT.defaultMessage, error.message)
    }

    @Test
    fun timeout_usesCustomMessage() {
        val error = AppError.timeout(
            message = "Request timed out after 30 seconds"
        )

        assertEquals("Request timed out after 30 seconds", error.message)
    }

    @Test
    fun timeout_includesDetails() {
        val error = AppError.timeout(
            message = "Operation timed out",
            inputDetails = mapOf("timeout" to "30s", "operation" to "fetchData")
        )

        assertEquals(mapOf("timeout" to "30s", "operation" to "fetchData"), error.details)
    }

    // unauthorized tests
    @Test
    fun unauthorized_createsUnauthorizedError() {
        val error = AppError.unauthorized()

        assertEquals(ErrorCode.UNAUTHORIZED, error.code)
        assertEquals(ErrorCode.UNAUTHORIZED.defaultMessage, error.message)
    }

    @Test
    fun unauthorized_usesCustomMessage() {
        val error = AppError.unauthorized(
            message = "Token has expired"
        )

        assertEquals("Token has expired", error.message)
    }

    @Test
    fun unauthorized_includesDetails() {
        val error = AppError.unauthorized(
            message = "Invalid credentials",
            inputDetails = mapOf("loginAttempts" to 3)
        )

        assertEquals(mapOf("loginAttempts" to 3), error.details)
    }

    // notFound tests
    @Test
    fun notFound_createsNotFoundError() {
        val error = AppError.notFound()

        assertEquals(ErrorCode.NOT_FOUND, error.code)
        assertEquals(ErrorCode.NOT_FOUND.defaultMessage, error.message)
    }

    @Test
    fun notFound_usesCustomMessage() {
        val error = AppError.notFound(
            message = "User not found"
        )

        assertEquals("User not found", error.message)
    }

    @Test
    fun notFound_includesDetails() {
        val error = AppError.notFound(
            message = "Resource not found",
            inputDetails = mapOf("userId" to 123, "resource" to "User")
        )

        assertEquals(mapOf("userId" to 123, "resource" to "User"), error.details)
    }

    // serverError tests
    @Test
    fun serverError_createsServerErrorWithoutCause() {
        val error = AppError.serverError()

        assertEquals(ErrorCode.SERVER_ERROR, error.code)
        assertEquals(ErrorCode.SERVER_ERROR.defaultMessage, error.message)
        assertNull(error.cause)
    }

    @Test
    fun serverError_createsServerErrorWithCause() {
        val exception = RuntimeException("Database error")
        val error = AppError.serverError(cause = exception)

        assertEquals(ErrorCode.SERVER_ERROR, error.code)
        assertEquals("Database error", error.message)
        assertEquals(exception, error.cause)
    }

    @Test
    fun serverError_usesCustomMessage() {
        val error = AppError.serverError(
            message = "Internal server error occurred"
        )

        assertEquals("Internal server error occurred", error.message)
    }

    @Test
    fun serverError_prioritizesCustomMessageOverCause() {
        val exception = RuntimeException("Exception message")
        val error = AppError.serverError(
            cause = exception,
            message = "Custom message"
        )

        assertEquals("Custom message", error.message)
    }

    @Test
    fun serverError_includesDetails() {
        val error = AppError.serverError(
            message = "Server error",
            inputDetails = mapOf("errorId" to "ERR-123")
        )

        assertEquals(mapOf("errorId" to "ERR-123"), error.details)
    }

    // getRootCause tests
    @Test
    fun getRootCause_returnsNullWhenNoCause() {
        val error = AppError.fromCode(ErrorCode.VALIDATION)
        assertNull(error.getRootCause())
    }

    @Test
    fun getRootCause_returnsCauseWhenSingleLevel() {
        val cause = RuntimeException("Root")
        val error = AppError.fromException(cause)

        assertEquals(cause, error.getRootCause())
    }

    @Test
    fun getRootCause_returnsDeepestCause() {
        val root = IllegalArgumentException("Root cause")
        val middle = RuntimeException("Middle", root)
        val top = Exception("Top", middle)
        val error = AppError.fromException(top)

        assertEquals(root, error.getRootCause())
    }

    // getAllCauses tests
    @Test
    fun getAllCauses_returnsEmptyListWhenNoCause() {
        val error = AppError.fromCode(ErrorCode.NETWORK)
        assertTrue(error.getAllCauses().isEmpty())
    }

    @Test
    fun getAllCauses_returnsSingleCause() {
        val cause = RuntimeException("Error")
        val error = AppError.fromException(cause)

        assertEquals(listOf(cause), error.getAllCauses())
    }

    @Test
    fun getAllCauses_returnsAllCausesInOrder() {
        val root = IllegalArgumentException("Root")
        val middle = RuntimeException("Middle", root)
        val top = Exception("Top", middle)
        val error = AppError.fromException(top)

        assertEquals(listOf(top, middle, root), error.getAllCauses())
    }

    // getStackTraceString tests
    @Test
    fun getStackTraceString_returnsEmptyWhenNoCause() {
        val error = AppError.fromCode(ErrorCode.TIMEOUT)
        assertEquals("", error.getStackTraceString())
    }

    @Test
    fun getStackTraceString_includesStackTrace() {
        val cause = RuntimeException("Test error")
        val error = AppError.fromException(cause)
        val stackTrace = error.getStackTraceString()

        assertTrue(stackTrace.contains("Stack trace:"))
        assertTrue(stackTrace.contains("RuntimeException"))
        assertTrue(stackTrace.contains("Test error"))
    }

    @Test
    fun getStackTraceString_includesCauseChain() {
        val root = IllegalArgumentException("Root")
        val middle = RuntimeException("Middle", root)
        val top = Exception("Top", middle)
        val error = AppError.fromException(top)
        val stackTrace = error.getStackTraceString()

        assertTrue(stackTrace.contains("Cause chain:"))
        assertTrue(stackTrace.contains("Exception: Top"))
        assertTrue(stackTrace.contains("RuntimeException: Middle"))
        assertTrue(stackTrace.contains("IllegalArgumentException: Root"))
    }

    // isRetryable tests
    @Test
    fun isRetryable_delegatesToErrorCode() {
        val networkError = AppError.fromCode(ErrorCode.NETWORK)
        val timeoutError = AppError.fromCode(ErrorCode.TIMEOUT)
        val validationError = AppError.fromCode(ErrorCode.VALIDATION)

        assertTrue(networkError.isRetryable())
        assertTrue(timeoutError.isRetryable())
        assertFalse(validationError.isRetryable())
    }

    // isClientError and isServerError tests
    @Test
    fun isClientError_delegatesToErrorCode() {
        val validationError = AppError.fromCode(ErrorCode.VALIDATION)
        val notFoundError = AppError.fromCode(ErrorCode.NOT_FOUND)
        val serverError = AppError.fromCode(ErrorCode.SERVER_ERROR)

        assertTrue(validationError.isClientError())
        assertTrue(notFoundError.isClientError())
        assertFalse(serverError.isClientError())
    }

    @Test
    fun isServerError_delegatesToErrorCode() {
        val serverError = AppError.fromCode(ErrorCode.SERVER_ERROR)
        val serviceUnavailable = AppError.fromCode(ErrorCode.SERVICE_UNAVAILABLE)
        val validationError = AppError.fromCode(ErrorCode.VALIDATION)

        assertTrue(serverError.isServerError())
        assertTrue(serviceUnavailable.isServerError())
        assertFalse(validationError.isServerError())
    }

    // withDetails tests
    @Test
    fun withDetails_addsNewDetails() {
        val error = AppError.fromCode(ErrorCode.NOT_FOUND)
        val enriched = error.withDetails("userId" to 123, "resource" to "User")

        assertEquals(mapOf("userId" to 123, "resource" to "User"), enriched.details)
        assertEquals(error.code, enriched.code)
        assertEquals(error.message, enriched.message)
    }

    @Test
    fun withDetails_mergesWithExistingDetails() {
        val error = AppError.fromCode(
            code = ErrorCode.VALIDATION,
            inputDetails = mapOf("field" to "email")
        )
        val enriched = error.withDetails("constraint" to "format", "pattern" to ".*@.*")

        assertEquals(
            mapOf("field" to "email", "constraint" to "format", "pattern" to ".*@.*"),
            enriched.details
        )
    }

    @Test
    fun withDetails_overwritesExistingKeys() {
        val error = AppError.fromCode(
            code = ErrorCode.CONFLICT,
            inputDetails = mapOf("resourceId" to 1)
        )
        val enriched = error.withDetails("resourceId" to 2, "version" to 1)

        assertEquals(mapOf("resourceId" to 2, "version" to 1), enriched.details)
    }

    @Test
    fun withDetails_preservesImmutability() {
        val error = AppError.fromCode(
            code = ErrorCode.NETWORK,
            inputDetails = mapOf("attempt" to 1)
        )
        val enriched = error.withDetails("attempt" to 2)

        assertEquals(mapOf("attempt" to 1), error.details)
        assertEquals(mapOf("attempt" to 2), enriched.details)
    }

    // toUserMessage tests
    @Test
    fun toUserMessage_providesUserFriendlyMessages() {
        assertEquals(
            "Please check your internet connection and try again",
            AppError.fromCode(ErrorCode.NETWORK).toUserMessage()
        )
        assertEquals(
            "The operation took too long. Please try again",
            AppError.fromCode(ErrorCode.TIMEOUT).toUserMessage()
        )
        assertEquals(
            "The service is temporarily unavailable. Please try again later",
            AppError.fromCode(ErrorCode.SERVICE_UNAVAILABLE).toUserMessage()
        )
        assertEquals(
            "Too many requests. Please wait a moment and try again",
            AppError.fromCode(ErrorCode.RATE_LIMIT_EXCEEDED).toUserMessage()
        )
        assertEquals(
            "Please sign in to continue",
            AppError.fromCode(ErrorCode.UNAUTHORIZED).toUserMessage()
        )
        assertEquals(
            "You don't have permission to perform this action",
            AppError.fromCode(ErrorCode.FORBIDDEN).toUserMessage()
        )
        assertEquals(
            "The requested resource was not found",
            AppError.fromCode(ErrorCode.NOT_FOUND).toUserMessage()
        )
    }

    @Test
    fun toUserMessage_showsValidationMessageDirectly() {
        val error = AppError.validation("Email is required")
        assertEquals("Email is required", error.toUserMessage())
    }

    @Test
    fun toUserMessage_showsGenericForUnknownErrors() {
        assertEquals(
            "Something went wrong. Please try again",
            AppError.fromCode(ErrorCode.UNKNOWN).toUserMessage()
        )
        assertEquals(
            "Something went wrong. Please try again",
            AppError.fromCode(ErrorCode.SERVER_ERROR).toUserMessage()
        )
        assertEquals(
            "Something went wrong. Please try again",
            AppError.fromCode(ErrorCode.SERIALIZATION).toUserMessage()
        )
    }

    // toString tests
    @Test
    fun toString_includesCodeAndMessage() {
        val error = AppError.fromCode(ErrorCode.VALIDATION, "Invalid input")
        val str = error.toString()

        assertTrue(str.contains("code=VALIDATION"))
        assertTrue(str.contains("message=\"Invalid input\""))
    }

    @Test
    fun toString_includesDetailsWhenPresent() {
        val error = AppError.fromCode(
            code = ErrorCode.NOT_FOUND,
            customMessage = "User not found",
            inputDetails = mapOf("userId" to 123)
        )
        val str = error.toString()

        assertTrue(str.contains("details={userId=123}"))
    }

    @Test
    fun toString_omitsDetailsWhenEmpty() {
        val error = AppError.fromCode(ErrorCode.NETWORK)
        val str = error.toString()

        assertFalse(str.contains("details="))
    }

    @Test
    fun toString_includesCauseWhenPresent() {
        val cause = RuntimeException("Root cause")
        val error = AppError.fromException(cause)
        val str = error.toString()

        assertTrue(str.contains("cause=RuntimeException: Root cause"))
    }

    @Test
    fun toString_startsWith_AppError() {
        val error = AppError.fromCode(ErrorCode.TIMEOUT)
        assertTrue(error.toString().startsWith("AppError["))
    }

    // Data class behavior tests
    @Test
    fun appError_dataClassCopy() {
        val error = AppError(
            code = ErrorCode.VALIDATION,
            message = "Original message",
            inputDetails = mapOf("field" to "email")
        )

        val copied = error.copy(message = "Updated message")

        assertEquals(ErrorCode.VALIDATION, copied.code)
        assertEquals("Updated message", copied.message)
        assertEquals(mapOf("field" to "email"), copied.details)
    }

    @Test
    fun appError_dataClassEquals() {
        val error1 = AppError(
            code = ErrorCode.NETWORK,
            message = "Network error",
            timestamp = 1000L
        )
        val error2 = AppError(
            code = ErrorCode.NETWORK,
            message = "Network error",
            timestamp = 1000L
        )
        val error3 = AppError(
            code = ErrorCode.NETWORK,
            message = "Different message",
            timestamp = 1000L
        )

        assertEquals(error1, error2)
        assertTrue(error1 != error3)
    }

    @Test
    fun appError_immutableDetails() {
        val mutableMap = mutableMapOf("key" to "value")
        val error = AppError(
            code = ErrorCode.UNKNOWN,
            message = "Test",
            inputDetails = mutableMap
        )

        mutableMap["key"] = "changed"

        // Error should have original value
        assertEquals(mapOf("key" to "value"), error.details)
    }

    // Integration tests
    @Test
    fun integration_fullErrorLifecycle() {
        val originalException = IllegalArgumentException("Invalid format")
        val error = AppError.fromException(
            exception = originalException,
            code = ErrorCode.VALIDATION,
            inputDetails = mapOf("field" to "email")
        )

        val enriched = error.withDetails(
            "userId" to 123,
            "attempt" to 1
        )

        assertEquals(ErrorCode.VALIDATION, enriched.code)
        assertEquals("Invalid format", enriched.message)
        assertEquals(
            mapOf("field" to "email", "userId" to 123, "attempt" to 1),
            enriched.details
        )
        assertEquals(originalException, enriched.cause)
        assertEquals(originalException, enriched.getRootCause())
        assertTrue(enriched.isClientError())
        assertFalse(enriched.isRetryable())
    }

    @Test
    fun integration_chainedExceptionsAnalysis() {
        val root = IllegalStateException("Invalid state")
        val middle = RuntimeException("Processing failed", root)
        val top = Exception("Request failed", middle)

        val error = AppError.fromException(
            exception = top,
            code = ErrorCode.SERVER_ERROR,
            inputDetails = mapOf("endpoint" to "/api/process")
        )

        assertEquals(root, error.getRootCause())
        assertEquals(listOf(top, middle, root), error.getAllCauses())

        val stackTrace = error.getStackTraceString()
        assertTrue(stackTrace.contains("Exception: Request failed"))
        assertTrue(stackTrace.contains("RuntimeException: Processing failed"))
        assertTrue(stackTrace.contains("IllegalStateException: Invalid state"))
    }

    @Test
    fun integration_errorFactoryMethods() {
        val errors = listOf(
            AppError.validation("Invalid email", "email"),
            AppError.network(RuntimeException("Connection failed")),
            AppError.timeout(inputDetails = mapOf("duration" to "30s")),
            AppError.unauthorized("Session expired"),
            AppError.notFound("User not found", mapOf("userId" to 123)),
            AppError.serverError(message = "Database error")
        )

        assertEquals(ErrorCode.VALIDATION, errors[0].code)
        assertEquals(ErrorCode.NETWORK, errors[1].code)
        assertEquals(ErrorCode.TIMEOUT, errors[2].code)
        assertEquals(ErrorCode.UNAUTHORIZED, errors[3].code)
        assertEquals(ErrorCode.NOT_FOUND, errors[4].code)
        assertEquals(ErrorCode.SERVER_ERROR, errors[5].code)
    }
}
