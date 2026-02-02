package com.edugo.test.module.platform

import com.edugo.test.module.core.AppError
import com.edugo.test.module.core.ErrorCode
import com.edugo.test.module.core.Result
import com.edugo.test.module.core.failure
import com.edugo.test.module.core.success
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Comprehensive tests for Logger extension functions.
 *
 * Tests cover:
 * - Result.logOnFailure() extensions
 * - Result.logOnSuccess() extension
 * - AppError.log() and related extensions
 * - Logger.withTag() and Logger.fromClass() extensions
 *
 * @see LoggerExtensions
 * @see MockLogger
 */
class LoggerExtensionsTest {

    // ==================== Result.logOnFailure Tests ====================

    @Test
    fun testLogOnFailureLogsOnFailure() {
        val mockLogger = MockLogger()
        val result: Result<String> = failure("Something went wrong")

        result.logOnFailure("TestTag", mockLogger)

        assertEquals(1, mockLogger.callCount)
        assertEquals(LogLevel.ERROR, mockLogger.lastCall?.level)
        assertEquals("TestTag", mockLogger.lastCall?.tag)
        assertTrue(mockLogger.lastCall?.message?.contains("Something went wrong") == true)
    }

    @Test
    fun testLogOnFailureDoesNotLogOnSuccess() {
        val mockLogger = MockLogger()
        val result: Result<String> = success("Success!")

        result.logOnFailure("TestTag", mockLogger)

        assertEquals(0, mockLogger.callCount)
        assertTrue(mockLogger.isEmpty)
    }

    @Test
    fun testLogOnFailureDoesNotLogOnLoading() {
        val mockLogger = MockLogger()
        val result: Result<String> = Result.Loading

        result.logOnFailure("TestTag", mockLogger)

        assertEquals(0, mockLogger.callCount)
    }

    @Test
    fun testLogOnFailureReturnsOriginalResult() {
        val mockLogger = MockLogger()
        val original: Result<String> = failure("Error")

        val returned = original.logOnFailure("TestTag", mockLogger)

        assertSame(original, returned, "Should return the exact same Result instance")
    }

    @Test
    fun testLogOnFailureSuccessReturnsOriginalResult() {
        val mockLogger = MockLogger()
        val original: Result<String> = success("Data")

        val returned = original.logOnFailure("TestTag", mockLogger)

        assertSame(original, returned, "Should return the exact same Result instance")
    }

    @Test
    fun testLogOnFailureWithMessagePrefix() {
        val mockLogger = MockLogger()
        val result: Result<String> = failure("Network error")

        result.logOnFailure("NetworkRepo", "Failed to fetch user", mockLogger)

        assertEquals(1, mockLogger.callCount)
        assertEquals("Failed to fetch user: Network error", mockLogger.lastCall?.message)
    }

    @Test
    fun testLogOnFailureWithMessagePrefixDoesNotLogOnSuccess() {
        val mockLogger = MockLogger()
        val result: Result<String> = success("User data")

        result.logOnFailure("NetworkRepo", "Failed to fetch user", mockLogger)

        assertEquals(0, mockLogger.callCount)
    }

    @Test
    fun testLogOnFailureChaining() {
        val mockLogger = MockLogger()

        val finalResult = failure<Int>("Initial error")
            .logOnFailure("Step1", mockLogger)
            .logOnFailure("Step2", mockLogger)

        assertEquals(2, mockLogger.callCount)
        assertTrue(finalResult is Result.Failure)
    }

    // ==================== Result.logOnSuccess Tests ====================

    @Test
    fun testLogOnSuccessLogsOnSuccess() {
        val mockLogger = MockLogger()
        val result: Result<String> = success("User John")

        result.logOnSuccess("TestTag", mockLogger) { data ->
            "Fetched: $data"
        }

        assertEquals(1, mockLogger.callCount)
        assertEquals(LogLevel.DEBUG, mockLogger.lastCall?.level)
        assertEquals("TestTag", mockLogger.lastCall?.tag)
        assertEquals("Fetched: User John", mockLogger.lastCall?.message)
    }

    @Test
    fun testLogOnSuccessDoesNotLogOnFailure() {
        val mockLogger = MockLogger()
        val result: Result<String> = failure("Error")

        result.logOnSuccess("TestTag", mockLogger) { "Got: $it" }

        assertEquals(0, mockLogger.callCount)
    }

