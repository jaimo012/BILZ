/**
 * @file SlackService.gs
 * @description Slack 알림 발송 기능을 담당합니다.
 */

/**
 * 슬랙으로 알림 메시지를 보냅니다.
 * @param {string} webhookUrl - 슬랙 웹훅 URL
 * @param {string} message - 보낼 메시지
 */
function sendSlackNotification_(webhookUrl, message) {
  if (!webhookUrl) {
    Logger.log('Slack Webhook URL이 설정되지 않아 알림을 보내지 않습니다.');
    Logger.log(`메시지 내용: ${message}`);
    return;
  }
  
  const payload = {
    text: message,
  };
  
    const options = {
    method: 'post',
    contentType: 'application/json',
    payload: JSON.stringify(payload),
    muteHttpExceptions: true,
  };
  
  UrlFetchApp.fetch(webhookUrl, options);
}
