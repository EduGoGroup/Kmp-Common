package com.edugo.test.module.network

import com.edugo.test.module.config.JsonConfig
import com.edugo.test.module.network.interceptor.*
import com.edugo.test.module.network.retry.RetryConfig
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Builder para crear EduGoHttpClient con configuración avanzada.
 *
 * Proporciona una API fluida para configurar:
 * - Timeouts granulares (connect, request, socket)
 * - Retry automático con backoff exponencial
 * - Interceptores personalizados
 * - Logging con sanitización
 *
 * ## Uso básico
 *
 * ```kotlin
 * val client = EduGoHttpClientBuilder()
 *     .connectTimeout(10.seconds)
 *     .requestTimeout(30.seconds)
 *     .retry(RetryConfig.Default)
 *     .interceptor(HeadersInterceptor.jsonDefaults())
 *     .interceptor(AuthInterceptor(tokenProvider))
 *     .logging(LogLevel.INFO)
 *     .build()
 * ```
 *
 * ## Presets
 *
 * ```kotlin
 * // Producción (sin logging, retry habilitado)
 * val prodClient = EduGoHttpClientBuilder.production().build()
 *
 * // Desarrollo (logging verbose, retry habilitado)
 * val devClient = EduGoHttpClientBuilder.development().build()
 * ```
 *
 * @see EduGoHttpClient
 * @see RetryConfig
 * @see Interceptor
 */
public class EduGoHttpClientBuilder {
    private var connectTimeout: Duration = 30.seconds
    private var requestTimeout: Duration = 60.seconds
    private var socketTimeout: Duration = 60.seconds
    private var retryConfig: RetryConfig = RetryConfig.NoRetry
    private var logLevel: LogLevel = LogLevel.NONE
    private var networkLogger: Logger = NetworkLogger.Default
    private val interceptors = mutableListOf<Interceptor>()

    /**
     * Establece el timeout de conexión.
     *
     * @param timeout Duración máxima para establecer conexión
     * @return Este builder para encadenar llamadas
     */
    public fun connectTimeout(timeout: Duration): EduGoHttpClientBuilder = apply {
        connectTimeout = timeout
    }

    /**
     * Establece el timeout total del request.
     *
     * @param timeout Duración máxima para el request completo
     * @return Este builder para encadenar llamadas
     */
    public fun requestTimeout(timeout: Duration): EduGoHttpClientBuilder = apply {
        requestTimeout = timeout
    }

    /**
     * Establece el timeout de socket.
     *
     * @param timeout Duración máxima de inactividad en socket
     * @return Este builder para encadenar llamadas
     */
    public fun socketTimeout(timeout: Duration): EduGoHttpClientBuilder = apply {
        socketTimeout = timeout
    }

    /**
     * Establece múltiples timeouts a la vez.
     *
     * @param connect Timeout de conexión (default: 30s)
     * @param request Timeout total de request (default: 60s)
     * @param socket Timeout de socket (default: 60s)
     * @return Este builder para encadenar llamadas
     */
    public fun timeouts(
        connect: Duration = 30.seconds,
        request: Duration = 60.seconds,
        socket: Duration = 60.seconds
    ): EduGoHttpClientBuilder = apply {
        connectTimeout = connect
        requestTimeout = request
        socketTimeout = socket
    }

    /**
     * Configura retry automático.
     *
     * @param config Configuración de retry (maxRetries, backoff, etc.)
     * @return Este builder para encadenar llamadas
     * @see RetryConfig
     */
    public fun retry(config: RetryConfig): EduGoHttpClientBuilder = apply {
        retryConfig = config
    }

    /**
     * Desactiva retry automático.
     *
     * @return Este builder para encadenar llamadas
     */
    public fun noRetry(): EduGoHttpClientBuilder = apply {
        retryConfig = RetryConfig.NoRetry
    }

