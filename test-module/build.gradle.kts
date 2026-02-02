/*
 * Copyright (c) 2026 EduGo Project
 * Licensed under the MIT License
 */

plugins {
    id("kmp.android")
    id("kover")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.edugo.test.module"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                // DateTime utilities
                implementation(libs.kotlinx.datetime)

                // Serialization
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.serialization.json)

                // Logging
                implementation(libs.kermit)
            }
        }
        val commonTest by getting {
            dependencies {
                // Coroutines test utilities
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

// Configure explicit API mode for library projects
kotlin.explicitApi()
