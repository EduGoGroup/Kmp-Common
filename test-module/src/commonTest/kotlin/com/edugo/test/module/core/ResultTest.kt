package com.edugo.test.module.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertIs

class ResultTest {

    @Test
    fun success_containsData() {
        val result: Result<String> = Result.Success("test data")
        assertIs<Result.Success<String>>(result)
        assertEquals("test data", result.data)
    }

    @Test
    fun failure_containsErrorMessage() {
        val result: Result<String> = Result.Failure("test error")
        assertIs<Result.Failure>(result)
        assertEquals("test error", result.error)
        assertEquals("test error", result.getSafeMessage())
    }

    @Test
    fun map_transformsSuccessData() {
        val result: Result<Int> = Result.Success(5)
        val mapped = result.map { it * 2 }
        assertIs<Result.Success<Int>>(mapped)
        assertEquals(10, mapped.data)
    }

    @Test
    fun map_preservesFailure() {
        val result: Result<Int> = Result.Failure("error")
        val mapped = result.map { it * 2 }
        assertIs<Result.Failure>(mapped)
        assertEquals("error", mapped.error)
    }

    @Test
    fun success_withNullableData() {
        val result: Result<String?> = Result.Success(null)
        assertIs<Result.Success<String?>>(result)
        assertEquals(null, result.data)
    }

    @Test
    fun map_withNullableData() {
        val result: Result<String?> = Result.Success("test")
        val mapped = result.map { it?.uppercase() }
        assertIs<Result.Success<String?>>(mapped)
        assertEquals("TEST", mapped.data)
    }

    @Test
    fun map_chainingMultipleOperations() {
        val result: Result<Int> = Result.Success(5)
        val mapped = result.map { it * 2 }.map { it + 1 }
        assertIs<Result.Success<Int>>(mapped)
        assertEquals(11, mapped.data)
    }

    @Test
    fun catching_returnsSuccessWhenNoException() {
        val result = catching {
            Result.Success("success")
        }
        assertIs<Result.Success<String>>(result)
        assertEquals("success", result.data)
    }

    @Test
    fun catching_returnsFailureWhenExceptionThrown() {
        val result = catching<String> {
            throw RuntimeException("test exception")
        }
        assertIs<Result.Failure>(result)
        assertEquals("test exception", result.error)
    }

    @Test
    fun catching_usesGenericMessageWhenExceptionHasNoMessage() {
        val result = catching<String> {
            throw RuntimeException()
        }
        assertIs<Result.Failure>(result)
        assertEquals("An error occurred", result.error)
    }

    @Test
    fun catching_worksWithDifferentResultTypes() {
        val result = catching {
            val value = 10
            Result.Success(value * 2)
        }
        assertIs<Result.Success<Int>>(result)
        assertEquals(20, result.data)
    }

    // flatMap tests
    @Test
    fun flatMap_chainsSuccessResults() {
        val result: Result<Int> = Result.Success(5)
        val flatMapped = result.flatMap { value ->
            Result.Success(value * 2)
        }
        assertIs<Result.Success<Int>>(flatMapped)
        assertEquals(10, flatMapped.data)
    }

    @Test
    fun flatMap_propagatesFailure() {
        val result: Result<Int> = Result.Failure("initial error")
        val flatMapped = result.flatMap { value ->
            Result.Success(value * 2)
        }
        assertIs<Result.Failure>(flatMapped)
        assertEquals("initial error", flatMapped.error)
    }

    @Test
    fun flatMap_canReturnFailure() {
        val result: Result<Int> = Result.Success(5)
        val flatMapped = result.flatMap<Int, Int> {
            Result.Failure("operation failed")
        }
        assertIs<Result.Failure>(flatMapped)
        assertEquals("operation failed", flatMapped.error)
    }

    @Test
    fun flatMap_preservesLoading() {
        val result: Result<Int> = Result.Loading
        val flatMapped = result.flatMap { value ->
            Result.Success(value * 2)
        }
        assertIs<Result.Loading>(flatMapped)
    }

