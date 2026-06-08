require('dotenv').config();
const { REST, Routes, Client, GatewayIntentBits } = require('discord.js');
const { voiceCommands } = require('./voicesystem');

const TOKEN = process.env.VOICE_BOT_TOKEN;
const GUILD_ID = process.env.DISCORD_GUILD_ID || '1508068507340771408';

if (!TOKEN) {
  console.error('Kein VOICE_BOT_TOKEN in .env!');
  process.exit(1);
}

// Login to get the client ID automatically
const client = new Client({ intents: [GatewayIntentBits.Guilds] });

client.once('ready', async () => {
  console.log(`Eingeloggt als ${client.user.tag} (ID: ${client.user.id})`);

  const rest = new REST({ version: '10' }).setToken(TOKEN);
  try {
    console.log(`Registriere ${voiceCommands.length} Voice-Commands...`);
    await rest.put(
      Routes.applicationGuildCommands(client.user.id, GUILD_ID),
      { body: voiceCommands },
    );
    console.log('✅ Alle Voice-Commands registriert!');
  } catch (e) {
    console.error('❌ Fehler:', e.message);
  }
  process.exit(0);
});

client.login(TOKEN);
