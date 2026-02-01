# BILZ 앱 ProGuard 규칙
# 
# 이 파일에서 프로젝트별 ProGuard 규칙을 추가할 수 있습니다.
# 기본적으로 아래 파일들의 규칙이 적용됩니다:
# - build/intermediates/proguard-files/proguard-android-optimize.txt (Android SDK)
# - proguard-rules.pro (이 파일)

# 자세한 내용은 아래 문서를 참고하세요:
# http://developer.android.com/guide/developing/tools/proguard.html

# ============================================================
# 공통 설정
# ============================================================
# 라인 번호 유지 (디버깅용)
-keepattributes SourceFile,LineNumberTable

# 어노테이션 유지
-keepattributes *Annotation*

# ============================================================
# Google API Client 라이브러리 규칙
# ============================================================
-keep class com.google.api.** { *; }
-keep class com.google.http.** { *; }
-dontwarn com.google.api.**
-dontwarn com.google.http.**

# ============================================================
# Google Play Services Auth 규칙
# ============================================================
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ============================================================
# Coil 이미지 로딩 라이브러리 규칙
# ============================================================
-keep class coil.** { *; }
-dontwarn coil.**

# ============================================================
# CanHub Image Cropper 규칙
# ============================================================
-keep class com.canhub.cropper.** { *; }
-dontwarn com.canhub.cropper.**

# ============================================================
# Kotlin Coroutines 규칙
# ============================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ============================================================
# Jetpack Compose 규칙
# ============================================================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ============================================================
# 데이터 클래스 유지 (JSON 직렬화/역직렬화용)
# ============================================================
# 앱에서 사용하는 데이터 클래스는 여기에 추가하세요
# -keep class com.bilz.app.model.** { *; }
