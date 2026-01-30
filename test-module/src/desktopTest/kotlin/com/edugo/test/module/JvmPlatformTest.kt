package com.edugo.test.module

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JvmPlatformTest {

    @Test
    fun platform_isJvm() {
        val platform = Platform()
        assertEquals("JVM", platform.name)
    }

    @Test
    fun jvmSpecificApi_works() {
        val platformName = getPlatformName()
        assertTrue(platformName.contains("JVM"), "Platform name should contain 'JVM'")
    }
}
