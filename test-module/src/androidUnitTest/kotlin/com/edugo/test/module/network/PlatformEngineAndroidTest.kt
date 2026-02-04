package com.edugo.test.module.network

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Android-specific tests for [createPlatformEngine].
 * Verifies that the OkHttp engine is correctly instantiated on Android.
 */
class PlatformEngineAndroidTest {

    @Test
    fun createPlatformEngine_returns_OkHttp_engine() {
        val engine = createPlatformEngine()

        assertNotNull(engine, "Engine should not be null")
        // OkHttp engine class name contains "OkHttp"
        val engineClassName = engine::class.simpleName ?: engine.toString()
        assertTrue(
            engineClassName.contains("OkHttp", ignoreCase = true) ||
                engine.toString().contains("OkHttp", ignoreCase = true),
            "Engine should be OkHttp on Android, but was: $engineClassName"
        )
    }

    @Test
    fun createPlatformEngine_returns_same_type_on_multiple_calls() {
        val engine1 = createPlatformEngine()
        val engine2 = createPlatformEngine()

        // Both should be OkHttp engines (same type)
        assertNotNull(engine1)
        assertNotNull(engine2)
        assertTrue(
            engine1::class == engine2::class,
            "Multiple calls should return same engine type"
        )
    }
}
