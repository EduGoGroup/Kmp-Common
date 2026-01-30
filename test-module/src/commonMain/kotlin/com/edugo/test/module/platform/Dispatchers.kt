package com.edugo.test.module.platform

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Dispatchers especificos por plataforma.
 *
 * En Android: Dispatchers.Main para UI
 * En Desktop: Dispatchers.Default o Swing
 * En JS: Single-threaded dispatcher
 */
expect object AppDispatchers {
    /** Dispatcher para operaciones de UI */
    val Main: CoroutineDispatcher

    /** Dispatcher para operaciones IO */
    val IO: CoroutineDispatcher

    /** Dispatcher para computacion */
    val Default: CoroutineDispatcher
}
