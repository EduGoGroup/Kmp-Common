package com.edugo.test.module.platform

/**
 * Represents a single log call captured by [MockLogger].
 *
 * @property level The log level (DEBUG, INFO, WARNING, ERROR)
 * @property tag The tag used for the log call
 * @property message The message that was logged
 * @property throwable The throwable if one was passed, null otherwise
 */
data class LogCall(
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null
) {
    /**
     * Returns true if this log call included a throwable.
     */
    fun hasThrowable(): Boolean = throwable != null
}

/**
 * A mock implementation of [Logger] for testing purposes.
 *
 * Captures all log calls for verification in tests. Supports:
 * - Recording all log calls with level, tag, message, and throwable
 * - Filtering calls by level or tag
 * - Verifying specific calls were made
 * - Clearing captured calls between tests
 *
 * ## Usage:
 * ```kotlin
 * val mockLogger = MockLogger()
 *
 * // Use the logger
 * mockLogger.d("MyTag", "Debug message")
 * mockLogger.e("MyTag", "Error message", RuntimeException("test"))
 *
 * // Verify calls
 * assertEquals(2, mockLogger.callCount)
 * assertEquals("Debug message", mockLogger.calls[0].message)
 * assertTrue(mockLogger.hasCall(LogLevel.ERROR, "MyTag"))
 * ```
 *
 * @see LogCall
 * @see Logger
 */
class MockLogger : Logger {

    /**
     * All captured log calls in order.
     */
    private val _calls: MutableList<LogCall> = mutableListOf()

    /**
     * Read-only list of all captured calls.
     */
    val calls: List<LogCall> get() = _calls.toList()

    /**
     * Total number of captured calls.
     */
    val callCount: Int get() = _calls.size

    /**
     * Returns true if no calls have been captured.
     */
    val isEmpty: Boolean get() = _calls.isEmpty()

    /**
     * The last captured call, or null if no calls.
     */
    val lastCall: LogCall? get() = _calls.lastOrNull()

    // ==================== DEBUG ====================

    override fun d(tag: String, message: String) {
        _calls.add(LogCall(LogLevel.DEBUG, tag, message))
    }

    override fun d(tag: String, message: String, throwable: Throwable) {
        _calls.add(LogCall(LogLevel.DEBUG, tag, message, throwable))
    }

    // ==================== INFO ====================

    override fun i(tag: String, message: String) {
        _calls.add(LogCall(LogLevel.INFO, tag, message))
    }

    override fun i(tag: String, message: String, throwable: Throwable) {
        _calls.add(LogCall(LogLevel.INFO, tag, message, throwable))
    }

    // ==================== WARNING ====================

    override fun w(tag: String, message: String) {
        _calls.add(LogCall(LogLevel.WARNING, tag, message))
    }

    override fun w(tag: String, message: String, throwable: Throwable) {
        _calls.add(LogCall(LogLevel.WARNING, tag, message, throwable))
    }

    // ==================== ERROR ====================

    override fun e(tag: String, message: String) {
        _calls.add(LogCall(LogLevel.ERROR, tag, message))
    }

    override fun e(tag: String, message: String, throwable: Throwable) {
        _calls.add(LogCall(LogLevel.ERROR, tag, message, throwable))
    }

    // ==================== Query Methods ====================

    /**
     * Returns all calls at the specified log level.
     */
    fun callsAt(level: LogLevel): List<LogCall> = _calls.filter { it.level == level }

    /**
     * Returns all calls with the specified tag.
     */
    fun callsWithTag(tag: String): List<LogCall> = _calls.filter { it.tag == tag }

    /**
     * Returns all calls at the specified level with the specified tag.
     */
    fun callsAt(level: LogLevel, tag: String): List<LogCall> =
        _calls.filter { it.level == level && it.tag == tag }

    /**
     * Returns true if there's at least one call at the specified level.
     */
    fun hasCallAt(level: LogLevel): Boolean = _calls.any { it.level == level }

    /**
     * Returns true if there's at least one call with the specified tag.
     */
    fun hasCallWithTag(tag: String): Boolean = _calls.any { it.tag == tag }

    /**
     * Returns true if there's at least one call matching both level and tag.
     */
    fun hasCall(level: LogLevel, tag: String): Boolean =
        _calls.any { it.level == level && it.tag == tag }

