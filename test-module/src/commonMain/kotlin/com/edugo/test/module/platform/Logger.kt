package com.edugo.test.module.platform

/**
 * Platform-agnostic logging interface for multiplatform applications.
 *
 * Provides a unified logging API that delegates to platform-specific
 * logging mechanisms while maintaining consistent behavior across targets.
 *
 * ## BREAKING CHANGE (v2.0)
 * This interface replaces the previous `expect object Logger`. Migration guide:
 * ```kotlin
 * // OLD (v1.x):
 * Logger.debug("Tag", "message")
 *
 * // NEW (v2.0):
 * val logger = createDefaultLogger()
 * logger.d("Tag", "message")
 * ```
 *
 * ## Platform-specific implementations:
 *
 * ### Android:
 * - Uses `android.util.Log`
 * - Logs appear in Logcat
 * - Tag is used for filtering
 *
 * ### JVM/Desktop:
 * - Uses `println` or SLF4J (if available)
 * - Logs to console or configured appender
 *
 * ### iOS:
 * - Uses `NSLog` or `os_log`
 * - Logs appear in Xcode console
 *
 * ### JS:
 * - Uses `console.log`, `console.info`, `console.warn`, `console.error`
 * - Logs appear in browser/Node.js console
 *
 * ## Tag naming conventions:
 * - Use class/module name: `"NetworkClient"`, `"UserRepository"`
 * - Keep it concise (max 23 chars on Android)
 * - Use PascalCase for consistency
 *
 * @see createDefaultLogger
 * @see TaggedLogger
 * @see [Android Logging Best Practices](https://developer.android.com/studio/debug/am-logcat)
 */
interface Logger {

    // ==================== DEBUG ====================

    /**
     * Logs a debug message.
     *
     * **Use for:**
     * - Development/debugging information
     * - Verbose diagnostics
     * - Flow tracing
     *
     * **Note**: Debug logs are typically stripped in release builds.
     *
     * @param tag Identifier for the log source (usually class name)
     * @param message The message to log
     *
     * Example:
     * ```kotlin
     * logger.d("NetworkClient", "Sending GET request to /api/users")
     * ```
     */
    fun d(tag: String, message: String)

    /**
     * Logs a debug message with lazy evaluation.
     *
     * The message lambda is only evaluated if debug logging is enabled,
     * avoiding string concatenation overhead in production.
     *
     * @param tag Identifier for the log source
     * @param message Lambda that produces the message (evaluated lazily)
     *
     * Example:
     * ```kotlin
     * logger.d("Cache") { "Stored ${items.size} items in ${duration}ms" }
     * ```
     */
    fun d(tag: String, message: () -> String) {
        d(tag, message())
    }

    /**
     * Logs a debug message with throwable.
     *
     * @param tag Identifier for the log source
     * @param message The message to log
     * @param throwable The exception to log
     */
    fun d(tag: String, message: String, throwable: Throwable)

    // ==================== INFO ====================

    /**
     * Logs an informational message.
     *
     * **Use for:**
     * - Important state changes
     * - Application lifecycle events
     * - Successful operations
     *
     * @param tag Identifier for the log source (usually class name)
     * @param message The message to log
     *
     * Example:
     * ```kotlin
     * logger.i("AuthManager", "User logged in successfully")
     * ```
     */
    fun i(tag: String, message: String)

    /**
     * Logs an informational message with lazy evaluation.
     *
     * @param tag Identifier for the log source
     * @param message Lambda that produces the message (evaluated lazily)
     */
    fun i(tag: String, message: () -> String) {
        i(tag, message())
    }

    /**
     * Logs an informational message with throwable.
     *
     * @param tag Identifier for the log source
     * @param message The message to log
     * @param throwable The exception to log
     */
    fun i(tag: String, message: String, throwable: Throwable)

    // ==================== WARNING ====================

    /**
     * Logs a warning message.
     *
     * **Use for:**
     * - Potentially harmful situations
     * - Deprecated API usage
     * - Recoverable issues
     *
     * @param tag Identifier for the log source (usually class name)
     * @param message The message to log
     *
     * Example:
     * ```kotlin
     * logger.w("ConfigLoader", "Config file not found, using defaults")
     * ```
     */
    fun w(tag: String, message: String)

    /**
     * Logs a warning message with lazy evaluation.
     *
     * @param tag Identifier for the log source
     * @param message Lambda that produces the message (evaluated lazily)
     */
    fun w(tag: String, message: () -> String) {
        w(tag, message())
    }

