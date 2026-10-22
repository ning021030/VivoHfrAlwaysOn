plugins {
    id("com.android.application")
}

android {
    namespace = "com.bomo.vivohfr"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bomo.vivohfr"
        minSdk = 26
        targetSdk = 34
        versionCode = 4
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
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
}

dependencies {
    compileOnly("androidx.annotation:annotation:1.9.1")
    compileOnly("de.robv.android.xposed:api:82")
}
