package com.edugo.test.full.platform

import co.touchlab.kermit.Logger

/**
 * Configuración de Kermit para JavaScript (Browser y Node.js).
 *
 * Usa ConsoleWriter de Kermit que delega a console.log/warn/error.
 * Los logs aparecen en la consola del navegador o terminal de Node.js.
 *
 * ## Características:
 * - Browser: console.log, console.warn, console.error
 * - Node.js: process.stdout, process.stderr
 * - Formateo automático con timestamp y tag
 * - Compatible con DevTools del navegador
 *
 * ## Uso:
 * ```kotlin
 * // En el main() o punto de entrada
 * fun main() {
 *     KermitLogger.initialize()
 *     // ... resto de la aplicación
 * }
 * ```
 *
 * ## Salida por nivel:
 * - DEBUG/VERBOSE: console.log (color: gris en DevTools)
 * - INFO: console.info (color: azul en DevTools)
 * - WARN: console.warn (color: amarillo en DevTools)
 * - ERROR/ASSERT: console.error (color: rojo en DevTools)
 */
actual object KermitConfig {
    /**
     * Crea un Logger de Kermit configurado para JavaScript.
     *
     * En JS, Kermit 2.0+ usa automáticamente ConsoleWriter que delega
     * a console.log/warn/error en browser o Node.js.
     *
     * ## Comportamiento por plataforma:
     * - **Browser**: Logs aparecen en Console del navegador (F12/DevTools)
     * - **Node.js**: Logs aparecen en stdout/stderr de terminal
     *
     * @return Logger de Kermit configurado para JS console
     */
    actual fun createLogger(): Logger {
        return Logger.withTag("EduGo")
    }

    /**
     * Crea un Logger personalizado con tag específico.
     *
     * Útil para módulos o componentes específicos.
     *
     * @param tag Tag base para los logs
     * @return Logger de Kermit configurado
     *
     * Ejemplo:
     * ```kotlin
     * val networkLogger = KermitConfig.createCustomLogger("Network")
     * val authLogger = KermitConfig.createCustomLogger("Auth")
     * ```
     */
    fun createCustomLogger(
        tag: String = "EduGo"
    ): Logger {
        return Logger.withTag(tag)
    }
}
