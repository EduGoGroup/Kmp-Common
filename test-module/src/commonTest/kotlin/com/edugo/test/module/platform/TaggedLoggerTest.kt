package com.edugo.test.module.platform

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class TaggedLoggerTest {

    @Test
    fun testCreateTaggedLogger() {
        val logger = TaggedLogger.create("EduGo.Auth")
        assertEquals("EduGo.Auth", logger.tag, "Logger tag should match the created tag")
    }

    @Test
    fun testTaggedLoggerValidation() {
        // Valid tags
        TaggedLogger.create("EduGo.Auth")
        TaggedLogger.create("EduGo.Auth.Login")
        TaggedLogger.create("EduGo.Auth.Login.OAuth")

        // Invalid tags should throw
        assertFailsWith<IllegalArgumentException> {
            TaggedLogger.create("")
        }
        assertFailsWith<IllegalArgumentException> {
            TaggedLogger.create(".EduGo")
        }
        assertFailsWith<IllegalArgumentException> {
            TaggedLogger.create("EduGo.")
        }
        assertFailsWith<IllegalArgumentException> {
            TaggedLogger.create("EduGo..Auth")
        }
    }

    @Test
    fun testWithChild() {
        val parent = TaggedLogger.create("EduGo.Auth")
        val child = parent.withChild("Login")
        assertEquals("EduGo.Auth.Login", child.tag, "Child tag should append to parent tag")
    }

    @Test
    fun testWithChildValidation() {
        val logger = TaggedLogger.create("EduGo.Auth")

        // Child tag cannot contain dots
        assertFailsWith<IllegalArgumentException> {
            logger.withChild("Login.OAuth")
        }
    }

    @Test
    fun testLoggerCache() {
        LoggerCacheUtils.clearCache()

        val logger1 = Logger.withTag("EduGo.Auth")
        val logger2 = Logger.withTag("EduGo.Auth")

        // Same instance should be returned
        assertTrue(logger1 === logger2, "Same tag should return cached instance")
        assertEquals(1, LoggerCacheUtils.getCacheSize(), "Cache should contain exactly one entry")
    }

    @Test
    fun testLoggerCacheMultipleTags() {
        LoggerCacheUtils.clearCache()

        val logger1 = Logger.withTag("EduGo.Auth")
        val logger2 = Logger.withTag("EduGo.Network")
        val logger3 = Logger.withTag("EduGo.Auth") // Same as logger1

        assertTrue(logger1 === logger3, "Same tag should return same cached instance")
        assertFalse(logger1 === logger2, "Different tags should return different instances")
        assertEquals(2, LoggerCacheUtils.getCacheSize(), "Cache should contain two distinct entries")

        val tags = LoggerCacheUtils.getAllCachedTags()
        assertTrue(tags.contains("EduGo.Auth"), "Cache should contain Auth tag")
        assertTrue(tags.contains("EduGo.Network"), "Cache should contain Network tag")
    }

    @Test
    fun testLoggerExtensions() {
        val logger = Logger.withTag("EduGo.Test")
        assertEquals("EduGo.Test", logger.tag, "Extension should create logger with correct tag")
    }

    @Test
    fun testFromClass() {
        val logger = TaggedLogger.fromClass(TaggedLoggerTest::class)
        assertNotNull(logger.tag, "Tag from class should not be null")
        // Tag should contain the class name
        assertTrue(logger.tag.contains("TaggedLoggerTest"), "Tag should contain class name")
    }

    @Test
    fun testEqualsAndHashCode() {
        val logger1 = TaggedLogger.create("EduGo.Auth")
        val logger2 = TaggedLogger.create("EduGo.Auth")
        val logger3 = TaggedLogger.create("EduGo.Network")

        assertEquals(logger1, logger2, "Loggers with same tag should be equal")
        assertEquals(logger1.hashCode(), logger2.hashCode(), "Equal loggers should have same hash code")
        assertFalse(logger1 == logger3, "Loggers with different tags should not be equal")
    }

    // Edge Cases Tests

    @Test
    fun testTagWithSpecialCharacters() {
        val logger1 = TaggedLogger.create("EduGo_Auth")
        val logger2 = TaggedLogger.create("EduGo-Network")

        assertEquals("EduGo_Auth", logger1.tag, "Tags should support underscore characters")
        assertEquals("EduGo-Network", logger2.tag, "Tags should support hyphen characters")
    }

    @Test
    fun testTagWithNumbers() {
        val logger = TaggedLogger.create("EduGo.V2.Auth")
        assertEquals("EduGo.V2.Auth", logger.tag)
    }

    @Test
    fun testVeryLongTag() {
        val longTag = "EduGo.Module1.Module2.Module3.Module4.Module5.Feature.Component"
        val logger = TaggedLogger.create(longTag)
        assertEquals(longTag, logger.tag)
    }

    @Test
    fun testSingleSegmentTag() {
        val logger = TaggedLogger.create("EduGo")
        assertEquals("EduGo", logger.tag)
    }

    @Test
    fun testBlankChildTag() {
        val logger = TaggedLogger.create("EduGo.Auth")
        assertFailsWith<IllegalArgumentException> {
            logger.withChild("")
        }
        assertFailsWith<IllegalArgumentException> {
            logger.withChild("   ")
        }
    }

    @Test
    fun testToString() {
        val logger = TaggedLogger.create("EduGo.Auth")
        val str = logger.toString()
        assertTrue(str.contains("EduGo.Auth"), "toString should include the tag")
    }

    // Thread-Safety Tests

    @Test
    fun testConcurrentCacheAccess() = runTest {
        LoggerCacheUtils.clearCache()
        val tag = "EduGo.Concurrent.Test"
        val results = mutableListOf<TaggedLogger>()
        val mutex = Mutex()

        // Launch 100 coroutines that all try to get the same logger
        coroutineScope {
            repeat(100) {
                launch {
                    val logger = Logger.withTag(tag)
                    mutex.withLock {
                        results.add(logger)
                    }
                }
            }
        }

        // All coroutines complete before test continues

        // All results should be the same instance
        assertEquals(100, results.size, "Should have 100 logger instances")
        val firstLogger = results[0]
        results.forEach { logger ->
            assertTrue(logger === firstLogger, "All loggers should be the same instance")
        }

        // Cache should only have one entry
        assertEquals(1, LoggerCacheUtils.getCacheSize(), "Cache should only have one entry for the tag")
    }

    @Test
    fun testConcurrentMultipleTags() = runTest {
        LoggerCacheUtils.clearCache()
        val tags = listOf("Tag1", "Tag2", "Tag3", "Tag4", "Tag5")
        val results = mutableMapOf<String, MutableList<TaggedLogger>>()
        val mutex = Mutex()

        // Initialize results map
        tags.forEach { tag ->
            results[tag] = mutableListOf()
        }

        // Launch multiple coroutines per tag (20 per tag = 100 total)
        coroutineScope {
            tags.forEach { tag ->
                repeat(20) {
                    launch {
                        val logger = Logger.withTag(tag)
                        mutex.withLock {
                            results[tag]?.add(logger)
                        }
                    }
                }
            }
        }

        // All coroutines complete before test continues

        // Verify each tag has same instance across all coroutines
        tags.forEach { tag ->
            val loggers = results[tag]
            assertNotNull(loggers, "Results for tag $tag should not be null")
            assertEquals(20, loggers.size, "Should have 20 loggers for tag $tag")
            val first = loggers[0]
            loggers.forEach { logger ->
                assertTrue(logger === first, "All loggers for tag $tag should be the same instance")
            }
        }

        // Cache should have 5 entries (one per tag)
        assertEquals(5, LoggerCacheUtils.getCacheSize(), "Cache should have 5 entries (one per tag)")
    }

    @Test
    fun testLoggerConfigIntegration() {
        LoggerConfig.reset()
        LoggerConfig.setLevel("EduGo.Auth.*", LogLevel.INFO)

        val logger = TaggedLogger.create("EduGo.Auth.Login")

        // Verify level filtering works
        assertFalse(LoggerConfig.isEnabled("EduGo.Auth.Login", LogLevel.DEBUG))
        assertTrue(LoggerConfig.isEnabled("EduGo.Auth.Login", LogLevel.INFO))
    }

    @Test
    fun testWithTagReplacesTag() {
        val logger1 = TaggedLogger.create("EduGo.Auth")
        val logger2 = logger1.withTag("EduGo.Network")

        assertEquals("EduGo.Auth", logger1.tag)
        assertEquals("EduGo.Network", logger2.tag)
        assertFalse(logger1 === logger2)
    }
}
