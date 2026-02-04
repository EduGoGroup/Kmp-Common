package com.edugo.test.module

/**
 * JavaScript implementation of AICapabilities.
 *
 * JavaScript (Browser/Node.js) does not have native On-Device AI capabilities.
 * Cloud API is recommended for AI functionalities.
 */
public actual object AICapabilities {

    actual fun isGeminiNanoAvailable(): Boolean = false

    actual fun isMLKitAvailable(): Boolean = false

    actual fun getPreferredAIProvider(): AIProvider = AIProvider.CLOUD_API
}
