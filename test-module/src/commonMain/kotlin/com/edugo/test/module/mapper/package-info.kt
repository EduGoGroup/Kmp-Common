/*
 * Copyright (c) 2026 EduGo Project
 * Licensed under the MIT License
 */

/**
 * Domain mapping utilities for bidirectional DTO ↔ Domain conversion.
 *
 * This package provides a type-safe, functional approach to mapping between Data Transfer Objects (DTOs)
 * and domain models, with built-in validation and error handling through `Result<T>`.
 *
 * ## Core Components
 *
 * - **DomainMapper**: Generic interface for bidirectional mapping
 * - **MapperExtensions**: Extension functions for convenient mapping operations
 * - **Integration with Result<T>**: All mappings return `Result<Domain>` for safe error handling
 *
 * ## Usage Patterns
 *
 * ### Basic Mapping
 * ```kotlin
 * data class UserDto(val name: String, val email: String)
 * data class User(val name: String, val email: String)
 *
 * object UserMapper : DomainMapper<UserDto, User> {
 *     override fun toDomain(dto: UserDto): Result<User> = success(
 *         User(dto.name, dto.email)
 *     )
 *     override fun toDto(domain: User): UserDto = UserDto(domain.name, domain.email)
 * }
 *
 * val userResult = userDto.toDomain(UserMapper)
 * ```
 *
 * ### Mapping with Validation
 * ```kotlin
 * override fun toDomain(dto: UserDto): Result<User> {
 *     return validateEmail(dto.email)
 *         .flatMap { email ->
 *             validateNotBlank(dto.name, "name")
 *                 .map { name -> User(name, email) }
 *         }
 * }
 * ```
 *
 * ### Collection Mapping
 * ```kotlin
 * val dtos: List<UserDto> = fetchUsers()
 * val users: Result<List<User>> = dtos.toDomainList(UserMapper)
 * ```
 *
 * ## Error Handling
 *
 * All mapping operations return `Result<T>`, allowing for:
 * - Composable error handling with `flatMap`, `map`, `mapError`
 * - Integration with validation utilities
 * - Accumulative error reporting for batch operations
 *
 * ## Platform Considerations
 *
 * - **Multiplatform**: All mappers are platform-agnostic
 * - **Performance**: Inline functions minimize overhead
 * - **Type Safety**: Leverages Kotlin's type system for compile-time guarantees
 *
 * @see com.edugo.test.module.validation for validation utilities
 * @see com.edugo.test.module.core.Result for Result type documentation
 */
package com.edugo.test.module.mapper
