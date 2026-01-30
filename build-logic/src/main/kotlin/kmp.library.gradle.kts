import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
}

// Acceso al version catalog desde included build
val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    // JVM Target
    jvm {
        compilations.all {
            compilerOptions.configure {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }
    }

    // Source Sets comunes
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Ktor Client
                implementation(libs.findLibrary("ktor-client-core").get())
                implementation(libs.findLibrary("ktor-client-content-negotiation").get())
                implementation(libs.findLibrary("ktor-serialization-kotlinx-json").get())
                implementation(libs.findLibrary("ktor-client-logging").get())

                // Serialization
                implementation(libs.findLibrary("kotlinx-serialization-core").get())
                implementation(libs.findLibrary("kotlinx-serialization-json").get())

                // Coroutines
                implementation(libs.findLibrary("kotlinx-coroutines-core").get())
            }
        }
        val commonTest by getting {
            dependencies {
                // Testing framework multiplataforma
                implementation(kotlin("test"))

                // Ktor mock para testing de network
                implementation(libs.findLibrary("ktor-client-mock").get())

                // Coroutines test utilities
                implementation(libs.findLibrary("kotlinx-coroutines-test").get())

                // Flow testing
                implementation(libs.findLibrary("turbine").get())
            }
        }
        val jvmMain by getting {
            dependencies {
                // Ktor engine para JVM (CIO - Coroutine I/O)
                implementation(libs.findLibrary("ktor-client-cio").get())

                // Coroutines con Dispatchers.Swing para Desktop GUI
                implementation(libs.findLibrary("kotlinx-coroutines-swing").get())
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.findLibrary("mockk").get())
                implementation(libs.findLibrary("kotlin-test-junit").get())
            }
        }
    }

    // Configuración del toolchain
    jvmToolchain(17)
}

// Opciones de compilador comunes
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xcontext-receivers",
            "-opt-in=kotlin.RequiresOptIn"
        )
    }
}
