package com.edugo.test.module.data.models.pagination

import kotlinx.serialization.Serializable

/**
 * Representa un resultado paginado genérico con metadatos de paginación.
 *
 * Esta clase encapsula una lista de elementos junto con información sobre
 * la paginación, facilitando el manejo de grandes conjuntos de datos
 * divididos en páginas.
 *
 * ## Características
 *
 * - **Genérico y Type-Safe**: Soporta cualquier tipo de elemento
 * - **Serializable**: Compatible con kotlinx.serialization
 * - **Inmutable**: Todos los campos son val (read-only)
 * - **Validado**: Valida parámetros de paginación en construcción
 *
 * ## Uso Básico
 *
 * ```kotlin
 * // Crear desde una lista
 * val users = listOf(user1, user2, user3)
 * val pagedUsers = PagedResult(
 *     items = users,
 *     totalCount = 100,
 *     page = 0,
 *     pageSize = 10
 * )
 *
 * // Acceder a información de paginación
 * println("Mostrando ${pagedUsers.items.size} de ${pagedUsers.totalCount}")
 * println("Página ${pagedUsers.page + 1} de ${pagedUsers.totalPages}")
 * ```
 *
 * ## Transformación de Elementos
 *
 * ```kotlin
 * // Transformar elementos manteniendo metadatos de paginación
 * val userDTOs = pagedUsers.map { user ->
 *     UserDTO(id = user.id, name = user.name)
 * }
 * ```
 *
 * ## Navegación entre Páginas
 *
 * ```kotlin
 * if (pagedUsers.hasNextPage) {
 *     loadPage(pagedUsers.page + 1)
 * }
 *
 * if (pagedUsers.hasPreviousPage) {
 *     loadPage(pagedUsers.page - 1)
 * }
 * ```
 *
 * ## Serialización
 *
 * ```kotlin
 * @Serializable
 * data class User(val id: String, val name: String)
 *
 * val pagedUsers = PagedResult(
 *     items = listOf(User("1", "John"), User("2", "Jane")),
 *     totalCount = 2,
 *     page = 0,
 *     pageSize = 10
 * )
 *
 * // Serializar a JSON
 * val json = Json.encodeToString(pagedUsers)
 *
 * // Deserializar desde JSON
 * val restored = Json.decodeFromString<PagedResult<User>>(json)
 * ```
 *
 * ## Integración con APIs REST
 *
 * ```kotlin
 * // Respuesta típica de API paginada
 * suspend fun getUsers(page: Int, pageSize: Int): PagedResult<User> {
 *     val response = api.getUsers(page, pageSize)
 *     return PagedResult(
 *         items = response.data,
 *         totalCount = response.total,
 *         page = page,
 *         pageSize = pageSize
 *     )
 * }
 * ```
 *
 * @param T El tipo de elementos en la página
 * @property items Lista de elementos en esta página
 * @property totalCount Número total de elementos en todas las páginas
 * @property page Índice de la página actual (base 0)
 * @property pageSize Número máximo de elementos por página
 *
 * @throws IllegalArgumentException si page < 0 o pageSize <= 0
 */
@Serializable
public data class PagedResult<T>(
    /**
     * Lista de elementos en esta página.
     *
     * Puede estar vacía si no hay resultados para esta página,
     * pero nunca debe exceder el tamaño de [pageSize].
     */
    val items: List<T>,

    /**
     * Número total de elementos disponibles en todas las páginas.
     *
     * Este valor representa el total de elementos que existen,
     * no solo los que están en esta página.
     *
     * Ejemplo: Si hay 95 usuarios en total y pageSize=10,
     * entonces totalCount=95 y totalPages=10.
     */
    val totalCount: Int,

    /**
     * Índice de la página actual (base 0).
     *
     * - Página 0: Primera página
     * - Página 1: Segunda página
     * - Etc.
     *
     * Debe ser >= 0.
     */
    val page: Int,

    /**
     * Número máximo de elementos por página.
     *
     * Define cuántos elementos como máximo puede contener
     * cada página. La última página puede tener menos elementos.
     *
     * Debe ser > 0.
     */
    val pageSize: Int
) {
    init {
        require(page >= 0) { "Page index must be non-negative, got: $page" }
        require(pageSize > 0) { "Page size must be positive, got: $pageSize" }
        require(totalCount >= 0) { "Total count must be non-negative, got: $totalCount" }
        require(items.size <= pageSize) {
            "Items count (${items.size}) cannot exceed page size ($pageSize)"
        }
    }

    /**
     * Número total de páginas disponibles.
     *
     * Calculado como: `ceil(totalCount / pageSize)`
     *
     * Ejemplo:
     * - totalCount=95, pageSize=10 → totalPages=10
     * - totalCount=100, pageSize=10 → totalPages=10
     * - totalCount=0, pageSize=10 → totalPages=0
     */
    val totalPages: Int
        get() = if (totalCount == 0) 0 else (totalCount + pageSize - 1) / pageSize

    /**
     * Indica si existe una página siguiente.
     *
     * Retorna `true` si el índice de página actual no es la última página.
     *
     * Ejemplo:
     * ```kotlin
     * if (result.hasNextPage) {
     *     val nextPage = result.page + 1
     *     loadPage(nextPage)
     * }
     * ```
     */
    val hasNextPage: Boolean
        get() = page < totalPages - 1

    /**
     * Indica si existe una página anterior.
     *
     * Retorna `true` si el índice de página actual no es la primera página.
     *
     * Ejemplo:
     * ```kotlin
     * if (result.hasPreviousPage) {
     *     val previousPage = result.page - 1
     *     loadPage(previousPage)
     * }
     * ```
     */
    val hasPreviousPage: Boolean
        get() = page > 0

    /**
     * Indica si esta es la primera página.
     */
    val isFirstPage: Boolean
        get() = page == 0

    /**
     * Indica si esta es la última página.
     */
    val isLastPage: Boolean
        get() = page == totalPages - 1 || totalPages == 0

    /**
     * Indica si la lista de items está vacía.
     */
    val isEmpty: Boolean
        get() = items.isEmpty()

    /**
     * Indica si la lista de items no está vacía.
     */
    val isNotEmpty: Boolean
        get() = items.isNotEmpty()

    /**
     * Número de elementos en esta página actual.
     */
    val itemCount: Int
        get() = items.size

    /**
     * Índice del primer elemento de esta página en el conjunto total (base 0).
     *
     * Ejemplo:
     * - Página 0, pageSize 10 → startIndex = 0
     * - Página 1, pageSize 10 → startIndex = 10
     * - Página 2, pageSize 10 → startIndex = 20
     */
    val startIndex: Int
        get() = page * pageSize

    /**
     * Índice del último elemento de esta página en el conjunto total (base 0).
     *
     * Retorna -1 si no hay elementos.
     *
     * Ejemplo:
     * - Página 0, 5 items, pageSize 10 → endIndex = 4
     * - Página 1, 10 items, pageSize 10 → endIndex = 19
     */
    val endIndex: Int
        get() = if (items.isEmpty()) -1 else startIndex + items.size - 1
}

