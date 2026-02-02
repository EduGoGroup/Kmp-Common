package com.edugo.test.module.data.models.pagination

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith

/**
 * Suite de tests para PagedResult<T>.
 *
 * Verifica:
 * - Construcción y validación de parámetros
 * - Propiedades computadas (totalPages, hasNextPage, etc.)
 * - Transformaciones (map, mapIndexed, filter)
 * - Serialización/deserialización con diferentes tipos genéricos
 * - Casos edge (lista vacía, página fuera de rango, etc.)
 * - Navegación entre páginas
 */
class PagedResultTest {

    // ========== Test Data Classes ==========

    @Serializable
    data class TestItem(
        val id: Int,
        val name: String
    )

    @Serializable
    data class SimplifiedItem(
        val id: Int
    )

    // ========== Tests de Construcción y Validación ==========

    @Test
    fun `PagedResult se construye correctamente con parámetros válidos`() {
        val items = listOf(TestItem(1, "Item 1"), TestItem(2, "Item 2"))
        val result = PagedResult(
            items = items,
            totalCount = 100,
            page = 0,
            pageSize = 10
        )

        assertEquals(items, result.items)
        assertEquals(100, result.totalCount)
        assertEquals(0, result.page)
        assertEquals(10, result.pageSize)
    }

