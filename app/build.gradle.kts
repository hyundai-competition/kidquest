// 앱 빌드 설정. 버전은 공식 샘플을 따랐다. 서명과 APK 내보내기는 앱 통합 역할이 정한다.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.kidquest.app"
    compileSdk = 35

    // 주행 규제 값을 읽는다.
    useLibrary("android.car")

    defaultConfig {
        applicationId = "com.kidquest.app"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // release 서명. 키와 비밀번호는 저장소 밖(GRADLE_USER_HOME의 gradle.properties)에 둔다.
    // 값이 없는 컴퓨터에서는 release를 서명 없이 만든다. 그래서 다른 역할과 CI의 빌드는 그대로 돈다.
    val releaseStore = providers.gradleProperty("KQ_RELEASE_STORE_FILE").orNull
    signingConfigs {
        if (releaseStore != null) create("release") {
            storeFile = file(releaseStore)
            storePassword = providers.gradleProperty("KQ_RELEASE_STORE_PASSWORD").get()
            keyAlias = providers.gradleProperty("KQ_RELEASE_KEY_ALIAS").get()
            keyPassword = providers.gradleProperty("KQ_RELEASE_KEY_PASSWORD").get()
        }
    }
    buildTypes {
        release {
            // Pleos SDK가 리플렉션을 쓰는지 확인하지 않아서 코드 줄이기(R8)는 끈다.
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
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
        compose = true
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.pleos.vehicle)
    implementation(libs.pleos.navi.helper)
    implementation(libs.pleos.fused.location)
    implementation(libs.pleos.text.to.speech)
    implementation(libs.pleos.speech.to.text)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
