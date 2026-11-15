plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "gh.otumfour.o24"
    compileSdk = 35

    defaultConfig {
        applicationId = "gh.otumfour.o24"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "SERWAA_SOURCE", "\"OriginalSolution/traffitech-gh-alert@cursor/traffialert-gh-v1-4c59\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    androidResources {
        noCompress += "mp3"
    }

    signingConfigs {
        create("testRelease") {
            val storePath = System.getenv("KEYSTORE_FILE") ?: "../keystore/o24-test.jks"
            storeFile = file(storePath)
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "o24keyboard"
            keyAlias = System.getenv("KEY_ALIAS") ?: "o24"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "o24keyboard"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("testRelease")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    testImplementation("junit:junit:4.13.2")
}
