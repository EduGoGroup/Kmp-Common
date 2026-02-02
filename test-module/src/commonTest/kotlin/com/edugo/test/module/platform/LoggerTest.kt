package com.edugo.test.module.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive tests for the [Logger] interface.
 *
 * Tests cover:
 * - Basic logging methods (d, i, w, e)
 * - Lazy evaluation with lambdas
 * - Throwable handling
 * - Legacy compatibility methods
 *
 * @see Logger
 * @see MockLogger
 */
class LoggerTest {

    // ==================== Basic Method Tests ====================

    @Test
    fun testDebugLogsCorrectly() {
        val mockLogger = MockLogger()

        mockLogger.d("TestTag", "Debug message")

        assertEquals(1, mockLogger.callCount)
        assertEquals(LogLevel.DEBUG, mockLogger.lastCall?.level)
        assertEquals("TestTag", mockLogger.lastCall?.tag)
        assertEquals("Debug message", mockLogger.lastCall?.message)
        assertNull(mockLogger.lastCall?.throwable)
    }

    @Test
    fun testInfoLogsCorrectly() {
        val mockLogger = MockLogger()

        mockLogger.i("TestTag", "Info message")

        assertEquals(1, mockLogger.callCount)
        assertEquals(LogLevel.INFO, mockLogger.lastCall?.level)
        assertEquals("TestTag", mockLogger.lastCall?.tag)
        assertEquals("Info message", mockLogger.lastCall?.message)
        assertNull(mockLogger.lastCall?.throwable)
    }

    @Test
    fun testWarningLogsCorrectly() {
        val mockLogger = MockLogger()

        mockLogger.w("TestTag", "Warning message")

        assertEquals(1, mockLogger.callCount)
        assertEquals(LogLevel.WARNING, mockLogger.lastCall?.level)
        assertEquals("TestTag", mockLogger.lastCall?.tag)
        assertEquals("Warning message", mockLogger.lastCall?.message)
        assertNull(mockLogger.lastCall?.throwable)
    }

    @Test
    fun testErrorLogsCorrectly() {
        val mockLogger = MockLogger()

        mockLogger.e("TestTag", "Error message")

        assertEquals(1, mockLogger.callCount)
        assertEquals(LogLevel.ERROR, mockLogger.lastCall?.level)
        assertEquals("TestTag", mockLogger.lastCall?.tag)
        assertEquals("Error message", mockLogger.lastCall?.message)
        assertNull(mockLogger.lastCall?.throwable)
    }

    // ==================== Throwable Tests ====================

    @Test
    fun testDebugWithThrowable() {
        val mockLogger = MockLogger()
        val exception = RuntimeException("Test exception")

        mockLogger.d("TestTag", "Debug with error", exception)

        assertEquals(1, mockLogger.callCount)
        assertEquals(LogLevel.DEBUG, mockLogger.lastCall?.level)
        assertEquals("Debug with error", mockLogger.lastCall?.message)
        assertNotNull(mockLogger.lastCall?.throwable)
        assertEquals(exception, mockLogger.lastCall?.throwable)
        assertEquals("Test exception", mockLogger.lastCall?.throwable?.message)
    }

    @Test
    fun testInfoWithThrowable() {
        val mockLogger = MockLogger()
        val exception = IllegalArgumentException("Invalid argument")

        mockLogger.i("TestTag", "Info with error", exception)

        assertEquals(1, mockLogger.callCount)
        assertEquals(LogLevel.INFO, mockLogger.lastCall?.level)
        assertNotNull(mockLogger.lastCall?.throwable)
        assertEquals(exception, mockLogger.lastCall?.throwable)
    }

    @Test
    fun testWarningWithThrowable() {
        val mockLogger = MockLogger()
        val exception = IllegalStateException("Bad state")

        mockLogger.w("TestTag", "Warning with error", exception)

        assertEquals(1, mockLogger.callCount)
        assertEquals(LogLevel.WARNING, mockLogger.lastCall?.level)
        assertNotNull(mockLogger.lastCall?.throwable)
        assertEquals(exception, mockLogger.lastCall?.throwable)
    }

