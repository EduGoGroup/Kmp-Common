package com.edugo.test.module.platform

import kotlin.reflect.KClass

/**
 * A wrapper around the Logger interface that provides hierarchical tag support.
 *
 * Hierarchical tags use dot notation (e.g., "EduGo.Auth.Login") to organize
 * logging output and enable pattern-based filtering.
 *
 * ## Features:
 * - **Hierarchical tags**: Support for nested tags with "." separator
 * - **Thread-safe**: Can be safely used from multiple threads
 * - **Immutable**: Tags cannot be modified after creation
 * - **Cached**: Loggers are cached by tag for performance
 * - **Interface-based**: Uses [Logger] interface for flexibility and testability
 *
 * ## Usage:
 * ```kotlin
 * // Create a tagged logger
 * val logger = DefaultLogger.withTag("EduGo.Auth")
 *
 * // Log with the tag prefix using new short methods
 * logger.d("User login initiated")
 * // Output: [DEBUG] EduGo.Auth: User login initiated
 *
 * // Create child logger
 * val childLogger = logger.withChild("Login")
 * childLogger.i("Login successful")
 * // Output: [INFO] EduGo.Auth.Login: Login successful
 *
 * // Warning logs (new!)
 * logger.w("Session will expire soon")
 * ```
 *
 * @property tag The hierarchical tag for this logger (e.g., "EduGo.Auth.Login")
 * @property logger The underlying Logger implementation (defaults to DefaultLogger)
 * @see Logger
 * @see LoggerConfig
 */
