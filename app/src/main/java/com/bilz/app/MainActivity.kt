package com.bilz.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
 * BILZ 앱의 최상위 Composable
 * 
 * 권한 상태에 따라 적절한 화면을 표시합니다.
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
    }
    
    // ============================================================
    // 화면 표시 로직
    // ============================================================
    
    // 모든 권한이 허용되었는지 확인
    val allPermissionsGranted = hasCameraPermission && hasStoragePermission
    
    if (allPermissionsGranted) {
        // 권한이 모두 허용되면 홈 화면 표시
        HomeScreen(
            onStartClick = {
                // TODO: 다음 화면으로 이동 (3단계에서 구현)
            }
        )
    } else {
        // 권한이 없으면 권한 요청 화면 표시
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
