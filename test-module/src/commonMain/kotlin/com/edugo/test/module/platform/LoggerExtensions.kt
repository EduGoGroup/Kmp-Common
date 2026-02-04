package com.edugo.test.module.platform

import com.edugo.test.module.core.AppError
import com.edugo.test.module.core.Result
import com.edugo.test.module.platform.platformSynchronized
import kotlin.reflect.KClass

/**
 * Extension functions for the Logger interface to create tagged loggers
 * and convenient logging utilities.
 *
 * Provides factory methods for creating TaggedLogger instances
 * with hierarchical tag support, plus extensions for Result and AppError logging.
 *
 * ## Usage:
 * ```kotlin
 * // Create from tag string
 * val logger = DefaultLogger.withTag("EduGo.Auth")
 *
 * // Create from class
 * class UserRepository {
 *     private val logger = DefaultLogger.fromClass(this::class)
 * }
 *
 * // Log Result failures
 * val result: Result<User> = fetchUser()
 * result.logOnFailure("UserRepo", DefaultLogger)
 *
 * // Log AppError
 * val error = AppError.notFound("User not found")
 * error.log("UserRepo", DefaultLogger)
 * ```
 *
 * @see TaggedLogger
 * @see LoggerCache
 */

/**
 * Creates a TaggedLogger with the specified hierarchical tag.
 *
 * Loggers are cached by tag for performance. Calling this function
 * multiple times with the same tag returns the same instance.
 *
 * @param tag Hierarchical tag (e.g., "EduGo.Auth.Login")
 * @return Cached or new TaggedLogger instance
 *
 * Example:
 * ```kotlin
 * val authLogger = DefaultLogger.withTag("EduGo.Auth")
 * authLogger.i("Authentication started")
 *
 * val loginLogger = DefaultLogger.withTag("EduGo.Auth.Login")
 * loginLogger.d("Login form displayed")
 * ```
 */
public fun Logger.withTag(tag: String): TaggedLogger {
    return LoggerCache.getOrCreate(tag, this)
}

/**
 * Creates a TaggedLogger from a KClass, using the qualified name as the tag.
 *
 * @param clazz The class to derive the tag from
 * @return TaggedLogger with tag based on class name
 *
 * Example:
 * ```kotlin
 * class UserRepository {
 *     private val logger = DefaultLogger.fromClass(this::class)
 *     // logger.tag == "com.edugo.UserRepository"
 * }
 *
 * // Or statically
 * val logger = DefaultLogger.fromClass(UserRepository::class)
 * ```
 */
public fun Logger.fromClass(clazz: KClass<*>): TaggedLogger {
    return TaggedLogger.fromClass(clazz, this)
}

// ==================== Result Extensions ====================

/**
 * Logs the failure if this Result is a Failure, then returns the original Result.
 *
 * This is useful for chaining with other operations while ensuring failures are logged.
 * The Result is returned unchanged, allowing for method chaining.
 *
 * @param tag The log tag to use
 * @param logger The Logger instance to use (defaults to DefaultLogger)
 * @return This Result unchanged
 *
 * Example:
 * ```kotlin
 * val result = fetchUser()
 *     .logOnFailure("UserRepo")
 *     .map { it.name }
 *
 * // Or with custom logger
 * val result = fetchUser()
 *     .logOnFailure("UserRepo", myLogger)
 *     .getOrNull()
 * ```
 */
public inline fun <T> Result<T>.logOnFailure(
    tag: String,
    logger: Logger = DefaultLogger
): Result<T> {
    if (this is Result.Failure) {
        logger.e(tag, "Operation failed: $error")
    }
    return this
}

/**
 * Logs the failure with a custom message prefix if this Result is a Failure.
 *
 * @param tag The log tag to use
 * @param messagePrefix Custom prefix for the error message
 * @param logger The Logger instance to use (defaults to DefaultLogger)
 * @return This Result unchanged
 *
 * Example:
 * ```kotlin
 * val result = fetchUser()
 *     .logOnFailure("UserRepo", "Failed to fetch user")
 *     .getOrNull()
 * ```
 */
