package com.edugo.test.module.storage

import com.edugo.test.module.core.Result
import kotlin.test.*

class StorageKeyValidatorTest {

    // =========================================================================
    // VALID KEYS
    // =========================================================================

    @Test
    fun `valid simple key passes validation`() {
        val result = StorageKeyValidator.validate("username")
        assertTrue(result is Result.Success)
        assertEquals("username", (result as Result.Success).data)
    }

    @Test
    fun `valid key with dots passes`() {
        val result = StorageKeyValidator.validate("user.profile.name")
        assertTrue(result is Result.Success)
    }

    @Test
    fun `valid key with underscores passes`() {
        val result = StorageKeyValidator.validate("user_settings")
        assertTrue(result is Result.Success)
    }

    @Test
    fun `valid key with dashes passes`() {
        val result = StorageKeyValidator.validate("cache-v2")
        assertTrue(result is Result.Success)
    }

    @Test
    fun `valid key with numbers passes`() {
        val result = StorageKeyValidator.validate("user123")
        assertTrue(result is Result.Success)
    }

    @Test
    fun `valid key with mixed characters passes`() {
        val result = StorageKeyValidator.validate("app.config_v2-beta.setting")
        assertTrue(result is Result.Success)
    }

    @Test
    fun `valid key at max length passes`() {
        val maxLengthKey = "a".repeat(StorageKeyValidator.MAX_KEY_LENGTH)
        val result = StorageKeyValidator.validate(maxLengthKey)
        assertTrue(result is Result.Success)
    }

    // =========================================================================
    // INVALID KEYS - EMPTY/BLANK
    // =========================================================================

