require('dotenv').config();
const { REST, Routes } = require('discord.js');
const commands = require('./commands');

const token = process.env.DISCORD_TOKEN;
const clientId = process.env.DISCORD_CLIENT_ID;
const guildId = process.env.DISCORD_GUILD_ID;

if (!token || !clientId || !guildId) {
  console.error('Fehlende Umgebungsvariablen: DISCORD_TOKEN, DISCORD_CLIENT_ID, DISCORD_GUILD_ID');
  process.exit(1);
}

const rest = new REST({ version: '10' }).setToken(token);

(async () => {
  console.log(`Registriere ${commands.length} Slash-Commands...`);
  await rest.put(
    Routes.applicationGuildCommands(clientId, guildId),
    { body: commands },
  );
  console.log('Fertig! Alle Commands sind jetzt auf deinem Discord Server verfügbar.');
})().catch(console.error);
