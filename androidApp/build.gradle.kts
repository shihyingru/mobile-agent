plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.luna.morningagent"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.luna.morningagent"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
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
}

dependencies {
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Google Fonts (Inter + JetBrains Mono — no TTF commits needed)
    implementation(libs.androidx.compose.google.fonts)

    // Material Icons (Settings, ArrowBack, OpenInNew, etc.)
    implementation(libs.androidx.compose.material.icons)

    // Activity + Lifecycle
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Splash Screen API
    implementation(libs.androidx.core.splashscreen)

    // kotlinx.datetime (Instant in data models)
    implementation(libs.kotlinx.datetime)

    // Phase 2 — DECLARED BUT UNUSED IN PHASE 1
    // implementation("ai.koog:koog-agents-jvm:0.7.3")
    // implementation("ai.koog:koog-agents-mcp:0.7.3")
    // implementation("androidx.security:security-crypto:1.1.0-alpha06")
    // implementation("androidx.work:work-runtime-ktx:2.10.0")
}
