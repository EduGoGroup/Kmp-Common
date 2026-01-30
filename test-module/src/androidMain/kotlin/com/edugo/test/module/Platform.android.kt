package com.edugo.test.module

actual class Platform actual constructor() {
    actual val name: String = "Android"
}

actual fun getPlatformName(): String = "Android"
