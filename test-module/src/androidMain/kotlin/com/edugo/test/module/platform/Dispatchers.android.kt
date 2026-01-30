package com.edugo.test.module.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Android implementation of AppDispatchers.
 */
actual object AppDispatchers {
    actual val Main: CoroutineDispatcher = Dispatchers.Main

    actual val IO: CoroutineDispatcher = Dispatchers.IO

    actual val Default: CoroutineDispatcher = Dispatchers.Default
}
