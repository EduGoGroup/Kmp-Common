package com.edugo.test.module.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LogFilterTest {

    @Test
    fun testExactMatch() {
        assertTrue(LogFilter.matches("EduGo.Auth.Login", "EduGo.Auth.Login"))
        assertFalse(LogFilter.matches("EduGo.Auth.Login", "EduGo.Auth.Logout"))
    }

    @Test
    fun testSingleWildcard() {
        // * matches within a single segment
        assertTrue(LogFilter.matches("EduGo.Auth.Login", "EduGo.Auth.*"))
        assertTrue(LogFilter.matches("EduGo.Auth.Logout", "EduGo.Auth.*"))
        assertTrue(LogFilter.matches("EduGo.Auth.Login", "EduGo.*.*"))
        assertTrue(LogFilter.matches("EduGo.Auth.Login", "*.Auth.Login"))

        // * should not match across segments
        assertFalse(LogFilter.matches("EduGo.Auth.Login.OAuth", "EduGo.Auth.*"))
    }

    @Test
    fun testDoubleWildcard() {
        // ** matches across multiple segments
        assertTrue(LogFilter.matches("EduGo.Auth.Login", "EduGo.**"))
        assertTrue(LogFilter.matches("EduGo.Auth.Login.OAuth", "EduGo.**"))
        assertTrue(LogFilter.matches("EduGo.Auth", "EduGo.**"))

        // ** alone matches everything
        assertTrue(LogFilter.matches("EduGo", "**"))
        assertTrue(LogFilter.matches("EduGo.Auth.Login", "**"))
    }

    @Test
    fun testUniversalWildcard() {
        assertTrue(LogFilter.matches("anything", "*"))
        assertTrue(LogFilter.matches("EduGo.Auth.Login", "**"))
    }

    @Test
    fun testMatchesAny() {
        val patterns = listOf("EduGo.Auth.*", "EduGo.Network.*")

        assertTrue(LogFilter.matchesAny("EduGo.Auth.Login", patterns))
        assertTrue(LogFilter.matchesAny("EduGo.Network.HTTP", patterns))
        assertFalse(LogFilter.matchesAny("EduGo.Data.Repository", patterns))
    }

    @Test
    fun testFilter() {
        val tags = listOf(
            "EduGo.Auth.Login",
            "EduGo.Auth.Logout",
            "EduGo.Network.HTTP",
            "EduGo.Network.WebSocket"
        )

        val authTags = LogFilter.filter(tags, "EduGo.Auth.*")
        assertEquals(2, authTags.size)
        assertTrue(authTags.contains("EduGo.Auth.Login"))
        assertTrue(authTags.contains("EduGo.Auth.Logout"))

        val networkTags = LogFilter.filter(tags, "EduGo.Network.*")
        assertEquals(2, networkTags.size)
    }

    @Test
    fun testCacheSize() {
        LogFilter.clearCache()
        assertEquals(0, LogFilter.getCacheSize())

        LogFilter.matches("EduGo.Auth.Login", "EduGo.Auth.*")
        assertTrue(LogFilter.getCacheSize() > 0)

        LogFilter.clearCache()
        assertEquals(0, LogFilter.getCacheSize())
    }

    @Test
    fun testIsValidPattern() {
        assertTrue(LogFilter.isValidPattern("EduGo.Auth.*"))
        assertTrue(LogFilter.isValidPattern("EduGo.**"))
        assertTrue(LogFilter.isValidPattern("*"))
        assertTrue(LogFilter.isValidPattern("EduGo.Auth.Login"))
    }

    @Test
    fun testComplexPatterns() {
        // Pattern with wildcard in the middle
        assertTrue(LogFilter.matches("EduGo.Auth.Login", "EduGo.*.Login"))
        assertTrue(LogFilter.matches("EduGo.Network.Login", "EduGo.*.Login"))
        assertFalse(LogFilter.matches("EduGo.Auth.Logout", "EduGo.*.Login"))

        // Multiple wildcards
        assertTrue(LogFilter.matches("EduGo.Auth.Login.OAuth", "EduGo.*.*.*"))
    }
}
