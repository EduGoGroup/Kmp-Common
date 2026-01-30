package com.edugo.test.module

actual class Platform actual constructor() {
    actual val name: String = "JVM"
}

actual fun getPlatformName(): String = "JVM"
