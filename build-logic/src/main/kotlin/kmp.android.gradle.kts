import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

// Acceso al version catalog desde included build
val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    androidTarget {
        compilations.all {
            compilerOptions.configure {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }
    }

    // JVM Desktop target - nombre específico para distinguir de otros JVM targets
    jvm("desktop") {
        compilations.all {
            compilerOptions.configure {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }
    }

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
        val androidMain by getting {
            dependencies {
                // Ktor engine para Android (OkHttp)
                implementation(libs.findLibrary("ktor-client-okhttp").get())

                // Coroutines con Dispatchers.Main para UI
                implementation(libs.findLibrary("kotlinx-coroutines-android").get())
            }
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.findLibrary("mockk-android").get())
                implementation(libs.findLibrary("kotlin-test-junit").get())
            }
        }
        val desktopMain by getting {
            dependencies {
                // Ktor engine para JVM Desktop (CIO - Coroutine I/O)
                implementation(libs.findLibrary("ktor-client-cio").get())

                // Coroutines con Dispatchers.Swing para Desktop GUI
                implementation(libs.findLibrary("kotlinx-coroutines-swing").get())
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(libs.findLibrary("mockk").get())
                implementation(libs.findLibrary("kotlin-test-junit").get())
            }
        }
    }

    jvmToolchain(17)
}

// Configuración Android
configure<LibraryExtension> {
    compileSdk = 35

    defaultConfig {
        minSdk = 29  // NNAPI 1.2, Scoped Storage, TLS 1.3
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Namespace se debe configurar en cada módulo
    // namespace = "com.edugo.module"
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
