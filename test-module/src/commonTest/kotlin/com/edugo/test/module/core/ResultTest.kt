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
}
