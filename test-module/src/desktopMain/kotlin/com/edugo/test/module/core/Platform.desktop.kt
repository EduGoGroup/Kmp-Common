package com.edugo.test.module.core

/**
 * Desktop (JVM) implementation of currentTimeMillis.
 *
 * Uses System.currentTimeMillis() which provides milliseconds since epoch.
 *
 * @return Current time in milliseconds since January 1, 1970, 00:00:00 GMT
 */
internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()
