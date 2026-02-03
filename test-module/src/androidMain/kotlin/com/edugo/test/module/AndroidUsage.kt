package com.edugo.test.module

/**
 * Archivo de validación de visibilidad para androidMain.
 *
 * Este archivo valida que androidMain puede acceder a:
 * 1. Código de commonMain (SharedClass)
 * 2. Código de jvmSharedMain (JvmShared, JvmUtils)
 *
 * Si este archivo compila, la jerarquía está correctamente configurada.
 */

/**
 * Valida acceso a commonMain desde androidMain.
 *
 * @return Resultado de SharedClass.commonFunction()
 */
public fun useSharedFromAndroid(): String = SharedClass.commonFunction()

/**
 * Valida acceso a constante de commonMain.
 *
 * @return SharedClass.SHARED_CONSTANT
 */
public fun useConstantFromAndroid(): String = SharedClass.SHARED_CONSTANT

/**
 * Valida acceso a jvmSharedMain desde androidMain.
 *
 * @return Resultado de JvmShared.jvmFunction()
 */
public fun useJvmSharedFromAndroid(): String = JvmShared.jvmFunction()

/**
 * Valida acceso a JvmUtils (también en jvmSharedMain).
 *
 * @return Directorio temporal del sistema
 */
public fun useJvmUtilsFromAndroid(): String = JvmUtils.getTempDirectory()

/**
 * Valida cadena de acceso: androidMain -> jvmSharedMain -> commonMain.
 *
 * @return Resultado de JvmShared.accessCommonFromJvmShared()
 */
public fun validateHierarchyFromAndroid(): String = JvmShared.accessCommonFromJvmShared()