    @Test
    fun flatMap_allowsTypeTransformation() {
        val result: Result<Int> = Result.Success(42)
        val flatMapped = result.flatMap { value ->
            Result.Success("Number: $value")
        }
        assertIs<Result.Success<String>>(flatMapped)
        assertEquals("Number: 42", flatMapped.data)
    }

    // mapError tests
    @Test
    fun mapError_transformsFailureMessage() {
        val result: Result<String> = Result.Failure("original error")
        val mapped = result.mapError { error -> "Transformed: $error" }
        assertIs<Result.Failure>(mapped)
        assertEquals("Transformed: original error", mapped.error)
    }

    @Test
    fun mapError_preservesSuccess() {
        val result: Result<String> = Result.Success("data")
        val mapped = result.mapError { error -> "Transformed: $error" }
        assertIs<Result.Success<String>>(mapped)
        assertEquals("data", mapped.data)
    }

    @Test
    fun mapError_preservesLoading() {
        val result: Result<String> = Result.Loading
        val mapped = result.mapError { error -> "Transformed: $error" }
        assertIs<Result.Loading>(mapped)
    }

    @Test
    fun mapError_canAddContext() {
        val result: Result<Int> = Result.Failure("network timeout")
        val mapped = result.mapError { error -> "Failed to fetch user: $error" }
        assertIs<Result.Failure>(mapped)
        assertEquals("Failed to fetch user: network timeout", mapped.error)
    }

    // fold tests
    @Test
    fun fold_executesOnSuccessForSuccess() {
        val result: Result<Int> = Result.Success(10)
        val folded = result.fold(
            onSuccess = { value -> "Success: $value" },
            onFailure = { error -> "Error: $error" }
        )
        assertEquals("Success: 10", folded)
    }

    @Test
    fun fold_executesOnFailureForFailure() {
        val result: Result<Int> = Result.Failure("test error")
        val folded = result.fold(
            onSuccess = { value -> "Success: $value" },
            onFailure = { error -> "Error: $error" }
        )
        assertEquals("Error: test error", folded)
    }

    @Test
    fun fold_returnsNullForLoading() {
        val result: Result<Int> = Result.Loading
        val folded = result.fold(
            onSuccess = { value -> "Success: $value" },
            onFailure = { error -> "Error: $error" }
        )
        assertEquals(null, folded)
    }

    @Test
    fun fold_canTransformToAnyType() {
        val result: Result<String> = Result.Success("hello")
        val folded = result.fold(
            onSuccess = { value -> value.length },
            onFailure = { _ -> 0 }
        )
        assertEquals(5, folded)
    }

    // getOrElse tests
    @Test
    fun getOrElse_returnsDataForSuccess() {
        val result: Result<String> = Result.Success("data")
        val value = result.getOrElse { "default" }
        assertEquals("data", value)
    }

    @Test
    fun getOrElse_returnsDefaultForFailure() {
        val result: Result<String> = Result.Failure("error")
        val value = result.getOrElse { "default" }
        assertEquals("default", value)
    }

    @Test
    fun getOrElse_returnsDefaultForLoading() {
        val result: Result<String> = Result.Loading
        val value = result.getOrElse { "default" }
        assertEquals("default", value)
    }

    @Test
    fun getOrElse_lazilyEvaluatesDefault() {
        val result: Result<Int> = Result.Success(42)
        var defaultCalled = false
        val value = result.getOrElse {
            defaultCalled = true
            0
        }
        assertEquals(42, value)
        assertEquals(false, defaultCalled)
    }

    // getOrNull tests
    @Test
    fun getOrNull_returnsDataForSuccess() {
        val result: Result<String> = Result.Success("data")
        val value = result.getOrNull()
        assertEquals("data", value)
    }

    @Test
    fun getOrNull_returnsNullForFailure() {
        val result: Result<String> = Result.Failure("error")
        val value = result.getOrNull()
        assertEquals(null, value)
    }

    @Test
    fun getOrNull_returnsNullForLoading() {
        val result: Result<String> = Result.Loading
        val value = result.getOrNull()
        assertEquals(null, value)
    }

