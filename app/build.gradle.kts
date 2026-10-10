plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "ir.ettehad.realestate"
    compileSdk = 35
    defaultConfig {
        applicationId = "ir.ettehad.realestate"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
