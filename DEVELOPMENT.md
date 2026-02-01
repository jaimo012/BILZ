# BILZ 앱 개발 문서

> 📱 Android 카메라 앱 개발 과정 기록  
> 개발 기간: 2026년 2월 1일

---

## 📋 목차

1. [프로젝트 기획](#1-프로젝트-기획)
2. [기술 스택 선정](#2-기술-스택-선정)
3. [개발 과정](#3-개발-과정)
4. [오류 대응 과정](#4-오류-대응-과정)
5. [배운 교훈 (Lessons Learned)](#5-배운-교훈-lessons-learned)
6. [최종 결과물](#6-최종-결과물)

---

## 1. 프로젝트 기획

### 1.1 앱 목적

**BILZ**는 지출 영수증 등을 촬영하여 Google Drive에 자동으로 백업하는 Android 앱입니다.

### 1.2 핵심 요구사항

| 요구사항 | 설명 |
|---------|------|
| 📷 카메라 촬영 | CameraX를 사용한 고품질 사진 촬영 |
| ✂️ 이미지 편집 | 촬영 후 원하는 영역만 자르기 |
| 📝 파일명 지정 | `{날짜}_{용도}.jpg` 형식으로 파일명 생성 |
| 💾 로컬 저장 | Pictures/BILZ 폴더에 저장 |
| ☁️ 클라우드 백업 | Google Drive 특정 폴더에 자동 업로드 |
| 🔄 연속 촬영 | 업로드 완료 후 카메라로 자동 복귀 |

### 1.3 사용자 플로우

```
앱 실행 → 권한 요청 → 홈 화면 → 시작 (Google 로그인)
    ↓
카메라 촬영 → 이미지 자르기 → 파일명 입력
    ↓
로컬 저장 → Google Drive 업로드 → 완료
    ↓
카메라로 자동 복귀 (반복)
```

---

## 2. 기술 스택 선정

### 2.1 핵심 기술

| 기술 | 버전 | 선정 이유 |
|------|------|----------|
| **Kotlin** | 2.0.21 | Android 공식 언어, 현대적 문법 |
| **Jetpack Compose** | BOM 2024.12.01 | 선언적 UI, 빠른 개발 속도 |
| **CameraX** | 1.4.1 | 카메라 API 추상화, 생명주기 관리 |
| **CanHub Image Cropper** | 4.5.0 | 검증된 이미지 자르기 라이브러리 |
| **Google Play Services Auth** | 21.3.0 | Google 로그인 표준 |
| **Google API Client** | 2.7.0 | Google Drive API 연동 |
| **Kotlin Coroutines** | 1.9.0 | 비동기 처리 |

### 2.2 Android SDK 설정

```kotlin
android {
    compileSdk = 35
    minSdk = 26      // Android 8.0 이상
    targetSdk = 35   // Android 15
}
```

---

## 3. 개발 과정

### 3.1 단계별 개발

#### 📌 1단계: 프로젝트 설정 및 의존성 추가

- `build.gradle.kts` 설정
- `libs.versions.toml` 버전 카탈로그 구성
- `AndroidManifest.xml` 권한 선언

#### 📌 2단계: 권한 요청 로직

- 카메라 권한 (`CAMERA`)
- 저장소 권한 (`READ_EXTERNAL_STORAGE`, `READ_MEDIA_IMAGES`)
- Android 13+ 대응

#### 📌 3단계: CameraX 카메라 구현

- `PreviewView`를 사용한 미리보기
- `ImageCapture`를 사용한 촬영
- 전면/후면 카메라 전환

#### 📌 4단계: 이미지 자르기 (Cropping)

- CanHub Android Image Cropper 라이브러리 연동
- `CropImageContract`를 사용한 Activity Result API 연동

#### 📌 5단계: 파일명 입력 다이얼로그

- `{yyyyMMdd}_{사용자입력}.jpg` 형식 파일명 생성
- 입력값 유효성 검사

#### 📌 6단계: 로컬 저장 (Scoped Storage)

- `MediaStore` API를 사용한 저장
- `Pictures/BILZ` 폴더에 저장
- Android 10+ Scoped Storage 대응

#### 📌 7단계: Google 로그인

- `GoogleSignInOptions` 설정
- `DriveScopes.DRIVE_FILE` 스코프 요청
- 로그인 상태 관리

#### 📌 8단계: Google Drive 업로드

- `DriveServiceHelper` 클래스 구현
- Content Uri에서 직접 업로드
- 특정 폴더 ID에 업로드

#### 📌 9단계: 전체 흐름 연결

- 화면 상태 관리 (`sealed class AppScreen`)
- `AnimatedContent`를 사용한 화면 전환
- 자동 플로우 구현

#### 📌 10단계: UI/UX 다듬기

- 다크 테마 적용
- 자동 초점 기능 추가
- 터치 초점 기능 추가
- 로딩 인디케이터 추가

---

## 4. 오류 대응 과정

### 4.1 빌드 오류

#### ❌ 오류 1: Image Cropper 버전 문제

```
Failed to resolve: com.github.CanHub:Android-Image-Cropper:4.6.0
```

**원인**: JitPack에 4.6.0 버전이 존재하지 않음

**해결**: 버전을 4.5.0으로 다운그레이드

```toml
# libs.versions.toml
imageCropper = "4.5.0"  # 4.6.0 → 4.5.0
```

---

#### ❌ 오류 2: compileSdk 버전 불일치

```
Dependency 'androidx.core:core:1.15.0' requires libraries and applications 
that depend on it to compile against version 35 or later of the Android APIs.
:app is currently compiled against android-34.
```

**원인**: `androidx.core:core:1.15.0`이 `compileSdk 35`를 요구

**해결**: `compileSdk`와 `targetSdk`를 35로 업그레이드

```kotlin
// app/build.gradle.kts
android {
    compileSdk = 35
    defaultConfig {
        targetSdk = 35
    }
}
```

**주의**: `libs.versions.toml`에서 설정해도 `build.gradle.kts`에서 하드코딩되어 있으면 무시됨!

---

#### ❌ 오류 3: 앱 아이콘 리소스 누락

```
AAPT: error: resource mipmap/ic_launcher (aka com.bilz.app:mipmap/ic_launcher) not found.
```

**원인**: `mipmap` 폴더에 앱 아이콘 파일이 없음

**해결**: Android Studio의 Image Asset 도구로 아이콘 생성

---

#### ❌ 오류 4: const val 초기화 오류

```kotlin
// ❌ 오류 발생
private const val RELATIVE_PATH = "${Environment.DIRECTORY_PICTURES}/$BILZ_FOLDER_NAME"
// Const 'val' initializer should be a constant value.
```

**원인**: `Environment.DIRECTORY_PICTURES`는 컴파일 타임 상수가 아님

**해결**: 문자열 리터럴로 직접 작성

```kotlin
// ✅ 해결
private const val RELATIVE_PATH = "Pictures/$BILZ_FOLDER_NAME"
```

---

### 4.2 런타임 오류

#### ❌ 오류 5: FileUriExposedException

**증상**: 사진 촬영 후 앱 크래시

**원인**: Android 7.0+ 에서 `file://` URI를 다른 앱에 전달할 수 없음

**해결**: `FileProvider`를 사용하여 `content://` URI 생성

```kotlin
// ❌ 오류 발생
val savedUri = Uri.fromFile(photoFile)

// ✅ 해결
val savedUri = FileProvider.getUriForFile(
    context,
    "${context.packageName}.fileprovider",
    photoFile
)
```

---

#### ❌ 오류 6: AppCompat 테마 필요

```
java.lang.IllegalStateException: 
You need to use a Theme.AppCompat theme (or descendant) with this activity.
```

**원인**: `CropImageActivity`가 `AppCompatActivity`를 상속하므로 AppCompat 테마 필요

**해결**: 앱 테마를 `Theme.AppCompat`으로 변경

```xml
<!-- ❌ 오류 발생 -->
<style name="Theme.BILZ" parent="android:Theme.Material.NoActionBar">

<!-- ✅ 해결 -->
<style name="Theme.BILZ" parent="Theme.AppCompat.DayNight.NoActionBar">
```

---

#### ❌ 오류 7: 크롭 화면 저장 버튼 안 보임

**증상**: 이미지 자르기 화면에서 저장 버튼이 보이지 않음

**원인**: `NoActionBar` 테마 사용으로 툴바가 숨겨짐

**해결**: 크롭 액티비티 전용 테마 생성 (ActionBar 포함)

```xml
<style name="Theme.BILZ.Crop" parent="Theme.AppCompat.DayNight.DarkActionBar">
```

---

#### ❌ 오류 8: Google 로그인 실패

**증상**: "로그인 실패" 메시지 표시

**원인**: Google Cloud Console에서 패키지 이름이 잘못 등록됨

| 설정 항목 | 잘못된 값 | 올바른 값 |
|----------|----------|----------|
| 패키지 이름 | `com.example.bilz` | `com.bilz.app` |

**해결**: Google Cloud Console에서 OAuth 클라이언트의 패키지 이름 수정

---

#### ❌ 오류 9: Coroutine Scope 에러

```
The coroutine scope left the composition
```

**증상**: 두 번째 업로드 시 에러 발생

**원인**: 화면 전환 시 `rememberCoroutineScope()`가 dispose되면서 coroutine 취소

**해결**: 로그인 상태를 앱 전체에서 유지하고, 시작 시 한 번만 로그인

```kotlin
// 로그인된 계정을 앱 전체에서 유지
var signedInAccount by remember { mutableStateOf<GoogleSignInAccount?>(null) }

// 시작 버튼 클릭 시 로그인 확인
val onStartClick: () -> Unit = {
    if (signedInAccount != null) {
        currentScreen = AppScreen.Camera  // 이미 로그인됨
    } else {
        // 로그인 진행 후 카메라로 이동
    }
}
```

---

## 5. 배운 교훈 (Lessons Learned)

### 🎓 Lesson 1: 의존성 버전 관리

> **항상 실제로 존재하는 버전을 확인하세요.**

- Maven Central, JitPack 등에서 실제 배포된 버전 확인
- `libs.versions.toml`로 버전을 중앙 관리하되, `build.gradle.kts`에서 오버라이드되지 않는지 확인

---

### 🎓 Lesson 2: Android 버전 호환성

> **새로운 의존성을 추가할 때 minSdk/compileSdk 요구사항을 확인하세요.**

```kotlin
// 의존성이 요구하는 최소 SDK 버전 확인 필수
implementation("androidx.core:core-ktx:1.15.0") // → compileSdk 35 필요
```

---

### 🎓 Lesson 3: FileProvider 필수

> **Android 7.0+ 에서 파일을 다른 앱과 공유하려면 FileProvider가 필수입니다.**

```xml
<!-- AndroidManifest.xml -->
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

---

### 🎓 Lesson 4: AppCompat 테마 호환성

> **외부 라이브러리가 AppCompatActivity를 사용하면 AppCompat 테마가 필요합니다.**

- Material Design 3 테마와 AppCompat 라이브러리가 호환되지 않을 수 있음
- 특정 Activity에만 다른 테마를 적용할 수 있음

---

### 🎓 Lesson 5: Compose에서 Coroutine 생명주기

> **rememberCoroutineScope()는 Composable의 생명주기에 묶입니다.**

```kotlin
// ❌ 화면 전환 시 coroutine이 취소됨
val scope = rememberCoroutineScope()
scope.launch { /* 장시간 작업 */ }

// ✅ 상태를 상위 레벨에서 관리하여 화면 전환과 분리
var signedInAccount by remember { mutableStateOf<GoogleSignInAccount?>(null) }
```

---

### 🎓 Lesson 6: Google Cloud Console 설정

> **OAuth 클라이언트 설정 시 패키지 이름과 SHA-1 지문이 정확해야 합니다.**

| 설정 항목 | 설명 |
|----------|------|
| 패키지 이름 | `AndroidManifest.xml`의 `package`와 정확히 일치 |
| SHA-1 지문 | Debug/Release 키스토어 각각 등록 필요 |
| 테스트 사용자 | 앱 게시 전까지 테스트 사용자만 로그인 가능 |

---

### 🎓 Lesson 7: 글로벌 예외 핸들러로 디버깅

> **크래시 원인을 파악하기 어려울 때는 글로벌 예외 핸들러를 추가하세요.**

```kotlin
class BilzApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e("App", "크래시 발생: ${exception.message}")
            exception.printStackTrace()
        }
    }
}
```

---

## 6. 최종 결과물

### 6.1 완성된 기능

| 기능 | 상태 | 설명 |
|------|------|------|
| 📷 카메라 촬영 | ✅ | CameraX 기반, 자동/터치 초점 |
| 🔄 카메라 전환 | ✅ | 전면/후면 전환 |
| ✂️ 이미지 자르기 | ✅ | 자유 비율 크롭 |
| 📝 파일명 입력 | ✅ | 날짜_용도.jpg 형식 |
| 💾 로컬 저장 | ✅ | Pictures/BILZ 폴더 |
| 🔐 Google 로그인 | ✅ | 시작 시 한 번만 로그인 |
| ☁️ Drive 업로드 | ✅ | 지정 폴더에 자동 업로드 |
| 🔄 연속 촬영 | ✅ | 업로드 후 카메라로 자동 복귀 |

### 6.2 프로젝트 구조

```
BILZ/
├── app/
│   ├── src/main/
│   │   ├── java/com/bilz/app/
│   │   │   ├── MainActivity.kt
│   │   │   ├── BilzApplication.kt
│   │   │   ├── ui/
│   │   │   │   ├── screens/
│   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   ├── CameraScreen.kt
│   │   │   │   │   ├── PermissionScreen.kt
│   │   │   │   │   └── FileNameInputScreen.kt
│   │   │   │   └── theme/
│   │   │   └── util/
│   │   │       ├── ImageSaver.kt
│   │   │       ├── GoogleAuthManager.kt
│   │   │       └── DriveServiceHelper.kt
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── README.md
├── DEVELOPMENT.md (이 문서)
└── rules.md
```

### 6.3 GitHub 저장소

🔗 **https://github.com/jaimo012/BILZ**

---

## 📝 마무리

이 프로젝트를 통해 Android 앱 개발의 전체 과정을 경험했습니다:

1. **기획** - 요구사항 정의 및 사용자 플로우 설계
2. **개발** - Kotlin + Jetpack Compose로 UI 구현
3. **연동** - CameraX, Google API 등 외부 라이브러리 연동
4. **디버깅** - 빌드 오류 및 런타임 오류 해결
5. **배포** - GitHub에 코드 배포

특히 **오류 해결 과정**에서 많은 것을 배웠습니다. 오류 메시지를 정확히 읽고, 원인을 분석하고, 적절한 해결책을 찾는 것이 개발의 핵심 역량임을 깨달았습니다.

---

*문서 작성일: 2026년 2월 1일*
