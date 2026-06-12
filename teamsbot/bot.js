const { TeamsActivityHandler, CardFactory, MessageFactory, TurnContext } = require('botbuilder');
const { containsProfanity, censorText } = require('./profanityFilter');
const { logMessage } = require('./chatLogger');
const { fetchAllUsers, buildDuolingoCard } = require('./duolingo');
const { analyzeMessage, buildPhishingAlert } = require('./phishingDetector');

class MonsterjagdTeamsBot extends TeamsActivityHandler {
  constructor(adapter, conversationReferences) {
    super();
    this.adapter = adapter;
    this.conversationReferences = conversationReferences;

    this.onMessage(async (context, next) => {
      const text = context.activity.text?.trim() || '';

      logMessage(context.activity);

      const profanityResult = containsProfanity(text);
      if (profanityResult.hasProfanity) {
        context.activity._profanityResult = profanityResult;
        logMessage(context.activity);

        await context.sendActivity(
          `⚠️ **${context.activity.from.name}**, deine Nachricht enthält unangemessene Sprache und wurde markiert.`
        );
      }

      const phishingResult = analyzeMessage(text, context.activity.from?.name);
      if (phishingResult.level !== 'safe') {
        const alertCard = buildPhishingAlert(phishingResult, text, context.activity.from?.name);
        await context.sendActivity({
          attachments: [CardFactory.adaptiveCard(alertCard)],
        });
      }

      if (text.toLowerCase().startsWith('!duolingo')) {
        await this.handleDuolingoCommand(context);
      }

      if (text.toLowerCase().startsWith('!check')) {
        const messageToCheck = text.substring(6).trim();
        if (messageToCheck) {
          await this.handlePhishingCheck(context, messageToCheck);
        } else {
          await context.sendActivity('Verwendung: `!check <Nachricht oder URL zum Prüfen>`');
        }
      }

      await next();
    });

    this.onConversationUpdate(async (context, next) => {
      const membersAdded = context.activity.membersAdded;
      if (membersAdded) {
        for (const member of membersAdded) {
          if (member.id !== context.activity.recipient.id) {
            await context.sendActivity(
              `Willkommen **${member.name}**! 👋 Ich bin der Monsterjagd Teams Bot.\n\n` +
              `Befehle:\n` +
              `- \`!duolingo\` - Zeigt Duolingo-Fortschritt aller User\n` +
              `- \`!check <text>\` - Prüft eine Nachricht auf Phishing/Hacking\n\n` +
              `🛡️ Ich prüfe automatisch alle Nachrichten auf Phishing-Versuche!`
            );
          }
        }
      }
      await next();
    });
  }

  async handleDuolingoCommand(context) {
    const usernames = (process.env.DUOLINGO_USERS || '').split(',').filter(Boolean);
    if (usernames.length === 0) {
      await context.sendActivity('⚠️ Keine Duolingo-User konfiguriert. Setze `DUOLINGO_USERS` in der .env Datei.');
      return;
    }

    await context.sendActivity('🦉 Lade Duolingo-Daten...');

    const usersData = await fetchAllUsers(usernames);
    const card = buildDuolingoCard(usersData);

    await context.sendActivity({
      attachments: [CardFactory.adaptiveCard(card)],
    });
  }

  async handlePhishingCheck(context, text) {
    const result = analyzeMessage(text, null);
    if (result.level === 'safe') {
      await context.sendActivity('✅ Diese Nachricht sieht sicher aus. Keine Bedrohungen erkannt.');
    } else {
      const alertCard = buildPhishingAlert(result, text, 'Manueller Check');
      await context.sendActivity({
        attachments: [CardFactory.adaptiveCard(alertCard)],
      });
    }
  }

  addConversationReference(activity) {
    const ref = TurnContext.getConversationReference(activity);
    this.conversationReferences[ref.conversation.id] = ref;
  }

  async sendProactiveDuolingoUpdate() {
    const usernames = (process.env.DUOLINGO_USERS || '').split(',').filter(Boolean);
    if (usernames.length === 0) return;

    const usersData = await fetchAllUsers(usernames);
    const card = buildDuolingoCard(usersData);

    for (const ref of Object.values(this.conversationReferences)) {
      await this.adapter.continueConversation(ref, async (context) => {
        await context.sendActivity({
          attachments: [CardFactory.adaptiveCard(card)],
        });
      });
    }
  }
}

module.exports = MonsterjagdTeamsBot;
