package com.bilz.app.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Google 로그인 결과를 나타내는 sealed class
 */
sealed class GoogleSignInResult {
    /**
     * 로그인 성공
     * 
     * @param account 로그인된 Google 계정 정보
     */
    data class Success(val account: GoogleSignInAccount) : GoogleSignInResult()
    
    /**
     * 로그인 실패
     * 
     * @param message 에러 메시지
     * @param exception 발생한 예외 (있는 경우)
     */
    data class Failure(
        val message: String,
        val exception: Exception? = null
    ) : GoogleSignInResult()
    
    /**
     * 로그인 취소 (사용자가 취소한 경우)
     */
    data object Cancelled : GoogleSignInResult()
    
    /**
     * 로그인 필요 (Intent를 통해 로그인 화면을 띄워야 함)
     * 
     * @param signInIntent 로그인 화면을 띄우기 위한 Intent
     */
    data class NeedSignIn(val signInIntent: Intent) : GoogleSignInResult()
}

/**
 * Google 로그인 관리 클래스
 * 
 * Google Drive API 사용을 위한 Google 로그인을 관리합니다.
 * DriveScopes.DRIVE_FILE 스코프를 요청하여 앱에서 생성한 파일에 대한
 * 읽기/쓰기 권한을 획득합니다.
 * 
 * 주요 기능:
 * - 기존 로그인 상태 확인
 * - 새로운 로그인 요청
 * - 로그인 결과 처리
 * - 로그아웃
 * 
 * 사용 예시:
 * ```kotlin
 * val authManager = GoogleAuthManager(context)
 * 
 * // 로그인 시도
 * val result = authManager.trySignIn()
 * when (result) {
 *     is GoogleSignInResult.Success -> {
 *         // 로그인 성공, account 사용 가능
 *     }
 *     is GoogleSignInResult.NeedSignIn -> {
 *         // 로그인 화면 표시 필요
 *         launcher.launch(result.signInIntent)
 *     }
 *     // ...
 * }
 * ```
 */
class GoogleAuthManager(private val context: Context) {
    
    companion object {
        private const val TAG = "GoogleAuthManager"
        
        /**
         * 요청하는 Google Drive 스코프
         * 
         * DRIVE_FILE: 앱에서 생성한 파일만 접근 가능 (가장 제한적이고 안전한 스코프)
         * - 앱이 생성한 파일 읽기/쓰기 가능
         * - 다른 앱이나 웹에서 생성한 파일은 접근 불가
         */
        private val DRIVE_SCOPE = Scope(DriveScopes.DRIVE_FILE)
    }
    
