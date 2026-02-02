plugins {
    id("kmp.android")
    id("kover")
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
