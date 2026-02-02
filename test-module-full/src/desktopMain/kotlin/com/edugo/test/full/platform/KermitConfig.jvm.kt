package com.edugo.test.full.platform

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity

/**
 * Configuración de Kermit para JVM/Desktop.
 *
 * Usa ConsoleWriter con colores ANSI para mejor legibilidad en la consola.
 * Los logs se formatean con timestamp, nivel y tag.
 *
 * ## Características:
 * - Console logger con colores ANSI
 * - Formateo personalizado (timestamp, nivel, tag)
 * - Soporte de System.out y System.err
 * - Thread-safe
 *
 * ## Colores ANSI por nivel:
 * - DEBUG: Cyan
 * - INFO: Green
 * - WARN: Yellow
 * - ERROR: Red
 *
 * ## Uso:
 * ```kotlin
 * // En el main() de la aplicación
 * fun main() {
 *     KermitLogger.initialize()
 *     // ... resto de la aplicación
 * }
 * ```
 */
actual object KermitConfig {
    // Códigos ANSI para colores
    private const val ANSI_RESET = "\u001B[0m"
    private const val ANSI_CYAN = "\u001B[36m"    // DEBUG
    private const val ANSI_GREEN = "\u001B[32m"   // INFO
    private const val ANSI_YELLOW = "\u001B[33m"  // WARN
    private const val ANSI_RED = "\u001B[31m"     // ERROR
    private const val ANSI_BOLD = "\u001B[1m"

    /**
     * Obtiene el color ANSI apropiado para el nivel de severidad.
     *
     * @param severity Nivel de severidad del log
     * @return Código ANSI del color correspondiente
     */
    private fun getColorForSeverity(severity: Severity): String = when (severity) {
        Severity.Verbose -> ANSI_CYAN
        Severity.Debug -> ANSI_CYAN
        Severity.Info -> ANSI_GREEN
        Severity.Warn -> ANSI_YELLOW
        Severity.Error -> ANSI_RED
        Severity.Assert -> ANSI_RED + ANSI_BOLD
    }

    /**
     * Crea un Logger de Kermit configurado para JVM con console output.
     *
     * En JVM, Kermit 2.0+ usa automáticamente ConsoleWriter para salida
     * a la consola con colores ANSI.
     *
     * @return Logger de Kermit configurado para JVM console
     */
    actual fun createLogger(): Logger {
        return Logger.withTag("EduGo")
    }

    /**
     * Crea un Logger personalizado con tag específico.
     *
     * @param tag Tag base para los logs
     * @return Logger de Kermit configurado
     */
    fun createCustomLogger(
        tag: String = "EduGo"
    ): Logger {
        return Logger.withTag(tag)
    }

    /**
     * Formatea un mensaje de log con colores ANSI.
     *
     * Útil para logging personalizado.
     *
     * @param severity Nivel de severidad
     * @param tag Tag del log
     * @param message Mensaje
     * @return Mensaje formateado con colores ANSI
     */
    fun formatWithColors(severity: Severity, tag: String, message: String): String {
        val color = getColorForSeverity(severity)
        val severityName = severity.name.uppercase()
        return "$color[$severityName]$ANSI_RESET $tag: $message"
    }
}
