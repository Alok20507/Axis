plugins { id("com.android.library"); id("org.jetbrains.kotlin.android") }
android { namespace = "com.racelink.controller.core.storage"; compileSdk = 35; defaultConfig { minSdk = 26 }; compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 } }
kotlin { jvmToolchain(17) }
dependencies { implementation("androidx.datastore:datastore-preferences:1.1.3") }
