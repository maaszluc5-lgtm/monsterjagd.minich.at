require('dotenv').config();
const { Client, GatewayIntentBits, Events, REST, Routes, EmbedBuilder } = require('discord.js');

const TOKEN = process.env.EVENT_BOT_TOKEN;
const GUILD_ID = process.env.DISCORD_GUILD_ID || '1508068507340771408';
const OWNER_ID = process.env.DISCORD_OWNER_ID || '1447174849628868681';

const client = new Client({ intents: [GatewayIntentBits.Guilds] });

// eventId -> { title, desc, time, participants: Set<userId> }
const events = new Map();
let eventCounter = 1;

const commands = [
  {
    name: 'event-erstellen',
    description: 'Neues Event erstellen (nur Owner)',
    options: [
      { name: 'titel', type: 3, description: 'Event Titel', required: true },
      { name: 'beschreibung', type: 3, description: 'Event Beschreibung', required: true },
      { name: 'zeit', type: 3, description: 'Datum & Uhrzeit (z.B. Samstag 20:00)', required: true },
      { name: 'preis', type: 3, description: 'Preis/Belohnung', required: false },
      { name: 'ping', type: 8, description: 'Rolle anpingen', required: false },
    ],
  },
  {
    name: 'event-teilnehmen',
    description: 'An einem Event teilnehmen',
    options: [{ name: 'id', type: 4, description: 'Event ID', required: true }],
  },
  {
    name: 'event-verlassen',
    description: 'Event verlassen',
    options: [{ name: 'id', type: 4, description: 'Event ID', required: true }],
  },
  {
    name: 'event-liste',
    description: 'Alle aktiven Events anzeigen',
  },
  {
    name: 'event-info',
    description: 'Event Details anzeigen',
    options: [{ name: 'id', type: 4, description: 'Event ID', required: true }],
  },
  {
    name: 'event-löschen',
    description: 'Event löschen (nur Owner)',
    options: [{ name: 'id', type: 4, description: 'Event ID', required: true }],
  },
  {
    name: 'event-gewinner',
    description: 'Zufälligen Gewinner aus Teilnehmern ziehen (nur Owner)',
    options: [{ name: 'id', type: 4, description: 'Event ID', required: true }],
  },
  {
    name: 'giveaway',
    description: 'Giveaway starten (nur Owner)',
    options: [
      { name: 'preis', type: 3, description: 'Was wird verlost?', required: true },
      { name: 'dauer', type: 3, description: 'Dauer (z.B. 1h, 30m, 1d)', required: true },
      { name: 'gewinner', type: 4, description: 'Anzahl Gewinner', required: false },
    ],
  },
];

client.once(Events.ClientReady, async () => {
  console.log(`[EventBot] Eingeloggt als ${client.user.tag}`);
  const rest = new REST({ version: '10' }).setToken(TOKEN);
  await rest.put(Routes.applicationGuildCommands(client.user.id, GUILD_ID), { body: commands });
  console.log('[EventBot] Commands registriert.');
});

function parseDuration(str) {
  const match = str.match(/^(\d+)(s|m|h|d)$/);
  if (!match) return null;
  const val = parseInt(match[1]);
  const mult = { s: 1000, m: 60000, h: 3600000, d: 86400000 }[match[2]];
  return val * mult;
}

