/**
 * @file DriveService.gs
 * @description 구글 드라이브 관련 기능(폴더/파일 제어, PDF 생성)을 담당합니다.
 * @author 코딩봇
 */

/**
 * 폴더 내의 이미지 파일들을 지출일(파일명) 기준으로 정렬하여 반환합니다.
 */
function getSortedReceiptFiles_(folder) {
  const imageFiles = [];
  const files = folder.getFiles();
  while (files.hasNext()) {
    const file = files.next();
    if (file.getMimeType().startsWith('image/')) {
      imageFiles.push(file);
    }
  }
  
  imageFiles.sort((a, b) => {
    const nameA = a.getName().substring(0, 8);
    const nameB = b.getName().substring(0, 8);
    return nameA.localeCompare(nameB);
  });
  
  return imageFiles;
}

/**
 * 정렬된 영수증 파일들을 PDF로 병합합니다. (고급 모드)
 * - Drive API를 사용하여 고용량/특수 포맷 이미지를 표준 JPEG로 변환하여 가져옵니다.
 * - 'Invalid image data' 및 'Service error'를 원천 차단합니다.
 */
function createConsolidatedPdf_(folder, sortedFiles, monthTitle) {
  Logger.log(`=== PDF 생성 시작: ${monthTitle} (총 ${sortedFiles.length}개 파일) ===`);

  // 임시 구글 문서 생성
  const tempDoc = DocumentApp.create(`temp_${new Date().getTime()}`);
  const body = tempDoc.getBody();
  
  // 여백 최소화
  body.setMarginTop(10).setMarginBottom(10).setMarginLeft(10).setMarginRight(10);
  const pageWidth = body.getPageWidth() - body.getMarginLeft() - body.getMarginRight();
  const pageHeight = body.getPageHeight() - body.getMarginTop() - body.getMarginBottom() - 50;

  for (let i = 0; i < sortedFiles.length; i++) {
    const file = sortedFiles[i];
    const fileName = file.getName();
    const fileId = file.getId();

    try {
      Logger.log(`[처리중] ${fileName}`);

      // [핵심 변경] 원본 Blob 대신 Drive API를 통해 '표준화된 썸네일'을 가져옵니다.
      // 이렇게 하면 5MB짜리 HEIC/ProRaw 파일도 1MB 미만의 호환 가능한 JPG로 받아집니다.
      let imageBlob;
      try {
        // 1. Drive API로 파일 메타데이터(thumbnailLink) 조회
        const driveFile = Drive.Files.get(fileId, { fields: 'thumbnailLink' });
        
        if (driveFile.thumbnailLink) {
          // 2. 고화질 썸네일 URL 생성 (기본값은 작으므로 =s1200으로 변경하여 1200px 확보)
          // s220 -> s1200 (긴 축 기준 1200px)
          const highResLink = driveFile.thumbnailLink.replace(/=s\d+/, '=s1200');
          
          // 3. 해당 URL에서 이미지 데이터 다운로드
          imageBlob = UrlFetchApp.fetch(highResLink).getBlob();
        } else {
          // 썸네일 링크가 없는 경우 (드문 경우) 원본 사용
          Logger.log(`   ℹ️ 썸네일 링크 없음. 원본 사용 시도.`);
          imageBlob = file.getBlob();
        }
      } catch (apiError) {
        Logger.log(`   ⚠️ Drive API 실패 (${apiError.message}). 원본 Blob 사용.`);
        imageBlob = file.getBlob();
      }

      // Blob이 비어있거나 타입이 이상하면 강제 변환 시도
      if (!imageBlob.getContentType().startsWith('image/')) {
         imageBlob = imageBlob.getAs('image/jpeg');
      }

      // 문서 삽입
      const p = body.appendParagraph('');
      p.setAlignment(DocumentApp.HorizontalAlignment.CENTER);
      p.setSpacingAfter(0).setSpacingBefore(0);
      
      const image = p.insertInlineImage(0, imageBlob);
      
      const imgW = image.getWidth();
      const imgH = image.getHeight();

      const widthRatio = (imgW > pageWidth) ? (pageWidth / imgW) : 1;
      const heightRatio = (imgH > pageHeight) ? (pageHeight / imgH) : 1;
      const finalRatio = Math.min(widthRatio, heightRatio, 1);

      image.setWidth(imgW * finalRatio).setHeight(imgH * finalRatio);
      
      if (i < sortedFiles.length - 1) body.appendPageBreak();

    } catch (e) {
      Logger.log(`   🚨 처리 실패: ${fileName} - ${e.message}`);
      // 실패 시 빨간 텍스트로 표시하여 누락 사실을 알림
      const errP = body.appendParagraph(`[❌ 이미지 로드 실패: ${fileName}]`);
      errP.setForegroundColor("#FF0000").setAlignment(DocumentApp.HorizontalAlignment.CENTER);
      
      if (i < sortedFiles.length - 1) body.appendPageBreak();
      continue;
    }
  }

  tempDoc.saveAndClose();
  
  // PDF 변환
  const pdfBlob = tempDoc.getAs('application/pdf');
  const pdfFileName = `${monthTitle}_Alpha 개인경비 지출증빙.pdf`;
  pdfBlob.setName(pdfFileName);

  // 덮어쓰기 로직
  const existingFiles = folder.getFilesByName(pdfFileName);
  while (existingFiles.hasNext()) {
    existingFiles.next().setTrashed(true);
  }

  // 새 파일 생성
  const pdfFile = folder.createFile(pdfBlob);
  DriveApp.getFileById(tempDoc.getId()).setTrashed(true);

  Logger.log(`=== PDF 생성 완료: ${pdfFileName} ===`);
  return pdfFile;
}
