package com.bilz.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bilz.app.ui.theme.BILZTheme

/**
 * BILZ 앱의 메인 액티비티
 * 
 * 이 클래스는 앱의 진입점(Entry Point)입니다.
 * Jetpack Compose를 사용하여 UI를 구성합니다.
 * 
 * ComponentActivity를 상속받아 Compose UI를 설정할 수 있습니다.
 */
class MainActivity : ComponentActivity() {
    
    /**
     * 액티비티가 생성될 때 호출되는 함수
     * 
     * @param savedInstanceState 이전 상태 정보 (화면 회전 등으로 재생성 시)
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-Edge 디스플레이 활성화
        // 상태바와 네비게이션 바 영역까지 콘텐츠가 확장됩니다.
        enableEdgeToEdge()
        
        // Compose UI 설정
        // setContent 블록 안에서 Composable 함수를 호출하여 UI를 구성합니다.
        setContent {
            // 앱 테마 적용
            BILZTheme {
                // Scaffold: Material Design의 기본 레이아웃 구조를 제공
                // 상단 앱바, 하단 네비게이션, FAB 등을 쉽게 배치할 수 있습니다.
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // 메인 화면 내용
                    Greeting(
                        name = "BILZ",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

/**
 * 인사말을 표시하는 Composable 함수
 * 
 * @param name 표시할 이름
 * @param modifier 레이아웃 수정자
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

/**
 * 미리보기 함수
 * 
 * Android Studio에서 UI를 실시간으로 미리볼 수 있습니다.
 * @Preview 어노테이션이 붙은 함수는 빌드 없이 UI를 확인할 수 있습니다.
 */
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BILZTheme {
        Greeting("BILZ")
    }
}
