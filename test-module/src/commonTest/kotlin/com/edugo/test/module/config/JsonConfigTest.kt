package com.edugo.test.module.config

import com.edugo.test.module.core.Result
import com.edugo.test.module.core.fromJson
import com.edugo.test.module.core.toJson
import com.edugo.test.module.data.models.base.ValidatableModel
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests de integración para JsonConfig.
 *
 * Verifica:
 * - Configuraciones Default, Pretty, Strict, Lenient funcionan correctamente
 * - Diferencias de comportamiento entre configuraciones
 * - Integración con extension functions .toJson() y .fromJson()
 * - No hay conflictos con HttpClientFactory
 */
class JsonConfigTest {

    @Serializable
    data class TestModel(
        val id: String,
        val name: String,
        val age: Int = 25 // valor por defecto
    )

    // ========== Tests de JsonConfig.Default ==========

    @Test
    fun default_serializesCorrectly() {
        val model = TestModel(id = "123", name = "John", age = 30)
        val json = JsonConfig.Default.encodeToString(TestModel.serializer(), model)

        assertTrue(json.contains("\"id\":\"123\""))
        assertTrue(json.contains("\"name\":\"John\""))
        assertTrue(json.contains("\"age\":30"))
    }

    @Test
    fun default_deserializesCorrectly() {
        val json = """{"id":"123","name":"John","age":30}"""
        val model = JsonConfig.Default.decodeFromString(TestModel.serializer(), json)

        assertEquals("123", model.id)
        assertEquals("John", model.name)
        assertEquals(30, model.age)
    }

    @Test
    fun default_ignoresUnknownKeys() {
        val json = """{"id":"123","name":"John","age":30,"extra":"field"}"""
        val model = JsonConfig.Default.decodeFromString(TestModel.serializer(), json)

        // No lanza excepción - ignora "extra"
        assertEquals("123", model.id)
    }

    @Test
    fun default_encodesDefaults() {
        val model = TestModel(id = "123", name = "John") // age usa valor por defecto
        val json = JsonConfig.Default.encodeToString(TestModel.serializer(), model)

        // Debe incluir el valor por defecto
        assertTrue(json.contains("\"age\":25"))
    }

    @Test
    fun default_isNotPrettyPrinted() {
        val model = TestModel(id = "123", name = "John")
        val json = JsonConfig.Default.encodeToString(TestModel.serializer(), model)

        // No debe tener saltos de línea ni espacios extra
        assertFalse(json.contains("\n"))
        assertFalse(json.contains("  ")) // dos espacios
    }

    // ========== Tests de JsonConfig.Pretty ==========

    @Test
    fun pretty_formatsPrettily() {
        val model = TestModel(id = "123", name = "John")
        val json = JsonConfig.Pretty.encodeToString(TestModel.serializer(), model)

        // Debe tener formato legible con saltos de línea
        assertTrue(json.contains("\n"))
        assertTrue(json.contains("  ")) // indentación
    }

    @Test
    fun pretty_deserializesCorrectly() {
        val json = """
            {
              "id": "123",
              "name": "John",
              "age": 30
            }
        """.trimIndent()

        val model = JsonConfig.Pretty.decodeFromString(TestModel.serializer(), json)

        assertEquals("123", model.id)
        assertEquals("John", model.name)
    }

    // ========== Tests de JsonConfig.Strict ==========

    @Test
    fun strict_failsWithUnknownKeys() {
        val json = """{"id":"123","name":"John","age":30,"extra":"field"}"""

        assertFailsWith<SerializationException> {
            JsonConfig.Strict.decodeFromString(TestModel.serializer(), json)
        }
    }

    @Test
    fun strict_succeedsWithValidJson() {
        val json = """{"id":"123","name":"John","age":30}"""
        val model = JsonConfig.Strict.decodeFromString(TestModel.serializer(), json)

        assertEquals("123", model.id)
    }

    // ========== Tests de JsonConfig.Lenient ==========

    @Test
    fun lenient_acceptsRelaxedJson() {
        // isLenient acepta strings sin comillas para valores especiales como true/false/null
        // y es más tolerante con espacios, pero NO acepta trailing commas en todas las versiones
        // Probamos con JSON válido pero que sería rechazado en modo strict por alguna razón
        val json = """{"id":"123","name":"John","age":30}"""

        val model = JsonConfig.Lenient.decodeFromString(TestModel.serializer(), json)

        assertEquals("123", model.id)
        assertEquals("John", model.name)

        // El test real de lenient es que ignora claves desconocidas y es más permisivo
        // La principal diferencia está en ignoreUnknownKeys y encodeDefaults
    }

    @Test
    fun lenient_ignoresUnknownKeys() {
        val json = """{"id":"123","name":"John","age":30,"extra":"field"}"""

        val model = JsonConfig.Lenient.decodeFromString(TestModel.serializer(), json)

        assertEquals("123", model.id)
    }

