package com.edugo.test.module.platform

/**
 * Platform-agnostic logging interface for multiplatform applications.
 *
 * Provides a unified logging API that delegates to platform-specific
 * logging mechanisms while maintaining consistent behavior across targets.
 *
 * ## Platform-specific implementations:
 *
 * ### Android:
 * - Uses `android.util.Log`
 * - Logs appear in Logcat
 * - Tag is used for filtering
 *
 * ### JVM/Desktop:
 * - Uses `println` or SLF4J (if available)
 * - Logs to console or configured appender
 *
 * ### iOS:
 * - Uses `NSLog` or `os_log`
 * - Logs appear in Xcode console
 *
 * ### JS:
 * - Uses `console.log`, `console.info`, `console.error`
 * - Logs appear in browser/Node.js console
 *
 * ## Tag naming conventions:
 * - Use class/module name: `"NetworkClient"`, `"UserRepository"`
 * - Keep it concise (max 23 chars on Android)
 * - Use PascalCase for consistency
 *
 * @see [Android Logging Best Practices](https://developer.android.com/studio/debug/am-logcat)
 */
expect object Logger {
    /**
     * Logs a debug message.
     *
     * **Use for:**
     * - Development/debugging information
     * - Verbose diagnostics
     * - Flow tracing
     *
     * **⚠️ Note**: Debug logs are typically stripped in release builds.
     * Don't rely on debug logs being available in production.
     *
     * @param tag Identifier for the log source (usually class name)
     * @param message The message to log
     *
     * Example:
     * ```kotlin
     * Logger.debug("NetworkClient", "Sending GET request to /api/users")
     * Logger.debug("UserRepository", "Cache hit for user ID: 123")
     * ```
     *
     * **Platform output:**
     * - Android: `Log.d(tag, message)`
     * - iOS: `os_log(.debug, "%{public}s: %{public}s", tag, message)`
     * - JVM: `println("[DEBUG] $tag: $message")`
     */
    fun debug(tag: String, message: String)

    /**
     * Logs an informational message.
     *
     * **Use for:**
     * - Important state changes
     * - Application lifecycle events
     * - Successful operations
     *
     * **Characteristics:**
     * - Visible in production builds (if enabled)
     * - Lower volume than debug logs
     * - Important for monitoring app behavior
     *
     * @param tag Identifier for the log source (usually class name)
     * @param message The message to log
     *
     * Example:
     * ```kotlin
     * Logger.info("AuthManager", "User logged in successfully")
     * Logger.info("AppDelegate", "Application entered foreground")
     * ```
     *
     * **Platform output:**
     * - Android: `Log.i(tag, message)`
     * - iOS: `os_log(.info, "%{public}s: %{public}s", tag, message)`
     * - JVM: `println("[INFO] $tag: $message")`
     */
    fun info(tag: String, message: String)

    /**
     * Logs an error message with optional exception.
     *
     * **Use for:**
     * - Unexpected errors
     * - Exception handling
     * - Failed operations
     * - Critical issues
     *
     * **Characteristics:**
     * - Always visible in production
     * - Includes stack trace if throwable provided
     * - Should be monitored in production
     *
     * @param tag Identifier for the log source (usually class name)
     * @param message The error message to log
     * @param throwable Optional exception/error to include with stack trace.
     *                  Default is `null`. When provided, the full exception details
     *                  and stack trace are logged to aid in debugging.
     *
     * Example:
     * ```kotlin
     * // Simple error message
     * Logger.error("NetworkClient", "Failed to connect to server")
     *
     * // Error with exception
     * try {
     *     riskyOperation()
     * } catch (e: Exception) {
     *     Logger.error("UserRepository", "Failed to save user", e)
     * }
     * ```
     *
     * **Platform output:**
     * - Android: `Log.e(tag, message, throwable)`
     * - iOS: `os_log(.error, "%{public}s: %{public}s", tag, message)` + stack trace
     * - JVM: `println("[ERROR] $tag: $message")` + `throwable.printStackTrace()`
     *
     * **Note on default parameter**: The `throwable = null` default is handled at
     * the call site. Actual implementations receive an explicit null value when
     * the parameter is omitted.
     */
    fun error(tag: String, message: String, throwable: Throwable? = null)
}
