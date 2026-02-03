package com.edugo.test.module.core

import com.edugo.test.module.core.serialization.ThrowableSerializer
import kotlin.js.JsExport
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
 * val error = AppError.fromCode(ErrorCode.BUSINESS_RESOURCE_NOT_FOUND, "User not found")
 *
 * // Error from exception with context
 * val error = AppError.fromException(
 *     exception = NetworkException("Connection failed"),
 *     code = ErrorCode.NETWORK_NO_CONNECTION,
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
@Serializable
@JsExport
class AppError internal constructor(
    val code: ErrorCode,
    val message: String,
    @SerialName("details")
    internal val _details: Map<String, String> = emptyMap(),
    @Serializable(with = ThrowableSerializer::class)
    val cause: Throwable? = null,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
) {
    // Public immutable view
    val details: Map<String, String> get() = _details

    init {
        require(message.isNotBlank()) { "Error message cannot be blank" }

        // Performance warning for large details maps
        if (_details.size > 50) {
            println("⚠️ AppError created with ${_details.size} detail entries. " +
                    "Consider if all this data is necessary for error context.")
        }
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
        details: Map<String, String> = this._details,
        cause: Throwable? = this.cause,
        timestamp: Long = this.timestamp
    ): AppError = AppError(code, message, detailsInternal = details, cause, timestamp)

    /**
     * Implements structural equality for AppError instances.
     *
     * Two AppError instances are equal if all their properties match.
     *
     * **Important:** The `cause` property is compared by reference equality (===),
     * not structural equality. This means two AppError instances with different
     * Throwable objects (even if they have identical messages and types) will
     * be considered unequal.
     *
     * Rationale: Throwable doesn't implement structural equality, and comparing
     * exceptions by reference is the most predictable behavior for debugging and
     * error tracking.
     *
     * Example:
     * ```kotlin
     * val error1 = AppError.fromException(RuntimeException("fail"))
     * val error2 = AppError.fromException(RuntimeException("fail"))
     * // error1 != error2 because the Throwable instances are different objects
     * ```
     *
     * Implemented manually instead of using data class to maintain @JsExport compatibility.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as AppError

        if (code != other.code) return false
        if (message != other.message) return false
        if (details != other.details) return false
        if (cause != other.cause) return false // Reference equality
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
        result = 31 * result + _details.hashCode()
        result = 31 * result + (cause?.hashCode() ?: 0)
        result = 31 * result + timestamp.toInt() // Direct conversion, no hashCode() needed
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
     * Cached list of all causes in the exception chain.
     *
     * Computed lazily on first access and cached for performance.
     * The list contains all causes from immediate to root.
     */
    private val _allCauses: List<Throwable> by lazy {
        val causes = mutableListOf<Throwable>()
        var current = cause
        while (current != null) {
            causes.add(current)
            current = current.cause
        }
        causes.toList()
    }

    /**
     * Gets all causes in the exception chain, from immediate to root.
     *
     * Returns a cached list where the first element is the immediate cause,
     * and the last element is the root cause. Returns an empty list
     * if there is no cause.
     *
     * **Performance note:** Results are cached on first call. Subsequent
     * calls return the same list without re-traversing the chain.
     *
     * Example:
     * ```kotlin
     * error.getAllCauses().forEachIndexed { index, cause ->
     *     println("Cause $index: ${cause.message}")
     * }
     * ```
     *
     * @return Immutable list of all causes in order from immediate to root
     */
    fun getAllCauses(): List<Throwable> = _allCauses

    /**
     * Formats the complete exception stack trace as a string.
     *
     * This includes the stack trace of the immediate cause and all
     * subsequent causes in the chain. Useful for logging and debugging.
     * If there is no cause, returns an empty string.
     *
     * **Cross-platform note:** Uses explicit '\n' for consistent newlines
     * across JVM, JS, and Native platforms.
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
            append("Stack trace:\n")
            cause.stackTraceToString().lines().forEach { line ->
                append("  $line\n")
            }

            val allCauses = getAllCauses()
            if (allCauses.size > 1) {
                append("\nCause chain:\n")
                allCauses.forEachIndexed { index, throwable ->
                    append("  ${index + 1}. ${throwable::class.simpleName}: ${throwable.message}\n")
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
    fun withDetails(vararg additionalDetails: Pair<String, String>): AppError {
        return copy(details = details + additionalDetails.toMap())
    }

    /**
     * Converts this AppError to a user-friendly message.
     *
     * This sanitizes the error message to be safe for display to end users,
     * hiding sensitive technical details while providing actionable information.
     *
     * **TODO - i18n:** User-facing strings are currently hardcoded in English.
     * When implementing internationalization:
     * 1. Extract all string literals to a resource file system
     * 2. Use message keys (e.g., "error.network.connection")
     * 3. Load localized strings based on user's language preference
     * 4. Consider using kotlinx-resources or platform-specific solutions
     *
     * @return A user-friendly error message
     */
    fun toUserMessage(): String {
        // TODO: Replace hardcoded strings with i18n resource lookups
        return when {
            code.isNetworkError() -> "Please check your internet connection and try again"
            code.isAuthError() -> when (code) {
                ErrorCode.AUTH_UNAUTHORIZED, ErrorCode.AUTH_TOKEN_EXPIRED,
                ErrorCode.AUTH_INVALID_CREDENTIALS, ErrorCode.AUTH_SESSION_INVALIDATED,
                ErrorCode.AUTH_REFRESH_TOKEN_INVALID -> "Please sign in to continue"
                ErrorCode.AUTH_FORBIDDEN -> "You don't have permission to perform this action"
                ErrorCode.AUTH_ACCOUNT_LOCKED -> "Your account has been locked. Please contact support"
                else -> "Authentication error. Please sign in again"
            }
            code.isValidationError() -> message // Validation messages are usually safe to show
            code.isBusinessError() -> when (code) {
                ErrorCode.BUSINESS_RESOURCE_NOT_FOUND -> "The requested resource was not found"
                ErrorCode.BUSINESS_RATE_LIMIT_EXCEEDED -> "Too many requests. Please wait a moment and try again"
                ErrorCode.BUSINESS_OPERATION_EXPIRED -> "This operation has expired. Please try again"
                else -> message
            }
            code.isSystemError() -> when (code) {
                ErrorCode.SYSTEM_SERVICE_UNAVAILABLE -> "The service is temporarily unavailable. Please try again later"
                else -> "Something went wrong. Please try again"
            }
            else -> "Something went wrong. Please try again"
        }
    }

    /**
     * Returns a structured string representation suitable for logging.
     *
     * This includes the error code, message, details, and cause information
     * in a readable format. Optimized for readability in logs.
     *
     * Format:
     * ```
     * AppError[
     *   code=NETWORK_TIMEOUT (1000, HTTP 408)
     *   message="Request timed out"
     *   details=3 entries: endpoint=/api/users, timeout=30s, retries=3
     *   cause=SocketTimeoutException: Read timed out
     * ]
     * ```
     *
     * @return Formatted string representation
     */
    override fun toString(): String = buildString {
        append("AppError[")

        // Código con info adicional
        append("code=${code.name} (${code.code}, HTTP ${code.httpStatusCode})")

        // Mensaje (truncado si es muy largo)
        append(", message=\"")
        if (message.length > 100) {
            append(message.take(97))
            append("...")
        } else {
            append(message)
        }
        append("\"")

        // Details con preview
        if (details.isNotEmpty()) {
            append(", details=${details.size} entries")
            if (details.size <= 5) {
                append(": ")
                append(details.entries.joinToString(", ") { "${it.key}=${it.value}" })
            } else {
                append(": ")
                val preview = details.entries.take(3).joinToString(", ") { "${it.key}=${it.value}" }
                append(preview)
                append(", ... (+${details.size - 3} more)")
            }
        }

        // Cause
        cause?.let {
            append(", cause=${it::class.simpleName}: ${it.message ?: "(no message)"}")
        }

        append("]")
    }

    companion object {
        /**
         * Public factory method that creates an AppError with defensive copy of details.
         * This acts as the public constructor.
         */
        operator fun invoke(
            code: ErrorCode,
            message: String,
            detailsInternal: Map<String, String> = emptyMap(),
            cause: Throwable? = null,
            timestamp: Long = Clock.System.now().toEpochMilliseconds()
        ): AppError = AppError(
            code = code,
            message = message,
            _details = detailsInternal.toMap(), // Defensive copy
            cause = cause,
            timestamp = timestamp
        )

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
         *         code = ErrorCode.NETWORK_NO_CONNECTION,
         *         details = mapOf("operation" to "fetchData")
         *     )
         * }
         * ```
         *
         * @param exception The exception that occurred
         * @param code The error code to categorize this error (defaults to SYSTEM_UNKNOWN_ERROR)
         * @param details Additional context information
         * @return A new AppError instance
         */
        fun fromException(
            exception: Throwable,
            code: ErrorCode = ErrorCode.SYSTEM_UNKNOWN_ERROR,
            details: Map<String, String> = emptyMap()
        ): AppError {
            val exceptionMessage = exception.message?.takeIf { it.isNotBlank() }

            // Log cuando el mensaje está vacío para ayudar en debugging
            val message = if (exceptionMessage != null) {
                exceptionMessage
            } else {
                // TODO: Integrar con sistema de logging cuando esté disponible
                // Logger.debug("Exception without message, using default: ${code.description}")
                println("⚠️ AppError.fromException: Exception '${exception::class.simpleName}' has blank message, using default: ${code.description}")
                code.description
            }

            return AppError(
                code = code,
                message = message,
                detailsInternal = details,
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
            details: Map<String, String> = emptyMap()
        ): AppError {
            return AppError(
                code = code,
                message = customMessage ?: code.description,
                detailsInternal = details,
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
         * @return A new AppError instance with VALIDATION_INVALID_INPUT code
         */
        fun validation(
            message: String,
            field: String? = null,
            details: Map<String, String> = emptyMap()
        ): AppError {
            val enrichedDetails = if (field != null) {
                details + ("field" to field)
            } else {
                details
            }

            return AppError(
                code = ErrorCode.VALIDATION_INVALID_INPUT,
                message = message,
                detailsInternal = enrichedDetails,
                cause = null
            )
        }

        /**
         * Creates a network error from an exception.
         *
         * This is a convenience method for network-related errors,
         * automatically setting the error code to NETWORK_NO_CONNECTION.
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
         * @return A new AppError instance with NETWORK_NO_CONNECTION code
         */
        fun network(
            cause: Throwable,
            details: Map<String, String> = emptyMap()
        ): AppError {
            return fromException(
                exception = cause,
                code = ErrorCode.NETWORK_NO_CONNECTION,
                details = details
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
         * @return A new AppError instance with NETWORK_TIMEOUT code
         */
        fun timeout(
            message: String = ErrorCode.NETWORK_TIMEOUT.description,
            details: Map<String, String> = emptyMap()
        ): AppError {
            return AppError(
                code = ErrorCode.NETWORK_TIMEOUT,
                message = message,
                detailsInternal = details,
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
         * @return A new AppError instance with AUTH_UNAUTHORIZED code
         */
        fun unauthorized(
            message: String = ErrorCode.AUTH_UNAUTHORIZED.description,
            details: Map<String, String> = emptyMap()
        ): AppError {
            return AppError(
                code = ErrorCode.AUTH_UNAUTHORIZED,
                message = message,
                detailsInternal = details,
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
         * @return A new AppError instance with BUSINESS_RESOURCE_NOT_FOUND code
         */
        fun notFound(
            message: String = ErrorCode.BUSINESS_RESOURCE_NOT_FOUND.description,
            details: Map<String, String> = emptyMap()
        ): AppError {
            return AppError(
                code = ErrorCode.BUSINESS_RESOURCE_NOT_FOUND,
                message = message,
                detailsInternal = details,
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
         * @return A new AppError instance with SYSTEM_INTERNAL_ERROR code
         */
        fun serverError(
            cause: Throwable? = null,
            message: String? = null,
            details: Map<String, String> = emptyMap()
        ): AppError {
            val errorMessage = message
                ?: cause?.message
                ?: ErrorCode.SYSTEM_INTERNAL_ERROR.description

            return AppError(
                code = ErrorCode.SYSTEM_INTERNAL_ERROR,
                message = errorMessage,
                detailsInternal = details,
                cause = cause
            )
        }
    }
}
