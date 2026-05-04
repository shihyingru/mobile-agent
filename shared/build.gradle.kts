plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget()

    // iOS target intentionally not configured.
    // Project uses KMP structure for future portability, but Android is the only
    // active target. Add iosX64(), iosArm64(), iosSimulatorArm64() here when ready.

    sourceSets {
        val commonMain by getting
        val androidMain by getting
    }
}

android {
    namespace = "com.luna.morningagent.shared"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
}
