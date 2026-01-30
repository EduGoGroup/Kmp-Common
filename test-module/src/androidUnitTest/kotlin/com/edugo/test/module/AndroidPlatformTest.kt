package com.edugo.test.module

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AndroidPlatformTest {

    @Test
    fun platform_isAndroid() {
        val platform = Platform()
        assertEquals("Android", platform.name)
    }

    @Test
    fun androidSpecificApi_works() {
        val platformName = getPlatformName()
        assertTrue(platformName.startsWith("Android"), "Platform name should start with 'Android'")
    }
}
