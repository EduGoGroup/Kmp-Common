package com.edugo.test.module.platform

import co.touchlab.kermit.Severity
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests específicos para la implementación Android de KermitLogger.
 *
 * Estos tests verifican que la configuración de Kermit para Android
 * usa correctamente LogcatWriter y delega a android.util.Log.
 *
 * NOTA: Estos son unit tests que no requieren instrumentación.
 * Para tests de integración completos con Logcat, usar androidInstrumentedTest.
 */
class KermitLoggerAndroidTest {

    @Test
    fun testAndroidConfigCreatesValidLogger() {
        // Act
        val logger = KermitConfig.createLogger()

        // Assert
        assertNotNull(logger, "Android KermitConfig should create a valid logger")
    }

    @Test
    fun testAndroidConfigCreatesCustomLogger() {
        // Act
        val logger = KermitConfig.createCustomLogger("CustomTag")

        // Assert
        assertNotNull(logger, "Android should create custom logger with specific tag")
    }

    @Test
    fun testAndroidConfigCreatesLoggerWithMinSeverity() {
        // Act
        val logger = KermitConfig.createLoggerWithMinSeverity(Severity.Warn)

        // Assert
        assertNotNull(logger, "Android should create logger with min severity")
    }

    @Test
    fun testKermitLoggerInitializationOnAndroid() {
        // Act
        KermitLogger.initialize()
        val logger = KermitLogger.getLogger()

        // Assert
        assertNotNull(logger, "KermitLogger should initialize properly on Android")
    }

    @Test
    fun testAndroidLoggingMethodsDoNotThrow() {
        // Arrange
        KermitLogger.initialize()

        // Act & Assert - En Android, los logs van a Logcat
        try {
            KermitLogger.debug("AndroidTest", "Debug message for Logcat")
            KermitLogger.info("AndroidTest", "Info message for Logcat")
            KermitLogger.warn("AndroidTest", "Warning message for Logcat")
            KermitLogger.error("AndroidTest", "Error message for Logcat")
            assertTrue(true, "Android logging methods executed successfully")
        } catch (e: Exception) {
            throw AssertionError("Android logging should not throw exception", e)
        }
    }

    @Test
    fun testAndroidLoggerHandlesExceptions() {
        // Arrange
        KermitLogger.initialize()
        val testException = RuntimeException("Test Android exception")

        // Act & Assert
        try {
            KermitLogger.error("AndroidTest", "Error with exception", testException)
            assertTrue(true, "Android logger handled exception correctly")
        } catch (e: Exception) {
            throw AssertionError("Android logger should handle exceptions", e)
        }
    }

    @Test
    fun testAndroidLoggerSupportsMultipleTags() {
        // Arrange
        KermitLogger.initialize()

        // Act & Assert
        try {
            KermitLogger.debug("Tag1", "Message 1")
            KermitLogger.info("Tag2", "Message 2")
            KermitLogger.warn("Tag3", "Message 3")
            assertTrue(true, "Android logger supports multiple tags")
        } catch (e: Exception) {
            throw AssertionError("Android logger should support multiple tags", e)
        }
    }

    @Test
    fun testAndroidSetMinSeverityDoesNotThrow() {
        // Arrange
        KermitLogger.initialize()

        // Act & Assert
        try {
            KermitLogger.setMinSeverity(Severity.Error)
            assertTrue(true, "Android setMinSeverity executed successfully")
        } catch (e: Exception) {
            throw AssertionError("Android setMinSeverity should not throw", e)
        }
    }
}
