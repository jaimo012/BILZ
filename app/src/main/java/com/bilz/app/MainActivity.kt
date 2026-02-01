package com.bilz.app

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.bilz.app.ui.screens.CameraScreen
import com.bilz.app.ui.screens.FileNameInputScreen
import com.bilz.app.ui.screens.HomeScreen
import com.bilz.app.ui.screens.PermissionScreen
import com.bilz.app.ui.theme.BILZTheme
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView

/**
 * BILZ 앱의 메인 액티비티
 * 
 * 앱의 진입점으로, 다음과 같은 흐름을 담당합니다:
 * 1. 앱 실행 시 필요한 권한(카메라, 저장소) 확인
 * 2. 권한이 없으면 권한 요청 화면 표시
 * 3. 권한이 모두 허용되면 홈 화면 표시
 * 4. 시작 버튼 클릭 시 카메라 화면으로 이동
 * 5. 촬영 완료 후 이미지 자르기 화면 표시
 * 6. 자르기 완료 후 파일명 입력 화면으로 이동
 */
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-Edge 디스플레이 활성화
        enableEdgeToEdge()
        
        setContent {
            BILZTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 앱의 메인 콘텐츠
                    BilzApp()
                }
            }
        }
    }
}

/**
 * 앱 화면 상태를 나타내는 sealed class
 * 
 * 각 화면 상태를 타입 안전하게 관리합니다.
 */
sealed class AppScreen {
    /** 권한 요청 화면 */
    data object Permission : AppScreen()
    
    /** 홈 화면 */
    data object Home : AppScreen()
    
    /** 카메라 촬영 화면 */
    data object Camera : AppScreen()
    
    /** 촬영 완료 - 크롭 대기 상태 (원본 이미지 Uri 포함) */
    data class WaitingForCrop(val originalImageUri: Uri) : AppScreen()
    
    /** 크롭 완료 - 파일명 입력 대기 상태 (크롭된 이미지 Uri 포함) */
    data class CropCompleted(val croppedImageUri: Uri) : AppScreen()
}

/**
 * BILZ 앱의 최상위 Composable
 * 
 * 권한 상태와 현재 화면 상태에 따라 적절한 화면을 표시합니다.
 */
