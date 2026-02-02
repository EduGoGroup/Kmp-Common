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
                // Dependencias de prueba
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")

                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.2")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

                // Logging - Kermit 2.0.4
                implementation(libs.kermit)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
            }
        }
    }
}
