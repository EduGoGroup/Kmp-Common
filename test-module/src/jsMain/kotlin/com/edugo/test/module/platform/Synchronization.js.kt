package com.edugo.test.module.platform

/**
 * JavaScript implementation of platformSynchronized.
 *
 * No-op implementation since JavaScript is single-threaded.
 * Simply executes the block without synchronization overhead.
 */
public actual inline fun <T> platformSynchronized(lock: Any, block: () -> T): T = block()
