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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.bilz.app.ui.screens.CameraScreen
import com.bilz.app.ui.screens.FileNameInputScreen
import com.bilz.app.ui.screens.HomeScreen
import com.bilz.app.ui.screens.PermissionScreen
import com.bilz.app.ui.theme.BILZTheme
import com.bilz.app.util.DriveServiceHelper
import com.bilz.app.util.DriveUploadResult
import com.bilz.app.util.GoogleAuthManager
import com.bilz.app.util.GoogleSignInResult
import com.bilz.app.util.ImageSaveResult
import com.bilz.app.util.ImageSaver
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.launch

/**
 * BILZ 앱의 메인 액티비티
 * 
 * 앱의 진입점으로, 다음과 같은 흐름을 담당합니다:
 * 1. 앱 실행 시 필요한 권한(카메라, 저장소) 확인
 * 2. 권한이 없으면 권한 요청 화면 표시
 * 3. 권한이 모두 허용되면 홈 화면 표시
 * 4. 시작 버튼 클릭 시 카메라 화면으로 이동
 * 5. 촬영 완료 후 이미지 자르기 화면 표시
 * 6. 자르기 완료 후 파일명 입력 및 로컬 저장
 * 7. 저장 완료 후 Google Drive 업로드 (로그인 필요시 로그인 진행)
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
    
    /** 저장 중 상태 */
    data class Saving(
        val croppedImageUri: Uri,
        val fileName: String
    ) : AppScreen()
    
    /** 저장 완료 상태 */
    data class SaveCompleted(
        val savedUri: Uri,
        val fileName: String,
        val relativePath: String
    ) : AppScreen()
    
    /** 저장 실패 상태 */
    data class SaveFailed(
        val errorMessage: String,
        val croppedImageUri: Uri
    ) : AppScreen()
    
    /** Google 로그인 중 상태 */
    data class SigningIn(
        val savedUri: Uri,
        val fileName: String,
        val relativePath: String
    ) : AppScreen()
    
    /** Google 로그인 완료 - Drive 업로드 준비 상태 */
    data class ReadyToUpload(
        val savedUri: Uri,
        val fileName: String,
        val relativePath: String,
        val account: GoogleSignInAccount
    ) : AppScreen()
    
    /** Google Drive 업로드 중 상태 */
    data class Uploading(
        val savedUri: Uri,
        val fileName: String,
        val account: GoogleSignInAccount
    ) : AppScreen()
    
    /** Google Drive 업로드 완료 상태 */
    data class UploadCompleted(
        val fileName: String,
        val driveFileId: String,
        val webViewLink: String?
    ) : AppScreen()
    
    /** Google Drive 업로드 실패 상태 */
    data class UploadFailed(
        val errorMessage: String,
        val savedUri: Uri,
        val fileName: String,
        val account: GoogleSignInAccount
    ) : AppScreen()
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
    val coroutineScope = rememberCoroutineScope()
    
    // ============================================================
    // Google Auth Manager
    // ============================================================
    
    val googleAuthManager = remember { GoogleAuthManager(context) }
    
    // ============================================================
    // 로그인된 Google 계정 상태 (앱 전체에서 유지)
    // ============================================================
    
    var signedInAccount by remember { mutableStateOf<GoogleSignInAccount?>(null) }
    
    // 앱 시작 시 기존 로그인 상태 확인
    LaunchedEffect(Unit) {
        if (googleAuthManager.isSignedIn()) {
            signedInAccount = googleAuthManager.getCurrentAccount()
            Log.d("MainActivity", "기존 로그인 계정 복원: ${signedInAccount?.email}")
        }
    }
    
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
    // Google 로그인 런처 설정 (시작 버튼 클릭 시 사용)
    // ============================================================
    
    // 로그인 대기 상태 (로그인 완료 후 카메라로 이동하기 위한 플래그)
    var waitingForSignIn by remember { mutableStateOf(false) }
    
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // 로그인 결과 처리
        when (val signInResult = googleAuthManager.handleSignInResult(result.data)) {
            is GoogleSignInResult.Success -> {
                Log.d("MainActivity", "Google 로그인 성공: ${signInResult.account.email}")
                // 로그인된 계정 저장
                signedInAccount = signInResult.account
                
                // 시작 버튼에서 로그인 대기 중이었다면 카메라로 이동
                if (waitingForSignIn) {
                    waitingForSignIn = false
                    currentScreen = AppScreen.Camera
                }
            }
            is GoogleSignInResult.Cancelled -> {
                Log.d("MainActivity", "Google 로그인 취소됨")
                Toast.makeText(context, "로그인이 취소되었습니다", Toast.LENGTH_SHORT).show()
                waitingForSignIn = false
                // 홈 화면 유지
            }
            is GoogleSignInResult.Failure -> {
                Log.e("MainActivity", "Google 로그인 실패: ${signInResult.message}")
                Toast.makeText(context, "로그인 실패: ${signInResult.message}", Toast.LENGTH_SHORT).show()
                waitingForSignIn = false
                // 홈 화면 유지
            }
            is GoogleSignInResult.NeedSignIn -> {
                // 이 경우는 발생하지 않음 (이미 로그인 결과 처리 중)
            }
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
                    outputCompressFormat = android.graphics.Bitmap.CompressFormat.JPEG,
                    // 툴바 스타일 - 더 눈에 띄게 설정
                    toolbarColor = android.graphics.Color.parseColor("#00BCD4"),
                    toolbarTitleColor = android.graphics.Color.WHITE,
                    toolbarBackButtonColor = android.graphics.Color.WHITE,
                    activityBackgroundColor = android.graphics.Color.parseColor("#121212"),
                    // 저장 버튼을 텍스트로 표시 (더 눈에 띄게)
                    cropMenuCropButtonTitle = "저장"
                )
            )
            
            // 크롭 화면 실행
            cropImageLauncher.launch(cropOptions)
        }
    }
    
    // ============================================================
    // 저장 상태에서 이미지 저장 실행 -> 자동으로 업로드 진행
    // ============================================================
    
    LaunchedEffect(currentScreen) {
        val screen = currentScreen
        if (screen is AppScreen.Saving) {
            // 이미지 저장 실행
            val result = ImageSaver.saveImage(
                context = context,
                sourceUri = screen.croppedImageUri,
                fileName = screen.fileName
            )
            
            // 결과에 따라 화면 전환
            currentScreen = when (result) {
                is ImageSaveResult.Success -> {
                    Log.d("MainActivity", "이미지 저장 성공: ${result.savedUri}")
                    
                    // 이미 로그인되어 있으면 바로 업로드, 아니면 로컬 저장만 완료
                    val account = signedInAccount
                    if (account != null) {
                        Toast.makeText(context, "로컬 저장 완료! 드라이브 업로드 중...", Toast.LENGTH_SHORT).show()
                        // 바로 업로드 시작 (로그인 화면 생략)
                        AppScreen.Uploading(
                            savedUri = result.savedUri,
                            fileName = result.displayName,
                            account = account
                        )
                    } else {
                        // 로그인 안 됨 - 로컬 저장만 완료
                        Toast.makeText(context, "로컬 저장 완료! (로그인 필요)", Toast.LENGTH_SHORT).show()
                        AppScreen.SaveCompleted(
                            savedUri = result.savedUri,
                            fileName = result.displayName,
                            relativePath = result.relativePath
                        )
                    }
                }
                is ImageSaveResult.Failure -> {
                    Log.e("MainActivity", "이미지 저장 실패: ${result.message}")
                    AppScreen.SaveFailed(
                        errorMessage = result.message,
                        croppedImageUri = screen.croppedImageUri
                    )
                }
            }
        }
    }
    
    // ============================================================
    // 시작 버튼 클릭 핸들러 (로그인 확인 후 카메라로 이동)
    // ============================================================
    
    val onStartClick: () -> Unit = {
        // 이미 로그인되어 있으면 바로 카메라로 이동
        if (signedInAccount != null) {
            Log.d("MainActivity", "이미 로그인됨: ${signedInAccount?.email}, 카메라로 이동")
            currentScreen = AppScreen.Camera
        } else {
            // 로그인 시도
            when (val result = googleAuthManager.trySignIn()) {
                is GoogleSignInResult.Success -> {
                    // 이미 로그인되어 있음 -> 계정 저장 후 카메라로
                    Log.d("MainActivity", "기존 로그인 계정 사용: ${result.account.email}")
                    signedInAccount = result.account
                    currentScreen = AppScreen.Camera
                }
                is GoogleSignInResult.NeedSignIn -> {
                    // 로그인 화면 실행
                    Log.d("MainActivity", "로그인 화면 실행")
                    waitingForSignIn = true
                    googleSignInLauncher.launch(result.signInIntent)
                }
                else -> {
                    // Failure, Cancelled는 trySignIn에서 발생하지 않음
                }
            }
        }
    }
    
    // ============================================================
    // 업로드 상태에서 Google Drive 업로드 실행
    // ============================================================
    
    LaunchedEffect(currentScreen) {
        val screen = currentScreen
        if (screen is AppScreen.Uploading) {
            Log.d("MainActivity", "Google Drive 업로드 시작: ${screen.fileName}")
            
            try {
                // DriveServiceHelper 생성
                val driveHelper = DriveServiceHelper(context, screen.account)
                
                // Content Uri에서 직접 업로드
                val result = driveHelper.uploadFromContentUri(
                    contentUri = screen.savedUri,
                    fileName = screen.fileName,
                    mimeType = DriveServiceHelper.MIME_TYPE_JPEG,
                    folderId = DriveServiceHelper.BILZ_FOLDER_ID
                )
                
                // 결과에 따라 처리
                when (result) {
                    is DriveUploadResult.Success -> {
                        Log.d("MainActivity", "업로드 성공: ${result.fileId}")
                        // 성공 Toast 표시 후 카메라로 자동 복귀
                        Toast.makeText(
                            context,
                            "✅ 저장 및 업로드 완료!",
                            Toast.LENGTH_LONG
                        ).show()
                        currentScreen = AppScreen.Camera
                    }
                    is DriveUploadResult.Failure -> {
                        Log.e("MainActivity", "업로드 실패: ${result.message}")
                        currentScreen = AppScreen.UploadFailed(
                            errorMessage = result.message,
                            savedUri = screen.savedUri,
                            fileName = screen.fileName,
                            account = screen.account
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "업로드 중 예외 발생", e)
                currentScreen = AppScreen.UploadFailed(
                    errorMessage = "업로드 중 오류 발생: ${e.message}",
                    savedUri = screen.savedUri,
                    fileName = screen.fileName,
                    account = screen.account
                )
            }
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
                    onStartClick = onStartClick,
                    isSignedIn = signedInAccount != null,
                    signedInEmail = signedInAccount?.email
                )
            }
            
            // 카메라 촬영 화면
            is AppScreen.Camera -> {
                CameraScreen(
                    onImageCaptured = { uri ->
                        // 촬영 성공: 크롭 대기 상태로 전환
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
                LoadingScreen(message = "이미지 편집 준비 중...")
            }
            
            // 크롭 완료 - 파일명 입력 화면
            is AppScreen.CropCompleted -> {
                FileNameInputScreen(
                    croppedImageUri = screen.croppedImageUri,
                    onConfirm = { fileName ->
                        // 파일명 확정 -> 저장 시작
                        Log.d("MainActivity", "저장 시작: $fileName")
                        currentScreen = AppScreen.Saving(
                            croppedImageUri = screen.croppedImageUri,
                            fileName = fileName
                        )
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
            
            // 저장 중 화면
            is AppScreen.Saving -> {
                LoadingScreen(message = "이미지 저장 중...")
            }
            
            // 저장 완료 화면
            is AppScreen.SaveCompleted -> {
                SaveCompletedScreen(
                    fileName = screen.fileName,
                    relativePath = screen.relativePath,
                    isSignedIn = googleAuthManager.isSignedIn(),
                    signedInEmail = googleAuthManager.getEmail(),
                    onUploadToDriveClick = {
                        // Google Drive 업로드 시작 -> 로그인 확인
                        currentScreen = AppScreen.SigningIn(
                            savedUri = screen.savedUri,
                            fileName = screen.fileName,
                            relativePath = screen.relativePath
                        )
                    },
                    onHomeClick = {
                        currentScreen = AppScreen.Home
                    },
                    onTakeAnotherClick = {
                        currentScreen = AppScreen.Camera
                    }
                )
            }
            
            // 저장 실패 화면
            is AppScreen.SaveFailed -> {
                SaveFailedScreen(
                    errorMessage = screen.errorMessage,
                    onRetryClick = {
                        // 파일명 입력 화면으로 돌아가기
                        currentScreen = AppScreen.CropCompleted(screen.croppedImageUri)
                    },
                    onHomeClick = {
                        currentScreen = AppScreen.Home
                    }
                )
            }
            
            // Google 로그인 중 화면
            is AppScreen.SigningIn -> {
                LoadingScreen(message = "Google 로그인 중...")
            }
            
            // 업로드 준비 완료 화면
            is AppScreen.ReadyToUpload -> {
                ReadyToUploadScreen(
                    fileName = screen.fileName,
                    relativePath = screen.relativePath,
                    accountEmail = screen.account.email ?: "알 수 없음",
                    onUploadClick = {
                        // 업로드 시작
                        currentScreen = AppScreen.Uploading(
                            savedUri = screen.savedUri,
                            fileName = screen.fileName,
                            account = screen.account
                        )
                    },
                    onCancelClick = {
                        currentScreen = AppScreen.SaveCompleted(
                            savedUri = screen.savedUri,
                            fileName = screen.fileName,
                            relativePath = screen.relativePath
                        )
                    },
                    onHomeClick = {
                        currentScreen = AppScreen.Home
                    }
                )
            }
            
            // 업로드 중 화면
            is AppScreen.Uploading -> {
                LoadingScreen(message = "Google Drive에 업로드 중...")
            }
            
            // 업로드 완료 화면
            is AppScreen.UploadCompleted -> {
                UploadCompletedScreen(
                    fileName = screen.fileName,
                    driveFileId = screen.driveFileId,
                    webViewLink = screen.webViewLink,
                    onTakeAnotherClick = {
                        currentScreen = AppScreen.Camera
                    },
                    onHomeClick = {
                        currentScreen = AppScreen.Home
                    }
                )
            }
            
            // 업로드 실패 화면
            is AppScreen.UploadFailed -> {
                UploadFailedScreen(
                    errorMessage = screen.errorMessage,
                    onRetryClick = {
                        // 재시도: 업로드 다시 시작
                        currentScreen = AppScreen.Uploading(
                            savedUri = screen.savedUri,
                            fileName = screen.fileName,
                            account = screen.account
                        )
                    },
                    onHomeClick = {
                        currentScreen = AppScreen.Home
                    }
                )
            }
        }
    }
}

/**
 * 로딩 화면 Composable
 */
@Composable
private fun LoadingScreen(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * 저장 완료 화면 Composable (Google Drive 업로드 버튼 포함)
 */
@Composable
private fun SaveCompletedScreen(
    fileName: String,
    relativePath: String,
    isSignedIn: Boolean,
    signedInEmail: String?,
    onUploadToDriveClick: () -> Unit,
    onHomeClick: () -> Unit,
    onTakeAnotherClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 성공 아이콘
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "저장 완료",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            // 성공 메시지
            Text(
                text = "저장 완료!",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // 파일 정보 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 파일명
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "파일명",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // 저장 위치
                    Text(
                        text = "저장 위치",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = relativePath,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Google Drive 업로드 버튼
            Button(
                onClick = onUploadToDriveClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Google Drive에 업로드",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (isSignedIn && signedInEmail != null) {
                        Text(
                            text = signedInEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            // 다른 영수증 촬영 버튼
            Button(
                onClick = onTakeAnotherClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "다른 영수증 촬영",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            OutlinedButton(
                onClick = onHomeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "홈으로",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

/**
 * 업로드 준비 완료 화면 Composable
 */
@Composable
private fun ReadyToUploadScreen(
    fileName: String,
    relativePath: String,
    accountEmail: String,
    onUploadClick: () -> Unit,
    onCancelClick: () -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 클라우드 아이콘
            Icon(
                imageVector = Icons.Default.CloudUpload,
                contentDescription = "업로드 준비",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            
            // 제목
            Text(
                text = "Google Drive 업로드",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // 정보 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 로그인된 계정
                    Text(
                        text = "로그인 계정",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = accountEmail,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // 업로드할 파일
                    Text(
                        text = "업로드할 파일",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 업로드 버튼
            Button(
                onClick = onUploadClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "업로드 시작",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "취소",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            OutlinedButton(
                onClick = onHomeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "홈으로",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

/**
 * 저장 실패 화면 Composable
 */
@Composable
private fun SaveFailedScreen(
    errorMessage: String,
    onRetryClick: () -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 에러 아이콘
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "저장 실패",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.error
            )
            
            // 실패 메시지
            Text(
                text = "저장 실패",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // 에러 상세
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 버튼들
            Button(
                onClick = onRetryClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "다시 시도",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            OutlinedButton(
                onClick = onHomeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "홈으로",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

/**
 * Google Drive 업로드 완료 화면 Composable
 */
@Composable
private fun UploadCompletedScreen(
    fileName: String,
    driveFileId: String,
    webViewLink: String?,
    onTakeAnotherClick: () -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 성공 아이콘
            Icon(
                imageVector = Icons.Default.CloudUpload,
                contentDescription = "업로드 완료",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            // 성공 메시지
            Text(
                text = "업로드 완료!",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // 파일 정보 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 파일명
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "업로드된 파일",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Drive 파일 ID
                    Text(
                        text = "Drive 파일 ID",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = driveFileId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // 웹 링크 (있는 경우)
                    if (webViewLink != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Google Drive에서 확인 가능",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 버튼들
            Button(
                onClick = onTakeAnotherClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "다른 영수증 촬영",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            OutlinedButton(
                onClick = onHomeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "홈으로",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

/**
 * Google Drive 업로드 실패 화면 Composable
 */
@Composable
private fun UploadFailedScreen(
    errorMessage: String,
    onRetryClick: () -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 에러 아이콘
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "업로드 실패",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.error
            )
            
            // 실패 메시지
            Text(
                text = "업로드 실패",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // 에러 상세
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 버튼들
            Button(
                onClick = onRetryClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "다시 시도",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            OutlinedButton(
                onClick = onHomeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "홈으로",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

/**
 * 저장소 권한 확인 함수
 */
private fun checkStoragePermission(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * 필요한 권한 목록을 생성하는 함수
 */
private fun buildRequiredPermissionsList(
    hasCameraPermission: Boolean,
    hasStoragePermission: Boolean
): List<String> {
    val permissions = mutableListOf<String>()
    
    if (!hasCameraPermission) {
        permissions.add(Manifest.permission.CAMERA)
    }
    
    if (!hasStoragePermission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    
    return permissions
}
