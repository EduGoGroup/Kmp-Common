package com.edugo.test.module.platform

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
        assertEquals("EduGo.Auth", logger.tag)
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
        assertEquals("EduGo.Auth.Login", child.tag)
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
        LoggerCache.clear()

        val logger1 = LoggerCache.getOrCreate("EduGo.Auth")
        val logger2 = LoggerCache.getOrCreate("EduGo.Auth")

        // Same instance should be returned
        assertTrue(logger1 === logger2)
        assertEquals(1, LoggerCache.size())
    }

    @Test
    fun testLoggerCacheMultipleTags() {
        LoggerCache.clear()

        val logger1 = LoggerCache.getOrCreate("EduGo.Auth")
        val logger2 = LoggerCache.getOrCreate("EduGo.Network")
        val logger3 = LoggerCache.getOrCreate("EduGo.Auth") // Same as logger1

        assertTrue(logger1 === logger3)
        assertFalse(logger1 === logger2)
        assertEquals(2, LoggerCache.size())

        val tags = LoggerCache.getAllTags()
        assertTrue(tags.contains("EduGo.Auth"))
        assertTrue(tags.contains("EduGo.Network"))
    }

    @Test
    fun testLoggerExtensions() {
        val logger = Logger.withTag("EduGo.Test")
        assertEquals("EduGo.Test", logger.tag)
    }

    @Test
    fun testFromClass() {
        val logger = TaggedLogger.fromClass(TaggedLoggerTest::class)
        assertNotNull(logger.tag)
        // Tag should contain the class name
        assertTrue(logger.tag.contains("TaggedLoggerTest"))
    }

    @Test
    fun testEqualsAndHashCode() {
        val logger1 = TaggedLogger.create("EduGo.Auth")
        val logger2 = TaggedLogger.create("EduGo.Auth")
        val logger3 = TaggedLogger.create("EduGo.Network")

        assertEquals(logger1, logger2)
        assertEquals(logger1.hashCode(), logger2.hashCode())
        assertFalse(logger1 == logger3)
    }

    // Edge Cases Tests

    @Test
    fun testTagWithSpecialCharacters() {
        val logger1 = TaggedLogger.create("EduGo_Auth")
        val logger2 = TaggedLogger.create("EduGo-Network")

        assertEquals("EduGo_Auth", logger1.tag)
        assertEquals("EduGo-Network", logger2.tag)
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
        assertTrue(str.contains("EduGo.Auth"))
    }

    // Thread-Safety Tests

    @Test
    fun testConcurrentCacheAccess() {
        LoggerCache.clear()
        val tag = "EduGo.Concurrent.Test"
        val results = mutableListOf<TaggedLogger>()
        val threads = mutableListOf<Thread>()

        // Create 100 threads that all try to get the same logger
        repeat(100) { i ->
            val thread = Thread {
                val logger = LoggerCache.getOrCreate(tag)
                synchronized(results) {
                    results.add(logger)
                }
            }
            threads.add(thread)
            thread.start()
        }

        // Wait for all threads to complete
        threads.forEach { it.join() }

        // All results should be the same instance
        assertEquals(100, results.size)
        val firstLogger = results[0]
        results.forEach { logger ->
            assertTrue(logger === firstLogger, "All loggers should be the same instance")
        }

        // Cache should only have one entry
        assertEquals(1, LoggerCache.size())
    }

    @Test
    fun testConcurrentMultipleTags() {
        LoggerCache.clear()
        val tags = listOf("Tag1", "Tag2", "Tag3", "Tag4", "Tag5")
        val results = mutableMapOf<String, MutableList<TaggedLogger>>()
        val threads = mutableListOf<Thread>()

        // Initialize results map
        tags.forEach { tag ->
            synchronized(results) {
                results[tag] = mutableListOf()
            }
        }

        // Create multiple threads per tag
        tags.forEach { tag ->
            repeat(20) {
                val thread = Thread {
                    val logger = LoggerCache.getOrCreate(tag)
                    synchronized(results) {
                        results[tag]?.add(logger)
                    }
                }
                threads.add(thread)
                thread.start()
            }
        }

        // Wait for all threads
        threads.forEach { it.join() }

        // Verify each tag has same instance across all threads
        tags.forEach { tag ->
            val loggers = results[tag]
            assertNotNull(loggers)
            assertEquals(20, loggers.size)
            val first = loggers[0]
            loggers.forEach { logger ->
                assertTrue(logger === first)
            }
        }

        // Cache should have 5 entries
        assertEquals(5, LoggerCache.size())
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
