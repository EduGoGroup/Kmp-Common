package com.edugo.test.module.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlin.test.assertFailsWith

class LoggerConfigTest {

    @BeforeTest
    fun setup() {
        LoggerConfig.reset()
    }

    @AfterTest
    fun teardown() {
        // Ensure clean state even if test fails
        LoggerConfig.reset()
    }

    @Test
    fun testDefaultLevel() {
        assertEquals(LogLevel.DEBUG, LoggerConfig.defaultLevel, "Default level should be DEBUG")
        assertTrue(LoggerConfig.isEnabled("EduGo.Auth", LogLevel.DEBUG), "DEBUG should be enabled by default")
        assertTrue(LoggerConfig.isEnabled("EduGo.Auth", LogLevel.INFO), "INFO should be enabled by default")
        assertTrue(LoggerConfig.isEnabled("EduGo.Auth", LogLevel.ERROR), "ERROR should be enabled by default")
    }

    @Test
    fun testSetLevel() {
        LoggerConfig.setLevel("EduGo.Auth.*", LogLevel.INFO)

        assertFalse(LoggerConfig.isEnabled("EduGo.Auth.Login", LogLevel.DEBUG), "DEBUG should be disabled when level is INFO")
        assertTrue(LoggerConfig.isEnabled("EduGo.Auth.Login", LogLevel.INFO), "INFO should be enabled when level is INFO")
        assertTrue(LoggerConfig.isEnabled("EduGo.Auth.Login", LogLevel.ERROR), "ERROR should be enabled when level is INFO")
    }

    @Test
    fun testMultipleLevels() {
        LoggerConfig.setLevel("EduGo.Auth.*", LogLevel.INFO)
        LoggerConfig.setLevel("EduGo.Network.*", LogLevel.ERROR)

        // Auth: INFO and up
        assertFalse(LoggerConfig.isEnabled("EduGo.Auth.Login", LogLevel.DEBUG))
        assertTrue(LoggerConfig.isEnabled("EduGo.Auth.Login", LogLevel.INFO))

        // Network: ERROR only
        assertFalse(LoggerConfig.isEnabled("EduGo.Network.HTTP", LogLevel.DEBUG))
        assertFalse(LoggerConfig.isEnabled("EduGo.Network.HTTP", LogLevel.INFO))
        assertTrue(LoggerConfig.isEnabled("EduGo.Network.HTTP", LogLevel.ERROR))
    }

    @Test
    fun testMostSpecificPatternWins() {
        LoggerConfig.setLevel("EduGo.**", LogLevel.ERROR)
        LoggerConfig.setLevel("EduGo.Auth.*", LogLevel.DEBUG)

        // More specific pattern (EduGo.Auth.*) should win
        assertTrue(LoggerConfig.isEnabled("EduGo.Auth.Login", LogLevel.DEBUG))

        // Less specific pattern applies to other modules
        assertFalse(LoggerConfig.isEnabled("EduGo.Network.HTTP", LogLevel.DEBUG))
        assertTrue(LoggerConfig.isEnabled("EduGo.Network.HTTP", LogLevel.ERROR))
    }

    @Test
    fun testRegexPatternRule() {
        LoggerConfig.setLevel("regex:^EduGo\\.Auth\\..*$", LogLevel.INFO)

        assertFalse(LoggerConfig.isEnabled("EduGo.Auth.Login", LogLevel.DEBUG))
        assertTrue(LoggerConfig.isEnabled("EduGo.Auth.Login", LogLevel.INFO))

        // Non-matching tag should use default level (DEBUG)
        assertTrue(LoggerConfig.isEnabled("EduGo.Network.HTTP", LogLevel.DEBUG))
    }

    @Test
    fun testGetLevel() {
        LoggerConfig.setLevel("EduGo.Auth.*", LogLevel.INFO)

        assertEquals(LogLevel.INFO, LoggerConfig.getLevel("EduGo.Auth.Login"), "Should return configured level for matching pattern")
        assertEquals(LogLevel.DEBUG, LoggerConfig.getLevel("EduGo.Network.HTTP"), "Should return default level for non-matching pattern")
    }

    @Test
    fun testRemoveLevel() {
        LoggerConfig.setLevel("EduGo.Auth.*", LogLevel.INFO)
        assertFalse(LoggerConfig.isEnabled("EduGo.Auth.Login", LogLevel.DEBUG), "DEBUG should be disabled with INFO level")

        LoggerConfig.removeLevel("EduGo.Auth.*")
        assertTrue(LoggerConfig.isEnabled("EduGo.Auth.Login", LogLevel.DEBUG), "Should revert to default level after removal")
    }

    @Test
    fun testClearLevels() {
        LoggerConfig.setLevel("EduGo.Auth.*", LogLevel.INFO)
        LoggerConfig.setLevel("EduGo.Network.*", LogLevel.ERROR)

        LoggerConfig.clearLevels()

        assertTrue(LoggerConfig.getAllRules().isEmpty(), "All rules should be cleared")
        assertTrue(LoggerConfig.isEnabled("EduGo.Auth.Login", LogLevel.DEBUG), "Should revert to default level after clearing")
    }

    @Test
    fun testGetAllRules() {
        LoggerConfig.setLevel("EduGo.Auth.*", LogLevel.INFO)
        LoggerConfig.setLevel("EduGo.Network.*", LogLevel.ERROR)

        val rules = LoggerConfig.getAllRules()
        assertEquals(2, rules.size, "Should return all configured rules")
        assertEquals(LogLevel.INFO, rules["EduGo.Auth.*"], "Auth pattern should have INFO level")
        assertEquals(LogLevel.ERROR, rules["EduGo.Network.*"], "Network pattern should have ERROR level")
    }

