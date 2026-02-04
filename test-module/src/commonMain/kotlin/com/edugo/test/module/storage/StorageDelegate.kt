/*
 * Copyright (c) 2026 EduGo Project
 * Licensed under the MIT License
 */

package com.edugo.test.module.storage

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

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

/**
 * Implementacion generica de delegated property para storage.
 */
private class StorageDelegate<T>(
    private val get: () -> T,
    private val set: (T) -> Unit
) : ReadWriteProperty<Any?, T> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = get()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = set(value)
}
