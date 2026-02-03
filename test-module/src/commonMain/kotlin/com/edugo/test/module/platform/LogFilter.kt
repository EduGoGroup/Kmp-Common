package com.edugo.test.module.platform

/**
 * Filtering utilities for hierarchical log tags.
 *
 * Supports both wildcard patterns (e.g., "EduGo.Auth.*") and regex patterns
 * (prefix with "regex:") for flexible tag matching.
 *
 * ## Wildcard Patterns:
 * - `*` matches any sequence within a single segment
 * - `**` matches any sequence across multiple segments
 * - `.` is treated as a literal separator
 *
 * ## Regex Patterns:
 * - Prefix with `regex:` to indicate a raw regex pattern
 * - Example: `regex:^EduGo\\.(Auth|Network)\\..*$`
 *
 * ## Examples:
 * ```kotlin
 * LogFilter.matches("EduGo.Auth.Login", "EduGo.Auth.*")     // true
 * LogFilter.matches("EduGo.Auth.Login", "EduGo.*")          // true
 * LogFilter.matches("EduGo.Auth.Login.OAuth", "EduGo.**")   // true
 * LogFilter.matches("EduGo.Auth.Login", "EduGo.Network.*")  // false
 * ```
 *
 * @see LoggerConfig
 * @see TaggedLogger
 */
public object LogFilter {
    /**
     * Prefix used to mark regex patterns explicitly.
     */
    private const val REGEX_PREFIX = "regex:"

    /**
     * Maximum number of compiled patterns to cache.
     * Prevents unbounded memory growth with dynamic patterns.
     */
    private const val MAX_CACHE_SIZE = CacheConfig.MAX_CACHE_SIZE

    /**
     * Cache of compiled regex patterns for performance.
     * Uses FIFO eviction when cache is full.
     */
    private val regexCache: MutableMap<String, Regex> = mutableMapOf()

    /**
     * Tracks insertion order for FIFO eviction.
     * When cache is full, removes the oldest pattern.
     */
    private val insertionOrder: MutableList<String> = mutableListOf()

    /**
     * Lock for thread-safe access to cache and insertion order.
     */
    private val cacheLock = Any()

