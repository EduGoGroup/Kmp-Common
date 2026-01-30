package com.edugo.test.module.platform

/**
 * Proporciona informacion sobre la plataforma actual.
 */
expect object Platform {
    /** Nombre de la plataforma (Android, JVM, JS, etc.) */
    val name: String

    /** Version del sistema operativo */
    val osVersion: String

    /** Indica si es modo debug */
    val isDebug: Boolean
}

/**
 * Funcion utilitaria para obtener nombre de plataforma.
 */
fun getPlatformDescription(): String = "Running on ${Platform.name} (${Platform.osVersion})"
