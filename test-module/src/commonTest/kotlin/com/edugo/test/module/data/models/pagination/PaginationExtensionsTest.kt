package com.edugo.test.module.data.models.pagination

import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith

/**
 * Suite de tests para extension functions de paginación.
 *
 * Verifica:
 * - toPagedResult(): conversión de listas a PagedResult
 * - paginate(): paginación en memoria de listas
 * - toList(): extracción de items
 * - merge(): combinación de múltiples PagedResults
 * - calculatePageRange(): cálculo de rangos de páginas para UI
 * - visiblePageRange(): rango de páginas visibles
 * - isValidPage(): validación de índice de página
 * - pageForItem(): cálculo de página para un item
 * - paginationSummary(): resumen legible de paginación
 * - Casos edge y validaciones
 */
class PaginationExtensionsTest {

    // ========== Test Data ==========

    @Serializable
    data class Item(val id: Int, val name: String)

    // ========== Tests de toPagedResult() ==========

    @Test
    fun `toPagedResult convierte lista a PagedResult correctamente`() {
        val items = listOf(Item(1, "One"), Item(2, "Two"))
        val result = items.toPagedResult(page = 0, pageSize = 10, totalCount = 100)

        assertEquals(items, result.items)
        assertEquals(0, result.page)
        assertEquals(10, result.pageSize)
        assertEquals(100, result.totalCount)
    }

    @Test
    fun `toPagedResult con lista vacía`() {
        val result = emptyList<Item>().toPagedResult(page = 0, pageSize = 10, totalCount = 0)

        assertTrue(result.isEmpty)
        assertEquals(0, result.totalCount)
    }

    @Test
    fun `toPagedResult lanza excepción con página negativa`() {
        val items = listOf(Item(1, "One"))
        assertFailsWith<IllegalArgumentException> {
            items.toPagedResult(page = -1, pageSize = 10, totalCount = 100)
        }
    }

    @Test
    fun `toPagedResult lanza excepción con pageSize cero`() {
        val items = listOf(Item(1, "One"))
        assertFailsWith<IllegalArgumentException> {
            items.toPagedResult(page = 0, pageSize = 0, totalCount = 100)
        }
    }

    @Test
    fun `toPagedResult lanza excepción si items excede pageSize`() {
        val items = (1..15).map { Item(it, "Item $it") }
        assertFailsWith<IllegalArgumentException> {
            items.toPagedResult(page = 0, pageSize = 10, totalCount = 100)
        }
    }

    // ========== Tests de paginate() ==========

    @Test
    fun `paginate divide lista correctamente en primera página`() {
        val allItems = (1..100).map { Item(it, "Item $it") }
        val result = allItems.paginate(page = 0, pageSize = 10)

        assertEquals(10, result.itemCount)
        assertEquals((1..10).map { Item(it, "Item $it") }, result.items)
        assertEquals(100, result.totalCount)
        assertEquals(10, result.totalPages)
    }

    @Test
    fun `paginate divide lista correctamente en segunda página`() {
        val allItems = (1..100).map { Item(it, "Item $it") }
        val result = allItems.paginate(page = 1, pageSize = 10)

        assertEquals(10, result.itemCount)
        assertEquals((11..20).map { Item(it, "Item $it") }, result.items)
        assertEquals(1, result.page)
    }

    @Test
    fun `paginate última página con menos items`() {
        val allItems = (1..25).map { Item(it, "Item $it") }
        val result = allItems.paginate(page = 2, pageSize = 10)

        assertEquals(5, result.itemCount)
        assertEquals((21..25).map { Item(it, "Item $it") }, result.items)
        assertTrue(result.isLastPage)
    }

    @Test
    fun `paginate con página fuera de rango retorna items vacíos`() {
        val allItems = (1..10).map { Item(it, "Item $it") }
        val result = allItems.paginate(page = 10, pageSize = 10)

        assertTrue(result.isEmpty)
        assertEquals(10, result.totalCount)
        assertEquals(10, result.page)
    }

