/*
 * Copyright (c) 2026 EduGo Project
 * Licensed under the MIT License
 */

package com.edugo.test.module.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings

/**
 * Implementacion JavaScript de Settings usando LocalStorage.
 * LocalStorage no soporta namespaces nativos, el aislamiento
 * se maneja via keyPrefix en EduGoStorage.
 */
actual fun createPlatformSettings(): Settings {
    return StorageSettings()
}

actual fun createPlatformSettings(name: String): Settings {
    // JS LocalStorage no soporta namespaces nativamente,
    // el aislamiento se maneja via keyPrefix en EduGoStorage
    return StorageSettings()
}
