plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ghost.caller"

    // Note: Standardized to API 35 (Android 15) as 36 is currently an early preview.
    // If you specifically need 36, you can revert this to your preview syntax.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ghost.caller"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 1. Configure Build Types
    buildTypes {
        // The default debug build (used when you just hit "Run" in Android Studio)
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }

        // 2. The "Other" Build (Staging / Beta)
        // Great for sharing test builds with others without overwriting the production app on their phone
        create("staging") {
            initWith(getByName("debug")) // Copies everything from the debug config
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-STAGING"
        }

        // 3. The Release Build
        getByName("release") {
            // 🔥 CRITICAL: Turned minification ON for release.
            // This shrinks your APK size and obfuscates your code to prevent reverse engineering.
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // 🔥 SIGNING: Signs the release build with your local Debug Key as requested
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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


    // accomplish - permission
    implementation(libs.accompanist.permissions)
    implementation(libs.libphone.number)
    implementation("com.googlecode.libphonenumber:geocoder:3.26")
}