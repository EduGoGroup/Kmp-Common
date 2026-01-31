plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Dependencias para los plugins que vamos a configurar
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.android.gradle.plugin)
    implementation(libs.kover.gradle.plugin)

    // Plugin de serialización para Kotlin Multiplatform
    implementation(libs.kotlin.serialization.gradle.plugin)
}

// Configuración de Java toolchain
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// Los convention plugins se registran automáticamente desde los archivos .gradle.kts
// No es necesario registrarlos explícitamente con gradlePlugin {}
