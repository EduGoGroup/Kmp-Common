plugins {
    id("kmp.android")
    id("kover")
}

android {
    namespace = "com.edugo.test.module"
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Dependencias de prueba
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
            }
        }
    }
}
