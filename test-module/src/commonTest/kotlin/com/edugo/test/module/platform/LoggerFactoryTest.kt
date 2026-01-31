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
        val logger = Logger.withTag("EduGo.Auth")
        assertEquals("EduGo.Auth", logger.tag)
    }

    @Test
    fun testFactoryMethodFromClass() {
        val logger = Logger.fromClass(LoggerFactoryTest::class)
        assertNotNull(logger.tag)
        assertTrue(logger.tag.contains("LoggerFactoryTest"))
    }

    @Test
    fun testRealWorldUseCaseAuthModule() {
        // Simula uso real en módulo de autenticación
        val authLogger = Logger.withTag("EduGo.Auth")
        val loginLogger = authLogger.withChild("Login")
        val oauthLogger = loginLogger.withChild("OAuth")

        assertEquals("EduGo.Auth", authLogger.tag)
        assertEquals("EduGo.Auth.Login", loginLogger.tag)
        assertEquals("EduGo.Auth.Login.OAuth", oauthLogger.tag)
    }

    @Test
    fun testRealWorldUseCaseNetworkModule() {
        // Simula uso real en módulo de red
        val networkLogger = Logger.withTag("EduGo.Network")
        val httpLogger = networkLogger.withChild("HTTP")
        val wsLogger = networkLogger.withChild("WebSocket")

        assertEquals("EduGo.Network", networkLogger.tag)
        assertEquals("EduGo.Network.HTTP", httpLogger.tag)
        assertEquals("EduGo.Network.WebSocket", wsLogger.tag)
    }

    @Test
    fun testRealWorldUseCaseDataModule() {
        // Simula uso real en módulo de datos
        val dataLogger = Logger.withTag("EduGo.Data")
        val repositoryLogger = dataLogger.withChild("Repository")
        val cacheLogger = dataLogger.withChild("Cache")

        assertEquals("EduGo.Data", dataLogger.tag)
        assertEquals("EduGo.Data.Repository", repositoryLogger.tag)
        assertEquals("EduGo.Data.Cache", cacheLogger.tag)
    }

    @Test
    fun testFactoryReturnsConsistentInstances() {
        val logger1 = Logger.withTag("EduGo.Test")
        val logger2 = Logger.withTag("EduGo.Test")

        // Should return same cached instance
        assertTrue(logger1 === logger2)
    }

    @Test
    fun testFactoryWithMultipleLevels() {
        val logger = Logger.withTag("EduGo.Module.Feature.Component")
        assertEquals("EduGo.Module.Feature.Component", logger.tag)
    }

    @Test
    fun testFactoryWithComplexHierarchy() {
        val root = Logger.withTag("EduGo")
        val l1 = root.withChild("Auth")
        val l2 = l1.withChild("Login")
        val l3 = l2.withChild("OAuth")
        val l4 = l3.withChild("Google")

        assertEquals("EduGo.Auth.Login.OAuth.Google", l4.tag)
    }

    @Test
    fun testMultipleModulesCoexist() {
        LoggerCache.clear()

        val auth = Logger.withTag("EduGo.Auth")
        val network = Logger.withTag("EduGo.Network")
        val data = Logger.withTag("EduGo.Data")
        val ui = Logger.withTag("EduGo.UI")

        assertEquals(4, LoggerCache.size())
        assertTrue(LoggerCache.contains("EduGo.Auth"))
        assertTrue(LoggerCache.contains("EduGo.Network"))
        assertTrue(LoggerCache.contains("EduGo.Data"))
        assertTrue(LoggerCache.contains("EduGo.UI"))
    }

    @Test
    fun testFactoryWithClassBasedTags() {
        class UserRepository
        class NetworkClient
        class CacheManager

        val logger1 = Logger.fromClass(UserRepository::class)
        val logger2 = Logger.fromClass(NetworkClient::class)
        val logger3 = Logger.fromClass(CacheManager::class)

        assertTrue(logger1.tag.contains("UserRepository"))
        assertTrue(logger2.tag.contains("NetworkClient"))
        assertTrue(logger3.tag.contains("CacheManager"))
    }
}
