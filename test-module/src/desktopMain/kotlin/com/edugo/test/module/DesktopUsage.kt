package com.edugo.test.module

/**
 * Archivo de validación de visibilidad para desktopMain.
 *
 * Este archivo valida que desktopMain puede acceder a:
 * 1. Código de commonMain (SharedClass)
 * 2. Código de jvmSharedMain (JvmShared, JvmUtils)
 *
 * Si este archivo compila, la jerarquía está correctamente configurada.
 */

/**
 * Valida acceso a commonMain desde desktopMain.
 *
 * @return Resultado de SharedClass.commonFunction()
 */
fun useSharedFromDesktop(): String = SharedClass.commonFunction()

/**
 * Valida acceso a constante de commonMain.
 *
 * @return SharedClass.SHARED_CONSTANT
 */
fun useConstantFromDesktop(): String = SharedClass.SHARED_CONSTANT

/**
 * Valida acceso a jvmSharedMain desde desktopMain.
 *
 * @return Resultado de JvmShared.jvmFunction()
 */
fun useJvmSharedFromDesktop(): String = JvmShared.jvmFunction()

/**
 * Valida acceso a JvmUtils (también en jvmSharedMain).
 *
 * @return Directorio temporal del sistema
 */
fun useJvmUtilsFromDesktop(): String = JvmUtils.getTempDirectory()

/**
 * Valida cadena de acceso: desktopMain -> jvmSharedMain -> commonMain.
 *
 * @return Resultado de JvmShared.accessCommonFromJvmShared()
 */
fun validateHierarchyFromDesktop(): String = JvmShared.accessCommonFromJvmShared()
