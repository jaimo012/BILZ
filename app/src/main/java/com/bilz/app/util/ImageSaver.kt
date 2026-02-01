package com.bilz.app.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * 이미지 저장 결과를 나타내는 sealed class
 */
sealed class ImageSaveResult {
    /**
     * 저장 성공
     * 
     * @param savedUri MediaStore에 저장된 이미지의 Content Uri
     * @param displayName 저장된 파일명
     * @param relativePath 저장된 상대 경로 (예: Pictures/BILZ)
     */
    data class Success(
        val savedUri: Uri,
        val displayName: String,
        val relativePath: String
    ) : ImageSaveResult()
    
    /**
     * 저장 실패
     * 
     * @param message 에러 메시지
     * @param exception 발생한 예외 (있는 경우)
     */
    data class Failure(
        val message: String,
        val exception: Exception? = null
    ) : ImageSaveResult()
}

/**
 * 이미지 저장 유틸리티 클래스
 * 
 * Android 10 (API 29) 이상의 Scoped Storage를 지원하며,
 * MediaStore를 통해 Pictures/BILZ 폴더에 이미지를 저장합니다.
 * 
 * 주요 기능:
 * - MediaStore를 통한 이미지 저장 (Android 10+)
 * - 레거시 저장 방식 지원 (Android 9 이하)
 * - 저장된 이미지의 Uri 및 InputStream 제공
 */
object ImageSaver {
    
    private const val TAG = "ImageSaver"
    
    /** BILZ 앱 전용 폴더명 */
    private const val BILZ_FOLDER_NAME = "BILZ"
    
    /** 이미지 저장 경로 (Pictures/BILZ) */
    private const val RELATIVE_PATH = "Pictures/$BILZ_FOLDER_NAME"
    
    /** MIME 타입 */
    private const val MIME_TYPE_JPEG = "image/jpeg"
    
