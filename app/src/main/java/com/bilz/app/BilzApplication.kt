package com.bilz.app

import android.app.Application
import android.util.Log
import android.widget.Toast

/**
 * BILZ 앱 Application 클래스
 * 
 * 전역 예외 핸들러를 설정하여 크래시 원인을 파악할 수 있게 합니다.
 */
class BilzApplication : Application() {
    
    companion object {
        private const val TAG = "BilzApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // 전역 예외 핸들러 설정
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // 에러 로그 출력
            Log.e(TAG, "===========================================")
            Log.e(TAG, "앱 크래시 발생!")
            Log.e(TAG, "스레드: ${thread.name}")
            Log.e(TAG, "에러 타입: ${throwable.javaClass.simpleName}")
            Log.e(TAG, "에러 메시지: ${throwable.message}")
            Log.e(TAG, "===========================================")
            
            // 전체 스택 트레이스 출력
            throwable.printStackTrace()
            
            // 원인(Cause) 체인 출력
            var cause = throwable.cause
            var depth = 1
            while (cause != null) {
                Log.e(TAG, "원인 $depth: ${cause.javaClass.simpleName} - ${cause.message}")
                cause = cause.cause
                depth++
            }
            
            // 기본 핸들러 호출 (앱 종료)
            defaultHandler?.uncaughtException(thread, throwable)
        }
        
        Log.d(TAG, "BILZ 앱 초기화 완료")
    }
}
