package com.edugo.test.module.platform

import kotlin.reflect.KClass

/**
 * Extension functions for the Logger object to create tagged loggers.
 *
 * Provides convenient factory methods for creating TaggedLogger instances
 * with hierarchical tag support.
 *
 * ## Usage:
 * ```kotlin
 * // Create from tag string
 * val logger = Logger.withTag("EduGo.Auth")
 *
 * // Create from class
 * class UserRepository {
 *     private val logger = Logger.fromClass(this::class)
 * }
 *
 * // Create inline
 * Logger.withTag("EduGo.Network").info("Request sent")
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
 * val authLogger = Logger.withTag("EduGo.Auth")
 * authLogger.info("Authentication started")
 *
 * val loginLogger = Logger.withTag("EduGo.Auth.Login")
 * loginLogger.debug("Login form displayed")
 * ```
 */
fun Logger.withTag(tag: String): TaggedLogger {
    return LoggerCache.getOrCreate(tag)
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
 *     private val logger = Logger.fromClass(this::class)
 *     // logger.tag == "com.edugo.UserRepository"
 * }
 *
 * // Or statically
 * val logger = Logger.fromClass(UserRepository::class)
 * ```
 */
fun Logger.fromClass(clazz: KClass<*>): TaggedLogger {
    return TaggedLogger.fromClass(clazz)
}

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
     * @return Cached or new TaggedLogger instance
     * @throws IllegalArgumentException if tag is invalid
     *
     * Example:
     * ```kotlin
     * val logger = LoggerCache.getOrCreate("EduGo.Auth")  // OK
     * val invalid = LoggerCache.getOrCreate("")            // Throws
     * ```
     */
    fun getOrCreate(tag: String): TaggedLogger {
        synchronized(lock) {
            return cache.getOrPut(tag) {
                TaggedLogger(tag)  // Constructor validates tag
            }
        }
    }

    /**
     * Gets a TaggedLogger from cache if it exists.
     *
     * @param tag The tag to look up
     * @return The cached logger, or null if not found
     */
    fun get(tag: String): TaggedLogger? {
        synchronized(lock) {
            return cache[tag]
        }
    }

    /**
     * Checks if a logger for the tag exists in cache.
     *
     * @param tag The tag to check
     * @return true if cached, false otherwise
     */
    fun contains(tag: String): Boolean {
        synchronized(lock) {
            return cache.containsKey(tag)
        }
    }

    /**
     * Clears the cache.
     *
     * Use this to free memory or reset state in tests.
     * Existing TaggedLogger instances remain valid but won't be cached.
     */
    fun clear() {
        synchronized(lock) {
            cache.clear()
        }
    }

    /**
     * Gets the current cache size.
     *
     * @return Number of cached loggers
     */
    fun size(): Int {
        synchronized(lock) {
            return cache.size
        }
    }

    /**
     * Gets all cached tags.
     *
     * @return Set of all tags currently in cache (defensive copy)
     */
    fun getAllTags(): Set<String> {
        synchronized(lock) {
            return cache.keys.toSet()
        }
    }

    /**
     * Removes a specific logger from cache.
     *
     * @param tag The tag to remove
     * @return The removed logger, or null if not found
     */
    fun remove(tag: String): TaggedLogger? {
        synchronized(lock) {
            return cache.remove(tag)
        }
    }
}
