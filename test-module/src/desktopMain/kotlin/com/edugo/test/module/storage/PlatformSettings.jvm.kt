/*
 * Copyright (c) 2026 EduGo Project
 * Licensed under the MIT License
 */

package com.edugo.test.module.storage

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences

/**
 * Implementacion Desktop/JVM de Settings usando java.util.prefs.Preferences.
 * Los datos se almacenan en el registro del sistema (Windows) o archivos plist (macOS).
 */
actual fun createPlatformSettings(): Settings {
    return PreferencesSettings(
        Preferences.userRoot().node("com.edugo.storage")
    )
}

actual fun createPlatformSettings(name: String): Settings {
    return PreferencesSettings(
        Preferences.userRoot().node("com.edugo.storage.$name")
    )
}
