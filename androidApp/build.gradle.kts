import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
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

    buildFeatures {
        compose = true
    }

    // Phase 2 deps (Koog → Ktor, Netty, OkHttp, AWS SDK) ship overlapping JVM-only metadata
    // that Android's runtime ignores. Drop them so packaging merges cleanly.
    packaging {
        resources {
            excludes += setOf(
                "/META-INF/INDEX.LIST",
                "/META-INF/io.netty.versions.properties",
                "/META-INF/versions/9/OSGI-INF/MANIFEST.MF",
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE*",
                "/META-INF/NOTICE*",
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
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

    // Phase 2 — agent + secure storage + scheduling
    implementation(libs.koog.agents)              // Koog single artifact (Gemini client + MCP client)
    implementation(libs.androidx.security.crypto) // Kept for the one-shot ESP→DataStore migration read; remove in a follow-up once verified.
    implementation(libs.androidx.work.runtime.ktx)

    // Secure storage = Preferences DataStore for storage + Tink AEAD for per-value encryption.
    // Replaces the (deprecated) androidx.security EncryptedSharedPreferences.
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.tink.android)

    // Phase 2 step 2 — Notion REST client (interim until hosted MCP grows token support)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
}
