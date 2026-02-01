package com.edugo.test.module.platform

/**
 * Public utilities for managing the logger cache.
 *
 * Provides controlled access to cache management operations for testing,
 * debugging, and memory management purposes.
 *
 * ## Use Cases:
 * - **Testing**: Clear cache between tests to ensure isolation
 * - **Debugging**: Inspect cache size and contents
 * - **Memory Management**: Clear cache to free memory in long-running applications
 *
 * ## Example:
 * ```kotlin
 * // In tests
 * @BeforeTest
 * fun setup() {
 *     LoggerCacheUtils.clearCache()
 * }
 *
 * // Check cache size
 * val cacheSize = LoggerCacheUtils.getCacheSize()
 * println("Loggers cached: $cacheSize")
 *
 * // Inspect cached tags
 * val tags = LoggerCacheUtils.getAllCachedTags()
 * println("Cached tags: ${tags.joinToString()}")
 * ```
 *
 * @see TaggedLogger
 * @see LoggerCache
 */
object LoggerCacheUtils {

    /**
     * Clears all cached logger instances.
     *
     * Existing TaggedLogger instances remain valid but won't be in the cache.
     * Subsequent calls to create loggers with the same tag will create new instances
     * until they are cached again.
     *
     * **Use this for:**
     * - Resetting state between tests
     * - Freeing memory in long-running applications
     * - Debugging cache-related issues
     *
     * Example:
     * ```kotlin
     * // Before each test
     * LoggerCacheUtils.clearCache()
     * ```
     */
    fun clearCache() {
        LoggerCache.clear()
    }

    /**
     * Gets the current number of cached logger instances.
     *
     * @return Number of loggers currently in the cache
     *
     * Example:
     * ```kotlin
     * val size = LoggerCacheUtils.getCacheSize()
     * println("Cache contains $size loggers")
     * ```
     */
    fun getCacheSize(): Int {
        return LoggerCache.size()
    }

    /**
     * Gets all tags currently cached.
     *
     * Returns a defensive copy of the tag set, so modifications to the
     * returned set won't affect the actual cache.
     *
     * @return Immutable set of all cached tags
     *
     * Example:
     * ```kotlin
     * val tags = LoggerCacheUtils.getAllCachedTags()
     * tags.forEach { tag ->
     *     println("Cached logger: $tag")
     * }
     * ```
     */
    fun getAllCachedTags(): Set<String> {
        return LoggerCache.getAllTags()
    }

    /**
     * Checks if a logger with the given tag is currently cached.
     *
     * @param tag The tag to check
     * @return true if a logger with this tag is cached, false otherwise
     *
     * Example:
     * ```kotlin
     * if (LoggerCacheUtils.isTagCached("EduGo.Auth")) {
     *     println("Auth logger is cached")
     * }
     * ```
     */
    fun isTagCached(tag: String): Boolean {
        return LoggerCache.contains(tag)
    }

    /**
     * Removes a specific logger from the cache.
     *
     * The logger instance itself remains valid, but won't be in the cache.
     * Next time a logger with this tag is requested, a new instance will be created.
     *
     * @param tag The tag of the logger to remove
     * @return The removed logger instance, or null if it wasn't cached
     *
     * Example:
     * ```kotlin
     * // Remove auth logger from cache
     * val removed = LoggerCacheUtils.removeFromCache("EduGo.Auth")
     * if (removed != null) {
     *     println("Removed logger: ${removed.tag}")
     * }
     * ```
     */
    fun removeFromCache(tag: String): TaggedLogger? {
        return LoggerCache.remove(tag)
    }
}
