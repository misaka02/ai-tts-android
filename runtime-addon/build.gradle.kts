plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.aitts.engine.offline.runtime"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aitts.engine.offline.runtime"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.13.6"

        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}