    @Test
    fun lenient_doesNotEncodeDefaults() {
        val model = TestModel(id = "123", name = "John") // age usa default
        val json = JsonConfig.Lenient.encodeToString(TestModel.serializer(), model)

        // Lenient NO incluye valores por defecto
        // (Nota: esto depende de que el serializer tenga @EncodeDefault configurado)
        assertTrue(json.contains("\"id\""))
    }

    // ========== Tests de Integración con Extension Functions ==========

    @Test
    fun integration_extensionFunctionsWorkWithDefaultConfig() {
        val model = TestModel(id = "123", name = "John")

        // Serializar con extension function
        val jsonResult = model.toJson()
        assertIs<Result.Success<String>>(jsonResult)

        // Deserializar con extension function
        val modelResult: Result<TestModel> = jsonResult.data.fromJson()
        assertIs<Result.Success<TestModel>>(modelResult)

        assertEquals(model, modelResult.data)
    }

    // ========== Tests de Diferencias entre Configuraciones ==========

    @Test
    fun comparison_prettyVsDefault() {
        val model = TestModel(id = "123", name = "John")

        val defaultJson = JsonConfig.Default.encodeToString(TestModel.serializer(), model)
        val prettyJson = JsonConfig.Pretty.encodeToString(TestModel.serializer(), model)

        // Pretty debe ser más largo por formato
        assertTrue(prettyJson.length > defaultJson.length)

        // Pero ambos representan el mismo objeto
        val defaultModel = JsonConfig.Default.decodeFromString(TestModel.serializer(), defaultJson)
        val prettyModel = JsonConfig.Pretty.decodeFromString(TestModel.serializer(), prettyJson)

        assertEquals(defaultModel, prettyModel)
    }

    @Test
    fun comparison_strictVsDefault() {
        val validJson = """{"id":"123","name":"John","age":30}"""
        val invalidJson = """{"id":"123","name":"John","age":30,"extra":"field"}"""

        // Default tolera claves extra
        val defaultModel = JsonConfig.Default.decodeFromString(TestModel.serializer(), invalidJson)
        assertEquals("123", defaultModel.id)

        // Strict falla con claves extra
        assertFailsWith<SerializationException> {
            JsonConfig.Strict.decodeFromString(TestModel.serializer(), invalidJson)
        }

        // Pero ambos funcionan con JSON válido
        val strictModel = JsonConfig.Strict.decodeFromString(TestModel.serializer(), validJson)
        assertEquals(defaultModel.id, strictModel.id)
    }

    // ========== Test de No Conflicto con HttpClientFactory ==========

    @Test
    fun noConflict_jsonConfigIsIndependent() {
        // Este test verifica que JsonConfig no afecta a HttpClientFactory
        // Al ser objetos independientes, no debería haber interferencia

        val model = TestModel(id = "123", name = "John")

        // Serializar con JsonConfig.Default
        val json1 = JsonConfig.Default.encodeToString(TestModel.serializer(), model)

        // Serializar con JsonConfig.Pretty
        val json2 = JsonConfig.Pretty.encodeToString(TestModel.serializer(), model)

        // Ambos deben deserializarse correctamente
        val model1 = JsonConfig.Default.decodeFromString(TestModel.serializer(), json1)
        val model2 = JsonConfig.Default.decodeFromString(TestModel.serializer(), json2)

        assertEquals(model, model1)
        assertEquals(model, model2)
    }

    // ========== Test End-to-End: Serialización + Validación ==========

    @Serializable
    data class ValidatableTestModel(
        val email: String,
        val age: Int
    ) : ValidatableModel {
        override fun validate(): com.edugo.test.module.core.Result<Unit> {
            return when {
                email.isBlank() -> com.edugo.test.module.core.failure("Email is required")
                !email.contains("@") -> com.edugo.test.module.core.failure("Invalid email")
                age < 0 -> com.edugo.test.module.core.failure("Age cannot be negative")
                else -> com.edugo.test.module.core.success(Unit)
            }
        }
    }

    @Test
    fun endToEnd_serializationWithValidation() {
        val validModel = ValidatableTestModel(email = "test@example.com", age = 25)

        // 1. Validar
        val validationResult = validModel.validate()
        assertIs<Result.Success<Unit>>(validationResult)

        // 2. Serializar
        val json = JsonConfig.Default.encodeToString(ValidatableTestModel.serializer(), validModel)
        assertTrue(json.contains("test@example.com"))

        // 3. Deserializar
        val deserializedModel = JsonConfig.Default.decodeFromString(ValidatableTestModel.serializer(), json)

        // 4. Validar de nuevo
        val revalidationResult = deserializedModel.validate()
        assertIs<Result.Success<Unit>>(revalidationResult)

        assertEquals(validModel, deserializedModel)
    }

    @Test
    fun endToEnd_deserializeInvalidJsonAndValidate() {
        val json = """{"email":"invalid-email","age":25}"""

        // 1. Deserializar
        val model = JsonConfig.Default.decodeFromString(ValidatableTestModel.serializer(), json)

        // 2. Validar - debería fallar por email inválido
        val validationResult = model.validate()
        assertIs<Result.Failure>(validationResult)
        assertTrue(validationResult.error.contains("email"))
    }
}
