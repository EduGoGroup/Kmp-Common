package com.edugo.test.module.platform

import co.touchlab.kermit.Severity
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertContains

/**
 * Tests específicos para la implementación JVM/Desktop de KermitLogger.
 *
 * Estos tests verifican que la configuración de Kermit para JVM
 * usa correctamente ConsoleWriter y soporta colores ANSI.
 */
class KermitLoggerJvmTest {

    @Test
    fun testJvmConfigCreatesValidLogger() {
        // Act
        val logger = KermitConfig.createLogger()

        // Assert
        assertNotNull(logger, "JVM KermitConfig should create a valid logger")
    }

    @Test
    fun testJvmConfigCreatesCustomLogger() {
        // Act
        val logger = KermitConfig.createCustomLogger("CustomTag")

        // Assert
        assertNotNull(logger, "JVM should create custom logger with specific tag")
    }

    @Test
    fun testJvmConfigCreatesLoggerWithMinSeverity() {
        // Act
        val logger = KermitConfig.createLoggerWithMinSeverity(Severity.Info)

        // Assert
        assertNotNull(logger, "JVM should create logger with min severity")
    }

    @Test
    fun testKermitLoggerInitializationOnJvm() {
        // Act
        KermitLogger.initialize()
        val logger = KermitLogger.getLogger()

        // Assert
        assertNotNull(logger, "KermitLogger should initialize properly on JVM")
    }

    @Test
    fun testJvmLoggingMethodsDoNotThrow() {
        // Arrange
        KermitLogger.initialize()

        // Act & Assert - En JVM, los logs van a console
        try {
            KermitLogger.debug("JvmTest", "Debug message for console")
            KermitLogger.info("JvmTest", "Info message for console")
            KermitLogger.warn("JvmTest", "Warning message for console")
            KermitLogger.error("JvmTest", "Error message for console")
            assertTrue(true, "JVM logging methods executed successfully")
        } catch (e: Exception) {
            throw AssertionError("JVM logging should not throw exception", e)
        }
    }

    @Test
    fun testJvmFormatWithColorsIncludesAnsiCodes() {
        // Act
        val formattedDebug = KermitConfig.formatWithColors(Severity.Debug, "TestTag", "Debug message")
        val formattedInfo = KermitConfig.formatWithColors(Severity.Info, "TestTag", "Info message")
        val formattedWarn = KermitConfig.formatWithColors(Severity.Warn, "TestTag", "Warn message")
        val formattedError = KermitConfig.formatWithColors(Severity.Error, "TestTag", "Error message")

        // Assert - Verificar que contienen códigos ANSI
        assertTrue(formattedDebug.contains("\u001B"), "Debug format should contain ANSI codes")
        assertTrue(formattedInfo.contains("\u001B"), "Info format should contain ANSI codes")
        assertTrue(formattedWarn.contains("\u001B"), "Warn format should contain ANSI codes")
        assertTrue(formattedError.contains("\u001B"), "Error format should contain ANSI codes")
    }

    @Test
    fun testJvmFormatWithColorsIncludesTag() {
        // Act
        val formatted = KermitConfig.formatWithColors(Severity.Info, "MyTag", "Test message")

        // Assert
        assertContains(formatted, "MyTag", ignoreCase = false,
            "Formatted message should contain the tag")
    }

    @Test
    fun testJvmFormatWithColorsIncludesMessage() {
        // Act
        val testMessage = "This is a test message"
        val formatted = KermitConfig.formatWithColors(Severity.Info, "Tag", testMessage)

        // Assert
        assertContains(formatted, testMessage, ignoreCase = false,
            "Formatted message should contain the original message")
    }

    @Test
    fun testJvmFormatWithColorsIncludesSeverity() {
        // Act
        val formattedDebug = KermitConfig.formatWithColors(Severity.Debug, "Tag", "Message")
        val formattedWarn = KermitConfig.formatWithColors(Severity.Warn, "Tag", "Message")

        // Assert
        assertTrue(
            formattedDebug.contains("DEBUG", ignoreCase = true),
            "Debug formatted message should contain severity level"
        )
        assertTrue(
            formattedWarn.contains("WARN", ignoreCase = true),
            "Warn formatted message should contain severity level"
        )
    }

    @Test
    fun testJvmLoggerHandlesExceptions() {
        // Arrange
        KermitLogger.initialize()
        val testException = RuntimeException("Test JVM exception")

        // Act & Assert
        try {
            KermitLogger.error("JvmTest", "Error with exception", testException)
            assertTrue(true, "JVM logger handled exception correctly")
        } catch (e: Exception) {
            throw AssertionError("JVM logger should handle exceptions", e)
        }
    }

    @Test
    fun testJvmSetMinSeverityDoesNotThrow() {
        // Arrange
        KermitLogger.initialize()

        // Act & Assert
        try {
            KermitLogger.setMinSeverity(Severity.Warn)
            assertTrue(true, "JVM setMinSeverity executed successfully")
        } catch (e: Exception) {
            throw AssertionError("JVM setMinSeverity should not throw", e)
        }
    }

    @Test
    fun testJvmLoggerSupportsMultipleTags() {
        // Arrange
        KermitLogger.initialize()

        // Act & Assert
        try {
            KermitLogger.debug("NetworkClient", "Request sent")
            KermitLogger.info("AuthManager", "User logged in")
            KermitLogger.warn("DatabaseRepo", "Connection slow")
            assertTrue(true, "JVM logger supports multiple tags")
        } catch (e: Exception) {
            throw AssertionError("JVM logger should support multiple tags", e)
        }
    }
}