@Composable
fun BilzApp() {
    // 현재 Context 가져오기
    val context = LocalContext.current
    
    // ============================================================
    // 권한 상태 관리
    // ============================================================
    
    // 카메라 권한 상태
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    // 저장소 권한 상태 (Android 13 이상에서는 READ_MEDIA_IMAGES 사용)
    var hasStoragePermission by remember {
        mutableStateOf(
            checkStoragePermission(context)
        )
    }
    
    // ============================================================
    // 화면 상태 관리
    // ============================================================
    
    // 현재 화면 상태
    var currentScreen by remember {
        mutableStateOf<AppScreen>(
            if (hasCameraPermission && hasStoragePermission) {
                AppScreen.Home
            } else {
                AppScreen.Permission
            }
        )
    }
    
    // ============================================================
    // 권한 요청 런처 설정
    // ============================================================
    
    // 여러 권한을 한 번에 요청하는 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // 카메라 권한 결과 처리
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
        
        // 저장소 권한 결과 처리 (Android 버전에 따라 다른 권한 확인)
        hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }
        
        // 모든 권한이 허용되면 홈 화면으로 이동
        if (hasCameraPermission && hasStoragePermission) {
            currentScreen = AppScreen.Home
        }
    }
    
    // ============================================================
    // 이미지 크롭 런처 설정 (CanHub Image Cropper)
    // ============================================================
    
    val cropImageLauncher = rememberLauncherForActivityResult(
        contract = CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            // 크롭 성공: 크롭된 이미지 Uri로 화면 전환
            result.uriContent?.let { croppedUri ->
                Log.d("MainActivity", "크롭 완료: $croppedUri")
                currentScreen = AppScreen.CropCompleted(croppedUri)
            } ?: run {
                // Uri가 null인 경우 (예외 상황)
                Log.e("MainActivity", "크롭 결과 Uri가 null입니다")
                Toast.makeText(context, "이미지 자르기 실패", Toast.LENGTH_SHORT).show()
                currentScreen = AppScreen.Home
            }
        } else {
            // 크롭 취소 또는 실패
            val error = result.error
            if (error != null) {
                Log.e("MainActivity", "크롭 실패", error)
                Toast.makeText(context, "이미지 자르기 실패: ${error.message}", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("MainActivity", "크롭 취소됨")
                Toast.makeText(context, "이미지 자르기가 취소되었습니다", Toast.LENGTH_SHORT).show()
            }
            // 홈 화면으로 이동
            currentScreen = AppScreen.Home
        }
    }
    
    // ============================================================
    // 크롭 대기 상태에서 크롭 화면 자동 실행
    // ============================================================
    
    // WaitingForCrop 상태가 되면 크롭 화면을 자동으로 실행
    LaunchedEffect(currentScreen) {
        val screen = currentScreen
        if (screen is AppScreen.WaitingForCrop) {
            // 크롭 옵션 설정
            val cropOptions = CropImageContractOptions(
                uri = screen.originalImageUri,
                cropImageOptions = CropImageOptions(
                    // 크롭 가이드라인 표시
                    guidelines = CropImageView.Guidelines.ON,
                    // 자유 비율 (사용자가 원하는 대로 자르기)
                    fixAspectRatio = false,
                    // 크롭 영역 최소 크기
                    minCropWindowWidth = 100,
                    minCropWindowHeight = 100,
                    // 크롭 영역 이동 가능
                    allowFlipping = true,
                    allowRotation = true,
                    // 출력 이미지 품질 (JPEG 압축률)
                    outputCompressQuality = 90,
                    // 출력 이미지 형식
                    outputCompressFormat = android.graphics.Bitmap.CompressFormat.JPEG
                )
            )
            
            // 크롭 화면 실행
            cropImageLauncher.launch(cropOptions)
        }
    }
    
    // ============================================================
    // 화면 표시 로직 (애니메이션 포함)
    // ============================================================
    
    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            // 화면 전환 애니메이션 설정
            when {
                // 홈 -> 카메라: 오른쪽에서 슬라이드
                initialState is AppScreen.Home && targetState is AppScreen.Camera -> {
                    (slideInHorizontally { width -> width } + fadeIn())
                        .togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                }
                // 카메라 -> 홈: 왼쪽에서 슬라이드
                initialState is AppScreen.Camera && targetState is AppScreen.Home -> {
                    (slideInHorizontally { width -> -width } + fadeIn())
                        .togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                }
                // 기타: 페이드 전환
                else -> {
                    fadeIn() togetherWith fadeOut()
                }
            }
        },
        label = "screenTransition"
    ) { screen ->
        when (screen) {
            // 권한 요청 화면
            is AppScreen.Permission -> {
                PermissionScreen(
                    hasCameraPermission = hasCameraPermission,
                    hasStoragePermission = hasStoragePermission,
                    onRequestPermissions = {
                        // 필요한 권한 목록 생성
                        val permissionsToRequest = buildRequiredPermissionsList(
                            hasCameraPermission = hasCameraPermission,
                            hasStoragePermission = hasStoragePermission
                        )
                        // 권한 요청 실행
                        permissionLauncher.launch(permissionsToRequest.toTypedArray())
                    }
                )
            }
            
            // 홈 화면
            is AppScreen.Home -> {
                HomeScreen(
                    onStartClick = {
                        // 카메라 화면으로 이동
                        currentScreen = AppScreen.Camera
                    }
                )
            }
            
            // 카메라 촬영 화면
            is AppScreen.Camera -> {
                CameraScreen(
                    onImageCaptured = { uri ->
                        // 촬영 성공: 크롭 대기 상태로 전환
                        // (LaunchedEffect에서 크롭 화면이 자동 실행됨)
                        Log.d("MainActivity", "이미지 촬영 완료: $uri")
                        currentScreen = AppScreen.WaitingForCrop(uri)
                    },
                    onError = { exception ->
                        // 촬영 실패: 에러 메시지 표시
                        Log.e("MainActivity", "촬영 실패", exception)
                        Toast.makeText(
                            context,
                            "촬영 실패: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onBackClick = {
                        // 뒤로가기: 홈 화면으로 이동
                        currentScreen = AppScreen.Home
                    }
                )
            }
            
            // 크롭 대기 상태 (로딩 화면 표시)
            is AppScreen.WaitingForCrop -> {
                // 크롭 화면이 열리는 동안 로딩 표시
                LoadingScreen(message = "이미지 편집 준비 중...")
            }
            
            // 크롭 완료 - 파일명 입력 화면
            is AppScreen.CropCompleted -> {
                FileNameInputScreen(
                    croppedImageUri = screen.croppedImageUri,
                    onConfirm = { fileName ->
                        // 파일명 확정
                        Log.d("MainActivity", "파일명 확정: $fileName")
                        Toast.makeText(
                            context,
                            "파일명: $fileName 으로 저장 준비 완료",
                            Toast.LENGTH_SHORT
                        ).show()
                        // TODO: 5단계에서 Google Drive 업로드 구현
                        currentScreen = AppScreen.Home
                    },
                    onCancel = {
                        // 취소: 홈으로 이동
                        currentScreen = AppScreen.Home
                    },
                    onRetake = {
                        // 다시 촬영: 카메라 화면으로 이동
                        currentScreen = AppScreen.Camera
                    }
                )
            }
        }
    }
}

/**
 * 로딩 화면 Composable
 * 
 * 작업이 진행 중일 때 표시되는 로딩 화면입니다.
 */
@Composable
private fun LoadingScreen(
    message: String,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            androidx.compose.material3.CircularProgressIndicator()
            
            androidx.compose.material3.Text(
                text = message,
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
            )
        }
    }
}

// dp 단위를 위한 import
private val dp = androidx.compose.ui.unit.dp

/**
 * 저장소 권한 확인 함수
 * 
 * Android 버전에 따라 다른 권한을 확인합니다:
 * - Android 13 (API 33) 이상: READ_MEDIA_IMAGES
 * - Android 13 미만: READ_EXTERNAL_STORAGE
 * 
 * @param context 현재 Context
 * @return 저장소 권한 허용 여부
 */
private fun checkStoragePermission(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Android 13 이상: READ_MEDIA_IMAGES 권한 확인
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        // Android 13 미만: READ_EXTERNAL_STORAGE 권한 확인
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * 필요한 권한 목록을 생성하는 함수
 * 
 * 아직 허용되지 않은 권한만 목록에 포함합니다.
 * 
 * @param hasCameraPermission 카메라 권한 보유 여부
 * @param hasStoragePermission 저장소 권한 보유 여부
 * @return 요청해야 할 권한 목록
 */
private fun buildRequiredPermissionsList(
    hasCameraPermission: Boolean,
    hasStoragePermission: Boolean
): List<String> {
    val permissions = mutableListOf<String>()
    
    // 카메라 권한이 없으면 추가
    if (!hasCameraPermission) {
        permissions.add(Manifest.permission.CAMERA)
    }
    
    // 저장소 권한이 없으면 추가 (Android 버전에 따라 다른 권한)
    if (!hasStoragePermission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    
    return permissions
}
