package com.edugo.test.module.platform

/**
 * JVM/Desktop implementation of [Logger].
 *
 * This class provides the JVM-specific logging implementation,
 * using println for standard output and System.err for errors.
 *
 * ## Output:
 * - d(), i(), w() -> stdout via println
 * - e() -> stderr via System.err.println
 * - Throwables are printed with full stack trace
 *
 * ## Usage:
 * ```kotlin
 * val logger: Logger = JvmLogger()
 * logger.d("MyTag", "Debug message")
 * ```
 *
 * Prefer using [createDefaultLogger] to obtain instances.
 *
 * @see Logger
 * @see LoggerFormatter
 * @see createDefaultLogger
 */
class JvmLogger : Logger {

    override fun d(tag: String, message: String) {
        println(LoggerFormatter.format("DEBUG", tag, message))
    }

    override fun d(tag: String, message: String, throwable: Throwable) {
        println(LoggerFormatter.format("DEBUG", tag, message))
        throwable.printStackTrace(System.out)
    }

    override fun i(tag: String, message: String) {
        println(LoggerFormatter.format("INFO", tag, message))
    }

    override fun i(tag: String, message: String, throwable: Throwable) {
        println(LoggerFormatter.format("INFO", tag, message))
        throwable.printStackTrace(System.out)
    }

    override fun w(tag: String, message: String) {
        println(LoggerFormatter.format("WARN", tag, message))
    }

    override fun w(tag: String, message: String, throwable: Throwable) {
        println(LoggerFormatter.format("WARN", tag, message))
        throwable.printStackTrace(System.out)
    }

    override fun e(tag: String, message: String) {
        System.err.println(LoggerFormatter.format("ERROR", tag, message))
    }

    override fun e(tag: String, message: String, throwable: Throwable) {
        System.err.println(LoggerFormatter.format("ERROR", tag, message))
        throwable.printStackTrace(System.err)
    }
}

/**
 * Creates the default JVM Logger implementation.
 *
 * @return A [JvmLogger] instance
 */
actual fun createDefaultLogger(): Logger = JvmLogger()
