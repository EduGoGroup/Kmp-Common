/*
 * Copyright (c) 2026 EduGo Project
 * Licensed under the MIT License
 */

package com.edugo.test.module.storage

import com.edugo.test.module.core.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Wrapper asíncrono sobre EduGoStorage.
 * Ejecuta operaciones I/O en el dispatcher configurado para evitar bloquear el thread principal.
 *
 * Por defecto usa [Dispatchers.Default] que es multiplataforma.
 * En Android/JVM puedes pasar Dispatchers.IO para operaciones de I/O.
 *
 * Ejemplo:
 * ```kotlin
 * val asyncStorage = AsyncEduGoStorage.create()
 *
 * // Uso en coroutine
 * launch {
 *     asyncStorage.putString("user.name", "John")
 *     val name = asyncStorage.getString("user.name", "Guest")
 * }
 * ```
 */
class AsyncEduGoStorage(
    private val storage: EduGoStorage,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    // ===== STRING =====

    suspend fun putString(key: String, value: String): Unit = withContext(dispatcher) {
        storage.putString(key, value)
    }

    suspend fun getString(key: String, default: String = ""): String = withContext(dispatcher) {
        storage.getString(key, default)
    }

    suspend fun getStringOrNull(key: String): String? = withContext(dispatcher) {
        storage.getStringOrNull(key)
    }

    // ===== INT =====

    suspend fun putInt(key: String, value: Int): Unit = withContext(dispatcher) {
        storage.putInt(key, value)
    }

    suspend fun getInt(key: String, default: Int = 0): Int = withContext(dispatcher) {
        storage.getInt(key, default)
    }

    suspend fun getIntOrNull(key: String): Int? = withContext(dispatcher) {
        storage.getIntOrNull(key)
    }

    // ===== LONG =====

    suspend fun putLong(key: String, value: Long): Unit = withContext(dispatcher) {
        storage.putLong(key, value)
    }

    suspend fun getLong(key: String, default: Long = 0L): Long = withContext(dispatcher) {
        storage.getLong(key, default)
    }

    suspend fun getLongOrNull(key: String): Long? = withContext(dispatcher) {
        storage.getLongOrNull(key)
    }

    // ===== BOOLEAN =====

    suspend fun putBoolean(key: String, value: Boolean): Unit = withContext(dispatcher) {
        storage.putBoolean(key, value)
    }

    suspend fun getBoolean(key: String, default: Boolean = false): Boolean = withContext(dispatcher) {
        storage.getBoolean(key, default)
    }

    suspend fun getBooleanOrNull(key: String): Boolean? = withContext(dispatcher) {
        storage.getBooleanOrNull(key)
    }

    // ===== FLOAT =====

    suspend fun putFloat(key: String, value: Float): Unit = withContext(dispatcher) {
        storage.putFloat(key, value)
    }

    suspend fun getFloat(key: String, default: Float = 0f): Float = withContext(dispatcher) {
        storage.getFloat(key, default)
    }

    suspend fun getFloatOrNull(key: String): Float? = withContext(dispatcher) {
        storage.getFloatOrNull(key)
    }

    // ===== DOUBLE =====

    suspend fun putDouble(key: String, value: Double): Unit = withContext(dispatcher) {
        storage.putDouble(key, value)
    }

    suspend fun getDouble(key: String, default: Double = 0.0): Double = withContext(dispatcher) {
        storage.getDouble(key, default)
    }

    suspend fun getDoubleOrNull(key: String): Double? = withContext(dispatcher) {
        storage.getDoubleOrNull(key)
    }

    // ===== OPERACIONES GENERALES =====

    suspend fun contains(key: String): Boolean = withContext(dispatcher) {
        storage.contains(key)
    }

    suspend fun remove(key: String): Unit = withContext(dispatcher) {
        storage.remove(key)
    }

    suspend fun clear(): Unit = withContext(dispatcher) {
        storage.clear()
    }

    suspend fun keys(): Set<String> = withContext(dispatcher) {
        storage.keys()
    }

    /**
     * Acceso al storage síncrono subyacente.
     * Útil cuando necesitas acceso directo sin suspender.
     */
    val sync: EduGoStorage get() = storage

    companion object {
        /**
         * Crea instancia con Settings por defecto de la plataforma.
         */
        fun create(): AsyncEduGoStorage {
            return AsyncEduGoStorage(EduGoStorage.create())
        }

        /**
         * Crea instancia con nombre personalizado para aislamiento.
         */
        fun create(name: String): AsyncEduGoStorage {
            return AsyncEduGoStorage(EduGoStorage.create(name))
        }

        /**
         * Envuelve un EduGoStorage existente en wrapper asíncrono.
         */
        fun wrap(storage: EduGoStorage): AsyncEduGoStorage {
            return AsyncEduGoStorage(storage)
        }

        /**
         * Envuelve un EduGoStorage con dispatcher personalizado.
         */
        fun wrap(storage: EduGoStorage, dispatcher: CoroutineDispatcher): AsyncEduGoStorage {
            return AsyncEduGoStorage(storage, dispatcher)
        }
    }
}
