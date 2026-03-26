# BILZ

> 영수증 촬영 → Google Drive 업로드 → Gemini AI 자동 분석까지 연결되는 개인 경비 관리 시스템

📖 **[개발 과정 문서 (DEVELOPMENT.md)](./DEVELOPMENT.md)** - 기획, 개발 과정, 오류 대응, 배운 교훈

---

## 프로젝트 개요

BILZ는 두 개의 컴포넌트로 구성됩니다.

**Android 앱** — 영수증을 촬영하고 `{날짜}_{용도}.jpg` 형식으로 파일명을 지정해 로컬 저장 및 Google Drive 특정 폴더에 자동 업로드합니다.

**Google Apps Script (GAS)** — Drive에 업로드된 영수증 이미지를 Gemini AI로 분석해 거래처명·금액을 추출하고, 계정과목을 자동 분류한 뒤 Google Sheets에 기록합니다. 월별 PDF 증빙을 생성하고 처리 결과를 Slack으로 알립니다.

---

## Android 앱

### 주요 기능

- 📷 **카메라 촬영** — CameraX 기반, 자동/터치 초점 지원
- ✂️ **이미지 자르기** — Android Image Cropper를 사용한 자유 비율 크롭
- 📝 **파일명 자동 생성** — `{yyyyMMdd}_{용도}.jpg` 형식
- 💾 **로컬 저장** — `Pictures/BILZ` 폴더 (Scoped Storage 대응)
- 🔐 **Google 로그인** — 앱 시작 시 한 번만 인증
- ☁️ **Drive 업로드** — 저장 완료 후 지정 폴더에 자동 업로드
- 🔄 **연속 촬영** — 업로드 완료 후 카메라로 자동 복귀

### 사용자 플로우

```
앱 실행 → 권한 요청 → Google 로그인 → 카메라 촬영
    ↓
이미지 자르기 → 용도 입력 → 로컬 저장 → Drive 업로드
    ↓
카메라로 자동 복귀 (반복)
```

### 기술 스택

| 기술 | 버전 | 용도 |
|------|------|------|
| Kotlin | 2.0.21 | 프로그래밍 언어 |
| Jetpack Compose | BOM 2024.12.01 | 선언적 UI 프레임워크 |
| CameraX | 1.4.1 | 카메라 기능 |
| Coil | 2.7.0 | 이미지 로딩 |
| Google Play Services Auth | 21.3.0 | Google 로그인 |
| Google API Client | 2.7.0 | Google Drive API |
| Android Image Cropper | 4.5.0 | 이미지 자르기 |
| Kotlin Coroutines | 1.9.0 | 비동기 처리 |

### 요구 사항

- Android Studio Ladybug (2024.2.1) 이상
- JDK 17 이상
- Android SDK 35 (Android 15)
- 최소 지원 버전: Android 8.0 (API 26)

### 시작하기

```bash
git clone https://github.com/jaimo012/BILZ.git
cd BILZ
```

Android Studio에서 프로젝트를 열고 Gradle Sync 완료 후 실행하세요.

### 앱 권한

| 권한 | 용도 |
|------|------|
| `CAMERA` | 사진 촬영 |
| `INTERNET` | 네트워크 통신 |
| `ACCESS_NETWORK_STATE` | 네트워크 상태 확인 |
| `READ_EXTERNAL_STORAGE` | 갤러리 이미지 읽기 (Android 12 이하) |
| `READ_MEDIA_IMAGES` | 미디어 이미지 접근 (Android 13+) |

### 프로젝트 구조

```
app/src/main/java/com/bilz/app/
├── MainActivity.kt
├── BilzApplication.kt
├── ui/
│   ├── screens/
│   │   ├── HomeScreen.kt
│   │   ├── CameraScreen.kt
│   │   ├── PermissionScreen.kt
│   │   └── FileNameInputScreen.kt
│   ├── components/
│   │   └── ExpenseInputDialog.kt
│   └── theme/
│       ├── Color.kt, Theme.kt, Type.kt
└── util/
    ├── ImageSaver.kt
    ├── GoogleAuthManager.kt
    └── DriveServiceHelper.kt
```

---

## Google Apps Script (GAS)

Drive에 업로드된 영수증 이미지를 자동으로 분석·분류·기록하는 백엔드 자동화 스크립트입니다.

### 주요 기능