    /**
     * Checks if a tag matches a pattern.
     *
     * Supports wildcard patterns with `*` and `**` operators:
     * - `*` matches any sequence within a single tag segment
     * - `**` matches any sequence across multiple segments
     *
     * ## Performance:
     * Patterns are compiled to regex and cached (max 100 patterns, FIFO eviction).
     * - First call for a pattern: O(n) compilation
     * - Subsequent calls: O(1) cache lookup
     *
     * @param tag The tag to check (e.g., "EduGo.Auth.Login")
     * @param pattern The pattern to match against (e.g., "EduGo.Auth.*")
     * @return true if the tag matches the pattern, false otherwise
     *
     * Example:
     * ```kotlin
     * LogFilter.matches("EduGo.Auth.Login", "EduGo.Auth.*")  // true
     * LogFilter.matches("EduGo.Auth.Login", "*.Login")       // true
     * LogFilter.matches("EduGo.Auth.Login", "EduGo.**")      // true
     * ```
     */
    public fun matches(tag: String, pattern: String): Boolean {
        if (pattern.isBlank()) return false

        // Exact match optimization
        if (!isRegexPattern(pattern) && tag == pattern) return true

        // Single wildcard optimization
        if (!isRegexPattern(pattern) && (pattern == "*" || pattern == "**")) return true

        // Convert wildcard pattern to regex and match
        return try {
            val regex = getOrCompileRegex(pattern)
            regex.matches(tag)
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /**
     * Checks if a tag matches any of the given patterns.
     *
     * @param tag The tag to check
     * @param patterns Collection of patterns to match against
     * @return true if the tag matches at least one pattern, false otherwise
     */
    public fun matchesAny(tag: String, patterns: Collection<String>): Boolean {
        return patterns.any { pattern -> matches(tag, pattern) }
    }

    /**
     * Filters a collection of tags by a pattern.
     *
     * @param tags Collection of tags to filter
     * @param pattern Pattern to match
     * @return List of tags that match the pattern
     *
     * Example:
     * ```kotlin
     * val tags = listOf("EduGo.Auth.Login", "EduGo.Auth.Logout", "EduGo.Network.HTTP")
     * val authTags = LogFilter.filter(tags, "EduGo.Auth.*")
     * // authTags == ["EduGo.Auth.Login", "EduGo.Auth.Logout"]
     * ```
     */
    public fun filter(tags: Collection<String>, pattern: String): List<String> {
        return tags.filter { tag -> matches(tag, pattern) }
    }

    /**
     * Clears the internal regex cache.
     *
     * Call this if you're done with filtering and want to free memory,
     * or if you want to force recompilation of patterns.
     */
    public fun clearCache() {
        synchronized(cacheLock) {
            regexCache.clear()
            insertionOrder.clear()
        }
    }

    /**
     * Gets the current size of the regex cache.
     *
     * @return Number of compiled patterns in the cache
     */
    public fun getCacheSize(): Int {
        synchronized(cacheLock) {
            return regexCache.size
        }
    }

    /**
     * Gets or compiles a regex pattern from a wildcard pattern.
     *
     * Results are cached for performance with bounded size (max 100 patterns).
     * Uses FIFO eviction when cache is full. Thread-safe.
     *
     * @param pattern Wildcard pattern (e.g., "EduGo.Auth.*")
     * @return Compiled Regex object
     */
    private fun getOrCompileRegex(pattern: String): Regex {
        synchronized(cacheLock) {
            // Check if already cached
            regexCache[pattern]?.let { return it }

            // Cache is full - evict oldest entry (FIFO)
            if (regexCache.size >= MAX_CACHE_SIZE) {
                val oldestKey = insertionOrder.removeAt(0)
                regexCache.remove(oldestKey)
            }

            // Compile and cache new pattern
            val compiled = compilePattern(pattern)
            regexCache[pattern] = compiled
            insertionOrder.add(pattern)

            return compiled
        }
    }

    /**
     * Determines whether a pattern should be treated as regex.
     */
    private fun isRegexPattern(pattern: String): Boolean = pattern.startsWith(REGEX_PREFIX)

    /**
     * Removes regex prefix from pattern.
     */
    private fun stripRegexPrefix(pattern: String): String = pattern.removePrefix(REGEX_PREFIX)

    /**
     * Compiles a pattern into a Regex, using wildcard or regex rules based on prefix.
     */
    private fun compilePattern(pattern: String): Regex {
        return if (isRegexPattern(pattern)) {
            compileRegexPattern(pattern)
        } else {
            compileWildcardPattern(pattern)
        }
    }

    /**
     * Compiles a regex pattern (prefixed with "regex:").
     *
     * @param pattern Regex pattern with prefix
     * @return Compiled Regex object
     */
    private fun compileRegexPattern(pattern: String): Regex {
        val raw = stripRegexPrefix(pattern)
        require(raw.isNotBlank()) { "Regex pattern cannot be blank" }
        return Regex(raw)
    }

    /**
     * Converts a wildcard pattern to a regex pattern.
     *
     * ## Wildcard rules:
     * - `**` → `.*` (matches any sequence, including dots)
     * - `*` → `[^.]*` (matches any sequence except dots)
     * - `.` → `\.` (literal dot)
     * - Other chars → escaped
     *
     * @param pattern Wildcard pattern
     * @return Compiled Regex object
     */
    private fun compileWildcardPattern(pattern: String): Regex {
        val regexPattern = buildString {
            append("^")
            var i = 0
            while (i < pattern.length) {
                when {
                    // Double wildcard: matches across segments
                    pattern.startsWith("**", i) -> {
                        append(".*")
                        i += 2
                    }
                    // Single wildcard: matches within segment
                    pattern[i] == '*' -> {
                        append("[^.]*")
                        i++
                    }
                    // Literal dot separator
                    pattern[i] == '.' -> {
                        append("\\.")
                        i++
                    }
                    // Other special regex characters need escaping
                    pattern[i] in "\\^$[](){}+?|" -> {
                        append("\\")
                        append(pattern[i])
                        i++
                    }
                    // Regular character
                    else -> {
                        append(pattern[i])
                        i++
                    }
                }
            }
            append("$")
        }
        return Regex(regexPattern)
    }

    /**
     * Checks if a pattern is a valid wildcard pattern.
     *
     * @param pattern Pattern to validate
     * @return true if valid, false otherwise
     */
    public fun isValidPattern(pattern: String): Boolean {
        if (pattern.isBlank()) return false
        return try {
            compilePattern(pattern)
            true
        } catch (e: Exception) {
            false
        }
    }
}
