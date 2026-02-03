package com.edugo.test.module

/**
 * Implementacion Desktop de AICapabilities.
 *
 * Desktop no tiene IA On-Device nativa disponible.
 * Se recomienda usar Cloud API para funcionalidades de IA.
 */
public actual object AICapabilities {

    actual fun isGeminiNanoAvailable(): Boolean = false

    actual fun isMLKitAvailable(): Boolean = false

    actual fun getPreferredAIProvider(): AIProvider = AIProvider.CLOUD_API
}
