package com.bilz.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bilz.app.ui.theme.BILZTheme
import kotlinx.coroutines.delay

/**
 * 권한 요청 화면 Composable
 * 
 * 앱 실행 시 필요한 권한을 요청하는 화면입니다.
 * 카메라 권한과 저장소 권한의 필요성을 설명하고,
 * 사용자가 권한을 허용할 수 있도록 안내합니다.
 * 
 * @param hasCameraPermission 카메라 권한 보유 여부
 * @param hasStoragePermission 저장소 권한 보유 여부
 * @param onRequestPermissions 권한 요청 버튼 클릭 시 실행되는 콜백
 * @param modifier 레이아웃 수정자
 */
@Composable
fun PermissionScreen(
    hasCameraPermission: Boolean,
    hasStoragePermission: Boolean,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ============================================================
    // 애니메이션 상태 관리
    // ============================================================
    
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(100) // 약간의 딜레이 후 애니메이션 시작
        isVisible = true
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "alpha"
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
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .alpha(alpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 타이틀
            Text(
                text = "권한이 필요합니다",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 설명 텍스트
            Text(
                text = "BILZ 앱을 사용하기 위해\n다음 권한이 필요합니다",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 권한 목록 카드
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(600)) + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(600)
                )
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        // 카메라 권한 항목
                        PermissionItem(
                            icon = Icons.Outlined.CameraAlt,
                            title = "카메라",
                            description = "사진 촬영을 위해 필요합니다",
                            isGranted = hasCameraPermission
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 저장소 권한 항목
                        PermissionItem(
                            icon = Icons.Outlined.Folder,
                            title = "저장소",
                            description = "사진 저장 및 불러오기를 위해 필요합니다",
                            isGranted = hasStoragePermission
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // 권한 요청 버튼
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Text(
                    text = "권한 허용하기",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 안내 텍스트
            Text(
                text = "권한은 앱 기능 사용에만 활용됩니다",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 개별 권한 항목 Composable
 * 
 * 각 권한의 아이콘, 이름, 설명, 허용 상태를 표시합니다.
 * 
 * @param icon 권한을 나타내는 아이콘
 * @param title 권한 이름
 * @param description 권한 설명
 * @param isGranted 권한 허용 여부
 * @param modifier 레이아웃 수정자
 */
@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 아이콘 배경
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isGranted) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = if (isGranted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // 권한 정보
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // 허용 상태 표시
        if (isGranted) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "허용됨",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

/**
 * 권한 화면 미리보기 (모든 권한 미허용)
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PermissionScreenPreview() {
    BILZTheme {
        PermissionScreen(
            hasCameraPermission = false,
            hasStoragePermission = false,
            onRequestPermissions = {}
        )
    }
}

/**
 * 권한 화면 미리보기 (일부 권한 허용)
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PermissionScreenPartialPreview() {
    BILZTheme {
        PermissionScreen(
            hasCameraPermission = true,
            hasStoragePermission = false,
            onRequestPermissions = {}
        )
    }
}

/**
 * 권한 화면 다크 테마 미리보기
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PermissionScreenDarkPreview() {
    BILZTheme(darkTheme = true) {
        PermissionScreen(
            hasCameraPermission = false,
            hasStoragePermission = false,
            onRequestPermissions = {}
        )
    }
}
