package com.edugo.test.module.network

/**
 * Sanitiza información sensible en logs HTTP.
 * Oculta tokens, passwords, API keys y otros datos confidenciales.
 */
public object LogSanitizer {

    private val SENSITIVE_HEADERS = setOf(
        "authorization",
        "x-api-key",
        "x-auth-token",
        "cookie",
        "set-cookie"
    )

    private val SENSITIVE_BODY_KEYS = listOf(
        "password",
        "token",
        "secret",
        "api_key",
        "apiKey",
        "access_token",
        "refresh_token",
        "credentials"
    )

    private const val MASKED_VALUE = "[REDACTED]"

    /**
     * Sanitiza headers HTTP ocultando valores sensibles.
     *
     * @param headers Map de headers HTTP
     * @return Map con valores sensibles reemplazados por [REDACTED]
     */
    public fun sanitizeHeaders(headers: Map<String, String>): Map<String, String> {
        return headers.mapValues { (key, value) ->
            if (key.lowercase() in SENSITIVE_HEADERS) MASKED_VALUE else value
        }
    }

    /**
     * Sanitiza header individual.
     *
     * @param key Nombre del header
     * @param value Valor del header
     * @return Valor sanitizado si es sensible, o el valor original
     */
    public fun sanitizeHeader(key: String, value: String): String {
        return if (key.lowercase() in SENSITIVE_HEADERS) MASKED_VALUE else value
    }

    /**
     * Sanitiza body JSON ocultando campos sensibles.
     * Usa regex simple para no depender de parsing JSON completo.
     *
     * @param body String con contenido JSON
     * @return Body con campos sensibles reemplazados por [REDACTED]
     */
    public fun sanitizeBody(body: String): String {
        var sanitized = body
        SENSITIVE_BODY_KEYS.forEach { key ->
            // Patrón para "key": "value" o "key":"value"
            val regex = Regex(""""$key"\s*:\s*"[^"]*"""", RegexOption.IGNORE_CASE)
            sanitized = sanitized.replace(regex, """"$key": "$MASKED_VALUE"""")
        }
        return sanitized
    }

    /**
     * Sanitiza URL ocultando query params sensibles.
     *
     * @param url URL completa con query params
     * @return URL con query params sensibles reemplazados por [REDACTED]
     */
    public fun sanitizeUrl(url: String): String {
        var sanitized = url
        SENSITIVE_BODY_KEYS.forEach { key ->
            val regex = Regex("""([?&])$key=[^&]*""", RegexOption.IGNORE_CASE)
            sanitized = sanitized.replace(regex, """$1$key=$MASKED_VALUE""")
        }
        return sanitized
    }
}
