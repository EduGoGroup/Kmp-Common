package com.edugo.test.module

/**
 * Clase compartida en jvmSharedMain para validar visibilidad del intermediate sourceset.
 *
 * Esta clase debe ser visible desde:
 * - androidMain
 * - desktopMain
 *
 * NO debe ser visible desde:
 * - commonMain (es hijo, no padre)
 *
 * Jerarquía de visibilidad:
 * ```
 * commonMain
 *    |
 *    v
 * jvmSharedMain (esta clase)
 *    |-- androidMain (puede ver esta clase)
 *    |-- desktopMain (puede ver esta clase)
 * ```
 */
object JvmShared {
    /**
     * Función JVM compartida accesible desde Android y Desktop.
     *
     * @return String identificando el origen del código
     */
    fun jvmFunction(): String = "From jvmSharedMain"

    /**
     * Demuestra acceso a APIs de java.* disponibles en JVM.
     *
     * @return Nombre del sistema operativo
     */
    fun getOsName(): String = System.getProperty("os.name") ?: "Unknown"

    /**
     * También puede acceder a código de commonMain (su padre).
     *
     * @return Resultado de SharedClass.commonFunction()
     */
    fun accessCommonFromJvmShared(): String = SharedClass.commonFunction()
}