    /**
     * Returns true if there's a call with the exact message.
     */
    fun hasMessage(message: String): Boolean = _calls.any { it.message == message }

    /**
     * Returns true if there's a call containing the message substring.
     */
    fun hasMessageContaining(substring: String): Boolean =
        _calls.any { it.message.contains(substring) }

    /**
     * Returns all calls that include a throwable.
     */
    fun callsWithThrowable(): List<LogCall> = _calls.filter { it.hasThrowable() }

    /**
     * Returns the count of calls at the specified level.
     */
    fun countAt(level: LogLevel): Int = _calls.count { it.level == level }

    /**
     * Returns debug calls count.
     */
    val debugCount: Int get() = countAt(LogLevel.DEBUG)

    /**
     * Returns info calls count.
     */
    val infoCount: Int get() = countAt(LogLevel.INFO)

    /**
     * Returns warning calls count.
     */
    val warningCount: Int get() = countAt(LogLevel.WARNING)

    /**
     * Returns error calls count.
     */
    val errorCount: Int get() = countAt(LogLevel.ERROR)

    // ==================== Utility ====================

    /**
     * Clears all captured calls.
     * Call this in test setup to ensure clean state.
     */
    fun clear() {
        _calls.clear()
    }

    /**
     * Returns a formatted string of all calls for debugging.
     */
    fun dump(): String = buildString {
        appendLine("MockLogger calls (${_calls.size} total):")
        _calls.forEachIndexed { index, call ->
            appendLine("  [$index] ${call.level} | ${call.tag}: ${call.message}")
            call.throwable?.let {
                appendLine("        Throwable: ${it::class.simpleName}: ${it.message}")
            }
        }
    }
}

/**
 * A mock logger that does NOT evaluate lazy messages.
 *
 * Used to test that lazy evaluation works correctly - the lambda
 * should NOT be invoked when using this logger.
 *
 * ## Usage:
 * ```kotlin
 * val noEvalLogger = NoEvalMockLogger()
 * var evaluated = false
 *
 * noEvalLogger.d("Tag") {
 *     evaluated = true  // This should NOT execute
 *     "message"
 * }
 *
 * assertFalse(evaluated, "Lambda should not be evaluated")
 * ```
 */
class NoEvalMockLogger : Logger {

    /**
     * Count of calls to each method (for verification).
     */
    private val _methodCalls: MutableMap<String, Int> = mutableMapOf()

    val methodCalls: Map<String, Int> get() = _methodCalls.toMap()

    private fun recordCall(method: String) {
        _methodCalls[method] = (_methodCalls[method] ?: 0) + 1
    }

    // Override the lazy methods to NOT call the lambda
    override fun d(tag: String, message: () -> String) {
        recordCall("d_lazy")
        // Intentionally NOT calling message() to test lazy evaluation
    }

    override fun i(tag: String, message: () -> String) {
        recordCall("i_lazy")
        // Intentionally NOT calling message() to test lazy evaluation
    }

    override fun w(tag: String, message: () -> String) {
        recordCall("w_lazy")
        // Intentionally NOT calling message() to test lazy evaluation
    }

    override fun e(tag: String, message: () -> String) {
        recordCall("e_lazy")
        // Intentionally NOT calling message() to test lazy evaluation
    }

    // Required implementations for non-lazy methods
    override fun d(tag: String, message: String) {
        recordCall("d")
    }

    override fun d(tag: String, message: String, throwable: Throwable) {
        recordCall("d_throwable")
    }

    override fun i(tag: String, message: String) {
        recordCall("i")
    }

    override fun i(tag: String, message: String, throwable: Throwable) {
        recordCall("i_throwable")
    }

    override fun w(tag: String, message: String) {
        recordCall("w")
    }

    override fun w(tag: String, message: String, throwable: Throwable) {
        recordCall("w_throwable")
    }

    override fun e(tag: String, message: String) {
        recordCall("e")
    }

    override fun e(tag: String, message: String, throwable: Throwable) {
        recordCall("e_throwable")
    }

    fun clear() {
        _methodCalls.clear()
    }

    fun getCallCount(method: String): Int = _methodCalls[method] ?: 0
}
