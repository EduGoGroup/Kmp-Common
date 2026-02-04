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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Extensiones para obtener Flow de valores del storage.
 * Emite el valor actual y cada cambio posterior.
 *
 * NOTA: Requiere que Settings sea ObservableSettings.
 * En Android funciona con SharedPreferences (implementa ObservableSettings).
 * En Desktop/JS usa fallback que emite solo el valor actual.
 *
 * ## Uso
 * ```kotlin
 * val storage = EduGoStorage.create()
 *
 * // Observar cambios en un valor
 * storage.observeString("user.name", "Guest")
 *     .collect { name -> println("Name changed: $name") }
 * ```
 */
@OptIn(ExperimentalSettingsApi::class)
object StorageFlow {

    /**
     * Observa un valor String.
     * Emite el valor actual inmediatamente y luego cada cambio.
     *
     * @param key Clave a observar
     * @param default Valor por defecto si la clave no existe
     * @return Flow que emite valores cuando cambian
     */
    fun EduGoStorage.observeString(
        key: String,
        default: String = ""
    ): Flow<String> {
        val observable = getObservableSettings()
        return if (observable != null) {
            observable.getStringFlow(key, default)
        } else {
            // Fallback: emitir valor actual (sin observación real)
            flowOf(getString(key, default))
        }
    }

    /**
     * Observa un valor Int.
     */
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
     * Observa un valor Long.
     */
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
     * Observa un valor Boolean.
     */
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
     * Observa un valor Float.
     */
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
     * Observa un valor Double.
     */
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
}

/**
 * Intenta obtener ObservableSettings del storage.
 * Retorna null si no está disponible en la plataforma.
 */
@ExperimentalSettingsApi
internal fun EduGoStorage.getObservableSettings(): ObservableSettings? {
    return internalSettings as? ObservableSettings
}
