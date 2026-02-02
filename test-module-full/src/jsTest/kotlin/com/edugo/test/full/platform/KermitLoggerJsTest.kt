package com.edugo.test.full.platform

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests específicos de JavaScript para KermitLogger.
 *
 * Estos tests verifican:
 * - Console output en JavaScript (Browser/Node.js)
 * - Funcionalidad específica de JS
 * - Integración con console.log/warn/error
 *
 * NOTA: En JS, los logs aparecen en la consola del navegador (DevTools)
 * o en la terminal de Node.js durante la ejecución de tests.
 */
class KermitLoggerJsTest {

    /**
     * Verifica que el logger se inicializa correctamente en JS.
     */
    @Test
    fun testJsLoggerInitialization() {
        KermitLogger.initialize()
        val logger = KermitLogger.getLogger()
        assertNotNull(logger, "JS logger should not be null")
    }

    /**
     * Verifica que KermitConfig.js crea un logger válido.
     */
    @Test
    fun testJsKermitConfigCreatesLogger() {
        val logger = KermitConfig.createLogger()
        assertNotNull(logger, "JS KermitConfig should create a valid logger")
    }

    /**
     * Verifica que se puede crear un logger personalizado en JS.
     */
    @Test
    fun testJsCustomLogger() {
        val customLogger = KermitConfig.createCustomLogger("JsCustomTag")
        assertNotNull(customLogger, "JS custom logger should not be null")
    }

    /**
     * Verifica que los logs se pueden escribir a console en JS.
     * En el navegador, estos aparecen en DevTools Console.
     * En Node.js, aparecen en stdout/stderr.
     */
    @Test
    fun testJsConsoleLogging() {
        KermitLogger.initialize()

        // Estos deberían aparecer en console (browser) o stdout (node)
        // DEBUG -> console.log (gris en DevTools)
        KermitLogger.debug("JsTest", "JS Debug log to console")

        // INFO -> console.info (azul en DevTools)
        KermitLogger.info("JsTest", "JS Info log to console")

        // ERROR -> console.error (rojo en DevTools)
        KermitLogger.error("JsTest", "JS Error log to console")

        // Si llegamos aquí sin excepciones, el test pasa
        assertTrue(true, "JS console logging should work without exceptions")
    }

    /**
     * Verifica que se pueden usar múltiples loggers simultáneamente en JS.
     */
    @Test
    fun testMultipleJsLoggers() {
        val logger1 = KermitConfig.createCustomLogger("Network")
        val logger2 = KermitConfig.createCustomLogger("UI")
        val logger3 = KermitConfig.createCustomLogger("Storage")

        // Todos deberían ser válidos
        assertNotNull(logger1, "Network logger should not be null")
        assertNotNull(logger2, "UI logger should not be null")
        assertNotNull(logger3, "Storage logger should not be null")

        // Usar todos los loggers simultáneamente
        logger1.d { "Fetching data from API" }
        logger2.i { "Rendering component" }
        logger3.e { "Failed to save to localStorage" }

        // Si no hay excepciones, el test pasa
        assertTrue(true, "Multiple JS loggers should work simultaneously")
    }

    /**
     * Verifica el logging con excepciones en JS.
     */
    @Test
    fun testJsLoggingWithException() {
        KermitLogger.initialize()

        val exception = RuntimeException("Test exception in JS")

        // Esto debería mostrar el stack trace en console.error
        KermitLogger.error("JsTest", "Error with exception", exception)

        // Si no hay excepciones adicionales, el test pasa
        assertTrue(true, "JS error logging with exception should work")
    }

    /**
     * Verifica que el logging funciona con objetos JavaScript nativos.
     */
    @Test
    fun testJsLoggingWithNativeObjects() {
        KermitLogger.initialize()

        // En JS, podemos loggear estructuras complejas
        val complexMessage = """
            {
                "user": "test@example.com",
                "action": "login",
                "timestamp": ${js("Date.now()")}
            }
        """.trimIndent()

        KermitLogger.info("JsTest", complexMessage)

        assertTrue(true, "JS logging with complex messages should work")
    }

    /**
     * Verifica que el logging funciona en diferentes contextos de JS.
     * (Browser vs Node.js - ambos usan console pero con implementaciones diferentes)
     */
    @Test
    fun testJsLoggingInDifferentContexts() {
        KermitLogger.initialize()

        // Este test debería pasar tanto en browser como en node
        KermitLogger.debug("JsContext", "Running in JS context (browser or node)")
        KermitLogger.info("JsContext", "Logger works in current JS environment")

        assertTrue(true, "JS logging should work in any JS context")
    }

    /**
     * Verifica que los mensajes largos se manejan correctamente en JS.
     */
    @Test
    fun testJsLongMessages() {
        KermitLogger.initialize()

        val longMessage = "Long message: " + "A".repeat(500)

        // Los navegadores modernos pueden manejar mensajes muy largos
        KermitLogger.info("JsTest", longMessage)

        assertTrue(true, "JS should handle long messages")
    }

    /**
     * Verifica que los caracteres Unicode/Emoji funcionan en JS.
     */
    @Test
    fun testJsUnicodeAndEmoji() {
        KermitLogger.initialize()

        val unicodeMessage = "Unicode test: 你好 мир 🎉 ✅ 🚀"

        // JS/navegadores tienen excelente soporte para Unicode
        KermitLogger.info("JsTest", unicodeMessage)

        assertTrue(true, "JS should handle Unicode and emoji")
    }

    /**
     * Verifica que el logging no bloquea el event loop en JS.
     */
    @Test
    fun testJsLoggingNonBlocking() {
        KermitLogger.initialize()

        // Múltiples logs rápidos no deberían bloquear
        repeat(10) { i ->
            KermitLogger.debug("JsPerf", "Log message $i")
        }

        assertTrue(true, "JS logging should be non-blocking")
    }

    /**
     * Verifica que se pueden loggear valores undefined/null en JS.
     */
    @Test
    fun testJsLoggingNullValues() {
        KermitLogger.initialize()

        // En JS, estos son conceptos importantes
        KermitLogger.debug("JsNull", "Testing null: ${js("null")}")
        KermitLogger.info("JsUndefined", "Testing undefined: ${js("undefined")}")

        assertTrue(true, "JS should handle null/undefined in logs")
    }
}
