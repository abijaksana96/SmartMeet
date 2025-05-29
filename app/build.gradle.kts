plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.smartmeet"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.smartmeet"
        minSdk = 24
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17) // Ini adalah cara paling modern dan disarankan
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // OSMBb
    implementation("org.osmdroid:osmdroid-android:6.1.10")

    // OkHttp Logging Interceptor
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")

    // Android Navigation Components
    implementation("androidx.navigation:navigation-fragment:2.5.3")
    implementation("androidx.navigation:navigation-ui:2.5.3")

    // Room Database
    implementation("androidx.room:room-runtime:2.5.0")

    kapt("androidx.room:room-compiler:2.5.0")
}