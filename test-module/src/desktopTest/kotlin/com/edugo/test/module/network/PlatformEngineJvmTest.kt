package com.edugo.test.module.network

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * JVM/Desktop-specific tests for [createPlatformEngine].
 * Verifies that the CIO engine is correctly instantiated on JVM/Desktop.
 */
class PlatformEngineJvmTest {

    @Test
    fun createPlatformEngine_returns_CIO_engine() {
        val engine = createPlatformEngine()

        assertNotNull(engine, "Engine should not be null")
        // CIO engine class name contains "CIO"
        val engineClassName = engine::class.simpleName ?: engine.toString()
        assertTrue(
            engineClassName.contains("CIO", ignoreCase = true) ||
                engine.toString().contains("CIO", ignoreCase = true),
            "Engine should be CIO on Desktop/JVM, but was: $engineClassName"
        )
    }

    @Test
    fun createPlatformEngine_returns_same_type_on_multiple_calls() {
        val engine1 = createPlatformEngine()
        val engine2 = createPlatformEngine()

        // Both should be CIO engines (same type)
        assertNotNull(engine1)
        assertNotNull(engine2)
        assertTrue(
            engine1::class == engine2::class,
            "Multiple calls should return same engine type"
        )
    }
}
