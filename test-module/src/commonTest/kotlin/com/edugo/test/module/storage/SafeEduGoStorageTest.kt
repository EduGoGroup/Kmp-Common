package com.edugo.test.module.storage

import com.edugo.test.module.core.Result
import com.russhwolf.settings.MapSettings
import kotlinx.serialization.Serializable
import kotlin.test.*

@Serializable
data class SafeTestConfig(val theme: String, val fontSize: Int)

@Serializable
data class SafeTestUser(val id: Int, val name: String)

class SafeEduGoStorageTest {

    private lateinit var safeStorage: SafeEduGoStorage

    @BeforeTest
    fun setup() {
        val storage = EduGoStorage.withSettings(MapSettings())
        safeStorage = SafeEduGoStorage.wrap(storage)
    }

    // =========================================================================
    // STRING OPERATIONS - VALID KEYS
    // =========================================================================

    @Test
    fun `putStringSafe and getStringSafe work for valid key`() {
        val result = safeStorage.putStringSafe("valid.key", "value")
        assertTrue(result is Result.Success)

        val value = safeStorage.getStringSafe("valid.key", "default")
        assertEquals("value", value)
    }

    @Test
    fun `getStringSafe returns default when key not found`() {
        val value = safeStorage.getStringSafe("nonexistent.key", "default")
        assertEquals("default", value)
    }

    // =========================================================================
    // INT OPERATIONS - VALID KEYS
    // =========================================================================

    @Test
    fun `putIntSafe and getIntSafe work for valid key`() {
        val result = safeStorage.putIntSafe("count", 42)
        assertTrue(result is Result.Success)

        assertEquals(42, safeStorage.getIntSafe("count"))
    }

    @Test
    fun `getIntSafe returns default when key not found`() {
        assertEquals(99, safeStorage.getIntSafe("nonexistent", 99))
    }

    // =========================================================================
    // LONG OPERATIONS - VALID KEYS
    // =========================================================================

    @Test
    fun `putLongSafe and getLongSafe work for valid key`() {
        val result = safeStorage.putLongSafe("timestamp", 1234567890123L)
        assertTrue(result is Result.Success)

        assertEquals(1234567890123L, safeStorage.getLongSafe("timestamp"))
    }

    @Test
    fun `getLongSafe returns default when key not found`() {
        assertEquals(0L, safeStorage.getLongSafe("nonexistent"))
    }

    // =========================================================================
    // BOOLEAN OPERATIONS - VALID KEYS
    // =========================================================================

    @Test
    fun `putBooleanSafe and getBooleanSafe work for valid key`() {
        val result = safeStorage.putBooleanSafe("enabled", true)
        assertTrue(result is Result.Success)

        assertTrue(safeStorage.getBooleanSafe("enabled"))
    }

    @Test
    fun `getBooleanSafe returns default when key not found`() {
        assertFalse(safeStorage.getBooleanSafe("nonexistent", false))
        assertTrue(safeStorage.getBooleanSafe("nonexistent", true))
    }

    // =========================================================================
    // FLOAT OPERATIONS - VALID KEYS
    // =========================================================================

    @Test
    fun `putFloatSafe and getFloatSafe work for valid key`() {
        val result = safeStorage.putFloatSafe("ratio", 3.14f)
        assertTrue(result is Result.Success)

        assertEquals(3.14f, safeStorage.getFloatSafe("ratio"), 0.001f)
    }

    @Test
    fun `getFloatSafe returns default when key not found`() {
        assertEquals(0f, safeStorage.getFloatSafe("nonexistent"))
    }

    // =========================================================================
    // DOUBLE OPERATIONS - VALID KEYS
    // =========================================================================

    @Test
    fun `putDoubleSafe and getDoubleSafe work for valid key`() {
        val result = safeStorage.putDoubleSafe("precise", 3.141592653589793)
        assertTrue(result is Result.Success)

        assertEquals(3.141592653589793, safeStorage.getDoubleSafe("precise"), 0.0000001)
    }

    @Test
    fun `getDoubleSafe returns default when key not found`() {
        assertEquals(0.0, safeStorage.getDoubleSafe("nonexistent"))
    }

    // =========================================================================
    // INVALID KEY HANDLING - PUT OPERATIONS
    // =========================================================================

