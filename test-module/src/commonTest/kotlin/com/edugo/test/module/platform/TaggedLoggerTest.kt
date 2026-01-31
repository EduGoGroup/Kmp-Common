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
}
