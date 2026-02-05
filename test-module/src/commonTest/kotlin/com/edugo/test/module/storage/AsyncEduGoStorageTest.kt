/*
 * Copyright (c) 2026 EduGo Project
 * Licensed under the MIT License
 */

package com.edugo.test.module.storage

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.*

/**
 * Test data class for async serialization tests.
 */
@Serializable
private data class AsyncTestUser(val id: Int, val name: String, val email: String)

@OptIn(ExperimentalCoroutinesApi::class)
class AsyncEduGoStorageTest {

    private lateinit var asyncStorage: AsyncEduGoStorage
    private lateinit var syncStorage: EduGoStorage

    @BeforeTest
    fun setup() {
        syncStorage = EduGoStorage.withSettings(MapSettings())
        // UnconfinedTestDispatcher ejecuta inmediatamente sin necesidad de advanceUntilIdle
        asyncStorage = AsyncEduGoStorage(syncStorage, UnconfinedTestDispatcher())
    }

    // ===== STRING TESTS =====

    @Test
    fun `putString and getString works asynchronously`() = runTest {
        asyncStorage.putString("name", "John")
        val result = asyncStorage.getString("name")

        assertEquals("John", result)
    }

    @Test
    fun `getString returns default when key missing`() = runTest {
        val result = asyncStorage.getString("missing", "DefaultValue")

        assertEquals("DefaultValue", result)
    }

    @Test
    fun `getStringOrNull returns null for missing key`() = runTest {
        val result = asyncStorage.getStringOrNull("missing")

        assertNull(result)
    }

    @Test
    fun `getStringOrNull returns value when key exists`() = runTest {
        asyncStorage.putString("exists", "value")
        val result = asyncStorage.getStringOrNull("exists")

        assertEquals("value", result)
    }

    // ===== INT TESTS =====

    @Test
    fun `putInt and getInt works asynchronously`() = runTest {
        asyncStorage.putInt("count", 42)
        val result = asyncStorage.getInt("count")

        assertEquals(42, result)
    }

    @Test
    fun `getInt returns default when key missing`() = runTest {
        val result = asyncStorage.getInt("missing", 100)

        assertEquals(100, result)
    }

    @Test
    fun `getIntOrNull returns null for missing key`() = runTest {
        val result = asyncStorage.getIntOrNull("missing")

        assertNull(result)
    }

    // ===== LONG TESTS =====

    @Test
    fun `putLong and getLong works asynchronously`() = runTest {
        asyncStorage.putLong("bigNumber", 9876543210L)
        val result = asyncStorage.getLong("bigNumber")

        assertEquals(9876543210L, result)
    }

    @Test
    fun `getLong returns default when key missing`() = runTest {
        val result = asyncStorage.getLong("missing", 999L)

        assertEquals(999L, result)
    }

    // ===== BOOLEAN TESTS =====

    @Test
    fun `putBoolean and getBoolean works asynchronously`() = runTest {
        asyncStorage.putBoolean("enabled", true)
        val result = asyncStorage.getBoolean("enabled")

        assertTrue(result)
    }

    @Test
    fun `getBoolean returns default when key missing`() = runTest {
        val result = asyncStorage.getBoolean("missing", true)

        assertTrue(result)
    }

    @Test
    fun `getBoolean returns false by default`() = runTest {
        val result = asyncStorage.getBoolean("missing")

        assertFalse(result)
    }

    // ===== FLOAT TESTS =====

    @Test
    fun `putFloat and getFloat works asynchronously`() = runTest {
        asyncStorage.putFloat("pi", 3.14f)
        val result = asyncStorage.getFloat("pi")

        assertEquals(3.14f, result)
    }

    // ===== DOUBLE TESTS =====

    @Test
    fun `putDouble and getDouble works asynchronously`() = runTest {
        asyncStorage.putDouble("precise", 3.141592653589793)
        val result = asyncStorage.getDouble("precise")

        assertEquals(3.141592653589793, result)
    }

    // ===== GENERAL OPERATIONS =====

    @Test
    fun `contains returns true for existing key`() = runTest {
        asyncStorage.putString("exists", "value")

        assertTrue(asyncStorage.contains("exists"))
    }

    @Test
    fun `contains returns false for missing key`() = runTest {
        assertFalse(asyncStorage.contains("missing"))
    }

    @Test
    fun `remove deletes key`() = runTest {
        asyncStorage.putString("toRemove", "value")
        assertTrue(asyncStorage.contains("toRemove"))

        asyncStorage.remove("toRemove")

        assertFalse(asyncStorage.contains("toRemove"))
    }

