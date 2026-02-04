/*
 * Copyright (c) 2026 EduGo Project
 * Licensed under the MIT License
 */

package com.edugo.test.module.storage

import com.russhwolf.settings.Settings

/**
 * Provee la instancia de Settings especifica para cada plataforma.
 * - Android: SharedPreferences
 * - JVM/Desktop: Properties file
 * - JS: LocalStorage
 */
expect fun createPlatformSettings(): Settings

/**
 * Provee Settings con nombre personalizado para aislamiento.
 * @param name Nombre del storage (ej: "user_prefs", "cache")
 */
expect fun createPlatformSettings(name: String): Settings