    @Test
    fun testErrorWithThrowable() {
        val mockLogger = MockLogger()
        val exception = NullPointerException("Null value")

        mockLogger.e("TestTag", "Error with exception", exception)

        assertEquals(1, mockLogger.callCount)
        assertEquals(LogLevel.ERROR, mockLogger.lastCall?.level)
        assertNotNull(mockLogger.lastCall?.throwable)
        assertEquals(exception, mockLogger.lastCall?.throwable)
        assertEquals("Null value", mockLogger.lastCall?.throwable?.message)
    }

    // ==================== Lazy Evaluation Tests ====================

    @Test
    fun testDebugLazyEvaluatesMessage() {
        val mockLogger = MockLogger()
        var evaluated = false

        mockLogger.d("TestTag") {
            evaluated = true
            "Lazy debug message"
        }

        assertTrue(evaluated, "Lambda should be evaluated by default implementation")
        assertEquals(1, mockLogger.callCount)
        assertEquals("Lazy debug message", mockLogger.lastCall?.message)
    }

    @Test
    fun testInfoLazyEvaluatesMessage() {
        val mockLogger = MockLogger()
        var evaluated = false

        mockLogger.i("TestTag") {
            evaluated = true
            "Lazy info message"
        }

        assertTrue(evaluated, "Lambda should be evaluated")
        assertEquals("Lazy info message", mockLogger.lastCall?.message)
    }

    @Test
    fun testWarningLazyEvaluatesMessage() {
        val mockLogger = MockLogger()
        var evaluated = false

        mockLogger.w("TestTag") {
            evaluated = true
            "Lazy warning message"
        }

        assertTrue(evaluated, "Lambda should be evaluated")
        assertEquals("Lazy warning message", mockLogger.lastCall?.message)
    }

    @Test
    fun testErrorLazyEvaluatesMessage() {
        val mockLogger = MockLogger()
        var evaluated = false

        mockLogger.e("TestTag") {
            evaluated = true
            "Lazy error message"
        }

        assertTrue(evaluated, "Lambda should be evaluated")
        assertEquals("Lazy error message", mockLogger.lastCall?.message)
    }

    @Test
    fun testLazyNotEvaluatedWhenLoggerSkips() {
        val noEvalLogger = NoEvalMockLogger()
        var evaluated = false

        noEvalLogger.d("TestTag") {
            evaluated = true
            "Should not be evaluated"
        }

        assertFalse(evaluated, "Lambda should NOT be evaluated when logger doesn't invoke it")
        assertEquals(1, noEvalLogger.getCallCount("d_lazy"))
    }

    @Test
    fun testAllLazyMethodsNotEvaluatedWhenSkipped() {
        val noEvalLogger = NoEvalMockLogger()
        var dEval = false
        var iEval = false
        var wEval = false
        var eEval = false

        noEvalLogger.d("Tag") { dEval = true; "d" }
        noEvalLogger.i("Tag") { iEval = true; "i" }
        noEvalLogger.w("Tag") { wEval = true; "w" }
        noEvalLogger.e("Tag") { eEval = true; "e" }

        assertFalse(dEval, "Debug lambda should not be evaluated")
        assertFalse(iEval, "Info lambda should not be evaluated")
        assertFalse(wEval, "Warning lambda should not be evaluated")
        assertFalse(eEval, "Error lambda should not be evaluated")

        assertEquals(1, noEvalLogger.getCallCount("d_lazy"))
        assertEquals(1, noEvalLogger.getCallCount("i_lazy"))
        assertEquals(1, noEvalLogger.getCallCount("w_lazy"))
        assertEquals(1, noEvalLogger.getCallCount("e_lazy"))
    }