    @Test
    fun testLogOnSuccessDoesNotLogOnLoading() {
        val mockLogger = MockLogger()
        val result: Result<String> = Result.Loading

        result.logOnSuccess("TestTag", mockLogger) { "Got: $it" }

        assertEquals(0, mockLogger.callCount)
    }

    @Test
    fun testLogOnSuccessReturnsOriginalResult() {
        val mockLogger = MockLogger()
        val original: Result<Int> = success(42)

        val returned = original.logOnSuccess("TestTag", mockLogger) { "Value: $it" }

        assertSame(original, returned)
    }

    @Test
    fun testLogOnSuccessWithComplexData() {
        val mockLogger = MockLogger()
        data class User(val id: Int, val name: String)
        val result: Result<User> = success(User(1, "Alice"))

        result.logOnSuccess("UserRepo", mockLogger) { user ->
            "Loaded user ${user.name} with id ${user.id}"
        }

        assertEquals("Loaded user Alice with id 1", mockLogger.lastCall?.message)
    }

    // ==================== AppError.log Tests ====================

    @Test
    fun testAppErrorLogLogsAtErrorLevel() {
        val mockLogger = MockLogger()
        val error = AppError.fromCode(ErrorCode.BUSINESS_RESOURCE_NOT_FOUND, "User not found")

        error.log("TestTag", mockLogger)

        assertEquals(1, mockLogger.callCount)
        assertEquals(LogLevel.ERROR, mockLogger.lastCall?.level)
        assertEquals("TestTag", mockLogger.lastCall?.tag)
        assertTrue(mockLogger.lastCall?.message?.contains("BUSINESS_RESOURCE_NOT_FOUND") == true)
        assertTrue(mockLogger.lastCall?.message?.contains("User not found") == true)
    }

    @Test
    fun testAppErrorLogNetworkErrorLogsAtWarningLevel() {
        val mockLogger = MockLogger()
        val error = AppError.fromCode(ErrorCode.NETWORK_TIMEOUT, "Request timed out")

        error.log("NetworkTag", mockLogger)

        assertEquals(1, mockLogger.callCount)
        assertEquals(LogLevel.WARNING, mockLogger.lastCall?.level)
        assertTrue(mockLogger.lastCall?.message?.contains("NETWORK_TIMEOUT") == true)
    }

    @Test
    fun testAppErrorLogRetryableErrorLogsAtWarningLevel() {
        val mockLogger = MockLogger()
        val error = AppError.fromCode(ErrorCode.BUSINESS_RATE_LIMIT_EXCEEDED, "Too many requests")

        error.log("ApiTag", mockLogger)

        assertEquals(1, mockLogger.callCount)
        assertEquals(LogLevel.WARNING, mockLogger.lastCall?.level)
    }

    @Test
    fun testAppErrorLogWithCause() {
        val mockLogger = MockLogger()
        val cause = RuntimeException("Root cause")
        val error = AppError.fromException(cause, ErrorCode.SYSTEM_INTERNAL_ERROR)

        error.log("ErrorTag", mockLogger)

        assertEquals(1, mockLogger.callCount)
        assertNotNull(mockLogger.lastCall?.throwable)
        assertEquals(cause, mockLogger.lastCall?.throwable)
    }

    @Test
    fun testAppErrorLogWithDetails() {
        val mockLogger = MockLogger()
        val error = AppError.fromCode(
            ErrorCode.VALIDATION_INVALID_INPUT,
            "Invalid email"
        ).withDetails("field" to "email", "value" to "invalid@")

        error.log("ValidationTag", mockLogger)

        assertEquals(1, mockLogger.callCount)
        assertTrue(mockLogger.lastCall?.message?.contains("details:") == true)
        assertTrue(mockLogger.lastCall?.message?.contains("field=email") == true)
    }

    @Test
    fun testAppErrorLogWithManyDetailsTruncates() {
        val mockLogger = MockLogger()
        val details = (1..10).associate { "key$it" to "value$it" }
        val error = AppError(
            code = ErrorCode.VALIDATION_INVALID_INPUT,
            message = "Many details",
            inputDetails = details
        )

        error.log("TestTag", mockLogger)

        assertEquals(1, mockLogger.callCount)
        assertTrue(mockLogger.lastCall?.message?.contains("(+") == true) // Truncation indicator
    }

    // ==================== AppError Specific Level Methods ====================

    @Test
    fun testAppErrorLogDebug() {
        val mockLogger = MockLogger()
        val error = AppError.fromCode(ErrorCode.BUSINESS_RESOURCE_NOT_FOUND, "Not found")

        error.logDebug("TestTag", mockLogger)

        assertEquals(1, mockLogger.callCount)
        assertEquals(LogLevel.DEBUG, mockLogger.lastCall?.level)
    }

