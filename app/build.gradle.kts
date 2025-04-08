import java.util.Properties
import java.io.FileInputStream
import java.io.IOException


plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.ksp)
}

fun getApiKeyFromLocalProperties(project: Project): String{
  val properties = Properties()
  val file = project.rootProject.file("local.properties")
  if (file.exists() && file.isFile) {
    try {
      properties.load(FileInputStream(file))
      return properties.getProperty("AVIATIONSTACK_API_KEY", "") // Return empty string if key not found
    } catch (e: IOException) {
      project.logger.warn("Could not read local.properties file.", e)
    }
  } else {
    project.logger.warn("local.properties file not found in project root: ${file.absolutePath}")
  }
  return "" // Return empty if file doesn't exist or error reading

}

android {
  namespace = "com.example.flighttracker"
  compileSdk = 35


  defaultConfig {
    applicationId = "com.example.flighttracker"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    val apikey = getApiKeyFromLocalProperties(project)
    if(apikey.isEmpty()){
      logger.error("AVIATIONSTACK_API_KEY not found in local.properties. Please create the file and add the key.")
    }
    buildConfigField("String", "AVIATIONSTACK_API_KEY", apikey)


  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    isCoreLibraryDesugaringEnabled = true
  }
  kotlinOptions {
    jvmTarget = "11"
  }
  buildFeatures {
    buildConfig = true
    compose = true
  }
}

dependencies {

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Compose UI Toolkit
  implementation(platform(libs.androidx.compose.bom)) // BOM for consistent versions
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview) // For Composable previews
  implementation(libs.androidx.material3)         // Material Design 3 components

  // ViewModel & Lifecycle Compose Integration
  implementation(libs.androidx.lifecycle.viewmodel.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.lifecycle.runtime.compose) // For collectAsStateWithLifecycle

  // Networking (Retrofit & OkHttp Interceptor)
  implementation(libs.retrofit.core)
  implementation(libs.retrofit.converter.moshi)       // Moshi converter for Retrofit
  implementation(libs.okhttp.logging.interceptor)    // For logging network requests/responses

  // JSON Parsing (Moshi - Runtime & KSP Codegen)
  implementation(libs.moshi.core)                    // Core Moshi runtime library
  implementation(libs.moshi.adapters)                 // Standard built-in adapters
  implementation(libs.moshi.kotlin)
  implementation(libs.work.runtime.ktx)
  ksp(libs.moshi.kotlin.codegen)                     // Moshi codegen Annotation Processor (used by KSP)

  // Testing Dependencies
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom)) // BOM for Compose testing libraries
  androidTestImplementation(libs.androidx.ui.test.junit4)         // Compose testing utilities

  // Debug Dependencies (for Previews, Inspector, etc.)
  debugImplementation(libs.androidx.ui.tooling)
  debugImplementation(libs.androidx.ui.test.manifest)

  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx) // Coroutine support for Room
  // Use ksp for Room annotation processor (matches Moshi setup)
  ksp(libs.androidx.room.compiler)

  coreLibraryDesugaring(libs.android.desugar)

}