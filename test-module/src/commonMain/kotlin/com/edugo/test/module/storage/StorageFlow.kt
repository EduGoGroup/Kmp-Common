/*
 * Copyright (c) 2026 EduGo Project
 * Licensed under the MIT License
 */

package com.edugo.test.module.storage

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getStringFlow
import com.russhwolf.settings.coroutines.getIntFlow
import com.russhwolf.settings.coroutines.getLongFlow
import com.russhwolf.settings.coroutines.getBooleanFlow
import com.russhwolf.settings.coroutines.getFloatFlow
import com.russhwolf.settings.coroutines.getDoubleFlow
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import com.russhwolf.settings.coroutines.getIntOrNullFlow
import com.russhwolf.settings.coroutines.getLongOrNullFlow
import com.russhwolf.settings.coroutines.getBooleanOrNullFlow
import com.russhwolf.settings.coroutines.getFloatOrNullFlow
import com.russhwolf.settings.coroutines.getDoubleOrNullFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Extensiones para obtener Flow de valores del storage.
 * Emite el valor actual y cada cambio posterior.
 *
 * ## Soporte por plataforma
 * - **Android:** Observación completa via SharedPreferences (implementa ObservableSettings)
 * - **Desktop/JS:** Fallback que emite solo el valor actual (sin observación de cambios)
 *
 * ## Uso
 * ```kotlin
 * val storage = EduGoStorage.create()
 *
 * // Observar cambios en un valor
 * storage.observeString("user.name", "Guest")
 *     .collect { name -> println("Name changed: $name") }
 *
 * // Observar valor nullable
 * storage.observeStringOrNull("optional.key")
 *     .collect { value -> println("Value: $value") }
 * ```
 */

// =============================================================================
// STRING
// =============================================================================

/**
 * Observa un valor String.
 * Emite el valor actual inmediatamente y luego cada cambio.
 *
 * En plataformas sin soporte de observación, emite solo el valor actual.
 *
 * @param key Clave a observar
 * @param default Valor por defecto si la clave no existe
 * @return Flow que emite valores cuando cambian
 */
@OptIn(ExperimentalSettingsApi::class)
fun EduGoStorage.observeString(
    key: String,
    default: String = ""
): Flow<String> {
    val observable = getObservableSettings()
    return if (observable != null) {
        observable.getStringFlow(key, default)
    } else {
        flowOf(getString(key, default))
    }
}

/**
 * Observa un valor String nullable.
 * Emite null si la clave no existe.
 */
@OptIn(ExperimentalSettingsApi::class)
fun EduGoStorage.observeStringOrNull(key: String): Flow<String?> {
    val observable = getObservableSettings()
    return if (observable != null) {
        observable.getStringOrNullFlow(key)
    } else {
        flowOf(getStringOrNull(key))
    }
}

// =============================================================================
// INT
// =============================================================================

/**
 * Observa un valor Int.
 * En plataformas sin soporte de observación, emite solo el valor actual.
 */
@OptIn(ExperimentalSettingsApi::class)
fun EduGoStorage.observeInt(
    key: String,
    default: Int = 0
): Flow<Int> {
    val observable = getObservableSettings()
    return if (observable != null) {
        observable.getIntFlow(key, default)
    } else {
        flowOf(getInt(key, default))
    }
}

/**
 * Observa un valor Int nullable.
 */
@OptIn(ExperimentalSettingsApi::class)
fun EduGoStorage.observeIntOrNull(key: String): Flow<Int?> {
    val observable = getObservableSettings()
    return if (observable != null) {
        observable.getIntOrNullFlow(key)
    } else {
        flowOf(getIntOrNull(key))
    }
}

// =============================================================================
// LONG
// =============================================================================

/**
 * Observa un valor Long.
 * En plataformas sin soporte de observación, emite solo el valor actual.
 */
@OptIn(ExperimentalSettingsApi::class)
fun EduGoStorage.observeLong(
    key: String,
    default: Long = 0L
): Flow<Long> {
    val observable = getObservableSettings()
    return if (observable != null) {
        observable.getLongFlow(key, default)
    } else {
        flowOf(getLong(key, default))
    }
}

/**
 * Observa un valor Long nullable.
 */
@OptIn(ExperimentalSettingsApi::class)
fun EduGoStorage.observeLongOrNull(key: String): Flow<Long?> {
    val observable = getObservableSettings()
    return if (observable != null) {
        observable.getLongOrNullFlow(key)
    } else {
        flowOf(getLongOrNull(key))
    }
}

// =============================================================================
// BOOLEAN
// =============================================================================

/**
 * Observa un valor Boolean.
 * En plataformas sin soporte de observación, emite solo el valor actual.
 */
@OptIn(ExperimentalSettingsApi::class)
fun EduGoStorage.observeBoolean(
    key: String,
    default: Boolean = false
): Flow<Boolean> {
    val observable = getObservableSettings()
    return if (observable != null) {
        observable.getBooleanFlow(key, default)
    } else {
        flowOf(getBoolean(key, default))
    }
}

/**
 * Observa un valor Boolean nullable.
 */
@OptIn(ExperimentalSettingsApi::class)
fun EduGoStorage.observeBooleanOrNull(key: String): Flow<Boolean?> {
    val observable = getObservableSettings()
    return if (observable != null) {
        observable.getBooleanOrNullFlow(key)
    } else {
        flowOf(getBooleanOrNull(key))
    }
}

// =============================================================================
// FLOAT
// =============================================================================

/**
 * Observa un valor Float.
 * En plataformas sin soporte de observación, emite solo el valor actual.
 */
@OptIn(ExperimentalSettingsApi::class)
fun EduGoStorage.observeFloat(
    key: String,
    default: Float = 0f
): Flow<Float> {
    val observable = getObservableSettings()
    return if (observable != null) {
        observable.getFloatFlow(key, default)
    } else {
        flowOf(getFloat(key, default))
    }
}

/**
 * Observa un valor Float nullable.
 */
@OptIn(ExperimentalSettingsApi::class)
fun EduGoStorage.observeFloatOrNull(key: String): Flow<Float?> {
    val observable = getObservableSettings()
    return if (observable != null) {
        observable.getFloatOrNullFlow(key)
    } else {
        flowOf(getFloatOrNull(key))
    }
}

// =============================================================================
// DOUBLE
// =============================================================================

/**
 * Observa un valor Double.
 * En plataformas sin soporte de observación, emite solo el valor actual.
 */
@OptIn(ExperimentalSettingsApi::class)
fun EduGoStorage.observeDouble(
    key: String,
    default: Double = 0.0
): Flow<Double> {
    val observable = getObservableSettings()
    return if (observable != null) {
        observable.getDoubleFlow(key, default)
    } else {
        flowOf(getDouble(key, default))
    }
}

/**
 * Observa un valor Double nullable.
 */
@OptIn(ExperimentalSettingsApi::class)
fun EduGoStorage.observeDoubleOrNull(key: String): Flow<Double?> {
    val observable = getObservableSettings()
    return if (observable != null) {
        observable.getDoubleOrNullFlow(key)
    } else {
        flowOf(getDoubleOrNull(key))
    }
}

// =============================================================================
// INTERNAL HELPER
// =============================================================================

/**
 * Intenta obtener ObservableSettings del storage.
 * Retorna null si no está disponible en la plataforma.
 */
@ExperimentalSettingsApi
internal fun EduGoStorage.getObservableSettings(): ObservableSettings? {
    return internalSettings as? ObservableSettings
}
