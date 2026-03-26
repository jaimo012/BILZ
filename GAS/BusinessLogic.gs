/**
 * @file BusinessLogic.gs
 * @description 파일명 분석, 계정과목 결정 등 핵심 비즈니스 로직을 담당합니다.
 */

/**
 * 특정 파일 하나를 처리하는 함수
 * @param {GoogleAppsScript.Drive.File} file - 처리할 파일 객체
 * @param {string} apiKey - Gemini API 키
 * @returns {object|null} 처리 결과 정보 객체 또는 실패 시 null
 */
function processSingleReceipt_(file, apiKey) {
  const fileName = file.getName();
  
  const parsedName = parseFileName_(fileName);
  if (!parsedName) {
    Logger.log(`파일명 형식이 올바르지 않아 건너뜁니다: ${fileName}`);
    return null;
  }

  const { expenseDate, purpose } = parsedName;
  // 수정: 상세 용도 분류 로직이 적용된 함수 호출
  const accountCategory = determineAccountCategory_(purpose);
  
  const geminiResult = extractInfoWithGemini_(file.getBlob(), apiKey);
  if (!geminiResult || !geminiResult.vendor || geminiResult.amount === 0) {
    Logger.log(`Gemini 분석 실패 또는 필수 정보 누락: ${fileName}`);
    return null; 
  }

  return {
    analysisTimestamp: Utilities.formatDate(new Date(), "GMT+9", "yyyy-MM-dd HH:mm:ss"),
    originalFileName: fileName,
    accountCategory: accountCategory,
    expenseDate: expenseDate,
    purpose: purpose,
    vendor: geminiResult.vendor,
    amount: geminiResult.amount,
  };
}


/**
 * 파일명(yyyymmdd_지출목적.확장자)을 파싱하여 지출일과 내용을 반환합니다.
 * @param {string} fileName - 파일명
 * @returns {{expenseDate: string, purpose: string}|null} 파싱 결과 또는 null
 */
function parseFileName_(fileName) {
  const match = fileName.match(/^(\d{8})_([^.]+)\..+$/);
  if (!match) return null;
  
  const dateString = match[1];
  const yyyy = dateString.substring(0, 4);
  const mm = dateString.substring(4, 6);
  const dd = dateString.substring(6, 8);
  
  return {
    expenseDate: `${yyyy}-${mm}-${dd}`,
    purpose: match[2],
  };
}

/**
 * 지출 내용(파일명의 purpose)에 따라 요청하신 6가지 용도 중 하나를 결정합니다.
 * @param {string} purpose - 지출 내용 (파일명에서 추출)
 * @returns {string} 상세 용도명
 */
function determineAccountCategory_(purpose) {
  // 키워드 매칭을 위해 입력값을 소문자로 변환하지 않고 그대로 사용하되, 공백을 제거하여 비교
  const cleanPurpose = purpose.replace(/\s+/g, '');

  // 1. 교통비 관련 (가장 우선순위가 높은 키워드부터 검사)
  if (cleanPurpose.includes('택시') || 
      cleanPurpose.includes('버스') || 
      cleanPurpose.includes('주차') || 
      cleanPurpose.includes('톨게이트') || 
      cleanPurpose.includes('하이패스') || 
      cleanPurpose.includes('대리') ||
      cleanPurpose.includes('주유') ||
      cleanPurpose.includes('출장') ||
      cleanPurpose.includes('복귀') ||
      cleanPurpose.includes('이동')) {
    return '개인경비_업무수행과 관련된 교통비용 (외근, 야근, 출장, 주차비 등)';
  }

  // 2. 접대비 관련
  if (cleanPurpose.includes('미팅') || 
      cleanPurpose.includes('식사') || 
      cleanPurpose.includes('음료') || 
      cleanPurpose.includes('커피') || 
      cleanPurpose.includes('접대') || 
      cleanPurpose.includes('선물') || 
      cleanPurpose.includes('고객')) {
    return '개인경비_외부인 식사접대, 음료접대, 선물구입 등';
  }

  // 3. 야근 식대
  if (cleanPurpose.includes('야근')) {
    return '개인경비_야근식대';
  }

  // 4. 점심 식대
  if (cleanPurpose.includes('외근중식대') || 
      cleanPurpose.includes('점심') || 
      cleanPurpose.includes('중식') || 
      cleanPurpose.includes('식대')) {
    return '개인경비_점심식대';
  }

  // 5. 티미팅 (임직원)
  if (cleanPurpose.includes('간식') || 
      cleanPurpose.includes('티타임')) {
    return '개인경비_임직원 티미팅 비용 (접대용이 아닌 임직원 사용분)';
  }

  // 6. 수수료
  if (cleanPurpose.includes('수수료')) {
    return '개인경비_엄무상 지출되는 일반 수수료비용';
  }

  // 기본값 (매칭되는 키워드가 없을 경우 가장 일반적인 항목으로 처리하거나 '미분류'로 반환)
  // 여기서는 안전하게 교통비 또는 기타로 분류될 수 있도록 설정합니다.
  return '개인경비_외부인 식사접대, 음료접대, 선물구입 등';
}
