package com.edugo.test.module.data.models.pagination

/**
 * Extension functions para facilitar operaciones de paginación.
 *
 * Este archivo contiene funciones de extensión que simplifican la creación
 * y manipulación de resultados paginados desde listas y colecciones.
 */

/**
 * Convierte una lista en un PagedResult.
 *
 * Esta función toma una lista de elementos y crea un PagedResult con los
 * metadatos de paginación proporcionados. Es útil cuando ya tienes los datos
 * paginados y solo necesitas envolverlos en la estructura PagedResult.
 *
 * ## Uso Típico
 *
 * ```kotlin
 * // Desde una API que ya retorna datos paginados
 * val users = api.getUsers(page = 0, size = 10)
 * val pagedResult = users.toPagedResult(
 *     page = 0,
 *     pageSize = 10,
 *     totalCount = api.getTotalUsersCount()
 * )
 * ```
 *
 * ## Validaciones
 *
 * - `page` debe ser >= 0
 * - `pageSize` debe ser > 0
 * - `totalCount` debe ser >= 0
 * - El tamaño de la lista no debe exceder `pageSize`
 *
 * @param page Índice de la página (base 0)
 * @param pageSize Tamaño de la página
 * @param totalCount Número total de elementos en todas las páginas
 * @return PagedResult conteniendo los elementos con metadatos de paginación
 * @throws IllegalArgumentException si los parámetros no son válidos
 */
public fun <T> List<T>.toPagedResult(
    page: Int,
    pageSize: Int,
    totalCount: Int
): PagedResult<T> {
    return PagedResult(
        items = this,
        totalCount = totalCount,
        page = page,
        pageSize = pageSize
    )
}

/**
 * Pagina una lista en memoria y retorna la página solicitada.
 *
 * Esta función divide una lista completa en páginas y retorna la página
 * específica. Es útil para paginar datos que ya están cargados en memoria.
 *
 * **NOTA**: Para grandes conjuntos de datos, es más eficiente paginar en
 * el backend/base de datos en lugar de cargar todo en memoria.
 *
 * ## Uso
 *
 * ```kotlin
 * val allUsers = listOf(user1, user2, user3, ..., user100)
 *
 * // Obtener página 0 (primeros 10 usuarios)
 * val firstPage = allUsers.paginate(page = 0, pageSize = 10)
 * // firstPage.items = [user1, user2, ..., user10]
 * // firstPage.totalCount = 100
 * // firstPage.totalPages = 10
 *
 * // Obtener página 9 (últimos 10 usuarios)
 * val lastPage = allUsers.paginate(page = 9, pageSize = 10)
 * // lastPage.items = [user91, user92, ..., user100]
 * ```
 *
 * ## Casos Edge
 *
 * ```kotlin
 * // Lista vacía
 * emptyList<User>().paginate(0, 10)
 * // Retorna: PagedResult(items=[], totalCount=0, page=0, pageSize=10)
 *
 * // Página fuera de rango
 * listOf(1, 2, 3).paginate(page = 10, pageSize = 10)
 * // Retorna: PagedResult(items=[], totalCount=3, page=10, pageSize=10)
 *
 * // Última página con menos elementos
 * (1..25).toList().paginate(page = 2, pageSize = 10)
 * // Retorna: PagedResult(items=[21,22,23,24,25], totalCount=25, page=2, pageSize=10)
 * ```
 *
 * @param page Índice de la página a obtener (base 0)
 * @param pageSize Número de elementos por página
 * @return PagedResult con los elementos de la página solicitada
 * @throws IllegalArgumentException si page < 0 o pageSize <= 0
 */
public fun <T> List<T>.paginate(page: Int, pageSize: Int): PagedResult<T> {
    require(page >= 0) { "Page must be non-negative" }
    require(pageSize > 0) { "Page size must be positive" }

    val totalCount = this.size
    val startIndex = page * pageSize
    val endIndex = minOf(startIndex + pageSize, totalCount)

    val items = if (startIndex >= totalCount) {
        emptyList()
    } else {
        this.subList(startIndex, endIndex)
    }

    return PagedResult(
        items = items,
        totalCount = totalCount,
        page = page,
        pageSize = pageSize
    )
}

/**
 * Obtiene todos los elementos de todas las páginas de un PagedResult.
 *
 * **ADVERTENCIA**: Esta función solo retorna los elementos de la página actual.
 * Para obtener realmente todos los elementos de todas las páginas, necesitas
 * hacer múltiples llamadas a la API/fuente de datos.
 *
 * Esta función está aquí por conveniencia, pero debes entender que solo
 * retorna `result.items`, no hace llamadas adicionales.
 *
 * Ejemplo de uso correcto:
 * ```kotlin
 * suspend fun getAllUsers(pageSize: Int = 50): List<User> {
 *     val allUsers = mutableListOf<User>()
 *     var page = 0
 *
 *     do {
 *         val pagedResult = api.getUsers(page, pageSize)
 *         allUsers.addAll(pagedResult.items)
 *         page++
 *     } while (pagedResult.hasNextPage)
 *
 *     return allUsers
 * }
 * ```
 *
 * @return Lista de elementos en la página actual
 */
public fun <T> PagedResult<T>.toList(): List<T> = items

/**
 * Combina múltiples PagedResults en uno solo.
 *
 * Útil cuando has cargado varias páginas y quieres combinarlas.
 * Los metadatos se toman del primer PagedResult.
 *
 * Ejemplo:
 * ```kotlin
 * val page1 = api.getUsers(0, 10)
 * val page2 = api.getUsers(1, 10)
 * val page3 = api.getUsers(2, 10)
 *
 * val allPages = listOf(page1, page2, page3).merge()
 * // allPages.items contiene elementos de las 3 páginas
 * // allPages.totalCount = page1.totalCount (sin cambios)
 * ```
 *
 * @return PagedResult con todos los items combinados
 */
