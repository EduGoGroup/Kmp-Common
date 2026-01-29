/**
 * EduGo KMP Shared - Settings Configuration
 *
 * Configura el proyecto raíz, repositorios de plugins y version catalog.
 * Gradle 8.11+ | Kotlin 2.1.20
 */

pluginManagement {
    // Convention Plugins locales
    includeBuild("build-logic")

    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Forzar uso centralizado de repositorios
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
    }

    // Version Catalog: gradle/libs.versions.toml (auto-detectado por Gradle 8.11+)
}

rootProject.name = "EduGo-KMP-Modules"

// Habilitar type-safe project accessors
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Build cache local
buildCache {
    local {
        isEnabled = true
        directory = File(rootDir, ".gradle/build-cache")
    }
}

// Incluir módulos cuando existan
// include(":core:common")
// include(":core:network")
// include(":feature:auth")
