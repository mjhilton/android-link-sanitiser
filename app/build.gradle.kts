plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "id.hiltons.linksanitiser"
    compileSdk = 35

    defaultConfig {
        applicationId = "id.hiltons.linksanitiser"
        minSdk = 24
        // Deliberately not 35: targeting SDK 35 makes Android 15's edge-to-edge
        // enforcement mandatory, which breaks windowSoftInputMode="adjustResize"'s
        // usual automatic keyboard handling (the OS stops resizing/padding the
        // window for you, and you have to reimplement it by hand via
        // WindowInsetsCompat - which turned out fiddly and unreliable in practice).
        // This app is sideloaded only, never published to Play, so there's no
        // targetSdk floor to satisfy - simplest fix is to not opt into that.
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