    @Test
    fun `paginate con lista vacía`() {
        val result = emptyList<Item>().paginate(page = 0, pageSize = 10)

        assertTrue(result.isEmpty)
        assertEquals(0, result.totalCount)
        assertEquals(0, result.totalPages)
    }

    @Test
    fun `paginate lanza excepción con page negativo`() {
        val items = listOf(Item(1, "One"))
        assertFailsWith<IllegalArgumentException> {
            items.paginate(page = -1, pageSize = 10)
        }
    }

    @Test
    fun `paginate lanza excepción con pageSize cero`() {
        val items = listOf(Item(1, "One"))
        assertFailsWith<IllegalArgumentException> {
            items.paginate(page = 0, pageSize = 0)
        }
    }

    @Test
    fun `paginate con lista más pequeña que pageSize`() {
        val allItems = (1..5).map { Item(it, "Item $it") }
        val result = allItems.paginate(page = 0, pageSize = 10)

        assertEquals(5, result.itemCount)
        assertEquals(allItems, result.items)
        assertEquals(1, result.totalPages)
    }

    @Test
    fun `paginate con múltiples páginas completas`() {
        val allItems = (1..30).map { Item(it, "Item $it") }

        // Primera página
        val page0 = allItems.paginate(page = 0, pageSize = 10)
        assertEquals((1..10).map { Item(it, "Item $it") }, page0.items)

        // Segunda página
        val page1 = allItems.paginate(page = 1, pageSize = 10)
        assertEquals((11..20).map { Item(it, "Item $it") }, page1.items)

        // Tercera página
        val page2 = allItems.paginate(page = 2, pageSize = 10)
        assertEquals((21..30).map { Item(it, "Item $it") }, page2.items)
    }

    // ========== Tests de toList() ==========

    @Test
    fun `toList retorna items del PagedResult`() {
        val items = (1..10).map { Item(it, "Item $it") }
        val result = PagedResult(items, 100, 0, 10)
        val list = result.toList()

        assertEquals(items, list)
    }

    @Test
    fun `toList con PagedResult vacío retorna lista vacía`() {
        val result = emptyPagedResult<Item>()
        val list = result.toList()

        assertTrue(list.isEmpty())
    }

    // ========== Tests de merge() ==========

    @Test
    fun `merge combina múltiples PagedResults`() {
        val page1 = PagedResult((1..10).map { Item(it, "Item $it") }, 30, 0, 10)
        val page2 = PagedResult((11..20).map { Item(it, "Item $it") }, 30, 1, 10)
        val page3 = PagedResult((21..30).map { Item(it, "Item $it") }, 30, 2, 10)

        val merged = listOf(page1, page2, page3).merge()

        assertEquals(30, merged.itemCount)
        assertEquals(30, merged.totalCount)
        assertEquals((1..30).map { Item(it, "Item $it") }, merged.items)
    }

    @Test
    fun `merge con lista vacía retorna emptyPagedResult`() {
        val merged = emptyList<PagedResult<Item>>().merge()

        assertTrue(merged.isEmpty)
        assertEquals(0, merged.totalCount)
    }

    @Test
    fun `merge con un solo PagedResult retorna el mismo`() {
        val single = PagedResult((1..10).map { Item(it, "Item $it") }, 100, 0, 10)
        val merged = listOf(single).merge()

        assertEquals(10, merged.itemCount)
        assertEquals(100, merged.totalCount)
    }

    @Test
    fun `merge toma metadatos del primer PagedResult`() {
        val page1 = PagedResult((1..5).map { Item(it, "Item $it") }, 20, 0, 5)
        val page2 = PagedResult((6..10).map { Item(it, "Item $it") }, 20, 1, 5)

        val merged = listOf(page1, page2).merge()

        assertEquals(20, merged.totalCount)
        assertEquals(0, merged.page)
        // pageSize se ajusta al tamaño real de items combinados
        assertEquals(10, merged.pageSize)
    }

