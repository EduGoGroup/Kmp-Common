/*
 * Copyright (c) 2026 EduGo Project
 * Licensed under the MIT License
 */

package com.edugo.test.module.storage

import com.russhwolf.settings.MapSettings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EduGoStorageTest {

    private lateinit var storage: EduGoStorage

    @BeforeTest
    fun setup() {
        // Usar MapSettings para tests (in-memory)
        storage = EduGoStorage.withSettings(MapSettings())
    }

    // ===== STRING TESTS =====
    @Test
    fun putString_and_getString_works() {
        storage.putString("name", "John")
        assertEquals("John", storage.getString("name"))
    }

    @Test
    fun getString_returns_default_when_key_not_found() {
        assertEquals("Guest", storage.getString("missing", "Guest"))
    }

    @Test
    fun getStringOrNull_returns_null_when_key_not_found() {
        assertNull(storage.getStringOrNull("missing"))
    }

    @Test
    fun getStringOrNull_returns_value_when_key_exists() {
        storage.putString("name", "John")
        assertEquals("John", storage.getStringOrNull("name"))
    }

    // ===== INT TESTS =====
    @Test
    fun putInt_and_getInt_works() {
        storage.putInt("age", 25)
        assertEquals(25, storage.getInt("age"))
    }

    @Test
    fun getInt_returns_default_when_key_not_found() {
        assertEquals(0, storage.getInt("missing"))
        assertEquals(42, storage.getInt("missing", 42))
    }

    @Test
    fun getIntOrNull_returns_null_when_key_not_found() {
        assertNull(storage.getIntOrNull("missing"))
    }

    // ===== LONG TESTS =====
    @Test
    fun putLong_and_getLong_works() {
        storage.putLong("timestamp", 1234567890123L)
        assertEquals(1234567890123L, storage.getLong("timestamp"))
    }

    @Test
    fun getLong_returns_default_when_key_not_found() {
        assertEquals(0L, storage.getLong("missing"))
        assertEquals(999L, storage.getLong("missing", 999L))
    }

    @Test
    fun getLongOrNull_returns_null_when_key_not_found() {
        assertNull(storage.getLongOrNull("missing"))
    }

    // ===== BOOLEAN TESTS =====
    @Test
    fun putBoolean_and_getBoolean_works() {
        storage.putBoolean("active", true)
        assertTrue(storage.getBoolean("active"))
    }

    @Test
    fun getBoolean_returns_false_by_default() {
        assertFalse(storage.getBoolean("missing"))
    }

    @Test
    fun getBoolean_returns_custom_default() {
        assertTrue(storage.getBoolean("missing", true))
    }

    @Test
    fun getBooleanOrNull_returns_null_when_key_not_found() {
        assertNull(storage.getBooleanOrNull("missing"))
    }

    // ===== FLOAT TESTS =====
    @Test
    fun putFloat_and_getFloat_works() {
        storage.putFloat("rate", 3.14f)
        assertEquals(3.14f, storage.getFloat("rate"), 0.001f)
    }

    @Test
    fun getFloat_returns_default_when_key_not_found() {
        assertEquals(0f, storage.getFloat("missing"), 0.001f)
        assertEquals(1.5f, storage.getFloat("missing", 1.5f), 0.001f)
    }

    @Test
    fun getFloatOrNull_returns_null_when_key_not_found() {
        assertNull(storage.getFloatOrNull("missing"))
    }

    // ===== DOUBLE TESTS =====
    @Test
    fun putDouble_and_getDouble_works() {
        storage.putDouble("precise", 3.141592653589793)
        assertEquals(3.141592653589793, storage.getDouble("precise"), 0.0000001)
    }

    @Test
    fun getDouble_returns_default_when_key_not_found() {
        assertEquals(0.0, storage.getDouble("missing"), 0.0000001)
        assertEquals(2.718, storage.getDouble("missing", 2.718), 0.0000001)
    }

    @Test
    fun getDoubleOrNull_returns_null_when_key_not_found() {
        assertNull(storage.getDoubleOrNull("missing"))
    }

    // ===== OPERATIONS TESTS =====
    @Test
    fun contains_returns_true_for_existing_key() {
        storage.putString("exists", "value")
        assertTrue(storage.contains("exists"))
    }

    @Test
    fun contains_returns_false_for_missing_key() {
        assertFalse(storage.contains("missing"))
    }

    @Test
    fun remove_deletes_key() {
        storage.putString("toRemove", "value")
        storage.remove("toRemove")
        assertFalse(storage.contains("toRemove"))
    }

    @Test
    fun clear_removes_all_keys() {
        storage.putString("key1", "value1")
        storage.putInt("key2", 42)
        storage.clear()
        assertFalse(storage.contains("key1"))
        assertFalse(storage.contains("key2"))
    }

    @Test
    fun keys_returns_all_stored_keys() {
        storage.putString("a", "1")
        storage.putString("b", "2")
        val keys = storage.keys()
        assertTrue(keys.contains("a"))
        assertTrue(keys.contains("b"))
    }

    // ===== KEY PREFIX TESTS =====
    @Test
    fun keyPrefix_isolates_storage() {
        val settings = MapSettings()
        val storage1 = EduGoStorage.withSettings(settings, "user")
        val storage2 = EduGoStorage.withSettings(settings, "cache")

        storage1.putString("name", "John")
        storage2.putString("name", "CachedData")

        assertEquals("John", storage1.getString("name"))
        assertEquals("CachedData", storage2.getString("name"))
    }

    @Test
    fun keys_filters_by_prefix() {
        val settings = MapSettings()
        val storage1 = EduGoStorage.withSettings(settings, "user")
        val storage2 = EduGoStorage.withSettings(settings, "cache")

        storage1.putString("name", "John")
        storage1.putInt("age", 25)
        storage2.putString("token", "abc123")

        val userKeys = storage1.keys()
        assertEquals(2, userKeys.size)
        assertTrue(userKeys.contains("name"))
        assertTrue(userKeys.contains("age"))
        assertFalse(userKeys.contains("token"))
    }
}
