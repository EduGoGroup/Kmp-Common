package com.edugo.test.module.core

import kotlin.js.Date

/**
 * JavaScript implementation of currentTimeMillis.
 *
 * Uses JavaScript's Date.now() to get the current timestamp in milliseconds.
 *
 * @return Current time in milliseconds since January 1, 1970, 00:00:00 GMT
 */
internal actual fun currentTimeMillis(): Long {
    return Date.now().toLong()
}
