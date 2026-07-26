plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android { namespace = "com.racelink.controller.core.ui"; compileSdk = 35; defaultConfig { minSdk = 26 } ; buildFeatures { compose = true }; compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 } }
kotlin { jvmToolchain(17) }
dependencies {
    api(platform("androidx.compose:compose-bom:2025.03.01"))
    api("androidx.compose.material3:material3")
    api("androidx.compose.ui:ui")
    api("androidx.compose.ui:ui-tooling-preview")
}
