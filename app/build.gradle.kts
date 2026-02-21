import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.gms.google-services")
}

// Load MAPTILER_API_KEY from local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
    namespace = "com.example.parkkar"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.parkkar"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ✅ Safely load API keys
        val mapTilerApiKey = localProperties.getProperty("MAPTILER_API_KEY") ?: ""
        val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY") ?: ""

        buildConfigField("String", "MAPTILER_API_KEY", "\"$mapTilerApiKey\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")

        // ✅ Use stable, compatible MapTiler style (v1 instead of v2-dark)
        buildConfigField(
            "String",
            "MAP_STYLE_URL",
            "\"https://api.maptiler.com/maps/streets-v4-dark/style.json?key=$mapTilerApiKey\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    implementation(libs.kotlinx.serialization.json)

    // ✅ Use latest stable MapLibre SDK
    implementation("org.maplibre.gl:android-sdk:11.5.0")
    implementation("org.maplibre.gl:android-plugin-annotation-v9:3.0.0")
    implementation("com.mapbox.mapboxsdk:mapbox-android-gestures:0.7.0")
    implementation("com.mapbox.mapboxsdk:mapbox-sdk-geojson:5.8.0")

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation(libs.firebase.auth.ktx)

    // Google Sign-In + Location
    implementation(libs.play.services.auth)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // ✅ Ktor Client (OkHttp)
    implementation("io.ktor:ktor-client-core:2.3.10")
    implementation("io.ktor:ktor-client-okhttp:2.3.10")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.10")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.10")

    // JSON Serialization
    implementation("com.google.code.gson:gson:2.10.1")

    // Google AI (Gemini)
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // QR Code Generator
    implementation("com.google.zxing:core:3.5.3")

    // Jetpack DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
