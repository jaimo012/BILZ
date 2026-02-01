package com.bilz.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * BILZ 앱 컬러 팔레트
 * 
 * 어두운 배경의 깔끔한 스타일을 위한 다크 테마 중심 색상 시스템
 * 
 * 색상 명명 규칙:
 * - Primary: 앱의 주요 브랜드 색상 (액션, 강조)
 * - Secondary: 보조 색상 (성공, 완료)
 * - Tertiary: 세 번째 색상 (정보)
 * - Background: 배경색
 * - Surface: 카드, 시트 등의 표면 색상
 * - Error: 오류 상태 색상
 */

// ============================================================
// BILZ 다크 테마 색상 (메인 색상 팔레트)
// ============================================================

// 배경 색상 계열 - 깊은 다크 그레이
val BilzDarkBackground = Color(0xFF0D0D0D)       // 가장 어두운 배경
val BilzDarkSurface = Color(0xFF1A1A1A)          // 카드, 시트 배경
val BilzDarkSurfaceVariant = Color(0xFF252525)   // 입력 필드, 선택 영역
val BilzDarkSurfaceHigh = Color(0xFF2D2D2D)      // 강조 표면

// 주요 색상 - 시안 블루 (액션, 버튼)
val BilzPrimaryDark = Color(0xFF00BCD4)          // 시안 - 주요 액션
val BilzPrimaryVariantDark = Color(0xFF00ACC1)   // 시안 변형
val BilzOnPrimaryDark = Color(0xFF000000)        // 주요 색상 위 텍스트

// 보조 색상 - 민트 그린 (성공, 완료)
val BilzSecondaryDark = Color(0xFF4CAF50)        // 그린 - 성공
val BilzSecondaryVariantDark = Color(0xFF43A047) // 그린 변형
val BilzOnSecondaryDark = Color(0xFF000000)      // 보조 색상 위 텍스트

// 세 번째 색상 - 앰버 (정보, 경고)
val BilzTertiaryDark = Color(0xFFFFC107)         // 앰버 - 정보/경고
val BilzOnTertiaryDark = Color(0xFF000000)       // 세 번째 색상 위 텍스트

// 텍스트/아이콘 색상
val BilzOnBackgroundDark = Color(0xFFE8E8E8)     // 배경 위 주요 텍스트
val BilzOnSurfaceDark = Color(0xFFE8E8E8)        // 표면 위 주요 텍스트
val BilzOnSurfaceVariantDark = Color(0xFFAAAAAA) // 표면 위 보조 텍스트

// 에러 색상
val BilzErrorDark = Color(0xFFCF6679)            // 에러 핑크
val BilzOnErrorDark = Color(0xFF000000)          // 에러 색상 위 텍스트
val BilzErrorContainerDark = Color(0xFF93000A)   // 에러 컨테이너

// 아웃라인/구분선
val BilzOutlineDark = Color(0xFF3D3D3D)          // 아웃라인
val BilzOutlineVariantDark = Color(0xFF2A2A2A)   // 약한 아웃라인

// ============================================================
// 추가 유틸리티 색상
// ============================================================

// 투명도 색상
val BilzScrim = Color(0xFF000000)                // 스크림 (오버레이)
val BilzInverseSurface = Color(0xFFE8E8E8)       // 역 표면
val BilzInverseOnSurface = Color(0xFF1A1A1A)     // 역 표면 위 텍스트
val BilzInversePrimary = Color(0xFF006874)       // 역 주요 색상

// 상태 색상
val BilzSuccess = Color(0xFF4CAF50)              // 성공 그린
val BilzWarning = Color(0xFFFFC107)              // 경고 앰버
val BilzInfo = Color(0xFF2196F3)                 // 정보 블루

// ============================================================
// 레거시 색상 (호환성 유지)
// ============================================================
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// 라이트 테마 (필요시 사용)
val BilzPrimary = Color(0xFF006874)
val BilzPrimaryVariant = Color(0xFF004D57)
val BilzOnPrimary = Color(0xFFFFFFFF)

val BilzSecondary = Color(0xFF2E7D32)
val BilzSecondaryVariant = Color(0xFF1B5E20)
val BilzOnSecondary = Color(0xFFFFFFFF)

val BilzBackground = Color(0xFFFAFAFA)
val BilzOnBackground = Color(0xFF1C1B1F)

val BilzSurface = Color(0xFFFFFFFF)
val BilzOnSurface = Color(0xFF1C1B1F)

val BilzError = Color(0xFFB00020)
val BilzOnError = Color(0xFFFFFFFF)
