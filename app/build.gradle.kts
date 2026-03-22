import org.gradle.language.nativeplatform.internal.Dimensions.applicationVariants
import com.android.build.api.variant.FilterConfiguration

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ghost.caller"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ghost.caller"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 🔥 Signing (optional but cleaner)
    signingConfigs {
        getByName("debug")

        create("release") {
            // TODO: replace with your real keystore later
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {

        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }

        create("staging") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-STAGING"
        }

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig = signingConfigs.getByName("release")
        }
    }

    // 🔥 ABI SPLITS (multi-APK + universal)
    splits {
        abi {
            isEnable = true
            reset()

            // Supported architectures
            include(
                "armeabi-v7a",
                "arm64-v8a",
                "x86_64"
            )

            // 🔥 This generates ONE universal APK too
            isUniversalApk = true
        }
    }

    // 🔥 Unique versionCode per ABI (required for Play Store multi APK)

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

androidComponents {
    onVariants { variant ->

        variant.outputs.forEach { output ->

            val abi = output.filters
                .find { it.filterType.name == "ABI" }
                ?.identifier

            val baseAbiCode = when (abi) {
                "armeabi-v7a" -> 1
                "arm64-v8a" -> 2
                "x86_64" -> 3
                else -> 0
            }

            val baseVersionCode = output.versionCode.orNull ?: 1

            val newVersionCode = baseVersionCode * 1000 + baseAbiCode

            output.versionCode.set(newVersionCode)
        }
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

    // Permissions
    implementation(libs.accompanist.permissions)

    // Phone number + geocoder
    implementation(libs.libphone.number)
    implementation(libs.geocoder)
}