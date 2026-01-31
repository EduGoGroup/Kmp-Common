package com.edugo.test.module.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.BeforeTest

class LoggerConfigTest {

    @BeforeTest
    fun setup() {
        LoggerConfig.reset()
    }

    @Test
    fun testDefaultLevel() {
        assertEquals(LogLevel.DEBUG, LoggerConfig.defaultLevel)
        assertTrue(LoggerConfig.isEnabled("EduGo.Auth", LogLevel.DEBUG))
        assertTrue(LoggerConfig.isEnabled("EduGo.Auth", LogLevel.INFO))
        assertTrue(LoggerConfig.isEnabled("EduGo.Auth", LogLevel.ERROR))
    }

    @Test
    fun testSetLevel() {
        LoggerConfig.setLevel("EduGo.Auth.*", LogLevel.INFO)

        assertFalse(LoggerConfig.isEnabled("EduGo.Auth.Login", LogLevel.DEBUG))
        assertTrue(LoggerConfig.isEnabled("EduGo.Auth.Login", LogLevel.INFO))
        assertTrue(LoggerConfig.isEnabled("EduGo.Auth.Login", LogLevel.ERROR))
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
    fun testGetLevel() {
        LoggerConfig.setLevel("EduGo.Auth.*", LogLevel.INFO)

        assertEquals(LogLevel.INFO, LoggerConfig.getLevel("EduGo.Auth.Login"))
        assertEquals(LogLevel.DEBUG, LoggerConfig.getLevel("EduGo.Network.HTTP")) // default
    }

    @Test
    fun testRemoveLevel() {
        LoggerConfig.setLevel("EduGo.Auth.*", LogLevel.INFO)
        assertFalse(LoggerConfig.isEnabled("EduGo.Auth.Login", LogLevel.DEBUG))

        LoggerConfig.removeLevel("EduGo.Auth.*")
        assertTrue(LoggerConfig.isEnabled("EduGo.Auth.Login", LogLevel.DEBUG)) // back to default
    }

    @Test
    fun testClearLevels() {
        LoggerConfig.setLevel("EduGo.Auth.*", LogLevel.INFO)
        LoggerConfig.setLevel("EduGo.Network.*", LogLevel.ERROR)

        LoggerConfig.clearLevels()

        assertTrue(LoggerConfig.getAllRules().isEmpty())
        assertTrue(LoggerConfig.isEnabled("EduGo.Auth.Login", LogLevel.DEBUG))
    }

    @Test
    fun testGetAllRules() {
        LoggerConfig.setLevel("EduGo.Auth.*", LogLevel.INFO)
        LoggerConfig.setLevel("EduGo.Network.*", LogLevel.ERROR)

        val rules = LoggerConfig.getAllRules()
        assertEquals(2, rules.size)
        assertEquals(LogLevel.INFO, rules["EduGo.Auth.*"])
        assertEquals(LogLevel.ERROR, rules["EduGo.Network.*"])
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
}
