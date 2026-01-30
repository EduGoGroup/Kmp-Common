import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
}

// Acceso al version catalog desde included build
val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    // Desktop (JVM) Target con nombre explícito
    jvm("desktop") {
        compilations.all {
            compilerOptions.configure {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }
    }

    // JavaScript Target con IR compiler (modo recomendado para producción)
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        nodejs {
            testTask {
                useMocha()
            }
        }
        // Genera JS ejecutable
        binaries.executable()
    }

    // Source Sets
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

        // Desktop (JVM) source sets
        val desktopMain by getting {
            dependencies {
                // Ktor engine para JVM/Desktop (CIO)
                implementation(libs.findLibrary("ktor-client-cio").get())

                // Coroutines con Dispatchers.Swing
                implementation(libs.findLibrary("kotlinx-coroutines-swing").get())
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(libs.findLibrary("mockk").get())
                implementation(libs.findLibrary("kotlin-test-junit").get())
            }
        }

        // JavaScript source sets
        val jsMain by getting {
            dependencies {
                // Ktor engine para JavaScript (usa fetch API)
                implementation(libs.findLibrary("ktor-client-js").get())
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }

    // Configuración del toolchain para JVM
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