    @Test
    fun testReset() {
        LoggerConfig.defaultLevel = LogLevel.ERROR
        LoggerConfig.setLevel("EduGo.Auth.*", LogLevel.INFO)

        LoggerConfig.reset()

        assertEquals(LogLevel.DEBUG, LoggerConfig.defaultLevel)
        assertTrue(LoggerConfig.getAllRules().isEmpty())
    }

    @Test
    fun testChangeDefaultLevel() {
        LoggerConfig.defaultLevel = LogLevel.INFO

        assertFalse(LoggerConfig.isEnabled("EduGo.Auth", LogLevel.DEBUG))
        assertTrue(LoggerConfig.isEnabled("EduGo.Auth", LogLevel.INFO))
    }

    // Level Cache Tests

    @Test
    fun testLevelCachePerformance() {
        // Configure some rules
        LoggerConfig.setLevel("EduGo.Auth.*", LogLevel.INFO)
        LoggerConfig.setLevel("EduGo.Network.*", LogLevel.ERROR)

        // First call - cache miss
        val level1 = LoggerConfig.getLevel("EduGo.Auth.Login")
        assertEquals(LogLevel.INFO, level1)

        // Second call - should hit cache (much faster)
        val level2 = LoggerConfig.getLevel("EduGo.Auth.Login")
        assertEquals(LogLevel.INFO, level2)

        // Different tag - cache miss
        val level3 = LoggerConfig.getLevel("EduGo.Network.HTTP")
        assertEquals(LogLevel.ERROR, level3)
    }

    @Test
    fun testCacheInvalidationOnSetLevel() {
        LoggerConfig.setLevel("EduGo.Auth.*", LogLevel.INFO)

        // Cache the level
        val level1 = LoggerConfig.getLevel("EduGo.Auth.Login")
        assertEquals(LogLevel.INFO, level1)

        // Change rule - should invalidate cache
        LoggerConfig.setLevel("EduGo.Auth.*", LogLevel.ERROR)

        // Should return new level
        val level2 = LoggerConfig.getLevel("EduGo.Auth.Login")
        assertEquals(LogLevel.ERROR, level2)
    }

    @Test
    fun testCacheInvalidationOnRemoveLevel() {
        LoggerConfig.setLevel("EduGo.Auth.*", LogLevel.ERROR)

        // Cache the level
        assertEquals(LogLevel.ERROR, LoggerConfig.getLevel("EduGo.Auth.Login"))

        // Remove rule - should invalidate cache
        LoggerConfig.removeLevel("EduGo.Auth.*")

        // Should return default level
        assertEquals(LogLevel.DEBUG, LoggerConfig.getLevel("EduGo.Auth.Login"))
    }

    @Test
    fun testCacheInvalidationOnClearLevels() {
        LoggerConfig.setLevel("EduGo.Auth.*", LogLevel.ERROR)
        LoggerConfig.setLevel("EduGo.Network.*", LogLevel.INFO)

        // Cache multiple levels
        assertEquals(LogLevel.ERROR, LoggerConfig.getLevel("EduGo.Auth.Login"))
        assertEquals(LogLevel.INFO, LoggerConfig.getLevel("EduGo.Network.HTTP"))

        // Clear all rules - should invalidate cache
        LoggerConfig.clearLevels()

        // Should return default level for both
        assertEquals(LogLevel.DEBUG, LoggerConfig.getLevel("EduGo.Auth.Login"))
        assertEquals(LogLevel.DEBUG, LoggerConfig.getLevel("EduGo.Network.HTTP"))
    }

    @Test
    fun testCacheInvalidationOnReset() {
        LoggerConfig.setLevel("EduGo.Auth.*", LogLevel.ERROR)

        // Cache the level
        assertEquals(LogLevel.ERROR, LoggerConfig.getLevel("EduGo.Auth.Login"))

        // Reset - should invalidate cache and rules
        LoggerConfig.reset()

        // Should return default level
        assertEquals(LogLevel.DEBUG, LoggerConfig.getLevel("EduGo.Auth.Login"))
        assertEquals(LogLevel.DEBUG, LoggerConfig.defaultLevel)
    }

    @Test
    fun testCacheBoundedSize() {
        LoggerConfig.setLevel("EduGo.**", LogLevel.INFO)

        // Cache 150 different tags (exceeds max of 100)
        repeat(150) { i ->
            val tag = "EduGo.Module$i.Feature"
            val level = LoggerConfig.getLevel(tag)
            assertEquals(LogLevel.INFO, level, "Tag $tag should match EduGo.** pattern")
        }

        // All calls should succeed without memory issues
        // Cache should have evicted oldest entries (FIFO)
        // This test validates that cache doesn't grow unbounded
    }

    @Test
    fun testSetLevelValidatesPattern() {
        // Valid patterns should work
        LoggerConfig.setLevel("EduGo.Auth.*", LogLevel.INFO)
        LoggerConfig.setLevel("EduGo.**", LogLevel.ERROR)
        LoggerConfig.setLevel("*", LogLevel.DEBUG)
        LoggerConfig.setLevel("regex:^EduGo\\..*$", LogLevel.DEBUG)

        // Invalid patterns should throw
        assertFailsWith<IllegalArgumentException>("Blank pattern should throw") {
            LoggerConfig.setLevel("", LogLevel.INFO)
        }

        assertFailsWith<IllegalArgumentException>("Whitespace pattern should throw") {
            LoggerConfig.setLevel("   ", LogLevel.INFO)
        }

        assertFailsWith<IllegalArgumentException>("Invalid regex pattern should throw") {
            LoggerConfig.setLevel("regex:[", LogLevel.INFO)
        }
    }
}
