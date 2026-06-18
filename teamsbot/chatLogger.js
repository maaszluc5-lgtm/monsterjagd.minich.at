const fs = require('fs');
const path = require('path');

const LOGS_DIR = path.join(__dirname, 'logs');

if (!fs.existsSync(LOGS_DIR)) {
  fs.mkdirSync(LOGS_DIR, { recursive: true });
}

function getLogFilePath(channelId) {
  const date = new Date().toISOString().split('T')[0];
  const safeChannelId = channelId.replace(/[^a-zA-Z0-9_-]/g, '_');
  return path.join(LOGS_DIR, `${safeChannelId}_${date}.json`);
}

function readLog(filePath) {
  if (fs.existsSync(filePath)) {
    const data = fs.readFileSync(filePath, 'utf-8');
    return JSON.parse(data);
  }
  return { channelId: null, date: null, messages: [] };
}

function logMessage(activity) {
  const channelId = activity.channelData?.channel?.id || activity.conversation?.id || 'unknown';
  const filePath = getLogFilePath(channelId);
  const log = readLog(filePath);

  if (!log.channelId) {
    log.channelId = channelId;
    log.date = new Date().toISOString().split('T')[0];
  }

  const entry = {
    timestamp: new Date().toISOString(),
    userId: activity.from?.id,
    userName: activity.from?.name,
    text: activity.text || '',
    type: activity.type,
    messageId: activity.id,
  };

  if (activity._profanityResult) {
    entry.profanityDetected = true;
    entry.profanityWords = activity._profanityResult.words;
  }

  log.messages.push(entry);
  fs.writeFileSync(filePath, JSON.stringify(log, null, 2), 'utf-8');

  return entry;
}

function getLogsForChannel(channelId, date) {
  const filePath = getLogFilePath(channelId);
  return readLog(filePath);
}

module.exports = { logMessage, getLogsForChannel };
