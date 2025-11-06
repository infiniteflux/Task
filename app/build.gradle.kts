plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.task"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.task"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

   // implementation("com.github.pedroSG94.RootEncoder:library:2.6.5")

    // dependency for navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // dependencies for third party library.
    implementation("com.github.pedroSG94:RTSP-Server:1.3.6")
    implementation("com.github.pedroSG94.RootEncoder:library:2.6.1")
    // The RTSP module that RTSP-Server depends on
    implementation("com.github.pedroSG94.RootEncoder:rtsp:2.6.1")

    // Accompanist for permissions
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    implementation("androidx.core:core-ktx:1.13.1")

}