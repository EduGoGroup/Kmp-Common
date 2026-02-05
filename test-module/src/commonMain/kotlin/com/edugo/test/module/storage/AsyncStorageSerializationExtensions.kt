/*
 * Copyright (c) 2026 EduGo Project
 * Licensed under the MIT License
 */

package com.edugo.test.module.storage

import com.edugo.test.module.core.Result

/**
 * Extensiones asíncronas para serialización/deserialización de objetos en AsyncEduGoStorage.
 *
 * ## Limitación técnica de Kotlin
 * Las funciones con `reified` type parameters no pueden usar `withContext` internamente
 * debido a que `inline` + `reified` requiere que el cuerpo de la función sea inlineable,
 * y `withContext` es una función suspendida que no puede ser inlineada.
 *
 * **Implicación:** Estas operaciones se ejecutan en el thread que las invoca, NO en el
 * dispatcher configurado en AsyncEduGoStorage. Para objetos muy grandes donde la
 * serialización/deserialización podría ser costosa, considera envolver la llamada
 * manualmente:
 *
 * ```kotlin
 * // Para objetos grandes, envolver manualmente en el dispatcher deseado:
 * withContext(Dispatchers.Default) {
 *     asyncStorage.putObject("largeObject", myLargeObject)
 * }
 * ```
 *
 * ## Uso normal
 * Para la mayoría de casos de uso con objetos pequeños/medianos, el overhead es
 * negligible y puedes usar las funciones directamente:
 *
 * ```kotlin
 * @Serializable
 * data class User(val id: Int, val name: String)
 *
 * val asyncStorage = AsyncEduGoStorage.create()
 *
 * launch {
 *     asyncStorage.putObject("user", User(1, "John"))
 *     val user: User? = asyncStorage.getObject<User>("user")
 * }
 * ```
 */

// =============================================================================
// OBJETOS INDIVIDUALES
// =============================================================================

/**
 * Guarda un objeto serializable como JSON de forma asíncrona.
 */
suspend inline fun <reified T> AsyncEduGoStorage.putObject(key: String, value: T) {
    sync.putObject(key, value)
}

/**
 * Recupera un objeto deserializándolo desde JSON de forma asíncrona.
 */
suspend inline fun <reified T> AsyncEduGoStorage.getObject(key: String): T? {
    return sync.getObject<T>(key)
}

/**
 * Recupera un objeto con valor por defecto de forma asíncrona.
 */
suspend inline fun <reified T> AsyncEduGoStorage.getObject(key: String, default: T): T {
    return sync.getObject(key, default)
}

/**
 * Recupera un objeto retornando Result para manejo explícito de errores.
 */
suspend inline fun <reified T> AsyncEduGoStorage.getObjectSafe(key: String): Result<T> {
    return sync.getObjectSafe<T>(key)
}

/**
 * Guarda un objeto retornando Result para confirmar éxito.
 */
suspend inline fun <reified T> AsyncEduGoStorage.putObjectSafe(key: String, value: T): Result<Unit> {
    return sync.putObjectSafe(key, value)
}

// =============================================================================
// LISTAS
// =============================================================================

/**
 * Guarda una lista de objetos serializables de forma asíncrona.
 */
suspend inline fun <reified T> AsyncEduGoStorage.putList(key: String, value: List<T>) {
    sync.putList(key, value)
}

/**
 * Recupera una lista de objetos de forma asíncrona.
 */
suspend inline fun <reified T> AsyncEduGoStorage.getList(key: String): List<T> {
    return sync.getList<T>(key)
}

/**
 * Recupera una lista con Result para manejo de errores.
 */
suspend inline fun <reified T> AsyncEduGoStorage.getListSafe(key: String): Result<List<T>> {
    return sync.getListSafe<T>(key)
}

/**
 * Agrega un elemento a una lista existente de forma asíncrona.
 */
suspend inline fun <reified T> AsyncEduGoStorage.addToList(key: String, element: T) {
    sync.addToList(key, element)
}

/**
 * Elimina un elemento de una lista existente de forma asíncrona.
 */
suspend inline fun <reified T> AsyncEduGoStorage.removeFromList(key: String, element: T) {
    sync.removeFromList(key, element)
}

// =============================================================================
// SETS
// =============================================================================

/**
 * Guarda un Set de objetos serializables de forma asíncrona.
 */
suspend inline fun <reified T> AsyncEduGoStorage.putSet(key: String, value: Set<T>) {
    sync.putSet(key, value)
}

/**
 * Recupera un Set de objetos de forma asíncrona.
 */
suspend inline fun <reified T> AsyncEduGoStorage.getSet(key: String): Set<T> {
    return sync.getSet<T>(key)
}

// =============================================================================
// MAPS
// =============================================================================

/**
 * Guarda un Map serializable de forma asíncrona.
 */
suspend inline fun <reified V> AsyncEduGoStorage.putMap(key: String, value: Map<String, V>) {
    sync.putMap(key, value)
}

/**
 * Recupera un Map de forma asíncrona.
 */
suspend inline fun <reified V> AsyncEduGoStorage.getMap(key: String): Map<String, V> {
    return sync.getMap<V>(key)
}
