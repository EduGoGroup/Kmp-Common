package com.edugo.test.module.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

/**
 * Android implementation using OkHttp engine.
 *
 * OkHttp provides:
 * - HTTP/2 support
 * - Connection pooling
 * - Transparent GZIP compression
 * - Response caching
 * - Native Android integration
 */
public actual fun createPlatformEngine(): HttpClientEngine = OkHttp.create()
