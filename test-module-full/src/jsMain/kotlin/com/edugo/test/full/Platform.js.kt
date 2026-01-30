package com.edugo.test.full

actual class Platform actual constructor() {
    actual val name: String = "JavaScript"
}

actual fun getPlatformName(): String = "JavaScript"
