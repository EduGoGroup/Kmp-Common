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

    // JVM también para tests
    jvm {
        compilations.all {
            compilerOptions.configure {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }
    }

    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.findLibrary("kotlinx-coroutines-test").get())
                implementation(libs.findLibrary("turbine").get())
            }
        }
        val androidMain by getting
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.findLibrary("mockk-android").get())
                implementation(libs.findLibrary("kotlin-test-junit").get())
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
