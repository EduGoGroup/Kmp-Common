plugins {
    id("kmp.full")
    id("kover")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Dependencias de prueba
            }
        }
    }
}