public fun <T> List<PagedResult<T>>.merge(): PagedResult<T> {
    if (isEmpty()) {
        return emptyPagedResult()
    }

    val first = first()
    val allItems = flatMap { it.items }

    return PagedResult(
        items = allItems,
        totalCount = first.totalCount,
        page = first.page,
        pageSize = allItems.size.coerceAtLeast(first.pageSize)
    )
}

/**
 * Calcula el rango de páginas a mostrar en un paginador UI.
 *
 * Útil para crear controles de paginación que muestran un número limitado
 * de páginas alrededor de la página actual.
 *
 * Ejemplo:
 * ```kotlin
 * // Total: 100 páginas, página actual: 50, mostrar 5 páginas
 * val range = calculatePageRange(
 *     currentPage = 50,
 *     totalPages = 100,
 *     maxVisible = 5
 * )
 * // range = [48, 49, 50, 51, 52]
 *
 * // Para UI:
 * // < [48] [49] [50] [51] [52] >
 * //              ^^^^
 * //           (actual)
 * ```
 *
 * ## Casos Edge
 *
 * ```kotlin
 * // Cerca del inicio
 * calculatePageRange(currentPage = 1, totalPages = 100, maxVisible = 5)
 * // Retorna: [0, 1, 2, 3, 4]
 *
 * // Cerca del final
 * calculatePageRange(currentPage = 98, totalPages = 100, maxVisible = 5)
 * // Retorna: [95, 96, 97, 98, 99]
 *
 * // Menos páginas que maxVisible
 * calculatePageRange(currentPage = 2, totalPages = 3, maxVisible = 5)
 * // Retorna: [0, 1, 2]
 * ```
 *
 * @param currentPage Página actual (base 0)
 * @param totalPages Total de páginas disponibles
 * @param maxVisible Número máximo de páginas a mostrar
 * @return Lista de índices de página a mostrar
 */
public fun calculatePageRange(
    currentPage: Int,
    totalPages: Int,
    maxVisible: Int = 5
): List<Int> {
    if (totalPages <= 0) return emptyList()
    if (maxVisible <= 0) return emptyList()

    val actualMaxVisible = minOf(maxVisible, totalPages)
    val halfVisible = actualMaxVisible / 2

    val startPage = when {
        currentPage <= halfVisible -> 0
        currentPage >= totalPages - halfVisible -> totalPages - actualMaxVisible
        else -> currentPage - halfVisible
    }.coerceAtLeast(0)

    val endPage = minOf(startPage + actualMaxVisible, totalPages)

    return (startPage until endPage).toList()
}

/**
 * Extension property para obtener el rango de páginas visibles.
 *
 * Ejemplo:
 * ```kotlin
 * val result = getUsers(page = 50, pageSize = 10)
 * val pageRange = result.visiblePageRange()
 * // pageRange = [48, 49, 50, 51, 52] (por defecto 5 páginas)
 * ```
 *
 * @param maxVisible Número máximo de páginas a mostrar (por defecto 5)
 * @return Lista de índices de página a mostrar
 */
public fun <T> PagedResult<T>.visiblePageRange(maxVisible: Int = 5): List<Int> {
    return calculatePageRange(
        currentPage = page,
        totalPages = totalPages,
        maxVisible = maxVisible
    )
}

/**
 * Verifica si un número de página es válido para este resultado paginado.
 *
 * Ejemplo:
 * ```kotlin
 * val result = getUsers(page = 0, pageSize = 10) // 100 users total
 * result.isValidPage(5)   // true
 * result.isValidPage(15)  // false (solo hay 10 páginas: 0-9)
 * result.isValidPage(-1)  // false
 * ```
 *
 * @param pageIndex Índice de página a verificar
 * @return true si el índice está en el rango [0, totalPages)
 */
public fun <T> PagedResult<T>.isValidPage(pageIndex: Int): Boolean {
    return pageIndex >= 0 && pageIndex < totalPages
}

/**
 * Calcula el número de página para un índice de elemento específico.
 *
 * Ejemplo:
 * ```kotlin
 * // pageSize = 10
 * pageForItem(0)   // 0 (primera página)
 * pageForItem(5)   // 0 (primera página)
 * pageForItem(10)  // 1 (segunda página)
 * pageForItem(25)  // 2 (tercera página)
 * ```
 *
 * @param itemIndex Índice del elemento (base 0)
 * @param pageSize Tamaño de página
 * @return Índice de la página que contiene el elemento
 */
public fun pageForItem(itemIndex: Int, pageSize: Int): Int {
    require(itemIndex >= 0) { "Item index must be non-negative" }
    require(pageSize > 0) { "Page size must be positive" }
    return itemIndex / pageSize
}

/**
 * Información resumida de paginación como String legible.
 *
 * Útil para mostrar en UI o logs.
 *
 * Ejemplo:
 * ```kotlin
 * val result = getUsers(page = 0, pageSize = 10)
 * println(result.paginationSummary())
 * // Output: "Showing 10 of 100 items (Page 1 of 10)"
 *
 * val emptyResult = emptyPagedResult<User>()
 * println(emptyResult.paginationSummary())
 * // Output: "No items"
 * ```
 *
 * @return String con resumen de paginación
 */
public fun <T> PagedResult<T>.paginationSummary(): String {
    if (isEmpty) {
        return "No items"
    }

    val showing = itemCount
    val total = totalCount
    val currentPage = page + 1  // Display as 1-based
    val pages = totalPages

    return "Showing $showing of $total items (Page $currentPage of $pages)"
}
