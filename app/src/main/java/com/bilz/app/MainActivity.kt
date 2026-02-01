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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.bilz.app.ui.screens.CameraScreen
import com.bilz.app.ui.screens.HomeScreen
import com.bilz.app.ui.screens.PermissionScreen
import com.bilz.app.ui.theme.BILZTheme

/**
 * BILZ 앱의 메인 액티비티
 * 
 * 앱의 진입점으로, 다음과 같은 흐름을 담당합니다:
 * 1. 앱 실행 시 필요한 권한(카메라, 저장소) 확인
 * 2. 권한이 없으면 권한 요청 화면 표시
 * 3. 권한이 모두 허용되면 홈 화면 표시
 * 4. 시작 버튼 클릭 시 카메라 화면으로 이동
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
    
    /** 촬영 완료 화면 (촬영된 이미지 Uri 포함) */
    data class PhotoCaptured(val imageUri: Uri) : AppScreen()
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
    
    // 촬영된 이미지 Uri (임시 저장)
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    
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
                        // 촬영 성공: Uri 저장 및 화면 전환
                        capturedImageUri = uri
                        Log.d("MainActivity", "이미지 촬영 완료: $uri")
                        
                        // TODO: 4단계에서 크롭 화면으로 이동
                        // 현재는 토스트 메시지 표시 후 촬영 완료 화면으로 이동
                        Toast.makeText(
                            context,
                            "촬영 완료!",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        currentScreen = AppScreen.PhotoCaptured(uri)
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
            
            // 촬영 완료 화면 (임시)
            is AppScreen.PhotoCaptured -> {
                // TODO: 4단계에서 크롭 화면으로 교체
                // 현재는 촬영된 이미지 정보를 표시하는 임시 화면
                PhotoCapturedScreen(
                    imageUri = screen.imageUri,
                    onRetakeClick = {
                        // 다시 촬영
                        currentScreen = AppScreen.Camera
                    },
                    onHomeClick = {
                        // 홈으로 이동
                        currentScreen = AppScreen.Home
                    }
                )
            }
        }
    }
}

/**
 * 촬영 완료 임시 화면 Composable
 * 
 * 촬영된 이미지 정보를 표시하고, 다시 촬영 또는 홈으로 이동할 수 있습니다.
 * TODO: 4단계에서 크롭 화면으로 교체 예정
 */
@Composable
private fun PhotoCapturedScreen(
    imageUri: Uri,
    onRetakeClick: () -> Unit,
    onHomeClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 촬영 완료 아이콘
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "촬영 완료",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            // 완료 메시지
            Text(
                text = "촬영이 완료되었습니다!",
                style = MaterialTheme.typography.headlineSmall
            )
            
            // 파일 경로
            Text(
                text = "저장 경로: ${imageUri.lastPathSegment}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 버튼들
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 다시 촬영 버튼
                OutlinedButton(
                    onClick = onRetakeClick
                ) {
                    Text("다시 촬영")
                }
                
                // 홈으로 버튼
                Button(
                    onClick = onHomeClick
                ) {
                    Text("홈으로")
                }
            }
        }
    }
}

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
