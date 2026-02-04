package com.edugo.test.module.platform

/**
 * JVM implementation of PlatformVolatile.
 *
 * Maps directly to kotlin.jvm.Volatile for proper memory visibility between threads
 * on JVM platforms (Android/Desktop).
 */
public actual typealias PlatformVolatile = kotlin.jvm.Volatile
