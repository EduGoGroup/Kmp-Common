package com.edugo.test.module.platform

/**
 * JVM implementation of platformSynchronized.
 *
 * Uses kotlin.synchronized for actual thread-safety on JVM platforms (Android/Desktop).
 */
public actual inline fun <T> platformSynchronized(lock: Any, block: () -> T): T =
    kotlin.synchronized(lock, block)