    @Test
    fun getOrNull_worksWithNullableTypes() {
        val result: Result<String?> = Result.Success(null)
        val value = result.getOrNull()
        assertEquals(null, value)
    }

    // Integration tests combining multiple operations
    @Test
    fun integration_mapAndFlatMapChaining() {
        val result: Result<Int> = Result.Success(5)
        val final = result
            .map { it * 2 }
            .flatMap { value -> Result.Success("Result: $value") }
        assertIs<Result.Success<String>>(final)
        assertEquals("Result: 10", final.data)
    }

    @Test
    fun integration_errorTransformationAndFold() {
        val result: Result<String> = Result.Failure("network error")
        val message = result
            .mapError { error -> "Failed to load: $error" }
            .fold(
                onSuccess = { "Data: $it" },
                onFailure = { it }
            )
        assertEquals("Failed to load: network error", message)
    }

    @Test
    fun integration_complexChainWithFallback() {
        val result: Result<Int> = Result.Success(10)
        val value = result
            .flatMap { v -> Result.Success(v * 2) }
            .map { it + 5 }
            .getOrElse { 0 }
        assertEquals(25, value)
    }

    // Factory functions tests
    @Test
    fun success_factoryCreatesSuccessResult() {
        val result = success("test value")
        assertIs<Result.Success<String>>(result)
        assertEquals("test value", result.data)
    }

    @Test
    fun success_factoryWorksWithDifferentTypes() {
        val intResult = success(42)
        val listResult = success(listOf(1, 2, 3))
        assertIs<Result.Success<Int>>(intResult)
        assertIs<Result.Success<List<Int>>>(listResult)
        assertEquals(42, intResult.data)
        assertEquals(listOf(1, 2, 3), listResult.data)
    }

    @Test
    fun failure_factoryCreatesFailureResult() {
        val result: Result<String> = failure("test error")
        assertIs<Result.Failure>(result)
        assertEquals("test error", result.error)
    }

    @Test
    fun failure_factoryCanBeTyped() {
        val result: Result<Int> = failure("invalid number")
        assertIs<Result.Failure>(result)
        assertEquals("invalid number", result.error)
    }

    // zip tests
    @Test
    fun zip_combinesTwoSuccessResults() {
        val result1: Result<Int> = success(5)
        val result2: Result<Int> = success(10)
        val zipped = result1.zip(result2) { a, b -> a + b }
        assertIs<Result.Success<Int>>(zipped)
        assertEquals(15, zipped.data)
    }

    @Test
    fun zip_returnsFirstFailure() {
        val result1: Result<Int> = failure("error 1")
        val result2: Result<Int> = failure("error 2")
        val zipped = result1.zip(result2) { a, b -> a + b }
        assertIs<Result.Failure>(zipped)
        assertEquals("error 1", zipped.error)
    }

    @Test
    fun zip_returnsFailureWhenFirstIsFailure() {
        val result1: Result<Int> = failure("error")
        val result2: Result<Int> = success(10)
        val zipped = result1.zip(result2) { a, b -> a + b }
        assertIs<Result.Failure>(zipped)
        assertEquals("error", zipped.error)
    }

    @Test
    fun zip_returnsFailureWhenSecondIsFailure() {
        val result1: Result<Int> = success(5)
        val result2: Result<Int> = failure("error")
        val zipped = result1.zip(result2) { a, b -> a + b }
        assertIs<Result.Failure>(zipped)
        assertEquals("error", zipped.error)
    }

    @Test
    fun zip_returnsLoadingWhenEitherIsLoading() {
        val result1: Result<Int> = Result.Loading
        val result2: Result<Int> = success(10)
        val zipped = result1.zip(result2) { a, b -> a + b }
        assertIs<Result.Loading>(zipped)
    }

    @Test
    fun zip_allowsTypeTransformation() {
        val result1: Result<Int> = success(42)
        val result2: Result<String> = success("items")
        val zipped = result1.zip(result2) { count, label -> "$count $label" }
        assertIs<Result.Success<String>>(zipped)
        assertEquals("42 items", zipped.data)
    }