    /**
     * 이미지를 Pictures/BILZ 폴더에 저장합니다.
     * 
     * Android 10 이상에서는 MediaStore API를 사용하고,
     * Android 9 이하에서는 직접 파일 시스템에 저장합니다.
     * 
     * @param context Android Context
     * @param sourceUri 원본 이미지 Uri (크롭된 이미지)
     * @param fileName 저장할 파일명 (예: 20260201_점심식사.jpg)
     * @return ImageSaveResult 저장 결과
     */
    suspend fun saveImage(
        context: Context,
        sourceUri: Uri,
        fileName: String
    ): ImageSaveResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "이미지 저장 시작: $fileName")
            
            // 원본 이미지를 Bitmap으로 로드
            val bitmap = loadBitmapFromUri(context, sourceUri)
                ?: return@withContext ImageSaveResult.Failure("이미지를 불러올 수 없습니다")
            
            // Android 버전에 따라 저장 방식 분기
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10 이상: MediaStore API 사용
                saveImageWithMediaStore(context, bitmap, fileName)
            } else {
                // Android 9 이하: 직접 파일 시스템 접근
                saveImageLegacy(context, bitmap, fileName)
            }
            
            // Bitmap 메모리 해제
            bitmap.recycle()
            
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "이미지 저장 실패", e)
            ImageSaveResult.Failure("이미지 저장 중 오류가 발생했습니다: ${e.message}", e)
        }
    }
    
    /**
     * Uri에서 Bitmap을 로드합니다.
     */
    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Bitmap 로드 실패: $uri", e)
            null
        }
    }
    
    /**
     * MediaStore API를 사용하여 이미지를 저장합니다. (Android 10+)
     */
    private fun saveImageWithMediaStore(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ): ImageSaveResult {
        val contentResolver = context.contentResolver
        
        // ContentValues 설정
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, MIME_TYPE_JPEG)
            put(MediaStore.Images.Media.RELATIVE_PATH, RELATIVE_PATH)
            
            // Android 10 이상: IS_PENDING 플래그로 다른 앱에서 접근 방지
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        
        // MediaStore에 새 항목 삽입
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        
        val insertedUri = contentResolver.insert(collection, contentValues)
            ?: return ImageSaveResult.Failure("MediaStore에 항목을 생성할 수 없습니다")
        
        try {
            // OutputStream을 통해 이미지 저장
            contentResolver.openOutputStream(insertedUri)?.use { outputStream ->
                val success = bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                if (!success) {
                    // 저장 실패 시 삽입된 항목 삭제
                    contentResolver.delete(insertedUri, null, null)
                    return ImageSaveResult.Failure("이미지 압축에 실패했습니다")
                }
            } ?: run {
                contentResolver.delete(insertedUri, null, null)
                return ImageSaveResult.Failure("출력 스트림을 열 수 없습니다")
            }
            
            // IS_PENDING 플래그 해제 (저장 완료)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val updateValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                contentResolver.update(insertedUri, updateValues, null, null)
            }
            
            Log.d(TAG, "이미지 저장 성공: $insertedUri")
            
            return ImageSaveResult.Success(
                savedUri = insertedUri,
                displayName = fileName,
                relativePath = RELATIVE_PATH
            )
            
        } catch (e: Exception) {
            // 오류 발생 시 삽입된 항목 삭제
            contentResolver.delete(insertedUri, null, null)
            throw e
        }
    }
    
    /**
     * 레거시 방식으로 이미지를 저장합니다. (Android 9 이하)
     */
    @Suppress("DEPRECATION")
    private fun saveImageLegacy(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ): ImageSaveResult {
        // Pictures/BILZ 폴더 생성
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val bilzDir = File(picturesDir, BILZ_FOLDER_NAME)
        
        if (!bilzDir.exists() && !bilzDir.mkdirs()) {
            return ImageSaveResult.Failure("저장 폴더를 생성할 수 없습니다")
        }
        
        // 파일 저장
        val imageFile = File(bilzDir, fileName)
        
        try {
            FileOutputStream(imageFile).use { outputStream ->
                val success = bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                if (!success) {
                    imageFile.delete()
                    return ImageSaveResult.Failure("이미지 압축에 실패했습니다")
                }
            }
            
            // MediaStore에 등록 (갤러리에서 보이도록)
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, MIME_TYPE_JPEG)
                put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
            }
            
            val savedUri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: Uri.fromFile(imageFile)
            
            Log.d(TAG, "이미지 저장 성공 (레거시): ${imageFile.absolutePath}")
            
            return ImageSaveResult.Success(
                savedUri = savedUri,
                displayName = fileName,
                relativePath = RELATIVE_PATH
            )
            
        } catch (e: Exception) {
            imageFile.delete()
            throw e
        }
    }
    
    /**
     * 저장된 이미지의 InputStream을 가져옵니다.
     * 
     * Google Drive 업로드 등에 사용할 수 있습니다.
     * 
     * @param context Android Context
     * @param savedUri 저장된 이미지의 Uri
     * @return InputStream (사용 후 close 필요) 또는 null
     */
    fun getInputStream(context: Context, savedUri: Uri): InputStream? {
        return try {
            context.contentResolver.openInputStream(savedUri)
        } catch (e: Exception) {
            Log.e(TAG, "InputStream 열기 실패: $savedUri", e)
            null
        }
    }
    
    /**
     * 저장된 이미지의 실제 파일 경로를 가져옵니다.
     * 
     * 주의: Android 10 이상에서는 Scoped Storage로 인해
     * 직접적인 파일 경로 접근이 제한될 수 있습니다.
     * 가능하면 Uri와 ContentResolver를 사용하는 것이 권장됩니다.
     * 
     * @param context Android Context
     * @param savedUri 저장된 이미지의 Uri
     * @return 파일 경로 문자열 또는 null
     */
    fun getFilePath(context: Context, savedUri: Uri): String? {
        return try {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            context.contentResolver.query(savedUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    cursor.getString(columnIndex)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "파일 경로 조회 실패: $savedUri", e)
            null
        }
    }
    
    /**
     * Pictures/BILZ 폴더의 전체 경로를 반환합니다.
     * 
     * @return 폴더 경로 문자열
     */
    fun getBilzFolderPath(): String {
        return "${Environment.DIRECTORY_PICTURES}/$BILZ_FOLDER_NAME"
    }
}
