package com.edugo.test.module.extensions

import com.edugo.test.module.core.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Suite de tests para CollectionResultExtensions.
 *
 * Verifica:
 * - sequence() para List<Result<T>> → Result<List<T>>
 * - sequenceCollectingErrors() acumulando todos los errores
 * - traverse() aplicando transformaciones
 * - traverseCollectingErrors() con acumulación de errores
 * - partition() separando éxitos y fallos
 * - Helpers: collectSuccesses, collectFailures, allSuccess, anyFailure, etc.
 * - Performance O(n) y sin copias innecesarias
 */
class CollectionResultExtensionsTest {

    // ========== Tests de sequence() ==========

    @Test
    fun `sequence con todos Success retorna Success con lista`() {
        val results = listOf(
            Result.Success(1),
            Result.Success(2),
            Result.Success(3)
        )

        val result = results.sequence()

        assertIs<Result.Success<List<Int>>>(result)
        assertEquals(listOf(1, 2, 3), result.data)
    }

    @Test
    fun `sequence con un Failure retorna el primer Failure`() {
        val results = listOf(
            Result.Success(1),
            Result.Failure("Error 2"),
            Result.Success(3)
        )

        val result = results.sequence()

        assertIs<Result.Failure>(result)
        assertEquals("Error 2", result.error)
    }

    @Test
    fun `sequence con múltiples Failures retorna el primero`() {
        val results = listOf(
            Result.Failure("Error 1"),
            Result.Failure("Error 2"),
            Result.Failure("Error 3")
        )

        val result = results.sequence()

        assertIs<Result.Failure>(result)
        assertEquals("Error 1", result.error)
    }

    @Test
    fun `sequence con Loading retorna Loading si no hay Failure`() {
        val results = listOf(
            Result.Success(1),
            Result.Loading,
            Result.Success(3)
        )

        val result = results.sequence()

        assertIs<Result.Loading>(result)
    }

    @Test
    fun `sequence con lista vacía retorna Success con lista vacía`() {
        val results = emptyList<Result<Int>>()
        val result = results.sequence()

        assertIs<Result.Success<List<Int>>>(result)
        assertTrue(result.data.isEmpty())
    }

    // ========== Tests de sequenceCollectingErrors() ==========

    @Test
    fun `sequenceCollectingErrors acumula todos los errores`() {
        val results = listOf(
            Result.Failure("Error 1"),
            Result.Success(2),
            Result.Failure("Error 3"),
            Result.Success(4),
            Result.Failure("Error 5")
        )

        val result = results.sequenceCollectingErrors()

        assertIs<Result.Failure>(result)
        assertTrue(result.error.contains("Error 1"))
        assertTrue(result.error.contains("Error 3"))
        assertTrue(result.error.contains("Error 5"))
        assertEquals("Error 1; Error 3; Error 5", result.error)
    }

    @Test
    fun `sequenceCollectingErrors con separador custom`() {
        val results = listOf(
            Result.Failure("Error A"),
            Result.Failure("Error B")
        )

        val result = results.sequenceCollectingErrors(separator = " | ")

        assertIs<Result.Failure>(result)
        assertEquals("Error A | Error B", result.error)
    }

    @Test
    fun `sequenceCollectingErrors con todos Success retorna Success`() {
        val results = listOf(
            Result.Success(10),
            Result.Success(20),
            Result.Success(30)
        )

        val result = results.sequenceCollectingErrors()

        assertIs<Result.Success<List<Int>>>(result)
        assertEquals(listOf(10, 20, 30), result.data)
    }

    // ========== Tests de traverse() ==========

    @Test
    fun `traverse aplica transformación exitosamente`() {
        val numbers = listOf("1", "2", "3", "4", "5")

        val result = numbers.traverse { str ->
            val num = str.toIntOrNull()
            if (num != null) {
                Result.Success(num)
            } else {
                Result.Failure("Invalid number: $str")
            }
        }

        assertIs<Result.Success<List<Int>>>(result)
        assertEquals(listOf(1, 2, 3, 4, 5), result.data)
    }

    @Test
    fun `traverse falla en el primer error (fail-fast)`() {
        val strings = listOf("1", "2", "invalid", "4", "5")

        val result = strings.traverse { str ->
            val num = str.toIntOrNull()
            if (num != null) {
                Result.Success(num)
            } else {
                Result.Failure("Invalid number: $str")
            }
        }

        assertIs<Result.Failure>(result)
        assertEquals("Invalid number: invalid", result.error)
    }

