/**
 * @file SheetService.gs
 * @description 구글 시트 관련 기능을 담당합니다.
 */

/**
 * 구글 시트에 처리 결과를 한 줄 기록합니다.
 * @param {string} sheetId - 대상 스프레드시트 ID
 * @param {string} sheetName - 대상 시트 이름
 * @param {object} fileInfo - 기록할 정보 객체
 */
function logToSheet_(sheetId, sheetName, fileInfo) {
  const sheet = SpreadsheetApp.openById(sheetId).getSheetByName(sheetName);
  sheet.appendRow([
    fileInfo.analysisTimestamp,
    fileInfo.originalFileName,
    fileInfo.accountCategory,
    fileInfo.expenseDate,
    fileInfo.purpose,
    fileInfo.vendor,
    fileInfo.amount,
  ]);
}

/**
 * 시트에서 기존에 처리된 모든 원본 파일명을 가져와 Set으로 반환합니다.
 * @param {string} sheetId - 대상 스프레드시트 ID
 * @param {string} sheetName - 대상 시트 이름
 * @returns {Set<string>} 파일명 Set
 */
function getExistingFileNames_(sheetId, sheetName) {
  const sheet = SpreadsheetApp.openById(sheetId).getSheetByName(sheetName);
  const lastRow = sheet.getLastRow();
  if (lastRow < 2) {
    return new Set();
  }
  // '원본파일명'은 두 번째 열(B열)에 위치합니다.
  const range = sheet.getRange(2, 2, lastRow - 1, 1);
  const values = range.getValues().flat();
  return new Set(values);
}

/**
 * 시트에서 특정 월에 해당하는 금액만 더해 총 지출액을 반환합니다.
 * @param {string} sheetId - 대상 스프레드시트 ID
 * @param {string} sheetName - 대상 시트 이름
 * @param {Date} dateObject - 기준이 될 날짜 객체
 * @returns {number} 해당 월의 총 지출액
 */
function getTotalAmount_(sheetId, sheetName, dateObject) {
  const sheet = SpreadsheetApp.openById(sheetId).getSheetByName(sheetName);
  const lastRow = sheet.getLastRow();
  if (lastRow < 2) {
    return 0;
  }

  // 지출일(D열)부터 금액(G열)까지의 데이터를 한번에 가져옵니다.
  const range = sheet.getRange(2, 4, lastRow - 1, 4);
  // getDisplayValues()는 날짜를 'yyyy-MM-dd' 형식의 문자열로 가져와 비교하기 용이합니다.
  const displayValues = range.getDisplayValues();

  // 'yyyy-MM' 형식으로 비교할 기준 월 문자열을 만듭니다.
  const targetMonthStr = Utilities.formatDate(dateObject, 'GMT+9', 'yyyy-MM');
  
  // reduce를 사용하여 조건에 맞는 금액만 합산합니다.
  const total = displayValues.reduce((sum, row) => {
    const expenseDateStr = row[0]; // 범위의 첫 번째 열 = 지출일
    const amountStr = row[3];      // 범위의 네 번째 열 = 금액

    // 지출일이 기준 월과 일치하는지 확인합니다.
    if (expenseDateStr && expenseDateStr.startsWith(targetMonthStr)) {
      return sum + (Number(amountStr.replace(/,/g, '')) || 0);
    }
    return sum;
  }, 0);

  return total;
}