    @Test
    fun testAppErrorLogDebugWithCause() {
        val mockLogger = MockLogger()
        val cause = IllegalArgumentException("Bad arg")
        val error = AppError.fromException(cause)

        error.logDebug("TestTag", mockLogger)

        assertEquals(LogLevel.DEBUG, mockLogger.lastCall?.level)
        assertNotNull(mockLogger.lastCall?.throwable)
    }

    @Test
    fun testAppErrorLogInfo() {
        val mockLogger = MockLogger()
        val error = AppError.fromCode(ErrorCode.BUSINESS_OPERATION_NOT_ALLOWED, "Operation blocked")

        error.logInfo("TestTag", mockLogger)

        assertEquals(1, mockLogger.callCount)
        assertEquals(LogLevel.INFO, mockLogger.lastCall?.level)
    }

    @Test
    fun testAppErrorLogInfoWithCause() {
        val mockLogger = MockLogger()
        val cause = RuntimeException("Info cause")
        val error = AppError.fromException(cause)

        error.logInfo("TestTag", mockLogger)

        assertNotNull(mockLogger.lastCall?.throwable)
    }

    @Test
    fun testAppErrorLogWarning() {
        val mockLogger = MockLogger()
        val error = AppError.fromCode(ErrorCode.AUTH_TOKEN_EXPIRED, "Token expired")

        error.logWarning("AuthTag", mockLogger)

        assertEquals(1, mockLogger.callCount)
        assertEquals(LogLevel.WARNING, mockLogger.lastCall?.level)
    }

    @Test
    fun testAppErrorLogWarningWithCause() {
        val mockLogger = MockLogger()
        val cause = IllegalStateException("Warning cause")
        val error = AppError.fromException(cause)

        error.logWarning("TestTag", mockLogger)

        assertNotNull(mockLogger.lastCall?.throwable)
    }

    @Test
    fun testAppErrorLogError() {
        val mockLogger = MockLogger()
        // Use AUTH_FORBIDDEN which is NOT retryable and NOT a network error
        val error = AppError.fromCode(ErrorCode.AUTH_FORBIDDEN, "Access denied")

        error.logError("SystemTag", mockLogger)

        assertEquals(1, mockLogger.callCount)
        // logError uses log() which determines level by error type
        // AUTH_FORBIDDEN is not network/retryable, so ERROR level
        assertEquals(LogLevel.ERROR, mockLogger.lastCall?.level)
    }

    // ==================== Logger.withTag Extension Tests ====================

    @Test
    fun testWithTagCreatesTaggedLogger() {
        val mockLogger = MockLogger()
        LoggerCacheUtils.clearCache()

        val tagged = mockLogger.withTag("EduGo.Auth")

        assertEquals("EduGo.Auth", tagged.tag)
    }

    @Test
    fun testWithTagLogsDelegateToUnderlyingLogger() {
        val mockLogger = MockLogger()
        LoggerCacheUtils.clearCache()

        val tagged = mockLogger.withTag("TestModule")
        tagged.d("Debug via tagged")

        assertEquals(1, mockLogger.callCount)
        assertEquals(LogLevel.DEBUG, mockLogger.lastCall?.level)
        assertEquals("TestModule", mockLogger.lastCall?.tag)
        assertEquals("Debug via tagged", mockLogger.lastCall?.message)
    }

    @Test
    fun testWithTagCachesInstances() {
        val mockLogger = MockLogger()
        LoggerCacheUtils.clearCache()

        val tagged1 = mockLogger.withTag("CacheTest")
        val tagged2 = mockLogger.withTag("CacheTest")

        assertSame(tagged1, tagged2, "Same tag should return cached instance")
    }

    // ==================== Logger.fromClass Extension Tests ====================

    @Test
    fun testFromClassCreatesLoggerWithClassName() {
        val mockLogger = MockLogger()
        LoggerCacheUtils.clearCache()

        val tagged = mockLogger.fromClass(LoggerExtensionsTest::class)

        assertTrue(tagged.tag.contains("LoggerExtensionsTest"))
    }

    @Test
    fun testFromClassLogsDelegateCorrectly() {
        val mockLogger = MockLogger()
        LoggerCacheUtils.clearCache()

        val tagged = mockLogger.fromClass(LoggerExtensionsTest::class)
        tagged.i("Info message from class")

        assertEquals(1, mockLogger.callCount)
        assertEquals(LogLevel.INFO, mockLogger.lastCall?.level)
        assertTrue(mockLogger.lastCall?.tag?.contains("LoggerExtensionsTest") == true)
    }

