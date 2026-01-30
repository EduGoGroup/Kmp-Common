package com.edugo.test.module

import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformTest {

    @Test
    fun platform_hasValidName() {
        val platform = Platform()
        assertTrue(platform.name.isNotEmpty(), "Platform name should not be empty")
    }

    @Test
    fun getPlatformName_returnsNonEmpty() {
        val platformName = getPlatformName()
        assertTrue(platformName.isNotEmpty(), "getPlatformName() should return non-empty string")
    }
}
