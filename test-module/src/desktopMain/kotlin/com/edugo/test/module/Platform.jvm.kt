package com.edugo.test.module

public actual class Platform actual constructor() {
    actual val name: String = "JVM"
}

public actual fun getPlatformName(): String = "JVM"
