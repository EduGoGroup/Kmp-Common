/*
 * Copyright (c) 2026 EduGo Project
 * Licensed under the MIT License
 */

package com.edugo.test.module.storage

import com.edugo.test.module.config.JsonConfig
import com.edugo.test.module.core.Result
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString

/**
 * Extension functions para serialización/deserialización de objetos @Serializable en EduGoStorage.
 *
 * Permite almacenar cualquier objeto marcado con @Serializable como JSON string en el storage
 * multiplataforma, utilizando [JsonConfig.Default] para la serialización.
 *
 * ## Uso básico
 *
 * ```kotlin
 * @Serializable
 * data class User(val id: Int, val name: String)
 *
 * val storage = EduGoStorage.create()
 *
 * // Guardar objeto
 * storage.putObject("current_user", User(1, "John"))
 *
 * // Recuperar objeto
 * val user: User? = storage.getObject<User>("current_user")
 *
 * // Con valor por defecto
 * val user = storage.getObject("current_user", User(0, "Guest"))
 *
 * // Con manejo explícito de errores
 * val result: Result<User> = storage.getObjectSafe<User>("current_user")
 * ```
 */

// =============================================================================
// OBJETOS INDIVIDUALES
// =============================================================================

/**
 * Guarda un objeto serializable como JSON.
 *
 * El objeto se serializa a JSON string usando [JsonConfig.Default] y se almacena
 * en el storage como un String.
 *
 * @param key Clave de almacenamiento
 * @param value Objeto @Serializable a guardar
 * @throws SerializationException si el objeto no puede ser serializado
 *
 * @sample
 * ```kotlin
 * @Serializable
 * data class Settings(val theme: String, val fontSize: Int)
 *
 * storage.putObject("app.settings", Settings("dark", 14))
 * ```
 */
inline fun <reified T> EduGoStorage.putObject(key: String, value: T) {
    val json = JsonConfig.Default.encodeToString(value)
    putString(key, json)
}

/**
 * Recupera un objeto deserializándolo desde JSON.
 *
 * Lee el JSON string del storage y lo deserializa al tipo especificado
 * usando [JsonConfig.Default].
 *
 * @param key Clave de almacenamiento
 * @return Objeto deserializado o null si no existe la key o hay error de deserialización
 *
 * @sample
 * ```kotlin
 * val settings: Settings? = storage.getObject<Settings>("app.settings")
 * settings?.let { println("Theme: ${it.theme}") }
 * ```
 */
inline fun <reified T> EduGoStorage.getObject(key: String): T? {
    val json = getStringOrNull(key) ?: return null
    return try {
        JsonConfig.Default.decodeFromString<T>(json)
    } catch (e: SerializationException) {
        null
    }
}

/**
 * Recupera un objeto con valor por defecto si no existe.
 *
 * @param key Clave de almacenamiento
 * @param default Valor por defecto a retornar si la key no existe o hay error
 * @return Objeto deserializado o el valor por defecto
 *
 * @sample
 * ```kotlin
 * val settings = storage.getObject("app.settings", Settings("light", 12))
 * ```
 */
inline fun <reified T> EduGoStorage.getObject(key: String, default: T): T {
    return getObject<T>(key) ?: default
}

