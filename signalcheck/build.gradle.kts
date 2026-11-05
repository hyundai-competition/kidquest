// 첫 시험 전용 앱 빌드 설정. 제출 앱(app)과 따로 빌드하고, 제출 APK에 들어가지 않는다.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.kidquest.signalcheck"
    compileSdk = 35

    // 주행 규제 값과 AAOS 기어 값을 읽는다.
    useLibrary("android.car")

    defaultConfig {
        applicationId = "com.kidquest.signalcheck"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.pleos.vehicle)
    implementation(libs.pleos.navi.helper)
    implementation(libs.pleos.fused.location)
    implementation(libs.pleos.text.to.speech)
    implementation(libs.pleos.speech.to.text)
}
