package com.edugo.test.module.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PlatformJsTest {
    @Test
    fun platform_name_is_JS() {
        assertEquals("JS", Platform.name)
    }

    @Test
    fun platform_osVersion_returns_value() {
        val version = Platform.osVersion
        assertNotNull(version)
    }

    @Test
    fun platform_isDebug_returns_boolean() {
        // Just verify it doesn't throw and returns a boolean
        val isDebug = Platform.isDebug
        assertNotNull(isDebug)
    }
}
