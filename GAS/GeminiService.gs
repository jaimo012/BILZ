/**
 * @file GeminiService.gs
 * @description Gemini API 호출 관련 기능을 담당합니다. (gemini-2.5-flash 모델 사용)
 */

/**
 * Gemini 2.5 Flash API를 호출하여 이미지에서 정보를 추출합니다.
 * @param {GoogleAppsScript.Base.Blob} imageBlob - 분석할 영수증 이미지 블랍
 * @param {string} apiKey - Gemini API 키
 * @returns {{vendor: string, amount: number}|null} 추출된 정보 또는 null
 */
function extractInfoWithGemini_(imageBlob, apiKey) {
  const API_ENDPOINT = `https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=${apiKey}`;
  
  const prompt = `
    Analyze the provided receipt image and extract the following information into a raw JSON object.
    1. "vendor": The name of the store, company, or service provider.
    2. "amount": The final total amount paid. It must be a number only, without commas or currency symbols.
    
    Your entire output MUST be only the raw JSON object. Do not include markdown like \`\`\`json or any other explanatory text.
    
    Example of a perfect response:
    {"vendor": "스타벅스 역삼대로점", "amount": 13500}
  `;

  const payload = {
    contents: [
      {
        parts: [
          { text: prompt },
          {
            inline_data: {
              mime_type: imageBlob.getContentType(),
              data: Utilities.base64Encode(imageBlob.getBytes()),
            },
          },
        ],
      },
    ],
  };

  const options = {
    method: 'post',
    contentType: 'application/json',
    payload: JSON.stringify(payload),
    muteHttpExceptions: true,
  };

  try {
    const response = UrlFetchApp.fetch(API_ENDPOINT, options);
    const responseCode = response.getResponseCode();
    const responseBody = response.getContentText();

    if (responseCode !== 200) {
      Logger.log(`Gemini API Error: Code ${responseCode}, Body: ${responseBody}`);
      return null;
    }
    
    const result = JSON.parse(responseBody);
    
    if (!result.candidates || result.candidates.length === 0) {
      Logger.log('Gemini response has no candidates.');
      return null;
    }

    let contentText = result.candidates[0].content.parts[0].text;
    
    const jsonMatch = contentText.match(/\{[\s\S]*\}/);
    if (jsonMatch && jsonMatch[0]) {
      contentText = jsonMatch[0];
    } else {
      Logger.log(`Failed to find a valid JSON object in the response text: ${contentText}`);
      return null;
    }

    const parsedContent = JSON.parse(contentText);

    return {
      vendor: parsedContent.vendor || '추출 실패',
      amount: Number(parsedContent.amount) || 0,
    };

  } catch (e) {
    Logger.log(`Exception while processing Gemini response: ${e.toString()}`);
    return null;
  }
}
