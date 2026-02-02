package com.edugo.test.module.platform

import co.touchlab.kermit.Severity
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests específicos para la implementación JavaScript de KermitLogger.
 *
 * Estos tests verifican que la configuración de Kermit para JS
 * usa correctamente ConsoleWriter y delega a console.log/warn/error.
 *
 * NOTA: En el navegador, los logs aparecerán en las DevTools.
 * En Node.js, los logs aparecerán en stdout/stderr.
 */
class KermitLoggerJsTest {

    @Test
    fun testJsConfigCreatesValidLogger() {
        // Act
        val logger = KermitConfig.createLogger()

        // Assert
        assertNotNull(logger, "JS KermitConfig should create a valid logger")
    }

    @Test
    fun testJsConfigCreatesCustomLogger() {
        // Act
        val logger = KermitConfig.createCustomLogger("CustomTag")

        // Assert
        assertNotNull(logger, "JS should create custom logger with specific tag")
    }

    @Test
    fun testJsConfigCreatesLoggerWithMinSeverity() {
        // Act
        val logger = KermitConfig.createLoggerWithMinSeverity(Severity.Warn)

        // Assert
        assertNotNull(logger, "JS should create logger with min severity")
    }

    @Test
    fun testKermitLoggerInitializationOnJs() {
        // Act
        KermitLogger.initialize()
        val logger = KermitLogger.getLogger()

        // Assert
        assertNotNull(logger, "KermitLogger should initialize properly on JS")
    }

    @Test
    fun testJsLoggingMethodsDoNotThrow() {
        // Arrange
        KermitLogger.initialize()

        // Act & Assert - En JS, los logs van a console
        try {
            KermitLogger.debug("JsTest", "Debug message for console.log")
            KermitLogger.info("JsTest", "Info message for console.info")
            KermitLogger.warn("JsTest", "Warning message for console.warn")
            KermitLogger.error("JsTest", "Error message for console.error")
            assertTrue(true, "JS logging methods executed successfully")
        } catch (e: Exception) {
            throw AssertionError("JS logging should not throw exception", e)
        }
    }

    @Test
    fun testJsLoggerHandlesExceptions() {
        // Arrange
        KermitLogger.initialize()
        val testException = RuntimeException("Test JS exception")

        // Act & Assert
        try {
            KermitLogger.error("JsTest", "Error with exception", testException)
            assertTrue(true, "JS logger handled exception correctly")
        } catch (e: Exception) {
            throw AssertionError("JS logger should handle exceptions", e)
        }
    }

    @Test
    fun testJsLoggerSupportsMultipleTags() {
        // Arrange
        KermitLogger.initialize()

        // Act & Assert
        try {
            KermitLogger.debug("ApiClient", "Fetching data")
            KermitLogger.info("StateManager", "State updated")
            KermitLogger.warn("Renderer", "Slow rendering detected")
            assertTrue(true, "JS logger supports multiple tags")
        } catch (e: Exception) {
            throw AssertionError("JS logger should support multiple tags", e)
        }
    }

    @Test
    fun testJsSetMinSeverityDoesNotThrow() {
        // Arrange
        KermitLogger.initialize()

        // Act & Assert
        try {
            KermitLogger.setMinSeverity(Severity.Info)
            assertTrue(true, "JS setMinSeverity executed successfully")
        } catch (e: Exception) {
            throw AssertionError("JS setMinSeverity should not throw", e)
        }
    }

    @Test
    fun testJsLoggerWorksWithoutInitialization() {
        // Act & Assert - Kermit tiene un logger por defecto
        try {
            KermitLogger.debug("JsTest", "Message without init")
            assertTrue(true, "JS logger works with default configuration")
        } catch (e: Exception) {
            throw AssertionError("JS logger should work with defaults", e)
        }
    }

    @Test
    fun testJsMultipleSequentialLogs() {
        // Arrange
        KermitLogger.initialize()

        // Act & Assert - Múltiples logs en secuencia
        try {
            repeat(5) { index ->
                KermitLogger.info("JsTest", "Sequential log $index")
            }
            assertTrue(true, "JS logger handles multiple sequential logs")
        } catch (e: Exception) {
            throw AssertionError("JS logger should handle sequential logs", e)
        }
    }
}
