import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")

    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun projectSecret(
    name: String,
    default: String = ""
): String {
    return (
            localProperties.getProperty(name)
                ?: System.getenv(name)
                ?: default
            )
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
}

android {
    namespace = "com.example.nirapod"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.nirapod"

        minSdk = 23
        targetSdk = 35

        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "FIREBASE_API_KEY",
            "\"${projectSecret("FIREBASE_API_KEY")}\""
        )

        buildConfigField(
            "String",
            "FIREBASE_APP_ID",
            "\"${projectSecret("FIREBASE_APP_ID")}\""
        )

        buildConfigField(
            "String",
            "FIREBASE_PROJECT_ID",
            "\"${projectSecret("FIREBASE_PROJECT_ID")}\""
        )

        buildConfigField(
            "String",
            "FIREBASE_GCM_SENDER_ID",
            "\"${projectSecret("FIREBASE_GCM_SENDER_ID")}\""
        )

        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${projectSecret("SUPABASE_URL")}\""
        )

        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            "\"${projectSecret("SUPABASE_ANON_KEY")}\""
        )

        buildConfigField(
            "String",
            "SUPABASE_BUCKET",
            "\"${projectSecret("SUPABASE_BUCKET", "hazard-images")}\""
        )

        buildConfigField(
            "String",
            "GEMINI_MODEL",
            "\"${projectSecret("GEMINI_MODEL", "gemini-3.5-flash-lite")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf(
            "META-INF/AL2.0",
            "META-INF/LGPL2.1",
            "META-INF/LICENSE.md",
            "META-INF/NOTICE.md"
        )
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {

    // =========================================================
    // Android Core and XML UI
    // =========================================================

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.1")

    implementation(
        "com.google.android.material:material:1.12.0"
    )

    implementation(
        "androidx.constraintlayout:constraintlayout:2.2.1"
    )

    implementation(
        "androidx.activity:activity-ktx:1.10.1"
    )

    implementation(
        "androidx.fragment:fragment-ktx:1.8.8"
    )

    implementation(
        "androidx.recyclerview:recyclerview:1.4.0"
    )

    // =========================================================
    // Lifecycle, MVVM and State
    // =========================================================

    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.2"
    )

    implementation(
        "androidx.lifecycle:lifecycle-runtime-ktx:2.9.2"
    )

    // =========================================================
    // Navigation
    // =========================================================

    implementation(
        "androidx.navigation:navigation-fragment-ktx:2.9.8"
    )

    implementation(
        "androidx.navigation:navigation-ui-ktx:2.9.8"
    )

    // =========================================================
    // Firebase
    // =========================================================

    implementation(
        platform("com.google.firebase:firebase-bom:34.16.0")
    )

    implementation(
        "com.google.firebase:firebase-auth"
    )

    implementation(
        "com.google.firebase:firebase-firestore"
    )

    implementation(
        "com.google.firebase:firebase-messaging"
    )

    implementation(
        "com.google.firebase:firebase-ai"
    )

    implementation(
        "com.google.firebase:firebase-appcheck-debug"
    )

    implementation(
        "com.google.firebase:firebase-appcheck-playintegrity"
    )

    // =========================================================
    // Location
    // =========================================================

    implementation(
        "com.google.android.gms:play-services-location:21.3.0"
    )

    // =========================================================
    // CameraX
    // =========================================================

    val cameraXVersion = "1.4.2"

    implementation(
        "androidx.camera:camera-core:$cameraXVersion"
    )

    implementation(
        "androidx.camera:camera-camera2:$cameraXVersion"
    )

    implementation(
        "androidx.camera:camera-lifecycle:$cameraXVersion"
    )

    implementation(
        "androidx.camera:camera-view:$cameraXVersion"
    )

    // Required for CameraX ProcessCameraProvider ListenableFuture
    implementation(
        "com.google.guava:guava:33.4.8-android"
    )

    // =========================================================
    // MapLibre and OpenStreetMap
    // =========================================================

    implementation(
        "org.maplibre.gl:android-sdk:13.0.2"
    )

    // =========================================================
    // Image Loading
    // =========================================================

    implementation(
        "com.github.bumptech.glide:glide:4.16.0"
    )

    // =========================================================
    // Retrofit and Networking
    // =========================================================

    implementation(
        "com.squareup.retrofit2:retrofit:2.11.0"
    )

    implementation(
        "com.squareup.retrofit2:converter-gson:2.11.0"
    )

    implementation(
        "com.squareup.okhttp3:logging-interceptor:4.12.0"
    )

    // =========================================================
    // Coroutines
    // =========================================================

    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2"
    )

    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2"
    )

    // =========================================================
    // Background Work
    // =========================================================

    implementation(
        "androidx.work:work-runtime-ktx:2.10.1"
    )

    implementation(platform("com.google.firebase:firebase-bom:34.17.0"))

    implementation("com.google.firebase:firebase-ai")
    implementation("com.google.firebase:firebase-appcheck-debug")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    implementation("androidx.preference:preference-ktx:1.2.1")

    // =========================================================
    // Testing
    // =========================================================

    testImplementation(
        "junit:junit:4.13.2"
    )

    androidTestImplementation(
        "androidx.test.ext:junit:1.2.1"
    )

    androidTestImplementation(
        "androidx.test.espresso:espresso-core:3.6.1"
    )
}

tasks.register("unitTestClasses") {
    description = "Synthetic task to satisfy IDE requirement for unitTestClasses"
    group = "verification"
}