    // ========== Tests de calculatePageRange() ==========

    @Test
    fun `calculatePageRange retorna rango centrado en página actual`() {
        val range = calculatePageRange(currentPage = 50, totalPages = 100, maxVisible = 5)

        assertEquals(listOf(48, 49, 50, 51, 52), range)
    }

    @Test
    fun `calculatePageRange cerca del inicio`() {
        val range = calculatePageRange(currentPage = 1, totalPages = 100, maxVisible = 5)

        assertEquals(listOf(0, 1, 2, 3, 4), range)
    }

    @Test
    fun `calculatePageRange cerca del final`() {
        val range = calculatePageRange(currentPage = 98, totalPages = 100, maxVisible = 5)

        assertEquals(listOf(95, 96, 97, 98, 99), range)
    }

    @Test
    fun `calculatePageRange con menos páginas que maxVisible`() {
        val range = calculatePageRange(currentPage = 1, totalPages = 3, maxVisible = 5)

        assertEquals(listOf(0, 1, 2), range)
    }

    @Test
    fun `calculatePageRange con totalPages cero retorna lista vacía`() {
        val range = calculatePageRange(currentPage = 0, totalPages = 0, maxVisible = 5)

        assertTrue(range.isEmpty())
    }

    @Test
    fun `calculatePageRange con maxVisible cero retorna lista vacía`() {
        val range = calculatePageRange(currentPage = 5, totalPages = 10, maxVisible = 0)

        assertTrue(range.isEmpty())
    }

    @Test
    fun `calculatePageRange con maxVisible 1 retorna solo página actual o ajustada`() {
        val range = calculatePageRange(currentPage = 5, totalPages = 10, maxVisible = 1)

        assertEquals(1, range.size)
    }

    @Test
    fun `calculatePageRange en primera página`() {
        val range = calculatePageRange(currentPage = 0, totalPages = 10, maxVisible = 5)

        assertEquals(listOf(0, 1, 2, 3, 4), range)
    }

    @Test
    fun `calculatePageRange en última página`() {
        val range = calculatePageRange(currentPage = 9, totalPages = 10, maxVisible = 5)

        assertEquals(listOf(5, 6, 7, 8, 9), range)
    }

    // ========== Tests de visiblePageRange() ==========

    @Test
    fun `visiblePageRange retorna rango visible para PagedResult`() {
        val result = PagedResult(emptyList<Item>(), 1000, 50, 10)
        val range = result.visiblePageRange(maxVisible = 5)

        assertEquals(listOf(48, 49, 50, 51, 52), range)
    }

    @Test
    fun `visiblePageRange usa valor por defecto de 5`() {
        val result = PagedResult(emptyList<Item>(), 1000, 50, 10)
        val range = result.visiblePageRange()

        assertEquals(5, range.size)
    }

    @Test
    fun `visiblePageRange en primera página`() {
        val result = PagedResult((1..10).map { Item(it, "Item $it") }, 100, 0, 10)
        val range = result.visiblePageRange(maxVisible = 5)

        assertEquals(listOf(0, 1, 2, 3, 4), range)
    }

    @Test
    fun `visiblePageRange con una sola página`() {
        val result = PagedResult((1..5).map { Item(it, "Item $it") }, 5, 0, 10)
        val range = result.visiblePageRange(maxVisible = 5)

        assertEquals(listOf(0), range)
    }

    // ========== Tests de isValidPage() ==========

    @Test
    fun `isValidPage retorna true para páginas válidas`() {
        val result = PagedResult(emptyList<Item>(), 100, 0, 10)

        assertTrue(result.isValidPage(0))
        assertTrue(result.isValidPage(5))
        assertTrue(result.isValidPage(9))
    }

