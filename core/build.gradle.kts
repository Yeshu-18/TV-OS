plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "com.firelite.core"
  compileSdk = 36

  defaultConfig {
    minSdk = 24
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  implementation(libs.androidx.compose.ui)

  implementation(libs.androidx.core.ktx)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.serialization.json)
}
