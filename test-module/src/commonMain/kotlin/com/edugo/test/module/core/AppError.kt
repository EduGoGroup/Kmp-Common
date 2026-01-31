package com.edugo.test.module.core

import kotlin.js.JsExport
import kotlinx.datetime.Clock

/**
 * Application-level error representation with comprehensive context and traceability.
 *
 * This class encapsulates all relevant error information in a structured, immutable way,
 * making it easier to handle, log, and debug errors across the application. It supports
 * error chaining, detailed context via key-value pairs, and provides utilities for
 * analyzing the error chain.
 *
 * **Design Principles:**
 * - Immutable: All properties are val and collections are copied defensively
 * - Thread-safe: No mutable state
 * - Traceable: Maintains full exception chain and timestamp
 * - Contextual: Supports arbitrary metadata via details map
 * - Multiplatform: Works across JVM, JS, Native targets
 *
 * **Why @JsExport?**
 * The @JsExport annotation makes this class accessible from JavaScript/TypeScript when
 * targeting JS platforms. This enables seamless error handling in Kotlin Multiplatform
 * projects with web frontends, allowing JS code to catch and handle AppError instances
 * with full type safety and IDE support.
 *
 * Note: @JsExport is experimental. Suppress warnings with @OptIn(ExperimentalJsExport::class)
 * if needed.
 *
 * **Why not a data class?**
 * While this class implements data class-like behavior (copy, equals, hashCode), it's
 * defined as a regular class for two reasons:
 * 1. Kotlin data classes with @JsExport may have compatibility limitations on JS targets
 * 2. Allows custom initialization logic (defensive copying, validation) in the init block
 * 3. Provides explicit control over equality semantics for Throwable cause comparison
 *
 * Example usage:
 * ```kotlin
 * // Simple error from code
 * val error = AppError.fromCode(ErrorCode.NOT_FOUND, "User not found")
 *
 * // Error from exception with context
 * val error = AppError.fromException(
 *     exception = NetworkException("Connection failed"),
 *     code = ErrorCode.NETWORK,
 *     details = mapOf("endpoint" to "/api/users", "retries" to 3)
 * )
 *
 * // Validation error with field context
 * val error = AppError.validation(
 *     message = "Email is required",
 *     field = "email"
 * )
 *
 * // Network error preserving cause
 * val error = AppError.network(
 *     cause = SocketTimeoutException("Read timed out"),
 *     details = mapOf("timeout" to "30s")
 * )
 * ```
 *
 * @property code The categorized error code
 * @property message Human-readable error message
 * @property details Additional context as key-value pairs (immutable copy)
 * @property cause The underlying exception that caused this error, if any
 * @property timestamp When this error was created (milliseconds since epoch)
 */
