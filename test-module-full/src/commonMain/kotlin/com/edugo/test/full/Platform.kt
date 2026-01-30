package com.edugo.test.full

expect class Platform() {
    val name: String
}

expect fun getPlatformName(): String
