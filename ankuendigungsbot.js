require('dotenv').config();
const { Client, GatewayIntentBits, Events, REST, Routes, EmbedBuilder } = require('discord.js');

const TOKEN = process.env.ANKUENDIGUNGS_BOT_TOKEN;
const GUILD_ID = process.env.DISCORD_GUILD_ID || '1508068507340771408';
const OWNER_ID = process.env.DISCORD_OWNER_ID || '1447174849628868681';

const client = new Client({ intents: [GatewayIntentBits.Guilds] });

const commands = [
  {
    name: 'ankündigung',
    description: 'Ankündigung posten (nur Owner)',
    options: [
      { name: 'titel', type: 3, description: 'Titel der Ankündigung', required: true },
      { name: 'text', type: 3, description: 'Text der Ankündigung', required: true },
      { name: 'farbe', type: 3, description: 'Farbe (rot/grün/blau/gelb/lila)', required: false },
      { name: 'ping', type: 8, description: 'Rolle anpingen', required: false },
    ],
  },
  {
    name: 'news',
    description: 'Server-News posten (nur Owner)',
    options: [
      { name: 'text', type: 3, description: 'News Text', required: true },
    ],
  },
  {
    name: 'wartung',
    description: 'Wartungs-Ankündigung posten (nur Owner)',
    options: [
      { name: 'start', type: 3, description: 'Wann beginnt die Wartung?', required: true },
      { name: 'dauer', type: 3, description: 'Wie lange?', required: true },
      { name: 'grund', type: 3, description: 'Grund', required: false },
    ],
  },
  {
    name: 'update',
    description: 'Update-Ankündigung posten (nur Owner)',
    options: [
      { name: 'version', type: 3, description: 'Version (z.B. 1.2.0)', required: true },
      { name: 'aenderungen', type: 3, description: 'Was ist neu? (mit | trennen)', required: true },
    ],
  },
];

client.once(Events.ClientReady, async () => {
  console.log(`[AnkündigungsBot] Eingeloggt als ${client.user.tag}`);
  const rest = new REST({ version: '10' }).setToken(TOKEN);
  await rest.put(Routes.applicationGuildCommands(client.user.id, GUILD_ID), { body: commands });
  console.log('[AnkündigungsBot] Commands registriert.');
});

function colorFromName(name) {
  const map = { rot: 0xff0000, grün: 0x00ff00, blau: 0x0099ff, gelb: 0xffff00, lila: 0x9b59b6 };
  return map[name?.toLowerCase()] || 0x5865f2;
}

client.on(Events.InteractionCreate, async (interaction) => {
  if (!interaction.isChatInputCommand()) return;

  if (interaction.user.id !== OWNER_ID) {
    return interaction.reply({ content: '❌ Nur der Server-Owner kann diesen Befehl nutzen.', ephemeral: true });
  }

  try {
    switch (interaction.commandName) {
      case 'ankündigung': {
        const titel = interaction.options.getString('titel');
        const text = interaction.options.getString('text');
        const farbe = interaction.options.getString('farbe');
        const ping = interaction.options.getRole('ping');

        const embed = new EmbedBuilder()
          .setTitle('📢 ' + titel)
          .setDescription(text)
          .setColor(colorFromName(farbe))
          .setTimestamp()
          .setFooter({ text: 'Monsterjagd Ankündigung' });

        const content = ping ? `${ping}` : undefined;
        await interaction.channel.send({ content, embeds: [embed] });
        return interaction.reply({ content: '✅ Ankündigung gepostet!', ephemeral: true });
      }

      case 'news': {
        const text = interaction.options.getString('text');
        const embed = new EmbedBuilder()
          .setTitle('📰 Server News')
          .setDescription(text)
          .setColor(0x00b0f4)
          .setTimestamp()
          .setFooter({ text: 'Monsterjagd News' });

        await interaction.channel.send({ embeds: [embed] });
        return interaction.reply({ content: '✅ News gepostet!', ephemeral: true });
      }

      case 'wartung': {
        const start = interaction.options.getString('start');
        const dauer = interaction.options.getString('dauer');
        const grund = interaction.options.getString('grund') || 'Allgemeine Wartungsarbeiten';

        const embed = new EmbedBuilder()
          .setTitle('🔧 Wartungsarbeiten')
          .setDescription('Der Server wird kurzzeitig offline sein.')
          .addFields(
            { name: '⏰ Beginn', value: start, inline: true },
            { name: '⌛ Dauer', value: dauer, inline: true },
            { name: '📋 Grund', value: grund },
          )
          .setColor(0xff9900)
          .setTimestamp()
          .setFooter({ text: 'Monsterjagd Wartung' });

        await interaction.channel.send({ embeds: [embed] });
        return interaction.reply({ content: '✅ Wartungs-Ankündigung gepostet!', ephemeral: true });
      }

      case 'update': {
        const version = interaction.options.getString('version');
        const aenderungen = interaction.options.getString('aenderungen');
        const list = aenderungen.split('|').map(s => `• ${s.trim()}`).join('\n');

        const embed = new EmbedBuilder()
          .setTitle(`🚀 Update v${version}`)
          .setDescription('Ein neues Update ist verfügbar!')
          .addFields({ name: '📝 Änderungen', value: list })
          .setColor(0x00ff88)
          .setTimestamp()
          .setFooter({ text: 'Monsterjagd Update' });

        await interaction.channel.send({ embeds: [embed] });
        return interaction.reply({ content: '✅ Update-Ankündigung gepostet!', ephemeral: true });
      }
    }
  } catch (e) {
    console.error('[AnkündigungsBot]', e);
    if (!interaction.replied) interaction.reply({ content: '❌ Fehler: ' + e.message, ephemeral: true });
  }
});

process.on('uncaughtException', e => console.error('[AnkündigungsBot]', e));
process.on('unhandledRejection', e => console.error('[AnkündigungsBot]', e));

client.login(TOKEN);
