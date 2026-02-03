package com.edugo.test.module

public actual class Platform actual constructor() {
    actual val name: String = "Android"
}

public actual fun getPlatformName(): String = "Android"