public inline fun <T> Result<T>.logOnFailure(
    tag: String,
    messagePrefix: String,
    logger: Logger = DefaultLogger
): Result<T> {
    if (this is Result.Failure) {
        logger.e(tag, "$messagePrefix: $error")
    }
    return this
}

/**
 * Logs the success if this Result is a Success, then returns the original Result.
 *
 * Useful for debugging successful operations in the pipeline.
 *
 * @param tag The log tag to use
 * @param logger The Logger instance to use (defaults to DefaultLogger)
 * @param message Lambda that produces the success message from the data
 * @return This Result unchanged
 *
 * Example:
 * ```kotlin
 * val result = fetchUser()
 *     .logOnSuccess("UserRepo") { user -> "Fetched user: ${user.name}" }
 *     .map { it.name }
 * ```
 */
public inline fun <T> Result<T>.logOnSuccess(
    tag: String,
    logger: Logger = DefaultLogger,
    message: (T) -> String
): Result<T> {
    if (this is Result.Success) {
        logger.d(tag, message(data))
    }
    return this
}

// ==================== AppError Extensions ====================

/**
 * Logs this AppError with appropriate severity based on the error code.
 *
 * - Network errors and retryable errors are logged at WARNING level
 * - All other errors are logged at ERROR level
 * - Includes the cause throwable if present
 *
 * @param tag The log tag to use
 * @param logger The Logger instance to use (defaults to DefaultLogger)
 *
 * Example:
 * ```kotlin
 * try {
 *     performOperation()
 * } catch (e: Exception) {
 *     val error = AppError.fromException(e)
 *     error.log("MyClass", myLogger)
 * }
 * ```
 */
public fun AppError.log(tag: String, logger: Logger = DefaultLogger) {
    val formattedMessage = buildString {
        append("[${code.name}] $message")
        if (details.isNotEmpty()) {
            append(" | details: ")
            append(details.entries.take(5).joinToString(", ") { "${it.key}=${it.value}" })
            if (details.size > 5) {
                append(", ... (+${details.size - 5} more)")
            }
        }
    }

    when {
        isRetryable() || code.isNetworkError() -> {
            if (cause != null) {
                logger.w(tag, formattedMessage, cause)
            } else {
                logger.w(tag, formattedMessage)
            }
        }
        else -> {
            if (cause != null) {
                logger.e(tag, formattedMessage, cause)
            } else {
                logger.e(tag, formattedMessage)
            }
        }
    }
}

/**
 * Logs this AppError at DEBUG level (for non-critical errors).
 *
 * @param tag The log tag to use
 * @param logger The Logger instance to use (defaults to DefaultLogger)
 */
public fun AppError.logDebug(tag: String, logger: Logger = DefaultLogger) {
    val formattedMessage = "[${code.name}] $message"
    if (cause != null) {
        logger.d(tag, formattedMessage, cause)
    } else {
        logger.d(tag, formattedMessage)
    }
}

/**
 * Logs this AppError at INFO level.
 *
 * @param tag The log tag to use
 * @param logger The Logger instance to use (defaults to DefaultLogger)
 */
public fun AppError.logInfo(tag: String, logger: Logger = DefaultLogger) {
    val formattedMessage = "[${code.name}] $message"
    if (cause != null) {
        logger.i(tag, formattedMessage, cause)
    } else {
        logger.i(tag, formattedMessage)
    }
}

/**
 * Logs this AppError at WARNING level.
 *
 * @param tag The log tag to use
 * @param logger The Logger instance to use (defaults to DefaultLogger)
 */
public fun AppError.logWarning(tag: String, logger: Logger = DefaultLogger) {
    val formattedMessage = "[${code.name}] $message"
    if (cause != null) {
        logger.w(tag, formattedMessage, cause)
    } else {
        logger.w(tag, formattedMessage)
    }
}

/**
 * Logs this AppError at ERROR level with full details.
 *
 * @param tag The log tag to use
 * @param logger The Logger instance to use (defaults to DefaultLogger)
 */
