// BILZ Android 프로젝트 - 최상위 빌드 파일
// 이 파일에서는 모든 하위 프로젝트/모듈에 공통으로 적용되는 설정을 정의합니다.

plugins {
    // Android Application 플러그인 - 앱 빌드에 필요
    alias(libs.plugins.android.application) apply false
    
    // Kotlin Android 플러그인 - Kotlin 언어 지원
    alias(libs.plugins.kotlin.android) apply false
    
    // Kotlin Compose Compiler 플러그인 - Jetpack Compose 지원
    alias(libs.plugins.kotlin.compose) apply false
}
