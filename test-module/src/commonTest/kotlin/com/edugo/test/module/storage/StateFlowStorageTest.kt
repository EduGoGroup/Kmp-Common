/*
 * Copyright (c) 2026 EduGo Project
 * Licensed under the MIT License
 */

package com.edugo.test.module.storage

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class StateFlowStorageTest {

    private lateinit var storage: EduGoStorage
    private lateinit var stateStorage: StateFlowStorage
    private lateinit var testScope: TestScope

    @BeforeTest
    fun setup() {
        testScope = TestScope()
        storage = EduGoStorage.withSettings(MapSettings())
        stateStorage = StateFlowStorage(testScope, storage)
    }

    // ===== STRING TESTS =====

    @Test
    fun `stateFlowString returns initial value from storage`() = runTest {
        storage.putString("name", "John")

        val flow = stateStorage.stateFlowString("name", "Guest")

        assertEquals("John", flow.value)
    }

    @Test
    fun `stateFlowString returns default when key missing`() = runTest {
        val flow = stateStorage.stateFlowString("missing", "Default")

        assertEquals("Default", flow.value)
    }

    @Test
    fun `putString updates StateFlow value`() = runTest {
        val flow = stateStorage.stateFlowString("name", "")

        stateStorage.putString("name", "Jane")

        assertEquals("Jane", flow.value)
    }

    @Test
    fun `putString persists to storage`() = runTest {
        stateStorage.putString("name", "Stored")

        assertEquals("Stored", storage.getString("name"))
    }

    // ===== INT TESTS =====

    @Test
    fun `stateFlowInt returns initial value from storage`() = runTest {
        storage.putInt("count", 42)

        val flow = stateStorage.stateFlowInt("count", 0)

        assertEquals(42, flow.value)
    }

    @Test
    fun `stateFlowInt returns default when key missing`() = runTest {
        val flow = stateStorage.stateFlowInt("missing", 100)

        assertEquals(100, flow.value)
    }

    @Test
    fun `putInt updates StateFlow value`() = runTest {
        val flow = stateStorage.stateFlowInt("count", 0)

        stateStorage.putInt("count", 99)

        assertEquals(99, flow.value)
    }

    // ===== LONG TESTS =====

    @Test
    fun `stateFlowLong works correctly`() = runTest {
        val flow = stateStorage.stateFlowLong("bigNumber", 0L)

        assertEquals(0L, flow.value)

        stateStorage.putLong("bigNumber", 9876543210L)

        assertEquals(9876543210L, flow.value)
    }

    // ===== BOOLEAN TESTS =====

    @Test
    fun `stateFlowBoolean works correctly`() = runTest {
        val flow = stateStorage.stateFlowBoolean("enabled", false)

        assertFalse(flow.value)

        stateStorage.putBoolean("enabled", true)

        assertTrue(flow.value)
    }

    @Test
    fun `stateFlowBoolean returns initial value from storage`() = runTest {
        storage.putBoolean("flag", true)

        val flow = stateStorage.stateFlowBoolean("flag", false)

        assertTrue(flow.value)
    }

    // ===== FLOAT TESTS =====

    @Test
    fun `stateFlowFloat works correctly`() = runTest {
        val flow = stateStorage.stateFlowFloat("pi", 0f)

        assertEquals(0f, flow.value)

        stateStorage.putFloat("pi", 3.14f)

        assertEquals(3.14f, flow.value)
    }

    // ===== DOUBLE TESTS =====

    @Test
    fun `stateFlowDouble works correctly`() = runTest {
        val flow = stateStorage.stateFlowDouble("precise", 0.0)

        assertEquals(0.0, flow.value)

        stateStorage.putDouble("precise", 3.141592653589793)

        assertEquals(3.141592653589793, flow.value)
    }

    // ===== REFRESH TESTS =====

    @Test
    fun `refresh reloads values from storage`() = runTest {
        val flow = stateStorage.stateFlowString("key", "initial")

        // Modificar storage directamente (simulando cambio externo)
        storage.putString("key", "changed externally")

        // El flow aún tiene valor viejo
        assertEquals("initial", flow.value)

        // Refresh sincroniza
        stateStorage.refresh()

        assertEquals("changed externally", flow.value)
    }

    @Test
    fun `refresh updates all tracked flows`() = runTest {
        val stringFlow = stateStorage.stateFlowString("str", "")
        val intFlow = stateStorage.stateFlowInt("num", 0)
        val boolFlow = stateStorage.stateFlowBoolean("flag", false)

        // Modificar storage directamente
        storage.putString("str", "updated")
        storage.putInt("num", 42)
        storage.putBoolean("flag", true)

        // Valores viejos
        assertEquals("", stringFlow.value)
        assertEquals(0, intFlow.value)
        assertFalse(boolFlow.value)

        // Refresh
        stateStorage.refresh()

        // Valores actualizados
        assertEquals("updated", stringFlow.value)
        assertEquals(42, intFlow.value)
        assertTrue(boolFlow.value)
    }

    // ===== CACHE TESTS =====

    @Test
    fun `same key shares underlying state`() = runTest {
        val flow1 = stateStorage.stateFlowString("key", "initial")
        val flow2 = stateStorage.stateFlowString("key", "different-default")

        // Ambos flows comparten el mismo estado subyacente
        assertEquals(flow1.value, flow2.value)

        // Cambiar uno afecta al otro
        stateStorage.putString("key", "updated")

        assertEquals("updated", flow1.value)
        assertEquals("updated", flow2.value)
    }

    @Test
    fun `different keys are independent`() = runTest {
        stateStorage.putString("key1", "value1")
        stateStorage.putString("key2", "value2")

        val flow1 = stateStorage.stateFlowString("key1", "")
        val flow2 = stateStorage.stateFlowString("key2", "")

        assertEquals("value1", flow1.value)
        assertEquals("value2", flow2.value)
    }

    @Test
    fun `different types have separate caches`() = runTest {
        // Creamos flows de diferentes tipos para keys diferentes
        val stringFlow = stateStorage.stateFlowString("stringKey", "default")
        val intFlow = stateStorage.stateFlowInt("intKey", 99)

        // Cada flow mantiene su propio valor
        assertEquals("default", stringFlow.value)
        assertEquals(99, intFlow.value)

        // Actualizar uno no afecta al otro
        stateStorage.putString("stringKey", "updated")

        assertEquals("updated", stringFlow.value)
        assertEquals(99, intFlow.value) // sin cambios
    }

    // ===== SYNC ACCESS =====

    @Test
    fun `sync property provides access to underlying storage`() = runTest {
        stateStorage.putString("key", "value")

        assertEquals("value", stateStorage.sync.getString("key"))
    }
}
