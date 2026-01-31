package com.edugo.test.module.core

/**
 * Enumeration of standardized error codes for the application.
 *
 * This enum provides a consistent way to categorize errors across the application,
 * making it easier to handle different error scenarios and provide appropriate
 * user feedback.
 *
 * Each error code includes a default message and optional HTTP status code for
 * API-related errors, making this suitable for both client and server-side usage.
 *
 * Example usage:
 * ```kotlin
 * val error = AppError.fromCode(ErrorCode.VALIDATION, "Email is required")
 * when (error.code) {
 *     ErrorCode.VALIDATION -> showValidationError(error.message)
 *     ErrorCode.NETWORK -> showNetworkError()
 *     ErrorCode.UNAUTHORIZED -> navigateToLogin()
 *     else -> showGenericError()
 * }
 * ```
 *
 * @property defaultMessage A human-readable default message for this error type
 * @property httpStatusCode The HTTP status code associated with this error (null if not HTTP-related)
 */
enum class ErrorCode(
    val defaultMessage: String,
    val httpStatusCode: Int? = null
) {
    /**
     * Unknown or unexpected error.
     * Use this as a fallback when the error type cannot be determined or classified.
     * This is for internal/non-HTTP errors where the specific category is unclear.
     * For HTTP-related server errors, use SERVER_ERROR instead.
     */
    UNKNOWN(
        defaultMessage = "An unexpected error occurred",
        httpStatusCode = null
    ),

    /**
     * Validation error for user input or data constraints.
     * This is the primary error code for HTTP 400 Bad Request scenarios.
     * Use when data doesn't meet required criteria, format, or validation rules.
     */
    VALIDATION(
        defaultMessage = "Validation failed",
        httpStatusCode = 400
    ),

    /**
     * Network-related error (connection timeout, no internet, etc.).
     * Use this for any network connectivity or communication issues.
     */
    NETWORK(
        defaultMessage = "Network error occurred",
        httpStatusCode = null
    ),

    /**
     * Resource not found error.
     * Use when a requested entity or resource doesn't exist.
     */
    NOT_FOUND(
        defaultMessage = "Resource not found",
        httpStatusCode = 404
    ),

    /**
     * Unauthorized access error.
     * Use when authentication is required but not provided or invalid.
     */
    UNAUTHORIZED(
        defaultMessage = "Unauthorized access",
        httpStatusCode = 401
    ),

    /**
     * Forbidden access error.
     * Use when the user is authenticated but lacks permissions for the resource.
     */
    FORBIDDEN(
        defaultMessage = "Access forbidden",
        httpStatusCode = 403
    ),

    /**
     * Server-side error.
     * Use for errors that originate from the backend or external services.
     */
    SERVER_ERROR(
        defaultMessage = "Server error occurred",
        httpStatusCode = 500
    ),

    /**
     * Conflict error (resource already exists, concurrent modification, etc.).
     * Use when an operation conflicts with the current state of the resource.
     */
    CONFLICT(
        defaultMessage = "Conflict detected",
        httpStatusCode = 409
    ),

    /**
     * Rate limit exceeded error.
     * Use when too many requests have been made in a given time period.
     */
    RATE_LIMIT_EXCEEDED(
        defaultMessage = "Too many requests, please try again later",
        httpStatusCode = 429
    ),

    /**
     * Service unavailable error.
     * Use when the service is temporarily unavailable (maintenance, overload, etc.).
     */
    SERVICE_UNAVAILABLE(
        defaultMessage = "Service temporarily unavailable",
        httpStatusCode = 503
    ),

    /**
     * Timeout error.
     * Use when an operation exceeds its time limit.
     */
    TIMEOUT(
        defaultMessage = "Operation timed out",
        httpStatusCode = 408
    ),

    /**
     * Serialization/Deserialization error.
     * Use when data cannot be properly serialized or deserialized.
     */
    SERIALIZATION(
        defaultMessage = "Data serialization error",
        httpStatusCode = null
    ),

    /**
     * Database or persistence error.
     * Use for errors related to data storage or retrieval.
     */
    PERSISTENCE(
        defaultMessage = "Data persistence error",
        httpStatusCode = null
    ),

    /**
     * Business logic error.
     * Use for domain-specific validation or business rule violations.
     */
    BUSINESS_LOGIC(
        defaultMessage = "Business logic error",
        httpStatusCode = 422
    );

    /**
     * Checks if this error code represents a client-side error.
     * Client errors typically have HTTP status codes in the 4xx range.
     *
     * @return true if this is a client error, false otherwise
     */
    fun isClientError(): Boolean = httpStatusCode in 400..499

    /**
     * Checks if this error code represents a server-side error.
     * Server errors typically have HTTP status codes in the 5xx range.
     *
     * @return true if this is a server error, false otherwise
     */
    fun isServerError(): Boolean = httpStatusCode in 500..599

    /**
     * Checks if this error code is retryable.
     * Some errors (network, timeout, service unavailable) are typically retryable,
     * while others (validation, not found) are not.
     *
     * @return true if the operation could be retried, false otherwise
     */
    fun isRetryable(): Boolean = when (this) {
        NETWORK, TIMEOUT, SERVICE_UNAVAILABLE, RATE_LIMIT_EXCEEDED -> true
        else -> false
    }

    companion object {
        /**
         * Gets an ErrorCode from an HTTP status code.
         * If no matching code is found, returns UNKNOWN.
         *
         * @param statusCode The HTTP status code
         * @return The matching ErrorCode or UNKNOWN
         */
        fun fromHttpStatus(statusCode: Int): ErrorCode {
            return entries.firstOrNull { it.httpStatusCode == statusCode } ?: UNKNOWN
        }
    }
}
