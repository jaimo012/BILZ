# BILZ - Android 앱

> 카메라, 이미지 처리, Google Drive 연동 기능을 갖춘 Android 앱

## 📱 프로젝트 개요

BILZ는 Kotlin과 Jetpack Compose를 사용하여 개발된 Android 앱입니다.

### 주요 기능

- 📷 **카메라 촬영** - CameraX를 사용한 사진 촬영
- 🖼️ **이미지 자르기** - Android Image Cropper를 사용한 이미지 편집
- 🔐 **Google 로그인** - Google Play Services Auth를 통한 인증
- ☁️ **Google Drive 업로드** - 촬영한 이미지를 Drive에 업로드

## 🛠️ 기술 스택

| 기술 | 버전 | 용도 |
|------|------|------|
| Kotlin | 2.0.21 | 프로그래밍 언어 |
| Jetpack Compose | BOM 2024.12.01 | 선언적 UI 프레임워크 |
| CameraX | 1.4.1 | 카메라 기능 |
| Coil | 2.7.0 | 이미지 로딩 |
| Google Play Services Auth | 21.3.0 | Google 로그인 |
| Google API Client | 2.7.0 | Google Drive API |
| Android Image Cropper | 4.6.0 | 이미지 자르기 |
| Kotlin Coroutines | 1.9.0 | 비동기 처리 |

## 📋 요구 사항

- Android Studio Ladybug (2024.2.1) 이상
- JDK 17 이상
- Android SDK 34 (Android 14)
- 최소 지원 버전: Android 8.0 (API 26)

## 🚀 시작하기

### 1. 프로젝트 클론

```bash
git clone https://github.com/your-username/BILZ.git
cd BILZ
```

### 2. Android Studio에서 열기

1. Android Studio 실행
2. "Open" 선택
3. 프로젝트 폴더 선택
4. Gradle Sync가 완료될 때까지 대기

### 3. 실행

1. Android 에뮬레이터 또는 실제 기기 연결
2. "Run" 버튼 클릭 또는 `Shift + F10`

## 📁 프로젝트 구조

```
BILZ/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/bilz/app/
│   │       │   ├── MainActivity.kt           # 메인 액티비티 (권한 처리, 화면 전환)
│   │       │   ├── ui/
│   │       │   │   ├── screens/
│   │       │   │   │   ├── HomeScreen.kt         # 홈 화면
│   │       │   │   │   ├── PermissionScreen.kt   # 권한 요청 화면
│   │       │   │   │   ├── CameraScreen.kt       # 카메라 촬영 화면
│   │       │   │   │   └── FileNameInputScreen.kt # 파일명 입력 화면
│   │       │   │   ├── components/
│   │       │   │   │   └── ExpenseInputDialog.kt  # 지출 용도 입력 다이얼로그
│   │       │   │   └── theme/
│   │       │   │       ├── Color.kt
│   │       │   │       ├── Theme.kt
│   │       │   │       └── Type.kt
│   │       │   └── util/
│   │       │       ├── ImageSaver.kt          # MediaStore 이미지 저장 유틸리티
│   │       │       └── GoogleAuthManager.kt   # Google 로그인 관리 클래스
│   │       ├── res/
│   │       │   ├── values/
│   │       │   │   ├── strings.xml
│   │       │   │   └── themes.xml
│   │       │   └── xml/
│   │       │       └── file_paths.xml
│   │       └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       └── gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

## 📄 권한

앱에서 사용하는 권한:

| 권한 | 용도 |
|------|------|
| `CAMERA` | 사진 촬영 |
| `INTERNET` | 네트워크 통신 (Google Drive 업로드) |
| `ACCESS_NETWORK_STATE` | 네트워크 상태 확인 |
| `READ_EXTERNAL_STORAGE` | 갤러리 이미지 읽기 (Android 12 이하) |
| `READ_MEDIA_IMAGES` | 미디어 이미지 접근 (Android 13+) |

## 🔧 의존성

### 핵심 의존성 (`app/build.gradle.kts`)

```kotlin
dependencies {
    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    
    // Coil (이미지 로딩)
    implementation(libs.coil.compose)
    
    // Google Services
    implementation(libs.play.services.auth)
    implementation(libs.google.api.client.android)
    implementation(libs.google.api.services.drive)
    
    // Image Cropper
    implementation(libs.android.image.cropper)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
}
```

## 📝 버전 히스토리

### v1.0.0 (2026-02-01)
- 🎉 초기 프로젝트 설정
- 📦 의존성 추가 (CameraX, Coil, Google Auth, Drive API, Image Cropper, Coroutines)
- 📋 AndroidManifest 권한 선언
- 🔐 카메라/저장소 권한 요청 로직 구현
- 🏠 홈 화면 UI 구현 (시작 버튼)
- 📷 CameraX 카메라 촬영 화면 구현
- 🔄 화면 전환 애니메이션 적용
- ✂️ CanHub Image Cropper 이미지 자르기 기능 구현
- 📝 파일명 입력 화면 구현
- 💬 지출 용도 입력 다이얼로그 구현 ({yyyyMMdd}_{사용자입력}.jpg 형식)
- 💾 MediaStore를 이용한 Scoped Storage 이미지 저장 기능 구현
  - Pictures/BILZ 폴더에 이미지 저장
  - Android 10+ Scoped Storage 지원
  - 저장 완료/실패 화면 UI 구현
- 🔐 Google 로그인 기능 구현 (Drive API용)
  - GoogleAuthManager 클래스로 로그인 상태 관리
  - DriveScopes.DRIVE_FILE 스코프 요청
  - 저장 완료 화면에 Google Drive 업로드 버튼 추가
  - 로그인 상태에 따른 자동 로그인/로그인 화면 표시

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📜 라이선스

이 프로젝트는 MIT 라이선스를 따릅니다.

## 📞 연락처

프로젝트에 대한 질문이 있으시면 이슈를 생성해 주세요.
