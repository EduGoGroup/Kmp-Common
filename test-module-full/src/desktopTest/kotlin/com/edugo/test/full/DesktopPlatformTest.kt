package com.edugo.test.full

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopPlatformTest {

    @Test
    fun platform_isDesktop() {
        val platform = Platform()
        assertEquals("Desktop", platform.name)
    }

    @Test
    fun desktopSpecificApi_works() {
        val platformName = getPlatformName()
        assertTrue(platformName.contains("Desktop"), "Platform name should contain 'Desktop'")
    }
}
