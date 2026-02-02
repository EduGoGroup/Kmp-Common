package com.edugo.test.module.platform

import android.util.Log

/**
 * Android implementation of [Logger] using android.util.Log.
 *
 * This class provides the Android-specific logging implementation,
 * delegating all log calls to the Android logging framework.
 *
 * ## Log Levels Mapping:
 * - d() -> Log.d()
 * - i() -> Log.i()
 * - w() -> Log.w()
 * - e() -> Log.e()
 *
 * ## Usage:
 * ```kotlin
 * val logger: Logger = AndroidLogger()
 * logger.d("MyTag", "Debug message")
 * ```
 *
 * Prefer using [createDefaultLogger] to obtain instances.
 *
 * @see Logger
 * @see createDefaultLogger
 */
class AndroidLogger : Logger {

    override fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun d(tag: String, message: String, throwable: Throwable) {
        Log.d(tag, message, throwable)
    }

    override fun i(tag: String, message: String) {
        Log.i(tag, message)
    }

    override fun i(tag: String, message: String, throwable: Throwable) {
        Log.i(tag, message, throwable)
    }

    override fun w(tag: String, message: String) {
        Log.w(tag, message)
    }

    override fun w(tag: String, message: String, throwable: Throwable) {
        Log.w(tag, message, throwable)
    }

    override fun e(tag: String, message: String) {
        Log.e(tag, message)
    }

    override fun e(tag: String, message: String, throwable: Throwable) {
        Log.e(tag, message, throwable)
    }
}

/**
 * Creates the default Android Logger implementation.
 *
 * @return An [AndroidLogger] instance
 */
actual fun createDefaultLogger(): Logger = AndroidLogger()
