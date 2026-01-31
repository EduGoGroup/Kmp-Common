package com.edugo.test.module.core

/**
 * Standardized error codes for the EduGo application.
 *
 * This enum provides a comprehensive catalog of error codes organized by categories,
 * each with a unique numeric code and human-readable description. The numeric codes
 * are organized in ranges by category to allow easy identification and extension.
 *
 * ## Categories and Ranges
 *
 * | Category   | Range       | Description                                    |
 * |------------|-------------|------------------------------------------------|
 * | NETWORK    | 1000-1999   | Network connectivity and communication errors |
 * | AUTH       | 2000-2999   | Authentication and authorization errors       |
 * | VALIDATION | 3000-3999   | Input validation and format errors            |
 * | BUSINESS   | 4000-4999   | Business logic and domain rule violations     |
 * | SYSTEM     | 5000-5999   | System-level and infrastructure errors        |
 *
 * ## Usage Examples
 *
 * ```kotlin
 * // Create an error with a specific code
 * val error = AppError.fromCode(ErrorCode.AUTH_TOKEN_EXPIRED, "Session has expired")
 *
 * // Check error category
 * when {
 *     error.code.isNetworkError() -> showNetworkError()
 *     error.code.isAuthError() -> navigateToLogin()
 *     error.code.isValidationError() -> showValidationFeedback()
 *     else -> showGenericError()
 * }
 *
 * // Check if error is retryable
 * if (error.code.isRetryable()) {
 *     scheduleRetry()
 * }
 * ```
 *
 * @property code Unique numeric identifier for this error (used for logging and tracking)
 * @property description Human-readable description of the error condition
 */
