pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // PREFER_SETTINGS is required for Kotlin/Wasm+JS targets:
    // the Kotlin Gradle plugin dynamically registers the Node.js distribution repo
    // during configuration. We also declare it explicitly here so Gradle can resolve
    // `org.nodejs:node` from the settings-managed repo list.
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // Node.js distribution — required by the Kotlin/JS + Kotlin/Wasm Gradle plugin.
        ivy {
            name = "Node.js"
            setUrl("https://nodejs.org/dist")
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
            }
            metadataSources { artifact() }
            content { includeModule("org.nodejs", "node") }
        }
        // Yarn package manager — required by Kotlin/JS + Kotlin/Wasm for npm resolution.
        ivy {
            name = "Yarn"
            setUrl("https://github.com/yarnpkg/yarn/releases/download")
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]).[ext]")
            }
            metadataSources { artifact() }
            content { includeModule("com.yarnpkg", "yarn") }
        }
    }
}

rootProject.name = "MensaKMP"

include(":shared")
include(":androidApp")
