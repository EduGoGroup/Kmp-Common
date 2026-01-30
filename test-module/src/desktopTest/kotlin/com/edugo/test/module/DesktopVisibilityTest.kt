package com.edugo.test.module

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests de visibilidad para desktopTest.
 *
 * Estos tests validan que desktopMain puede acceder a:
 * 1. Código de commonMain (SharedClass)
 * 2. Código de jvmSharedMain (JvmShared, JvmUtils)
 * 3. Funciones de validación en DesktopUsage.kt
 */
class DesktopVisibilityTest {

    @Test
    fun commonMain_isAccessibleFromDesktop() {
        val result = useSharedFromDesktop()
        assertEquals("From commonMain", result)
    }

    @Test
    fun sharedConstant_isAccessibleFromDesktop() {
        val result = useConstantFromDesktop()
        assertEquals("SharedConstant", result)
    }

    @Test
    fun jvmSharedMain_isAccessibleFromDesktop() {
        val result = useJvmSharedFromDesktop()
        assertEquals("From jvmSharedMain", result)
    }

    @Test
    fun jvmUtils_isAccessibleFromDesktop() {
        val tempDir = useJvmUtilsFromDesktop()
        assertTrue(tempDir.isNotEmpty(), "Temp directory should not be empty")
    }

    @Test
    fun hierarchyChain_worksFromDesktop() {
        // Valida: desktopMain -> jvmSharedMain -> commonMain
        val result = validateHierarchyFromDesktop()
        assertEquals("From commonMain", result)
    }
}
