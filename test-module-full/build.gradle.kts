plugins {
    id("kmp.full")
    id("kover")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Dependencias de prueba

                // Logging - Kermit 2.0.4
                implementation(libs.kermit)
            }
        }
    }
}
