package com.edugo.test.module

import android.os.Build

/**
 * Implementacion Android de AICapabilities.
 *
 * Detecta:
 * - Gemini Nano: Android 14+ (API 34) con AICore service
 * - ML Kit: Android 5+ (API 21) - siempre disponible en minSdk 29
 */
actual object AICapabilities {

    // API level minimo para Gemini Nano (Android 14)
    private const val GEMINI_NANO_MIN_API = 34

    // API level minimo para ML Kit (nuestro minSdk es 29)
    private const val ML_KIT_MIN_API = 21

    actual fun isGeminiNanoAvailable(): Boolean {
        // Verificar API level
        if (Build.VERSION.SDK_INT < GEMINI_NANO_MIN_API) {
            return false
        }

        // TODO: Verificar si AICore service esta disponible
        // Esto requiere consultar PackageManager o AICore API
        // Por ahora, asumimos disponible en API 34+
        return true
    }

    actual fun isMLKitAvailable(): Boolean {
        // ML Kit disponible en API 21+, nuestro minSdk es 29
        return Build.VERSION.SDK_INT >= ML_KIT_MIN_API
    }

    actual fun getPreferredAIProvider(): AIProvider {
        return when {
            isGeminiNanoAvailable() -> AIProvider.GEMINI_NANO
            isMLKitAvailable() -> AIProvider.ML_KIT
            else -> AIProvider.CLOUD_API
        }
    }
}
