package com.edugo.test.module

/**
 * Clase compartida en commonMain para validar visibilidad entre sourcesets.
 *
 * Esta clase debe ser visible desde:
 * - androidMain
 * - desktopMain
 * - jvmSharedMain
 * - commonTest
 *
 * Jerarquía de visibilidad:
 * ```
 * commonMain (esta clase)
 *    |
 *    v
 * jvmSharedMain
 *    |-- androidMain
 *    |-- desktopMain
 * ```
 */
object SharedClass {
    /**
     * Función común accesible desde todos los sourcesets.
     *
     * @return String identificando el origen del código
     */
    fun commonFunction(): String = "From commonMain"

    /**
     * Constante compartida para validar acceso a propiedades.
     */
    const val SHARED_CONSTANT = "SharedConstant"
}