    @Test
    fun `traverse con lista vacía retorna Success con lista vacía`() {
        val empty = emptyList<String>()
        val result = empty.traverse { Result.Success(it) }

        assertIs<Result.Success<List<String>>>(result)
        assertTrue(result.data.isEmpty())
    }

    @Test
    fun `traverse con Loading retorna Loading`() {
        val items = listOf(1, 2, 3)

        val result = items.traverse { num ->
            if (num == 2) Result.Loading else Result.Success(num * 2)
        }

        assertIs<Result.Loading>(result)
    }

    // ========== Tests de traverseCollectingErrors() ==========

    @Test
    fun `traverseCollectingErrors acumula todos los errores con índices`() {
        val strings = listOf("1", "invalid1", "3", "invalid2", "5")

        val result = strings.traverseCollectingErrors { str ->
            val num = str.toIntOrNull()
            if (num != null) {
                Result.Success(num)
            } else {
                Result.Failure("Invalid number: $str")
            }
        }

        assertIs<Result.Failure>(result)
        assertTrue(result.error.contains("Index 1"))
        assertTrue(result.error.contains("Index 3"))
    }

    @Test
    fun `traverseCollectingErrors sin includeIndex`() {
        val strings = listOf("invalid1", "invalid2")

        val result = strings.traverseCollectingErrors(includeIndex = false) { str ->
            Result.Failure("Error: $str")
        }

        assertIs<Result.Failure>(result)
        assertFalse(result.error.contains("Index"))
        assertEquals("Error: invalid1; Error: invalid2", result.error)
    }

    @Test
    fun `traverseCollectingErrors con todos Success retorna Success`() {
        val numbers = listOf(1, 2, 3, 4, 5)

        val result = numbers.traverseCollectingErrors { num ->
            Result.Success(num * 2)
        }

        assertIs<Result.Success<List<Int>>>(result)
        assertEquals(listOf(2, 4, 6, 8, 10), result.data)
    }

    // ========== Tests de partition() ==========

    @Test
    fun `partition separa Success y Failure correctamente`() {
        val results = listOf(
            Result.Success(1),
            Result.Failure("Error 2"),
            Result.Success(3),
            Result.Failure("Error 4"),
            Result.Success(5)
        )

        val (successes, failures) = results.partition()

        assertEquals(listOf(1, 3, 5), successes)
        assertEquals(listOf("Error 2", "Error 4"), failures)
    }

    @Test
    fun `partition con todos Success retorna solo successes`() {
        val results = listOf(
            Result.Success(10),
            Result.Success(20),
            Result.Success(30)
        )

        val (successes, failures) = results.partition()

        assertEquals(listOf(10, 20, 30), successes)
        assertTrue(failures.isEmpty())
    }

    @Test
    fun `partition con todos Failure retorna solo failures`() {
        val results = listOf(
            Result.Failure("Error 1"),
            Result.Failure("Error 2"),
            Result.Failure("Error 3")
        )

        val (successes, failures) = results.partition()

        assertTrue(successes.isEmpty())
        assertEquals(listOf("Error 1", "Error 2", "Error 3"), failures)
    }

    // ========== Tests de traversePartition() ==========

    @Test
    fun `traversePartition separa éxitos y fallos en transformación`() {
        val strings = listOf("1", "invalid", "3", "bad", "5")

        val (successes, failures) = strings.traversePartition { str ->
            val num = str.toIntOrNull()
            if (num != null) {
                Result.Success(num)
            } else {
                Result.Failure("Invalid: $str")
            }
        }

        assertEquals(listOf(1, 3, 5), successes)
        assertEquals(2, failures.size)
    }

    // ========== Tests de Helpers ==========

    @Test
    fun `collectSuccesses extrae solo valores exitosos`() {
        val results = listOf(
            Result.Success(100),
            Result.Failure("Error"),
            Result.Success(200),
            Result.Loading,
            Result.Success(300)
        )

        val successes = results.collectSuccesses()

        assertEquals(listOf(100, 200, 300), successes)
    }

    @Test
    fun `collectFailures extrae solo mensajes de error`() {
        val results = listOf(
            Result.Success(1),
            Result.Failure("Error A"),
            Result.Failure("Error B"),
            Result.Success(2)
        )

        val failures = results.collectFailures()

        assertEquals(listOf("Error A", "Error B"), failures)
    }

    @Test
    fun `allSuccess retorna true solo si todos son Success`() {
        val allSuccessResults = listOf(
            Result.Success(1),
            Result.Success(2),
            Result.Success(3)
        )

        assertTrue(allSuccessResults.allSuccess())

        val mixedResults = listOf(
            Result.Success(1),
            Result.Failure("Error"),
            Result.Success(3)
        )

        assertFalse(mixedResults.allSuccess())
    }