    @Test
    fun `clear removes all keys`() = runTest {
        asyncStorage.putString("key1", "value1")
        asyncStorage.putInt("key2", 42)
        asyncStorage.putBoolean("key3", true)

        asyncStorage.clear()

        assertFalse(asyncStorage.contains("key1"))
        assertFalse(asyncStorage.contains("key2"))
        assertFalse(asyncStorage.contains("key3"))
    }

    @Test
    fun `keys returns all stored keys`() = runTest {
        asyncStorage.putString("name", "John")
        asyncStorage.putInt("age", 30)

        val keys = asyncStorage.keys()

        assertTrue(keys.contains("name"))
        assertTrue(keys.contains("age"))
        assertEquals(2, keys.size)
    }

    // ===== SYNC ACCESS =====

    @Test
    fun `sync property provides access to synchronous storage`() = runTest {
        asyncStorage.putString("key", "value")

        // Acceso síncrono directo
        assertEquals("value", asyncStorage.sync.getString("key"))
    }

    @Test
    fun `sync modifications are visible in async`() = runTest {
        // Modificar via sync
        asyncStorage.sync.putString("syncKey", "syncValue")

        // Leer via async
        val result = asyncStorage.getString("syncKey")

        assertEquals("syncValue", result)
    }

    // ===== FACTORY METHODS =====

    @Test
    fun `wrap creates async wrapper from existing storage`() = runTest {
        val syncStorage = EduGoStorage.withSettings(MapSettings())
        syncStorage.putString("existing", "data")

        val wrapped = AsyncEduGoStorage.wrap(syncStorage)
        val result = wrapped.getString("existing")

        assertEquals("data", result)
    }

    @Test
    fun `wrap with dispatcher uses custom dispatcher`() = runTest {
        val testStorage = EduGoStorage.withSettings(MapSettings())
        val customDispatcher = UnconfinedTestDispatcher()

        val wrapped = AsyncEduGoStorage.wrap(testStorage, customDispatcher)
        wrapped.putString("test", "value")

        assertEquals("value", wrapped.getString("test"))
    }

    // ===== SERIALIZATION EXTENSIONS TESTS =====

    @Test
    fun `putObject and getObject works with serializable data class`() = runTest {
        val user = AsyncTestUser(id = 1, name = "John", email = "john@example.com")

        asyncStorage.putObject("user", user)
        val result: AsyncTestUser? = asyncStorage.getObject("user")

        assertEquals(user, result)
    }

    @Test
    fun `getObject returns null for missing key`() = runTest {
        val result: AsyncTestUser? = asyncStorage.getObject("missingUser")

        assertNull(result)
    }

    @Test
    fun `getObject with default returns default when key missing`() = runTest {
        val default = AsyncTestUser(id = 0, name = "Guest", email = "")

        val result: AsyncTestUser = asyncStorage.getObject("missingUser", default)

        assertEquals(default, result)
    }

    @Test
    fun `putList and getList works with serializable objects`() = runTest {
        val users = listOf(
            AsyncTestUser(1, "John", "john@example.com"),
            AsyncTestUser(2, "Jane", "jane@example.com")
        )

        asyncStorage.putList("users", users)
        val result: List<AsyncTestUser> = asyncStorage.getList("users")

        assertEquals(users, result)
    }

    @Test
    fun `addToList appends element to existing list`() = runTest {
        val initialUsers = listOf(AsyncTestUser(1, "John", "john@example.com"))
        asyncStorage.putList("users", initialUsers)

        val newUser = AsyncTestUser(2, "Jane", "jane@example.com")
        asyncStorage.addToList("users", newUser)

        val result: List<AsyncTestUser> = asyncStorage.getList("users")
        assertEquals(2, result.size)
        assertEquals(newUser, result[1])
    }

    @Test
    fun `putSet and getSet works with serializable objects`() = runTest {
        val tags = setOf("kotlin", "multiplatform", "coroutines")

        asyncStorage.putSet("tags", tags)
        val result: Set<String> = asyncStorage.getSet("tags")

        assertEquals(tags, result)
    }

    @Test
    fun `putMap and getMap works with string keys`() = runTest {
        val config = mapOf(
            "theme" to "dark",
            "language" to "es",
            "notifications" to "enabled"
        )

        asyncStorage.putMap("config", config)
        val result: Map<String, String> = asyncStorage.getMap("config")

        assertEquals(config, result)
    }
}
