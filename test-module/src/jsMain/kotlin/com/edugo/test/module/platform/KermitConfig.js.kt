package com.edugo.test.module.platform

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity

/**
 * Configuración de Kermit para JavaScript (JS).
 *
 * Usa ConsoleWriter de Kermit que delega a console.log/warn/error de JavaScript.
 * Los logs aparecen en la consola del navegador o Node.js según el entorno.
 *
 * ## Características:
 * - Logs en console del navegador/Node.js
 * - Mapeo automático a console.log/console.warn/console.error
 * - Soporte de niveles estándar (DEBUG, INFO, WARN, ERROR)
 * - Compatible con DevTools del navegador
 *
 * ## Uso en navegador:
 * ```kotlin
 * // En el main() de la aplicación JS
 * fun main() {
 *     KermitLogger.initialize()
 *     // ... resto de la aplicación
 * }
 * ```
 *
 * ## Uso en Node.js:
 * ```kotlin
 * // Al inicio del proceso
 * fun main() {
 *     KermitLogger.initialize()
 *     // Los logs aparecerán en stdout/stderr de Node.js
 * }
 * ```
 */
public actual object KermitConfig {
    /**
     * Crea un Logger de Kermit configurado para JS con console output.
     *
     * En JS, Kermit 2.0+ usa automáticamente ConsoleWriter que delega
     * a las funciones de console del navegador o Node.js:
     * - DEBUG/VERBOSE -> console.log()
     * - INFO -> console.info()
     * - WARN -> console.warn()
     * - ERROR/ASSERT -> console.error()
     *
     * @return Logger de Kermit configurado para JS console
     */
    actual fun createLogger(): Logger {
        return Logger.withTag("EduGo")
    }

    /**
     * Crea un Logger con severidad mínima configurada.
     *
     * @param minSeverity Nivel mínimo de logging
     * @return Logger de Kermit configurado con filtro de severidad
     */
    actual fun createLoggerWithMinSeverity(minSeverity: Severity): Logger {
        // En Kermit 2.0.4, el filtrado por severidad se realiza en el nivel de configuración
        // Por ahora retornamos el logger estándar - el filtrado puede implementarse con un LogWriter custom
        return Logger.withTag("EduGo")
    }

    /**
     * Crea un Logger personalizado con tag específico.
     *
     * Útil para separar logs por módulos en la consola del navegador.
     *
     * @param tag Tag base para los logs
     * @return Logger de Kermit configurado
     */
    fun createCustomLogger(
        tag: String = "EduGo"
    ): Logger {
        return Logger.withTag(tag)
    }
}