@JsExport
class AppError(
    val code: ErrorCode,
    val message: String,
    inputDetails: Map<String, Any?> = emptyMap(),
    val cause: Throwable? = null,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
) {
    // Defensive copy to ensure immutability
    val details: Map<String, Any?> = inputDetails.toMap()

    init {
        require(message.isNotBlank()) { "Error message cannot be blank" }
    }

    /**
     * Creates a copy of this AppError with optionally modified properties.
     *
     * Implements data class-like behavior manually to maintain compatibility
     * with @JsExport and allow custom validation logic.
     */
    fun copy(
        code: ErrorCode = this.code,
        message: String = this.message,
        details: Map<String, Any?> = this.details,
        cause: Throwable? = this.cause,
        timestamp: Long = this.timestamp
    ): AppError = AppError(code, message, details, cause, timestamp)

    /**
     * Implements structural equality for AppError instances.
     *
     * Two AppError instances are equal if all their properties match.
     * Implemented manually instead of using data class to maintain @JsExport compatibility.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as AppError

        if (code != other.code) return false
        if (message != other.message) return false
        if (details != other.details) return false
        if (cause != other.cause) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    /**
     * Computes hash code for this AppError instance.
     *
     * Implements consistent hashing based on all properties.
     * Implemented manually instead of using data class to maintain @JsExport compatibility.
     */
    override fun hashCode(): Int {
        var result = code.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + details.hashCode()
        result = 31 * result + (cause?.hashCode() ?: 0)
        result = 31 * result + timestamp.hashCode()
        return result
    }



    /**
     * Gets the root cause of this error by traversing the exception chain.
     *
     * This follows the cause chain until it finds an exception with no cause,
     * which is considered the root cause. If this AppError has no cause,
     * returns null.
     *
     * Example:
     * ```kotlin
     * val rootCause = error.getRootCause()
     * println("Root cause: ${rootCause?.message}")
     * ```
     *
     * @return The root cause exception, or null if there is no cause
     */
    fun getRootCause(): Throwable? {
        var current = cause
        while (current?.cause != null) {
            current = current.cause
        }
        return current
    }

    /**
     * Gets all causes in the exception chain, from immediate to root.
     *
     * Returns a list where the first element is the immediate cause,
     * and the last element is the root cause. Returns an empty list
     * if there is no cause.
     *
     * Example:
     * ```kotlin
     * error.getAllCauses().forEachIndexed { index, cause ->
     *     println("Cause $index: ${cause.message}")
     * }
     * ```
     *
     * @return List of all causes in order from immediate to root
     */
    fun getAllCauses(): List<Throwable> {
        val causes = mutableListOf<Throwable>()
        var current = cause
        while (current != null) {
            causes.add(current)
            current = current.cause
        }
        return causes
    }

    /**
     * Formats the complete exception stack trace as a string.
     *
     * This includes the stack trace of the immediate cause and all
     * subsequent causes in the chain. Useful for logging and debugging.
     * If there is no cause, returns an empty string.
     *
     * Example:
     * ```kotlin
     * logger.error("Error occurred: ${error.message}\n${error.getStackTraceString()}")
     * ```
     *
     * @return Formatted stack trace string, or empty string if no cause
     */
    fun getStackTraceString(): String {
        val cause = this.cause ?: return ""
        return buildString {
            appendLine("Stack trace:")
            cause.stackTraceToString().lines().forEach { line ->
                appendLine("  $line")
            }

            val allCauses = getAllCauses()
            if (allCauses.size > 1) {
                appendLine("\nCause chain:")
                allCauses.forEachIndexed { index, throwable ->
                    appendLine("  ${index + 1}. ${throwable::class.simpleName}: ${throwable.message}")
                }
            }
        }
    }

    /**
     * Checks if this error is retryable based on its error code.
     *
     * @return true if the operation that caused this error could be retried
     */
    fun isRetryable(): Boolean = code.isRetryable()

    /**
     * Checks if this error represents a client-side error.
     *
     * @return true if this is a client error (4xx range)
     */
    fun isClientError(): Boolean = code.isClientError()

    /**
     * Checks if this error represents a server-side error.
     *
     * @return true if this is a server error (5xx range)
     */
    fun isServerError(): Boolean = code.isServerError()

    /**
     * Creates a copy of this error with additional details merged in.
     *
     * This is useful for adding context as the error propagates through layers.
     *
     * Example:
     * ```kotlin
     * val enrichedError = error.withDetails(
     *     "userId" to currentUserId,
     *     "operation" to "updateProfile"
     * )
     * ```
     *
     * @param additionalDetails Key-value pairs to add to details
     * @return A new AppError with merged details
     */
    fun withDetails(vararg additionalDetails: Pair<String, Any?>): AppError {
        return copy(details = details + additionalDetails.toMap())
    }

    /**
     * Converts this AppError to a user-friendly message.
     *
     * This sanitizes the error message to be safe for display to end users,
     * hiding sensitive technical details while providing actionable information.
     *
     * @return A user-friendly error message
     */
    fun toUserMessage(): String {
        return when (code) {
            ErrorCode.NETWORK -> "Please check your internet connection and try again"
            ErrorCode.TIMEOUT -> "The operation took too long. Please try again"
            ErrorCode.SERVICE_UNAVAILABLE -> "The service is temporarily unavailable. Please try again later"
            ErrorCode.RATE_LIMIT_EXCEEDED -> "Too many requests. Please wait a moment and try again"
            ErrorCode.UNAUTHORIZED -> "Please sign in to continue"
            ErrorCode.FORBIDDEN -> "You don't have permission to perform this action"
            ErrorCode.NOT_FOUND -> "The requested resource was not found"
            ErrorCode.VALIDATION -> message // Validation messages are usually safe to show
            else -> "Something went wrong. Please try again"
        }
    }

    /**
     * Returns a structured string representation suitable for logging.
     *
     * This includes the error code, message, details, and cause information
     * in a readable format. Sensitive information should not be included in
     * the details map.
     *
     * Format:
     * ```
     * AppError[code=NETWORK, message="Connection failed", details={endpoint=/api/users}, cause=SocketException]
     * ```
     *
     * @return Formatted string representation
     */
    override fun toString(): String = buildString {
        append("AppError[")
        append("code=$code")
        append(", message=\"$message\"")

        if (details.isNotEmpty()) {
            append(", details={")
            append(details.entries.joinToString(", ") { "${it.key}=${it.value}" })
            append("}")
        }

        cause?.let {
            append(", cause=${it::class.simpleName}: ${it.message}")
        }

        append("]")
    }

    companion object {
        /**
         * Creates an AppError from an exception.
         *
         * The error message will be extracted from the exception's message,
         * falling back to the error code's default message if the exception
         * message is null or blank.
         *
         * Example:
         * ```kotlin
         * try {
         *     performOperation()
         * } catch (e: Exception) {
         *     val error = AppError.fromException(
         *         exception = e,
         *         code = ErrorCode.NETWORK,
         *         details = mapOf("operation" to "fetchData")
         *     )
         * }
         * ```
         *
         * @param exception The exception that occurred
         * @param code The error code to categorize this error (defaults to UNKNOWN)
         * @param details Additional context information
         * @return A new AppError instance
         */
        fun fromException(
            exception: Throwable,
            code: ErrorCode = ErrorCode.UNKNOWN,
            inputDetails: Map<String, Any?> = emptyMap()
        ): AppError {
            val message = exception.message?.takeIf { it.isNotBlank() } ?: code.defaultMessage
            return AppError(
                code = code,
                message = message,
                inputDetails = inputDetails,
                cause = exception
            )
        }

        /**
         * Creates an AppError from an error code with an optional custom message.
         *
         * If no custom message is provided, uses the error code's default message.
         *
         * Example:
         * ```kotlin
         * val error = AppError.fromCode(
         *     code = ErrorCode.NOT_FOUND,
         *     customMessage = "User with ID 123 not found",
         *     details = mapOf("userId" to 123)
         * )
         * ```
         *
         * @param code The error code
         * @param customMessage Custom error message (uses code's default if null)
         * @param details Additional context information
         * @return A new AppError instance
         */
        fun fromCode(
            code: ErrorCode,
            customMessage: String? = null,
            inputDetails: Map<String, Any?> = emptyMap()
        ): AppError {
            return AppError(
                code = code,
                message = customMessage ?: code.defaultMessage,
                inputDetails = inputDetails,
                cause = null
            )
        }

        /**
         * Creates a validation error with optional field context.
         *
         * This is a convenience method for creating validation errors,
         * automatically adding the field name to the details if provided.
         *
         * Example:
         * ```kotlin
         * val error = AppError.validation(
         *     message = "Email format is invalid",
         *     field = "email"
         * )
         * ```
         *
         * @param message The validation error message
         * @param field The field that failed validation (optional)
         * @param details Additional context information
         * @return A new AppError instance with VALIDATION code
         */
        fun validation(
            message: String,
            field: String? = null,
            inputDetails: Map<String, Any?> = emptyMap()
        ): AppError {
            val enrichedDetails = if (field != null) {
                inputDetails + ("field" to field)
            } else {
                inputDetails
            }

            return AppError(
                code = ErrorCode.VALIDATION,
                message = message,
                inputDetails = enrichedDetails,
                cause = null
            )
        }

        /**
         * Creates a network error from an exception.
         *
         * This is a convenience method for network-related errors,
         * automatically setting the error code to NETWORK.
         *
         * Example:
         * ```kotlin
         * try {
         *     makeApiCall()
         * } catch (e: IOException) {
         *     val error = AppError.network(
         *         cause = e,
         *         details = mapOf("url" to apiUrl, "method" to "GET")
         *     )
         * }
         * ```
         *
         * @param cause The network-related exception
         * @param details Additional context information
         * @return A new AppError instance with NETWORK code
         */
        fun network(
            cause: Throwable,
            inputDetails: Map<String, Any?> = emptyMap()
        ): AppError {
            return fromException(
                exception = cause,
                code = ErrorCode.NETWORK,
                inputDetails = inputDetails
            )
        }

        /**
         * Creates a timeout error with optional timeout duration.
         *
         * This is a convenience method for timeout errors.
         *
         * Example:
         * ```kotlin
         * val error = AppError.timeout(
         *     message = "Request timed out after 30 seconds",
         *     details = mapOf("timeout" to "30s", "operation" to "fetchData")
         * )
         * ```
         *
         * @param message The timeout error message
         * @param details Additional context information
         * @return A new AppError instance with TIMEOUT code
         */
        fun timeout(
            message: String = ErrorCode.TIMEOUT.defaultMessage,
            inputDetails: Map<String, Any?> = emptyMap()
        ): AppError {
            return AppError(
                code = ErrorCode.TIMEOUT,
                message = message,
                inputDetails = inputDetails,
                cause = null
            )
        }

        /**
         * Creates an unauthorized access error.
         *
         * This is a convenience method for authentication errors.
         *
         * Example:
         * ```kotlin
         * val error = AppError.unauthorized(
         *     message = "Authentication token has expired",
         *     details = mapOf("tokenExpiry" to expiryTime)
         * )
         * ```
         *
         * @param message The unauthorized error message
         * @param details Additional context information
         * @return A new AppError instance with UNAUTHORIZED code
         */
        fun unauthorized(
            message: String = ErrorCode.UNAUTHORIZED.defaultMessage,
            inputDetails: Map<String, Any?> = emptyMap()
        ): AppError {
            return AppError(
                code = ErrorCode.UNAUTHORIZED,
                message = message,
                inputDetails = inputDetails,
                cause = null
            )
        }

        /**
         * Creates a not found error.
         *
         * This is a convenience method for resource not found errors.
         *
         * Example:
         * ```kotlin
         * val error = AppError.notFound(
         *     message = "User not found",
         *     details = mapOf("userId" to userId, "resource" to "User")
         * )
         * ```
         *
         * @param message The not found error message
         * @param details Additional context information
         * @return A new AppError instance with NOT_FOUND code
         */
        fun notFound(
            message: String = ErrorCode.NOT_FOUND.defaultMessage,
            inputDetails: Map<String, Any?> = emptyMap()
        ): AppError {
            return AppError(
                code = ErrorCode.NOT_FOUND,
                message = message,
                inputDetails = inputDetails,
                cause = null
            )
        }

        /**
         * Creates a server error from an exception.
         *
         * This is a convenience method for server-side errors.
         *
         * Example:
         * ```kotlin
         * try {
         *     processRequest()
         * } catch (e: Exception) {
         *     val error = AppError.serverError(
         *         cause = e,
         *         details = mapOf("endpoint" to "/api/process")
         *     )
         * }
         * ```
         *
         * @param cause The server-side exception
         * @param message Custom error message (uses cause message if null)
         * @param details Additional context information
         * @return A new AppError instance with SERVER_ERROR code
         */
        fun serverError(
            cause: Throwable? = null,
            message: String? = null,
            inputDetails: Map<String, Any?> = emptyMap()
        ): AppError {
            val errorMessage = message
                ?: cause?.message
                ?: ErrorCode.SERVER_ERROR.defaultMessage

            return AppError(
                code = ErrorCode.SERVER_ERROR,
                message = errorMessage,
                inputDetails = inputDetails,
                cause = cause
            )
        }
    }
}