    @Test
    fun `PagedResult lanza excepción si page es negativo`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            PagedResult(
                items = emptyList<TestItem>(),
                totalCount = 100,
                page = -1,
                pageSize = 10
            )
        }
        assertTrue(exception.message!!.contains("Page index must be non-negative"))
    }

    @Test
    fun `PagedResult lanza excepción si pageSize es cero`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            PagedResult(
                items = emptyList<TestItem>(),
                totalCount = 100,
                page = 0,
                pageSize = 0
            )
        }
        assertTrue(exception.message!!.contains("Page size must be positive"))
    }

    @Test
    fun `PagedResult lanza excepción si pageSize es negativo`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            PagedResult(
                items = emptyList<TestItem>(),
                totalCount = 100,
                page = 0,
                pageSize = -5
            )
        }
        assertTrue(exception.message!!.contains("Page size must be positive"))
    }

    @Test
    fun `PagedResult lanza excepción si totalCount es negativo`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            PagedResult(
                items = emptyList<TestItem>(),
                totalCount = -1,
                page = 0,
                pageSize = 10
            )
        }
        assertTrue(exception.message!!.contains("Total count must be non-negative"))
    }

    @Test
    fun `PagedResult lanza excepción si items excede pageSize`() {
        val items = (1..15).map { TestItem(it, "Item $it") }
        val exception = assertFailsWith<IllegalArgumentException> {
            PagedResult(
                items = items,
                totalCount = 100,
                page = 0,
                pageSize = 10
            )
        }
        assertTrue(exception.message!!.contains("Items count (15) cannot exceed page size (10)"))
    }

    @Test
    fun `PagedResult acepta items igual a pageSize`() {
        val items = (1..10).map { TestItem(it, "Item $it") }
        val result = PagedResult(
            items = items,
            totalCount = 100,
            page = 0,
            pageSize = 10
        )

        assertEquals(10, result.itemCount)
    }

    @Test
    fun `PagedResult acepta lista vacía`() {
        val result = PagedResult(
            items = emptyList<TestItem>(),
            totalCount = 0,
            page = 0,
            pageSize = 10
        )

        assertTrue(result.isEmpty)
        assertEquals(0, result.itemCount)
    }

    // ========== Tests de Propiedades Computadas ==========

    @Test
    fun `totalPages se calcula correctamente`() {
        // 100 items, 10 per page = 10 pages
        val result1 = PagedResult(emptyList<TestItem>(), 100, 0, 10)
        assertEquals(10, result1.totalPages)

        // 95 items, 10 per page = 10 pages (ceil)
        val result2 = PagedResult(emptyList<TestItem>(), 95, 0, 10)
        assertEquals(10, result2.totalPages)

        // 101 items, 10 per page = 11 pages
        val result3 = PagedResult(emptyList<TestItem>(), 101, 0, 10)
        assertEquals(11, result3.totalPages)

        // 0 items = 0 pages
        val result4 = PagedResult(emptyList<TestItem>(), 0, 0, 10)
        assertEquals(0, result4.totalPages)
    }

    @Test
    fun `hasNextPage retorna true cuando no es la última página`() {
        val result = PagedResult(
            items = (1..10).map { TestItem(it, "Item $it") },
            totalCount = 100,
            page = 0,
            pageSize = 10
        )

        assertTrue(result.hasNextPage)
    }

    @Test
    fun `hasNextPage retorna false cuando es la última página`() {
        val result = PagedResult(
            items = (91..100).map { TestItem(it, "Item $it") },
            totalCount = 100,
            page = 9,
            pageSize = 10
        )

        assertFalse(result.hasNextPage)
    }

    @Test
    fun `hasPreviousPage retorna false en la primera página`() {
        val result = PagedResult(
            items = (1..10).map { TestItem(it, "Item $it") },
            totalCount = 100,
            page = 0,
            pageSize = 10
        )

        assertFalse(result.hasPreviousPage)
    }

    @Test
    fun `hasPreviousPage retorna true cuando no es la primera página`() {
        val result = PagedResult(
            items = (11..20).map { TestItem(it, "Item $it") },
            totalCount = 100,
            page = 1,
            pageSize = 10
        )

        assertTrue(result.hasPreviousPage)
    }

    @Test
    fun `isFirstPage retorna true para página 0`() {
        val result = PagedResult(
            items = listOf(TestItem(1, "Item 1")),
            totalCount = 100,
            page = 0,
            pageSize = 10
        )

        assertTrue(result.isFirstPage)
    }

    @Test
    fun `isFirstPage retorna false para página mayor a 0`() {
        val result = PagedResult(
            items = listOf(TestItem(1, "Item 1")),
            totalCount = 100,
            page = 1,
            pageSize = 10
        )

        assertFalse(result.isFirstPage)
    }

    @Test
    fun `isLastPage retorna true para la última página`() {
        val result = PagedResult(
            items = (91..100).map { TestItem(it, "Item $it") },
            totalCount = 100,
            page = 9,
            pageSize = 10
        )

        assertTrue(result.isLastPage)
    }

    @Test
    fun `isLastPage retorna false cuando no es la última página`() {
        val result = PagedResult(
            items = (1..10).map { TestItem(it, "Item $it") },
            totalCount = 100,
            page = 0,
            pageSize = 10
        )

        assertFalse(result.isLastPage)
    }

    @Test
    fun `isEmpty retorna true cuando items está vacío`() {
        val result = PagedResult(
            items = emptyList<TestItem>(),
            totalCount = 0,
            page = 0,
            pageSize = 10
        )

        assertTrue(result.isEmpty)
    }

    @Test
    fun `isNotEmpty retorna true cuando hay items`() {
        val result = PagedResult(
            items = listOf(TestItem(1, "Item 1")),
            totalCount = 1,
            page = 0,
            pageSize = 10
        )

        assertTrue(result.isNotEmpty)
    }

    @Test
    fun `itemCount retorna el número correcto de items`() {
        val items = (1..5).map { TestItem(it, "Item $it") }
        val result = PagedResult(
            items = items,
            totalCount = 100,
            page = 0,
            pageSize = 10
        )

        assertEquals(5, result.itemCount)
    }

    @Test
    fun `startIndex se calcula correctamente`() {
        // Página 0
        val result1 = PagedResult(emptyList<TestItem>(), 100, 0, 10)
        assertEquals(0, result1.startIndex)

        // Página 1
        val result2 = PagedResult(emptyList<TestItem>(), 100, 1, 10)
        assertEquals(10, result2.startIndex)

        // Página 5
        val result3 = PagedResult(emptyList<TestItem>(), 100, 5, 10)
        assertEquals(50, result3.startIndex)
    }

    @Test
    fun `endIndex se calcula correctamente`() {
        // Página con 5 items, empezando en índice 0
        val result1 = PagedResult(
            items = (1..5).map { TestItem(it, "Item $it") },
            totalCount = 100,
            page = 0,
            pageSize = 10
        )
        assertEquals(4, result1.endIndex)

        // Página 1 con 10 items
        val result2 = PagedResult(
            items = (11..20).map { TestItem(it, "Item $it") },
            totalCount = 100,
            page = 1,
            pageSize = 10
        )
        assertEquals(19, result2.endIndex)

        // Lista vacía
        val result3 = PagedResult(
            items = emptyList<TestItem>(),
            totalCount = 0,
            page = 0,
            pageSize = 10
        )
        assertEquals(-1, result3.endIndex)
    }

    // ========== Tests de Transformación: map() ==========

    @Test
    fun `map transforma items correctamente`() {
        val items = listOf(TestItem(1, "One"), TestItem(2, "Two"))
        val result = PagedResult(
            items = items,
            totalCount = 100,
            page = 0,
            pageSize = 10
        )

        val mapped = result.map { SimplifiedItem(it.id) }

        assertEquals(2, mapped.items.size)
        assertEquals(SimplifiedItem(1), mapped.items[0])
        assertEquals(SimplifiedItem(2), mapped.items[1])

        // Metadatos se preservan
        assertEquals(100, mapped.totalCount)
        assertEquals(0, mapped.page)
        assertEquals(10, mapped.pageSize)
    }

    @Test
    fun `map preserva metadatos de paginación`() {
        val result = PagedResult(
            items = (1..10).map { TestItem(it, "Item $it") },
            totalCount = 100,
            page = 2,
            pageSize = 10
        )

        val mapped = result.map { it.name }

        assertEquals(100, mapped.totalCount)
        assertEquals(2, mapped.page)
        assertEquals(10, mapped.pageSize)
        assertEquals(10, mapped.totalPages)
    }

    @Test
    fun `map con lista vacía retorna PagedResult vacío`() {
        val result = PagedResult(
            items = emptyList<TestItem>(),
            totalCount = 0,
            page = 0,
            pageSize = 10
        )

        val mapped = result.map { it.id.toString() }

        assertTrue(mapped.isEmpty)
        assertEquals(0, mapped.itemCount)
    }

    // ========== Tests de Transformación: mapIndexed() ==========

    @Test
    fun `mapIndexed transforma items con índice`() {
        val items = listOf(TestItem(1, "One"), TestItem(2, "Two"), TestItem(3, "Three"))
        val result = PagedResult(
            items = items,
            totalCount = 100,
            page = 0,
            pageSize = 10
        )

        val mapped = result.mapIndexed { index, item ->
            "${index + 1}. ${item.name}"
        }

        assertEquals("1. One", mapped.items[0])
        assertEquals("2. Two", mapped.items[1])
        assertEquals("3. Three", mapped.items[2])
    }

    @Test
    fun `mapIndexed preserva metadatos de paginación`() {
        val result = PagedResult(
            items = (1..5).map { TestItem(it, "Item $it") },
            totalCount = 50,
            page = 3,
            pageSize = 5
        )

        val mapped = result.mapIndexed { index, item -> "$index-${item.id}" }

        assertEquals(50, mapped.totalCount)
        assertEquals(3, mapped.page)
        assertEquals(5, mapped.pageSize)
    }

    // ========== Tests de Transformación: filter() ==========

    @Test
    fun `filter filtra items correctamente`() {
        val items = (1..10).map { TestItem(it, "Item $it") }
        val result = PagedResult(
            items = items,
            totalCount = 100,
            page = 0,
            pageSize = 10
        )

        val filtered = result.filter { it.id % 2 == 0 }

        assertEquals(5, filtered.itemCount)
        assertTrue(filtered.items.all { it.id % 2 == 0 })
    }

    @Test
    fun `filter preserva metadatos sin cambios`() {
        val result = PagedResult(
            items = (1..10).map { TestItem(it, "Item $it") },
            totalCount = 100,
            page = 0,
            pageSize = 10
        )

        val filtered = result.filter { it.id > 5 }

        // Metadatos NO cambian (comportamiento documentado)
        assertEquals(100, filtered.totalCount)
        assertEquals(0, filtered.page)
        assertEquals(10, filtered.pageSize)

        // Pero items sí se filtran
        assertEquals(5, filtered.itemCount)
    }

    @Test
    fun `filter con predicado que no coincide retorna items vacíos`() {
        val result = PagedResult(
            items = (1..10).map { TestItem(it, "Item $it") },
            totalCount = 100,
            page = 0,
            pageSize = 10
        )

        val filtered = result.filter { it.id > 100 }

        assertTrue(filtered.isEmpty)
        assertEquals(0, filtered.itemCount)
    }

    // ========== Tests de emptyPagedResult() ==========

    @Test
    fun `emptyPagedResult crea PagedResult vacío`() {
        val empty = emptyPagedResult<TestItem>()

        assertTrue(empty.isEmpty)
        assertEquals(0, empty.totalCount)
        assertEquals(0, empty.page)
        assertEquals(10, empty.pageSize)
        assertEquals(0, empty.totalPages)
    }

    @Test
    fun `emptyPagedResult con diferentes tipos genéricos`() {
        val emptyInt = emptyPagedResult<Int>()
        val emptyString = emptyPagedResult<String>()
        val emptyItem = emptyPagedResult<TestItem>()

        assertTrue(emptyInt.isEmpty)
        assertTrue(emptyString.isEmpty)
        assertTrue(emptyItem.isEmpty)
    }

    // ========== Tests de Serialización ==========

    @Test
    fun `PagedResult se serializa correctamente`() {
        val items = listOf(TestItem(1, "One"), TestItem(2, "Two"))
        val result = PagedResult(
            items = items,
            totalCount = 100,
            page = 2,
            pageSize = 10
        )

        val json = Json.encodeToString(result)

        assertTrue(json.contains("\"totalCount\":100"))
        assertTrue(json.contains("\"page\":2"))
        assertTrue(json.contains("\"pageSize\":10"))
    }

    @Test
    fun `PagedResult se deserializa correctamente`() {
        val original = PagedResult(
            items = listOf(TestItem(1, "One"), TestItem(2, "Two")),
            totalCount = 100,
            page = 2,
            pageSize = 10
        )

        val json = Json.encodeToString(original)
        val deserialized = Json.decodeFromString<PagedResult<TestItem>>(json)

        assertEquals(original.items, deserialized.items)
        assertEquals(original.totalCount, deserialized.totalCount)
        assertEquals(original.page, deserialized.page)
        assertEquals(original.pageSize, deserialized.pageSize)
    }

    @Test
    fun `serialización round-trip preserva todos los datos`() {
        val original = PagedResult(
            items = (1..10).map { TestItem(it, "Item $it") },
            totalCount = 100,
            page = 5,
            pageSize = 10
        )

        val json = Json.encodeToString(original)
        val deserialized = Json.decodeFromString<PagedResult<TestItem>>(json)

        assertEquals(original, deserialized)
        assertEquals(original.items.size, deserialized.items.size)
        assertEquals(original.totalPages, deserialized.totalPages)
        assertEquals(original.hasNextPage, deserialized.hasNextPage)
    }

    @Test
    fun `serialización con diferentes tipos genéricos`() {
        val intResult = PagedResult(items = listOf(1, 2, 3), totalCount = 3, page = 0, pageSize = 10)
        val stringResult = PagedResult(items = listOf("a", "b"), totalCount = 2, page = 0, pageSize = 10)

        val intJson = Json.encodeToString(intResult)
        val stringJson = Json.encodeToString(stringResult)

        val intDeserialized = Json.decodeFromString<PagedResult<Int>>(intJson)
        val stringDeserialized = Json.decodeFromString<PagedResult<String>>(stringJson)

        assertEquals(intResult, intDeserialized)
        assertEquals(stringResult, stringDeserialized)
    }

    @Test
    fun `serialización de PagedResult vacío`() {
        val empty = emptyPagedResult<TestItem>()
        val json = Json.encodeToString(empty)
        val deserialized = Json.decodeFromString<PagedResult<TestItem>>(json)

        assertTrue(deserialized.isEmpty)
        assertEquals(0, deserialized.totalCount)
    }

    // ========== Tests de Casos Edge ==========

    @Test
    fun `última página con menos items que pageSize`() {
        val items = (91..95).map { TestItem(it, "Item $it") }
        val result = PagedResult(
            items = items,
            totalCount = 95,
            page = 9,
            pageSize = 10
        )

        assertEquals(5, result.itemCount)
        assertTrue(result.isLastPage)
        assertFalse(result.hasNextPage)
    }

    @Test
    fun `página fuera de rango con items vacíos es válida`() {
        val result = PagedResult(
            items = emptyList<TestItem>(),
            totalCount = 10,
            page = 100,
            pageSize = 10
        )

        assertTrue(result.isEmpty)
        assertEquals(100, result.page)
    }

    @Test
    fun `totalCount cero con página cero`() {
        val result = PagedResult(
            items = emptyList<TestItem>(),
            totalCount = 0,
            page = 0,
            pageSize = 10
        )

        assertEquals(0, result.totalPages)
        assertTrue(result.isFirstPage)
        assertTrue(result.isLastPage)
        assertFalse(result.hasNextPage)
        assertFalse(result.hasPreviousPage)
    }

    @Test
    fun `una sola página con todos los elementos`() {
        val items = (1..5).map { TestItem(it, "Item $it") }
        val result = PagedResult(
            items = items,
            totalCount = 5,
            page = 0,
            pageSize = 10
        )

        assertEquals(1, result.totalPages)
        assertTrue(result.isFirstPage)
        assertTrue(result.isLastPage)
        assertFalse(result.hasNextPage)
        assertFalse(result.hasPreviousPage)
    }

    @Test
    fun `exactamente pageSize items en totalCount`() {
        val items = (1..10).map { TestItem(it, "Item $it") }
        val result = PagedResult(
            items = items,
            totalCount = 10,
            page = 0,
            pageSize = 10
        )

        assertEquals(1, result.totalPages)
        assertEquals(10, result.itemCount)
        assertTrue(result.isLastPage)
    }
}
