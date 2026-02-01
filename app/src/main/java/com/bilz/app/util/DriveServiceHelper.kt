package com.bilz.app.util

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * Google Drive 업로드 결과를 나타내는 sealed class
 */
sealed class DriveUploadResult {
    /**
     * 업로드 성공
     * 
     * @param fileId 업로드된 파일의 Google Drive 파일 ID
     * @param fileName 업로드된 파일명
     * @param webViewLink Drive에서 파일을 볼 수 있는 웹 링크 (있는 경우)
     */
    data class Success(
        val fileId: String,
        val fileName: String,
        val webViewLink: String? = null
    ) : DriveUploadResult()
    
    /**
     * 업로드 실패
     * 
     * @param message 에러 메시지
     * @param exception 발생한 예외 (있는 경우)
     */
    data class Failure(
        val message: String,
        val exception: Exception? = null
    ) : DriveUploadResult()
}

/**
 * Google Drive 서비스 헬퍼 클래스
 * 
 * Google Drive API를 사용하여 파일을 업로드하는 기능을 제공합니다.
 * 
 * 주요 기능:
 * - GoogleSignInAccount를 사용한 Drive 서비스 초기화
 * - 지정된 폴더에 파일 업로드
 * - 업로드 진행 상태 콜백 지원
 * 
 * 사용 예시:
 * ```kotlin
 * val driveHelper = DriveServiceHelper(context, account)
 * val result = driveHelper.uploadFile(
 *     localFile = imageFile,
 *     mimeType = "image/jpeg",
 *     folderId = DriveServiceHelper.BILZ_FOLDER_ID
 * )
 * 
 * when (result) {
 *     is DriveUploadResult.Success -> {
 *         println("업로드 성공: ${result.fileId}")
 *     }
 *     is DriveUploadResult.Failure -> {
 *         println("업로드 실패: ${result.message}")
 *     }
 * }
 * ```
 */
