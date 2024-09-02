plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.gms.google-services")

}

android {
    namespace = "com.example.wellnest"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.wellnest"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        viewBinding = true
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
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation ("com.caverock:androidsvg:1.4")
    implementation ("androidx.cardview:cardview:1.0.0")
    implementation ("com.mikhaellopez:circularprogressbar:3.1.0")
    implementation ("com.google.firebase:firebase-auth-ktx:22.1.1")
    implementation ("pl.droidsonroids.gif:android-gif-drawable:1.2.17")
    implementation ("com.google.android.recaptcha:recaptcha:18.4.0")

    implementation(platform("com.google.firebase:firebase-bom:32.2.3"))
    implementation ("com.google.firebase:firebase-firestore-ktx")
    implementation ("com.google.firebase:firebase-storage:20.2.1") // Use the latest version

    implementation ("link.magic:magic-android:4.0.0")
    implementation ("com.google.firebase:firebase-core:21.1.1")
    implementation ("link.magic:magic-android:4.0.0")
    implementation ("link.magic:magic-ext-oauth:3.0.0")
    implementation ("link.magic:magic-android:4.0.0")
    implementation ("org.web3j:geth:4.8.8-android")
    implementation ("org.web3j:core:5.0.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    implementation ("link.magic:magic-ext-oauth:[4.0,5.0[")
    implementation ("link.magic:magic-ext-oidc:[2.0,3.0[")
    implementation ("jp.wasabeef:blurry:4.0.1")

    implementation ("com.google.android.gms:play-services-auth:20.7.0")
    implementation(kotlin("script-runtime"))

}