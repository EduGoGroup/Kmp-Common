package com.edugo.test.module

import android.os.Build

/**
 * Implementación Android de AICapabilities.
 *
 * Detecta:
 * - Gemini Nano: Android 14+ (API 34) con AICore service
 * - ML Kit: Android 5+ (API 21) - siempre disponible en minSdk 29
 *
 * NOTA: Implementación simplificada para Sprint 1 (Setup Base KMP - Sourcesets)
 * Ver Documents_Analisys/SOURCESETS-ARCHITECTURE.md para detalles de arquitectura.
 */
actual object AICapabilities {

    // API level mínimo para Gemini Nano (Android 14)
    private const val GEMINI_NANO_MIN_API = 34

    // API level mínimo para ML Kit
    // NOTA: Valor 21 mantenido por documentación, pero redundante dado minSdk = 29
    @Suppress("unused") // Mantener por claridad documental
    private const val ML_KIT_MIN_API = 21

    /**
     * Verifica disponibilidad de Gemini Nano.
     *
     * IMPLEMENTACIÓN ACTUAL (Sprint 1):
     * - Detección básica por API level >= 34
     * - Suficiente para configuración inicial de sourcesets
     *
     * TODO (Sprints futuros): Implementar verificación completa de AICore service
     *   Métodos sugeridos:
     *   1. PackageManager.hasSystemFeature("android.software.on_device_inference")
     *   2. Consultar AICore API directamente para verificar disponibilidad
     *   3. Verificar cuota/límites de uso de Gemini Nano
     *   Referencia: Documents_Analisys/SOURCESETS-ARCHITECTURE.md sección AICapabilities
     *
     * @return true si API level >= 34 (aproximación válida para este sprint)
     */
    actual fun isGeminiNanoAvailable(): Boolean {
        // Verificar API level como condición necesaria (pero no suficiente a largo plazo)
        if (Build.VERSION.SDK_INT < GEMINI_NANO_MIN_API) {
            return false
        }

        // Retornar true para API 34+ como implementación simplificada
        // La verificación completa de AICore se implementará en sprints futuros
        return true
    }

    /**
     * Verifica disponibilidad de ML Kit.
     *
     * ML Kit está disponible desde API 21+, y nuestro minSdk es 29,
     * por lo que esta función siempre retornará true en producción.
     *
     * NOTA: Se mantiene la constante ML_KIT_MIN_API (21) por claridad documental,
     * aunque podría simplificarse a `return true` dado que minSdk = 29.
     *
     * @return true si API level >= 21 (siempre true con minSdk 29)
     */
    actual fun isMLKitAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= ML_KIT_MIN_API
    }

    /**
     * Obtiene el proveedor de IA preferido según disponibilidad.
     *
     * Jerarquía de preferencia:
     * 1. GEMINI_NANO - Mejor rendimiento, On-Device, Android 14+
     * 2. ML_KIT - Disponible en API 21+, menor capacidad que Gemini
     * 3. CLOUD_API - Fallback si no hay opciones On-Device
     *
     * @return Provider de IA recomendado según capacidades del dispositivo
     */
    actual fun getPreferredAIProvider(): AIProvider {
        return when {
            isGeminiNanoAvailable() -> AIProvider.GEMINI_NANO
            isMLKitAvailable() -> AIProvider.ML_KIT
            else -> AIProvider.CLOUD_API
        }
    }
}
