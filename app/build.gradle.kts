// BILZ 앱 모듈 빌드 설정
// 이 파일에서는 앱 빌드에 필요한 모든 설정과 의존성을 정의합니다.

plugins {
    // Android Application 플러그인 - 앱 빌드에 필수
    alias(libs.plugins.android.application)
    // Kotlin Android 플러그인 - Kotlin 언어 지원
    alias(libs.plugins.kotlin.android)
    // Kotlin Compose Compiler 플러그인 - Jetpack Compose 컴파일러 지원
    alias(libs.plugins.kotlin.compose)
}

android {
    // 앱의 고유 식별자 (패키지명)
    namespace = "com.bilz.app"
    
    // 컴파일에 사용할 Android SDK 버전
    compileSdk = 34

    defaultConfig {
        // Google Play Store에서 앱을 식별하는 고유 ID
        applicationId = "com.bilz.app"
        
        // 앱이 지원하는 최소 Android 버전 (Android 8.0 Oreo)
        minSdk = 26
        
        // 앱이 테스트된 최신 Android 버전
        targetSdk = 34
        
        // 앱 버전 코드 (업데이트 시 증가해야 함)
        versionCode = 1
        
        // 사용자에게 표시되는 버전 이름
        versionName = "1.0.0"

        // 테스트 실행을 위한 테스트 러너
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        // 릴리즈 빌드 설정
        release {
            // 코드 축소(minify) 비활성화 - 필요 시 true로 변경
            isMinifyEnabled = false
            // ProGuard 규칙 파일
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    // Java 버전 호환성 설정
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    // Kotlin 컴파일러 옵션
    kotlinOptions {
        jvmTarget = "11"
    }
    
    // 빌드 기능 활성화
    buildFeatures {
        // Jetpack Compose 활성화
        compose = true
    }
    
    // JAR 패키징 시 중복 파일 제외 설정
    // Google API Client 라이브러리 사용 시 필요한 설정
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/INDEX.LIST"
        }
    }
}

dependencies {
    // ============================================================
    // AndroidX Core 라이브러리
    // ============================================================
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // ============================================================
    // Jetpack Compose (선언적 UI 프레임워크)
    // BOM(Bill of Materials)을 사용하여 모든 Compose 라이브러리 버전을 일관되게 관리
    // ============================================================
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    
    // Material Icons Extended - 추가 아이콘 (CameraAlt, Folder 등)
    implementation(libs.androidx.material.icons.extended)

    // ============================================================
    // CameraX (카메라 기능)
    // Android Jetpack의 카메라 라이브러리로, 기기별 호환성 문제를 해결하고
    // 일관된 카메라 API를 제공합니다.
    // ============================================================
    implementation(libs.androidx.camera.core)      // 핵심 카메라 기능
    implementation(libs.androidx.camera.camera2)   // Camera2 API 구현체
    implementation(libs.androidx.camera.lifecycle) // 생명주기 인식 카메라 관리
    implementation(libs.androidx.camera.view)      // 카메라 미리보기 뷰

    // ============================================================
    // Coil (이미지 로딩 라이브러리)
    // Kotlin으로 작성된 경량 이미지 로딩 라이브러리로,
    // Coroutines를 기반으로 하여 빠르고 효율적입니다.
    // ============================================================
    implementation(libs.coil.compose)

    // ============================================================
    // Google Play Services Auth (구글 로그인)
    // Google 계정을 사용한 로그인 기능을 제공합니다.
    // ============================================================
    implementation(libs.play.services.auth)

    // ============================================================
    // Google API Client for Drive (구글 드라이브 업로드)
    // Google Drive API를 사용하여 파일을 업로드/다운로드할 수 있습니다.
    // ============================================================
    implementation(libs.google.api.client.android)   // Android용 Google API 클라이언트
    implementation(libs.google.api.services.drive)   // Google Drive API 서비스
    implementation(libs.google.http.client.gson)     // JSON 파싱용 HTTP 클라이언트

    // ============================================================
    // CanHub Android Image Cropper (이미지 자르기)
    // 이미지를 자르고 회전시키는 기능을 제공하는 라이브러리입니다.
    // 원래 ArthurHub 라이브러리의 포크 버전으로, 최신 Android 버전을 지원합니다.
    // ============================================================
    implementation(libs.android.image.cropper)

    // ============================================================
    // Kotlin Coroutines (비동기 처리)
    // Kotlin의 비동기 프로그래밍을 위한 라이브러리입니다.
    // 콜백 지옥 없이 깔끔한 비동기 코드를 작성할 수 있습니다.
    // ============================================================
    implementation(libs.kotlinx.coroutines.core)     // 코루틴 핵심 기능
    implementation(libs.kotlinx.coroutines.android)  // Android 메인 스레드 디스패처

    // ============================================================
    // 테스트 라이브러리
    // ============================================================
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    
    // ============================================================
    // 디버그 빌드 전용 라이브러리
    // ============================================================
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
