package com.bilz.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bilz.app.ui.theme.BILZTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 지출 용도 입력 다이얼로그 Composable
 * 
 * 크롭이 완료된 후 사용자가 지출 용도를 입력하는 다이얼로그입니다.
 * 입력된 내용은 파일명의 일부로 사용됩니다.
 * 
 * 파일명 형식: {yyyyMMdd}_{사용자입력}.jpg
 * 예시: 20260201_점심식사.jpg
 * 
 * @param onDismiss 다이얼로그 닫기 시 호출되는 콜백
 * @param onConfirm 저장 버튼 클릭 시 호출되는 콜백 (생성된 파일명 전달)
 */
@Composable
fun ExpenseInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (fileName: String) -> Unit
) {
    // ============================================================
    // 상태 관리
    // ============================================================
    
    // 지출 용도 입력 상태
    var expenseDescription by remember { mutableStateOf("") }
    
    // 입력 필드 에러 상태
    var inputError by remember { mutableStateOf<String?>(null) }
    
    // 포커스 요청자
    val focusRequester = remember { FocusRequester() }
    
    // 다이얼로그가 열리면 입력 필드에 포커스
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    // ============================================================
    // 유효성 검사 함수
    // ============================================================
    
    fun validateInput(input: String): String? {
        return when {
            input.isBlank() -> "지출 용도를 입력해주세요"
            input.length < 2 -> "2자 이상 입력해주세요"
            input.length > 30 -> "30자 이하로 입력해주세요"
            input.contains(Regex("[\\\\/:*?\"<>|.]")) -> "사용할 수 없는 문자가 포함되어 있습니다"
            else -> null
        }
    }
    
    // ============================================================
    // 파일명 생성 함수
    // ============================================================
    
    fun generateFileName(description: String): String {
        // 현재 날짜를 yyyyMMdd 형식으로 변환
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val dateString = dateFormat.format(Date())
        
        // 파일명에서 사용할 수 없는 문자 제거/치환
        val sanitizedDescription = description
            .trim()
            .replace(Regex("[\\s]+"), "_")  // 공백을 언더스코어로 변환
        
        // 최종 파일명: {yyyyMMdd}_{사용자입력}.jpg
        return "${dateString}_${sanitizedDescription}.jpg"
    }
    
    // ============================================================
    // 저장 처리 함수
    // ============================================================
    
    fun handleSave() {
        val error = validateInput(expenseDescription)
        if (error != null) {
            inputError = error
        } else {
            val fileName = generateFileName(expenseDescription)
            onConfirm(fileName)
        }
    }
    
    // ============================================================
    // 다이얼로그 UI
    // ============================================================
    
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "지출 용도 입력",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 안내 텍스트
                Text(
                    text = "영수증의 지출 용도를 입력해주세요.\n파일명으로 사용됩니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 입력 필드
                OutlinedTextField(
                    value = expenseDescription,
                    onValueChange = { newValue ->
                        expenseDescription = newValue
                        inputError = null  // 입력 시 에러 초기화
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    label = {
                        Text("지출 용도")
                    },
                    placeholder = {
                        Text("예: 점심식사, 교통비, 사무용품")
                    },
                    isError = inputError != null,
                    supportingText = if (inputError != null) {
                        {
                            Text(
                                text = inputError!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        {
                            // 파일명 미리보기
                            val preview = if (expenseDescription.isNotBlank()) {
                                generateFileName(expenseDescription)
                            } else {
                                "파일명 미리보기"
                            }
                            Text(
                                text = "파일명: $preview",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { handleSave() }
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { handleSave() },
                enabled = expenseDescription.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "저장",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("취소")
            }
        }
    )
}

/**
 * 파일명 생성 유틸리티 함수 (외부에서 사용 가능)
 * 
 * 지출 용도를 입력받아 {yyyyMMdd}_{사용자입력}.jpg 형식의 파일명을 생성합니다.
 * 
 * @param description 지출 용도 설명
 * @return 생성된 파일명
 */
fun generateExpenseFileName(description: String): String {
    // 현재 날짜를 yyyyMMdd 형식으로 변환
    val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val dateString = dateFormat.format(Date())
    
    // 파일명에서 사용할 수 없는 문자 제거/치환
    val sanitizedDescription = description
        .trim()
        .replace(Regex("[\\s]+"), "_")  // 공백을 언더스코어로 변환
        .replace(Regex("[\\\\/:*?\"<>|.]"), "")  // 금지 문자 제거
    
    // 최종 파일명: {yyyyMMdd}_{사용자입력}.jpg
    return "${dateString}_${sanitizedDescription}.jpg"
}

/**
 * 지출 용도 입력 다이얼로그 미리보기
 */
@Preview(showBackground = true)
@Composable
fun ExpenseInputDialogPreview() {
    BILZTheme {
        ExpenseInputDialog(
            onDismiss = {},
            onConfirm = {}
        )
    }
}

/**
 * 지출 용도 입력 다이얼로그 다크 테마 미리보기
 */
@Preview(showBackground = true)
@Composable
fun ExpenseInputDialogDarkPreview() {
    BILZTheme(darkTheme = true) {
        ExpenseInputDialog(
            onDismiss = {},
            onConfirm = {}
        )
    }
}
