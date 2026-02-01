package com.edugo.test.module.platform

/**
 * Internal configuration for cache sizes across the logging system.
 *
 * Centralizes cache size limits to ensure consistency and easy tuning.
 *
 * @since 1.0.0
 */
internal object CacheConfig {
    /**
     * Maximum number of entries in pattern regex cache (LogFilter)
     * and level cache (LoggerConfig).
     *
     * Uses FIFO eviction when capacity is reached.
     */
    const val MAX_CACHE_SIZE = 100
}
