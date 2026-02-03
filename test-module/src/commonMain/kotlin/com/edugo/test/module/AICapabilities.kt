package com.edugo.test.module

/**
 * Detecta capacidades de IA On-Device disponibles por plataforma.
 *
 * Android (API 34+): Gemini Nano via AICore
 * Android (API 29-33): ML Kit como fallback
 * Desktop/JS: Sin IA On-Device disponible
 */
public expect object AICapabilities {
    /**
     * Verifica si Gemini Nano esta disponible (Android 14+ con AICore).
     * @return true si Gemini Nano puede ser usado
     */
    public fun isGeminiNanoAvailable(): Boolean

    /**
     * Verifica si ML Kit esta disponible como fallback.
     * @return true si ML Kit puede ser usado
     */
    public fun isMLKitAvailable(): Boolean

    /**
     * Obtiene el proveedor de IA preferido segun la plataforma.
     * @return AIProvider indicando la mejor opcion disponible
     */
    public fun getPreferredAIProvider(): AIProvider
}

/**
 * Proveedores de IA disponibles.
 */
public enum class AIProvider {
    /** Google Gemini Nano via AICore (Android 14+) */
    GEMINI_NANO,
    /** Google ML Kit (Android 5+) */
    ML_KIT,
    /** Cloud API como fallback */
    CLOUD_API,
    /** Sin IA disponible */
    NONE
}
