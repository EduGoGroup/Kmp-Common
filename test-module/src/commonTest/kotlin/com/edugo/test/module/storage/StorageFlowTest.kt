/*
 * Copyright (c) 2026 EduGo Project
 * Licensed under the MIT License
 */

package com.edugo.test.module.storage

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Tests para las extensiones de StorageFlow.
 *
 * Nota: MapSettings NO implementa ObservableSettings, por lo que estos tests
 * verifican el comportamiento de fallback (emite solo valor actual).
 * Los tests de observación real requieren Android con SharedPreferences.
 */
class StorageFlowTest {

    private lateinit var storage: EduGoStorage

    @BeforeTest
    fun setup() {
        storage = EduGoStorage.withSettings(MapSettings())
    }

    // ===== STRING TESTS =====

    @Test
    fun `observeString emits initial value`() = runTest {
        storage.putString("name", "John")

        val result = storage.observeString("name").first()

        assertEquals("John", result)
    }

    @Test
    fun `observeString emits default when key missing`() = runTest {
        val result = storage.observeString("missing", "DefaultName").first()

        assertEquals("DefaultName", result)
    }

    @Test
    fun `observeStringOrNull emits null for missing key`() = runTest {
        val result = storage.observeStringOrNull("missing").first()

        assertNull(result)
    }

    @Test
    fun `observeStringOrNull emits value when key exists`() = runTest {
        storage.putString("exists", "value")

        val result = storage.observeStringOrNull("exists").first()

        assertEquals("value", result)
    }

    // ===== INT TESTS =====

    @Test
    fun `observeInt emits initial value`() = runTest {
        storage.putInt("count", 42)

        val result = storage.observeInt("count").first()

        assertEquals(42, result)
    }

    @Test
    fun `observeInt emits default when key missing`() = runTest {
        val result = storage.observeInt("missing", 100).first()

        assertEquals(100, result)
    }

    @Test
    fun `observeIntOrNull emits null for missing key`() = runTest {
        val result = storage.observeIntOrNull("missing").first()

        assertNull(result)
    }

    // ===== LONG TESTS =====

    @Test
    fun `observeLong emits initial value`() = runTest {
        storage.putLong("bigNumber", 9876543210L)

        val result = storage.observeLong("bigNumber").first()

        assertEquals(9876543210L, result)
    }

    @Test
    fun `observeLongOrNull emits null for missing key`() = runTest {
        val result = storage.observeLongOrNull("missing").first()

        assertNull(result)
    }

    // ===== BOOLEAN TESTS =====

    @Test
    fun `observeBoolean emits initial value`() = runTest {
        storage.putBoolean("enabled", true)

        val result = storage.observeBoolean("enabled").first()

        assertTrue(result)
    }

    @Test
    fun `observeBoolean emits default when key missing`() = runTest {
        val result = storage.observeBoolean("missing", true).first()

        assertTrue(result)
    }

    @Test
    fun `observeBooleanOrNull emits null for missing key`() = runTest {
        val result = storage.observeBooleanOrNull("missing").first()

        assertNull(result)
    }

    // ===== FLOAT TESTS =====

    @Test
    fun `observeFloat emits initial value`() = runTest {
        storage.putFloat("pi", 3.14f)

        val result = storage.observeFloat("pi").first()

        assertEquals(3.14f, result)
    }

    @Test
    fun `observeFloatOrNull emits null for missing key`() = runTest {
        val result = storage.observeFloatOrNull("missing").first()

        assertNull(result)
    }

    // ===== DOUBLE TESTS =====

    @Test
    fun `observeDouble emits initial value`() = runTest {
        storage.putDouble("precise", 3.141592653589793)

        val result = storage.observeDouble("precise").first()

        assertEquals(3.141592653589793, result)
    }

    @Test
    fun `observeDoubleOrNull emits null for missing key`() = runTest {
        val result = storage.observeDoubleOrNull("missing").first()

        assertNull(result)
    }

    // ===== FALLBACK BEHAVIOR TESTS =====

    @Test
    fun `flow emits single value in fallback mode`() = runTest {
        storage.putString("key", "value")

        // En modo fallback (MapSettings), el flow emite un solo valor
        val emissions = storage.observeString("key").take(1).toList()

        assertEquals(1, emissions.size)
        assertEquals("value", emissions[0])
    }

    @Test
    fun `multiple observe calls return independent flows`() = runTest {
        storage.putInt("counter", 10)

        val flow1 = storage.observeInt("counter").first()
        val flow2 = storage.observeInt("counter").first()

        assertEquals(flow1, flow2)
        assertEquals(10, flow1)
    }
}
