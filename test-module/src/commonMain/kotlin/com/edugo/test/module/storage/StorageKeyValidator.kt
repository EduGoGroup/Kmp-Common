package com.edugo.test.module.storage

import com.edugo.test.module.core.AppError
import com.edugo.test.module.core.ErrorCode
import com.edugo.test.module.core.Result

/**
 * Validator for storage keys.
 *
 * Valid keys contain only: letters (a-z, A-Z), numbers (0-9), underscores (_),
 * dashes (-), and dots (.).
 *
 * Examples of valid keys:
 * - "user.name"
 * - "app_config"
 * - "cache-v2"
 * - "settings.theme.dark"
 *
 * Examples of invalid keys:
 * - "" (empty)
 * - "   " (blank)
 * - "user name" (contains space)
 * - "user@email" (contains @)
 * - "path/to/key" (contains /)
 */
public object StorageKeyValidator {

    /**
     * Regex pattern for valid storage keys.
     * Allows: a-z, A-Z, 0-9, underscore, dash, dot.
     */
    private val VALID_KEY_REGEX = Regex("^[a-zA-Z0-9_.\\-]+$")

    /**
     * Maximum allowed length for a storage key.
     */
    public const val MAX_KEY_LENGTH: Int = 256

    /**
     * Validates a storage key.
     *
     * @param key The key to validate
     * @return Result.Success with the key if valid, Result.Failure with error message if invalid
     */
    public fun validate(key: String): Result<String> {
        return when {
            key.isBlank() -> Result.Failure(
                AppError.fromCode(
                    code = ErrorCode.STORAGE_INVALID_KEY,
                    customMessage = "Storage key cannot be empty or blank"
                ).message
            )

            key.length > MAX_KEY_LENGTH -> Result.Failure(
                AppError.fromCode(
                    code = ErrorCode.STORAGE_INVALID_KEY,
                    customMessage = "Storage key exceeds maximum length of $MAX_KEY_LENGTH characters"
                ).message
            )

            !VALID_KEY_REGEX.matches(key) -> Result.Failure(
                AppError.fromCode(
                    code = ErrorCode.STORAGE_INVALID_KEY,
                    customMessage = "Storage key contains invalid characters. Only alphanumeric, dots, dashes and underscores are allowed: '$key'"
                ).message
            )

            else -> Result.Success(key)
        }
    }

    /**
     * Validates a key and throws an exception if invalid.
     *
     * @param key The key to validate
     * @return The validated key
     * @throws IllegalArgumentException if the key is invalid
     */
    public fun requireValid(key: String): String {
        return when (val result = validate(key)) {
            is Result.Success -> result.data
            is Result.Failure -> throw IllegalArgumentException(result.error)
            is Result.Loading -> throw IllegalStateException("Unexpected loading state")
        }
    }

    /**
     * Checks if a key is valid without throwing exceptions.
     *
     * @param key The key to check
     * @return true if the key is valid, false otherwise
     */
    public fun isValid(key: String): Boolean {
        return validate(key) is Result.Success
    }

    /**
     * Sanitizes a key by replacing invalid characters with underscores.
     *
     * This method:
     * 1. Trims leading/trailing whitespace
     * 2. Replaces invalid characters with underscores
     * 3. Truncates to MAX_KEY_LENGTH if necessary
     *
     * @param key The key to sanitize
     * @return A sanitized version of the key
     */
    public fun sanitize(key: String): String {
        return key
            .trim()
            .replace(Regex("[^a-zA-Z0-9_.\\-]"), "_")
            .take(MAX_KEY_LENGTH)
    }
}