    /**
     * Configura logging de requests/responses.
     *
     * @param level Nivel de logging (NONE, INFO, HEADERS, BODY, ALL)
     * @param logger Logger personalizado (default: NetworkLogger con sanitización)
     * @return Este builder para encadenar llamadas
     */
    public fun logging(
        level: LogLevel,
        logger: Logger = NetworkLogger.Default
    ): EduGoHttpClientBuilder = apply {
        logLevel = level
        networkLogger = logger
    }

    /**
     * Agrega un interceptor a la cadena.
     *
     * Los interceptores se ejecutan en el orden en que se agregan,
     * ordenados internamente por su propiedad `order`.
     *
     * @param interceptor Interceptor a agregar
     * @return Este builder para encadenar llamadas
     * @see Interceptor
     */
    public fun interceptor(interceptor: Interceptor): EduGoHttpClientBuilder = apply {
        interceptors.add(interceptor)
    }

    /**
     * Agrega múltiples interceptores a la vez.
     *
     * @param interceptors Interceptores a agregar
     * @return Este builder para encadenar llamadas
     */
    public fun interceptors(vararg interceptors: Interceptor): EduGoHttpClientBuilder = apply {
        this.interceptors.addAll(interceptors)
    }

    /**
     * Construye el cliente HTTP configurado.
     *
     * Crea un HttpClient con todos los plugins configurados:
     * - ContentNegotiation (JSON)
     * - HttpTimeout
     * - Logging (si logLevel != NONE)
     * - HttpRequestRetry (si maxRetries > 0)
     *
     * Y una cadena de interceptores ordenados.
     *
     * @return Nueva instancia de [EduGoHttpClient] configurada
     */
    public fun build(): EduGoHttpClient {
        val chain = InterceptorChain(interceptors.toList())

        val httpClient = HttpClient(createPlatformEngine()) {
            install(ContentNegotiation) {
                json(JsonConfig.Default)
            }

            install(HttpTimeout) {
                connectTimeoutMillis = connectTimeout.inWholeMilliseconds
                requestTimeoutMillis = requestTimeout.inWholeMilliseconds
                socketTimeoutMillis = socketTimeout.inWholeMilliseconds
            }

            if (logLevel != LogLevel.NONE) {
                install(Logging) {
                    logger = networkLogger
                    level = logLevel
                }
            }

            if (retryConfig.maxRetries > 0) {
                install(HttpRequestRetry) {
                    maxRetries = retryConfig.maxRetries
                    retryIf { _, response ->
                        retryConfig.retryOnStatusCode(response.status.value)
                    }
                    retryOnExceptionIf { _, cause ->
                        retryConfig.retryOnException(cause)
                    }
                    delayMillis { retry ->
                        retryConfig.calculateDelay(retry).inWholeMilliseconds
                    }
                }
            }
        }

        return EduGoHttpClient(httpClient, chain)
    }

    public companion object {
        /**
         * Configuración por defecto para producción.
         *
         * - Timeouts: 30s conexión, 60s request
         * - Retry habilitado (3 intentos con backoff exponencial)
         * - Sin logging (seguridad)
         *
         * @return Builder preconfigurado para producción
         */
        public fun production(): EduGoHttpClientBuilder = EduGoHttpClientBuilder()
            .timeouts(connect = 30.seconds, request = 60.seconds)
            .retry(RetryConfig.Default)
            .logging(LogLevel.NONE)

        /**
         * Configuración para desarrollo con logging verbose.
         *
         * - Timeouts: 30s conexión, 60s request
         * - Retry habilitado (3 intentos con backoff exponencial)
         * - Logging INFO con sanitización automática
         *
         * @return Builder preconfigurado para desarrollo
         */
        public fun development(): EduGoHttpClientBuilder = EduGoHttpClientBuilder()
            .timeouts(connect = 30.seconds, request = 60.seconds)
            .retry(RetryConfig.Default)
            .logging(LogLevel.INFO)
    }
}
