plugins {
    id("com.android.application")
}

android {
    namespace = "com.wuhen.shi"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.wuhen.shi"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        jniLibs.useLegacyPackaging = true
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a")
            isUniversalApk = false
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation(files("../local-repo/youtubedl-android-library.aar"))
    implementation(files("../local-repo/youtubedl-android-ffmpeg.aar"))
    implementation(files("../local-repo/youtubedl-android-aria2c.aar"))
}
