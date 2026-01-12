import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("androidx.navigation.safeargs.kotlin")
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.thenewsapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.thenewsapp"
        minSdk = 28
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"


        val localProps = Properties()
        localProps.load(rootProject.file("local.properties").inputStream())

        buildConfigField(
            "String",
            "NEWS_API_KEY",
            "\"${localProps["NEWS_API_KEY"]}\""
        )
        println(">>> NEWS_API_KEY = ${localProps["NEWS_API_KEY"]}")

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures{
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx.v1120)
    implementation(libs.androidx.appcompat.v161)
    implementation(libs.material.v1100)
    implementation(libs.androidx.constraintlayout.v214)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.v115)
    androidTestImplementation(libs.androidx.espresso.core.v351)

    // Architectural Components
    implementation (libs.androidx.lifecycle.viewmodel.ktx)

    // Room
    implementation (libs.androidx.room.runtime.v260)
    ksp (libs.androidx.room.compiler)

    // Kotlin Extensions and Coroutines support for Room
    implementation (libs.androidx.room.ktx.v260)

    // Coroutines
    implementation (libs.kotlinx.coroutines.core.v171)
    implementation (libs.kotlinx.coroutines.android.v171)

    // Coroutine Lifecycle Scopes
    implementation (libs.androidx.lifecycle.viewmodel.ktx)
    implementation (libs.androidx.lifecycle.runtime.ktx.v2100)

    // Retrofit
    implementation (libs.retrofit.v290)
    implementation (libs.converter.gson.v290)
    implementation (libs.logging.interceptor.v450)

    // Navigation Components
    implementation (libs.androidx.navigation.fragment.ktx.v275)
    implementation (libs.androidx.navigation.ui.ktx.v296)

    // Glide
    implementation (libs.glide)
    ksp (libs.compiler.v505)
}