package com.edugo.test.module

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests de visibilidad para validar que commonMain es accesible desde commonTest.
 *
 * Estos tests validan que:
 * 1. SharedClass está disponible en commonTest
 * 2. Las funciones de SharedClass funcionan correctamente
 */
class VisibilityTest {

    @Test
    fun commonMain_isAccessibleFromCommonTest() {
        val result = SharedClass.commonFunction()
        assertEquals("From commonMain", result)
    }

    @Test
    fun sharedConstant_isAccessibleFromCommonTest() {
        assertEquals("SharedConstant", SharedClass.SHARED_CONSTANT)
    }
}