    // ==================== Legacy Compatibility Tests ====================

    @Test
    @Suppress("DEPRECATION")
    fun testLegacyDebugDelegatesToD() {
        val mockLogger = MockLogger()

        mockLogger.debug("TestTag", "Legacy debug")

        assertEquals(1, mockLogger.callCount)
        assertEquals(LogLevel.DEBUG, mockLogger.lastCall?.level)
        assertEquals("Legacy debug", mockLogger.lastCall?.message)
    }

    @Test
    @Suppress("DEPRECATION")
    fun testLegacyInfoDelegatesToI() {
        val mockLogger = MockLogger()

        mockLogger.info("TestTag", "Legacy info")

        assertEquals(1, mockLogger.callCount)
        assertEquals(LogLevel.INFO, mockLogger.lastCall?.level)
        assertEquals("Legacy info", mockLogger.lastCall?.message)
    }

    @Test
    @Suppress("DEPRECATION")
    fun testLegacyErrorDelegatesToE() {
        val mockLogger = MockLogger()

        mockLogger.error("TestTag", "Legacy error")

        assertEquals(1, mockLogger.callCount)
        assertEquals(LogLevel.ERROR, mockLogger.lastCall?.level)
        assertEquals("Legacy error", mockLogger.lastCall?.message)
    }

    @Test
    @Suppress("DEPRECATION")
    fun testLegacyErrorWithThrowable() {
        val mockLogger = MockLogger()
        val exception = RuntimeException("Legacy exception")

        mockLogger.error("TestTag", "Legacy error with throwable", exception)

        assertEquals(1, mockLogger.callCount)
        assertEquals(LogLevel.ERROR, mockLogger.lastCall?.level)
        assertNotNull(mockLogger.lastCall?.throwable)
        assertEquals(exception, mockLogger.lastCall?.throwable)
    }

    // ==================== Multiple Calls Tests ====================

    @Test
    fun testMultipleCallsAreRecorded() {
        val mockLogger = MockLogger()

        mockLogger.d("Tag1", "Debug 1")
        mockLogger.i("Tag2", "Info 1")
        mockLogger.w("Tag3", "Warning 1")
        mockLogger.e("Tag4", "Error 1")

        assertEquals(4, mockLogger.callCount)
        assertEquals(1, mockLogger.debugCount)
        assertEquals(1, mockLogger.infoCount)
        assertEquals(1, mockLogger.warningCount)
        assertEquals(1, mockLogger.errorCount)
    }

    @Test
    fun testCallsFilteredByLevel() {
        val mockLogger = MockLogger()

        mockLogger.d("Tag", "Debug")
        mockLogger.d("Tag", "Debug 2")
        mockLogger.e("Tag", "Error")

        val debugCalls = mockLogger.callsAt(LogLevel.DEBUG)
        assertEquals(2, debugCalls.size)
        assertEquals("Debug", debugCalls[0].message)
        assertEquals("Debug 2", debugCalls[1].message)
    }

    @Test
    fun testCallsFilteredByTag() {
        val mockLogger = MockLogger()

        mockLogger.d("TagA", "Message A")
        mockLogger.d("TagB", "Message B")
        mockLogger.d("TagA", "Message A2")

        val tagACalls = mockLogger.callsWithTag("TagA")
        assertEquals(2, tagACalls.size)
    }

    @Test
    fun testCallsFilteredByLevelAndTag() {
        val mockLogger = MockLogger()

        mockLogger.d("TagA", "Debug A")
        mockLogger.e("TagA", "Error A")
        mockLogger.d("TagB", "Debug B")

        val debugTagA = mockLogger.callsAt(LogLevel.DEBUG, "TagA")
        assertEquals(1, debugTagA.size)
        assertEquals("Debug A", debugTagA[0].message)
    }

