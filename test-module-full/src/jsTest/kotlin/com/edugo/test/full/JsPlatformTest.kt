package com.edugo.test.full

import kotlin.test.Test
import kotlin.test.assertEquals

class JsPlatformTest {

    @Test
    fun platform_isJavaScript() {
        val platform = Platform()
        assertEquals("JavaScript", platform.name)
    }
}
