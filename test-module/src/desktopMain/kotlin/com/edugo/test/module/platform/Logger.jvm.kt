package com.edugo.test.module.platform

/**
 * JVM implementation of Logger.
 */
actual object Logger {
    actual fun debug(tag: String, message: String) {
        println(LoggerFormatter.format("DEBUG", tag, message))
    }

    actual fun info(tag: String, message: String) {
        println(LoggerFormatter.format("INFO", tag, message))
    }

    actual fun error(tag: String, message: String, throwable: Throwable?) {
        System.err.println(LoggerFormatter.format("ERROR", tag, message))
        throwable?.printStackTrace(System.err)
    }
}
