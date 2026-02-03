package com.edugo.test.module

import java.io.File
import java.net.URL

/**
 * Utilidades JVM compartidas entre Android y Desktop.
 *
 * Este sourceset (jvmSharedMain) permite compartir código que usa APIs de java.*
 * entre Android y Desktop JVM sin duplicación.
 *
 * **Importante**: NO usar APIs específicas de Android aquí (android.*, androidx.*).
 * Solo usar APIs estándar de Java que están disponibles en ambas plataformas.
 *
 * Jerarquía:
 * ```
 * commonMain
 *    |
 *    v
 * jvmSharedMain (este sourceset)
 *    |-- androidMain
 *    |-- desktopMain
 * ```
 */
public object JvmUtils {
    /**
     * Lee el contenido de un archivo como String.
     *
     * @param path Ruta absoluta al archivo
     * @return Contenido del archivo como String
     * @throws java.io.FileNotFoundException si el archivo no existe
     */
    public fun readFileAsString(path: String): String = File(path).readText()

    /**
     * Obtiene el contenido de una URL como String.
     *
     * @param url URL a consultar
     * @return Contenido de la respuesta como String
     * @throws java.io.IOException si hay un error de red
     */
    public fun fetchUrl(url: String): String = URL(url).readText()

    /**
     * Verifica si un archivo existe.
     *
     * @param path Ruta al archivo
     * @return true si el archivo existe, false en caso contrario
     */
    public fun fileExists(path: String): Boolean = File(path).exists()

    /**
     * Obtiene el directorio temporal del sistema.
     *
     * @return Ruta al directorio temporal
     */
    public fun getTempDirectory(): String = System.getProperty("java.io.tmpdir") ?: "/tmp"
}
