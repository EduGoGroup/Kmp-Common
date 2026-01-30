package com.edugo.test.module

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests de visibilidad para androidUnitTest.
 *
 * Estos tests validan que androidMain puede acceder a:
 * 1. Código de commonMain (SharedClass)
 * 2. Código de jvmSharedMain (JvmShared, JvmUtils)
 * 3. Funciones de validación en AndroidUsage.kt
 */
class AndroidVisibilityTest {

    @Test
    fun commonMain_isAccessibleFromAndroid() {
        val result = useSharedFromAndroid()
        assertEquals("From commonMain", result)
    }

    @Test
    fun sharedConstant_isAccessibleFromAndroid() {
        val result = useConstantFromAndroid()
        assertEquals("SharedConstant", result)
    }

    @Test
    fun jvmSharedMain_isAccessibleFromAndroid() {
        val result = useJvmSharedFromAndroid()
        assertEquals("From jvmSharedMain", result)
    }

    @Test
    fun jvmUtils_isAccessibleFromAndroid() {
        val tempDir = useJvmUtilsFromAndroid()
        assertTrue(tempDir.isNotEmpty(), "Temp directory should not be empty")
    }

    @Test
    fun hierarchyChain_worksFromAndroid() {
        // Valida: androidMain -> jvmSharedMain -> commonMain
        val result = validateHierarchyFromAndroid()
        assertEquals("From commonMain", result)
    }
}