    @Test
    fun `isValidPage retorna false para páginas inválidas`() {
        val result = PagedResult(emptyList<Item>(), 100, 0, 10)

        assertFalse(result.isValidPage(-1))
        assertFalse(result.isValidPage(10))
        assertFalse(result.isValidPage(100))
    }

    @Test
    fun `isValidPage con totalPages cero`() {
        val result = PagedResult(emptyList<Item>(), 0, 0, 10)

        assertFalse(result.isValidPage(0))
        assertFalse(result.isValidPage(1))
    }

    @Test
    fun `isValidPage con una sola página`() {
        val result = PagedResult((1..5).map { Item(it, "Item $it") }, 5, 0, 10)

        assertTrue(result.isValidPage(0))
        assertFalse(result.isValidPage(1))
    }

    // ========== Tests de pageForItem() ==========

    @Test
    fun `pageForItem calcula página correctamente`() {
        assertEquals(0, pageForItem(itemIndex = 0, pageSize = 10))
        assertEquals(0, pageForItem(itemIndex = 5, pageSize = 10))
        assertEquals(0, pageForItem(itemIndex = 9, pageSize = 10))
        assertEquals(1, pageForItem(itemIndex = 10, pageSize = 10))
        assertEquals(1, pageForItem(itemIndex = 15, pageSize = 10))
        assertEquals(2, pageForItem(itemIndex = 25, pageSize = 10))
    }

    @Test
    fun `pageForItem lanza excepción con itemIndex negativo`() {
        assertFailsWith<IllegalArgumentException> {
            pageForItem(itemIndex = -1, pageSize = 10)
        }
    }

    @Test
    fun `pageForItem lanza excepción con pageSize cero`() {
        assertFailsWith<IllegalArgumentException> {
            pageForItem(itemIndex = 5, pageSize = 0)
        }
    }

    @Test
    fun `pageForItem con diferentes tamaños de página`() {
        // pageSize = 5
        assertEquals(0, pageForItem(itemIndex = 4, pageSize = 5))
        assertEquals(1, pageForItem(itemIndex = 5, pageSize = 5))
        assertEquals(2, pageForItem(itemIndex = 10, pageSize = 5))

        // pageSize = 20
        assertEquals(0, pageForItem(itemIndex = 19, pageSize = 20))
        assertEquals(1, pageForItem(itemIndex = 20, pageSize = 20))
        assertEquals(5, pageForItem(itemIndex = 100, pageSize = 20))
    }

    // ========== Tests de paginationSummary() ==========

    @Test
    fun `paginationSummary con items`() {
        val result = PagedResult(
            items = (1..10).map { Item(it, "Item $it") },
            totalCount = 100,
            page = 0,
            pageSize = 10
        )

        val summary = result.paginationSummary()

        assertEquals("Showing 10 of 100 items (Page 1 of 10)", summary)
    }

    @Test
    fun `paginationSummary con página vacía retorna No items`() {
        val result = emptyPagedResult<Item>()
        val summary = result.paginationSummary()

        assertEquals("No items", summary)
    }

    @Test
    fun `paginationSummary en segunda página`() {
        val result = PagedResult(
            items = (11..20).map { Item(it, "Item $it") },
            totalCount = 50,
            page = 1,
            pageSize = 10
        )

        val summary = result.paginationSummary()

        assertEquals("Showing 10 of 50 items (Page 2 of 5)", summary)
    }

    @Test
    fun `paginationSummary en última página con menos items`() {
        val result = PagedResult(
            items = (91..95).map { Item(it, "Item $it") },
            totalCount = 95,
            page = 9,
            pageSize = 10
        )

        val summary = result.paginationSummary()

        assertEquals("Showing 5 of 95 items (Page 10 of 10)", summary)
    }

    @Test
    fun `paginationSummary con una sola página`() {
        val result = PagedResult(
            items = (1..5).map { Item(it, "Item $it") },
            totalCount = 5,
            page = 0,
            pageSize = 10
        )

        val summary = result.paginationSummary()

        assertEquals("Showing 5 of 5 items (Page 1 of 1)", summary)
    }

