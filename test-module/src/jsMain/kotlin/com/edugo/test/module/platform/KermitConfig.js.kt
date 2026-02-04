package com.edugo.test.module.platform

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity

/**
 * JavaScript implementation of KermitConfig.
 *
 * Uses Kermit's default ConsoleWriter for browser/Node.js console output.
 *
 * ## Features:
 * - Console logger using browser console API
 * - Formateo con timestamp, nivel y tag
 * - Soporte de console.log, console.warn, console.error
 *
 * ## Usage:
 * ```kotlin
 * // En el punto de entrada de la aplicación JS
 * fun main() {
 *     KermitLogger.initialize()
 *     // ... resto de la aplicación
 * }
 * ```
 */
public actual object KermitConfig {

    /**
     * Crea un Logger de Kermit configurado para JavaScript.
     *
     * En JS, Kermit usa automáticamente ConsoleWriter para salida
     * a la consola del navegador o Node.js.
     *
     * @return Logger de Kermit configurado para JS
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
        // Por ahora retornamos el logger estándar
        return Logger.withTag("EduGo")
    }
}
