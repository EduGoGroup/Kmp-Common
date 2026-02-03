package com.edugo.test.module.network

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Factory for creating configured HttpClient instances.
 *
 * This factory provides a centralized way to create HTTP clients with
 * consistent configuration across the application, including JSON serialization
 * and optional logging.
 *
 * Example usage:
 * ```kotlin
 * // Production: No logging
 * val client = HttpClientFactory.createBaseClient(engine)
 *
 * // Development: With logging
 * val debugClient = HttpClientFactory.createBaseClient(engine, LogLevel.INFO)
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
}
