package com.edugo.test.module

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests de visibilidad para jvmSharedTest.
 *
 * Estos tests validan que jvmSharedTest puede acceder a:
 * 1. Código de commonMain (SharedClass)
 * 2. Código de jvmSharedMain (JvmShared, JvmUtils)
 */
class JvmSharedVisibilityTest {

    @Test
    fun commonMain_isAccessibleFromJvmSharedTest() {
        val result = SharedClass.commonFunction()
        assertEquals("From commonMain", result)
    }

    @Test
    fun jvmSharedMain_isAccessibleFromJvmSharedTest() {
        val result = JvmShared.jvmFunction()
        assertEquals("From jvmSharedMain", result)
    }

    @Test
    fun jvmUtils_isAccessibleFromJvmSharedTest() {
        val tempDir = JvmUtils.getTempDirectory()
        assertTrue(tempDir.isNotEmpty(), "Temp directory should not be empty")
    }

    @Test
    fun jvmShared_canAccessCommonMain() {
        val result = JvmShared.accessCommonFromJvmShared()
        assertEquals("From commonMain", result)
    }

    @Test
    fun jvmShared_canAccessJavaApis() {
        val osName = JvmShared.getOsName()
        assertTrue(osName.isNotEmpty(), "OS name should not be empty")
    }
}
