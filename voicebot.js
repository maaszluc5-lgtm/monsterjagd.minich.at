require('dotenv').config();
const { Client, GatewayIntentBits, Events, REST, Routes } = require('discord.js');
const { handleVoiceCreate, handleVoiceLeave, handleVoiceCommand, voiceCommands, isVoiceCommand } = require('./voicesystem');

const TOKEN = process.env.VOICE_BOT_TOKEN;
const CLIENT_ID = process.env.VOICE_BOT_CLIENT_ID;
const GUILD_ID = process.env.DISCORD_GUILD_ID || '1508068507340771408';

const client = new Client({
  intents: [
    GatewayIntentBits.Guilds,
    GatewayIntentBits.GuildVoiceStates,
  ],
});

client.once(Events.ClientReady, async () => {
  console.log(`[VoiceBot] Eingeloggt als ${client.user.tag}`);

  // Auto-register slash commands on startup
  try {
    const rest = new REST({ version: '10' }).setToken(TOKEN);
    await rest.put(
      Routes.applicationGuildCommands(CLIENT_ID || client.user.id, GUILD_ID),
      { body: voiceCommands },
    );
    console.log(`[VoiceBot] ${voiceCommands.length} Voice-Commands registriert.`);
  } catch (e) {
    console.error('[VoiceBot] Command-Registrierung fehlgeschlagen:', e.message);
  }
});

client.on(Events.VoiceStateUpdate, async (oldState, newState) => {
  try {
    const joined = newState.channel && (!oldState.channel || oldState.channelId !== newState.channelId);
    const left = oldState.channel && (!newState.channel || oldState.channelId !== newState.channelId);
    if (joined) await handleVoiceCreate(newState.member, newState.channel);
    if (left) await handleVoiceLeave(oldState.member, oldState.channel);
  } catch (e) {
    console.error('[VoiceBot] VoiceStateUpdate Fehler:', e);
  }
});

client.on(Events.InteractionCreate, async (interaction) => {
  if (!interaction.isChatInputCommand()) return;
  if (isVoiceCommand(interaction.commandName)) {
    return handleVoiceCommand(interaction);
  }
});

process.on('uncaughtException', (err) => {
  console.error('[VoiceBot] Uncaught Exception:', err);
});

process.on('unhandledRejection', (reason) => {
  console.error('[VoiceBot] Unhandled Rejection:', reason);
});

client.login(TOKEN).catch(err => {
  console.error('[VoiceBot] Login fehlgeschlagen:', err.message);
  process.exit(1);
});
