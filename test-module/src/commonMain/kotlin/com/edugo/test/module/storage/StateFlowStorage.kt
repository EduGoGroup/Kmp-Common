/*
 * Copyright (c) 2026 EduGo Project
 * Licensed under the MIT License
 */

package com.edugo.test.module.storage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Storage con StateFlow para valores que necesitan ser observados reactivamente.
 * Mantiene cache in-memory sincronizado con el storage persistente.
 *
 * Ideal para valores de configuración que se leen frecuentemente desde la UI.
 *
 * ## Uso
 * ```kotlin
 * class UserPrefsManager(scope: CoroutineScope, storage: EduGoStorage) {
 *     private val stateStorage = StateFlowStorage(scope, storage)
 *
 *     val userName: StateFlow<String> = stateStorage.stateFlowString("user.name", "Guest")
 *     val isDarkMode: StateFlow<Boolean> = stateStorage.stateFlowBoolean("ui.dark_mode", false)
 *
 *     suspend fun setUserName(name: String) {
 *         stateStorage.putString("user.name", name)
 *     }
 * }
 *
 * // En Compose:
 * @Composable
 * fun UserScreen(prefs: UserPrefsManager) {
 *     val userName by prefs.userName.collectAsState()
 *     Text("Hello, $userName")
 * }
 * ```
 *
 * @param scope CoroutineScope para el ciclo de vida de los flows.
 *              Actualmente no se usa, pero está reservado para observación
 *              automática de cambios del storage en versiones futuras
 *              (integrando con StorageFlow), eliminando la necesidad de
 *              llamar refresh() manualmente.
 * @param storage Instancia de EduGoStorage a observar
 *
 * ## Thread-Safety
 * Esta clase está diseñada para uso desde un único thread (típicamente el UI thread).
 * Aunque MutableStateFlow es thread-safe, el acceso concurrente a los mapas internos
 * desde múltiples threads puede causar comportamiento indefinido. Para uso multi-thread,
 * sincroniza externamente o usa desde un único Dispatcher (ej. Dispatchers.Main).
 */
class StateFlowStorage(
    private val scope: CoroutineScope,
    private val storage: EduGoStorage
) {
    private val stringFlows = mutableMapOf<String, MutableStateFlow<String>>()
    private val intFlows = mutableMapOf<String, MutableStateFlow<Int>>()
    private val longFlows = mutableMapOf<String, MutableStateFlow<Long>>()
    private val booleanFlows = mutableMapOf<String, MutableStateFlow<Boolean>>()
    private val floatFlows = mutableMapOf<String, MutableStateFlow<Float>>()
    private val doubleFlows = mutableMapOf<String, MutableStateFlow<Double>>()

    // ===== STRING =====

    /**
     * Obtiene StateFlow para un String con valor inicial desde storage.
     * Si la key no existe, usa el valor default.
     */
    fun stateFlowString(key: String, default: String = ""): StateFlow<String> {
        return stringFlows.getOrPut(key) {
            MutableStateFlow(storage.getString(key, default))
        }.asStateFlow()
    }

    /**
     * Actualiza String y notifica al StateFlow.
     */
    suspend fun putString(key: String, value: String) {
        storage.putString(key, value)
        stringFlows[key]?.value = value
    }

    // ===== INT =====

    /**
     * Obtiene StateFlow para un Int.
     */
    fun stateFlowInt(key: String, default: Int = 0): StateFlow<Int> {
        return intFlows.getOrPut(key) {
            MutableStateFlow(storage.getInt(key, default))
        }.asStateFlow()
    }

    /**
     * Actualiza Int y notifica al StateFlow.
     */
    suspend fun putInt(key: String, value: Int) {
        storage.putInt(key, value)
        intFlows[key]?.value = value
    }

    // ===== LONG =====

    /**
     * Obtiene StateFlow para un Long.
     */
    fun stateFlowLong(key: String, default: Long = 0L): StateFlow<Long> {
        return longFlows.getOrPut(key) {
            MutableStateFlow(storage.getLong(key, default))
        }.asStateFlow()
    }

    /**
     * Actualiza Long y notifica al StateFlow.
     */
    suspend fun putLong(key: String, value: Long) {
        storage.putLong(key, value)
        longFlows[key]?.value = value
    }

    // ===== BOOLEAN =====

    /**
     * Obtiene StateFlow para un Boolean.
     */
    fun stateFlowBoolean(key: String, default: Boolean = false): StateFlow<Boolean> {
        return booleanFlows.getOrPut(key) {
            MutableStateFlow(storage.getBoolean(key, default))
        }.asStateFlow()
    }

    /**
     * Actualiza Boolean y notifica al StateFlow.
     */
    suspend fun putBoolean(key: String, value: Boolean) {
        storage.putBoolean(key, value)
        booleanFlows[key]?.value = value
    }

    // ===== FLOAT =====

    /**
     * Obtiene StateFlow para un Float.
     */
    fun stateFlowFloat(key: String, default: Float = 0f): StateFlow<Float> {
        return floatFlows.getOrPut(key) {
            MutableStateFlow(storage.getFloat(key, default))
        }.asStateFlow()
    }

    /**
     * Actualiza Float y notifica al StateFlow.
     */
    suspend fun putFloat(key: String, value: Float) {
        storage.putFloat(key, value)
        floatFlows[key]?.value = value
    }

    // ===== DOUBLE =====

    /**
     * Obtiene StateFlow para un Double.
     */
    fun stateFlowDouble(key: String, default: Double = 0.0): StateFlow<Double> {
        return doubleFlows.getOrPut(key) {
            MutableStateFlow(storage.getDouble(key, default))
        }.asStateFlow()
    }

    /**
     * Actualiza Double y notifica al StateFlow.
     */
    suspend fun putDouble(key: String, value: Double) {
        storage.putDouble(key, value)
        doubleFlows[key]?.value = value
    }

    // ===== OPERACIONES GENERALES =====

    /**
     * Refresca todos los StateFlows desde storage.
     * Útil cuando el storage fue modificado externamente.
     *
     * NOTA: En versiones futuras, este método podría volverse obsoleto
     * si se implementa observación automática usando el scope y StorageFlow.
     */
    fun refresh() {
        stringFlows.forEach { (key, flow) ->
            flow.value = storage.getString(key, flow.value)
        }
        intFlows.forEach { (key, flow) ->
            flow.value = storage.getInt(key, flow.value)
        }
        longFlows.forEach { (key, flow) ->
            flow.value = storage.getLong(key, flow.value)
        }
        booleanFlows.forEach { (key, flow) ->
            flow.value = storage.getBoolean(key, flow.value)
        }
        floatFlows.forEach { (key, flow) ->
            flow.value = storage.getFloat(key, flow.value)
        }
        doubleFlows.forEach { (key, flow) ->
            flow.value = storage.getDouble(key, flow.value)
        }
    }

    /**
     * Acceso al storage síncrono subyacente.
     */
    val sync: EduGoStorage get() = storage
}
