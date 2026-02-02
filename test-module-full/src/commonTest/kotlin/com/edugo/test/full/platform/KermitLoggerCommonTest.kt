package com.edugo.test.full.platform

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests comunes de KermitLogger que se ejecutan en todas las plataformas.
 *
 * Estos tests verifican:
 * - Inicialización correcta
 * - Mapeo de niveles Logger -> Kermit
 * - Configuración de loggers personalizados
 * - Múltiples loggers simultáneos
 */
class KermitLoggerCommonTest {

    /**
     * Verifica que KermitLogger se inicializa correctamente.
     */
    @Test
    fun testKermitLoggerInitialization() {
        // Inicializar
        KermitLogger.initialize()

        // Verificar que el logger no es nulo
        val logger = KermitLogger.getLogger()
        assertNotNull(logger, "Logger should not be null after initialization")
    }

    /**
     * Verifica que se puede obtener el logger sin inicializar explícitamente.
     * Kermit proporciona una instancia por defecto.
     */
    @Test
    fun testGetLoggerWithoutInitialization() {
        val logger = KermitLogger.getLogger()
        assertNotNull(logger, "Logger should have default instance")
    }

    /**
     * Verifica que se puede configurar un logger personalizado.
     */
    @Test
    fun testSetCustomLogger() {
        // Crear un logger personalizado con un tag específico
        val customLogger = Logger.withTag("CustomTag")

        // Configurarlo
        KermitLogger.setLogger(customLogger)

        // Verificar que se configuró correctamente
        val retrievedLogger = KermitLogger.getLogger()
        assertNotNull(retrievedLogger, "Custom logger should not be null")
    }

    /**
     * Verifica que los métodos de logging no lanzan excepciones.
     * El output real se verifica en los tests específicos de plataforma.
     */
    @Test
    fun testLoggingMethodsDontThrow() {
        KermitLogger.initialize()

        // Estos no deberían lanzar excepciones
        KermitLogger.debug("TestTag", "Debug message")
        KermitLogger.info("TestTag", "Info message")
        KermitLogger.error("TestTag", "Error message")

        // Con excepción
        val exception = RuntimeException("Test exception")
        KermitLogger.error("TestTag", "Error with exception", exception)
    }

    /**
     * Verifica que se pueden crear múltiples loggers con diferentes tags.
     */
    @Test
    fun testMultipleLoggersWithDifferentTags() {
        // Crear loggers con diferentes tags
        val logger1 = Logger.withTag("Network")
        val logger2 = Logger.withTag("Auth")
        val logger3 = Logger.withTag("Database")

        // Verificar que todos son instancias válidas
        assertNotNull(logger1, "Network logger should not be null")
        assertNotNull(logger2, "Auth logger should not be null")
        assertNotNull(logger3, "Database logger should not be null")

        // Los loggers deberían ser independientes
        // (Esto es garantizado por Kermit internamente)
        assertTrue(logger1 !== logger2, "Loggers should be different instances")
        assertTrue(logger2 !== logger3, "Loggers should be different instances")
    }

    /**
     * Verifica que KermitConfig puede crear loggers personalizados.
     */
    @Test
    fun testKermitConfigCreatesLogger() {
        val logger = KermitConfig.createLogger()
        assertNotNull(logger, "KermitConfig should create a valid logger")
    }

    /**
     * Verifica que se pueden usar diferentes niveles de severidad.
     */
    @Test
    fun testDifferentSeverityLevels() {
        KermitLogger.initialize()

        // Estos deberían ejecutarse sin errores
        // El comportamiento específico se verifica en tests de plataforma
        KermitLogger.debug("SeverityTest", "Verbose/Debug level")
        KermitLogger.info("SeverityTest", "Info level")
        KermitLogger.error("SeverityTest", "Error level")
    }

    /**
     * Verifica que los mensajes vacíos no causan problemas.
     */
    @Test
    fun testEmptyMessages() {
        KermitLogger.initialize()

        // Mensajes vacíos no deberían causar errores
        KermitLogger.debug("EmptyTest", "")
        KermitLogger.info("EmptyTest", "")
        KermitLogger.error("EmptyTest", "")
    }

    /**
     * Verifica que los tags vacíos no causan problemas.
     */
    @Test
    fun testEmptyTags() {
        KermitLogger.initialize()

        // Tags vacíos no deberían causar errores
        KermitLogger.debug("", "Message with empty tag")
        KermitLogger.info("", "Message with empty tag")
        KermitLogger.error("", "Message with empty tag")
    }

    /**
     * Verifica que los mensajes largos se manejan correctamente.
     */
    @Test
    fun testLongMessages() {
        KermitLogger.initialize()

        val longMessage = "A".repeat(1000)

        // Mensajes largos no deberían causar errores
        KermitLogger.debug("LongTest", longMessage)
        KermitLogger.info("LongTest", longMessage)
        KermitLogger.error("LongTest", longMessage)
    }

    /**
     * Verifica que los caracteres especiales se manejan correctamente.
     */
    @Test
    fun testSpecialCharacters() {
        KermitLogger.initialize()

        val specialMessage = "Special chars: \n\t\r\\ \"quotes\" 'single' émojis 🎉"

        // Caracteres especiales no deberían causar errores
        KermitLogger.debug("SpecialTest", specialMessage)
        KermitLogger.info("SpecialTest", specialMessage)
        KermitLogger.error("SpecialTest", specialMessage)
    }
}
