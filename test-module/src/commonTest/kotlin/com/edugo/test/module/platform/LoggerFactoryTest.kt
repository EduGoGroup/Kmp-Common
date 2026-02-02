package com.edugo.test.module.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for Logger factory methods and real-world use cases.
 */
class LoggerFactoryTest {

    @Test
    fun testFactoryMethodWithTag() {
        val logger = DefaultLogger.withTag("EduGo.Auth")
        assertEquals("EduGo.Auth", logger.tag)
    }

    @Test
    fun testFactoryMethodFromClass() {
        val logger = DefaultLogger.fromClass(LoggerFactoryTest::class)
        assertNotNull(logger.tag)
        assertTrue(logger.tag.contains("LoggerFactoryTest"))
    }

    @Test
    fun testRealWorldUseCaseAuthModule() {
        // Simula uso real en módulo de autenticación
        val authLogger = DefaultLogger.withTag("EduGo.Auth")
        val loginLogger = authLogger.withChild("Login")
        val oauthLogger = loginLogger.withChild("OAuth")

        assertEquals("EduGo.Auth", authLogger.tag)
        assertEquals("EduGo.Auth.Login", loginLogger.tag)
        assertEquals("EduGo.Auth.Login.OAuth", oauthLogger.tag)
    }

    @Test
    fun testRealWorldUseCaseNetworkModule() {
        // Simula uso real en módulo de red
        val networkLogger = DefaultLogger.withTag("EduGo.Network")
        val httpLogger = networkLogger.withChild("HTTP")
        val wsLogger = networkLogger.withChild("WebSocket")

        assertEquals("EduGo.Network", networkLogger.tag)
        assertEquals("EduGo.Network.HTTP", httpLogger.tag)
        assertEquals("EduGo.Network.WebSocket", wsLogger.tag)
    }

    @Test
    fun testRealWorldUseCaseDataModule() {
        // Simula uso real en módulo de datos
        val dataLogger = DefaultLogger.withTag("EduGo.Data")
        val repositoryLogger = dataLogger.withChild("Repository")
        val cacheLogger = dataLogger.withChild("Cache")

        assertEquals("EduGo.Data", dataLogger.tag)
        assertEquals("EduGo.Data.Repository", repositoryLogger.tag)
        assertEquals("EduGo.Data.Cache", cacheLogger.tag)
    }

    @Test
    fun testFactoryReturnsConsistentInstances() {
        val logger1 = DefaultLogger.withTag("EduGo.Test")
        val logger2 = DefaultLogger.withTag("EduGo.Test")

        // Should return same cached instance
        assertTrue(logger1 === logger2)
    }

    @Test
    fun testFactoryWithMultipleLevels() {
        val logger = DefaultLogger.withTag("EduGo.Module.Feature.Component")
        assertEquals("EduGo.Module.Feature.Component", logger.tag)
    }

    @Test
    fun testFactoryWithComplexHierarchy() {
        val root = DefaultLogger.withTag("EduGo")
        val l1 = root.withChild("Auth")
        val l2 = l1.withChild("Login")
        val l3 = l2.withChild("OAuth")
        val l4 = l3.withChild("Google")

        assertEquals("EduGo.Auth.Login.OAuth.Google", l4.tag)
    }

    @Test
    fun testMultipleModulesCoexist() {
        LoggerCacheUtils.clearCache()

        val auth = DefaultLogger.withTag("EduGo.Auth")
        val network = DefaultLogger.withTag("EduGo.Network")
        val data = DefaultLogger.withTag("EduGo.Data")
        val ui = DefaultLogger.withTag("EduGo.UI")

        assertEquals(4, LoggerCacheUtils.getCacheSize())
        assertTrue(LoggerCacheUtils.isTagCached("EduGo.Auth"))
        assertTrue(LoggerCacheUtils.isTagCached("EduGo.Network"))
        assertTrue(LoggerCacheUtils.isTagCached("EduGo.Data"))
        assertTrue(LoggerCacheUtils.isTagCached("EduGo.UI"))
    }

    @Test
    fun testFactoryWithClassBasedTags() {
        class UserRepository
        class NetworkClient
        class CacheManager

        val logger1 = DefaultLogger.fromClass(UserRepository::class)
        val logger2 = DefaultLogger.fromClass(NetworkClient::class)
        val logger3 = DefaultLogger.fromClass(CacheManager::class)

        assertTrue(logger1.tag.contains("UserRepository"))
        assertTrue(logger2.tag.contains("NetworkClient"))
        assertTrue(logger3.tag.contains("CacheManager"))
    }
}
