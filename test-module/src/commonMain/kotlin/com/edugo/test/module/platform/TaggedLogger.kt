package com.edugo.test.module.platform

import kotlin.reflect.KClass

/**
 * A wrapper around the Logger object that provides hierarchical tag support.
 *
 * Hierarchical tags use dot notation (e.g., "EduGo.Auth.Login") to organize
 * logging output and enable pattern-based filtering.
 *
 * ## Features:
 * - **Hierarchical tags**: Support for nested tags with "." separator
 * - **Thread-safe**: Can be safely used from multiple threads
 * - **Immutable**: Tags cannot be modified after creation
 * - **Cached**: Loggers are cached by tag for performance
 *
 * ## Usage:
 * ```kotlin
 * // Create a tagged logger
 * val logger = Logger.withTag("EduGo.Auth")
 *
 * // Log with the tag prefix
 * logger.debug("User login initiated")
 * // Output: [DEBUG] EduGo.Auth: User login initiated
 *
 * // Create child logger
 * val childLogger = logger.withChild("Login")
 * childLogger.info("Login successful")
 * // Output: [INFO] EduGo.Auth.Login: Login successful
 * ```
 *
 * @property tag The hierarchical tag for this logger (e.g., "EduGo.Auth.Login")
 * @see Logger
 * @see LoggerConfig
 */
class TaggedLogger internal constructor(
    val tag: String
) {
    init {
        require(tag.isNotBlank()) { "Tag cannot be blank" }
        require(!tag.startsWith(".")) { "Tag cannot start with '.'" }
        require(!tag.endsWith(".")) { "Tag cannot end with '.'" }
        require(!tag.contains("..")) { "Tag cannot contain consecutive dots '..'" }
    }

    /**
     * Logs a debug message with the configured tag.
     *
     * @param message The message to log
     * @see Logger.debug
     */
    fun debug(message: String) {
        if (LoggerConfig.isEnabled(tag, LogLevel.DEBUG)) {
            Logger.debug(tag, message)
        }
    }

    /**
     * Logs an informational message with the configured tag.
     *
     * @param message The message to log
     * @see Logger.info
     */
    fun info(message: String) {
        if (LoggerConfig.isEnabled(tag, LogLevel.INFO)) {
            Logger.info(tag, message)
        }
    }

    /**
     * Logs an error message with the configured tag and optional throwable.
     *
     * @param message The error message to log
     * @param throwable Optional exception to log with stack trace
     * @see Logger.error
     */
    fun error(message: String, throwable: Throwable? = null) {
        if (LoggerConfig.isEnabled(tag, LogLevel.ERROR)) {
            Logger.error(tag, message, throwable)
        }
    }

    /**
     * Creates a child logger with a sub-tag appended to the current tag.
     *
     * @param childTag The child tag to append (e.g., "Login")
     * @return A new TaggedLogger with tag "parent.child"
     *
     * Example:
     * ```kotlin
     * val parent = Logger.withTag("EduGo.Auth")
     * val child = parent.withChild("Login")
     * // child.tag == "EduGo.Auth.Login"
     * ```
     */
    fun withChild(childTag: String): TaggedLogger {
        require(childTag.isNotBlank()) { "Child tag cannot be blank" }
        require(!childTag.contains(".")) { "Child tag cannot contain '.', use withTag() for hierarchical tags" }
        return LoggerCache.getOrCreate("$tag.$childTag")
    }

    /**
     * Creates a logger with a new tag, replacing the current tag entirely.
     *
     * @param newTag The new tag to use (can be hierarchical)
     * @return A new TaggedLogger with the specified tag
     */
    fun withTag(newTag: String): TaggedLogger {
        return LoggerCache.getOrCreate(newTag)
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
        fun fromClass(clazz: KClass<*>): TaggedLogger {
            val className = clazz.qualifiedName ?: clazz.simpleName ?: "Unknown"
            return LoggerCache.getOrCreate(className)
        }

        /**
         * Creates a TaggedLogger with the specified tag.
         *
         * @param tag The hierarchical tag to use (e.g., "EduGo.Auth.Login")
         * @return A cached or new TaggedLogger instance
         */
        fun create(tag: String): TaggedLogger {
            return LoggerCache.getOrCreate(tag)
        }
    }
}

/**
 * Log levels for filtering and controlling log output verbosity.
 *
 * Levels are ordered by severity: DEBUG < INFO < ERROR.
 * When a minimum level is set, all logs at that level or higher are shown.
 *
 * ## Usage:
 * ```kotlin
 * // Set minimum level to INFO (hides DEBUG logs)
 * LoggerConfig.setLevel("EduGo.Auth.*", LogLevel.INFO)
 *
 * // Check if level is enabled before expensive operations
 * if (LoggerConfig.isEnabled(tag, LogLevel.DEBUG)) {
 *     logger.debug(expensiveDebugInfo())
 * }
 * ```
 *
 * @see LoggerConfig
 * @see TaggedLogger
 */
enum class LogLevel {
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
     * Error conditions that need attention.
     * Use for exceptions, failures, and abnormal conditions.
     * Always logged in production.
     */
    ERROR
}
