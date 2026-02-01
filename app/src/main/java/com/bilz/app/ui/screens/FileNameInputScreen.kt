package com.bilz.app.ui.screens

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bilz.app.ui.theme.BILZTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 파일명 입력 화면 Composable
 * 
 * 크롭이 완료된 이미지의 미리보기를 표시하고,
 * 사용자가 파일명을 입력할 수 있는 화면입니다.
 * 
 * @param croppedImageUri 크롭된 이미지의 Uri
 * @param onConfirm 파일명 확정 시 호출되는 콜백 (입력된 파일명 전달)
 * @param onCancel 취소 버튼 클릭 시 호출되는 콜백
 * @param onRetake 다시 촬영 버튼 클릭 시 호출되는 콜백
 * @param modifier 레이아웃 수정자
 */
@Composable
fun FileNameInputScreen(
    croppedImageUri: Uri,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    
    // ============================================================
    // 상태 관리
    // ============================================================
    
    // 파일명 입력 상태 (기본값: 현재 날짜/시간 기반)
    var fileName by remember {
        mutableStateOf(generateDefaultFileName())
    }
    
    // 파일명 유효성 검사 에러 메시지
    var fileNameError by remember { mutableStateOf<String?>(null) }
    
    // 애니메이션 상태
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "alpha"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = tween(durationMillis = 400),
        label = "scale"
    )
    
    // ============================================================
    // 파일명 유효성 검사 함수
    // ============================================================
    
    fun validateFileName(name: String): String? {
        return when {
            name.isBlank() -> "파일명을 입력해주세요"
            name.length < 2 -> "파일명은 2자 이상이어야 합니다"
            name.length > 50 -> "파일명은 50자 이하여야 합니다"
            name.contains(Regex("[\\\\/:*?\"<>|]")) -> "사용할 수 없는 문자가 포함되어 있습니다"
            else -> null
        }
    }
    
    // ============================================================
    // UI 구성
    // ============================================================
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .alpha(alpha)
                .scale(scale),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상단 타이틀 및 취소 버튼
            TopBar(
                onCancel = onCancel
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 이미지 미리보기 카드
            ImagePreviewCard(
                imageUri = croppedImageUri
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 파일명 입력 필드
            FileNameInputField(
                fileName = fileName,
                onFileNameChange = { newName ->
                    fileName = newName
                    fileNameError = validateFileName(newName)
                },
                error = fileNameError,
                focusRequester = focusRequester,
                onDone = {
                    keyboardController?.hide()
                    if (validateFileName(fileName) == null) {
                        onConfirm(fileName)
                    }
                }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 하단 버튼들
            BottomButtons(
                onRetake = onRetake,
                onConfirm = {
                    val error = validateFileName(fileName)
                    if (error != null) {
                        fileNameError = error
                    } else {
                        onConfirm(fileName)
                    }
                },
                isConfirmEnabled = validateFileName(fileName) == null
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 상단 바 Composable
 */
@Composable
private fun TopBar(
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 타이틀
        Text(
            text = "파일명 입력",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // 취소 버튼
        IconButton(
            onClick = onCancel
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "취소",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 이미지 미리보기 카드 Composable
 */
@Composable
private fun ImagePreviewCard(
    imageUri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Coil을 사용한 이미지 로딩
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "크롭된 이미지 미리보기",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Fit
            )
        }
    }
}

/**
 * 파일명 입력 필드 Composable
 */
@Composable
private fun FileNameInputField(
    fileName: String,
    onFileNameChange: (String) -> Unit,
    error: String?,
    focusRequester: FocusRequester,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // 레이블
        Text(
            text = "저장할 파일명",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // 입력 필드
        OutlinedTextField(
            value = fileName,
            onValueChange = onFileNameChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            placeholder = {
                Text("파일명을 입력하세요")
            },
            suffix = {
                Text(
                    text = ".jpg",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            isError = error != null,
            supportingText = if (error != null) {
                {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                {
                    Text(
                        text = "파일은 Google Drive에 업로드됩니다",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onDone() }
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

/**
 * 하단 버튼 영역 Composable
 */
@Composable
private fun BottomButtons(
    onRetake: () -> Unit,
    onConfirm: () -> Unit,
    isConfirmEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 확인 버튼
        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = isConfirmEnabled,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "확인",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
        
        // 다시 촬영 버튼
        OutlinedButton(
            onClick = onRetake,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "다시 촬영",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/**
 * 기본 파일명 생성 함수
 * 
 * 현재 날짜와 시간을 기반으로 기본 파일명을 생성합니다.
 * 형식: BILZ_yyyyMMdd_HHmmss
 * 
 * @return 생성된 기본 파일명
 */
private fun generateDefaultFileName(): String {
    val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    return "BILZ_${dateFormat.format(Date())}"
}

/**
 * 파일명 입력 화면 미리보기
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun FileNameInputScreenPreview() {
    BILZTheme {
        FileNameInputScreen(
            croppedImageUri = Uri.EMPTY,
            onConfirm = {},
            onCancel = {},
            onRetake = {}
        )
    }
}

/**
 * 파일명 입력 화면 다크 테마 미리보기
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun FileNameInputScreenDarkPreview() {
    BILZTheme(darkTheme = true) {
        FileNameInputScreen(
            croppedImageUri = Uri.EMPTY,
            onConfirm = {},
            onCancel = {},
            onRetake = {}
        )
    }
}
