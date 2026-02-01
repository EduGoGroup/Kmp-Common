package com.edugo.test.module.platform

/**
 * Configuration for logger filtering by tag patterns.
 *
 * Allows configuring minimum log levels for different tag patterns,
 * enabling fine-grained control over logging output.
 *
 * ## Features:
 * - **Pattern-based filtering**: Use wildcards to match multiple tags
 * - **Hierarchical rules**: More specific patterns override general ones
 * - **Thread-safe**: Configuration can be safely modified at runtime
 * - **Default level**: Configurable fallback for unmatched tags
 *
 * ## Usage:
 * ```kotlin
 * // Set minimum level for all Auth logs
 * LoggerConfig.setLevel("EduGo.Auth.*", LogLevel.DEBUG)
 *
 * // Disable debug logs for Network
 * LoggerConfig.setLevel("EduGo.Network.*", LogLevel.INFO)
 *
 * // Check if a specific tag/level is enabled
 * if (LoggerConfig.isEnabled("EduGo.Auth.Login", LogLevel.DEBUG)) {
 *     // Log expensive debug info
 * }
 * ```
 *
 * @see TaggedLogger
 * @see LogFilter
 */
object LoggerConfig {
    /**
     * Default log level for tags without specific configuration.
     * Default is DEBUG (all logs enabled).
     *
     * Thread-safe: Uses @Volatile for visibility across threads.
     */
    @Volatile
    var defaultLevel: LogLevel = LogLevel.DEBUG

    /**
     * Maximum number of computed levels to cache.
     * Prevents unbounded memory growth with dynamic tags.
     */
    private const val MAX_LEVEL_CACHE_SIZE = 100

    /**
     * Map of tag patterns to their minimum log levels.
     * Patterns support wildcards (e.g., "EduGo.Auth.*").
     */
    private val levelRules: MutableMap<String, LogLevel> = mutableMapOf()

    /**
     * Cache of computed log levels per tag for performance.
     * Uses FIFO eviction when cache is full.
     * Invalidated when rules change.
     */
    private val levelCache: MutableMap<String, LogLevel> = mutableMapOf()

    /**
     * Tracks insertion order for FIFO eviction of level cache.
     */
    private val cacheInsertionOrder: MutableList<String> = mutableListOf()

    /**
     * Lock for thread-safe access to levelRules and cache.
     */
    private val lock = Any()

    /**
     * Sets the minimum log level for a tag pattern.
     *
     * Invalidates the level cache to ensure new rules take effect.
     *
     * @param pattern Tag pattern to match (supports wildcards: "EduGo.Auth.*")
     * @param level Minimum log level for matching tags
     *
     * Example:
     * ```kotlin
     * // Only show INFO and ERROR for network logs
     * LoggerConfig.setLevel("EduGo.Network.*", LogLevel.INFO)
     *
     * // Only show errors for third-party libraries
     * LoggerConfig.setLevel("com.thirdparty.*", LogLevel.ERROR)
     * ```
     */
    fun setLevel(pattern: String, level: LogLevel) {
        require(pattern.isNotBlank()) { "Pattern cannot be blank" }
        synchronized(lock) {
            levelRules[pattern] = level
            // Invalidate cache when rules change
            levelCache.clear()
            cacheInsertionOrder.clear()
        }
    }

    /**
     * Removes a log level rule for a pattern.
     *
     * Invalidates the level cache to ensure changes take effect.
     *
     * @param pattern The pattern to remove
     */
    fun removeLevel(pattern: String) {
        synchronized(lock) {
            levelRules.remove(pattern)
            // Invalidate cache when rules change
            levelCache.clear()
            cacheInsertionOrder.clear()
        }
    }

    /**
     * Clears all log level rules, keeping only the default level.
     *
     * Invalidates the level cache to ensure changes take effect.
     */
    fun clearLevels() {
        synchronized(lock) {
            levelRules.clear()
            // Invalidate cache when rules change
            levelCache.clear()
            cacheInsertionOrder.clear()
        }
    }

    /**
     * Gets the effective minimum log level for a tag.
     *
     * Finds the most specific matching pattern and returns its level,
     * or the default level if no pattern matches.
     *
     * Uses a bounded cache (max 100 tags) for performance.
     * First call for a tag: O(n*m), subsequent calls: O(1).
     *
     * @param tag The tag to check
     * @return The minimum log level for this tag
     */
    fun getLevel(tag: String): LogLevel {
        synchronized(lock) {
            // Check cache first (fast path)
            levelCache[tag]?.let { return it }

            // Cache miss - compute level (slow path)
            val matchingPatterns = levelRules.entries
                .filter { (pattern, _) -> LogFilter.matches(tag, pattern) }
                .sortedByDescending { (pattern, _) -> pattern.length }

            val level = matchingPatterns.firstOrNull()?.value ?: defaultLevel

            // Cache is full - evict oldest entry (FIFO)
            if (levelCache.size >= MAX_LEVEL_CACHE_SIZE) {
                val oldestTag = cacheInsertionOrder.removeAt(0)
                levelCache.remove(oldestTag)
            }

            // Store in cache for future calls
            levelCache[tag] = level
            cacheInsertionOrder.add(tag)

            return level
        }
    }

    /**
     * Checks if a specific tag and log level combination is enabled.
     *
     * @param tag The tag to check
     * @param level The log level to check
     * @return true if the level is enabled for this tag, false otherwise
     *
     * Example:
     * ```kotlin
     * if (LoggerConfig.isEnabled("EduGo.Auth", LogLevel.DEBUG)) {
     *     logger.debug("Expensive debug info: ${computeExpensiveData()}")
     * }
     * ```
     */
    fun isEnabled(tag: String, level: LogLevel): Boolean {
        val minLevel = getLevel(tag)
        return level.ordinal >= minLevel.ordinal
    }

    /**
     * Gets a copy of all configured level rules.
     *
     * @return Map of patterns to levels (defensive copy)
     */
    fun getAllRules(): Map<String, LogLevel> {
        synchronized(lock) {
            return levelRules.toMap()
        }
    }

    /**
     * Resets configuration to defaults (all levels enabled).
     *
     * Clears all rules and cache.
     */
    fun reset() {
        synchronized(lock) {
            levelRules.clear()
            levelCache.clear()
            cacheInsertionOrder.clear()
            defaultLevel = LogLevel.DEBUG
        }
    }
}