    @Test
    fun testHasMessageContaining() {
        val mockLogger = MockLogger()

        mockLogger.d("Tag", "User john@example.com logged in")

        assertTrue(mockLogger.hasMessageContaining("john@example.com"))
        assertTrue(mockLogger.hasMessageContaining("logged in"))
        assertFalse(mockLogger.hasMessageContaining("logged out"))
    }

    @Test
    fun testCallsWithThrowable() {
        val mockLogger = MockLogger()
        val ex1 = RuntimeException("ex1")
        val ex2 = RuntimeException("ex2")

        mockLogger.d("Tag", "No throwable")
        mockLogger.e("Tag", "With throwable 1", ex1)
        mockLogger.w("Tag", "With throwable 2", ex2)

        val throwableCalls = mockLogger.callsWithThrowable()
        assertEquals(2, throwableCalls.size)
    }

    @Test
    fun testClearResetsLogger() {
        val mockLogger = MockLogger()

        mockLogger.d("Tag", "Message 1")
        mockLogger.d("Tag", "Message 2")
        assertEquals(2, mockLogger.callCount)

        mockLogger.clear()

        assertEquals(0, mockLogger.callCount)
        assertTrue(mockLogger.isEmpty)
    }

    // ==================== Edge Cases ====================

    @Test
    fun testEmptyTag() {
        val mockLogger = MockLogger()

        mockLogger.d("", "Message with empty tag")

        assertEquals("", mockLogger.lastCall?.tag)
    }

    @Test
    fun testEmptyMessage() {
        val mockLogger = MockLogger()

        mockLogger.d("Tag", "")

        assertEquals("", mockLogger.lastCall?.message)
    }

    @Test
    fun testVeryLongMessage() {
        val mockLogger = MockLogger()
        val longMessage = "A".repeat(10000)

        mockLogger.d("Tag", longMessage)

        assertEquals(longMessage, mockLogger.lastCall?.message)
        assertEquals(10000, mockLogger.lastCall?.message?.length)
    }

    @Test
    fun testSpecialCharactersInMessage() {
        val mockLogger = MockLogger()
        val specialMessage = "Line1\nLine2\tTabbed\r\nNewline with special: @#\$%^&*()"

        mockLogger.d("Tag", specialMessage)

        assertEquals(specialMessage, mockLogger.lastCall?.message)
    }

    @Test
    fun testUnicodeInMessage() {
        val mockLogger = MockLogger()
        val unicodeMessage = "Hello from Kotlin"

        mockLogger.i("Tag", unicodeMessage)

        assertEquals(unicodeMessage, mockLogger.lastCall?.message)
    }

    @Test
    fun testNestedExceptions() {
        val mockLogger = MockLogger()
        val rootCause = IllegalStateException("Root cause")
        val wrapper = RuntimeException("Wrapper", rootCause)

        mockLogger.e("Tag", "Nested exception", wrapper)

        val logged = mockLogger.lastCall?.throwable
        assertNotNull(logged)
        assertEquals("Wrapper", logged.message)
        assertNotNull(logged.cause)
        assertEquals("Root cause", logged.cause?.message)
    }

    // ==================== LogCall Data Class Tests ====================

    @Test
    fun testLogCallHasThrowable() {
        val callWithThrowable = LogCall(LogLevel.ERROR, "Tag", "Message", RuntimeException())
        val callWithoutThrowable = LogCall(LogLevel.DEBUG, "Tag", "Message")

        assertTrue(callWithThrowable.hasThrowable())
        assertFalse(callWithoutThrowable.hasThrowable())
    }

    @Test
    fun testLogCallEquality() {
        val call1 = LogCall(LogLevel.DEBUG, "Tag", "Message")
        val call2 = LogCall(LogLevel.DEBUG, "Tag", "Message")
        val call3 = LogCall(LogLevel.INFO, "Tag", "Message")

        assertEquals(call1, call2)
        assertFalse(call1 == call3)
    }
}
