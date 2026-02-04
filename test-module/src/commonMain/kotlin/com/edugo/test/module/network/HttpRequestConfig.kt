package com.edugo.test.module.network

/**
 * Configuración para requests HTTP individuales.
 *
 * Permite personalizar headers, query parameters y content type por cada request,
 * sin afectar la configuración global del cliente HTTP.
 *
 * ## Uso básico
 *
 * ```kotlin
 * // Usar configuración por defecto
 * val response = client.get<User>(url)
 *
 * // Con headers personalizados
 * val config = HttpRequestConfig.builder()
 *     .header("Authorization", "Bearer token123")
 *     .queryParam("page", "1")
 *     .build()
 * val response = client.get<User>(url, config)
 * ```
 *
 * @property headers Headers HTTP adicionales para el request
 * @property queryParams Query parameters a añadir a la URL
 * @property contentType Content-Type del request (default: application/json)
 */
public data class HttpRequestConfig(
    val headers: Map<String, String> = emptyMap(),
    val queryParams: Map<String, String> = emptyMap(),
    val contentType: String = "application/json"
) {
    /**
     * Builder para construir [HttpRequestConfig] de forma fluida.
     *
     * ```kotlin
     * val config = HttpRequestConfig.builder()
     *     .header("X-Request-ID", "abc123")
     *     .header("Authorization", "Bearer token")
     *     .queryParam("page", "1")
     *     .queryParam("limit", "10")
     *     .contentType("application/json")
     *     .build()
     * ```
     */
    public class Builder {
        private val headers = mutableMapOf<String, String>()
        private val queryParams = mutableMapOf<String, String>()
        private var contentType = "application/json"

        /**
         * Añade un header al request.
         *
         * @param key Nombre del header
         * @param value Valor del header
         * @return Este builder para encadenar llamadas
         */
        public fun header(key: String, value: String): Builder = apply { headers[key] = value }

        /**
         * Añade un query parameter a la URL.
         *
         * @param key Nombre del parámetro
         * @param value Valor del parámetro
         * @return Este builder para encadenar llamadas
         */
        public fun queryParam(key: String, value: String): Builder = apply { queryParams[key] = value }

        /**
         * Establece el Content-Type del request.
         *
         * @param type Valor del Content-Type (ej: "application/json", "text/plain")
         * @return Este builder para encadenar llamadas
         */
        public fun contentType(type: String): Builder = apply { contentType = type }

        /**
         * Construye la configuración inmutable.
         *
         * @return Nueva instancia de [HttpRequestConfig]
         */
        public fun build(): HttpRequestConfig = HttpRequestConfig(
            headers = headers.toMap(),
            queryParams = queryParams.toMap(),
            contentType = contentType
        )
    }

    public companion object {
        /**
         * Configuración por defecto sin headers ni query params adicionales.
         * Content-Type: application/json
         */
        public val Default: HttpRequestConfig = HttpRequestConfig()

        /**
         * Crea un nuevo builder para configuración personalizada.
         *
         * @return Nuevo [Builder]
         */
        public fun builder(): Builder = Builder()
    }
}