client.on(Events.InteractionCreate, async (interaction) => {
  if (!interaction.isChatInputCommand()) return;

  try {
    switch (interaction.commandName) {
      case 'event-erstellen': {
        if (interaction.user.id !== OWNER_ID) return interaction.reply({ content: '❌ Nur der Owner!', ephemeral: true });

        const id = eventCounter++;
        const titel = interaction.options.getString('titel');
        const beschreibung = interaction.options.getString('beschreibung');
        const zeit = interaction.options.getString('zeit');
        const preis = interaction.options.getString('preis') || 'Keine Angabe';
        const ping = interaction.options.getRole('ping');

        events.set(id, { titel, beschreibung, zeit, preis, participants: new Set() });

        const embed = new EmbedBuilder()
          .setTitle(`🎉 Event #${id}: ${titel}`)
          .setDescription(beschreibung)
          .addFields(
            { name: '⏰ Zeit', value: zeit, inline: true },
            { name: '🏆 Belohnung', value: preis, inline: true },
            { name: '👥 Teilnehmer', value: '0', inline: true },
          )
          .setColor(0xff6b6b)
          .setTimestamp()
          .setFooter({ text: `Event ID: ${id} | /event-teilnehmen ${id}` });

        const content = ping ? `${ping}` : undefined;
        await interaction.channel.send({ content, embeds: [embed] });
        return interaction.reply({ content: `✅ Event #${id} erstellt!`, ephemeral: true });
      }

      case 'event-teilnehmen': {
        const id = interaction.options.getInteger('id');
        const event = events.get(id);
        if (!event) return interaction.reply({ content: `❌ Event #${id} nicht gefunden!`, ephemeral: true });
        if (event.participants.has(interaction.user.id)) return interaction.reply({ content: '❌ Du nimmst bereits teil!', ephemeral: true });
        event.participants.add(interaction.user.id);
        return interaction.reply({ content: `✅ Du nimmst jetzt an **${event.titel}** teil! (${event.participants.size} Teilnehmer)`, ephemeral: true });
      }

      case 'event-verlassen': {
        const id = interaction.options.getInteger('id');
        const event = events.get(id);
        if (!event) return interaction.reply({ content: `❌ Event #${id} nicht gefunden!`, ephemeral: true });
        event.participants.delete(interaction.user.id);
        return interaction.reply({ content: `✅ Du hast **${event.titel}** verlassen.`, ephemeral: true });
      }

      case 'event-liste': {
        if (events.size === 0) return interaction.reply({ content: '📋 Keine aktiven Events.', ephemeral: true });
        const list = [...events.entries()].map(([id, e]) =>
          `**#${id}** ${e.titel} — ⏰ ${e.zeit} — 👥 ${e.participants.size} Teilnehmer`
        ).join('\n');
        const embed = new EmbedBuilder().setTitle('📋 Aktive Events').setDescription(list).setColor(0x5865f2);
        return interaction.reply({ embeds: [embed] });
      }

      case 'event-info': {
        const id = interaction.options.getInteger('id');
        const event = events.get(id);
        if (!event) return interaction.reply({ content: `❌ Event #${id} nicht gefunden!`, ephemeral: true });
        const embed = new EmbedBuilder()
          .setTitle(`🎉 Event #${id}: ${event.titel}`)
          .setDescription(event.beschreibung)
          .addFields(
            { name: '⏰ Zeit', value: event.zeit, inline: true },
            { name: '🏆 Belohnung', value: event.preis, inline: true },
            { name: '👥 Teilnehmer', value: String(event.participants.size), inline: true },
          )
          .setColor(0xff6b6b);
        return interaction.reply({ embeds: [embed] });
      }

      case 'event-löschen': {
        if (interaction.user.id !== OWNER_ID) return interaction.reply({ content: '❌ Nur der Owner!', ephemeral: true });
        const id = interaction.options.getInteger('id');
        if (!events.has(id)) return interaction.reply({ content: `❌ Event #${id} nicht gefunden!`, ephemeral: true });
        events.delete(id);
        return interaction.reply({ content: `✅ Event #${id} gelöscht.`, ephemeral: true });
      }

      case 'event-gewinner': {
        if (interaction.user.id !== OWNER_ID) return interaction.reply({ content: '❌ Nur der Owner!', ephemeral: true });
        const id = interaction.options.getInteger('id');
        const event = events.get(id);
        if (!event) return interaction.reply({ content: `❌ Event #${id} nicht gefunden!`, ephemeral: true });
        if (event.participants.size === 0) return interaction.reply({ content: '❌ Keine Teilnehmer!', ephemeral: true });
        const arr = [...event.participants];
        const winner = arr[Math.floor(Math.random() * arr.length)];
        const embed = new EmbedBuilder()
          .setTitle(`🏆 Gewinner: Event #${id}`)
          .setDescription(`🎉 <@${winner}> hat gewonnen!\n\n**Event:** ${event.titel}\n**Belohnung:** ${event.preis}`)
          .setColor(0xffd700)
          .setTimestamp();
        await interaction.channel.send({ embeds: [embed] });
        return interaction.reply({ content: '✅ Gewinner gezogen!', ephemeral: true });
      }

      case 'giveaway': {
        if (interaction.user.id !== OWNER_ID) return interaction.reply({ content: '❌ Nur der Owner!', ephemeral: true });
        const preis = interaction.options.getString('preis');
        const dauerStr = interaction.options.getString('dauer');
        const winnerCount = interaction.options.getInteger('gewinner') || 1;
        const ms = parseDuration(dauerStr);

        if (!ms) return interaction.reply({ content: '❌ Ungültige Dauer! Beispiel: 1h, 30m, 1d', ephemeral: true });

        const endsAt = new Date(Date.now() + ms);
        const embed = new EmbedBuilder()
          .setTitle('🎁 GIVEAWAY!')
          .setDescription(`**Preis:** ${preis}\n\nReagiere mit 🎉 um teilzunehmen!\n\n**Endet:** <t:${Math.floor(endsAt.getTime() / 1000)}:R>\n**Gewinner:** ${winnerCount}`)
          .setColor(0xff69b4)
          .setTimestamp(endsAt)
          .setFooter({ text: `Endet am` });

        const msg = await interaction.channel.send({ embeds: [embed] });
        await msg.react('🎉');

        setTimeout(async () => {
          try {
            const fetched = await msg.fetch();
            const reaction = fetched.reactions.cache.get('🎉');
            if (!reaction) return;
            const users = await reaction.users.fetch();
            const eligible = users.filter(u => !u.bot).map(u => u.id);
            if (eligible.length === 0) {
              await interaction.channel.send('❌ Keine Teilnehmer beim Giveaway!');
              return;
            }
            const winners = [];
            const pool = [...eligible];
            for (let i = 0; i < Math.min(winnerCount, pool.length); i++) {
              const idx = Math.floor(Math.random() * pool.length);
              winners.push(pool.splice(idx, 1)[0]);
            }
            const winnerMentions = winners.map(id => `<@${id}>`).join(', ');
            await interaction.channel.send(`🎉 Glückwunsch ${winnerMentions}! Ihr habt **${preis}** gewonnen!`);
          } catch (e) {
            console.error('[EventBot] Giveaway Fehler:', e);
          }
        }, ms);

        return interaction.reply({ content: `✅ Giveaway gestartet! Endet in ${dauerStr}.`, ephemeral: true });
      }
    }
  } catch (e) {
    console.error('[EventBot]', e);
    if (!interaction.replied) interaction.reply({ content: '❌ Fehler: ' + e.message, ephemeral: true });
  }
});

process.on('uncaughtException', e => console.error('[EventBot]', e));
process.on('unhandledRejection', e => console.error('[EventBot]', e));

client.login(TOKEN);
