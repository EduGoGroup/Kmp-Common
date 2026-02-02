package com.edugo.test.module.platform

import co.touchlab.kermit.Logger

/**
 * Configuración de Kermit para Android.
 *
 * Usa LogcatWriter de Kermit que delega a android.util.Log.
 * Los logs aparecen en Logcat con el tag especificado.
 *
 * ## Características:
 * - Logs en Logcat (android.util.Log)
 * - Soporte de niveles estándar (DEBUG, INFO, WARN, ERROR)
 * - Filtrado por tag en Logcat
 * - Compatible con ProGuard/R8
 *
 * ## Uso:
 * ```kotlin
 * // En Application.onCreate()
 * class MyApplication : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         KermitLogger.initialize()
 *     }
 * }
 * ```
 */
actual object KermitConfig {
    /**
     * Crea un Logger de Kermit configurado para Android con LogcatWriter.
     *
     * En Android, Kermit 2.0+ usa automáticamente LogcatWriter que delega
     * a android.util.Log.
     *
     * @return Logger de Kermit configurado para Android Logcat
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
}
