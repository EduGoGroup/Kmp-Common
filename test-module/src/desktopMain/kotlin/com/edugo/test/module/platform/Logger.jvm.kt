package com.edugo.test.module.platform

/**
 * JVM implementation of Logger.
 */
actual object Logger {
    actual fun debug(tag: String, message: String) {
        println("[DEBUG] $tag: $message")
    }

    actual fun info(tag: String, message: String) {
        println("[INFO] $tag: $message")
    }

    actual fun error(tag: String, message: String, throwable: Throwable?) {
        System.err.println("[ERROR] $tag: $message")
        throwable?.printStackTrace(System.err)
    }
}
