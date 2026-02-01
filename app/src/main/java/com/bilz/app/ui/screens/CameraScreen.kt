package com.bilz.app.ui.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.bilz.app.ui.theme.BILZTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 카메라 촬영 화면 Composable
 * 
 * CameraX를 사용하여 전체 화면 카메라 미리보기를 표시하고,
 * 촬영 버튼을 통해 이미지를 캡처합니다.
 * 
 * @param onImageCaptured 이미지 촬영 성공 시 호출되는 콜백 (촬영된 이미지의 Uri 전달)
 * @param onError 에러 발생 시 호출되는 콜백
 * @param onBackClick 뒤로가기 버튼 클릭 시 호출되는 콜백
 * @param modifier 레이아웃 수정자
 */
@Composable
fun CameraScreen(
    onImageCaptured: (Uri) -> Unit,
    onError: (Exception) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    
    // ============================================================
    // 상태 관리
    // ============================================================
    
    // 카메라 선택 상태 (전면/후면)
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    
    // 촬영 중 상태
    var isCapturing by remember { mutableStateOf(false) }
    
    // 셔터 애니메이션 상태
    var showShutterEffect by remember { mutableStateOf(false) }
    
    // ImageCapture 인스턴스 (촬영에 사용)
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }
    
    // PreviewView 인스턴스
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    
    // ============================================================
    // 카메라 초기화
    // ============================================================
    
    LaunchedEffect(lensFacing) {
        val cameraProvider = context.getCameraProvider()
        
        // 이전 카메라 바인딩 해제
        cameraProvider.unbindAll()
        
        // 미리보기 설정
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        
        // 카메라 선택 (전면/후면)
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
        
        try {
            // 카메라 바인딩
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            Log.e("CameraScreen", "카메라 바인딩 실패", e)
            onError(e)
        }
    }
    
    // 화면 종료 시 카메라 해제
    DisposableEffect(Unit) {
        onDispose {
            // 필요 시 정리 작업
        }
    }
    
    // 셔터 효과 애니메이션
    val shutterScale by animateFloatAsState(
        targetValue = if (showShutterEffect) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "shutterScale",
        finishedListener = {
            showShutterEffect = false
        }
    )
    
    // ============================================================
    // UI 구성
    // ============================================================
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 카메라 미리보기
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .scale(shutterScale)
        )
        
        // 셔터 효과 (촬영 시 화면 깜빡임)
        if (showShutterEffect) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
        
        // 상단 컨트롤 바
        TopControlBar(
            onBackClick = onBackClick,
            onSwitchCamera = {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        )
        
        // 하단 촬영 버튼
        BottomCaptureBar(
            isCapturing = isCapturing,
            onCaptureClick = {
                if (!isCapturing) {
                    isCapturing = true
                    showShutterEffect = true
                    
                    coroutineScope.launch {
                        try {
                            val uri = captureImage(
                                context = context,
                                imageCapture = imageCapture
                            )
                            onImageCaptured(uri)
                        } catch (e: Exception) {
                            Log.e("CameraScreen", "촬영 실패", e)
                            onError(e)
                        } finally {
                            isCapturing = false
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        )
    }
}

/**
 * 상단 컨트롤 바 Composable
 * 
 * 뒤로가기 버튼과 카메라 전환 버튼이 포함됩니다.
 */
@Composable
private fun TopControlBar(
    onBackClick: () -> Unit,
    onSwitchCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(16.dp)
            .padding(top = 32.dp), // 상태바 여백
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 뒤로가기 버튼
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "뒤로가기",
                tint = Color.White
            )
        }
        
        // 카메라 전환 버튼
        IconButton(
            onClick = onSwitchCamera,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.Default.Cameraswitch,
                contentDescription = "카메라 전환",
                tint = Color.White
            )
        }
    }
}

/**
 * 하단 촬영 버튼 바 Composable
 * 
 * 촬영 버튼이 중앙에 위치합니다.
 */
@Composable
private fun BottomCaptureBar(
    isCapturing: Boolean,
    onCaptureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(bottom = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        // 촬영 버튼
        CaptureButton(
            isCapturing = isCapturing,
            onClick = onCaptureClick
        )
    }
}

