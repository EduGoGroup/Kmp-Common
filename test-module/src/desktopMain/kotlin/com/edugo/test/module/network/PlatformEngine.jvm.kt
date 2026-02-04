package com.edugo.test.module.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO

/**
 * JVM/Desktop implementation using CIO (Coroutine-based I/O) engine.
 *
 * CIO provides:
 * - Pure Kotlin implementation (no native dependencies)
 * - Coroutine-based async I/O
 * - HTTP/1.1 and HTTP/2 support
 * - TLS support
 * - Lightweight and fast
 */
public actual fun createPlatformEngine(): HttpClientEngine = CIO.create()
