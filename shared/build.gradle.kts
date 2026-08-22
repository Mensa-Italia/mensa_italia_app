import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget()

    val xcf = XCFramework("Shared")

    applyDefaultHierarchyTemplate()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
        watchosArm64(),
        watchosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
            xcf.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines.extensions)
            // Required by generateAsync=true mode: `awaitAs*` query extensions and
            // `Schema.synchronous()` adapter used by iOS/Android driver factories.
            implementation(libs.sqldelight.async.extensions)
            // Cross-platform key/value with secure backends (Keychain on iOS,
            // EncryptedSharedPreferences on Android).
            implementation(libs.multiplatform.settings)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.ktor.client.mock)
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.sqldelight.sqlite.driver)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.mock)
            }
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.androidx.security.crypto)
            implementation(libs.koin.android.main)
        }

        // Darwin-common deps (iOS + watchOS). With applyDefaultHierarchyTemplate(),
        // `appleMain` is the parent of `iosMain` and `watchosMain`, so both inherit.
        appleMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }
    }
}

android {
    namespace = "it.mensa.shared"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("MensaDatabase") {
            packageName.set("it.mensa.shared.db")
            // `generateAsync` resta acceso: tutte le call site di Android e iOS
            // usano la forma suspend (`awaitAs*`, `suspend transaction { }`).
            // Spegnerlo ora vorrebbe dire riscriverle tutte, senza guadagnarci
            // niente.
            generateAsync.set(true)
        }
    }
}
