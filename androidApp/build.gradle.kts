plugins {
    id("com.android.application")
    kotlin("android")
    id("com.google.gms.google-services")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

group = "me.eduard"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":shared"))
    implementation("pl.droidsonroids.gif:android-gif-drawable:1.2.17")
    implementation(platform("com.google.firebase:firebase-bom:30.0.1"))
    implementation("com.google.android.gms:play-services-location:19.0.1")
    implementation("com.google.android.gms:play-services-location:19.0.1")
    implementation("com.google.android.material:material:1.6.0")
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.annotation:annotation:1.3.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.4.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.1")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-common-ktx")
    implementation("com.google.android.gms:play-services-maps:18.0.2")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.1")
    implementation("androidx.room:room-common:2.4.2")
    implementation("com.google.firebase:firebase-database-ktx:20.0.5")
    implementation("com.google.firebase:firebase-storage-ktx:20.0.1")
    implementation("com.google.android.libraries.places:places:2.6.0")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter")
    annotationProcessor("androidx.room:room-compiler:2.4.2")
    implementation("com.google.ar.sceneform:filament-android:1.17.1")
    implementation("com.google.firebase:firebase-auth-ktx:21.0.4")
    testImplementation(kotlin("test"))
}



android {
    compileSdk = 31
    defaultConfig {
        applicationId = "me.eduard.androidApp"
        versionCode = 1
        versionName = "1.0"
        minSdk = 24
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    buildFeatures {
        viewBinding = true
    }
    buildToolsVersion = "30.0.3"
}