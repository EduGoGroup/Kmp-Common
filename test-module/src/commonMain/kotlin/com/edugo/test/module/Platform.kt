package com.edugo.test.module

expect class Platform() {
    val name: String
}

expect fun getPlatformName(): String
