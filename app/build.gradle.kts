plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
//    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services") version "4.4.2" apply false
    kotlin("kapt")
}

android {
    namespace = "com.example.travelupa"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.travelupa"
        minSdk = 23
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
}

dependencies {

    implementation ("androidx.compose.ui:ui:1.6.0")
    implementation ("androidx.compose.material:material:1.6.0")
    implementation ("androidx.compose.material:material-icons-extended:1.5.4")
    implementation ("androidx.compose.ui:ui-tooling-preview:1.6.0")

    implementation ("androidx.activity:activity-compose:1.7.2")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.androidx.compose.foundation)

    debugImplementation ("androidx.compose.ui:ui-tooling:1.6.0")
    implementation("io.coil-kt:coil-compose:2.4.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation (platform("com.google.firebase:firebase-bom:33.6.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("androidx.navigation:navigation-compose:2.5.3")
    implementation("androidx.room:room-runtime:2.5.0")
    kapt("androidx.room:room-compiler:2.5.0")
    implementation("androidx.room:room-ktx:2.5.0")
    implementation("androidx.camera:camera-core:1.1.0")
    implementation("androidx.camera:camera-camera2:1.1.0")
    implementation("androidx.camera:camera-lifecycle:1.1.0")
    implementation("androidx.camera:camera-view:1.0.0-alpha30")
    implementation("androidx.camera:camera-extensions:1.0.0-alpha30")
}

apply(plugin = "com.google.gms.google-services")