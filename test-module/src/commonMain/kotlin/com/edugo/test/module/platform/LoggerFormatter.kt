package com.edugo.test.module.platform

/**
 * Internal formatter for log messages.
 *
 * Provides consistent formatting for platforms that don't have native logging systems
 * (e.g., JVM desktop, JS, Native). Android uses its own android.util.Log format.
 *
 * @since 1.0.0
 */
internal object LoggerFormatter {
    /**
     * Formats a log message with level prefix and tag.
     *
     * @param level The log level (DEBUG, INFO, ERROR)
     * @param tag The hierarchical tag
     * @param message The log message
     * @return Formatted string: "[LEVEL] tag: message"
     */
    public fun format(level: String, tag: String, message: String): String {
        return "[$level] $tag: $message"
    }
}
