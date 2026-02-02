/*
 * Copyright (c) 2026 EduGo Project
 * Licensed under the MIT License
 */

/**
 * End-to-end integration tests for Result, error handling, and domain mapping patterns.
 *
 * This package contains comprehensive integration tests that verify the complete workflow
 * of data conversion, validation, error handling, and domain mapping across the application.
 *
 * ## Purpose
 *
 * Integration tests in this package ensure that:
 * - Multiple components work together correctly (Result + AppError + Mappers + Validators)
 * - Real-world scenarios are handled properly (JSON → DTO → Domain → Persistence)
 * - Edge cases and error conditions are gracefully managed
 * - Performance characteristics meet requirements for production use
 *
 * ## Test Structure
 *
 * ### ConversionIntegrationTest
 * Tests complete data conversion workflows:
 * - **JSON Deserialization → DTO → Domain**: Full pipeline with serialization, mapping, and validation
 * - **Error Propagation**: Ensures errors bubble up correctly through the conversion chain
 * - **Composition Patterns**: Tests chaining of `catchSerialization + flatMap + zip + sequence`
 * - **Edge Cases**: Empty collections, null values, malformed data
 *
 * ```kotlin
 * @Test
 * fun `complete conversion workflow from JSON to domain`() {
 *     val json = """{"id": 1, "name": "John", "email": "john@example.com"}"""
 *
 *     val result = catchSerialization {
 *         Json.decodeFromString<UserDto>(json)
 *     }.flatMap { dto ->
 *         dto.toDomain(UserMapper)
 *     }.flatMap { user ->
 *         validateUser(user)
 *     }
 *
 *     assertTrue(result.isSuccess)
 * }
 * ```
 *
 * ### ValidationIntegrationTest
 * Tests end-to-end validation scenarios:
 * - **Multi-Field Validation**: Combines multiple validators with `zip3`, `zip4`, `zip5`
 * - **Accumulative Errors**: Tests `sequenceCollectingErrors` for batch validation
 * - **Fail-Fast vs Collect-All**: Verifies both error handling strategies
 * - **Real-World Forms**: Registration forms, profile updates, complex data structures
 *
 * ```kotlin
 * @Test
 * fun `validate registration form with multiple fields`() {
 *     val form = RegistrationForm(
 *         email = "user@example.com",
 *         username = "johndoe",
 *         password = "SecurePass123",
 *         age = 25
 *     )
 *
 *     val result = zip4(
 *         validateEmail(form.email),
 *         validateUsername(form.username),
 *         validatePassword(form.password),
 *         validateAge(form.age)
 *     ) { email, username, password, age ->
 *         ValidatedUser(username, email, password, age)
 *     }
 *
 *     assertTrue(result.isSuccess)
 * }
 * ```
 *
 * ## Test Scenarios Covered
 *
 * ### Happy Path
 * - ✅ Valid JSON → Valid DTO → Valid Domain
 * - ✅ Multiple items batch conversion (List<DTO> → Result<List<Domain>>)
 * - ✅ Complex object graphs with nested validation
 *
 * ### Error Handling
 * - ✅ Invalid JSON format (SerializationException)
 * - ✅ Missing required fields (validation failures)
 * - ✅ Invalid data formats (email, phone, ranges)
 * - ✅ Accumulative errors (multiple validation failures at once)
 *
 * ### Edge Cases
 * - ✅ Empty collections
 * - ✅ Null values and optional fields
 * - ✅ Boundary values (min/max ranges)
 * - ✅ Special characters and unicode
 * - ✅ Large datasets (performance verification)
 *
 * ### Composition Patterns
 * - ✅ `catchSerialization + flatMap`: Safe deserialization with transformation
 * - ✅ `zip + sequence`: Parallel validation with collection processing
 * - ✅ `traverse + fold`: Transform collections with custom error handling
 * - ✅ `recover + mapError`: Fallback strategies and error transformation
 *
 * ## Performance Validation
 *
 * Integration tests also verify performance characteristics:
 * - Batch operations complete in O(n) time
 * - No unnecessary object allocations
 * - Efficient error accumulation (collect-all mode)
 *
 * ## Running Integration Tests
 *
 * ```bash
 * # Run all integration tests
 * ./gradlew :test-module:test --tests "*.integration.*"
 *
 * # Run specific integration test class
 * ./gradlew :test-module:test --tests "ConversionIntegrationTest"
 *
 * # Run with detailed output
 * ./gradlew :test-module:test --tests "*.integration.*" --info
 * ```
 *
 * ## Coverage Goals
 *
 * Integration tests aim for:
 * - **Line Coverage**: >= 85% for core error handling modules
 * - **Branch Coverage**: >= 80% for conditional logic
 * - **Scenario Coverage**: All critical user workflows validated
 *
 * @see com.edugo.test.module.core for Result and AppError types
 * @see com.edugo.test.module.mapper for domain mapping utilities
 * @see com.edugo.test.module.validation for validation helpers
 * @see com.edugo.test.module.extensions for Result combinators
 */
package com.edugo.test.module.integration