    /**
     * Google Sign-In 옵션 설정
     * 
     * - 이메일 요청: 사용자 식별용
     * - Drive 스코프 요청: Drive API 접근 권한
     */
    private val googleSignInOptions: GoogleSignInOptions by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(DRIVE_SCOPE)
            .build()
    }
    
    /**
     * Google Sign-In 클라이언트
     */
    private val googleSignInClient: GoogleSignInClient by lazy {
        GoogleSignIn.getClient(context, googleSignInOptions)
    }
    
    /**
     * 현재 로그인된 계정 가져오기
     * 
     * @return 로그인된 GoogleSignInAccount 또는 null
     */
    fun getCurrentAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }
    
    /**
     * 로그인 상태 확인
     * 
     * @return 로그인되어 있으면 true
     */
    fun isSignedIn(): Boolean {
        val account = getCurrentAccount()
        // 계정이 있고, Drive 스코프가 허용되어 있는지 확인
        return account != null && GoogleSignIn.hasPermissions(account, DRIVE_SCOPE)
    }
    
    /**
     * 로그인 시도
     * 
     * 이미 로그인되어 있으면 Success를 반환하고,
     * 그렇지 않으면 NeedSignIn을 반환합니다.
     * 
     * @return GoogleSignInResult
     */
    fun trySignIn(): GoogleSignInResult {
        Log.d(TAG, "로그인 시도")
        
        // 기존 로그인 상태 확인
        val account = getCurrentAccount()
        
        return if (account != null && GoogleSignIn.hasPermissions(account, DRIVE_SCOPE)) {
            // 이미 로그인되어 있고 필요한 권한이 있음
            Log.d(TAG, "기존 로그인 계정 사용: ${account.email}")
            GoogleSignInResult.Success(account)
        } else {
            // 로그인 필요
            Log.d(TAG, "로그인 필요 - 로그인 Intent 반환")
            GoogleSignInResult.NeedSignIn(googleSignInClient.signInIntent)
        }
    }
    
    /**
     * Silent 로그인 시도 (UI 없이 로그인)
     * 
     * 이전에 로그인한 적이 있다면 자동으로 로그인합니다.
     * UI를 표시하지 않으므로, 앱 시작 시 호출하기 적합합니다.
     * 
     * @return GoogleSignInResult
     */
    suspend fun trySilentSignIn(): GoogleSignInResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Silent 로그인 시도")
        
        return@withContext suspendCancellableCoroutine { continuation ->
            googleSignInClient.silentSignIn()
                .addOnSuccessListener { account ->
                    Log.d(TAG, "Silent 로그인 성공: ${account.email}")
                    continuation.resume(GoogleSignInResult.Success(account))
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "Silent 로그인 실패 - 명시적 로그인 필요", exception)
                    // Silent 로그인 실패 시 명시적 로그인 필요
                    continuation.resume(GoogleSignInResult.NeedSignIn(googleSignInClient.signInIntent))
                }
        }
    }
    
    /**
     * 로그인 결과 처리
     * 
     * Activity Result에서 받은 Intent를 처리하여 로그인 결과를 반환합니다.
     * 
     * @param data Activity Result에서 받은 Intent
     * @return GoogleSignInResult
     */
    fun handleSignInResult(data: Intent?): GoogleSignInResult {
        Log.d(TAG, "로그인 결과 처리")
        
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            
            if (account != null) {
                Log.d(TAG, "로그인 성공: ${account.email}")
                GoogleSignInResult.Success(account)
            } else {
                Log.e(TAG, "로그인 결과: account가 null")
                GoogleSignInResult.Failure("로그인에 실패했습니다")
            }
        } catch (e: ApiException) {
            Log.e(TAG, "로그인 실패: statusCode=${e.statusCode}", e)
            
            when (e.statusCode) {
                // 사용자가 로그인을 취소한 경우
                12501 -> {
                    Log.d(TAG, "사용자가 로그인을 취소했습니다")
                    GoogleSignInResult.Cancelled
                }
                // 네트워크 오류
                7 -> {
                    GoogleSignInResult.Failure("네트워크 연결을 확인해주세요", e)
                }
                // 기타 오류
                else -> {
                    GoogleSignInResult.Failure(
                        "로그인에 실패했습니다 (코드: ${e.statusCode})",
                        e
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "로그인 처리 중 예외 발생", e)
            GoogleSignInResult.Failure("로그인 처리 중 오류가 발생했습니다: ${e.message}", e)
        }
    }
    
    /**
     * 로그아웃
     * 
     * 현재 계정에서 로그아웃합니다.
     * 다음 로그인 시 계정 선택 화면이 표시됩니다.
     */
    suspend fun signOut(): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "로그아웃 시도")
        
        return@withContext suspendCancellableCoroutine { continuation ->
            googleSignInClient.signOut()
                .addOnSuccessListener {
                    Log.d(TAG, "로그아웃 성공")
                    continuation.resume(true)
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "로그아웃 실패", exception)
                    continuation.resume(false)
                }
        }
    }
    
    /**
     * 계정 연결 해제 (Revoke)
     * 
     * 앱에 부여된 모든 권한을 취소하고 계정 연결을 해제합니다.
     * 다음 로그인 시 다시 권한 동의가 필요합니다.
     */
    suspend fun revokeAccess(): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "계정 연결 해제 시도")
        
        return@withContext suspendCancellableCoroutine { continuation ->
            googleSignInClient.revokeAccess()
                .addOnSuccessListener {
                    Log.d(TAG, "계정 연결 해제 성공")
                    continuation.resume(true)
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "계정 연결 해제 실패", exception)
                    continuation.resume(false)
                }
        }
    }
    
    /**
     * 로그인된 사용자의 이메일 가져오기
     * 
     * @return 이메일 주소 또는 null
     */
    fun getEmail(): String? {
        return getCurrentAccount()?.email
    }
    
    /**
     * 로그인된 사용자의 표시 이름 가져오기
     * 
     * @return 표시 이름 또는 null
     */
    fun getDisplayName(): String? {
        return getCurrentAccount()?.displayName
    }
    
    /**
     * 로그인된 사용자의 프로필 사진 URL 가져오기
     * 
     * @return 프로필 사진 URL 또는 null
     */
    fun getPhotoUrl(): String? {
        return getCurrentAccount()?.photoUrl?.toString()
    }
}
