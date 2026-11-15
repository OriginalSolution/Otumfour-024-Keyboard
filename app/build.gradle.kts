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
        versionCode = 8
        versionName = "1.0.7"
        buildConfigField("String", "O24_AI_URL", "\"https://ffqchvmcdstxmjsimgdt.supabase.co/functions/v1/o24-ai\"")
        // Public Supabase anon key for the O24 AI proxy. The OpenAI key stays on the server.
        buildConfigField(
            "String",
            "O24_AI_ANON",
            "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZmcWNodm1jZHN0eG1qc2ltZ2R0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTQ3MzA4MTMsImV4cCI6MjExMDMwNjgxM30.VMFl4o-TaJW_Hp0lfwuniCgheS_KoQjS-0BuxPYeAj4\"",
        )
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

    signingConfigs {
        create("testRelease") {
            // GitHub Actions sets unset secrets to empty strings. An empty value must
            // not replace the committed test key (alias o24, password o24keyboard).
            fun envOrDefault(name: String, default: String): String =
                System.getenv(name)?.takeIf { it.isNotBlank() } ?: default

            storeFile = file(envOrDefault("KEYSTORE_FILE", "../keystore/o24-test.jks"))
            storePassword = envOrDefault("KEYSTORE_PASSWORD", "o24keyboard")
            keyAlias = envOrDefault("KEY_ALIAS", "o24")
            keyPassword = envOrDefault("KEY_PASSWORD", "o24keyboard")
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
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    testImplementation("junit:junit:4.13.2")
}
