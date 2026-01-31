package com.edugo.test.module.core

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/**
 * iOS implementation of currentTimeMillis.
 *
 * Uses NSDate to get the current time and converts it to milliseconds.
 *
 * @return Current time in milliseconds since January 1, 1970, 00:00:00 GMT
 */
internal actual fun currentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}
