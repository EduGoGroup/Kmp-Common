package com.edugo.test.module.network

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
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
     * - Optionally enables logging
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
     * @param logLevel Logging level for HTTP operations. Default is [LogLevel.NONE].
     * @param connectTimeoutMs Maximum time to establish a connection in milliseconds.
     *                         Default is 30,000ms (30 seconds).
     * @param requestTimeoutMs Maximum time for the entire request in milliseconds.
     *                         Default is 60,000ms (60 seconds).
     * @return Configured HttpClient instance ready for making requests
     *
     * @see createBaseClient For creating clients with custom engines (useful for testing)
     */
    public fun create(
        logLevel: LogLevel = LogLevel.NONE,
        connectTimeoutMs: Long = DEFAULT_CONNECT_TIMEOUT_MS,
        requestTimeoutMs: Long = DEFAULT_REQUEST_TIMEOUT_MS
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