    @Test
    fun `anyFailure retorna true si hay al menos un Failure`() {
        val withFailure = listOf(
            Result.Success(1),
            Result.Failure("Error"),
            Result.Success(3)
        )

        assertTrue(withFailure.anyFailure())

        val noFailure = listOf(
            Result.Success(1),
            Result.Success(2),
            Result.Loading
        )

        assertFalse(noFailure.anyFailure())
    }

    @Test
    fun `countSuccesses cuenta correctamente`() {
        val results = listOf(
            Result.Success(1),
            Result.Failure("Error"),
            Result.Success(2),
            Result.Loading,
            Result.Success(3)
        )

        assertEquals(3, results.countSuccesses())
    }

    @Test
    fun `countFailures cuenta correctamente`() {
        val results = listOf(
            Result.Failure("Error 1"),
            Result.Success(2),
            Result.Failure("Error 2"),
            Result.Loading,
            Result.Failure("Error 3")
        )

        assertEquals(3, results.countFailures())
    }

    // ========== Tests de Casos de Uso Reales ==========

    @Test
    fun `validación de múltiples emails con sequence`() {
        fun validateEmail(email: String): Result<String> {
            return if (email.contains("@")) {
                Result.Success(email)
            } else {
                Result.Failure("Invalid email: $email")
            }
        }

        val emails = listOf("john@test.com", "jane@test.com", "bob@test.com")
        val validationResults = emails.map { validateEmail(it) }
        val result = validationResults.sequence()

        assertIs<Result.Success<List<String>>>(result)
        assertEquals(emails, result.data)
    }

    @Test
    fun `conversión DTO con traverse`() {
        data class UserDto(val name: String, val age: Int)
        data class User(val name: String, val age: Int)

        fun convertDto(dto: UserDto): Result<User> {
            return if (dto.age >= 18) {
                Result.Success(User(dto.name, dto.age))
            } else {
                Result.Failure("User must be 18 or older")
            }
        }

        val dtos = listOf(
            UserDto("John", 25),
            UserDto("Jane", 30),
            UserDto("Bob", 22)
        )

        val result = dtos.traverse { convertDto(it) }

        assertIs<Result.Success<List<User>>>(result)
        assertEquals(3, result.data.size)
    }

    @Test
    fun `procesamiento parcial con partition`() {
        data class Record(val id: Int, val valid: Boolean)

        fun processRecord(record: Record): Result<Int> {
            return if (record.valid) {
                Result.Success(record.id)
            } else {
                Result.Failure("Invalid record: ${record.id}")
            }
        }

        val records = listOf(
            Record(1, true),
            Record(2, false),
            Record(3, true),
            Record(4, false),
            Record(5, true)
        )

        val results = records.map { processRecord(it) }
        val (processed, failed) = results.partition()

        assertEquals(listOf(1, 3, 5), processed)
        assertEquals(2, failed.size)
    }

    // ========== Tests de Performance ==========

    @Test
    fun `traverse es O(n) para lista grande`() {
        val largeList = (1..10000).toList()

        val result = largeList.traverse { num ->
            Result.Success(num * 2)
        }

        assertIs<Result.Success<List<Int>>>(result)
        assertEquals(10000, result.data.size)
        assertEquals(2, result.data.first())
        assertEquals(20000, result.data.last())
    }

    @Test
    fun `sequence no hace copias innecesarias`() {
        data class LargeObject(val data: ByteArray) {
            override fun equals(other: Any?) = this === other
            override fun hashCode() = data.contentHashCode()
        }

        val objects = List(100) { LargeObject(ByteArray(1024)) }
        val results = objects.map { Result.Success(it) }

        val sequenced = results.sequence()

        assertIs<Result.Success<List<LargeObject>>>(sequenced)
        // Verificar que son los mismos objetos (no copias)
        objects.forEachIndexed { index, obj ->
            assertTrue(obj === sequenced.data[index])
        }
    }

    @Test
    fun `filterTraverse filtra y transforma eficientemente`() {
        val mixed = listOf("1", "invalid", "3", "4", "bad", "6")

        val result = mixed.filterTraverse { str ->
            val num = str.toIntOrNull()
            if (num != null) {
                Result.Success(num)
            } else {
                null // Filtrar
            }
        }

        assertIs<Result.Success<List<Int>>>(result)
        assertEquals(listOf(1, 3, 4, 6), result.data)
    }
}
