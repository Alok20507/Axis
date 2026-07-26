plugins { id("com.android.library"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android { namespace = "com.racelink.controller.feature.settings"; compileSdk = 35; defaultConfig { minSdk = 26 }; buildFeatures { compose = true }; compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 } }
kotlin { jvmToolchain(17) }
dependencies { implementation(project(":core-ui")) }
