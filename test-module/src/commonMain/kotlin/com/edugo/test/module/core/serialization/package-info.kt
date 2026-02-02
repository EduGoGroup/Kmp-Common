/*
 * Copyright (c) 2026 EduGo Project
 * Licensed under the MIT License
 */

/**
 * Serialization utilities for Throwable and exception handling.
 *
 * This package provides custom serializers for types that are not natively serializable
 * in kotlinx.serialization, particularly `Throwable` and its subclasses.
 *
 * ## Purpose
 *
 * - Enable serialization of exception information for error reporting and logging
 * - Support multiplatform error transmission between client and server
 * - Preserve essential debugging information (message, stack trace, cause chain)
 *
 * ## Key Components
 *
 * - [ThrowableSerializer]: Custom KSerializer for Throwable instances
 *
 * ## Usage Example
 *
 * ```kotlin
 * @Serializable
 * data class ErrorReport(
 *     val errorCode: String,
 *     @Serializable(with = ThrowableSerializer::class)
 *     val exception: Throwable?
 * )
 *
 * val report = ErrorReport("E001", RuntimeException("Something failed"))
 * val json = Json.encodeToString(report)
 * ```
 *
 * ## Thread Safety
 *
 * All serializers in this package are thread-safe and stateless.
 *
 * ## Platform Considerations
 *
 * - **JVM**: Full stack trace support
 * - **JS**: Limited stack trace format
 * - **Native**: Platform-dependent stack trace availability
 *
 * @since 1.0.0
 */
package com.edugo.test.module.core.serialization
