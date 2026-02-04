package com.edugo.test.module.network

import kotlin.test.Test
import kotlin.test.assertNotNull

class PlatformEngineJsTest {
    @Test
    fun createPlatformEngine_returns_Js_engine() {
        val engine = createPlatformEngine()
        assertNotNull(engine)
    }
}
