package com.bilz.app

import org.junit.Test
import org.junit.Assert.*

/**
 * BILZ 앱 단위 테스트 예시
 * 
 * 이 테스트는 로컬 JVM에서 실행됩니다 (Android 기기/에뮬레이터 불필요).
 * 비즈니스 로직, 유틸리티 함수 등을 테스트하기에 적합합니다.
 * 
 * 테스트 실행: ./gradlew test
 */
class ExampleUnitTest {
    
    /**
     * 기본 덧셈 테스트 예시
     */
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
    
    /**
     * 문자열 테스트 예시
     */
    @Test
    fun appName_isCorrect() {
        val appName = "BILZ"
        assertEquals("BILZ", appName)
        assertTrue(appName.isNotEmpty())
    }
}