    @Test
    fun `empty key fails validation`() {
        val result = StorageKeyValidator.validate("")
        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error.contains("empty"))
    }

    @Test
    fun `blank key fails validation`() {
        val result = StorageKeyValidator.validate("   ")
        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error.contains("empty"))
    }

    // =========================================================================
    // INVALID KEYS - SPECIAL CHARACTERS
    // =========================================================================

    @Test
    fun `key with spaces fails validation`() {
        val result = StorageKeyValidator.validate("user name")
        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error.contains("invalid characters"))
    }

    @Test
    fun `key with at sign fails validation`() {
        val result = StorageKeyValidator.validate("user@name")
        assertTrue(result is Result.Failure)
    }

    @Test
    fun `key with hash fails validation`() {
        val result = StorageKeyValidator.validate("user#id")
        assertTrue(result is Result.Failure)
    }

    @Test
    fun `key with slash fails validation`() {
        val result = StorageKeyValidator.validate("path/to/key")
        assertTrue(result is Result.Failure)
    }

    @Test
    fun `key with backslash fails validation`() {
        val result = StorageKeyValidator.validate("path\\to\\key")
        assertTrue(result is Result.Failure)
    }

    @Test
    fun `key with colon fails validation`() {
        val result = StorageKeyValidator.validate("prefix:key")
        assertTrue(result is Result.Failure)
    }

    @Test
    fun `key with asterisk fails validation`() {
        val result = StorageKeyValidator.validate("key*")
        assertTrue(result is Result.Failure)
    }

    @Test
    fun `key with question mark fails validation`() {
        val result = StorageKeyValidator.validate("key?")
        assertTrue(result is Result.Failure)
    }

    // =========================================================================
    // INVALID KEYS - LENGTH
    // =========================================================================

    @Test
    fun `key exceeding max length fails validation`() {
        val longKey = "a".repeat(StorageKeyValidator.MAX_KEY_LENGTH + 1)
        val result = StorageKeyValidator.validate(longKey)
        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error.contains("maximum length"))
    }

    @Test
    fun `key significantly exceeding max length fails validation`() {
        val veryLongKey = "a".repeat(1000)
        val result = StorageKeyValidator.validate(veryLongKey)
        assertTrue(result is Result.Failure)
    }

    // =========================================================================
    // isValid() METHOD
    // =========================================================================

    @Test
    fun `isValid returns true for valid key`() {
        assertTrue(StorageKeyValidator.isValid("valid.key"))
        assertTrue(StorageKeyValidator.isValid("another_valid-key"))
        assertTrue(StorageKeyValidator.isValid("key123"))
    }

    @Test
    fun `isValid returns false for invalid keys`() {
        assertFalse(StorageKeyValidator.isValid(""))
        assertFalse(StorageKeyValidator.isValid("   "))
        assertFalse(StorageKeyValidator.isValid("invalid key"))
        assertFalse(StorageKeyValidator.isValid("key@invalid"))
    }

    // =========================================================================
    // requireValid() METHOD
    // =========================================================================

    @Test
    fun `requireValid returns key for valid input`() {
        val key = StorageKeyValidator.requireValid("valid.key")
        assertEquals("valid.key", key)
    }

    @Test
    fun `requireValid throws for empty key`() {
        assertFailsWith<IllegalArgumentException> {
            StorageKeyValidator.requireValid("")
        }
    }

    @Test
    fun `requireValid throws for blank key`() {
        assertFailsWith<IllegalArgumentException> {
            StorageKeyValidator.requireValid("   ")
        }
    }

    @Test
    fun `requireValid throws for invalid characters`() {
        assertFailsWith<IllegalArgumentException> {
            StorageKeyValidator.requireValid("invalid key")
        }
    }

    @Test
    fun `requireValid throws for key exceeding max length`() {
        val longKey = "a".repeat(StorageKeyValidator.MAX_KEY_LENGTH + 1)
        assertFailsWith<IllegalArgumentException> {
            StorageKeyValidator.requireValid(longKey)
        }
    }

    // =========================================================================
    // sanitize() METHOD
    // =========================================================================

    @Test
    fun `sanitize replaces invalid characters with underscore`() {
        val sanitized = StorageKeyValidator.sanitize("user@name#123")
        assertEquals("user_name_123", sanitized)
    }

    @Test
    fun `sanitize replaces spaces with underscore`() {
        val sanitized = StorageKeyValidator.sanitize("user name")
        assertEquals("user_name", sanitized)
    }

    @Test
    fun `sanitize trims whitespace`() {
        val sanitized = StorageKeyValidator.sanitize("  key  ")
        assertEquals("key", sanitized)
    }

    @Test
    fun `sanitize truncates long keys`() {
        val longKey = "a".repeat(300)
        val sanitized = StorageKeyValidator.sanitize(longKey)
        assertEquals(StorageKeyValidator.MAX_KEY_LENGTH, sanitized.length)
    }

    @Test
    fun `sanitize preserves valid characters`() {
        val sanitized = StorageKeyValidator.sanitize("valid.key_name-123")
        assertEquals("valid.key_name-123", sanitized)
    }

    @Test
    fun `sanitize handles multiple consecutive invalid characters`() {
        val sanitized = StorageKeyValidator.sanitize("key@#\$%value")
        assertEquals("key____value", sanitized)
    }

    @Test
    fun `sanitize produces valid key`() {
        val originalKey = "  user@email/path:key  "
        val sanitized = StorageKeyValidator.sanitize(originalKey)
        assertTrue(StorageKeyValidator.isValid(sanitized))
    }

    // =========================================================================
    // MAX_KEY_LENGTH CONSTANT
    // =========================================================================

    @Test
    fun `MAX_KEY_LENGTH is 256`() {
        assertEquals(256, StorageKeyValidator.MAX_KEY_LENGTH)
    }

    // =========================================================================
    // EDGE CASES
    // =========================================================================

    @Test
    fun `single character key is valid`() {
        assertTrue(StorageKeyValidator.isValid("a"))
        assertTrue(StorageKeyValidator.isValid("1"))
        assertTrue(StorageKeyValidator.isValid("_"))
        assertTrue(StorageKeyValidator.isValid("-"))
        assertTrue(StorageKeyValidator.isValid("."))
    }

    @Test
    fun `key starting with number is valid`() {
        assertTrue(StorageKeyValidator.isValid("123key"))
    }

    @Test
    fun `key starting with underscore is valid`() {
        assertTrue(StorageKeyValidator.isValid("_private"))
    }

    @Test
    fun `key starting with dash is valid`() {
        assertTrue(StorageKeyValidator.isValid("-key"))
    }

    @Test
    fun `key starting with dot is valid`() {
        assertTrue(StorageKeyValidator.isValid(".hidden"))
    }

    @Test
    fun `key with consecutive dots is valid`() {
        assertTrue(StorageKeyValidator.isValid("key..name"))
    }

    @Test
    fun `key with unicode characters fails`() {
        assertFalse(StorageKeyValidator.isValid("ключ"))
        assertFalse(StorageKeyValidator.isValid("键"))
        assertFalse(StorageKeyValidator.isValid("키"))
    }

    @Test
    fun `key with emoji fails`() {
        assertFalse(StorageKeyValidator.isValid("key😀"))
    }
}
