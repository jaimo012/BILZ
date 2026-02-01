package com.bilz.app.ui.screens

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bilz.app.ui.components.ExpenseInputDialog
import com.bilz.app.ui.theme.BILZTheme

/**
 * 파일명 입력 화면 Composable
 * 
 * 크롭이 완료된 이미지의 미리보기를 표시하고,
 * 지출 용도 입력 다이얼로그를 통해 파일명을 설정합니다.
 * 
 * @param croppedImageUri 크롭된 이미지의 Uri
 * @param onConfirm 파일명 확정 시 호출되는 콜백 (생성된 파일명 전달)
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
    
    // ============================================================
    // 상태 관리
    // ============================================================
    
    // 다이얼로그 표시 상태 (화면 진입 시 자동으로 true)
    var showExpenseDialog by remember { mutableStateOf(true) }
    
    // 확정된 파일명
    var confirmedFileName by remember { mutableStateOf<String?>(null) }
    
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
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 파일명 표시 영역 (확정된 경우)
            if (confirmedFileName != null) {
                FileNameDisplay(
                    fileName = confirmedFileName!!,
                    onEditClick = { showExpenseDialog = true }
                )
            } else {
                // 파일명 미설정 안내
                Text(
                    text = "지출 용도를 입력해주세요",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { showExpenseDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("지출 용도 입력")
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 하단 버튼들
            BottomButtons(
                onRetake = onRetake,
                onConfirm = {
                    confirmedFileName?.let { fileName ->
                        onConfirm(fileName)
                    }
                },
                isConfirmEnabled = confirmedFileName != null
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 지출 용도 입력 다이얼로그
        if (showExpenseDialog) {
            ExpenseInputDialog(
                onDismiss = {
                    showExpenseDialog = false
                    // 파일명이 아직 설정되지 않았으면 취소로 처리
                    if (confirmedFileName == null) {
                        onCancel()
                    }
                },
                onConfirm = { fileName ->
                    confirmedFileName = fileName
                    showExpenseDialog = false
                }
            )
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
            text = "이미지 확인",
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
 * 파일명 표시 영역 Composable
 */
@Composable
private fun FileNameDisplay(
    fileName: String,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "파일명",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // 수정 버튼
            OutlinedButton(
                onClick = onEditClick,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("수정")
            }
        }
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
                text = "저장하기",
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
