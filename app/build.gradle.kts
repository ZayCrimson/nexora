plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.nexora.autoclicker"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.nexora.autoclicker"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
}
