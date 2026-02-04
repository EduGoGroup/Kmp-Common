package com.edugo.test.module.network

import kotlin.time.Duration

/**
 * Configuración para requests HTTP individuales.
 *
 * Permite personalizar headers, query parameters, content type y timeouts por cada request,
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
 *
 * // Con timeout override
 * val config = HttpRequestConfig.builder()
 *     .requestTimeout(5.seconds)
 *     .build()
 * val response = client.get<User>(url, config)
 * ```
 *
 * @property headers Headers HTTP adicionales para el request
 * @property queryParams Query parameters a añadir a la URL
 * @property contentType Content-Type del request (default: application/json)
 * @property connectTimeout Timeout de conexión override (null = usar default del cliente)
 * @property requestTimeout Timeout total del request override (null = usar default del cliente)
 * @property socketTimeout Timeout de socket override (null = usar default del cliente)
 */
public data class HttpRequestConfig(
    val headers: Map<String, String> = emptyMap(),
    val queryParams: Map<String, String> = emptyMap(),
    val contentType: String = "application/json",
    val connectTimeout: Duration? = null,
    val requestTimeout: Duration? = null,
    val socketTimeout: Duration? = null
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
        private var connectTimeout: Duration? = null
        private var requestTimeout: Duration? = null
        private var socketTimeout: Duration? = null

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
         * Establece timeout de conexión para este request específico.
         *
         * @param timeout Duración del timeout de conexión
         * @return Este builder para encadenar llamadas
         */
        public fun connectTimeout(timeout: Duration): Builder = apply { connectTimeout = timeout }

        /**
         * Establece timeout total del request para este request específico.
         *
         * @param timeout Duración del timeout de request
         * @return Este builder para encadenar llamadas
         */
        public fun requestTimeout(timeout: Duration): Builder = apply { requestTimeout = timeout }

        /**
         * Establece timeout de socket para este request específico.
         *
         * @param timeout Duración del timeout de socket
         * @return Este builder para encadenar llamadas
         */
        public fun socketTimeout(timeout: Duration): Builder = apply { socketTimeout = timeout }

        /**
         * Establece múltiples timeouts a la vez.
         *
         * @param connect Timeout de conexión (null = no override)
         * @param request Timeout total del request (null = no override)
         * @param socket Timeout de socket (null = no override)
         * @return Este builder para encadenar llamadas
         */
        public fun timeouts(
            connect: Duration? = null,
            request: Duration? = null,
            socket: Duration? = null
        ): Builder = apply {
            connect?.let { connectTimeout = it }
            request?.let { requestTimeout = it }
            socket?.let { socketTimeout = it }
        }

        /**
         * Construye la configuración inmutable.
         *
         * @return Nueva instancia de [HttpRequestConfig]
         */
        public fun build(): HttpRequestConfig = HttpRequestConfig(
            headers = headers.toMap(),
            queryParams = queryParams.toMap(),
            contentType = contentType,
            connectTimeout = connectTimeout,
            requestTimeout = requestTimeout,
            socketTimeout = socketTimeout
        )
    }

    /**
     * Verifica si hay algún timeout override configurado.
     *
     * @return true si al menos un timeout está configurado, false si todos son null
     */
    public fun hasTimeoutOverride(): Boolean =
        connectTimeout != null || requestTimeout != null || socketTimeout != null

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
