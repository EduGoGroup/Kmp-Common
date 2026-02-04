package com.edugo.test.module.platform

/**
 * Platform-specific synchronization primitive.
 *
 * This function provides thread-safe block execution across different platforms:
 * - JVM (Android/Desktop): Uses kotlin.synchronized for actual thread-safety
 * - JS: No-op wrapper since JavaScript is single-threaded
 *
 * @param lock The object to synchronize on
 * @param block The block of code to execute
 * @return The result of executing the block
 */
public expect inline fun <T> platformSynchronized(lock: Any, block: () -> T): T
