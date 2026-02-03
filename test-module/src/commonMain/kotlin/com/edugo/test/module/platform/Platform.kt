package com.edugo.test.module.platform

/**
 * Provides platform-specific information about the current runtime environment.
 *
 * This is a common interface for accessing platform details across all targets.
 * Each platform (Android, iOS, JVM, JS) provides its own actual implementation.
 *
 * ## Platform-specific implementations:
 * - **Android**: Uses `Build` class from Android SDK
 * - **JVM/Desktop**: Uses Java system properties
 * - **iOS**: Uses UIDevice (when available)
 * - **JS**: Uses navigator object (browser) or process (Node.js)
 *
 * Example usage:
 * ```kotlin
 * when (Platform.name) {
 *     "Android" -> showToast("Running on Android")
 *     "iOS" -> showAlert("Running on iOS")
 * }
 * ```
 *
 * @see [Kotlin Multiplatform expect/actual](https://kotlinlang.org/docs/multiplatform-expect-actual.html)
 */
public expect object Platform {
    /**
     * The name of the current platform.
     *
     * Expected values:
     * - `"Android"` - Android devices and emulators
     * - `"JVM"` - Desktop JVM (Windows, macOS, Linux)
     * - `"iOS"` - iOS devices and simulators
     * - `"JS"` - JavaScript (browser or Node.js)
     */
    val name: String

    /**
     * The operating system version string.
     *
     * Format varies by platform:
     * - **Android**: SDK version (e.g., `"34"` for Android 14)
     * - **JVM**: OS version (e.g., `"14.0"` for macOS Sonoma)
     * - **iOS**: iOS version (e.g., `"17.0"`)
     * - **JS**: Browser/Node version
     */
    val osVersion: String

    /**
     * Indicates whether the app is running in debug mode.
     *
     * Platform implementations:
     * - **Android**: Returns `BuildConfig.DEBUG`
     * - **JVM**: Checks for debug agent or system property
     * - **iOS**: Checks for `DEBUG` preprocessor flag
     * - **JS**: Checks for development mode
     *
     * Use this for enabling verbose logging or showing developer tools.
     */
    val isDebug: Boolean
}

/**
 * Returns a human-readable description of the current platform.
 *
 * **Stability**: Stable API - safe to use in production.
 *
 * Format: `"Running on {platform} ({osVersion})"`
 *
 * Example outputs:
 * - `"Running on Android (34)"`
 * - `"Running on JVM (14.0)"`
 * - `"Running on iOS (17.0)"`
 *
 * @return A formatted string with platform name and OS version
 */
public fun getPlatformDescription(): String = "Running on ${Platform.name} (${Platform.osVersion})"
