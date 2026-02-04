package com.edugo.test.module.platform

/**
 * JavaScript implementation of [Logger].
 *
 * This class provides the JS-specific logging implementation,
 * using console.log, console.warn, and console.error for output.
 *
 * ## Output:
 * - d(), i() -> console.log
 * - w() -> console.warn
 * - e() -> console.error
 *
 * ## Usage:
 * ```kotlin
 * val logger: Logger = JsLogger()
 * logger.d("MyTag", "Debug message")
 * ```
 *
 * Prefer using [createDefaultLogger] to obtain instances.
 *
 * @see Logger
 * @see LoggerFormatter
 * @see createDefaultLogger
 */
class JsLogger : Logger {

    override fun d(tag: String, message: String) {
        console.log(LoggerFormatter.format("DEBUG", tag, message))
    }

    override fun d(tag: String, message: String, throwable: Throwable) {
        console.log(LoggerFormatter.format("DEBUG", tag, message))
        console.log(throwable.stackTraceToString())
    }

    override fun i(tag: String, message: String) {
        console.log(LoggerFormatter.format("INFO", tag, message))
    }

    override fun i(tag: String, message: String, throwable: Throwable) {
        console.log(LoggerFormatter.format("INFO", tag, message))
        console.log(throwable.stackTraceToString())
    }

    override fun w(tag: String, message: String) {
        console.warn(LoggerFormatter.format("WARN", tag, message))
    }

    override fun w(tag: String, message: String, throwable: Throwable) {
        console.warn(LoggerFormatter.format("WARN", tag, message))
        console.warn(throwable.stackTraceToString())
    }

    override fun e(tag: String, message: String) {
        console.error(LoggerFormatter.format("ERROR", tag, message))
    }

    override fun e(tag: String, message: String, throwable: Throwable) {
        console.error(LoggerFormatter.format("ERROR", tag, message))
        console.error(throwable.stackTraceToString())
    }
}

/**
 * Creates the default JS Logger implementation.
 *
 * @return A [JsLogger] instance
 */
public actual fun createDefaultLogger(): Logger = JsLogger()
