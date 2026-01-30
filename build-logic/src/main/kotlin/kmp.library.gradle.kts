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
                // Dependencias comunes se agregarán aquí
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.findLibrary("kotlinx-coroutines-test").get())
                implementation(libs.findLibrary("turbine").get())
            }
        }
        val jvmMain by getting
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