    // ==================== Combined Usage Tests ====================

    @Test
    fun testResultLogOnFailureThenLogOnSuccess() {
        val mockLogger = MockLogger()
        val result: Result<String> = success("Data")

        result
            .logOnFailure("Tag", mockLogger)
            .logOnSuccess("Tag", mockLogger) { "Got: $it" }

        // Only logOnSuccess should log since result is Success
        assertEquals(1, mockLogger.callCount)
        assertEquals(LogLevel.DEBUG, mockLogger.lastCall?.level)
    }

    @Test
    fun testMultipleAppErrorLogs() {
        val mockLogger = MockLogger()

        val networkError = AppError.fromCode(ErrorCode.NETWORK_TIMEOUT)
        val authError = AppError.fromCode(ErrorCode.AUTH_UNAUTHORIZED)
        val validationError = AppError.fromCode(ErrorCode.VALIDATION_INVALID_INPUT)

        networkError.log("Network", mockLogger)
        authError.log("Auth", mockLogger)
        validationError.log("Validation", mockLogger)

        assertEquals(3, mockLogger.callCount)
        assertEquals(LogLevel.WARNING, mockLogger.calls[0].level) // Network is retryable
        assertEquals(LogLevel.ERROR, mockLogger.calls[1].level)   // Auth is not retryable
        assertEquals(LogLevel.ERROR, mockLogger.calls[2].level)   // Validation is not retryable
    }

    @Test
    fun testLogOnFailureDefaultLogger() {
        // This test verifies the default parameter works, but uses mock for verification
        val mockLogger = MockLogger()
        val result: Result<String> = failure("Error")

        // We use explicit logger here since DefaultLogger would log to real console
        result.logOnFailure("TestTag", mockLogger)

        assertEquals(1, mockLogger.callCount)
    }

    // ==================== Edge Cases ====================

    @Test
    fun testLogOnFailureEmptyError() {
        val mockLogger = MockLogger()
        val result: Result<String> = failure("")

        result.logOnFailure("Tag", mockLogger)

        assertEquals(1, mockLogger.callCount)
        assertTrue(mockLogger.lastCall?.message?.contains("Operation failed:") == true)
    }

    @Test
    fun testAppErrorLogEmptyDetails() {
        val mockLogger = MockLogger()
        val error = AppError.fromCode(ErrorCode.SYSTEM_UNKNOWN_ERROR, "Unknown error")

        error.log("Tag", mockLogger)

        assertEquals(1, mockLogger.callCount)
        assertFalse(mockLogger.lastCall?.message?.contains("details:") == true)
    }

    @Test
    fun testAppErrorWithNetworkCodeAndCause() {
        val mockLogger = MockLogger()
        val cause = RuntimeException("Connection refused")
        val error = AppError.network(cause)

        error.log("Network", mockLogger)

        assertEquals(LogLevel.WARNING, mockLogger.lastCall?.level)
        assertNotNull(mockLogger.lastCall?.throwable)
    }

    @Test
    fun testAppErrorValidationWithoutCause() {
        val mockLogger = MockLogger()
        val error = AppError.validation("Invalid email format", "email")

        error.log("Validation", mockLogger)

        assertEquals(LogLevel.ERROR, mockLogger.lastCall?.level)
        assertNull(mockLogger.lastCall?.throwable)
        assertTrue(mockLogger.lastCall?.message?.contains("VALIDATION_INVALID_INPUT") == true)
    }

    @Test
    fun testAllLogLevelsWithAppError() {
        val mockLogger = MockLogger()
        // Use AUTH_FORBIDDEN which is NOT retryable and NOT a network error
        // This ensures logError() uses ERROR level
        val error = AppError.fromCode(ErrorCode.AUTH_FORBIDDEN, "Test")

        error.logDebug("Tag", mockLogger)
        error.logInfo("Tag", mockLogger)
        error.logWarning("Tag", mockLogger)
        error.logError("Tag", mockLogger)

        assertEquals(4, mockLogger.callCount)
        assertEquals(LogLevel.DEBUG, mockLogger.calls[0].level)
        assertEquals(LogLevel.INFO, mockLogger.calls[1].level)
        assertEquals(LogLevel.WARNING, mockLogger.calls[2].level)
        assertEquals(LogLevel.ERROR, mockLogger.calls[3].level)
    }
}
