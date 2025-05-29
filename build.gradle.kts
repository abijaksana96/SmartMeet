// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false

    // Deklarasikan plugin Kotlin Android dan Kapt dengan versinya
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false // <--- Sesuaikan versi Kotlin Anda
    id("org.jetbrains.kotlin.jvm") version "1.9.22" apply false
    id("org.jetbrains.kotlin.kapt") version "1.9.22" apply false // <-- Tambahkan ini juga
}