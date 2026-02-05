package com.edugo.test.module.storage

import com.edugo.test.module.core.AppError
import com.edugo.test.module.core.ErrorCode
import com.edugo.test.module.platform.DefaultLogger
import com.edugo.test.module.platform.withTag

/**
 * Base exception class for storage-related errors.
 *
 * This sealed class hierarchy provides type-safe, specific exceptions for different
 * storage failure scenarios, each mapped to the appropriate [ErrorCode].
 *
 * ## Usage:
 * ```kotlin
 * try {
 *     storage.putObject("key", data)
 * } catch (e: StorageException) {
 *     when (e) {
 *         is StorageException.InvalidKey -> handleInvalidKey(e)
 *         is StorageException.SerializationFailed -> handleSerializationError(e)
 *         is StorageException.DeserializationFailed -> handleDeserializationError(e)
 *         is StorageException.DataCorrupted -> handleCorruptedData(e)
 *         is StorageException.OperationFailed -> handleOperationError(e)
 *     }
 * }
 * ```
 *
 * @property errorCode The specific [ErrorCode] for this storage error
 */
public sealed class StorageException(
    message: String,
    public val errorCode: ErrorCode,
    cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * Thrown when a storage key has an invalid format.
     *
     * @param key The invalid key that was provided
     */
    public class InvalidKey(
        public val key: String
    ) : StorageException(
        message = "Invalid storage key: '$key'",
        errorCode = ErrorCode.STORAGE_INVALID_KEY
    )

    /**
     * Thrown when serialization of a value fails.
     *
     * @param key The key for which serialization failed
     * @param cause The underlying serialization exception
     */
    public class SerializationFailed(
        public val key: String,
        cause: Throwable
    ) : StorageException(
        message = "Failed to serialize value for key '$key'",
        errorCode = ErrorCode.STORAGE_SERIALIZATION_ERROR,
        cause = cause
    )

    /**
     * Thrown when deserialization of stored data fails.
     *
     * @param key The key for which deserialization failed
     * @param cause The underlying deserialization exception
     */
    public class DeserializationFailed(
        public val key: String,
        cause: Throwable
    ) : StorageException(
        message = "Failed to deserialize value for key '$key'",
        errorCode = ErrorCode.STORAGE_DESERIALIZATION_ERROR,
        cause = cause
    )

    /**
     * Thrown when stored data is corrupted or malformed.
     *
     * @param key The key with corrupted data
     * @param details Optional details about the corruption
     */
    public class DataCorrupted(
        public val key: String,
        public val details: String? = null
    ) : StorageException(
        message = "Data corrupted for key '$key'${details?.let { ": $it" } ?: ""}",
        errorCode = ErrorCode.STORAGE_DATA_CORRUPTED
    )

    /**
     * Thrown when a general storage operation fails.
     *
     * @param operation The name of the operation that failed
     * @param cause The underlying exception
     */
    public class OperationFailed(
        public val operation: String,
        cause: Throwable
    ) : StorageException(
        message = "Storage operation '$operation' failed",
        errorCode = ErrorCode.STORAGE_OPERATION_FAILED,
        cause = cause
    )

    /**
     * Converts this exception to an [AppError] for consistent error handling.
     *
     * @return An [AppError] with the appropriate error code and message
     */
    public fun toAppError(): AppError = AppError(
        code = errorCode,
        message = message ?: errorCode.description,
        cause = cause
    )
}

/**
 * Centralized error handler for storage operations with integrated logging.
 *
 * This object provides consistent error handling, logging, and conversion
 * of exceptions to [AppError] instances.
 *
 * ## Usage:
 * ```kotlin
 * // Handle and log an exception
 * val appError = StorageErrorHandler.handle(exception, "getObject(user.settings)")
 *
 * // Execute with automatic error handling and fallback
 * val value = StorageErrorHandler.runCatching("getValue", defaultValue) {
 *     storage.getString("key", "")
 * }
 * ```
 */
public object StorageErrorHandler {

    private val logger = DefaultLogger.withTag("EduGo.Storage")

    /**
     * Handles an exception by logging it and converting to [AppError].
     *
     * @param exception The exception to handle
     * @param context Optional context string for logging (e.g., operation name)
     * @return An [AppError] representing the exception
     */
    public fun handle(exception: Throwable, context: String = ""): AppError {
        val contextInfo = if (context.isNotEmpty()) " [$context]" else ""

        return when (exception) {
            is StorageException -> {
                logger.e("Storage error$contextInfo: ${exception.message}", exception)
                exception.toAppError()
            }

            is kotlinx.serialization.SerializationException -> {
                logger.e("Serialization error$contextInfo: ${exception.message}", exception)
                AppError(
                    code = ErrorCode.STORAGE_SERIALIZATION_ERROR,
                    message = exception.message ?: "Serialization failed",
                    cause = exception
                )
            }

            else -> {
                logger.e("Unexpected storage error$contextInfo: ${exception.message}", exception)
                AppError(
                    code = ErrorCode.STORAGE_OPERATION_FAILED,
                    message = exception.message ?: "Storage operation failed",
                    cause = exception
                )
            }
        }
    }

    /**
     * Executes an operation with automatic error handling and fallback.
     *
     * If the operation throws an exception, it is logged and the fallback value is returned.
     *
     * @param context Context string for logging
     * @param fallback The value to return if the operation fails
     * @param operation The operation to execute
     * @return The result of the operation, or the fallback value if it fails
     */
    public inline fun <T> runCatching(
        context: String = "",
        fallback: T,
        operation: () -> T
    ): T {
        return try {
            operation()
        } catch (e: Throwable) {
            handle(e, context)
            fallback
        }
    }
}
