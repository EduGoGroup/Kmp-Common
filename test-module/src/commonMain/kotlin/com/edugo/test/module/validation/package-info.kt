/*
 * Copyright (c) 2026 EduGo Project
 * Licensed under the MIT License
 */

/**
 * Validation utilities for domain models and DTOs.
 *
 * This package provides a comprehensive set of reusable validation functions and patterns
 * for data validation with integrated error handling through `Result<T>`.
 *
 * ## Validation Strategies
 *
 * ### 1. Fail-Fast Validation
 * Stops at the first validation error encountered.
 *
 * ```kotlin
 * fun validateUser(dto: UserDto): Result<User> {
 *     return validateEmail(dto.email)  // Stops here if invalid
 *         .flatMap { email ->
 *             validateNotBlank(dto.name, "name")
 *                 .map { name -> User(name, email) }
 *         }
 * }
 * ```
 *
 * ### 2. Accumulative Validation
 * Collects all validation errors before failing.
 *
 * ```kotlin
 * fun validateUserAccumulative(dto: UserDto): Result<User> {
 *     return accumulateValidationErrors {
 *         val email = validateEmail(dto.email).getOrAccumulate()
 *         val name = validateNotBlank(dto.name, "name").getOrAccumulate()
 *         val age = validateRange(dto.age, 18, 120, "age").getOrAccumulate()
 *
 *         User(name, email, age)
 *     }
 * }
 * // Returns Result.Failure with all validation errors if any fail
 * ```
 *
 * ## Validation Helpers
 *
 * ### String Validation
 * - `validateNotBlank(value, fieldName)`: Ensures string is not empty/blank
 * - `validateEmail(email)`: Email format validation
 * - `validateMinLength(value, minLength, fieldName)`: Minimum length check
 * - `validateMaxLength(value, maxLength, fieldName)`: Maximum length check
 * - `validateLengthRange(value, min, max, fieldName)`: Length within range
 * - `validatePattern(value, regex, fieldName)`: Regex pattern matching
 *
 * ### Numeric Validation
 * - `validateRange(value, min, max, fieldName)`: Value within range (Int, Long, Double)
 * - `validatePositive(value, fieldName)`: Value > 0
 * - `validateNonNegative(value, fieldName)`: Value >= 0
 *
 * ### Collection Validation
 * - `validateNotEmpty(collection, fieldName)`: Collection not empty
 * - `validateMinSize(collection, minSize, fieldName)`: Minimum size check
 * - `validateMaxSize(collection, maxSize, fieldName)`: Maximum size check
 * - `validateIn(value, allowedValues, fieldName)`: Value in allowed set
 *
 * ## Custom Validations
 *
 * Create reusable validators using the same pattern:
 *
 * ```kotlin
 * fun validateUsername(username: String): Result<String> {
 *     return when {
 *         username.isBlank() -> failure("Username cannot be blank")
 *         username.length < 3 -> failure("Username must be at least 3 characters")
 *         username.length > 30 -> failure("Username must be at most 30 characters")
 *         !username.matches(Regex("[a-zA-Z0-9_]+")) ->
 *             failure("Username can only contain letters, numbers, and underscores")
 *         else -> success(username)
 *     }
 * }
 * ```
 *
 * ## Integration with Mappers
 *
 * Validations integrate seamlessly with domain mappers:
 *
 * ```kotlin
 * override fun toDomain(dto: UserDto): Result<User> {
 *     return accumulateValidationErrors {
 *         val email = validateEmail(dto.email).getOrAccumulate()
 *         val username = validateUsername(dto.username).getOrAccumulate()
 *         val age = validateRange(dto.age, 18, 120, "age").getOrAccumulate()
 *
 *         User(username, email, age)
 *     }
 * }
 * ```
 *
 * ## Error Messages
 *
 * All validation functions generate descriptive error messages:
 * - Include field names for clarity
 * - Specify constraints that were violated
 * - Support i18n through message customization
 *
 * ## Platform Considerations
 *
 * - **Multiplatform**: All validators are platform-agnostic
 * - **Performance**: Lightweight with minimal allocations
 * - **Composable**: Validators can be combined using Result operations
 *
 * @see com.edugo.test.module.mapper for domain mapping utilities
 * @see com.edugo.test.module.core.Result for Result type documentation
 */
package com.edugo.test.module.validation
