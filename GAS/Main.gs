/**
 * @file Main.gs
 * @description 스크립트의 메인 실행 흐름과 전역 설정을 관리합니다.
 * @author 코딩봇
 */

// --- UI 및 실행 함수 ---

function onOpen() {
  SpreadsheetApp.getUi()
      .createMenu('영수증 처리')
      .addItem('📥 미처리 영수증 일괄 분석', 'processUnprocessedReceipts')
      .addToUi();
}

/**
 * 통합 폴더에 있는 영수증을 분석하고(없으면 패스), 최신 현황을 슬랙으로 보고합니다.
 */
function processUnprocessedReceipts() {
  _processReceiptsCore();
}


// --- 핵심 처리 로직 ---

function _processReceiptsCore() {
  const CONSTS = getScriptConstants_();
  
  try {
    const targetFolder = DriveApp.getFolderById(CONSTS.BASE_FOLDER_ID);
    
    // 1. 폴더 확인
    if (!targetFolder) {
      sendSlackNotification_(CONSTS.SLACK_WEBHOOK_URL, `🚨 폴더 오류: 설정된 ID(${CONSTS.BASE_FOLDER_ID})의 폴더를 찾을 수 없습니다.`);
      return;
    }

    // 2. 폴더 내 모든 이미지 파일 가져오기 (PDF 생성 등에 사용)
    const allReceiptFiles = getSortedReceiptFiles_(targetFolder);
    if (allReceiptFiles.length === 0) {
      sendSlackNotification_(CONSTS.SLACK_WEBHOOK_URL, `📂 [${targetFolder.getName()}] 폴더가 비어있습니다. 파일이 없습니다.`);
      return;
    }

    // 3. 미처리 파일 식별
    const processedFileNames = getExistingFileNames_(CONSTS.BASE_SHEET_ID, CONSTS.TARGET_SHEET_NAME);
    const filesToAnalyze = allReceiptFiles.filter(file => !processedFileNames.has(file.getName()));

    // 4. Gemini 분석 및 시트 기록 (할 게 있을 때만 실행)
    const newlyProcessedInfos = []; 
    
    if (filesToAnalyze.length > 0) {
      for (const file of filesToAnalyze) {
        const fileInfo = processSingleReceipt_(file, CONSTS.GEMINI_API_KEY);
        if (fileInfo) {
          logToSheet_(CONSTS.BASE_SHEET_ID, CONSTS.TARGET_SHEET_NAME, fileInfo);
          newlyProcessedInfos.push(fileInfo);
        }
      }
    }

    // 5. 기준 날짜(Anchor Date) 선정 로직
    // - 신규 처리 건이 있으면: 그 중 가장 최근 날짜 사용
    // - 신규 처리 건이 없으면: 오늘 날짜 사용 (단순 현황 보고용)
    let maxDate;
    if (newlyProcessedInfos.length > 0) {
      const maxDateStr = newlyProcessedInfos.map(info => info.expenseDate).sort().pop();
      maxDate = new Date(maxDateStr);
    } else {
      maxDate = new Date(); // 오늘
    }
    
    const prevDate = new Date(maxDate);
    prevDate.setMonth(maxDate.getMonth() - 1); // 지난 달 기준

    // 6. 이번 달 & 지난 달 PDF 갱신 (덮어쓰기) 및 통계 산출
    // (처리가 없더라도 PDF는 최신 상태로 갱신하여 덮어씁니다)
    const currentMonthStats = processMonthClose_(targetFolder, allReceiptFiles, maxDate, CONSTS);
    const prevMonthStats = processMonthClose_(targetFolder, allReceiptFiles, prevDate, CONSTS);

    // 7. 결과 알림 발송
    const slackUserId = CONSTS.SLACK_USER_ID ? `<@${CONSTS.SLACK_USER_ID}>` : '';
    const todayStr = Utilities.formatDate(new Date(), 'GMT+9', 'MM/dd HH:mm');
    
    // 신규 건수 강조 여부
    const countMsg = newlyProcessedInfos.length > 0 
      ? `이번에 *${newlyProcessedInfos.length}건*을 새로 처리했습니다.` 
      : `새로 처리된 내역은 없습니다. (현재 기준 현황)`;

    const successMessage = `${slackUserId} *[${todayStr}] 영수증 처리 및 월간 현황* :credit_card:
${countMsg}

${currentMonthStats}

${prevMonthStats}

• *전체내역*: <https://docs.google.com/spreadsheets/d/${CONSTS.BASE_SHEET_ID}/edit|📂 시트 확인>
<https://eapprove-kmong.cloud-office.co.kr/approval/documentPicker.do?activeMenuId=documentPickerActTab|🔗 그룹웨어 접속>`;

    sendSlackNotification_(CONSTS.SLACK_WEBHOOK_URL, successMessage);

  } catch (error) {
    Logger.log(`오류 발생: ${error.toString()}\n${error.stack}`);
    const errorMessage = `🚨 스크립트 실행 중 오류가 발생했습니다.\n- 오류: ${error.toString()}`;
    sendSlackNotification_(CONSTS.SLACK_WEBHOOK_URL, errorMessage);
  }
}

/**
 * 특정 월에 대한 PDF를 생성(덮어쓰기)하고 통계를 계산하여 슬랙 메시지 파트를 반환합니다.
 */
function processMonthClose_(folder, allFiles, dateObj, CONSTS) {
  const monthPrefix = Utilities.formatDate(dateObj, 'GMT+9', 'yyyyMM'); // 파일명 필터 (202402)
  const monthTitle = Utilities.formatDate(dateObj, 'GMT+9', 'yyyy.MM월'); // 표시용 (2024.02월)
  
  // 해당 월 파일 필터링
  const monthFiles = allFiles.filter(f => f.getName().startsWith(monthPrefix));
  
  let pdfUrl = "(파일 없음)";
  let fileCount = 0;

  // 파일이 있으면 PDF 생성 (기존 것 덮어쓰기)
  if (monthFiles.length > 0) {
    const pdfFile = createConsolidatedPdf_(folder, monthFiles, monthTitle);
    pdfUrl = `<${pdfFile.getUrl()}|다운로드>`;
    fileCount = monthFiles.length;
  }

  // 월간 총 지출액 계산
  const totalAmount = getTotalAmount_(CONSTS.BASE_SHEET_ID, CONSTS.TARGET_SHEET_NAME, dateObj);

  return `*📅 ${monthTitle} 현황*
>• 합계: *${totalAmount.toLocaleString('ko-KR')}원*
>• 증빙: ${pdfUrl} (총 ${fileCount}장)`;
}

/**
 * 스크립트 속성 가져오기
 */
function getScriptConstants_() {
  const properties = PropertiesService.getScriptProperties();
  return {
    GEMINI_API_KEY: properties.getProperty('GEMINI_API_KEY'),
    SLACK_WEBHOOK_URL: properties.getProperty('SLACK_WEBHOOK_URL'),
    BASE_FOLDER_ID: properties.getProperty('BASE_FOLDER_ID'),
    BASE_SHEET_ID: properties.getProperty('BASE_SHEET_ID'),
    TARGET_SHEET_NAME: 'RAW',
    SLACK_USER_ID: properties.getProperty('SLACK_USER_ID'), 
  };
}
