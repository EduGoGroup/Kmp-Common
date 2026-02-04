/*
 * Copyright (c) 2026 EduGo Project
 * Licensed under the MIT License
 */

package com.edugo.test.module.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import com.russhwolf.settings.get

/**
 * Wrapper type-safe sobre multiplatform-settings.
 * Provee API coherente para almacenar datos primitivos en todas las plataformas.
 *
 * Ejemplo:
 * ```kotlin
 * val storage = EduGoStorage.create()
 * storage.putString("user.name", "John")
 * val name = storage.getString("user.name", "Guest")
 * ```
 */
class EduGoStorage internal constructor(
    private val settings: Settings,
    private val keyPrefix: String = ""
) {

    private fun prefixedKey(key: String): String =
        if (keyPrefix.isEmpty()) key else "$keyPrefix.$key"

    // ===== STRING =====
    fun putString(key: String, value: String) {
        settings[prefixedKey(key)] = value
    }

    fun getString(key: String, default: String = ""): String {
        return settings[prefixedKey(key), default]
    }

    fun getStringOrNull(key: String): String? {
        return settings.getStringOrNull(prefixedKey(key))
    }

    // ===== INT =====
    fun putInt(key: String, value: Int) {
        settings[prefixedKey(key)] = value
    }

    fun getInt(key: String, default: Int = 0): Int {
        return settings[prefixedKey(key), default]
    }

    fun getIntOrNull(key: String): Int? {
        return settings.getIntOrNull(prefixedKey(key))
    }

    // ===== LONG =====
    fun putLong(key: String, value: Long) {
        settings[prefixedKey(key)] = value
    }

    fun getLong(key: String, default: Long = 0L): Long {
        return settings[prefixedKey(key), default]
    }

    fun getLongOrNull(key: String): Long? {
        return settings.getLongOrNull(prefixedKey(key))
    }

    // ===== BOOLEAN =====
    fun putBoolean(key: String, value: Boolean) {
        settings[prefixedKey(key)] = value
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean {
        return settings[prefixedKey(key), default]
    }

    fun getBooleanOrNull(key: String): Boolean? {
        return settings.getBooleanOrNull(prefixedKey(key))
    }

    // ===== FLOAT =====
    fun putFloat(key: String, value: Float) {
        settings[prefixedKey(key)] = value
    }

    fun getFloat(key: String, default: Float = 0f): Float {
        return settings[prefixedKey(key), default]
    }

    fun getFloatOrNull(key: String): Float? {
        return settings.getFloatOrNull(prefixedKey(key))
    }

    // ===== DOUBLE =====
    fun putDouble(key: String, value: Double) {
        settings[prefixedKey(key)] = value
    }

    fun getDouble(key: String, default: Double = 0.0): Double {
        return settings[prefixedKey(key), default]
    }

    fun getDoubleOrNull(key: String): Double? {
        return settings.getDoubleOrNull(prefixedKey(key))
    }

    // ===== OPERACIONES GENERALES =====

    /**
     * Verifica si existe una key.
     */
    fun contains(key: String): Boolean {
        return settings.hasKey(prefixedKey(key))
    }

    /**
     * Elimina un valor por key.
     */
    fun remove(key: String) {
        settings.remove(prefixedKey(key))
    }

    /**
     * Elimina todos los valores.
     */
    fun clear() {
        settings.clear()
    }

    /**
     * Obtiene todas las keys almacenadas.
     */
    fun keys(): Set<String> {
        return settings.keys.filter {
            keyPrefix.isEmpty() || it.startsWith("$keyPrefix.")
        }.map {
            if (keyPrefix.isEmpty()) it else it.removePrefix("$keyPrefix.")
        }.toSet()
    }

    companion object {
        /**
         * Crea instancia con Settings por defecto de la plataforma.
         */
        fun create(): EduGoStorage {
            return EduGoStorage(createPlatformSettings())
        }

        /**
         * Crea instancia con nombre personalizado para aislamiento.
         */
        fun create(name: String): EduGoStorage {
            return EduGoStorage(createPlatformSettings(name), name)
        }

        /**
         * Crea instancia con Settings inyectado (para testing).
         */
        fun withSettings(settings: Settings, keyPrefix: String = ""): EduGoStorage {
            return EduGoStorage(settings, keyPrefix)
        }
    }
}
