// =====================================================================
// build.gradle.kts (Module :app)
// Tempelkan blok "dependencies" di bawah ini ke file build.gradle
// module app pada proyek Android Studio Anda (bukan build.gradle
// tingkat project). Pastikan juga plugin Compose sudah aktif.
// =====================================================================

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.julian.absenpro"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.julian.absenpro"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true // wajib diaktifkan agar Jetpack Compose bisa dipakai
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14" // sesuaikan dgn versi Kotlin
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    // ---- Jetpack Compose (UI deklaratif) ----
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    // Ikon Material (LocationOn, Person, ArrowBack, dll) dipakai bottom nav
    // & app bar. Ditambah eksplisit agar pasti tersedia (tidak selalu ikut
    // transitif dari material3). Versi diambil dari Compose BOM di atas.
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ---- Networking: Retrofit2 + Gson Converter ----
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0") // untuk debug request/response

    // ---- Lokasi GPS ----
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // ---- Keamanan: EncryptedSharedPreferences (PRD bagian 10) ----
    // Menyimpan token login dalam bentuk terenkripsi (bukan teks biasa),
    // kuncinya dikelola Android Keystore. Versi 1.1.0-alpha06 dipakai
    // karena stabil di API 26+ dan tersedia luas di Maven.
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // ---- Lifecycle & Coroutines (untuk memanggil API secara async) ----
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // ---- Basic Android/Kotlin ----
    implementation("androidx.core:core-ktx:1.13.1")
}