enum class ErrorCode(
    val code: Int,
    val description: String,
    val retryable: Boolean = false
) {
    // ============================================================================
    // NETWORK ERRORS (1000-1999)
    // Network connectivity, communication, and transport layer errors
    // ============================================================================

    /**
     * Network connection timeout.
     * The connection attempt exceeded the configured timeout period.
     */
    NETWORK_TIMEOUT(1000, "Network connection timed out", retryable = true),

    /**
     * No network connection available.
     * The device has no active network connection (WiFi, cellular, etc.).
     */
    NETWORK_NO_CONNECTION(1001, "No network connection available", retryable = true),

    /**
     * Server returned an error response.
     * The server responded with an HTTP 5xx status code.
     */
    NETWORK_SERVER_ERROR(1002, "Server returned an error response", retryable = true),

    /**
     * DNS resolution failed.
     * Unable to resolve the server hostname to an IP address.
     */
    NETWORK_DNS_FAILURE(1003, "DNS resolution failed", retryable = true),

    /**
     * SSL/TLS certificate error.
     * The server's SSL certificate is invalid, expired, or untrusted.
     */
    NETWORK_SSL_ERROR(1004, "SSL/TLS certificate error", retryable = false),

    /**
     * Connection was reset by the server.
     * The server unexpectedly closed the connection.
     */
    NETWORK_CONNECTION_RESET(1005, "Connection reset by server", retryable = true),

    /**
     * Request was cancelled.
     * The network request was cancelled before completion.
     */
    NETWORK_REQUEST_CANCELLED(1006, "Network request was cancelled", retryable = false),

    // ============================================================================
    // AUTH ERRORS (2000-2999)
    // Authentication, authorization, and session management errors
    // ============================================================================

    /**
     * User is not authenticated.
     * The request requires authentication but no valid credentials were provided.
     */
    AUTH_UNAUTHORIZED(2000, "Authentication required", retryable = false),

    /**
     * Authentication token has expired.
     * The session or access token is no longer valid.
     */
    AUTH_TOKEN_EXPIRED(2001, "Authentication token has expired", retryable = true),

    /**
     * Invalid credentials provided.
     * The username/password or other credentials are incorrect.
     */
    AUTH_INVALID_CREDENTIALS(2002, "Invalid credentials provided", retryable = false),

    /**
     * User does not have permission.
     * The authenticated user lacks the required permissions for this operation.
     */
    AUTH_FORBIDDEN(2003, "Access denied - insufficient permissions", retryable = false),

    /**
     * Account is locked or disabled.
     * The user account has been locked due to security policy or admin action.
     */
    AUTH_ACCOUNT_LOCKED(2004, "Account is locked or disabled", retryable = false),

    /**
     * Session has been invalidated.
     * The user session was invalidated (e.g., logged out from another device).
     */
    AUTH_SESSION_INVALIDATED(2005, "Session has been invalidated", retryable = false),

    /**
     * Refresh token is invalid or expired.
     * Cannot refresh the access token with the provided refresh token.
     */
    AUTH_REFRESH_TOKEN_INVALID(2006, "Refresh token is invalid or expired", retryable = false),

    // ============================================================================
    // VALIDATION ERRORS (3000-3999)
    // Input validation, data format, and constraint errors
    // ============================================================================

    /**
     * Invalid input data.
     * The provided input does not meet validation requirements.
     */
    VALIDATION_INVALID_INPUT(3000, "Invalid input data", retryable = false),

    /**
     * Required field is missing.
     * A required field was not provided in the request.
     */
    VALIDATION_MISSING_FIELD(3001, "Required field is missing", retryable = false),

    /**
     * Invalid data format.
     * The data format does not match the expected pattern (e.g., email, phone).
     */
    VALIDATION_FORMAT_ERROR(3002, "Invalid data format", retryable = false),

    /**
     * Value is out of allowed range.
     * The provided value exceeds minimum or maximum bounds.
     */
    VALIDATION_OUT_OF_RANGE(3003, "Value is out of allowed range", retryable = false),

    /**
     * Value exceeds maximum length.
     * The provided string or collection exceeds the maximum allowed length.
     */
    VALIDATION_MAX_LENGTH_EXCEEDED(3004, "Value exceeds maximum length", retryable = false),

    /**
     * Invalid email format.
     * The provided email address is not in a valid format.
     */
    VALIDATION_INVALID_EMAIL(3005, "Invalid email format", retryable = false),

    /**
     * Duplicate value not allowed.
     * The provided value already exists and duplicates are not permitted.
     */
    VALIDATION_DUPLICATE_VALUE(3006, "Duplicate value not allowed", retryable = false),

    // ============================================================================
    // BUSINESS ERRORS (4000-4999)
    // Business logic, domain rules, and operation errors
    // ============================================================================

    /**
     * Resource not found.
     * The requested resource does not exist.
     */
    BUSINESS_RESOURCE_NOT_FOUND(4000, "Resource not found", retryable = false),

    /**
     * Operation not allowed.
     * The requested operation is not permitted in the current state.
     */
    BUSINESS_OPERATION_NOT_ALLOWED(4001, "Operation not allowed", retryable = false),

    /**
     * Resource conflict.
     * The operation conflicts with the current state of the resource.
     */
    BUSINESS_RESOURCE_CONFLICT(4002, "Resource conflict detected", retryable = false),

    /**
     * Insufficient balance or quota.
     * The user does not have sufficient balance or quota for this operation.
     */
    BUSINESS_INSUFFICIENT_BALANCE(4003, "Insufficient balance or quota", retryable = false),

    /**
     * Rate limit exceeded.
     * Too many requests have been made in a given time period.
     */
    BUSINESS_RATE_LIMIT_EXCEEDED(4004, "Rate limit exceeded", retryable = true),

    /**
     * Feature not available.
     * The requested feature is not available in the current plan or configuration.
     */
    BUSINESS_FEATURE_NOT_AVAILABLE(4005, "Feature not available", retryable = false),

    /**
     * Operation expired.
     * The operation or action has expired and is no longer valid.
     */
    BUSINESS_OPERATION_EXPIRED(4006, "Operation has expired", retryable = false),

    // ============================================================================
    // SYSTEM ERRORS (5000-5999)
    // System-level, infrastructure, and unexpected errors
    // ============================================================================

    /**
     * Unknown system error.
     * An unexpected error occurred that could not be categorized.
     */
    SYSTEM_UNKNOWN_ERROR(5000, "An unexpected error occurred", retryable = true),

    /**
     * Configuration error.
     * The system is misconfigured or missing required configuration.
     */
    SYSTEM_CONFIGURATION_ERROR(5001, "System configuration error", retryable = false),

    /**
     * Service unavailable.
     * The service is temporarily unavailable (maintenance, overload, etc.).
     */
    SYSTEM_SERVICE_UNAVAILABLE(5002, "Service temporarily unavailable", retryable = true),

    /**
     * Database error.
     * An error occurred while accessing the database.
     */
    SYSTEM_DATABASE_ERROR(5003, "Database error occurred", retryable = true),

    /**
     * Serialization error.
     * Failed to serialize or deserialize data.
     */
    SYSTEM_SERIALIZATION_ERROR(5004, "Data serialization error", retryable = false),

    /**
     * External service error.
     * An external dependency or third-party service failed.
     */
    SYSTEM_EXTERNAL_SERVICE_ERROR(5005, "External service error", retryable = true),

    /**
     * Internal error.
     * An internal system error occurred.
     */
    SYSTEM_INTERNAL_ERROR(5006, "Internal system error", retryable = true);

    /**
     * Maps this error code to the corresponding HTTP status code.
     *
     * @return The HTTP status code (400-599), or 500 if no specific mapping exists
     */
    val httpStatusCode: Int
        get() = HTTP_STATUS_MAP[this] ?: 500

    /**
     * Checks if this is a network-related error (1000-1999 range).
     */
    fun isNetworkError(): Boolean = code in 1000..1999

    /**
     * Checks if this is an authentication/authorization error (2000-2999 range).
     */
    fun isAuthError(): Boolean = code in 2000..2999

    /**
     * Checks if this is a validation error (3000-3999 range).
     */
    fun isValidationError(): Boolean = code in 3000..3999

    /**
     * Checks if this is a business logic error (4000-4999 range).
     */
    fun isBusinessError(): Boolean = code in 4000..4999

    /**
     * Checks if this is a system error (5000-5999 range).
     */
    fun isSystemError(): Boolean = code in 5000..5999

    /**
     * Checks if this error code represents a client-side error.
     * Client errors typically have HTTP status codes in the 4xx range.
     */
    fun isClientError(): Boolean = httpStatusCode in 400..499

    /**
     * Checks if this error code represents a server-side error.
     * Server errors typically have HTTP status codes in the 5xx range.
     */
    fun isServerError(): Boolean = httpStatusCode in 500..599

    /**
     * Checks if this error code is retryable.
     *
     * Retryable errors are typically:
     * - Temporary network issues (timeout, no connection, DNS failure)
     * - Server-side errors (server error, service unavailable)
     * - Rate limiting (can retry after waiting)
     * - Token expiration (can refresh and retry)
     *
     * @return true if the error condition might be resolved by retrying the operation
     */
    fun isRetryable(): Boolean = retryable

    companion object {
        /**
         * Maps error codes to their corresponding HTTP status codes.
         * Errors without a direct HTTP mapping default to 500.
         */
        private val HTTP_STATUS_MAP: Map<ErrorCode, Int> = mapOf(
            // Network errors -> 4xx/5xx
            NETWORK_TIMEOUT to 408,
            NETWORK_NO_CONNECTION to 503,
            NETWORK_SERVER_ERROR to 502,
            NETWORK_DNS_FAILURE to 503,
            NETWORK_SSL_ERROR to 495,
            NETWORK_CONNECTION_RESET to 503,
            NETWORK_REQUEST_CANCELLED to 499,

            // Auth errors -> 401/403/423
            AUTH_UNAUTHORIZED to 401,
            AUTH_TOKEN_EXPIRED to 401,
            AUTH_INVALID_CREDENTIALS to 401,
            AUTH_FORBIDDEN to 403,
            AUTH_ACCOUNT_LOCKED to 423,
            AUTH_SESSION_INVALIDATED to 401,
            AUTH_REFRESH_TOKEN_INVALID to 401,

            // Validation errors -> 400/409/422
            VALIDATION_INVALID_INPUT to 400,
            VALIDATION_MISSING_FIELD to 422,
            VALIDATION_FORMAT_ERROR to 422,
            VALIDATION_OUT_OF_RANGE to 422,
            VALIDATION_MAX_LENGTH_EXCEEDED to 422,
            VALIDATION_INVALID_EMAIL to 422,
            VALIDATION_DUPLICATE_VALUE to 409,

            // Business errors -> 402/403/404/409/410/429/501
            BUSINESS_RESOURCE_NOT_FOUND to 404,
            BUSINESS_OPERATION_NOT_ALLOWED to 403,
            BUSINESS_RESOURCE_CONFLICT to 409,
            BUSINESS_INSUFFICIENT_BALANCE to 402,
            BUSINESS_RATE_LIMIT_EXCEEDED to 429,
            BUSINESS_FEATURE_NOT_AVAILABLE to 501,
            BUSINESS_OPERATION_EXPIRED to 410,

            // System errors -> 500/502/503
            SYSTEM_UNKNOWN_ERROR to 500,
            SYSTEM_CONFIGURATION_ERROR to 500,
            SYSTEM_SERVICE_UNAVAILABLE to 503,
            SYSTEM_DATABASE_ERROR to 500,
            SYSTEM_SERIALIZATION_ERROR to 500,
            SYSTEM_EXTERNAL_SERVICE_ERROR to 502,
            SYSTEM_INTERNAL_ERROR to 500
        )
        /**
         * Gets an ErrorCode from a numeric code value.
         * Returns [SYSTEM_UNKNOWN_ERROR] if no matching code is found.
         *
         * @param code The numeric error code
         * @return The matching ErrorCode or SYSTEM_UNKNOWN_ERROR
         */
        fun fromCode(code: Int): ErrorCode {
            return entries.firstOrNull { it.code == code } ?: SYSTEM_UNKNOWN_ERROR
        }

        /**
         * Gets an ErrorCode from an HTTP status code.
         * Returns the most appropriate ErrorCode for the given HTTP status.
         *
         * @param statusCode The HTTP status code
         * @return The matching ErrorCode or SYSTEM_UNKNOWN_ERROR
         */
        fun fromHttpStatus(statusCode: Int): ErrorCode {
            return when (statusCode) {
                400 -> VALIDATION_INVALID_INPUT
                401 -> AUTH_UNAUTHORIZED
                402 -> BUSINESS_INSUFFICIENT_BALANCE
                403 -> AUTH_FORBIDDEN
                404 -> BUSINESS_RESOURCE_NOT_FOUND
                408 -> NETWORK_TIMEOUT
                409 -> BUSINESS_RESOURCE_CONFLICT
                410 -> BUSINESS_OPERATION_EXPIRED
                423 -> AUTH_ACCOUNT_LOCKED
                429 -> BUSINESS_RATE_LIMIT_EXCEEDED
                500 -> SYSTEM_INTERNAL_ERROR
                502 -> NETWORK_SERVER_ERROR
                503 -> SYSTEM_SERVICE_UNAVAILABLE
                else -> SYSTEM_UNKNOWN_ERROR
            }
        }

        /**
         * Gets all error codes in a specific category.
         *
         * @param category The category prefix (e.g., "NETWORK", "AUTH")
         * @return List of error codes in that category
         */
        fun getByCategory(category: String): List<ErrorCode> {
            val prefix = category.uppercase() + "_"
            return entries.filter { it.name.startsWith(prefix) }
        }

        /**
         * Gets all network error codes (1000-1999).
         */
        fun networkErrors(): List<ErrorCode> = entries.filter { it.isNetworkError() }

        /**
         * Gets all authentication error codes (2000-2999).
         */
        fun authErrors(): List<ErrorCode> = entries.filter { it.isAuthError() }

        /**
         * Gets all validation error codes (3000-3999).
         */
        fun validationErrors(): List<ErrorCode> = entries.filter { it.isValidationError() }

        /**
         * Gets all business error codes (4000-4999).
         */
        fun businessErrors(): List<ErrorCode> = entries.filter { it.isBusinessError() }

        /**
         * Gets all system error codes (5000-5999).
         */
        fun systemErrors(): List<ErrorCode> = entries.filter { it.isSystemError() }
    }
}
