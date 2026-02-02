package com.edugo.test.full.platform

import co.touchlab.kermit.Logger as KermitLoggerImpl
import co.touchlab.kermit.Severity

/**
 * Wrapper/alternativa al Logger expect/actual que usa Kermit 2.0.4 como backend.
 *
 * Este wrapper permite usar Kermit como sistema de logging mientras mantiene
 * compatibilidad con la interfaz Logger existente. Kermit proporciona
 * logging multiplataforma con configuraciones específicas por plataforma.
 *
 * ## Características:
 * - Android: Usa Logcat para logging
 * - JVM: Console logger con colores ANSI
 * - JS: Console logger (console.log/warn/error)
 * - Formatters personalizables (timestamp, thread, class)
 * - Múltiples loggers simultáneos
 *
 * ## Uso:
 * ```kotlin
 * // Inicializar Kermit con configuración específica por plataforma
 * KermitLogger.initialize()
 *
 * // Usar el wrapper para logging
 * KermitLogger.debug("NetworkClient", "Request sent")
 * KermitLogger.info("AuthManager", "User logged in")
 * KermitLogger.error("Repository", "Failed to save", exception)
 * ```
 *
 * @see KermitConfig Configuración específica por plataforma
 */
object KermitLogger {
    private var kermitInstance: KermitLoggerImpl = KermitLoggerImpl

    /**
     * Inicializa Kermit con la configuración específica de la plataforma.
     *
     * Debe llamarse una vez al inicio de la aplicación, preferiblemente
     * en el punto de entrada de cada plataforma (Application.onCreate en Android,
     * main() en JVM, etc.).
     *
     * La configuración específica se obtiene de KermitConfig expect/actual.
     */
    fun initialize() {
        kermitInstance = KermitConfig.createLogger()
    }

    /**
     * Permite configurar un logger personalizado de Kermit.
     *
     * Útil para testing o configuraciones avanzadas.
     *
     * @param logger Instancia personalizada de Kermit Logger
     */
    fun setLogger(logger: KermitLoggerImpl) {
        kermitInstance = logger
    }

    /**
     * Obtiene la instancia actual de Kermit Logger.
     *
     * @return Instancia de Kermit Logger configurada
     */
    fun getLogger(): KermitLoggerImpl = kermitInstance

    /**
     * Logs a debug message.
     *
     * Mapea al nivel DEBUG de Kermit.
     *
     * @param tag Identificador de la fuente del log (nombre de clase)
     * @param message Mensaje a registrar
     */
    fun debug(tag: String, message: String) {
        kermitInstance.d(tag = tag) { message }
    }

    /**
     * Logs an informational message.
     *
     * Mapea al nivel INFO de Kermit.
     *
     * @param tag Identificador de la fuente del log (nombre de clase)
     * @param message Mensaje a registrar
     */
    fun info(tag: String, message: String) {
        kermitInstance.i(tag = tag) { message }
    }

    /**
     * Logs an error message with optional exception.
     *
     * Mapea al nivel ERROR de Kermit.
     *
     * @param tag Identificador de la fuente del log (nombre de clase)
     * @param message Mensaje de error a registrar
     * @param throwable Excepción opcional a incluir con stack trace
     */
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        kermitInstance.e(tag = tag, throwable = throwable) { message }
    }

    /**
     * Configura el nivel mínimo de severidad para logging.
     *
     * @param severity Nivel mínimo de Kermit (Verbose, Debug, Info, Warn, Error, Assert)
     */
    fun setMinSeverity(severity: Severity) {
        kermitInstance = kermitInstance.withTag("").apply {
            // Kermit 2.0.4 maneja la severidad mínima a través de la configuración
            // Esta función es un placeholder para futuras extensiones
        }
    }
}

/**
 * Configuración de Kermit específica por plataforma.
 *
 * Implementado mediante expect/actual para cada target:
 * - Android: LogcatWriter
 * - JVM: ConsoleWriter con ANSI colors
 * - JS: ConsoleWriter
 *
 * @see KermitConfig.android.kt
 * @see KermitConfig.jvm.kt
 * @see KermitConfig.js.kt
 */
expect object KermitConfig {
    /**
     * Crea y configura una instancia de Kermit Logger específica para la plataforma.
     *
     * @return Logger de Kermit configurado con writers y formatters apropiados
     */
    fun createLogger(): KermitLoggerImpl
}
