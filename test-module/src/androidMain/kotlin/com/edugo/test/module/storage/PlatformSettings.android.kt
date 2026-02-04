/*
 * Copyright (c) 2026 EduGo Project
 * Licensed under the MIT License
 */

package com.edugo.test.module.storage

import com.russhwolf.settings.Settings

/**
 * Implementacion Android de Settings usando SharedPreferences.
 * Usa multiplatform-settings-no-arg que auto-detecta el contexto.
 *
 * Settings() usa PreferenceManager.getDefaultSharedPreferences() internamente.
 * Para named settings, el prefijo se maneja en EduGoStorage via keyPrefix.
 */
actual fun createPlatformSettings(): Settings {
    return Settings()
}

actual fun createPlatformSettings(name: String): Settings {
    // En Android con no-arg, no podemos crear SharedPreferences con nombre custom
    // sin Context. El aislamiento se maneja via keyPrefix en EduGoStorage.
    return Settings()
}