/**
 * Transforma los elementos de un PagedResult usando la función de transformación proporcionada.
 *
 * Los metadatos de paginación (totalCount, page, pageSize) se mantienen sin cambios.
 *
 * Ejemplo:
 * ```kotlin
 * @Serializable
 * data class User(val id: String, val name: String, val email: String)
 * data class UserSummary(val id: String, val name: String)
 *
 * val pagedUsers: PagedResult<User> = getUsers()
 * val pagedSummaries = pagedUsers.map { user ->
 *     UserSummary(id = user.id, name = user.name)
 * }
 * ```
 *
 * @param transform Función que transforma cada elemento de tipo T a tipo R
 * @return Nuevo PagedResult con elementos transformados
 */
public inline fun <T, R> PagedResult<T>.map(transform: (T) -> R): PagedResult<R> {
    return PagedResult(
        items = items.map(transform),
        totalCount = totalCount,
        page = page,
        pageSize = pageSize
    )
}

/**
 * Transforma los elementos de un PagedResult de forma indexada.
 *
 * La función de transformación recibe tanto el índice como el elemento.
 *
 * Ejemplo:
 * ```kotlin
 * val numberedUsers = pagedUsers.mapIndexed { index, user ->
 *     "${index + 1}. ${user.name}"
 * }
 * ```
 *
 * @param transform Función que recibe el índice y el elemento, y retorna el elemento transformado
 * @return Nuevo PagedResult con elementos transformados
 */
public inline fun <T, R> PagedResult<T>.mapIndexed(transform: (index: Int, T) -> R): PagedResult<R> {
    return PagedResult(
        items = items.mapIndexed(transform),
        totalCount = totalCount,
        page = page,
        pageSize = pageSize
    )
}

/**
 * Filtra los elementos de un PagedResult.
 *
 * **IMPORTANTE**: Esta función filtra solo los items de la página actual.
 * Los metadatos (totalCount, totalPages) NO se actualizan automáticamente.
 * Si necesitas metadatos precisos después del filtrado, considera hacer
 * una nueva consulta en el backend con los filtros aplicados.
 *
 * Ejemplo:
 * ```kotlin
 * val activeUsers = pagedUsers.filter { it.isActive }
 * // Nota: totalCount sigue siendo el mismo (sin filtrar)
 * ```
 *
 * @param predicate Función que determina si un elemento debe incluirse
 * @return Nuevo PagedResult con elementos filtrados (metadatos sin cambios)
 */
public inline fun <T> PagedResult<T>.filter(predicate: (T) -> Boolean): PagedResult<T> {
    return copy(items = items.filter(predicate))
}

/**
 * Crea un PagedResult vacío.
 *
 * Útil como estado inicial o para casos donde no hay resultados.
 *
 * Ejemplo:
 * ```kotlin
 * var users: PagedResult<User> = emptyPagedResult()
 *
 * // Más tarde...
 * users = loadUsers()
 * ```
 */
public fun <T> emptyPagedResult(): PagedResult<T> {
    return PagedResult(
        items = emptyList(),
        totalCount = 0,
        page = 0,
        pageSize = 10
    )
}
