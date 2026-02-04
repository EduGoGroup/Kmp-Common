package com.edugo.test.module.platform

/**
 * JavaScript implementation of Platform.
 *
 * Provides platform information for browser and Node.js environments.
 */
public actual object Platform {
    actual val name: String = "JS"

    actual val osVersion: String
        get() = js("typeof navigator !== 'undefined' ? navigator.userAgent : (typeof process !== 'undefined' ? process.version : 'Unknown')").toString()

    actual val isDebug: Boolean
        get() = js("typeof process !== 'undefined' && process.env && process.env.NODE_ENV !== 'production'") as? Boolean ?: false
}
