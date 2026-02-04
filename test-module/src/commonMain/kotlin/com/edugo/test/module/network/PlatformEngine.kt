package com.edugo.test.module.network

import io.ktor.client.engine.HttpClientEngine

/**
 * Provides the platform-specific [HttpClientEngine] for Ktor Client.
 *
 * Each platform uses an optimized engine:
 * - **Android**: OkHttp - Native Android HTTP client with HTTP/2 support
 * - **JVM/Desktop**: CIO - Coroutine-based I/O engine, pure Kotlin
 * - **JS**: Js - Browser Fetch API based engine
 *
 * Usage:
 * ```kotlin
 * val engine = createPlatformEngine()
 * val client = HttpClientFactory.createBaseClient(engine)
 * ```
 *
 * @return Platform-specific [HttpClientEngine] instance
 */
public expect fun createPlatformEngine(): HttpClientEngine