public fun AppError.logError(tag: String, logger: Logger = DefaultLogger) {
    log(tag, logger)
}

// ==================== LoggerCache ====================

/**
 * Thread-safe cache of TaggedLogger instances.
 *
 * Ensures that only one TaggedLogger instance exists per unique tag,
 * reducing memory usage and improving performance.
 *
 * ## Cache Strategy:
 * - **Unbounded**: Cache grows indefinitely (no eviction policy)
 * - **Thread-safe**: All operations synchronized for concurrent access
 * - **Identity**: Same tag always returns same instance (===)
 *
 * ## Thread-safety:
 * All operations are synchronized to ensure safe concurrent access from multiple threads/coroutines.
 *
 * ## Usage Example:
 * ```kotlin
 * // First call creates new instance
 * val logger1 = LoggerCache.getOrCreate("EduGo.Auth")
 *
 * // Second call returns cached instance
 * val logger2 = LoggerCache.getOrCreate("EduGo.Auth")
 *
 * assert(logger1 === logger2) // Same instance
 * ```
 *
 * @see TaggedLogger
 * @see LoggerCacheUtils
 */
internal object LoggerCache {
    /**
     * Cache map: tag -> TaggedLogger instance
     */
    private val cache: MutableMap<String, TaggedLogger> = mutableMapOf()

    /**
     * Lock for thread-safe access
     */
    private val lock = Any()

    /**
     * Gets or creates a TaggedLogger for the specified tag.
     *
     * ## Preconditions:
     * Tag must be valid (not blank, no leading/trailing dots, no consecutive dots).
     * Validation is performed by TaggedLogger constructor, which throws IllegalArgumentException
     * if tag is invalid.
     *
     * @param tag The hierarchical tag (must be valid)
     * @param logger The Logger implementation to use (defaults to DefaultLogger)
     * @return Cached or new TaggedLogger instance
     * @throws IllegalArgumentException if tag is invalid
     *
     * Example:
     * ```kotlin
     * val logger = LoggerCache.getOrCreate("EduGo.Auth")  // OK
     * val invalid = LoggerCache.getOrCreate("")            // Throws
     * ```
     */
    public fun getOrCreate(tag: String, logger: Logger = DefaultLogger): TaggedLogger {
        platformSynchronized(lock) {
            return cache.getOrPut(tag) {
                TaggedLogger(tag, logger)  // Constructor validates tag
            }
        }
    }

    /**
     * Gets a TaggedLogger from cache if it exists.
     *
     * @param tag The tag to look up
     * @return The cached logger, or null if not found
     */
    public fun get(tag: String): TaggedLogger? {
        platformSynchronized(lock) {
            return cache[tag]
        }
    }

    /**
     * Checks if a logger for the tag exists in cache.
     *
     * @param tag The tag to check
     * @return true if cached, false otherwise
     */
    public fun contains(tag: String): Boolean {
        platformSynchronized(lock) {
            return cache.containsKey(tag)
        }
    }

    /**
     * Clears the cache.
     *
     * Use this to free memory or reset state in tests.
     * Existing TaggedLogger instances remain valid but won't be cached.
     */
    public fun clear() {
        platformSynchronized(lock) {
            cache.clear()
        }
    }

    /**
     * Gets the current cache size.
     *
     * @return Number of cached loggers
     */
    public fun size(): Int {
        platformSynchronized(lock) {
            return cache.size
        }
    }

    /**
     * Gets all cached tags.
     *
     * @return Set of all tags currently in cache (defensive copy)
     */
    public fun getAllTags(): Set<String> {
        platformSynchronized(lock) {
            return cache.keys.toSet()
        }
    }

    /**
     * Removes a specific logger from cache.
     *
     * @param tag The tag to remove
     * @return The removed logger, or null if not found
     */
    public fun remove(tag: String): TaggedLogger? {
        platformSynchronized(lock) {
            return cache.remove(tag)
        }
    }
}