    @Test
    fun `putStringSafe returns Failure for invalid key`() {
        val result = safeStorage.putStringSafe("invalid key", "value")
        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error.contains("Invalid key"))
    }

    @Test
    fun `putIntSafe returns Failure for empty key`() {
        val result = safeStorage.putIntSafe("", 42)
        assertTrue(result is Result.Failure)
    }

    @Test
    fun `putBooleanSafe returns Failure for key with special characters`() {
        val result = safeStorage.putBooleanSafe("key@invalid", true)
        assertTrue(result is Result.Failure)
    }

    @Test
    fun `putLongSafe returns Failure for blank key`() {
        val result = safeStorage.putLongSafe("   ", 123L)
        assertTrue(result is Result.Failure)
    }

    @Test
    fun `putFloatSafe returns Failure for invalid key`() {
        val result = safeStorage.putFloatSafe("path/to/key", 1.5f)
        assertTrue(result is Result.Failure)
    }

    @Test
    fun `putDoubleSafe returns Failure for invalid key`() {
        val result = safeStorage.putDoubleSafe("key#hash", 2.5)
        assertTrue(result is Result.Failure)
    }

    // =========================================================================
    // INVALID KEY HANDLING - GET OPERATIONS
    // =========================================================================

    @Test
    fun `getStringSafe returns default for invalid key`() {
        val value = safeStorage.getStringSafe("invalid key", "default")
        assertEquals("default", value)
    }

    @Test
    fun `getIntSafe returns default for empty key`() {
        val value = safeStorage.getIntSafe("", 99)
        assertEquals(99, value)
    }

    @Test
    fun `getBooleanSafe returns default for key with special characters`() {
        val value = safeStorage.getBooleanSafe("key@invalid", true)
        assertTrue(value)
    }

    @Test
    fun `getLongSafe returns default for invalid key`() {
        val value = safeStorage.getLongSafe("path/key", 555L)
        assertEquals(555L, value)
    }

    @Test
    fun `getFloatSafe returns default for invalid key`() {
        val value = safeStorage.getFloatSafe("key space", 1.1f)
        assertEquals(1.1f, value)
    }

    @Test
    fun `getDoubleSafe returns default for invalid key`() {
        val value = safeStorage.getDoubleSafe("key:colon", 2.2)
        assertEquals(2.2, value)
    }

    // =========================================================================
    // OBJECT OPERATIONS - VALID KEYS
    // =========================================================================

    @Test
    fun `putObjectSafe and getObjectSafe work for valid data`() {
        val config = SafeTestConfig("dark", 14)
        val result = safeStorage.putObjectSafe("config", config)
        assertTrue(result is Result.Success)

        val retrieved = safeStorage.getObjectSafe<SafeTestConfig>("config")
        assertEquals(config, retrieved)
    }

    @Test
    fun `getObjectSafe returns null for missing key without default`() {
        val result = safeStorage.getObjectSafe<SafeTestConfig>("missing")
        assertNull(result)
    }

    @Test
    fun `getObjectSafe returns default for missing key`() {
        val default = SafeTestConfig("light", 12)
        val result = safeStorage.getObjectSafe("missing", default)
        assertEquals(default, result)
    }

    @Test
    fun `getObjectSafe returns default for corrupted data`() {
        // Store invalid JSON directly via unsafe
        safeStorage.unsafe.putString("corrupted", "not valid json{{")

        val default = SafeTestConfig("light", 12)
        val result = safeStorage.getObjectSafe("corrupted", default)

        assertEquals(default, result)
    }

    // =========================================================================
    // OBJECT OPERATIONS - INVALID KEYS
    // =========================================================================

    @Test
    fun `putObjectSafe returns Failure for invalid key`() {
        val config = SafeTestConfig("dark", 14)
        val result = safeStorage.putObjectSafe("invalid key", config)
        assertTrue(result is Result.Failure)
    }

    @Test
    fun `getObjectSafe returns default for invalid key`() {
        val default = SafeTestConfig("light", 12)
        val result = safeStorage.getObjectSafe("key@invalid", default)
        assertEquals(default, result)
    }

    // =========================================================================
    // REMOVE AND CLEAR OPERATIONS
    // =========================================================================

    @Test
    fun `removeSafe works for valid key`() {
        safeStorage.putStringSafe("toRemove", "value")
        val result = safeStorage.removeSafe("toRemove")

        assertTrue(result is Result.Success)
        assertEquals("", safeStorage.getStringSafe("toRemove", ""))
    }

    @Test
    fun `removeSafe returns Failure for invalid key`() {
        val result = safeStorage.removeSafe("invalid key")
        assertTrue(result is Result.Failure)
    }

    @Test
    fun `clearSafe removes all data`() {
        safeStorage.putStringSafe("key1", "value1")
        safeStorage.putIntSafe("key2", 42)

        val result = safeStorage.clearSafe()
        assertTrue(result is Result.Success)

        assertEquals("", safeStorage.getStringSafe("key1", ""))
        assertEquals(0, safeStorage.getIntSafe("key2", 0))
    }

    // =========================================================================
    // CONTAINS OPERATION
    // =========================================================================

    @Test
    fun `containsSafe returns true for existing key`() {
        safeStorage.putStringSafe("exists", "value")
        assertTrue(safeStorage.containsSafe("exists"))
    }

    @Test
    fun `containsSafe returns false for non-existing key`() {
        assertFalse(safeStorage.containsSafe("nonexistent"))
    }

    @Test
    fun `containsSafe returns false for invalid key`() {
        assertFalse(safeStorage.containsSafe("invalid key"))
    }

    // =========================================================================
    // UNSAFE ACCESS
    // =========================================================================

    @Test
    fun `unsafe provides direct access to storage`() {
        safeStorage.unsafe.putString("direct", "value")
        assertEquals("value", safeStorage.getStringSafe("direct"))
    }

    @Test
    fun `unsafe allows invalid keys`() {
        // This should work through unsafe (no validation)
        safeStorage.unsafe.putString("key with spaces", "value")
        // But reading through safe API with invalid key returns default
        assertEquals("default", safeStorage.getStringSafe("key with spaces", "default"))
        // Reading directly through unsafe works
        assertEquals("value", safeStorage.unsafe.getString("key with spaces", ""))
    }

    // =========================================================================
    // FACTORY METHODS
    // =========================================================================

    @Test
    fun `create factory method works`() {
        val storage = SafeEduGoStorage.create()
        assertNotNull(storage)
        assertTrue(storage.putStringSafe("test", "value") is Result.Success)
    }

    @Test
    fun `wrap factory method preserves storage`() {
        val underlying = EduGoStorage.withSettings(MapSettings())
        underlying.putString("existing", "value")

        val safe = SafeEduGoStorage.wrap(underlying)
        assertEquals("value", safe.getStringSafe("existing"))
    }

    @Test
    fun `wrap with validateKeys false skips validation`() {
        val underlying = EduGoStorage.withSettings(MapSettings())
        val safe = SafeEduGoStorage.wrap(underlying, validateKeys = false)

        // Should succeed even with invalid key when validation is disabled
        val result = safe.putStringSafe("invalid key", "value")
        assertTrue(result is Result.Success)
    }

    // =========================================================================
    // EDGE CASES
    // =========================================================================

    @Test
    fun `storing empty string is valid`() {
        val result = safeStorage.putStringSafe("empty", "")
        assertTrue(result is Result.Success)
        assertEquals("", safeStorage.getStringSafe("empty", "default"))
    }

    @Test
    fun `storing zero values works`() {
        safeStorage.putIntSafe("zero.int", 0)
        safeStorage.putLongSafe("zero.long", 0L)
        safeStorage.putFloatSafe("zero.float", 0f)
        safeStorage.putDoubleSafe("zero.double", 0.0)

        assertEquals(0, safeStorage.getIntSafe("zero.int", 99))
        assertEquals(0L, safeStorage.getLongSafe("zero.long", 99L))
        assertEquals(0f, safeStorage.getFloatSafe("zero.float", 99f))
        assertEquals(0.0, safeStorage.getDoubleSafe("zero.double", 99.0))
    }

    @Test
    fun `overwriting existing value works`() {
        safeStorage.putStringSafe("key", "first")
        safeStorage.putStringSafe("key", "second")
        assertEquals("second", safeStorage.getStringSafe("key"))
    }

    @Test
    fun `complex object serialization works`() {
        val user = SafeTestUser(123, "John Doe")
        safeStorage.putObjectSafe("user", user)

        val retrieved = safeStorage.getObjectSafe<SafeTestUser>("user")
        assertEquals(user, retrieved)
    }

    @Test
    fun `list of objects can be stored and retrieved`() {
        val users = listOf(
            SafeTestUser(1, "Alice"),
            SafeTestUser(2, "Bob")
        )
        safeStorage.putObjectSafe("users", users)

        val retrieved = safeStorage.getObjectSafe<List<SafeTestUser>>("users")
        assertEquals(users, retrieved)
    }
}
