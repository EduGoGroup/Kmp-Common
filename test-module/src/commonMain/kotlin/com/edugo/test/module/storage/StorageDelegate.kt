/*
 * Copyright (c) 2026 EduGo Project
 * Licensed under the MIT License
 */

package com.edugo.test.module.storage

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

// =============================================================================
// DELEGATED PROPERTIES PARA TIPOS PRIMITIVOS
// =============================================================================

/**
 * Delegated property para String en storage.
 *
 * Ejemplo:
 * ```kotlin
 * class UserPrefs(storage: EduGoStorage) {
 *     var username by storage.string("user.name", "Guest")
 *     var isLoggedIn by storage.boolean("user.logged_in", false)
 * }
 * ```
 */
fun EduGoStorage.string(key: String, default: String = ""): ReadWriteProperty<Any?, String> {
    return StorageDelegate(
        get = { getString(key, default) },
        set = { putString(key, it) }
    )
}

fun EduGoStorage.stringOrNull(key: String): ReadWriteProperty<Any?, String?> {
    return StorageDelegate(
        get = { getStringOrNull(key) },
        set = { if (it != null) putString(key, it) else remove(key) }
    )
}

fun EduGoStorage.int(key: String, default: Int = 0): ReadWriteProperty<Any?, Int> {
    return StorageDelegate(
        get = { getInt(key, default) },
        set = { putInt(key, it) }
    )
}

fun EduGoStorage.intOrNull(key: String): ReadWriteProperty<Any?, Int?> {
    return StorageDelegate(
        get = { getIntOrNull(key) },
        set = { if (it != null) putInt(key, it) else remove(key) }
    )
}

fun EduGoStorage.long(key: String, default: Long = 0L): ReadWriteProperty<Any?, Long> {
    return StorageDelegate(
        get = { getLong(key, default) },
        set = { putLong(key, it) }
    )
}

fun EduGoStorage.longOrNull(key: String): ReadWriteProperty<Any?, Long?> {
    return StorageDelegate(
        get = { getLongOrNull(key) },
        set = { if (it != null) putLong(key, it) else remove(key) }
    )
}

fun EduGoStorage.boolean(key: String, default: Boolean = false): ReadWriteProperty<Any?, Boolean> {
    return StorageDelegate(
        get = { getBoolean(key, default) },
        set = { putBoolean(key, it) }
    )
}

fun EduGoStorage.booleanOrNull(key: String): ReadWriteProperty<Any?, Boolean?> {
    return StorageDelegate(
        get = { getBooleanOrNull(key) },
        set = { if (it != null) putBoolean(key, it) else remove(key) }
    )
}

fun EduGoStorage.float(key: String, default: Float = 0f): ReadWriteProperty<Any?, Float> {
    return StorageDelegate(
        get = { getFloat(key, default) },
        set = { putFloat(key, it) }
    )
}

fun EduGoStorage.floatOrNull(key: String): ReadWriteProperty<Any?, Float?> {
    return StorageDelegate(
        get = { getFloatOrNull(key) },
        set = { if (it != null) putFloat(key, it) else remove(key) }
    )
}

fun EduGoStorage.double(key: String, default: Double = 0.0): ReadWriteProperty<Any?, Double> {
    return StorageDelegate(
        get = { getDouble(key, default) },
        set = { putDouble(key, it) }
    )
}

fun EduGoStorage.doubleOrNull(key: String): ReadWriteProperty<Any?, Double?> {
    return StorageDelegate(
        get = { getDoubleOrNull(key) },
        set = { if (it != null) putDouble(key, it) else remove(key) }
    )
}

// =============================================================================
// DELEGATED PROPERTIES PARA OBJETOS @SERIALIZABLE
// =============================================================================

