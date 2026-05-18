@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

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

    // wasmJs browser target. `:shared` is a library; we don't call `binaries.executable()`.
    wasmJs {
        outputModuleName = "shared"
        browser {
            // No webpack/karma overrides at the shared level — :webApp owns those.
        }
    }

    // js(IR) browser target. Produces a regular JS library + auto-generated .d.ts
    // consumed by the Astro+React frontend as an npm dependency.
    //
    // - `binaries.library()`: emit a library (no executable bundle), so downstream
    //   bundlers (Astro/Vite) can tree-shake and inline as needed.
    // - `generateTypeScriptDefinitions()`: write `.d.ts` next to the `.js` for
    //   `@JsExport`-annotated declarations under `it.mensa.web.*`.
    // - `outputModuleName = "shared"`: pin the emitted JS module/package name
    //   so the consumer imports a predictable `shared` artifact.
    js(IR) {
        outputModuleName = "shared"
        browser {
            // Astro owns webpack/vite config; nothing to override at shared level.
        }
        binaries.library()
        generateTypeScriptDefinitions()
        // Emit ES modules instead of UMD. UMD's sibling-file resolution doesn't
        // work cleanly under Vite/bun symlinked file-deps; ESM resolves via
        // standard import statements that bundlers handle natively.
        useEsModules()
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
            // EncryptedSharedPreferences on Android, localStorage on web).
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

        val wasmJsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
                implementation(libs.sqldelight.web.worker.driver)
                implementation(libs.kotlinx.browser)
                // Koin core + kotlinx-* are already in commonMain and resolve to -wasm-js variants.
            }
        }

        // js(IR) source set. Notes vs wasmJs:
        //  - `ktor-client-js` and `sqldelight-web-worker-driver` publish a -js
        //    variant; we reuse the same aliases.
        //  - `kotlinx-browser:0.3` is wasm-only (no js variant published). On
        //    js(IR) the equivalent symbols (`kotlinx.browser.localStorage`,
        //    `org.w3c.dom.*`) ship in `kotlin-stdlib-js` itself, so no extra
        //    dependency is needed.
        //  - Koin core + kotlinx-* in commonMain resolve to -js variants
        //    automatically via Gradle metadata.
        val jsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
                implementation(libs.sqldelight.web.worker.driver)
            }
        }
    }
}

android {
    namespace = "it.mensa.shared"
    compileSdk = 34
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
            // Required for the wasmJs WebWorkerDriver. Forces SQLDelight to generate
            // `suspend awaitAs*()` query APIs instead of blocking `executeAs*()` ones,
            // plus suspend `transaction { }` and suspend mutators (insertOrReplace, etc.).
            // All call sites on Android/iOS must use the suspend form.
            generateAsync.set(true)
        }
    }
}

