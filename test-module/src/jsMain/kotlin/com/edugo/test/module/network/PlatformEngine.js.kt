package com.edugo.test.module.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js

/**
 * JavaScript implementation using Js engine.
 *
 * Js engine provides:
 * - Browser Fetch API integration
 * - Node.js HTTP module support
 * - Native async/await compatibility
 * - Zero additional dependencies in browser
 */
public actual fun createPlatformEngine(): HttpClientEngine = Js.create()
