package com.edugo.test.module.platform

/**
 * Filtering utilities for hierarchical log tags.
 *
 * Supports both wildcard patterns (e.g., "EduGo.Auth.*") and regex patterns
 * for flexible tag matching.
 *
 * ## Wildcard Patterns:
 * - `*` matches any sequence within a single segment
 * - `**` matches any sequence across multiple segments
 * - `.` is treated as a literal separator
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
object LogFilter {
    /**
     * Cache of compiled regex patterns for performance.
     * Lazily initialized when patterns are first used.
     */
    private val regexCache: MutableMap<String, Regex> = mutableMapOf()

    /**
     * Lock for thread-safe access to regexCache.
     */
    private val cacheLock = Any()

    /**
     * Checks if a tag matches a pattern.
     *
     * Supports wildcard patterns with `*` and `**` operators:
     * - `*` matches any sequence within a single tag segment
     * - `**` matches any sequence across multiple segments
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
    fun matches(tag: String, pattern: String): Boolean {
        // Exact match optimization
        if (tag == pattern) return true

        // Single wildcard optimization
        if (pattern == "*" || pattern == "**") return true

        // Convert wildcard pattern to regex and match
        val regex = getOrCompileRegex(pattern)
        return regex.matches(tag)
    }

    /**
     * Checks if a tag matches any of the given patterns.
     *
     * @param tag The tag to check
     * @param patterns Collection of patterns to match against
     * @return true if the tag matches at least one pattern, false otherwise
     */
    fun matchesAny(tag: String, patterns: Collection<String>): Boolean {
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
    fun filter(tags: Collection<String>, pattern: String): List<String> {
        return tags.filter { tag -> matches(tag, pattern) }
    }

    /**
     * Clears the internal regex cache.
     *
     * Call this if you're done with filtering and want to free memory,
     * or if you want to force recompilation of patterns.
     */
    fun clearCache() {
        synchronized(cacheLock) {
            regexCache.clear()
        }
    }

    /**
     * Gets the current size of the regex cache.
     *
     * @return Number of compiled patterns in the cache
     */
    fun getCacheSize(): Int {
        synchronized(cacheLock) {
            return regexCache.size
        }
    }

    /**
     * Gets or compiles a regex pattern from a wildcard pattern.
     *
     * Results are cached for performance. Thread-safe.
     *
     * @param pattern Wildcard pattern (e.g., "EduGo.Auth.*")
     * @return Compiled Regex object
     */
    private fun getOrCompileRegex(pattern: String): Regex {
        synchronized(cacheLock) {
            return regexCache.getOrPut(pattern) {
                compileWildcardPattern(pattern)
            }
        }
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
    fun isValidPattern(pattern: String): Boolean {
        return try {
            compileWildcardPattern(pattern)
            true
        } catch (e: Exception) {
            false
        }
    }
}