    @Test
    fun zip_prioritizesLoadingOverSuccess() {
        val result1: Result<Int> = success(5)
        val result2: Result<Int> = Result.Loading
        val zipped = result1.zip(result2) { a, b -> a + b }
        assertIs<Result.Loading>(zipped)
    }

    // combine tests
    @Test
    fun combine_returnsSuccessWithAllValues() {
        val result1 = success(1)
        val result2 = success(2)
        val result3 = success(3)
        val combined = combine(result1, result2, result3)
        assertIs<Result.Success<List<Int>>>(combined)
        assertEquals(listOf(1, 2, 3), combined.data)
    }

    @Test
    fun combine_returnsFirstFailure() {
        val result1 = success(1)
        val result2: Result<Int> = failure("error 2")
        val result3: Result<Int> = failure("error 3")
        val combined = combine(result1, result2, result3)
        assertIs<Result.Failure>(combined)
        assertEquals("error 2", combined.error)
    }

    @Test
    fun combine_returnsLoadingWhenAnyIsLoading() {
        val result1 = success(1)
        val result2: Result<Int> = Result.Loading
        val result3 = success(3)
        val combined = combine(result1, result2, result3)
        assertIs<Result.Loading>(combined)
    }

    @Test
    fun combine_worksWithSingleResult() {
        val result = success(42)
        val combined = combine(result)
        assertIs<Result.Success<List<Int>>>(combined)
        assertEquals(listOf(42), combined.data)
    }

    @Test
    fun combine_worksWithEmptyArray() {
        val combined = combine<Int>()
        assertIs<Result.Success<List<Int>>>(combined)
        assertEquals(emptyList(), combined.data)
    }

    @Test
    fun combine_prioritizesFailureOverLoading() {
        val result1 = success(1)
        val result2: Result<Int> = failure("error")
        val result3: Result<Int> = Result.Loading
        val combined = combine(result1, result2, result3)
        assertIs<Result.Failure>(combined)
        assertEquals("error", combined.error)
    }

    @Test
    fun combine_worksWithDifferentSuccessValues() {
        val results = listOf(success(10), success(20), success(30), success(40))
        val combined = combine(*results.toTypedArray())
        assertIs<Result.Success<List<Int>>>(combined)
        assertEquals(listOf(10, 20, 30, 40), combined.data)
    }

    // Integration tests with factory functions and combinators
    @Test
    fun integration_factoryFunctionsWithZip() {
        val result1 = success(5)
        val result2 = success(10)
        val zipped = result1.zip(result2) { a, b -> a * b }
        assertEquals(50, zipped.getOrElse { 0 })
    }

    @Test
    fun integration_combineWithMapAndFold() {
        val results = listOf(success(1), success(2), success(3))
        val sum = combine(*results.toTypedArray())
            .map { list -> list.sum() }
            .fold(
                onSuccess = { it },
                onFailure = { 0 }
            )
        assertEquals(6, sum)
    }

    @Test
    fun integration_validationWithFactoryFunctions() {
        fun validatePositive(value: Int): Result<Int> =
            if (value > 0) success(value) else failure("Value must be positive")

        val valid = validatePositive(10)
        val invalid = validatePositive(-5)

        assertIs<Result.Success<Int>>(valid)
        assertIs<Result.Failure>(invalid)
        assertEquals(10, valid.data)
        assertEquals("Value must be positive", invalid.error)
    }

    // Loading state comprehensive tests
    @Test
    fun loading_preservedThroughMap() {
        val result: Result<Int> = Result.Loading
        val mapped = result.map { it * 2 }
        assertIs<Result.Loading>(mapped)
    }

    @Test
    fun loading_preservedThroughFlatMap() {
        val result: Result<Int> = Result.Loading
        val flatMapped = result.flatMap { value -> success(value * 2) }
        assertIs<Result.Loading>(flatMapped)
    }