/**
 * Delegated property para objeto @Serializable nullable.
 *
 * Permite usar objetos serializables como propiedades delegadas, con soporte
 * para valores null. Cuando se asigna null, la key se elimina del storage.
 *
 * Ejemplo:
 * ```kotlin
 * @Serializable
 * data class UserSettings(val theme: String, val fontSize: Int)
 *
 * class Prefs(storage: EduGoStorage) {
 *     var settings: UserSettings? by storage.serializable<UserSettings>("user.settings")
 * }
 *
 * // Uso:
 * val prefs = Prefs(storage)
 * prefs.settings = UserSettings("dark", 14)  // Guarda en storage
 * println(prefs.settings?.theme)              // Lee de storage
 * prefs.settings = null                       // Elimina de storage
 * ```
 *
 * @param key Clave de almacenamiento
 * @param defaultValue Valor por defecto si no existe la key (default: null)
 * @return ReadWriteProperty delegada para el objeto nullable
 */
inline fun <reified T> EduGoStorage.serializable(
    key: String,
    defaultValue: T? = null
): ReadWriteProperty<Any?, T?> {
    return StorageDelegate(
        get = { getObject<T>(key) ?: defaultValue },
        set = { value ->
            if (value != null) {
                putObject(key, value)
            } else {
                remove(key)
            }
        }
    )
}

/**
 * Delegated property para objeto @Serializable con valor por defecto requerido.
 *
 * Similar a [serializable], pero garantiza que siempre retorna un valor no-null
 * usando un provider para generar el valor por defecto cuando no existe.
 *
 * Ejemplo:
 * ```kotlin
 * @Serializable
 * data class AppConfig(val version: Int = 1, val features: List<String> = emptyList())
 *
 * class Settings(storage: EduGoStorage) {
 *     var config: AppConfig by storage.serializableWithDefault("app.config") {
 *         AppConfig() // Valor por defecto lazy
 *     }
 * }
 *
 * // Uso:
 * val settings = Settings(storage)
 * println(settings.config.version) // Siempre retorna valor, nunca null
 * ```
 *
 * @param key Clave de almacenamiento
 * @param defaultProvider Lambda que provee el valor por defecto (evaluado lazy)
 * @return ReadWriteProperty delegada para el objeto no-null
 */
inline fun <reified T : Any> EduGoStorage.serializableWithDefault(
    key: String,
    crossinline defaultProvider: () -> T
): ReadWriteProperty<Any?, T> {
    return StorageDelegate(
        get = { getObject<T>(key) ?: defaultProvider() },
        set = { value -> putObject(key, value) }
    )
}

/**
 * Delegated property para lista de objetos @Serializable.
 *
 * Simplifica el almacenamiento de listas serializables como propiedades delegadas.
 * Siempre retorna una lista (vacía si no existe la key).
 *
 * Ejemplo:
 * ```kotlin
 * @Serializable
 * data class RecentSearch(val query: String, val timestamp: Long)
 *
 * class SearchHistory(storage: EduGoStorage) {
 *     var recentSearches: List<RecentSearch> by storage.serializableList("search.history")
 * }
 *
 * // Uso:
 * val history = SearchHistory(storage)
 * history.recentSearches = listOf(RecentSearch("kotlin", System.currentTimeMillis()))
 * history.recentSearches.forEach { println(it.query) }
 * ```
 *
 * @param key Clave de almacenamiento
 * @return ReadWriteProperty delegada para la lista
 */
inline fun <reified T> EduGoStorage.serializableList(
    key: String
): ReadWriteProperty<Any?, List<T>> {
    return StorageDelegate(
        get = { getList<T>(key) },
        set = { value -> putList(key, value) }
    )
}

// =============================================================================
// IMPLEMENTACIÓN DEL DELEGATE
// =============================================================================

/**
 * Implementación genérica de delegated property para storage.
 *
 * Esta clase es internal para permitir su uso desde funciones inline
 * dentro del mismo módulo.
 */
@PublishedApi
internal class StorageDelegate<T>(
    private val get: () -> T,
    private val set: (T) -> Unit
) : ReadWriteProperty<Any?, T> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = get()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = set(value)
}
