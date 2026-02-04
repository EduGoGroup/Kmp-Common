package com.edugo.test.module

public actual class Platform actual constructor() {
    actual val name: String = "JS"
}

public actual fun getPlatformName(): String = "JS"
