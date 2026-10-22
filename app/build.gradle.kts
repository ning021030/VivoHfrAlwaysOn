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
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // UI 及基础库
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")

    // 【核心关键】必须加上这一行，否则编译时找不到 de.robv.android.xposed.*
    compileOnly("de.robv.android.xposed:api:82")
}