    @Test
    fun loading_preservedThroughMapError() {
        val result: Result<Int> = Result.Loading
        val mapped = result.mapError { "Error: $it" }
        assertIs<Result.Loading>(mapped)
    }

    @Test
    fun loading_returnsNullInFold() {
        val result: Result<Int> = Result.Loading
        val folded = result.fold(
            onSuccess = { it },
            onFailure = { 0 }
        )
        assertEquals(null, folded)
    }

    @Test
    fun loading_returnsDefaultInGetOrElse() {
        val result: Result<String> = Result.Loading
        val value = result.getOrElse { "loading default" }
        assertEquals("loading default", value)
    }

    @Test
    fun loading_returnsNullInGetOrNull() {
        val result: Result<String> = Result.Loading
        val value = result.getOrNull()
        assertEquals(null, value)
    }

    // Nullable types edge cases
    @Test
    fun nullable_successWithNull() {
        val result: Result<String?> = success(null)
        assertIs<Result.Success<String?>>(result)
        assertEquals(null, result.data)
    }

    @Test
    fun nullable_mapPreservesNull() {
        val result: Result<String?> = success(null)
        val mapped = result.map { it?.length }
        assertIs<Result.Success<Int?>>(mapped)
        assertEquals(null, mapped.data)
    }

    @Test
    fun nullable_flatMapWithNull() {
        val result: Result<String?> = success(null)
        val flatMapped = result.flatMap { value ->
            if (value != null) success(value.length)
            else failure("Value is null")
        }
        assertIs<Result.Failure>(flatMapped)
        assertEquals("Value is null", flatMapped.error)
    }

    @Test
    fun nullable_getOrNullDistinguishesSuccessNullFromFailure() {
        val successNull: Result<String?> = success(null)
        val failure: Result<String?> = failure("error")

        assertEquals(null, successNull.getOrNull())
        assertEquals(null, failure.getOrNull())
    }

    @Test
    fun nullable_foldHandlesNullableSuccess() {
        val result: Result<Int?> = success(null)
        val folded = result.fold(
            onSuccess = { it ?: -1 },
            onFailure = { -2 }
        )
        assertEquals(-1, folded)
    }

    // Complex chaining scenarios
    @Test
    fun chaining_mapFlatMapMapGetOrElse() {
        val result = success(10)
            .map { it * 2 }
            .flatMap { value -> success(value + 5) }
            .map { it.toString() }
            .getOrElse { "error" }
        assertEquals("25", result)
    }

    @Test
    fun chaining_errorPropagationThroughChain() {
        val result: Result<String> = failure<Int>("initial error")
            .map { it * 2 }
            .flatMap { value -> success(value + 5) }
            .map { it.toString() }
            .mapError { "Wrapped: $it" }

        assertIs<Result.Failure>(result)
        assertEquals("Wrapped: initial error", result.error)
    }

    @Test
    fun chaining_loadingPropagationThroughChain() {
        val result: Result<String> = Result.Loading
        val chained = result
            .map { it.length }
            .flatMap { value -> success(value * 2) }
            .map { it.toString() }

        assertIs<Result.Loading>(chained)
    }

    @Test
    fun chaining_multipleTransformations() {
        val result = success(5)
            .map { it * 2 }        // 10
            .map { it + 3 }        // 13
            .map { it - 1 }        // 12
            .flatMap { value -> success(value / 2) }  // 6
            .map { it.toString() }  // "6"

        assertIs<Result.Success<String>>(result)
        assertEquals("6", result.data)
    }

    @Test
    fun chaining_shortCircuitsOnFirstError() {
        var mapCalled = 0
        var flatMapCalled = 0

        val result = success(10)
            .map {
                mapCalled++
                it * 2
            }
            .flatMap<Int, Int> {
                flatMapCalled++
                failure("error in flatMap")
            }
            .map {
                mapCalled++  // Should NOT be called
                it + 1
            }

        assertIs<Result.Failure>(result)
        assertEquals(1, mapCalled)
        assertEquals(1, flatMapCalled)
    }