/**
 * Recupera un objeto retornando Result para manejo explícito de errores.
 *
 * Útil cuando necesitas distinguir entre:
 * - Key no encontrada
 * - Error de deserialización (JSON corrupto o incompatible)
 *
 * @param key Clave de almacenamiento
 * @return [Result.Success] con el objeto deserializado o [Result.Failure] con mensaje de error
 *
 * @sample
 * ```kotlin
 * when (val result = storage.getObjectSafe<Settings>("app.settings")) {
 *     is Result.Success -> println("Settings: ${result.data}")
 *     is Result.Failure -> println("Error: ${result.error}")
 *     is Result.Loading -> { /* no aplica */ }
 * }
 * ```
 */
inline fun <reified T> EduGoStorage.getObjectSafe(key: String): Result<T> {
    val json = getStringOrNull(key)
        ?: return Result.Failure("Key '$key' not found in storage")

    return try {
        Result.Success(JsonConfig.Default.decodeFromString<T>(json))
    } catch (e: SerializationException) {
        Result.Failure("Failed to deserialize '$key': ${e.message}")
    }
}

/**
 * Guarda un objeto retornando Result para confirmar éxito.
 *
 * Variante de [putObject] que captura excepciones de serialización
 * y retorna un [Result] en lugar de lanzar la excepción.
 *
 * @param key Clave de almacenamiento
 * @param value Objeto @Serializable a guardar
 * @return [Result.Success] con Unit si se guardó correctamente,
 *         [Result.Failure] si hubo error de serialización
 *
 * @sample
 * ```kotlin
 * val result = storage.putObjectSafe("app.settings", settings)
 * if (result is Result.Failure) {
 *     logger.error("Failed to save settings: ${result.error}")
 * }
 * ```
 */
inline fun <reified T> EduGoStorage.putObjectSafe(key: String, value: T): Result<Unit> {
    return try {
        val json = JsonConfig.Default.encodeToString(value)
        putString(key, json)
        Result.Success(Unit)
    } catch (e: SerializationException) {
        Result.Failure("Failed to serialize '$key': ${e.message}")
    }
}

// =============================================================================
// LISTAS
// =============================================================================

/**
 * Guarda una lista de objetos serializables.
 *
 * @param key Clave de almacenamiento
 * @param value Lista de objetos @Serializable a guardar
 * @throws SerializationException si la lista no puede ser serializada
 *
 * @sample
 * ```kotlin
 * val users = listOf(User(1, "John"), User(2, "Jane"))
 * storage.putList("recent_users", users)
 * ```
 */
inline fun <reified T> EduGoStorage.putList(key: String, value: List<T>) {
    putObject(key, value)
}

/**
 * Recupera una lista de objetos.
 *
 * @param key Clave de almacenamiento
 * @return Lista deserializada o lista vacía si no existe la key o hay error
 *
 * @sample
 * ```kotlin
 * val users: List<User> = storage.getList<User>("recent_users")
 * users.forEach { println(it.name) }
 * ```
 */
inline fun <reified T> EduGoStorage.getList(key: String): List<T> {
    return getObject<List<T>>(key) ?: emptyList()
}

/**
 * Recupera una lista con Result para manejo de errores.
 *
 * @param key Clave de almacenamiento
 * @return [Result.Success] con la lista o [Result.Failure] con mensaje de error
 */
inline fun <reified T> EduGoStorage.getListSafe(key: String): Result<List<T>> {
    return getObjectSafe<List<T>>(key)
}

/**
 * Agrega un elemento a una lista existente.
 *
 * Si la lista no existe, crea una nueva con el elemento.
 *
 * @param key Clave de almacenamiento
 * @param element Elemento a agregar
 *
 * @sample
 * ```kotlin
 * storage.addToList("favorites", Product(123, "Laptop"))
 * ```
 */
inline fun <reified T> EduGoStorage.addToList(key: String, element: T) {
    val current = getList<T>(key).toMutableList()
    current.add(element)
    putList(key, current)
}

/**
 * Elimina un elemento de una lista existente.
 *
 * Usa equals() para comparar elementos. Si el elemento no existe, no hace nada.
 *
 * @param key Clave de almacenamiento
 * @param element Elemento a eliminar
 *
 * @sample
 * ```kotlin
 * storage.removeFromList("favorites", productToRemove)
 * ```
 */
inline fun <reified T> EduGoStorage.removeFromList(key: String, element: T) {
    val current = getList<T>(key).toMutableList()
    current.remove(element)
    putList(key, current)
}

// =============================================================================
// SETS
// =============================================================================

/**
 * Guarda un Set de objetos serializables.
 *
 * Internamente se serializa como List para compatibilidad JSON estándar.
 *
 * @param key Clave de almacenamiento
 * @param value Set de objetos @Serializable a guardar
 * @throws SerializationException si el set no puede ser serializado
 *
 * @sample
 * ```kotlin
 * val tags = setOf("kotlin", "multiplatform", "storage")
 * storage.putSet("user_tags", tags)
 * ```
 */
inline fun <reified T> EduGoStorage.putSet(key: String, value: Set<T>) {
    putObject(key, value.toList())
}

/**
 * Recupera un Set de objetos.
 *
 * @param key Clave de almacenamiento
 * @return Set deserializado o set vacío si no existe la key o hay error
 *
 * @sample
 * ```kotlin
 * val tags: Set<String> = storage.getSet<String>("user_tags")
 * if ("kotlin" in tags) { println("Kotlin fan!") }
 * ```
 */
inline fun <reified T> EduGoStorage.getSet(key: String): Set<T> {
    return getObject<List<T>>(key)?.toSet() ?: emptySet()
}

// =============================================================================
// MAPS
// =============================================================================

/**
 * Guarda un Map serializable.
 *
 * Las keys deben ser String para compatibilidad con JSON estándar.
 *
 * @param key Clave de almacenamiento
 * @param value Map con keys String y valores @Serializable
 * @throws SerializationException si el map no puede ser serializado
 *
 * @sample
 * ```kotlin
 * val preferences = mapOf(
 *     "theme" to "dark",
 *     "language" to "es",
 *     "notifications" to "enabled"
 * )
 * storage.putMap("user_preferences", preferences)
 * ```
 */
inline fun <reified V> EduGoStorage.putMap(key: String, value: Map<String, V>) {
    putObject(key, value)
}

/**
 * Recupera un Map.
 *
 * @param key Clave de almacenamiento
 * @return Map deserializado o map vacío si no existe la key o hay error
 *
 * @sample
 * ```kotlin
 * val preferences: Map<String, String> = storage.getMap<String>("user_preferences")
 * val theme = preferences["theme"] ?: "light"
 * ```
 */
inline fun <reified V> EduGoStorage.getMap(key: String): Map<String, V> {
    return getObject<Map<String, V>>(key) ?: emptyMap()
}
