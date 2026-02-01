package com.bilz.app

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * BILZ 앱 계측 테스트 (Instrumented Test)
 * 
 * 이 테스트는 Android 기기 또는 에뮬레이터에서 실행됩니다.
 * Context가 필요한 테스트, UI 테스트 등에 적합합니다.
 * 
 * 테스트 실행: ./gradlew connectedAndroidTest
 * 
 * @see [안드로이드 테스트 가이드](http://d.android.com/tools/testing)
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    
    /**
     * 앱 패키지명 확인 테스트
     * 
     * 앱이 올바른 패키지명으로 설치되었는지 확인합니다.
     */
    @Test
    fun useAppContext() {
        // 테스트 대상 앱의 Context 가져오기
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        
        // 패키지명 확인
        assertEquals("com.bilz.app", appContext.packageName)
    }
}
