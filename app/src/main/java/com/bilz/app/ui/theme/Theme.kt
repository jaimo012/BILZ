package com.bilz.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * BILZ 앱 테마 설정
 * 
 * 어두운 배경의 깔끔한 스타일을 위한 다크 테마 중심 디자인
 * - 깊은 다크 그레이 배경
 * - 시안 블루 액센트
 * - 민트 그린 성공 상태
 * - 높은 가독성의 텍스트 대비
 */

// ============================================================
// 다크 테마 색상 스키마 (기본)
// ============================================================
private val DarkColorScheme = darkColorScheme(
    // Primary - 시안 블루 (주요 액션)
    primary = BilzPrimaryDark,
    onPrimary = BilzOnPrimaryDark,
    primaryContainer = Color(0xFF004D57),
    onPrimaryContainer = Color(0xFF97F0FF),
    
    // Secondary - 민트 그린 (성공, 완료)
    secondary = BilzSecondaryDark,
    onSecondary = BilzOnSecondaryDark,
    secondaryContainer = Color(0xFF1B5E20),
    onSecondaryContainer = Color(0xFFA5D6A7),
    
    // Tertiary - 앰버 (정보, 경고)
    tertiary = BilzTertiaryDark,
    onTertiary = BilzOnTertiaryDark,
    tertiaryContainer = Color(0xFF5D4200),
    onTertiaryContainer = Color(0xFFFFE082),
    
    // Background & Surface - 깊은 다크 그레이
    background = BilzDarkBackground,
    onBackground = BilzOnBackgroundDark,
    surface = BilzDarkSurface,
    onSurface = BilzOnSurfaceDark,
    surfaceVariant = BilzDarkSurfaceVariant,
    onSurfaceVariant = BilzOnSurfaceVariantDark,
    
    // Error - 소프트 레드
    error = BilzErrorDark,
    onError = BilzOnErrorDark,
    errorContainer = BilzErrorContainerDark,
    onErrorContainer = Color(0xFFFFDAD6),
    
    // Outline
    outline = BilzOutlineDark,
    outlineVariant = BilzOutlineVariantDark,
    
    // Inverse
    inverseSurface = BilzInverseSurface,
    inverseOnSurface = BilzInverseOnSurface,
    inversePrimary = BilzInversePrimary,
    
    // Scrim
    scrim = BilzScrim
)

// ============================================================
// 라이트 테마 색상 스키마 (필요시 사용)
// ============================================================
private val LightColorScheme = lightColorScheme(
    primary = BilzPrimary,
    onPrimary = BilzOnPrimary,
    primaryContainer = Color(0xFFB2EBF2),
    onPrimaryContainer = Color(0xFF002022),
    
    secondary = BilzSecondary,
    onSecondary = BilzOnSecondary,
    secondaryContainer = Color(0xFFC8E6C9),
    onSecondaryContainer = Color(0xFF002106),
    
    tertiary = Color(0xFFF57C00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE0B2),
    onTertiaryContainer = Color(0xFF331200),
    
    background = BilzBackground,
    onBackground = BilzOnBackground,
    surface = BilzSurface,
    onSurface = BilzOnSurface,
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF49454F),
    
    error = BilzError,
    onError = BilzOnError,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Color(0xFF80DEEA),
    
    scrim = Color(0xFF000000)
)

/**
 * BILZ 앱 테마 Composable
 * 
 * @param darkTheme 다크 테마 사용 여부 (기본값: true - 항상 다크 테마)
 * @param content 테마가 적용될 콘텐츠
 * 
 * 사용 예시:
 * ```kotlin
 * BILZTheme {
 *     // 여기에 UI 컴포넌트들이 들어갑니다
 *     Text("Hello BILZ!")
 * }
 * ```
 */
@Composable
fun BILZTheme(
    // 기본값을 true로 설정하여 항상 다크 테마 사용
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    // 항상 다크 테마 사용 (깔끔한 어두운 스타일)
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    // 시스템 바 색상 설정
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            
            // 상태바와 네비게이션 바를 배경색과 동일하게
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            
            // 시스템 바 아이콘 색상 설정 (다크 테마에서는 밝은 아이콘)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    // MaterialTheme 적용
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
