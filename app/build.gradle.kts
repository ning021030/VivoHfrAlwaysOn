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
    // 必须引入此项以解决 Theme.AppCompat 找不到的问题
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
}
