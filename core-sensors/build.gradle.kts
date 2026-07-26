plugins { id("com.android.library"); id("org.jetbrains.kotlin.android") }
android { namespace = "com.racelink.controller.core.sensors"; compileSdk = 35; defaultConfig { minSdk = 26 }; compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 } }
kotlin { jvmToolchain(17) }
dependencies { implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1") }
