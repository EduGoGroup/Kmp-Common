package com.edugo.test.module.platform

import android.os.Build

/**
 * Android implementation of Platform.
 *
 * Note: isDebug defaults to false since BuildConfig is disabled by default.
 * Applications should override this or enable buildconfig in their modules.
 */
actual object Platform {
    actual val name: String = "Android"

    actual val osVersion: String
        get() = Build.VERSION.SDK_INT.toString()

    /**
     * Debug flag. Defaults to false since BuildConfig is disabled.
     * Override via initialization if needed.
     */
    actual val isDebug: Boolean = false
}
