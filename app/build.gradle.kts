//import com.android.build.api.dsl.AaptOptions

plugins {
    id("com.android.application")
//    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.campus"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.campus"
        minSdk = 31
        targetSdk = 35
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
        kotlinCompilerExtensionVersion = "1.5.10" // Ensure compatibility with your Compose version
    }

    androidResources {
        noCompress += "tflite"
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets") // Correct way to include assets
        }
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
//    implementation(libs.androidx.compose.foundation)

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // CameraX
    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")

    // TensorFlow Lite
//    implementation("org.tensorflow:tensorflow-lite:2.9.0") // Core TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite-support:0.5.0") // Support Library
    // implementation("org.tensorflow:tensorflow-lite-task-vision:2.13.0") // Uncomment if needed for vision tasks
    implementation("com.google.mlkit:face-detection:16.1.5")

    implementation ("com.google.firebase:firebase-appcheck:17.0.1")
    implementation ("com.google.firebase:firebase-appcheck-playintegrity:17.0.1")
    implementation (platform("com.google.firebase:firebase-bom:32.7.0"))

    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("com.google.firebase:firebase-firestore-ktx:24.9.1")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.1")
    implementation ("com.google.firebase:firebase-auth-ktx")
    implementation ("com.google.android.gms:play-services-auth:20.7.0")

    implementation("androidx.compose.animation:animation:1.6.1") // use the latest matching your Compose version

    implementation ("com.airbnb.android:lottie-compose:6.0.0")

    implementation("androidx.compose.material:material-icons-extended:1.6.1")
    implementation("com.google.android.gms:play-services-location:21.0.1")


    implementation("androidx.navigation:navigation-compose:2.7.5")

    // CameraX
    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")

    // TensorFlow Lite
//    implementation("org.tensorflow:tensorflow-lite:2.9.0") // Core TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite-support:0.5.0") // Support Library
    // implementation("org.tensorflow:tensorflow-lite-task-vision:2.13.0") // Uncomment if needed for vision tasks
    implementation("com.google.mlkit:face-detection:16.1.5")

    implementation ("com.google.firebase:firebase-appcheck:17.0.1")
    implementation ("com.google.firebase:firebase-appcheck-playintegrity:17.0.1")
    implementation (platform("com.google.firebase:firebase-bom:32.7.0"))

    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("com.google.firebase:firebase-firestore-ktx:24.9.1")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.1")
    implementation ("com.google.firebase:firebase-auth-ktx")
    implementation ("com.google.android.gms:play-services-auth:20.7.0")

    implementation("androidx.compose.animation:animation:1.6.1") // use the latest matching your Compose version

    implementation ("com.airbnb.android:lottie-compose:6.0.0")

    implementation("androidx.compose.material:material-icons-extended:1.6.1")

    implementation ("com.google.android.gms:play-services-location:21.0.1")

    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")
0    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}