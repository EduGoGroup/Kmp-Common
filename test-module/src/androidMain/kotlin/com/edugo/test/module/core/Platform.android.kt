package com.edugo.test.module.core

/**
 * Android implementation of currentTimeMillis.
 *
 * Uses System.currentTimeMillis() which is available on Android's JVM.
 *
 * @return Current time in milliseconds since January 1, 1970, 00:00:00 GMT
 */
internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()