    // ========== Tests de Casos de Uso Reales ==========

    @Test
    fun `simular navegación entre páginas con paginate`() {
        val allItems = (1..100).map { Item(it, "Item $it") }

        // Primera página
        var current = allItems.paginate(page = 0, pageSize = 10)
        assertEquals(0, current.page)
        assertTrue(current.hasNextPage)
        assertFalse(current.hasPreviousPage)

        // Siguiente página
        current = allItems.paginate(page = current.page + 1, pageSize = 10)
        assertEquals(1, current.page)
        assertTrue(current.hasNextPage)
        assertTrue(current.hasPreviousPage)

        // Ir a última página
        current = allItems.paginate(page = 9, pageSize = 10)
        assertEquals(9, current.page)
        assertFalse(current.hasNextPage)
        assertTrue(current.hasPreviousPage)
    }

    @Test
    fun `cargar múltiples páginas y combinarlas`() {
        val allItems = (1..30).map { Item(it, "Item $it") }

        val page1 = allItems.paginate(page = 0, pageSize = 10)
        val page2 = allItems.paginate(page = 1, pageSize = 10)
        val page3 = allItems.paginate(page = 2, pageSize = 10)

        val combined = listOf(page1, page2, page3).merge()

        assertEquals(30, combined.itemCount)
        assertEquals(allItems, combined.items)
    }

    @Test
    fun `transformar PagedResult y extraer lista`() {
        val items = (1..10).map { Item(it, "Item $it") }
        val paged = items.toPagedResult(page = 0, pageSize = 10, totalCount = 100)

        val transformed = paged.map { it.name }
        val list = transformed.toList()

        assertEquals((1..10).map { "Item $it" }, list)
    }

    @Test
    fun `crear controles de paginación con visiblePageRange`() {
        val result = PagedResult(emptyList<Item>(), 1000, 50, 10)
        val visiblePages = result.visiblePageRange(maxVisible = 7)

        // Debería mostrar: [47, 48, 49, 50, 51, 52, 53]
        assertEquals(7, visiblePages.size)
        assertTrue(visiblePages.contains(result.page))
    }

    @Test
    fun `validar navegación antes de cambiar de página`() {
        val result = PagedResult((1..10).map { Item(it, "Item $it") }, 100, 5, 10)

        // Validar que podemos ir a página siguiente
        val nextPage = result.page + 1
        assertTrue(result.isValidPage(nextPage))
        assertTrue(result.hasNextPage)

        // Validar que podemos ir a página anterior
        val prevPage = result.page - 1
        assertTrue(result.isValidPage(prevPage))
        assertTrue(result.hasPreviousPage)
    }

    @Test
    fun `buscar página que contiene un item específico`() {
        val itemIndex = 47 // Item #48 (base 0)
        val pageSize = 10
        val pageNumber = pageForItem(itemIndex, pageSize)

        assertEquals(4, pageNumber) // Página 4 (índices 40-49)

        // Verificar con paginación real
        val allItems = (0..99).map { Item(it, "Item $it") }
        val page = allItems.paginate(page = pageNumber, pageSize = pageSize)

        assertTrue(page.items.any { it.id == itemIndex })
    }

    @Test
    fun `mostrar resumen de paginación en UI`() {
        val result = PagedResult(
            items = (1..20).map { Item(it, "Item $it") },
            totalCount = 200,
            page = 0,
            pageSize = 20
        )

        val summary = result.paginationSummary()
        val visiblePages = result.visiblePageRange()

        // Para UI podríamos mostrar:
        // "Showing 20 of 200 items (Page 1 of 10)"
        // [1] [2] [3] [4] [5] >

        assertTrue(summary.contains("Showing 20 of 200 items"))
        assertEquals(listOf(0, 1, 2, 3, 4), visiblePages)
    }
}