    // Catching with different exception types
    @Test
    fun catching_handlesIllegalArgumentException() {
        val result = catching<String> {
            throw IllegalArgumentException("Invalid argument")
        }
        assertIs<Result.Failure>(result)
        assertEquals("Invalid argument", result.error)
    }

    @Test
    fun catching_handlesNullPointerException() {
        val result = catching<String> {
            throw NullPointerException("Null value")
        }
        assertIs<Result.Failure>(result)
        assertEquals("Null value", result.error)
    }

    @Test
    fun catching_handlesCustomException() {
        class CustomException(message: String) : Exception(message)

        val result = catching<String> {
            throw CustomException("Custom error")
        }
        assertIs<Result.Failure>(result)
        assertEquals("Custom error", result.error)
    }

    @Test
    fun catching_preservesSuccessWhenNoException() {
        val result = catching {
            val value = "computed value"
            success(value)
        }
        assertIs<Result.Success<String>>(result)
        assertEquals("computed value", result.data)
    }

    // Zip edge cases
    @Test
    fun zip_withNullableTypes() {
        val result1: Result<Int?> = success(null)
        val result2: Result<String?> = success("test")
        val zipped = result1.zip(result2) { a, b ->
            "a=$a, b=$b"
        }
        assertIs<Result.Success<String>>(zipped)
        assertEquals("a=null, b=test", zipped.data)
    }

    @Test
    fun zip_bothLoading() {
        val result1: Result<Int> = Result.Loading
        val result2: Result<Int> = Result.Loading
        val zipped = result1.zip(result2) { a, b -> a + b }
        assertIs<Result.Loading>(zipped)
    }

    @Test
    fun zip_loadingTakesPrecedenceOverFailure() {
        val result1: Result<Int> = Result.Loading
        val result2: Result<Int> = failure("error")
        val zipped = result1.zip(result2) { a, b -> a + b }
        assertIs<Result.Loading>(zipped)
    }

    // Combine edge cases
    @Test
    fun combine_withNullableValues() {
        val results = listOf(
            success<String?>(null),
            success<String?>("test"),
            success<String?>(null)
        )
        val combined = combine(*results.toTypedArray())
        assertIs<Result.Success<List<String?>>>(combined)
        assertEquals(listOf(null, "test", null), combined.data)
    }

    @Test
    fun combine_allLoading() {
        val results = listOf<Result<Int>>(
            Result.Loading,
            Result.Loading,
            Result.Loading
        )
        val combined = combine(*results.toTypedArray())
        assertIs<Result.Loading>(combined)
    }

    @Test
    fun combine_mixedSuccessAndLoading() {
        val results = listOf(
            success(1),
            Result.Loading,
            success(3)
        )
        val combined = combine(*results.toTypedArray())
        assertIs<Result.Loading>(combined)
    }

    @Test
    fun combine_largeNumberOfResults() {
        val results = (1..100).map { success(it) }
        val combined = combine(*results.toTypedArray())
        assertIs<Result.Success<List<Int>>>(combined)
        assertEquals((1..100).toList(), combined.data)
    }

    // getSafeMessage tests
    @Test
    fun getSafeMessage_returnsErrorString() {
        val result = Result.Failure("specific error message")
        assertEquals("specific error message", result.getSafeMessage())
    }

    @Test
    fun getSafeMessage_handlesEmptyString() {
        val result = Result.Failure("")
        assertEquals("", result.getSafeMessage())
    }

    // Complex integration scenarios
    @Test
    fun integration_zipWithCombineAndFold() {
        val result1 = success(10)
        val result2 = success(20)

        val zipped = result1.zip(result2) { a, b -> listOf(a, b) }
        val results = listOf(success(30), success(40))
        val combined = combine(*results.toTypedArray())

        val final = zipped.zip(combined) { list1, list2 ->
            list1 + list2
        }.fold(
            onSuccess = { it.sum() },
            onFailure = { 0 }
        )

        assertEquals(100, final) // 10 + 20 + 30 + 40
    }

