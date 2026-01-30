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
    // Preferir settings pero permitir repositorios de proyecto para Kotlin/JS
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    repositories {
        google()
        mavenCentral()

        // Repositorio Node.js para Kotlin/JS
        exclusiveContent {
            forRepository {
                ivy("https://nodejs.org/dist") {
                    patternLayout {
                        artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
                    }
                    metadataSources { artifact() }
                }
            }
            filter { includeModule("org.nodejs", "node") }
        }

        // Repositorio Yarn para Kotlin/JS
        exclusiveContent {
            forRepository {
                ivy("https://github.com/yarnpkg/yarn/releases/download") {
                    patternLayout {
                        artifact("v[revision]/[artifact]-v[revision].[ext]")
                    }
                    metadataSources { artifact() }
                }
            }
            filter { includeModule("com.yarnpkg", "yarn") }
        }
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

// Módulo de prueba temporal para validación de Convention Plugins
include(":test-module")
// Módulo de prueba para full multiplatform (JS + Desktop)
include(":test-module-full")
