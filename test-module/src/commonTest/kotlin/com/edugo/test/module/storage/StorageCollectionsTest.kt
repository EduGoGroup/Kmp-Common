/*
 * Copyright (c) 2026 EduGo Project
 * Licensed under the MIT License
 */

package com.edugo.test.module.storage

import com.edugo.test.module.core.Result
import com.russhwolf.settings.MapSettings
import kotlinx.serialization.Serializable
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Serializable
data class TestItem(val id: Int, val name: String)

class StorageCollectionsTest {

    private lateinit var storage: EduGoStorage

    @BeforeTest
    fun setup() {
        storage = EduGoStorage.withSettings(MapSettings())
    }

    // ===== LIST TESTS =====

    @Test
    fun putList_and_getList_works() {
        val items = listOf(
            TestItem(1, "One"),
            TestItem(2, "Two"),
            TestItem(3, "Three")
        )

        storage.putList("items", items)
        val retrieved = storage.getList<TestItem>("items")

        assertEquals(items, retrieved)
    }

    @Test
    fun getList_returns_empty_list_for_missing_key() {
        val result = storage.getList<TestItem>("missing")
        assertTrue(result.isEmpty())
    }

    @Test
    fun getListSafe_returns_Success_for_valid_list() {
        val items = listOf(TestItem(1, "One"), TestItem(2, "Two"))
        storage.putList("items", items)

        val result = storage.getListSafe<TestItem>("items")

        assertTrue(result is Result.Success)
        assertEquals(items, (result as Result.Success).data)
    }

    @Test
    fun getListSafe_returns_Failure_for_missing_key() {
        val result = storage.getListSafe<TestItem>("missing")
        assertTrue(result is Result.Failure)
    }

    @Test
    fun addToList_appends_element() {
        storage.putList("items", listOf(TestItem(1, "One")))
        storage.addToList("items", TestItem(2, "Two"))

        val items = storage.getList<TestItem>("items")
        assertEquals(2, items.size)
        assertEquals(TestItem(2, "Two"), items[1])
    }

    @Test
    fun addToList_creates_list_if_not_exists() {
        storage.addToList("newList", TestItem(1, "First"))

        val items = storage.getList<TestItem>("newList")
        assertEquals(1, items.size)
        assertEquals(TestItem(1, "First"), items[0])
    }

    @Test
    fun removeFromList_removes_element() {
        val items = listOf(TestItem(1, "One"), TestItem(2, "Two"))
        storage.putList("items", items)

        storage.removeFromList("items", TestItem(1, "One"))

        val remaining = storage.getList<TestItem>("items")
        assertEquals(1, remaining.size)
        assertEquals(TestItem(2, "Two"), remaining[0])
    }

    @Test
    fun removeFromList_does_nothing_if_element_not_found() {
        val items = listOf(TestItem(1, "One"), TestItem(2, "Two"))
        storage.putList("items", items)

        storage.removeFromList("items", TestItem(99, "NotExist"))

        val remaining = storage.getList<TestItem>("items")
        assertEquals(2, remaining.size)
    }

    @Test
    fun empty_list_can_be_stored_and_retrieved() {
        storage.putList<TestItem>("empty", emptyList())

        val retrieved = storage.getList<TestItem>("empty")
        assertTrue(retrieved.isEmpty())
    }

    // ===== SET TESTS =====

    @Test
    fun putSet_and_getSet_works() {
        val tags = setOf("kotlin", "multiplatform", "storage")

        storage.putSet("tags", tags)
        val retrieved = storage.getSet<String>("tags")

        assertEquals(tags, retrieved)
    }

    @Test
    fun getSet_returns_empty_set_for_missing_key() {
        val result = storage.getSet<String>("missing")
        assertTrue(result.isEmpty())
    }

    @Test
    fun putSet_removes_duplicates() {
        val withDuplicates = setOf("a", "b", "a", "c", "b")

        storage.putSet("unique", withDuplicates)
        val retrieved = storage.getSet<String>("unique")

        assertEquals(3, retrieved.size)
        assertTrue(retrieved.containsAll(setOf("a", "b", "c")))
    }

    @Test
    fun set_of_objects_works() {
        val itemSet = setOf(
            TestItem(1, "One"),
            TestItem(2, "Two"),
            TestItem(3, "Three")
        )

        storage.putSet("itemSet", itemSet)
        val retrieved = storage.getSet<TestItem>("itemSet")

        assertEquals(itemSet, retrieved)
    }

    // ===== MAP TESTS =====

    @Test
    fun putMap_and_getMap_works() {
        val settings = mapOf(
            "theme" to "dark",
            "language" to "es",
            "fontSize" to "14"
        )

        storage.putMap("settings", settings)
        val retrieved = storage.getMap<String>("settings")

        assertEquals(settings, retrieved)
    }

    @Test
    fun getMap_returns_empty_map_for_missing_key() {
        val result = storage.getMap<String>("missing")
        assertTrue(result.isEmpty())
    }

    @Test
    fun map_with_object_values_works() {
        val userMap = mapOf(
            "admin" to TestItem(1, "Admin User"),
            "guest" to TestItem(2, "Guest User")
        )

        storage.putMap("users", userMap)
        val retrieved = storage.getMap<TestItem>("users")

        assertEquals(userMap, retrieved)
        assertEquals(TestItem(1, "Admin User"), retrieved["admin"])
        assertEquals(TestItem(2, "Guest User"), retrieved["guest"])
    }

    @Test
    fun map_with_int_values_works() {
        val scores = mapOf(
            "player1" to 100,
            "player2" to 250,
            "player3" to 75
        )

        storage.putMap("scores", scores)
        val retrieved = storage.getMap<Int>("scores")

        assertEquals(scores, retrieved)
    }

    // ===== PRIMITIVE LISTS =====

    @Test
    fun list_of_integers_works() {
        val numbers = listOf(1, 2, 3, 4, 5)
        storage.putList("numbers", numbers)

        val retrieved = storage.getList<Int>("numbers")
        assertEquals(numbers, retrieved)
    }

    @Test
    fun list_of_strings_works() {
        val names = listOf("Alice", "Bob", "Charlie")
        storage.putList("names", names)

        val retrieved = storage.getList<String>("names")
        assertEquals(names, retrieved)
    }

    @Test
    fun list_of_booleans_works() {
        val flags = listOf(true, false, true, true)
        storage.putList("flags", flags)

        val retrieved = storage.getList<Boolean>("flags")
        assertEquals(flags, retrieved)
    }

    @Test
    fun list_of_doubles_works() {
        val values = listOf(1.1, 2.2, 3.3, 4.4)
        storage.putList("values", values)

        val retrieved = storage.getList<Double>("values")
        assertEquals(values, retrieved)
    }

    // ===== DELEGATED PROPERTY FOR LISTS =====

    @Test
    fun serializableList_delegate_reads_and_writes() {
        var items: List<TestItem> by storage.serializableList("delegate.items")

        assertTrue(items.isEmpty())

        items = listOf(TestItem(1, "First"), TestItem(2, "Second"))
        assertEquals(2, items.size)
        assertEquals(TestItem(1, "First"), items[0])

        items = emptyList()
        assertTrue(items.isEmpty())
    }
}
