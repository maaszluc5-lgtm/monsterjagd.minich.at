require('dotenv').config();
const mineflayer = require('mineflayer');
const { EventEmitter } = require('events');

const emitter = new EventEmitter();
const MAX_LOGS = 200;
const logs = [];

let bot = null;
let online = false;
let reconnectTimer = null;

function log(msg) {
  const line = `[${new Date().toISOString()}] ${msg}`;
  logs.push(line);
  if (logs.length > MAX_LOGS) logs.shift();
  console.log(line);
}

function getLogs(n = 30) {
  return logs.slice(-n).join('\n');
}

function isOnline() {
  return online && bot !== null;
}

function sendCommand(cmd) {
  if (!isOnline()) throw new Error('Bot ist offline');
  // strip leading slash if present
  bot.chat(cmd.startsWith('/') ? cmd : '/' + cmd);
}

function connect() {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }

  log(`Verbinde mit ${process.env.MC_HOST}:${process.env.MC_PORT} als ${process.env.MC_USERNAME}`);

  bot = mineflayer.createBot({
    host: process.env.MC_HOST,
    port: parseInt(process.env.MC_PORT) || 25565,
    username: process.env.MC_USERNAME,
    version: '1.21.4',
    auth: 'offline',
    physicsEnabled: false,
  });

  bot.once('spawn', () => {
    online = true;
    log('Bot ist online');
    emitter.emit('online');
  });

  bot.on('chat', (username, message) => {
    log(`<${username}> ${message}`);
    emitter.emit('chat', username, message);
  });

  bot.on('death', () => {
    log('Bot gestorben');
    emitter.emit('death');
  });

  bot.on('kicked', (reason) => {
    online = false;
    log(`Bot wurde gekickt: ${JSON.stringify(reason)}`);
    emitter.emit('kicked', reason);
    scheduleReconnect();
  });

  bot.on('error', (err) => {
    online = false;
    log(`Bot Fehler: ${err.message}`);
    emitter.emit('error', err);
  });

  bot.on('end', (reason) => {
    online = false;
    log(`Bot Verbindung getrennt: ${reason}`);
    emitter.emit('offline', reason);
    scheduleReconnect();
  });
}

function scheduleReconnect(delay = 30000) {
  if (reconnectTimer) return;
  log(`Reconnect in ${delay / 1000}s...`);
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null;
    connect();
  }, delay);
}

function reconnect() {
  if (bot) {
    try { bot.quit(); } catch (_) {}
    bot = null;
  }
  online = false;
  connect();
}

// watchdog: every 60s check if bot is connected, reconnect if not
setInterval(() => {
  if (!isOnline() && !reconnectTimer) {
    log('Watchdog: Bot offline, reconnecte...');
    emitter.emit('watchdog_reconnect');
    connect();
  }
}, 60_000);

module.exports = { connect, reconnect, sendCommand, isOnline, getLogs, emitter };
