package com.edugo.test.full

actual class Platform actual constructor() {
    actual val name: String = "Desktop"
}

actual fun getPlatformName(): String = "Desktop"