    @Test
    fun integration_errorRecoveryPattern() {
        val primaryResult: Result<String> = failure("primary failed")
        val fallbackResult: Result<String> = success("fallback value")

        val recovered = primaryResult.getOrNull() ?: fallbackResult.getOrElse { "default" }
        assertEquals("fallback value", recovered)
    }

    @Test
    fun integration_validationPipeline() {
        fun validateNotEmpty(s: String): Result<String> =
            if (s.isNotEmpty()) success(s) else failure("String is empty")

        fun validateMinLength(s: String, min: Int): Result<String> =
            if (s.length >= min) success(s) else failure("String too short")

        fun validateMaxLength(s: String, max: Int): Result<String> =
            if (s.length <= max) success(s) else failure("String too long")

        val input = "test"
        val result = validateNotEmpty(input)
            .flatMap { validateMinLength(it, 3) }
            .flatMap { validateMaxLength(it, 10) }

        assertIs<Result.Success<String>>(result)
        assertEquals("test", result.data)
    }

    @Test
    fun integration_parallelOperationsWithCombine() {
        // Simula operaciones paralelas que retornan Results
        val op1 = success("Operation 1")
        val op2 = success("Operation 2")
        val op3 = success("Operation 3")

        val combined = combine(op1, op2, op3)
            .map { list -> list.joinToString(", ") }

        assertIs<Result.Success<String>>(combined)
        assertEquals("Operation 1, Operation 2, Operation 3", combined.data)
    }

    @Test
    fun integration_nestedResultHandling() {
        val outerResult: Result<Result<Int>> = success(success(42))

        val flattened = outerResult.flatMap { it }
        assertIs<Result.Success<Int>>(flattened)
        assertEquals(42, flattened.data)
    }

    // ========================================================================
    // TESTS: recover
    // ========================================================================

    @Test
    fun recover_returnsOriginalValueOnSuccess() {
        val result: Result<String> = success("original value")
        val recovered = result.recover { "fallback" }

        assertIs<Result.Success<String>>(recovered)
        assertEquals("original value", recovered.data)
    }

    @Test
    fun recover_recoversFromFailure() {
        val result: Result<String> = failure("error occurred")
        val recovered = result.recover { error ->
            "recovered from: $error"
        }

        assertIs<Result.Success<String>>(recovered)
        assertEquals("recovered from: error occurred", recovered.data)
    }

    @Test
    fun recover_preservesLoading() {
        val result: Result<String> = Result.Loading
        val recovered = result.recover { "fallback" }

        assertIs<Result.Loading>(recovered)
    }

    @Test
    fun recover_canProvideDefaultValue() {
        val result: Result<Int> = failure("parsing failed")
        val recovered = result.recover { 0 }

        assertIs<Result.Success<Int>>(recovered)
        assertEquals(0, recovered.data)
    }

    // ========================================================================
    // TESTS: recoverWith
    // ========================================================================

    @Test
    fun recoverWith_returnsOriginalValueOnSuccess() {
        val result: Result<String> = success("original")
        val recovered = result.recoverWith { success("fallback") }

        assertIs<Result.Success<String>>(recovered)
        assertEquals("original", recovered.data)
    }

    @Test
    fun recoverWith_canRecoverSuccessfully() {
        val result: Result<String> = failure("primary failed")
        val recovered = result.recoverWith { error ->
            success("recovered: $error")
        }

        assertIs<Result.Success<String>>(recovered)
        assertEquals("recovered: primary failed", recovered.data)
    }

    @Test
    fun recoverWith_canAlsoFail() {
        val result: Result<String> = failure("primary failed")
        val recovered = result.recoverWith { error ->
            failure("recovery also failed: $error")
        }

        assertIs<Result.Failure>(recovered)
        assertEquals("recovery also failed: primary failed", recovered.error)
    }

    @Test
    fun recoverWith_preservesLoading() {
        val result: Result<String> = Result.Loading
        val recovered = result.recoverWith { success("fallback") }

        assertIs<Result.Loading>(recovered)
    }

