/*
 * Copyright (c) 2026 EduGo Project
 * Licensed under the MIT License
 */

package com.edugo.test.module.storage

import com.edugo.test.module.core.Result
import com.edugo.test.module.platform.DefaultLogger
import com.edugo.test.module.platform.TaggedLogger
import com.edugo.test.module.platform.withTag

/**
 * Wrapper over [EduGoStorage] with key validation and robust error handling.
 *
 * This class provides a safe interface to storage operations that:
 * - Validates keys before each operation
 * - Never throws exceptions (returns defaults or [Result] instead)
 * - Logs all errors for debugging
 * - Provides fallback values for failed operations
 *
 * ## Usage
 *
 * ```kotlin
 * val safeStorage = SafeEduGoStorage.create()
 *
 * // Safe operations with validation
 * safeStorage.putStringSafe("user.name", "John")  // Returns Result<Unit>
 * val name = safeStorage.getStringSafe("user.name", "Guest")  // Returns "John" or "Guest" on error
 *
 * // Invalid keys are rejected gracefully
 * val result = safeStorage.putStringSafe("invalid key!", "value")  // Returns Failure
 * val value = safeStorage.getStringSafe("invalid key!", "default")  // Returns "default"
 *
 * // Access underlying storage when needed (bypasses validation)
 * safeStorage.unsafe.putString("any.key", "value")
 * ```
 *
 * @property storage The underlying [EduGoStorage] instance
 * @property validateKeys Whether to validate keys before operations (default: true)
 * @property logger The logger instance for error logging
 */
