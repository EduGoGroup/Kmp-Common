package com.edugo.test.module.data.models

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SampleModelTest {

    private val json = Json { ignoreUnknownKeys = true }

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
    }
}
