require('dotenv').config();
const restify = require('restify');
const cron = require('node-cron');
const {
  CloudAdapter,
  ConfigurationBotFrameworkAuthentication,
} = require('botbuilder');
const MonsterjagdTeamsBot = require('./bot');

const server = restify.createServer();
server.use(restify.plugins.bodyParser());

const appId = process.env.MICROSOFT_APP_ID;
const authConfig = appId && appId !== 'your-app-id-here'
  ? { MicrosoftAppId: appId, MicrosoftAppPassword: process.env.MICROSOFT_APP_PASSWORD, MicrosoftAppType: 'SingleTenant' }
  : {};
const botFrameworkAuth = new ConfigurationBotFrameworkAuthentication(authConfig);

const adapter = new CloudAdapter(botFrameworkAuth);

adapter.onTurnError = async (context, error) => {
  console.error(`[onTurnError] ${error.message}`);
  console.error(error.stack);
  await context.sendActivity('❌ Ein Fehler ist aufgetreten. Bitte versuche es erneut.');
};

const conversationReferences = {};
const bot = new MonsterjagdTeamsBot(adapter, conversationReferences);

server.post('/api/messages', async (req, res) => {
  await adapter.process(req, res, (context) => {
    bot.addConversationReference(context.activity);
    return bot.run(context);
  });
});

server.get('/api/health', (req, res, next) => {
  res.send(200, { status: 'ok', bot: 'MonsterjagdTeamsBot' });
  next();
});

const PORT = process.env.PORT || 3978;
server.listen(PORT, () => {
  console.log(`\n🤖 Monsterjagd Teams Bot läuft auf Port ${PORT}`);
  console.log(`   Endpoint: http://localhost:${PORT}/api/messages`);
  console.log(`   Health:   http://localhost:${PORT}/api/health`);
});

const cronSchedule = process.env.DUOLINGO_CRON || '0 20 * * *';
if (cron.validate(cronSchedule)) {
  cron.schedule(cronSchedule, async () => {
    console.log('[Cron] Sende Duolingo Tages-Update...');
    try {
      await bot.sendProactiveDuolingoUpdate();
      console.log('[Cron] Duolingo Update gesendet.');
    } catch (err) {
      console.error('[Cron] Fehler beim Duolingo Update:', err.message);
    }
  });
  console.log(`📅 Duolingo Cron-Job aktiv: ${cronSchedule}`);
} else {
  console.warn(`⚠️ Ungültiger Cron-Schedule: ${cronSchedule}`);
}