class DriveServiceHelper(
    private val context: Context,
    account: GoogleSignInAccount
) {
    
    companion object {
        private const val TAG = "DriveServiceHelper"
        
        /** 앱 이름 (Drive API 요청 시 사용) */
        private const val APP_NAME = "BILZ"
        
        /**
         * BILZ 앱 전용 Google Drive 폴더 ID
         * 
         * 모든 영수증 이미지는 이 폴더에 업로드됩니다.
         */
        const val BILZ_FOLDER_ID = "1Lbvto7NZrZkvlWoH-kFUhHU0bqQg7IMs"
        
        /** JPEG 이미지 MIME 타입 */
        const val MIME_TYPE_JPEG = "image/jpeg"
        
        /** PNG 이미지 MIME 타입 */
        const val MIME_TYPE_PNG = "image/png"
    }
    
    /**
     * Google Drive 서비스 인스턴스
     * 
     * GoogleSignInAccount의 계정 정보를 사용하여 인증된 Drive 서비스를 생성합니다.
     */
    private val driveService: Drive
    
    init {
        Log.d(TAG, "Drive 서비스 초기화: ${account.email}")
        
        // Google 계정 인증 정보 설정
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE)
        ).apply {
            selectedAccount = account.account
        }
        
        // Drive 서비스 빌드
        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName(APP_NAME)
            .build()
        
        Log.d(TAG, "Drive 서비스 초기화 완료")
    }
    
    /**
     * 파일을 Google Drive에 업로드합니다.
     * 
     * @param localFile 업로드할 로컬 파일
     * @param mimeType 파일의 MIME 타입 (예: "image/jpeg")
     * @param folderId 업로드할 대상 폴더의 Drive ID (기본값: BILZ_FOLDER_ID)
     * @return DriveUploadResult 업로드 결과
     */
    suspend fun uploadFile(
        localFile: File,
        mimeType: String,
        folderId: String = BILZ_FOLDER_ID
    ): DriveUploadResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "파일 업로드 시작: ${localFile.name}")
        Log.d(TAG, "MIME 타입: $mimeType")
        Log.d(TAG, "대상 폴더 ID: $folderId")
        
        try {
            // 파일 존재 여부 확인
            if (!localFile.exists()) {
                Log.e(TAG, "파일이 존재하지 않습니다: ${localFile.absolutePath}")
                return@withContext DriveUploadResult.Failure("파일이 존재하지 않습니다")
            }
            
            // 파일 크기 확인 (로깅용)
            val fileSizeKB = localFile.length() / 1024
            Log.d(TAG, "파일 크기: ${fileSizeKB}KB")
            
            // Drive 파일 메타데이터 설정
            val driveFileMetadata = DriveFile().apply {
                name = localFile.name
                // 부모 폴더 ID 설정 (지정된 폴더에 업로드)
                parents = listOf(folderId)
            }
            
            // 파일 내용을 InputStreamContent로 변환
            val fileContent = InputStreamContent(
                mimeType,
                FileInputStream(localFile)
            ).apply {
                length = localFile.length()
            }
            
            // Drive에 파일 업로드
            Log.d(TAG, "Drive API 업로드 요청 시작...")
            val uploadedFile = driveService.files()
                .create(driveFileMetadata, fileContent)
                .setFields("id, name, webViewLink")
                .execute()
            
            Log.d(TAG, "업로드 성공!")
            Log.d(TAG, "파일 ID: ${uploadedFile.id}")
            Log.d(TAG, "파일명: ${uploadedFile.name}")
            Log.d(TAG, "웹 링크: ${uploadedFile.webViewLink}")
            
            DriveUploadResult.Success(
                fileId = uploadedFile.id,
                fileName = uploadedFile.name,
                webViewLink = uploadedFile.webViewLink
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "업로드 실패", e)
            DriveUploadResult.Failure(
                message = "업로드 실패: ${e.message ?: "알 수 없는 오류"}",
                exception = e
            )
        }
    }
    
    /**
     * Uri에서 InputStream을 사용하여 파일을 Google Drive에 업로드합니다.
     * 
     * MediaStore의 Content Uri를 직접 사용할 때 유용합니다.
     * 
     * @param inputStream 업로드할 파일의 InputStream
     * @param fileName 저장할 파일명
     * @param mimeType 파일의 MIME 타입
     * @param fileSize 파일 크기 (바이트)
     * @param folderId 업로드할 대상 폴더의 Drive ID
     * @return DriveUploadResult 업로드 결과
     */
    suspend fun uploadFromInputStream(
        inputStream: InputStream,
        fileName: String,
        mimeType: String,
        fileSize: Long,
        folderId: String = BILZ_FOLDER_ID
    ): DriveUploadResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "InputStream에서 업로드 시작: $fileName")
        Log.d(TAG, "파일 크기: ${fileSize / 1024}KB")
        
        try {
            // Drive 파일 메타데이터 설정
            val driveFileMetadata = DriveFile().apply {
                name = fileName
                parents = listOf(folderId)
            }
            
            // InputStream을 InputStreamContent로 변환
            val fileContent = InputStreamContent(mimeType, inputStream).apply {
                length = fileSize
            }
            
            // Drive에 파일 업로드
            Log.d(TAG, "Drive API 업로드 요청 시작...")
            val uploadedFile = driveService.files()
                .create(driveFileMetadata, fileContent)
                .setFields("id, name, webViewLink")
                .execute()
            
            Log.d(TAG, "업로드 성공! 파일 ID: ${uploadedFile.id}")
            
            DriveUploadResult.Success(
                fileId = uploadedFile.id,
                fileName = uploadedFile.name,
                webViewLink = uploadedFile.webViewLink
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "업로드 실패", e)
            DriveUploadResult.Failure(
                message = "업로드 실패: ${e.message ?: "알 수 없는 오류"}",
                exception = e
            )
        }
    }
    
    /**
     * Content Uri에서 파일을 Google Drive에 업로드합니다.
     * 
     * MediaStore에 저장된 이미지를 직접 업로드할 때 사용합니다.
     * 
     * @param contentUri MediaStore Content Uri
     * @param fileName 저장할 파일명
     * @param mimeType 파일의 MIME 타입
     * @param folderId 업로드할 대상 폴더의 Drive ID
     * @return DriveUploadResult 업로드 결과
     */
    suspend fun uploadFromContentUri(
        contentUri: android.net.Uri,
        fileName: String,
        mimeType: String,
        folderId: String = BILZ_FOLDER_ID
    ): DriveUploadResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Content Uri에서 업로드 시작: $contentUri")
        
        try {
            // ContentResolver를 통해 InputStream 획득
            val inputStream = context.contentResolver.openInputStream(contentUri)
                ?: return@withContext DriveUploadResult.Failure("파일을 열 수 없습니다")
            
            // 파일 크기 조회
            val fileSize = context.contentResolver.openAssetFileDescriptor(contentUri, "r")?.use {
                it.length
            } ?: 0L
            
            // InputStream으로 업로드
            inputStream.use { stream ->
                uploadFromInputStream(
                    inputStream = stream,
                    fileName = fileName,
                    mimeType = mimeType,
                    fileSize = fileSize,
                    folderId = folderId
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Content Uri 업로드 실패", e)
            DriveUploadResult.Failure(
                message = "업로드 실패: ${e.message ?: "알 수 없는 오류"}",
                exception = e
            )
        }
    }
}