public class SafeEduGoStorage(
    @PublishedApi internal val storage: EduGoStorage,
    private val validateKeys: Boolean = true,
    @PublishedApi internal val logger: TaggedLogger = DefaultLogger.withTag("EduGo.Storage")
) {

    /**
     * Validates a key and logs a warning if invalid.
     *
     * @param key The key to validate
     * @return true if the key is valid or validation is disabled, false otherwise
     */
    @PublishedApi
    internal fun validateKey(key: String): Boolean {
        if (!validateKeys) return true

        return if (StorageKeyValidator.isValid(key)) {
            true
        } else {
            logger.w("Invalid storage key rejected: '$key'")
            false
        }
    }

    // =========================================================================
    // STRING OPERATIONS
    // =========================================================================

    /**
     * Safely stores a string value.
     *
     * @param key The storage key
     * @param value The value to store
     * @return [Result.Success] if stored, [Result.Failure] if key is invalid or operation failed
     */
    public fun putStringSafe(key: String, value: String): Result<Unit> {
        if (!validateKey(key)) {
            return Result.Failure("Invalid key: $key")
        }
        return StorageErrorHandler.runCatching("putString($key)", Result.Failure("Failed")) {
            storage.putString(key, value)
            Result.Success(Unit)
        }
    }

    /**
     * Safely retrieves a string value.
     *
     * @param key The storage key
     * @param default The default value to return if key is invalid or not found
     * @return The stored value or the default
     */
    public fun getStringSafe(key: String, default: String = ""): String {
        if (!validateKey(key)) return default
        return StorageErrorHandler.runCatching("getString($key)", default) {
            storage.getString(key, default)
        }
    }

    // =========================================================================
    // INT OPERATIONS
    // =========================================================================

    /**
     * Safely stores an integer value.
     *
     * @param key The storage key
     * @param value The value to store
     * @return [Result.Success] if stored, [Result.Failure] if key is invalid or operation failed
     */
    public fun putIntSafe(key: String, value: Int): Result<Unit> {
        if (!validateKey(key)) {
            return Result.Failure("Invalid key: $key")
        }
        return StorageErrorHandler.runCatching("putInt($key)", Result.Failure("Failed")) {
            storage.putInt(key, value)
            Result.Success(Unit)
        }
    }

    /**
     * Safely retrieves an integer value.
     *
     * @param key The storage key
     * @param default The default value to return if key is invalid or not found
     * @return The stored value or the default
     */
    public fun getIntSafe(key: String, default: Int = 0): Int {
        if (!validateKey(key)) return default
        return StorageErrorHandler.runCatching("getInt($key)", default) {
            storage.getInt(key, default)
        }
    }

    // =========================================================================
    // LONG OPERATIONS
    // =========================================================================

    /**
     * Safely stores a long value.
     *
     * @param key The storage key
     * @param value The value to store
     * @return [Result.Success] if stored, [Result.Failure] if key is invalid or operation failed
     */
    public fun putLongSafe(key: String, value: Long): Result<Unit> {
        if (!validateKey(key)) {
            return Result.Failure("Invalid key: $key")
        }
        return StorageErrorHandler.runCatching("putLong($key)", Result.Failure("Failed")) {
            storage.putLong(key, value)
            Result.Success(Unit)
        }
    }

    /**
     * Safely retrieves a long value.
     *
     * @param key The storage key
     * @param default The default value to return if key is invalid or not found
     * @return The stored value or the default
     */
    public fun getLongSafe(key: String, default: Long = 0L): Long {
        if (!validateKey(key)) return default
        return StorageErrorHandler.runCatching("getLong($key)", default) {
            storage.getLong(key, default)
        }
    }

    // =========================================================================
    // BOOLEAN OPERATIONS
    // =========================================================================

    /**
     * Safely stores a boolean value.
     *
     * @param key The storage key
     * @param value The value to store
     * @return [Result.Success] if stored, [Result.Failure] if key is invalid or operation failed
     */
    public fun putBooleanSafe(key: String, value: Boolean): Result<Unit> {
        if (!validateKey(key)) {
            return Result.Failure("Invalid key: $key")
        }
        return StorageErrorHandler.runCatching("putBoolean($key)", Result.Failure("Failed")) {
            storage.putBoolean(key, value)
            Result.Success(Unit)
        }
    }

    /**
     * Safely retrieves a boolean value.
     *
     * @param key The storage key
     * @param default The default value to return if key is invalid or not found
     * @return The stored value or the default
     */
    public fun getBooleanSafe(key: String, default: Boolean = false): Boolean {
        if (!validateKey(key)) return default
        return StorageErrorHandler.runCatching("getBoolean($key)", default) {
            storage.getBoolean(key, default)
        }
    }

    // =========================================================================
    // FLOAT OPERATIONS
    // =========================================================================

    /**
     * Safely stores a float value.
     *
     * @param key The storage key
     * @param value The value to store
     * @return [Result.Success] if stored, [Result.Failure] if key is invalid or operation failed
     */
    public fun putFloatSafe(key: String, value: Float): Result<Unit> {
        if (!validateKey(key)) {
            return Result.Failure("Invalid key: $key")
        }
        return StorageErrorHandler.runCatching("putFloat($key)", Result.Failure("Failed")) {
            storage.putFloat(key, value)
            Result.Success(Unit)
        }
    }

    /**
     * Safely retrieves a float value.
     *
     * @param key The storage key
     * @param default The default value to return if key is invalid or not found
     * @return The stored value or the default
     */
    public fun getFloatSafe(key: String, default: Float = 0f): Float {
        if (!validateKey(key)) return default
        return StorageErrorHandler.runCatching("getFloat($key)", default) {
            storage.getFloat(key, default)
        }
    }

    // =========================================================================
    // DOUBLE OPERATIONS
    // =========================================================================

    /**
     * Safely stores a double value.
     *
     * @param key The storage key
     * @param value The value to store
     * @return [Result.Success] if stored, [Result.Failure] if key is invalid or operation failed
     */
    public fun putDoubleSafe(key: String, value: Double): Result<Unit> {
        if (!validateKey(key)) {
            return Result.Failure("Invalid key: $key")
        }
        return StorageErrorHandler.runCatching("putDouble($key)", Result.Failure("Failed")) {
            storage.putDouble(key, value)
            Result.Success(Unit)
        }
    }

    /**
     * Safely retrieves a double value.
     *
     * @param key The storage key
     * @param default The default value to return if key is invalid or not found
     * @return The stored value or the default
     */
    public fun getDoubleSafe(key: String, default: Double = 0.0): Double {
        if (!validateKey(key)) return default
        return StorageErrorHandler.runCatching("getDouble($key)", default) {
            storage.getDouble(key, default)
        }
    }

    // =========================================================================
    // OBJECT OPERATIONS (Serialization)
    // =========================================================================

    /**
     * Safely stores a serializable object.
     *
     * @param key The storage key
     * @param value The object to store (must be @Serializable)
     * @return [Result.Success] if stored, [Result.Failure] if key is invalid or serialization failed
     */
    public inline fun <reified T> putObjectSafe(key: String, value: T): Result<Unit> {
        if (!validateKey(key)) {
            return Result.Failure("Invalid key: $key")
        }
        return try {
            storage.putObject(key, value)
            Result.Success(Unit)
        } catch (e: Throwable) {
            val error = StorageErrorHandler.handle(e, "putObject($key)")
            Result.Failure(error.message)
        }
    }

    /**
     * Safely retrieves a serializable object.
     *
     * @param key The storage key
     * @param default The default value to return if key is invalid, not found, or deserialization fails
     * @return The stored object or the default
     */
    public inline fun <reified T> getObjectSafe(key: String, default: T? = null): T? {
        if (!validateKey(key)) return default
        return try {
            storage.getObject<T>(key) ?: default
        } catch (e: Throwable) {
            StorageErrorHandler.handle(e, "getObject($key)")
            default
        }
    }

    // =========================================================================
    // GENERAL OPERATIONS
    // =========================================================================

    /**
     * Safely removes a value from storage.
     *
     * @param key The storage key to remove
     * @return [Result.Success] if removed, [Result.Failure] if key is invalid or operation failed
     */
    public fun removeSafe(key: String): Result<Unit> {
        if (!validateKey(key)) {
            return Result.Failure("Invalid key: $key")
        }
        return StorageErrorHandler.runCatching("remove($key)", Result.Failure("Failed")) {
            storage.remove(key)
            Result.Success(Unit)
        }
    }

    /**
     * Safely checks if a key exists in storage.
     *
     * @param key The storage key to check
     * @return true if the key exists, false if it doesn't or the key is invalid
     */
    public fun containsSafe(key: String): Boolean {
        if (!validateKey(key)) return false
        return StorageErrorHandler.runCatching("contains($key)", false) {
            storage.contains(key)
        }
    }

    /**
     * Safely clears all values from storage.
     *
     * @return [Result.Success] if cleared, [Result.Failure] if operation failed
     */
    public fun clearSafe(): Result<Unit> {
        return StorageErrorHandler.runCatching("clear()", Result.Failure("Failed")) {
            storage.clear()
            Result.Success(Unit)
        }
    }

    /**
     * Direct access to the underlying [EduGoStorage] without validation.
     *
     * Use this when you need to bypass key validation, for example when
     * working with keys that use non-standard characters.
     *
     * **Warning:** Operations through `unsafe` do not validate keys or handle errors.
     */
    public val unsafe: EduGoStorage get() = storage

    public companion object {
        /**
         * Creates a [SafeEduGoStorage] with a new default [EduGoStorage].
         *
         * @return A new SafeEduGoStorage instance
         */
        public fun create(): SafeEduGoStorage {
            return SafeEduGoStorage(EduGoStorage.create())
        }

        /**
         * Creates a [SafeEduGoStorage] with a named [EduGoStorage].
         *
         * @param name The name for the storage instance
         * @return A new SafeEduGoStorage instance
         */
        public fun create(name: String): SafeEduGoStorage {
            return SafeEduGoStorage(EduGoStorage.create(name))
        }

        /**
         * Wraps an existing [EduGoStorage] with safe operations.
         *
         * @param storage The storage to wrap
         * @param validateKeys Whether to validate keys (default: true)
         * @return A new SafeEduGoStorage wrapping the provided storage
         */
        public fun wrap(storage: EduGoStorage, validateKeys: Boolean = true): SafeEduGoStorage {
            return SafeEduGoStorage(storage, validateKeys)
        }
    }
}
