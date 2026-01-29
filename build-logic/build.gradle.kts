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
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.android.gradle.plugin)
}

// Configuración de Java toolchain
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "kmp.library"
            implementationClass = "KmpLibraryPlugin"
        }
        register("kmpAndroid") {
            id = "kmp.android"
            implementationClass = "KmpAndroidPlugin"
        }
    }
}