    // ========================================================================
    // TESTS: T?.toResult
    // ========================================================================

    @Test
    fun toResult_convertsNonNullToSuccess() {
        val value: String? = "test value"
        val result = value.toResult("Value is null")

        assertIs<Result.Success<String>>(result)
        assertEquals("test value", result.data)
    }

    @Test
    fun toResult_convertsNullToFailure() {
        val value: String? = null
        val result = value.toResult("Value is null")

        assertIs<Result.Failure>(result)
        assertEquals("Value is null", result.error)
    }

    @Test
    fun toResult_worksWithDifferentTypes() {
        val intValue: Int? = 42
        val result = intValue.toResult("No value")

        assertIs<Result.Success<Int>>(result)
        assertEquals(42, result.data)
    }

    @Test
    fun toResult_usesCustomErrorMessage() {
        val value: String? = null
        val result = value.toResult("Custom error message")

        assertIs<Result.Failure>(result)
        assertEquals("Custom error message", result.error)
    }

    // ========================================================================
    // TESTS: T?.toResultOrElse
    // ========================================================================

    @Test
    fun toResultOrElse_convertsNonNullToSuccess() {
        val value: String? = "test"
        val result = value.toResultOrElse { "Error message" }

        assertIs<Result.Success<String>>(result)
        assertEquals("test", result.data)
    }

    @Test
    fun toResultOrElse_lazilyEvaluatesError() {
        val value: String? = "test"
        var errorCalled = false

        val result = value.toResultOrElse {
            errorCalled = true
            "Error"
        }

        assertIs<Result.Success<String>>(result)
        assertEquals(false, errorCalled)
    }

    @Test
    fun toResultOrElse_evaluatesErrorOnNull() {
        val value: String? = null
        var errorCalled = false

        val result = value.toResultOrElse {
            errorCalled = true
            "Value was null"
        }

        assertIs<Result.Failure>(result)
        assertEquals(true, errorCalled)
        assertEquals("Value was null", result.error)
    }

    // ========================================================================
    // TESTS: flatten
    // ========================================================================

    @Test
    fun flatten_flattensNestedSuccess() {
        val nested: Result<Result<Int>> = success(success(42))
        val flattened = nested.flatten()

        assertIs<Result.Success<Int>>(flattened)
        assertEquals(42, flattened.data)
    }

    @Test
    fun flatten_preservesOuterFailure() {
        val nested: Result<Result<Int>> = failure("outer error")
        val flattened = nested.flatten()

        assertIs<Result.Failure>(flattened)
        assertEquals("outer error", flattened.error)
    }

    @Test
    fun flatten_flattensInnerFailure() {
        val nested: Result<Result<Int>> = success(failure("inner error"))
        val flattened = nested.flatten()

        assertIs<Result.Failure>(flattened)
        assertEquals("inner error", flattened.error)
    }

    @Test
    fun flatten_preservesLoading() {
        val nested: Result<Result<Int>> = Result.Loading
        val flattened = nested.flatten()

        assertIs<Result.Loading>(flattened)
    }

    // ========================================================================
    // TESTS: Integration with new extensions
    // ========================================================================

    @Test
    fun integration_recoverWithFallbackChain() {
        val primary: Result<String> = failure("primary failed")
        val secondary: Result<String> = failure("secondary failed")

        val result = primary
            .recoverWith { secondary }
            .recover { "final fallback" }

        assertIs<Result.Success<String>>(result)
        assertEquals("final fallback", result.data)
    }

    @Test
    fun integration_nullableToResultChain() {
        val user: String? = null
        val result = user.toResult("User not found")
            .recover { "Guest User" }

        assertIs<Result.Success<String>>(result)
        assertEquals("Guest User", result.data)
    }

    @Test
    fun integration_flattenWithRecover() {
        val nested: Result<Result<Int>> = success(failure("inner error"))
        val result = nested.flatten()
            .recover { 0 }

        assertIs<Result.Success<Int>>(result)
        assertEquals(0, result.data)
    }
}