/**
 * 촬영 버튼 Composable
 * 
 * 원형의 촬영 버튼입니다. 촬영 중에는 로딩 표시가 나타납니다.
 */
@Composable
private fun CaptureButton(
    isCapturing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 버튼 눌림 애니메이션
    var isPressed by remember { mutableStateOf(false) }
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "buttonScale"
    )
    
    Box(
        modifier = modifier
            .size(80.dp)
            .scale(buttonScale),
        contentAlignment = Alignment.Center
    ) {
        // 외부 테두리
        Box(
            modifier = Modifier
                .size(80.dp)
                .border(
                    width = 4.dp,
                    color = Color.White,
                    shape = CircleShape
                )
        )
        
        // 내부 버튼
        FloatingActionButton(
            onClick = {
                if (!isCapturing) {
                    isPressed = true
                    onClick()
                    // 애니메이션 후 원래대로
                    isPressed = false
                }
            },
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            containerColor = if (isCapturing) Color.Gray else Color.White,
            contentColor = Color.Black
        ) {
            if (isCapturing) {
                // 촬영 중 로딩 표시
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = Color.White,
                    strokeWidth = 3.dp
                )
            }
            // 촬영 대기 상태에서는 빈 원형 버튼
        }
    }
}

/**
 * 이미지 촬영 함수 (suspend)
 * 
 * ImageCapture를 사용하여 이미지를 촬영하고 캐시 디렉토리에 저장합니다.
 * 
 * @param context Android Context
 * @param imageCapture ImageCapture 인스턴스
 * @return 저장된 이미지의 Uri
 * @throws ImageCaptureException 촬영 실패 시
 */
private suspend fun captureImage(
    context: Context,
    imageCapture: ImageCapture
): Uri = suspendCancellableCoroutine { continuation ->
    
    // 캐시 디렉토리에 임시 파일 생성
    val photoFile = createTempImageFile(context)
    
    // 출력 옵션 설정
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    
    // 메인 스레드 Executor
    val executor = ContextCompat.getMainExecutor(context)
    
    // 이미지 촬영
    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                // 촬영 성공 - 저장된 파일의 Uri 반환
                val savedUri = Uri.fromFile(photoFile)
                Log.d("CameraScreen", "이미지 저장 성공: $savedUri")
                continuation.resume(savedUri)
            }
            
            override fun onError(exception: ImageCaptureException) {
                // 촬영 실패
                Log.e("CameraScreen", "이미지 촬영 실패", exception)
                // 실패 시 임시 파일 삭제
                photoFile.delete()
                continuation.resumeWithException(exception)
            }
        }
    )
    
    // 취소 시 임시 파일 삭제
    continuation.invokeOnCancellation {
        photoFile.delete()
    }
}

/**
 * 임시 이미지 파일 생성 함수
 * 
 * 캐시 디렉토리에 고유한 이름의 임시 이미지 파일을 생성합니다.
 * 
 * @param context Android Context
 * @return 생성된 임시 파일
 */
private fun createTempImageFile(context: Context): File {
    // 타임스탬프를 포함한 파일명 생성
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        .format(System.currentTimeMillis())
    val fileName = "BILZ_${timestamp}.jpg"
    
    // 캐시 디렉토리에 파일 생성
    val cacheDir = context.cacheDir
    return File(cacheDir, fileName)
}

/**
 * CameraProvider를 비동기로 가져오는 확장 함수
 * 
 * @return ProcessCameraProvider 인스턴스
 */
private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { continuation ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener(
            {
                try {
                    continuation.resume(cameraProviderFuture.get())
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            },
            ContextCompat.getMainExecutor(this)
        )
    }

/**
 * 카메라 화면 미리보기
 * 
 * 참고: 실제 카메라는 미리보기에서 작동하지 않습니다.
 */
@ComposePreview(showBackground = true)
@Composable
fun CameraScreenPreview() {
    BILZTheme {
        // 미리보기용 더미 UI
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray)
        ) {
            // 상단 바
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(top = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
            
            // 중앙 텍스트
            Text(
                text = "카메라 미리보기",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
            
            // 하단 촬영 버튼
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                CaptureButton(
                    isCapturing = false,
                    onClick = {}
                )
            }
        }
    }
}
