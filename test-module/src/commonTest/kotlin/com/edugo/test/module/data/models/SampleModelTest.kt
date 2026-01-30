package com.edugo.test.module.data.models

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotEquals
import kotlin.test.assertFails

class SampleModelTest {

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }

    @Test
    fun serialization_roundTrip() {
        val original = SampleModel(
            id = "123",
            name = "Test",
            timestamp = 1234567890L
        )

        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<SampleModel>(jsonString)

        assertEquals(original, decoded)
    }

    @Test
    fun serialization_producesValidJson() {
        val model = SampleModel(id = "1", name = "Test", timestamp = 0L)
        val jsonString = json.encodeToString(model)

        assertTrue(jsonString.contains("\"id\":\"1\""))
        assertTrue(jsonString.contains("\"name\":\"Test\""))
        assertTrue(jsonString.contains("\"timestamp\":0"))
    }

    @Test
    fun model_withEmptyId() {
        val model = SampleModel(id = "", name = "Test", timestamp = 0L)
        assertEquals("", model.id)
    }

    @Test
    fun model_withNegativeTimestamp() {
        val model = SampleModel(id = "1", name = "Test", timestamp = -1L)
        assertEquals(-1L, model.timestamp)
    }

    @Test
    fun model_withSpecialCharactersInName() {
        val model = SampleModel(id = "1", name = "Test @#$%", timestamp = 0L)
        assertEquals("Test @#$%", model.name)
    }

    @Test
    fun deserialization_withMissingField_fails() {
        val jsonString = """{"id":"1","name":"Test"}"""
        assertFails {
            json.decodeFromString<SampleModel>(jsonString)
        }
    }

    @Test
    fun deserialization_withInvalidType_fails() {
        val jsonString = """{"id":"1","name":"Test","timestamp":"invalid"}"""
        assertFails {
            json.decodeFromString<SampleModel>(jsonString)
        }
    }

    @Test
    fun deserialization_withMalformedJson_fails() {
        val jsonString = """{"id":"1","name":"Test","timestamp":0"""
        assertFails {
            json.decodeFromString<SampleModel>(jsonString)
        }
    }

    @Test
    fun equality_sameValues() {
        val model1 = SampleModel(id = "1", name = "Test", timestamp = 0L)
        val model2 = SampleModel(id = "1", name = "Test", timestamp = 0L)
        assertEquals(model1, model2)
        assertEquals(model1.hashCode(), model2.hashCode())
    }

    @Test
    fun equality_differentValues() {
        val model1 = SampleModel(id = "1", name = "Test", timestamp = 0L)
        val model2 = SampleModel(id = "2", name = "Test", timestamp = 0L)
        assertNotEquals(model1, model2)
    }
}
