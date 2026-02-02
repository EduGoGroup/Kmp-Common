package com.edugo.test.full.platform

import co.touchlab.kermit.Severity
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests específicos de JVM/Desktop para KermitLogger.
 *
 * Estos tests verifican:
 * - Console output en JVM
 * - Formateo con colores ANSI
 * - Funcionalidad específica de Desktop
 */
class KermitLoggerJvmTest {

    /**
     * Verifica que el logger se inicializa correctamente en JVM.
     */
    @Test
    fun testJvmLoggerInitialization() {
        KermitLogger.initialize()
        val logger = KermitLogger.getLogger()
        assertNotNull(logger, "JVM logger should not be null")
    }

    /**
     * Verifica que KermitConfig.jvm crea un logger válido.
     */
    @Test
    fun testJvmKermitConfigCreatesLogger() {
        val logger = KermitConfig.createLogger()
        assertNotNull(logger, "JVM KermitConfig should create a valid logger")
    }

    /**
     * Verifica que se puede crear un logger personalizado en JVM.
     */
    @Test
    fun testJvmCustomLogger() {
        val customLogger = KermitConfig.createCustomLogger("JvmCustomTag")
        assertNotNull(customLogger, "JVM custom logger should not be null")
    }

    /**
     * Verifica que el formateo con colores ANSI funciona.
     * Este test verifica que el método formatWithColors retorna un string válido.
     */
    @Test
    fun testAnsiColorFormatting() {
        val debugFormatted = KermitConfig.formatWithColors(Severity.Debug, "TestTag", "Debug message")
        val infoFormatted = KermitConfig.formatWithColors(Severity.Info, "TestTag", "Info message")
        val warnFormatted = KermitConfig.formatWithColors(Severity.Warn, "TestTag", "Warn message")
        val errorFormatted = KermitConfig.formatWithColors(Severity.Error, "TestTag", "Error message")

        // Verificar que los mensajes formateados no son vacíos
        assertTrue(debugFormatted.isNotEmpty(), "Debug formatted message should not be empty")
        assertTrue(infoFormatted.isNotEmpty(), "Info formatted message should not be empty")
        assertTrue(warnFormatted.isNotEmpty(), "Warn formatted message should not be empty")
        assertTrue(errorFormatted.isNotEmpty(), "Error formatted message should not be empty")

        // Verificar que contienen el tag y mensaje
        assertTrue(debugFormatted.contains("TestTag"), "Formatted message should contain tag")
        assertTrue(debugFormatted.contains("Debug message"), "Formatted message should contain message")

        // Verificar que contienen códigos ANSI (aunque no sean visibles en el output)
        assertTrue(debugFormatted.contains("\u001B["), "Debug should contain ANSI codes")
        assertTrue(infoFormatted.contains("\u001B["), "Info should contain ANSI codes")
        assertTrue(warnFormatted.contains("\u001B["), "Warn should contain ANSI codes")
        assertTrue(errorFormatted.contains("\u001B["), "Error should contain ANSI codes")
    }

    /**
     * Verifica que cada nivel de severidad tiene un color diferente.
     */
    @Test
    fun testDifferentColorsForDifferentSeverities() {
        val debugFormatted = KermitConfig.formatWithColors(Severity.Debug, "Tag", "Msg")
        val infoFormatted = KermitConfig.formatWithColors(Severity.Info, "Tag", "Msg")
        val errorFormatted = KermitConfig.formatWithColors(Severity.Error, "Tag", "Msg")

        // Los mensajes formateados deberían ser diferentes debido a los códigos de color
        assertTrue(debugFormatted != infoFormatted, "Debug and Info should have different formatting")
        assertTrue(infoFormatted != errorFormatted, "Info and Error should have different formatting")
        assertTrue(debugFormatted != errorFormatted, "Debug and Error should have different formatting")
    }

    /**
     * Verifica que los logs se pueden escribir a console en JVM.
     * Este test simplemente verifica que no hay excepciones.
     */
    @Test
    fun testJvmConsoleLogging() {
        KermitLogger.initialize()

        // Estos deberían aparecer en System.out/System.err
        // No lanzamos excepciones si funcionan correctamente
        KermitLogger.debug("JvmTest", "JVM Debug log to console")
        KermitLogger.info("JvmTest", "JVM Info log to console")
        KermitLogger.error("JvmTest", "JVM Error log to console")

        // Si llegamos aquí sin excepciones, el test pasa
        assertTrue(true, "JVM console logging should work without exceptions")
    }

    /**
     * Verifica que se pueden usar múltiples loggers simultáneamente en JVM.
     */
    @Test
    fun testMultipleJvmLoggers() {
        val logger1 = KermitConfig.createCustomLogger("Network")
        val logger2 = KermitConfig.createCustomLogger("Database")
        val logger3 = KermitConfig.createCustomLogger("Auth")

        // Todos deberían ser válidos
        assertNotNull(logger1, "Network logger should not be null")
        assertNotNull(logger2, "Database logger should not be null")
        assertNotNull(logger3, "Auth logger should not be null")

        // Usar todos los loggers simultáneamente
        logger1.d { "Network request" }
        logger2.i { "Database query executed" }
        logger3.e { "Authentication failed" }

        // Si no hay excepciones, el test pasa
        assertTrue(true, "Multiple JVM loggers should work simultaneously")
    }

    /**
     * Verifica que el formateo funciona con caracteres especiales.
     */
    @Test
    fun testFormattingWithSpecialCharacters() {
        val formatted = KermitConfig.formatWithColors(
            Severity.Info,
            "Special\"Tag",
            "Message with\nnewlines\tand\ttabs"
        )

        assertTrue(formatted.isNotEmpty(), "Formatted message with special chars should not be empty")
        assertTrue(formatted.contains("Special\"Tag"), "Should handle quotes in tag")
        assertTrue(formatted.contains("Message with"), "Should handle newlines in message")
    }

    /**
     * Verifica que el reset ANSI se aplica correctamente.
     */
    @Test
    fun testAnsiReset() {
        val formatted = KermitConfig.formatWithColors(Severity.Info, "Tag", "Message")

        // El mensaje debería contener el código de reset ANSI
        assertTrue(formatted.contains("\u001B[0m"), "Should contain ANSI reset code")
    }
}