- 🤖 **Gemini AI 분석** — `gemini-2.5-flash` 모델로 영수증 이미지에서 거래처명·금액 자동 추출
- 🗂️ **계정과목 자동 분류** — 파일명의 용도 키워드를 기반으로 6가지 경비 항목으로 분류
- 📊 **Google Sheets 기록** — 분석 결과를 RAW 시트에 자동 적재 (중복 처리 방지)
- 📄 **월별 PDF 생성** — 해당 월 영수증 이미지를 날짜 순으로 병합한 PDF 자동 생성 (덮어쓰기)
- 🔔 **Slack 알림** — 처리 결과 및 이번 달·지난 달 합계를 Slack으로 보고

### 처리 흐름

```
트리거 실행 (수동 또는 시간 기반)
    ↓
Drive 폴더에서 이미지 파일 수집 → 날짜순 정렬
    ↓
미처리 파일 식별 (Sheets 기록 대조)
    ↓
Gemini API로 거래처명·금액 추출 → 계정과목 분류 → Sheets 기록
    ↓
이번 달·지난 달 PDF 갱신 (덮어쓰기)
    ↓
Slack으로 처리 결과 및 월간 현황 알림
```

### 계정과목 분류 기준

파일명의 용도(`{날짜}_{용도}.jpg`)에서 키워드를 추출해 아래 6가지 항목으로 분류합니다.

| 분류 항목 | 인식 키워드 |
|-----------|------------|
| 교통비 | 택시, 버스, 주차, 톨게이트, 하이패스, 대리, 주유, 출장, 복귀, 이동 |
| 접대비 | 미팅, 식사, 음료, 커피, 접대, 선물, 고객 |
| 야근 식대 | 야근 |
| 점심 식대 | 외근중식대, 점심, 중식, 식대 |
| 임직원 티미팅 | 간식, 티타임 |
| 수수료 | 수수료 |

### GAS 파일 구조

```
GAS/
├── Main.gs          # 메인 실행 흐름 및 스프레드시트 UI 메뉴 등록
├── BusinessLogic.gs # 파일명 파싱, 계정과목 분류
├── GeminiService.gs # Gemini API 호출 및 응답 파싱
├── DriveService.gs  # 이미지 파일 수집, PDF 생성 및 덮어쓰기
├── SheetService.gs  # Sheets 읽기·쓰기, 월별 합계 계산
└── SlackService.gs  # Slack Webhook 알림 발송
```

### 스크립트 속성 설정

GAS 프로젝트의 스크립트 속성(Script Properties)에 아래 값을 설정해야 합니다.

| 속성 키 | 설명 |
|---------|------|
| `GEMINI_API_KEY` | Google AI Studio에서 발급한 Gemini API 키 |
| `SLACK_WEBHOOK_URL` | Slack Incoming Webhook URL |
| `BASE_FOLDER_ID` | 영수증 이미지가 업로드되는 Google Drive 폴더 ID |
| `BASE_SHEET_ID` | 분석 결과를 기록할 Google Spreadsheet ID |
| `SLACK_USER_ID` | Slack 알림에서 멘션할 사용자 ID (선택) |

### Sheets 데이터 구조 (RAW 시트)

| 열 | 항목 |
|----|------|
| A | 분석 타임스탬프 |
| B | 원본 파일명 |
| C | 계정과목 |
| D | 지출일 |
| E | 용도 |
| F | 거래처명 |
| G | 금액 |

### 실행 방법

스프레드시트 상단 메뉴에서 **영수증 처리 → 📥 미처리 영수증 일괄 분석**을 클릭하거나, GAS 트리거를 설정해 자동 실행할 수 있습니다.

Drive API 고급 서비스를 활성화해야 PDF 생성 시 고화질 이미지 변환이 정상 작동합니다.

---

## 전체 프로젝트 구조

```
BILZ/
├── app/                    # Android 앱
│   └── src/main/
│       ├── java/com/bilz/app/
│       └── res/
├── GAS/                    # Google Apps Script
│   ├── Main.gs
│   ├── BusinessLogic.gs
│   ├── GeminiService.gs
│   ├── DriveService.gs
│   ├── SheetService.gs
│   └── SlackService.gs
├── gradle/
├── README.md
├── DEVELOPMENT.md
└── rules.md
```

---

## 버전 히스토리

### v1.0.0 (2026-02-01)
- Android 앱 전체 기능 구현 (카메라, 크롭, 저장, Drive 업로드)
- GAS 영수증 자동 분석 파이프라인 구현 (Gemini, Sheets, PDF, Slack)

---

## 라이선스

이 프로젝트는 MIT 라이선스를 따릅니다.
