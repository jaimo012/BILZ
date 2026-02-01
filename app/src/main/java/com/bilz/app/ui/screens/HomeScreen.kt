package com.bilz.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bilz.app.ui.theme.BILZTheme

/**
 * 홈 화면 Composable
 * 
 * 권한이 모두 허용된 후 표시되는 메인 홈 화면입니다.
 * 중앙에 "시작" 버튼이 위치한 심플한 디자인입니다.
 * 
 * @param onStartClick 시작 버튼 클릭 시 실행되는 콜백
 * @param modifier 레이아웃 수정자
 */
@Composable
fun HomeScreen(
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ============================================================
    // 애니메이션 상태 관리
    // ============================================================
    
    // 화면 진입 애니메이션을 위한 상태
    var isVisible by remember { mutableStateOf(false) }
    
    // 화면이 표시될 때 애니메이션 시작
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    // 알파(투명도) 애니메이션
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "alpha"
    )
    
    // 스케일(크기) 애니메이션
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = tween(durationMillis = 600),
        label = "scale"
    )
    
    // ============================================================
    // UI 구성
    // ============================================================
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                // 그라데이션 배경
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .alpha(alpha)
                .scale(scale),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 앱 타이틀
            Text(
                text = "BILZ",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp,
                    letterSpacing = 8.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 서브 타이틀
            Text(
                text = "사진을 촬영하고 관리하세요",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // 시작 버튼 (원형 버튼)
            StartButton(
                onClick = onStartClick
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 버튼 안내 텍스트
            Text(
                text = "시작하려면 버튼을 누르세요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 시작 버튼 Composable
 * 
 * 원형의 큰 시작 버튼입니다.
 * 
 * @param onClick 버튼 클릭 시 실행되는 콜백
 * @param modifier 레이아웃 수정자
 */
@Composable
private fun StartButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.size(120.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 4.dp
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 재생 아이콘
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "시작",
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
            
            // "시작" 텍스트
            Text(
                text = "시작",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

/**
 * 홈 화면 미리보기
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    BILZTheme {
        HomeScreen(
            onStartClick = {}
        )
    }
}

/**
 * 홈 화면 다크 테마 미리보기
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenDarkPreview() {
    BILZTheme(darkTheme = true) {
        HomeScreen(
            onStartClick = {}
        )
    }
}
