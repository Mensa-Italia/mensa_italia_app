plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.compose)
    // NOTE: Uncomment after placing google-services.json in this directory
    // alias(libs.plugins.google.services)
}

android {
    namespace = "it.mensa.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "it.mensa.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        // Maps API key placeholder — override in local.properties or via CI secret
        manifestPlaceholders["MAPS_API_KEY"] = project.findProperty("MAPS_API_KEY") ?: ""
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":shared"))

    // KotlinX Serialization JSON (needed to parse JsonObject from shared models)
    implementation(libs.kotlinx.serialization.json)

    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3.window.size)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Activity
    implementation(libs.androidx.activity.compose)

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Media3
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)

    // Maps
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    // Accompanist
    implementation(libs.accompanist.permissions)

    // Firebase (BOM-driven — comment out until google-services.json is present)
    // val firebaseBom = platform(libs.firebase.bom)
    // implementation(firebaseBom)
    // implementation(libs.firebase.messaging.ktx)
    // implementation(libs.firebase.analytics.ktx)

    // Stripe
    implementation(libs.stripe.android)

    // ML Kit barcode & CameraX
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // Google Wallet
    implementation(libs.play.services.pay)

    // Chrome Custom Tabs
    implementation(libs.androidx.browser)

    // ZXing (QR fallback)
    implementation(libs.zxing.core)

    // Gson
    implementation(libs.gson)

    // KotlinX DateTime (needed for Instant, LocalDateTime etc.)
    implementation(libs.kotlinx.datetime)

    // KotlinX Coroutines (Android)
    implementation(libs.kotlinx.coroutines.core)
}
