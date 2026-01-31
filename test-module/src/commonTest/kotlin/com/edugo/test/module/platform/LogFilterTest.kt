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

    // Edge Cases Tests

    @Test
    fun testEmptyPattern() {
        assertFalse(LogFilter.matches("EduGo.Auth", ""))
    }

    @Test
    fun testPatternWithSpecialCharacters() {
        assertTrue(LogFilter.matches("EduGo_Auth", "EduGo_Auth"))
        assertTrue(LogFilter.matches("EduGo-Network", "EduGo-*"))
    }

    @Test
    fun testCaseSensitiveMatching() {
        assertTrue(LogFilter.matches("EduGo.Auth", "EduGo.Auth"))
        assertFalse(LogFilter.matches("edugo.auth", "EduGo.Auth"))
        assertFalse(LogFilter.matches("EduGo.Auth", "edugo.auth"))
    }

    @Test
    fun testWildcardAtBeginning() {
        assertTrue(LogFilter.matches("EduGo.Auth", "*.Auth"))
        assertTrue(LogFilter.matches("Module.Auth", "*.Auth"))
        assertFalse(LogFilter.matches("EduGo.Network", "*.Auth"))
    }

    @Test
    fun testWildcardAtEnd() {
        assertTrue(LogFilter.matches("EduGo.Auth", "EduGo.*"))
        assertTrue(LogFilter.matches("EduGo.Network", "EduGo.*"))
        assertFalse(LogFilter.matches("EduGo.Auth.Login", "EduGo.*"))
    }

    @Test
    fun testMultipleDoubleWildcards() {
        // Pattern with ** in multiple places
        assertTrue(LogFilter.matches("A.B.C.D.E", "A.**.E"))
        assertTrue(LogFilter.matches("A.B.E", "A.**.E"))

        // ** can match zero or more segments only when not between dots
        assertTrue(LogFilter.matches("A.B.C", "A.**"))
    }

    @Test
    fun testRealWorldPatterns() {
        // Auth module patterns
        assertTrue(LogFilter.matches("EduGo.Auth.Login", "EduGo.Auth.*"))
        assertTrue(LogFilter.matches("EduGo.Auth.Logout", "EduGo.Auth.*"))
        assertTrue(LogFilter.matches("EduGo.Auth.Register", "EduGo.Auth.*"))

        // Network module patterns
        assertTrue(LogFilter.matches("EduGo.Network.HTTP", "EduGo.Network.*"))
        assertTrue(LogFilter.matches("EduGo.Network.WebSocket", "EduGo.Network.*"))

        // All EduGo modules
        assertTrue(LogFilter.matches("EduGo.Auth.Login", "EduGo.**"))
        assertTrue(LogFilter.matches("EduGo.Network.HTTP", "EduGo.**"))
        assertTrue(LogFilter.matches("EduGo.Data.Cache", "EduGo.**"))
    }

    @Test
    fun testFilterEmptyCollection() {
        val result = LogFilter.filter(emptyList(), "EduGo.*")
        assertTrue(result.isEmpty())
    }

    @Test
    fun testMatchesAnyEmptyPatterns() {
        assertFalse(LogFilter.matchesAny("EduGo.Auth", emptyList()))
    }

    @Test
    fun testCachePerformance() {
        LogFilter.clearCache()
        val pattern = "EduGo.Auth.*"
        val tag = "EduGo.Auth.Login"

        // First call compiles pattern
        LogFilter.matches(tag, pattern)
        val sizeAfterFirst = LogFilter.getCacheSize()

        // Subsequent calls use cached pattern
        LogFilter.matches(tag, pattern)
        LogFilter.matches(tag, pattern)
        LogFilter.matches(tag, pattern)

        // Cache size should not change
        assertEquals(sizeAfterFirst, LogFilter.getCacheSize())
    }

    @Test
    fun testPatternWithNumbers() {
        assertTrue(LogFilter.matches("EduGo.V2.Auth", "EduGo.V2.*"))
        assertTrue(LogFilter.matches("Module123.Feature", "Module123.*"))
    }

    @Test
    fun testVeryComplexPattern() {
        val pattern = "EduGo.*.Login.*.Google"
        assertTrue(LogFilter.matches("EduGo.Auth.Login.OAuth.Google", pattern))
        assertTrue(LogFilter.matches("EduGo.Network.Login.SSO.Google", pattern))
        assertFalse(LogFilter.matches("EduGo.Auth.Logout.OAuth.Google", pattern))
    }
}