public class TaggedLogger internal constructor(
    val tag: String,
    @PublishedApi internal val logger: Logger = DefaultLogger
) {
    init {
        require(tag.isNotBlank()) { "Tag cannot be blank" }
        require(!tag.startsWith(".")) { "Tag cannot start with '.'" }
        require(!tag.endsWith(".")) { "Tag cannot end with '.'" }
        require(!tag.contains("..")) { "Tag cannot contain consecutive dots '..'" }
    }

    // ==================== DEBUG (d) ====================

    /**
     * Logs a debug message with the configured tag.
     *
     * @param message The message to log
     */
    public fun d(message: String) {
        if (LoggerConfig.isEnabled(tag, LogLevel.DEBUG)) {
            logger.d(tag, message)
        }
    }

    /**
     * Logs a debug message with lazy evaluation.
     *
     * @param message Lambda that produces the message
     */
    public inline fun d(message: () -> String) {
        if (LoggerConfig.isEnabled(tag, LogLevel.DEBUG)) {
            logger.d(tag, message())
        }
    }

    /**
     * Logs a debug message with throwable.
     *
     * @param message The message to log
     * @param throwable The exception to log
     */
    public fun d(message: String, throwable: Throwable) {
        if (LoggerConfig.isEnabled(tag, LogLevel.DEBUG)) {
            logger.d(tag, message, throwable)
        }
    }

    // ==================== INFO (i) ====================

    /**
     * Logs an informational message with the configured tag.
     *
     * @param message The message to log
     */
    public fun i(message: String) {
        if (LoggerConfig.isEnabled(tag, LogLevel.INFO)) {
            logger.i(tag, message)
        }
    }

    /**
     * Logs an informational message with lazy evaluation.
     *
     * @param message Lambda that produces the message
     */
    public inline fun i(message: () -> String) {
        if (LoggerConfig.isEnabled(tag, LogLevel.INFO)) {
            logger.i(tag, message())
        }
    }

    /**
     * Logs an informational message with throwable.
     *
     * @param message The message to log
     * @param throwable The exception to log
     */
    public fun i(message: String, throwable: Throwable) {
        if (LoggerConfig.isEnabled(tag, LogLevel.INFO)) {
            logger.i(tag, message, throwable)
        }
    }

    // ==================== WARNING (w) ====================

    /**
     * Logs a warning message with the configured tag.
     *
     * @param message The message to log
     */
    public fun w(message: String) {
        if (LoggerConfig.isEnabled(tag, LogLevel.WARNING)) {
            logger.w(tag, message)
        }
    }

    /**
     * Logs a warning message with lazy evaluation.
     *
     * @param message Lambda that produces the message
     */
    public inline fun w(message: () -> String) {
        if (LoggerConfig.isEnabled(tag, LogLevel.WARNING)) {
            logger.w(tag, message())
        }
    }

    /**
     * Logs a warning message with throwable.
     *
     * @param message The message to log
     * @param throwable The exception to log
     */
    public fun w(message: String, throwable: Throwable) {
        if (LoggerConfig.isEnabled(tag, LogLevel.WARNING)) {
            logger.w(tag, message, throwable)
        }
    }

    // ==================== ERROR (e) ====================

    /**
     * Logs an error message with the configured tag.
     *
     * @param message The error message to log
     */
    public fun e(message: String) {
        if (LoggerConfig.isEnabled(tag, LogLevel.ERROR)) {
            logger.e(tag, message)
        }
    }

    /**
     * Logs an error message with lazy evaluation.
     *
     * @param message Lambda that produces the message
     */
    public inline fun e(message: () -> String) {
        if (LoggerConfig.isEnabled(tag, LogLevel.ERROR)) {
            logger.e(tag, message())
        }
    }

    /**
     * Logs an error message with throwable.
     *
     * @param message The error message to log
     * @param throwable The exception to log with stack trace
     */
    public fun e(message: String, throwable: Throwable) {
        if (LoggerConfig.isEnabled(tag, LogLevel.ERROR)) {
            logger.e(tag, message, throwable)
        }
    }

    // ==================== LEGACY COMPATIBILITY ====================

    /**
     * Logs a debug message (legacy compatibility).
     *
     * @deprecated Use [d] instead. Will be removed in v3.0.
     */
    @Deprecated("Use d() instead", ReplaceWith("d(message)"))
    public fun debug(message: String) = d(message)

    /**
     * Logs an informational message (legacy compatibility).
     *
     * @deprecated Use [i] instead. Will be removed in v3.0.
     */
    @Deprecated("Use i() instead", ReplaceWith("i(message)"))
    public fun info(message: String) = i(message)

    /**
     * Logs an error message with optional throwable (legacy compatibility).
     *
     * @deprecated Use [e] instead. Will be removed in v3.0.
     */
    @Deprecated("Use e() instead", ReplaceWith("e(message)"))
    public fun error(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            e(message, throwable)
        } else {
            e(message)
        }
    }

    // ==================== NAVIGATION ====================

    /**
     * Creates a child logger with a sub-tag appended to the current tag.
     *
     * @param childTag The child tag to append (e.g., "Login")
     * @return A new TaggedLogger with tag "parent.child"
     *
     * Example:
     * ```kotlin
     * val parent = DefaultLogger.withTag("EduGo.Auth")
     * val child = parent.withChild("Login")
     * // child.tag == "EduGo.Auth.Login"
     * ```
     */
    public fun withChild(childTag: String): TaggedLogger {
        require(childTag.isNotBlank()) { "Child tag cannot be blank" }
        require(!childTag.contains(".")) { "Child tag cannot contain '.', use withTag() for hierarchical tags" }
        return LoggerCache.getOrCreate("$tag.$childTag", logger)
    }

    /**
     * Creates a logger with a new tag, replacing the current tag entirely.
     *
     * @param newTag The new tag to use (can be hierarchical)
     * @return A new TaggedLogger with the specified tag
     */
    public fun withTag(newTag: String): TaggedLogger {
        return LoggerCache.getOrCreate(newTag, logger)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TaggedLogger) return false
        return tag == other.tag
    }

    override fun hashCode(): Int = tag.hashCode()

    override fun toString(): String = "TaggedLogger(tag='$tag')"

    companion object {
        /**
         * Creates a TaggedLogger from a KClass, using the fully qualified name as the tag.
         *
         * @param clazz The class to derive the tag from
         * @param logger Optional Logger instance (defaults to DefaultLogger)
         * @return A TaggedLogger with tag based on the class name
         *
         * Example:
         * ```kotlin
         * class UserRepository { /* ... */ }
         *
         * val logger = TaggedLogger.fromClass(UserRepository::class)
         * // logger.tag == "com.edugo.UserRepository"
         * ```
         */
        public fun fromClass(clazz: KClass<*>, logger: Logger = DefaultLogger): TaggedLogger {
            // Use simpleName for JS compatibility (qualifiedName not supported in JS)
            val className = clazz.simpleName ?: "Unknown"
            return LoggerCache.getOrCreate(className, logger)
        }

        /**
         * Creates a TaggedLogger with the specified tag.
         *
         * @param tag The hierarchical tag to use (e.g., "EduGo.Auth.Login")
         * @param logger Optional Logger instance (defaults to DefaultLogger)
         * @return A cached or new TaggedLogger instance
         */
        public fun create(tag: String, logger: Logger = DefaultLogger): TaggedLogger {
            return LoggerCache.getOrCreate(tag, logger)
        }
    }
}

/**
 * Log levels for filtering and controlling log output verbosity.
 *
 * Levels are ordered by severity: DEBUG < INFO < WARNING < ERROR.
 * When a minimum level is set, all logs at that level or higher are shown.
 *
 * ## Usage:
 * ```kotlin
 * // Set minimum level to INFO (hides DEBUG logs)
 * LoggerConfig.setLevel("EduGo.Auth.*", LogLevel.INFO)
 *
 * // Check if level is enabled before expensive operations
 * if (LoggerConfig.isEnabled(tag, LogLevel.DEBUG)) {
 *     logger.d(expensiveDebugInfo())
 * }
 * ```
 *
 * @see LoggerConfig
 * @see TaggedLogger
 */
public enum class LogLevel {
    /**
     * Detailed diagnostic information for debugging.
     * Use for verbose output that helps troubleshoot issues.
     * Should be disabled in production.
     */
    DEBUG,

    /**
     * General informational messages about application flow.
     * Use for significant events like user actions, state changes.
     * Safe for production.
     */
    INFO,

    /**
     * Warning conditions that may indicate potential issues.
     * Use for recoverable problems, deprecated API usage, or
     * situations that may lead to errors if not addressed.
     * Important to monitor in production.
     */
    WARNING,

    /**
     * Error conditions that need attention.
     * Use for exceptions, failures, and abnormal conditions.
     * Always logged in production.
     */
    ERROR
}
