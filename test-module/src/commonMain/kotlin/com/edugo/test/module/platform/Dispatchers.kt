package com.edugo.test.module.platform

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Provides platform-specific coroutine dispatchers.
 *
 * This object abstracts the differences in threading and dispatching across platforms,
 * providing a unified API for coroutine execution contexts.
 *
 * ## Usage Guidelines:
 * - Use **Main** for UI updates and lightweight operations
 * - Use **IO** for network requests, disk operations, and database queries
 * - Use **Default** for CPU-intensive computations
 *
 * ## Platform-specific implementations:
 *
 * ### Android:
 * - `Main` → `Dispatchers.Main` (Main/UI thread)
 * - `IO` → `Dispatchers.IO` (Optimized for blocking I/O)
 * - `Default` → `Dispatchers.Default` (Shared thread pool)
 *
 * ### JVM/Desktop:
 * - `Main` → `Dispatchers.Swing` or custom EDT dispatcher
 * - `IO` → `Dispatchers.IO`
 * - `Default` → `Dispatchers.Default`
 *
 * ### iOS:
 * - `Main` → Main dispatch queue (UI-safe)
 * - `IO` → Background dispatch queue
 * - `Default` → Background dispatch queue
 *
 * @see [Kotlin Coroutines Dispatchers](https://kotlinlang.org/docs/coroutine-context-and-dispatchers.html)
 */
expect object AppDispatchers {
    /**
     * Dispatcher confined to the Main thread (UI thread).
     *
     * **Use for:**
     * - Updating UI components
     * - Lightweight, quick operations
     * - Interacting with UI frameworks
     *
     * **⚠️ Important**: Never perform blocking operations on this dispatcher.
     * Blocking the main thread will freeze the UI.
     *
     * Example:
     * ```kotlin
     * withContext(AppDispatchers.Main) {
     *     textView.text = "Updated from coroutine"
     * }
     * ```
     *
     * **Platform notes:**
     * - Android: Maps to main looper thread
     * - iOS: Maps to main dispatch queue
     * - Desktop: Maps to Swing EDT or custom UI thread
     */
    val Main: CoroutineDispatcher

    /**
     * Dispatcher optimized for offloading blocking IO operations.
     *
     * **Use for:**
     * - Network requests (HTTP clients)
     * - File system operations (read/write)
     * - Database queries
     * - Any blocking I/O operations
     *
     * **Characteristics:**
     * - Backed by a shared pool of threads
     * - Can grow to accommodate blocking operations
     * - Optimized for blocking tasks
     *
     * Example:
     * ```kotlin
     * val data = withContext(AppDispatchers.IO) {
     *     httpClient.get("https://api.example.com/data")
     * }
     * ```
     *
     * **Platform notes:**
     * - Android/JVM: Uses `Dispatchers.IO` (64 thread pool)
     * - iOS: Background dispatch queue
     */
    val IO: CoroutineDispatcher

    /**
     * Dispatcher optimized for CPU-intensive operations.
     *
     * **Use for:**
     * - Heavy computations
     * - JSON parsing (large payloads)
     * - Image processing
     * - Sorting/filtering large collections
     * - Cryptographic operations
     *
     * **Characteristics:**
     * - Limited to number of CPU cores
     * - Optimized for CPU-bound tasks
     * - Shared with other coroutines
     *
     * Example:
     * ```kotlin
     * val result = withContext(AppDispatchers.Default) {
     *     largeList.filter { it.matches(complexPredicate) }
     *               .sortedBy { it.priority }
     * }
     * ```
     *
     * **Platform notes:**
     * - Thread pool size = number of CPU cores (minimum 2)
     * - Shared across all Default dispatcher users
     */
    val Default: CoroutineDispatcher
}
