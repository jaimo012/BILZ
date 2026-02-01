package com.bilz.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * BILZ 앱 테마 설정
 * 
 * Material Design 3 테마를 정의합니다.
 * 시스템 설정에 따라 라이트/다크 테마가 자동으로 적용됩니다.
 * Android 12 이상에서는 Dynamic Color를 지원합니다.
 */

// ============================================================
// 다크 테마 색상 스키마
// ============================================================
private val DarkColorScheme = darkColorScheme(
    primary = BilzPrimaryDark,
    secondary = BilzSecondaryDark,
    tertiary = Pink80,
    background = BilzBackgroundDark,
    surface = BilzSurfaceDark,
    onPrimary = BilzOnBackground,
    onSecondary = BilzOnBackground,
    onTertiary = BilzOnBackground,
    onBackground = BilzOnBackgroundDark,
    onSurface = BilzOnSurfaceDark
)

// ============================================================
// 라이트 테마 색상 스키마
// ============================================================
private val LightColorScheme = lightColorScheme(
    primary = BilzPrimary,
    secondary = BilzSecondary,
    tertiary = Pink40,
    background = BilzBackground,
    surface = BilzSurface,
    onPrimary = BilzOnPrimary,
    onSecondary = BilzOnSecondary,
    onTertiary = BilzOnPrimary,
    onBackground = BilzOnBackground,
    onSurface = BilzOnSurface
)

/**
 * BILZ 앱 테마 Composable
 * 
 * @param darkTheme 다크 테마 사용 여부 (기본값: 시스템 설정 따름)
 * @param dynamicColor 동적 색상 사용 여부 (Android 12+ 전용, 기본값: true)
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
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color는 Android 12+ 에서만 사용 가능
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // 색상 스키마 결정
    val colorScheme = when {
        // Android 12 이상이고 동적 색상이 활성화된 경우
        // 사용자의 배경화면 색상에 맞춰 앱 색상이 자동으로 조정됩니다.
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        // 다크 테마인 경우
        darkTheme -> DarkColorScheme
        // 라이트 테마인 경우
        else -> LightColorScheme
    }

    // MaterialTheme 적용
    // 이 테마 안의 모든 컴포넌트들은 정의된 색상, 타이포그래피, 형태를 사용합니다.
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
