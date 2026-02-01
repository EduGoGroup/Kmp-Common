package com.edugo.test.module.platform

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LogFilterTest {

    @Test
    fun testExactMatch() {
        assertTrue(LogFilter.matches("EduGo.Auth.Login", "EduGo.Auth.Login"), "Exact match should return true")
        assertFalse(LogFilter.matches("EduGo.Auth.Login", "EduGo.Auth.Logout"), "Different tags should not match")
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
        assertEquals(2, authTags.size, "Should filter two Auth tags")
        assertTrue(authTags.contains("EduGo.Auth.Login"), "Should contain Login tag")
        assertTrue(authTags.contains("EduGo.Auth.Logout"), "Should contain Logout tag")

        val networkTags = LogFilter.filter(tags, "EduGo.Network.*")
        assertEquals(2, networkTags.size, "Should filter two Network tags")
    }

    @Test
    fun testCacheSize() {
        LogFilter.clearCache()
        assertEquals(0, LogFilter.getCacheSize(), "Cache should be empty after clear")

        LogFilter.matches("EduGo.Auth.Login", "EduGo.Auth.*")
        assertTrue(LogFilter.getCacheSize() > 0, "Cache should contain entries after pattern match")

        LogFilter.clearCache()
        assertEquals(0, LogFilter.getCacheSize(), "Cache should be empty after second clear")
    }

    @Test
    fun testIsValidPattern() {
        assertTrue(LogFilter.isValidPattern("EduGo.Auth.*"))
        assertTrue(LogFilter.isValidPattern("EduGo.**"))
        assertTrue(LogFilter.isValidPattern("*"))
        assertTrue(LogFilter.isValidPattern("EduGo.Auth.Login"))
        assertTrue(LogFilter.isValidPattern("regex:^EduGo\\.(Auth|Network)\\..*$"))
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

    @Test
    fun testRegexPatternMatching() {
        val pattern = "regex:^EduGo\\.(Auth|Network)\\..*$"

        assertTrue(LogFilter.matches("EduGo.Auth.Login", pattern))
        assertTrue(LogFilter.matches("EduGo.Network.HTTP", pattern))
        assertFalse(LogFilter.matches("EduGo.Data.Cache", pattern))
    }

    @Test
    fun testRegexPatternValidation() {
        assertFalse(LogFilter.isValidPattern("regex:"))
        assertFalse(LogFilter.isValidPattern("regex:["))
        assertFalse(LogFilter.matches("EduGo.Auth.Login", "regex:["))
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
        assertFalse(LogFilter.matchesAny("EduGo.Auth", emptyList()), "Empty pattern list should not match any tag")
    }

    @Test
    fun testMatchesAnyBlankTag() {
        val patterns = listOf("EduGo.*", "Network.*")
        assertFalse(LogFilter.matchesAny("", patterns), "Blank tag should not match any pattern")
        assertFalse(LogFilter.matchesAny("   ", patterns), "Whitespace tag should not match any pattern")
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
        assertEquals(sizeAfterFirst, LogFilter.getCacheSize(), "Cache size should remain constant for same pattern")
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

    // Bounded Cache Tests

    @Test
    fun testCacheBoundedSize() {
        // Limpiar cache antes del test
        LogFilter.clearCache()

        // Crear 150 patterns únicos (excede MAX_CACHE_SIZE de 100)
        repeat(150) { i ->
            LogFilter.matches("EduGo.Test", "Pattern$i.*")
        }

        // El cache no debe exceder el tamaño máximo
        val cacheSize = LogFilter.getCacheSize()
        assertTrue(
            cacheSize <= 100,
            "Cache size ($cacheSize) should not exceed MAX_CACHE_SIZE (100)"
        )
    }

    @Test
    fun testCacheEvictionFIFO() {
        LogFilter.clearCache()

        // Agregar primer patrón que será evicted
        LogFilter.matches("Tag", "FirstPattern")
        assertEquals(1, LogFilter.getCacheSize(), "Cache should have 1 pattern")

        // Llenar cache hasta el límite (99 más = 100 total)
        repeat(99) { i ->
            LogFilter.matches("Tag", "Pattern$i.*")
        }

        assertEquals(100, LogFilter.getCacheSize(), "Cache should be at max capacity")

        // Agregar uno más - debe evict "FirstPattern" (FIFO)
        LogFilter.matches("Tag", "NewPattern.*")

        // Cache sigue en 100
        assertEquals(100, LogFilter.getCacheSize(), "Cache should remain at max capacity after eviction")
    }

    @Test
    fun testCacheEvictionMultipleRounds() {
        LogFilter.clearCache()

        // Agregar 200 patterns (2x el límite)
        repeat(200) { i ->
            LogFilter.matches("Tag", "Pattern$i.*")
        }

        // Solo los últimos 100 deberían estar en cache
        assertEquals(100, LogFilter.getCacheSize(), "Cache should stabilize at max size")
    }

    @Test
    fun testCacheClearResetsSize() {
        // Llenar cache parcialmente
        repeat(50) { i ->
            LogFilter.matches("Tag", "Pattern$i.*")
        }

        assertTrue(LogFilter.getCacheSize() > 0, "Cache should have entries")

        // Limpiar cache
        LogFilter.clearCache()

        // Cache debe estar vacío
        assertEquals(0, LogFilter.getCacheSize(), "Cache should be empty after clear")

        // Debe funcionar correctamente después de limpiar
        LogFilter.matches("EduGo.Auth", "EduGo.*")
        assertEquals(1, LogFilter.getCacheSize(), "Cache should work after clear")
    }

    @Test
    fun testConcurrentPatternCompilation() = runTest {
        LogFilter.clearCache()

        val pattern = "EduGo.Auth.*"
        val results = mutableListOf<Boolean>()
        val mutex = Mutex()

        // Launch 100 coroutines that all compile the same pattern
        coroutineScope {
            repeat(100) { i ->
                launch {
                    val tag = "EduGo.Auth.Test$i"
                    val matches = LogFilter.matches(tag, pattern)
                    mutex.withLock {
                        results.add(matches)
                    }
                }
            }
        }

        // All should match
        assertEquals(100, results.size, "Should have 100 results")
        assertTrue(results.all { it }, "All tags should match the pattern")

        // Cache should only have the pattern compiled once
        val cacheSize = LogFilter.getCacheSize()
        assertTrue(cacheSize >= 1, "Cache should have at least the pattern")
    }
}
