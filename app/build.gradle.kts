plugins {
    id("sidedoor.android.application")
    id("sidedoor.android.compose")
}

android {
    namespace = "com.sidedoor.app"

    defaultConfig {
        applicationId = "com.sidedoor.app"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        buildConfig = true
    }

    // Every installed copy can only be updated by an APK signed with
    // this same key, so it lives outside the repo (see the README). With no
    // key configured the release APK comes out unsigned rather than silently
    // debug-signed - an APK nobody can update over is worse than none.
    val keystore = providers.gradleProperty("sidedoor.keystore").orNull
    val keystorePassword = providers.gradleProperty("sidedoor.keystore.password").orNull
    val release = if (keystore != null && keystorePassword != null) {
        signingConfigs.create("release") {
            storeFile = file(keystore)
            storePassword = keystorePassword
            keyAlias = "sidedoor"
            keyPassword = keystorePassword
        }
    } else {
        null
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            // Same key as release when available, so a debug build can be swapped
            // onto a phone running the release build without uninstalling.
            if (release != null) signingConfig = release
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = release
        }
    }
}

dependencies {
    implementation(projects.guardCore)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
