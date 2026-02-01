# BILZ 프로젝트 개발 규칙

## 📌 프로젝트 정보

- **프로젝트명**: BILZ
- **버전**: 1.0.0
- **플랫폼**: Android
- **최소 SDK**: 26 (Android 8.0 Oreo)
- **타겟 SDK**: 34 (Android 14)

## 🛠️ 기술 스택

### 핵심 기술
- **언어**: Kotlin 2.0.21
- **UI 프레임워크**: Jetpack Compose
- **비동기 처리**: Kotlin Coroutines

### 사용 라이브러리
| 라이브러리 | 버전 | 용도 |
|-----------|------|------|
| CameraX | 1.4.1 | 카메라 촬영 |
| Coil | 2.7.0 | 이미지 로딩 |
| Play Services Auth | 21.3.0 | Google 로그인 |
| Google API Client | 2.7.0 | Google Drive 연동 |
| Image Cropper | 4.6.0 | 이미지 자르기 |
| Coroutines | 1.9.0 | 비동기 처리 |

## 📁 디렉토리 구조 규칙

```
app/src/main/java/com/bilz/app/
├── MainActivity.kt          # 앱 진입점
├── ui/
│   ├── theme/              # 테마 관련 파일
│   ├── screens/            # 화면별 Composable
│   └── components/         # 재사용 가능한 컴포넌트
├── data/
│   ├── repository/         # 데이터 저장소
│   └── model/              # 데이터 모델
├── domain/
│   └── usecase/            # 비즈니스 로직
└── util/                   # 유틸리티 함수
```

## 🎨 코드 스타일 규칙

### 네이밍 컨벤션
- **클래스/인터페이스**: PascalCase (예: `MainActivity`, `UserRepository`)
- **함수/변수**: camelCase (예: `getUserData`, `userName`)
- **상수**: SCREAMING_SNAKE_CASE (예: `MAX_RETRY_COUNT`)
- **Composable 함수**: PascalCase (예: `HomeScreen`, `CustomButton`)

### Kotlin 스타일
- 불변 변수 우선 사용 (`val` > `var`)
- Nullable 타입 최소화
- 확장 함수 적극 활용
- data class 사용 권장

### Compose 스타일
- 상태 호이스팅 적용
- Preview 함수 작성 필수
- Modifier는 첫 번째 파라미터로 전달

## 🔒 보안 규칙

- API 키는 `local.properties`에 저장
- 민감한 정보 하드코딩 금지
- ProGuard 난독화 적용 (릴리즈 빌드)

## 📝 Git 커밋 메시지 규칙

```
<type>: <subject>

예시:
feat: 카메라 촬영 기능 추가
fix: 이미지 로딩 오류 수정
refactor: 로그인 로직 리팩토링
docs: README 업데이트
style: 코드 포맷팅
test: 단위 테스트 추가
```

## 🧪 테스트 규칙

- 비즈니스 로직은 단위 테스트 필수
- UI 테스트는 주요 화면 대상
- 테스트 커버리지 목표: 80% 이상

## 📱 브랜드 가이드

- **앱 이름**: BILZ
- **패키지명**: com.bilz.app
- **주 색상**: #1A73E8 (Google Blue)
- **보조 색상**: #03DAC6 (Teal)
