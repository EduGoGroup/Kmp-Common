package com.edugo.test.module

public expect class Platform() {
    val name: String
}

public expect fun getPlatformName(): String