    /**
     * Logs a warning message with throwable.
     *
     * @param tag Identifier for the log source
     * @param message The message to log
     * @param throwable The exception to log
     */
    fun w(tag: String, message: String, throwable: Throwable)

    // ==================== ERROR ====================

    /**
     * Logs an error message.
     *
     * **Use for:**
     * - Unexpected errors
     * - Failed operations
     * - Critical issues
     *
     * @param tag Identifier for the log source (usually class name)
     * @param message The error message to log
     *
     * Example:
     * ```kotlin
     * logger.e("NetworkClient", "Failed to connect to server")
     * ```
     */
    fun e(tag: String, message: String)

    /**
     * Logs an error message with lazy evaluation.
     *
     * @param tag Identifier for the log source
     * @param message Lambda that produces the message (evaluated lazily)
     */
    fun e(tag: String, message: () -> String) {
        e(tag, message())
    }

    /**
     * Logs an error message with throwable.
     *
     * **Use for:**
     * - Exception handling
     * - Failures with stack traces
     *
     * @param tag Identifier for the log source
     * @param message The error message to log
     * @param throwable The exception to log with stack trace
     *
     * Example:
     * ```kotlin
     * try {
     *     riskyOperation()
     * } catch (e: Exception) {
     *     logger.e("UserRepository", "Failed to save user", e)
     * }
     * ```
     */
    fun e(tag: String, message: String, throwable: Throwable)

    // ==================== LEGACY COMPATIBILITY ====================

    /**
     * Logs a debug message (legacy compatibility).
     *
     * @deprecated Use [d] instead. Will be removed in v3.0.
     */
    @Deprecated("Use d() instead", ReplaceWith("d(tag, message)"))
    fun debug(tag: String, message: String) = d(tag, message)

    /**
     * Logs an informational message (legacy compatibility).
     *
     * @deprecated Use [i] instead. Will be removed in v3.0.
     */
    @Deprecated("Use i() instead", ReplaceWith("i(tag, message)"))
    fun info(tag: String, message: String) = i(tag, message)

    /**
     * Logs an error message with optional throwable (legacy compatibility).
     *
     * @deprecated Use [e] instead. Will be removed in v3.0.
     */
    @Deprecated("Use e() instead", ReplaceWith("e(tag, message)"))
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            e(tag, message, throwable)
        } else {
            e(tag, message)
        }
    }
}

/**
 * Creates the default platform-specific Logger instance.
 *
 * This is the recommended way to obtain a Logger instance. Each platform
 * provides its own implementation:
 * - Android: Uses android.util.Log
 * - JVM/Desktop: Uses println with formatting
 * - iOS: Uses os_log (future)
 * - JS: Uses console (future)
 *
 * ## Usage:
 * ```kotlin
 * // In common code
 * val logger = createDefaultLogger()
 * logger.d("MyClass", "Debug message")
 *
 * // Or use with TaggedLogger
 * val taggedLogger = createDefaultLogger().withTag("MyClass")
 * taggedLogger.d("Debug message")
 * ```
 *
 * @return Platform-specific Logger implementation
 * @see Logger
 * @see TaggedLogger
 */
expect fun createDefaultLogger(): Logger

/**
 * Global default logger instance for convenience.
 *
 * This is a lazily-initialized singleton that uses [createDefaultLogger].
 * Use this when you don't need dependency injection or custom logger instances.
 *
 * ## Usage:
 * ```kotlin
 * DefaultLogger.d("QuickLog", "Simple debug message")
 * ```
 *
 * For production code, prefer injecting Logger instances for better testability.
 */
object DefaultLogger : Logger {
    private val delegate: Logger by lazy { createDefaultLogger() }

    override fun d(tag: String, message: String) = delegate.d(tag, message)
    override fun d(tag: String, message: String, throwable: Throwable) = delegate.d(tag, message, throwable)

    override fun i(tag: String, message: String) = delegate.i(tag, message)
    override fun i(tag: String, message: String, throwable: Throwable) = delegate.i(tag, message, throwable)

    override fun w(tag: String, message: String) = delegate.w(tag, message)
    override fun w(tag: String, message: String, throwable: Throwable) = delegate.w(tag, message, throwable)

    override fun e(tag: String, message: String) = delegate.e(tag, message)
    override fun e(tag: String, message: String, throwable: Throwable) = delegate.e(tag, message, throwable)
}
