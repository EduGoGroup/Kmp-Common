package com.edugo.test.module.platform

/**
 * Logger multiplataforma.
 *
 * Implementaciones:
 * - Android: Log.d/i/e
 * - JVM: SLF4J o println
 * - JS: console.log
 */
expect object Logger {
    fun debug(tag: String, message: String)
    fun info(tag: String, message: String)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}
