/*
 * Copyright (c) 2026 EduGo Project
 * Licensed under the MIT License
 */

/**
 * Functional extensions for Result<T> and collection operations.
 *
 * This package provides advanced functional programming utilities for working with `Result<T>`,
 * enabling safe, composable, and type-safe error handling patterns across the application.
 *
 * ## Core Utilities
 *
 * ### Result Combinators (zip, combine)
 * Combine multiple Result values with full type safety:
 *
 * ```kotlin
 * // Combine 3 independent validations
 * val result: Result<User> = zip3(
 *     validateEmail(email),
 *     validateUsername(username),
 *     validateAge(age)
 * ) { validEmail, validUsername, validAge ->
 *     User(validUsername, validEmail, validAge)
 * }
 *
 * // Extension function style
 * val user: Result<User> = validateEmail(email)
 *     .zip3(validateUsername(username), validateAge(age)) { email, username, age ->
 *         User(username, email, age)
 *     }
 * ```
 *
 * **Available combinators:**
 * - `zip2<A, B, R>`: Combine 2 Results (already in core Result.kt)
 * - `zip3<A, B, C, R>`: Combine 3 Results with different types
 * - `zip4<A, B, C, D, R>`: Combine 4 Results
 * - `zip5<A, B, C, D, E, R>`: Combine 5 Results
 *
 * ### Collection Extensions (sequence, traverse)
 * Transform collections of Result into Result of collections:
 *
 * ```kotlin
 * // Fail-fast: stops at first error
 * val users: List<UserDto> = fetchUsersDto()
 * val result: Result<List<User>> = users
 *     .map { dto -> dto.toDomain(UserMapper) }  // List<Result<User>>
 *     .sequence()                                // Result<List<User>>
 *
 * // Collect all errors
 * val result: Result<List<User>> = users
 *     .map { dto -> dto.toDomain(UserMapper) }
 *     .sequenceCollectingErrors()  // Returns all validation errors
 *
 * // Traverse: map + sequence in one operation
 * val result: Result<List<User>> = users.traverse { dto ->
 *     dto.toDomain(UserMapper)
 * }
 * ```
 *
 * ## Error Handling Strategies
 *
 * ### 1. Fail-Fast (Default)
 * Stops at the first error encountered. Best for:
 * - User input validation (show first error immediately)
 * - Critical operations where any failure should halt processing
 * - Performance-sensitive scenarios
 *
 * ```kotlin
 * val result = listOfResults.sequence()  // Stops at first Failure
 * ```
 *
 * ### 2. Collect All Errors
 * Accumulates all errors before failing. Best for:
 * - Form validation (show all field errors at once)
 * - Batch processing with error reports
 * - Data migration/import scenarios
 *
 * ```kotlin
 * val result = listOfResults.sequenceCollectingErrors()  // Collects all Failures
 * ```
 *
 * ## Helper Functions
 *
 * ### Inspection and Filtering
 * ```kotlin
 * val results: List<Result<T>> = fetchResults()
 *
 * // Extract successes and failures
 * val (successes, failures) = results.partition()
 * val successValues: List<T> = results.collectSuccesses()
 * val errorMessages: List<String> = results.collectFailures()
 *
 * // Counting and checking
 * val successCount: Int = results.countSuccesses()
 * val hasAnyError: Boolean = results.anyFailure()
 * val allOk: Boolean = results.allSuccess()
 * ```
 *
 * ### Partial Processing
 * ```kotlin
 * // Process only successful values, ignore failures
 * val validItems: List<T> = results.collectSuccesses()
 *
 * // Transform and partition in one pass
 * val (transformed, errors) = dtos.traversePartition { dto ->
 *     dto.toDomain(mapper)
 * }
 * ```
 *
 * ## Real-World Examples
 *
 * ### Multi-Field Form Validation
 * ```kotlin
 * data class RegistrationForm(
 *     val email: String,
 *     val username: String,
 *     val password: String,
 *     val age: Int
 * )
 *
 * fun validateForm(form: RegistrationForm): Result<ValidatedUser> {
 *     return zip4(
 *         validateEmail(form.email),
 *         validateUsername(form.username),
 *         validatePassword(form.password),
 *         validateAge(form.age)
 *     ) { email, username, password, age ->
 *         ValidatedUser(username, email, password, age)
 *     }
 * }
 * ```
 *
 * ### Batch DTO Conversion
 * ```kotlin
 * suspend fun importUsers(dtos: List<UserDto>): ImportResult {
 *     // Collect all errors for comprehensive report
 *     return dtos.traverseCollectingErrors { dto ->
 *         dto.toDomain(UserMapper)
 *     }.fold(
 *         onSuccess = { users ->
 *             repository.saveAll(users)
 *             ImportResult.Success(users.size)
 *         },
 *         onFailure = { errors ->
 *             ImportResult.PartialFailure(
 *                 imported = dtos.count { it.isValid() },
 *                 errors = errors
 *             )
 *         }
 *     )
 * }
 * ```
 *
 * ### Parallel Validation with Independent Checks
 * ```kotlin
 * fun validateUserProfile(profile: UserProfile): Result<ValidatedProfile> {
 *     // All validations run independently
 *     val emailCheck = validateEmail(profile.email)
 *     val phoneCheck = validatePhone(profile.phone)
 *     val addressCheck = validateAddress(profile.address)
 *     val ageCheck = validateAge(profile.age)
 *
 *     return zip4(emailCheck, phoneCheck, addressCheck, ageCheck) {
 *         email, phone, address, age ->
 *         ValidatedProfile(email, phone, address, age)
 *     }
 * }
 * ```
 *
 * ## Performance Considerations
 *
 * - **Zero-copy operations**: All functions use inline and avoid unnecessary allocations
 * - **O(n) complexity**: Collection operations are single-pass where possible
 * - **Lazy evaluation**: Fail-fast mode stops immediately on first error
 * - **Type safety**: No runtime casts, full compile-time guarantees
 *
 * ## Platform Support
 *
 * All utilities are multiplatform-compatible:
 * - **JVM**: Full support with @JvmName annotations for Java interop
 * - **JS**: Browser and Node.js compatible
 * - **Native**: iOS, Android Native, Desktop
 *
 * @see com.edugo.test.module.core.Result for base Result type
 * @see com.edugo.test.module.mapper for domain mapping utilities
 * @see com.edugo.test.module.validation for validation helpers
 */
package com.edugo.test.module.extensions
