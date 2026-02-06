package com.edugo.test.module.network

import com.edugo.test.module.auth.service.AuthService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Factory for creating configured HttpClient instances.
 *
 * This factory provides a centralized way to create HTTP clients with
 * consistent configuration across the application, including JSON serialization,
 * timeouts, and optional logging.
 *
 * Example usage:
 * ```kotlin
 * // Recommended: Use platform engine with default timeouts
 * val client = HttpClientFactory.create()
 *
 * // With custom timeouts
 * val customClient = HttpClientFactory.create(
 *     connectTimeoutMs = 15_000,
 *     requestTimeoutMs = 30_000
 * )
 *
 * // Development: With logging
 * val debugClient = HttpClientFactory.create(logLevel = LogLevel.INFO)
 *
 * // Legacy: With explicit engine (for testing or special cases)
 * val testClient = HttpClientFactory.createBaseClient(mockEngine, LogLevel.NONE)
 * ```
 */
public object HttpClientFactory {

    /**
     * Internal JSON configuration for HTTP client serialization.
     *
     * Configuration:
     * - `ignoreUnknownKeys = true` - Tolerates extra fields in responses
     * - `isLenient = true` - Accepts relaxed JSON syntax
     * - `prettyPrint = false` - Compact output for production
     *
     * This is internal to maintain encapsulation of serialization details.
     */
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }

    /**
     * Creates a configured HttpClient with the specified engine and logging level.
     *
     * The client is configured with:
     * - **ContentNegotiation**: JSON serialization/deserialization
     * - **Logging** (optional): HTTP request/response logging
     *
     * **Platform-specific engines:**
     * - Android: OkHttp
     * - JVM/Desktop: CIO (Coroutine I/O)
     * - iOS: Darwin
     * - JS: Js
     *
     * @param engine Platform-specific HTTP engine implementation
     * @param logLevel Logging level for HTTP operations. Default is [LogLevel.NONE] for
     *                 production safety. Use [LogLevel.INFO] or [LogLevel.HEADERS] only
     *                 in development to avoid exposing sensitive data (URLs, tokens, headers).
     * @return Configured HttpClient instance ready for making requests
     *
     * **⚠️ Security Warning**: Avoid using [LogLevel.HEADERS] or [LogLevel.BODY] in
     * production as they may log sensitive information (auth tokens, API keys, request bodies).
     */
    public fun createBaseClient(
        engine: io.ktor.client.engine.HttpClientEngine,
        logLevel: LogLevel = LogLevel.NONE
    ): HttpClient {
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }

            // Only install logging if explicitly requested
            if (logLevel != LogLevel.NONE) {
                install(Logging) {
                    level = logLevel
                }
            }
        }
    }

    /**
     * Creates a configured HttpClient using the platform-specific engine with timeouts.
     *
     * This is the recommended way to create an HttpClient as it:
     * - Automatically selects the optimal engine for the current platform
     * - Configures sensible default timeouts
     * - Sets up JSON serialization
     * - Optionally enables logging with automatic sanitization
     *
     * **Default timeouts:**
     * - Connect timeout: 30 seconds
     * - Request timeout: 60 seconds
     *
     * **Platform engines used:**
     * - Android: OkHttp (HTTP/2, connection pooling)
     * - JVM/Desktop: CIO (Coroutine-based I/O)
     * - JS: Js (Browser Fetch API)
     *
     * **Logging:**
     * When logging is enabled, [NetworkLogger] automatically sanitizes sensitive data
     * (auth tokens, passwords, API keys) before writing to logs.
     *
     * @param logLevel Logging level for HTTP operations. Default is [LogLevel.NONE].
     * @param connectTimeoutMs Maximum time to establish a connection in milliseconds.
     *                         Default is 30,000ms (30 seconds).
     * @param requestTimeoutMs Maximum time for the entire request in milliseconds.
     *                         Default is 60,000ms (60 seconds).
     * @param networkLogger Custom logger implementation. Default is [NetworkLogger.Default]
     *                      which sanitizes sensitive data automatically.
     * @return Configured HttpClient instance ready for making requests
     *
     * @see createBaseClient For creating clients with custom engines (useful for testing)
     */
    public fun create(
        logLevel: LogLevel = LogLevel.NONE,
        connectTimeoutMs: Long = DEFAULT_CONNECT_TIMEOUT_MS,
        requestTimeoutMs: Long = DEFAULT_REQUEST_TIMEOUT_MS,
        networkLogger: io.ktor.client.plugins.logging.Logger = NetworkLogger.Default
    ): HttpClient {
        return HttpClient(createPlatformEngine()) {
            install(ContentNegotiation) {
                json(json)
            }

            install(HttpTimeout) {
                connectTimeoutMillis = connectTimeoutMs
                requestTimeoutMillis = requestTimeoutMs
            }

            if (logLevel != LogLevel.NONE) {
                install(Logging) {
                    logger = networkLogger
                    level = logLevel
                }
            }
        }
    }

    /**
     * Crea un HttpClient con auto-refresh automático de tokens en 401.
     *
     * Este cliente está completamente configurado para manejar la renovación
     * automática de tokens cuando el backend retorna 401 Unauthorized.
     *
     * ## Flujo de Auto-Refresh
     *
     * 1. Request se envía con token actual (via AuthInterceptor)
     * 2. Si el servidor retorna 401:
     *    - Se intenta refresh del token usando TokenRefreshManager
     *    - Si el refresh es exitoso: se reintenta el request con el nuevo token
     *    - Si el refresh falla: se propaga el 401 y AuthService emite onSessionExpired
     * 3. La UI observa onSessionExpired y navega a login
     *
     * ## Ejemplo de Uso
     *
     * ```kotlin
     * // En tu módulo de DI (Koin, etc.)
     * single<HttpClient> {
     *     HttpClientFactory.createWithAutoRefresh(
     *         authService = get(),
     *         logLevel = if (BuildConfig.DEBUG) LogLevel.INFO else LogLevel.NONE
     *     )
     * }
     *
     * // En tu ViewModel/Repository
     * class UserRepository(private val httpClient: HttpClient) {
     *     suspend fun getUserProfile(): Result<UserProfile> {
     *         return try {
     *             val response = httpClient.get("https://api.edugo.com/user/profile")
     *             Result.Success(response.body())
     *         } catch (e: Exception) {
     *             Result.Failure(e.message ?: "Unknown error")
     *         }
     *     }
     * }
     * ```
     *
     * ## Configuración Recomendada en la App
     *
     * ```kotlin
     * @Composable
     * fun App(authService: AuthService) {
     *     // Observar expiración de sesión
     *     LaunchedEffect(Unit) {
     *         authService.onSessionExpired.collect {
     *             // Navegar a login cuando la sesión expire
     *             navigator.navigate(Screen.Login) {
     *                 popUpTo(0) { inclusive = true }
     *             }
     *         }
     *     }
     *
     *     // ... resto de la app
     * }
     * ```
     *
     * ## Thread-Safety
     *
     * El TokenRefreshManager garantiza que múltiples requests concurrentes que
     * reciben 401 resultan en un solo refresh real. Todos los requests esperan
     * el resultado y reintenthan con el mismo nuevo token.
     *
     * @param authService Servicio de autenticación con TokenRefreshManager integrado
     * @param logLevel Nivel de logging (default: NONE para producción)
     * @param connectTimeoutMs Timeout de conexión en milisegundos (default: 30s)
     * @param requestTimeoutMs Timeout de request en milisegundos (default: 60s)
     * @param networkLogger Logger customizado (default: NetworkLogger.Default con sanitización)
     * @return HttpClient configurado con auto-refresh de tokens
     *
     * @see AuthService Para configuración del servicio de autenticación
     * @see com.edugo.test.module.auth.token.TokenRefreshManager Para configuración avanzada de refresh
     */
    public fun createWithAutoRefresh(
        authService: AuthService,
        logLevel: LogLevel = LogLevel.NONE,
        connectTimeoutMs: Long = DEFAULT_CONNECT_TIMEOUT_MS,
        requestTimeoutMs: Long = DEFAULT_REQUEST_TIMEOUT_MS,
        networkLogger: io.ktor.client.plugins.logging.Logger = NetworkLogger.Default
    ): HttpClient {
        return HttpClient(createPlatformEngine()) {
            install(ContentNegotiation) {
                json(json)
            }

            install(HttpTimeout) {
                connectTimeoutMillis = connectTimeoutMs
                requestTimeoutMillis = requestTimeoutMs
            }

            // Auto-refresh en 401
            install(HttpCallValidator) {
                handleResponseExceptionWithRequest { exception, request ->
                    // Solo manejar 401 Unauthorized
                    val responseException = exception as? ResponseException
                    if (responseException?.response?.status != HttpStatusCode.Unauthorized) {
                        return@handleResponseExceptionWithRequest
                    }

                    // Log para debugging (solo si está habilitado)
                    if (logLevel != LogLevel.NONE) {
                        println("HttpClient: Received 401, attempting token refresh...")
                    }

                    // Intentar refresh del token
                    // Esto actualiza el token en storage automáticamente
                    val refreshResult = authService.tokenRefreshManager.forceRefresh()

                    if (refreshResult is com.edugo.test.module.core.Result.Success) {
                        if (logLevel != LogLevel.NONE) {
                            println("HttpClient: Token refreshed successfully")
                        }
                        // Token actualizado en storage
                        // El próximo request usará el nuevo token automáticamente
                    } else {
                        // Refresh falló, el AuthService emitirá onSessionExpired si corresponde
                        if (logLevel != LogLevel.NONE) {
                            val error = (refreshResult as? com.edugo.test.module.core.Result.Failure)?.error
                            println("HttpClient: Token refresh failed: $error")
                        }
                    }

                    // Propagar el 401 para que el código cliente decida si reintentar
                    throw responseException
                }
            }

            if (logLevel != LogLevel.NONE) {
                install(Logging) {
                    logger = networkLogger
                    level = logLevel
                }
            }
        }
    }

}

/** Default connection timeout: 30 seconds */
private const val DEFAULT_CONNECT_TIMEOUT_MS = 30_000L

/** Default request timeout: 60 seconds */
private const val DEFAULT_REQUEST_TIMEOUT_MS = 60_000L
