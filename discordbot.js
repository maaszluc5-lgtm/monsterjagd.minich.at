require('dotenv').config();
const { Client, GatewayIntentBits, Events, EmbedBuilder } = require('discord.js');
const mcbot = require('./mcbot');

const client = new Client({ intents: [GatewayIntentBits.Guilds] });

const OWNER_ID = '1447174849628868681';
const LIVE_CHANNEL_ID = process.env.DISCORD_LIVE_CHANNEL_ID || '1511341355647893667';

function isOwner(interaction) {
  return interaction.user.id === OWNER_ID;
}

async function deny(interaction) {
  return interaction.reply({ content: '❌ Nur der Server-Owner kann diesen Befehl nutzen.', ephemeral: true });
}

async function sendToLive(msg) {
  try {
    const ch = await client.channels.fetch(LIVE_CHANNEL_ID);
    if (ch) ch.send(msg);
  } catch (_) {}
}

async function logChannel(msg) {
  const channelId = process.env.DISCORD_LOG_CHANNEL_ID;
  if (!channelId) return;
  try {
    const ch = await client.channels.fetch(channelId);
    if (ch) ch.send(msg);
  } catch (_) {}
}

function run(cmd) {
  mcbot.sendCommand(cmd);
}

// ── Live Chat: Minecraft → Discord ────────────────────────────────────────────
mcbot.emitter.on('chat', (username, message) => {
  sendToLive(`💬 **${username}**: ${message}`);
});

mcbot.emitter.on('online', () => {
  sendToLive('🟢 **Bot ist online!**');
  logChannel('🟢 Bot ist jetzt **online**.');
});

mcbot.emitter.on('offline', (reason) => {
  sendToLive(`🔴 **Bot offline:** ${reason}`);
  logChannel(`🔴 Bot ist **offline**: ${reason}`);
});

mcbot.emitter.on('kicked', (reason) => {
  sendToLive(`⚠️ **Bot wurde gekickt:** ${reason}`);
  logChannel(`⚠️ Bot wurde **gekickt**: ${reason}`);
});

mcbot.emitter.on('death', () => {
  sendToLive('💀 **Bot ist gestorben!**');
});

mcbot.emitter.on('watchdog_reconnect', () => {
  sendToLive('🔄 **Watchdog:** Bot offline erkannt, verbinde neu...');
});

// ── Commands ──────────────────────────────────────────────────────────────────
client.on(Events.InteractionCreate, async (interaction) => {
  if (!interaction.isChatInputCommand()) return;

  if (!isOwner(interaction)) return deny(interaction);

  const { commandName, options } = interaction;

  try {
    switch (commandName) {

      case 'mc': {
        const cmd = options.getString('command');
        run('/' + cmd);
        return interaction.reply({ content: `✅ Ausgeführt: \`/${cmd}\``, ephemeral: true });
      }

      case 'mcsay': {
        const msg = options.getString('message');
        run(`/say ${msg}`);
        return interaction.reply({ content: `📢 Broadcast: **${msg}**`, ephemeral: true });
      }

      case 'mcstatus': {
        const embed = new EmbedBuilder()
          .setTitle('⚔ Monsterjagd — Bot Status')
          .addFields(
            { name: 'Online', value: mcbot.isOnline() ? '🟢 Ja' : '🔴 Nein', inline: true },
            { name: 'Server', value: `${process.env.MC_HOST || '?'}:${process.env.MC_PORT || 25565}`, inline: true },
            { name: 'Bot Name', value: process.env.MC_USERNAME || 'unbekannt', inline: true },
          )
          .setColor(mcbot.isOnline() ? 0x00ff00 : 0xff0000)
          .setTimestamp();
        return interaction.reply({ embeds: [embed] });
      }

      case 'mclist': {
        if (!mcbot.isOnline()) return interaction.reply({ content: '🔴 Bot ist offline.', ephemeral: true });
        run('/list');
        return interaction.reply({ content: '✅ Befehl gesendet — Antwort im #live Kanal.', ephemeral: true });
      }

      case 'mclogs': {
        const n = options.getInteger('lines') || 20;
        const text = mcbot.getLogs(n);
        return interaction.reply({ content: `\`\`\`\n${text || '(keine Logs)'}\n\`\`\``, ephemeral: true });
      }

      case 'mcrestart': {
        await interaction.reply('🔄 Bot wird neu gestartet...');
        mcbot.reconnect();
        return;
      }

      case 'mcop': {
        run(`/op ${options.getString('player')}`);
        return interaction.reply({ content: `✅ OP gegeben: **${options.getString('player')}**`, ephemeral: true });
      }

      case 'mcdeop': {
        run(`/deop ${options.getString('player')}`);
        return interaction.reply({ content: `✅ OP entfernt: **${options.getString('player')}**`, ephemeral: true });
      }

      case 'mckick': {
        const p = options.getString('player');
        const r = options.getString('reason') || '';
        run(`/kick ${p}${r ? ' ' + r : ''}`);
        return interaction.reply({ content: `👢 Gekickt: **${p}**${r ? ` (${r})` : ''}`, ephemeral: true });
      }

      case 'mcban': {
        const p = options.getString('player');
        const r = options.getString('reason') || '';
        run(`/ban ${p}${r ? ' ' + r : ''}`);
        return interaction.reply({ content: `🔨 Gebannt: **${p}**`, ephemeral: true });
      }

      case 'mcbanip': {
        run(`/ban-ip ${options.getString('ip')}`);
        return interaction.reply({ content: `🔨 IP gebannt: **${options.getString('ip')}**`, ephemeral: true });
      }

      case 'mcpardon': {
        run(`/pardon ${options.getString('player')}`);
        return interaction.reply({ content: `✅ Entbannt: **${options.getString('player')}**`, ephemeral: true });
      }

      case 'mcpardonip': {
        run(`/pardon-ip ${options.getString('ip')}`);
        return interaction.reply({ content: `✅ IP entbannt: **${options.getString('ip')}**`, ephemeral: true });
      }

      case 'mcwhitelist': {
        const action = options.getString('action');
        const player = options.getString('player') || '';
        if ((action === 'add' || action === 'remove') && !player) {
          return interaction.reply({ content: '❌ Bitte Spielername angeben.', ephemeral: true });
        }
        run(`/whitelist ${action}${player ? ' ' + player : ''}`);
        return interaction.reply({ content: `✅ Whitelist **${action}**${player ? ': ' + player : ''}`, ephemeral: true });
      }

      case 'mcgamemode': {
        const mode = options.getString('mode');
        const player = options.getString('player') || '';
        run(`/gamemode ${mode}${player ? ' ' + player : ''}`);
        return interaction.reply({ content: `✅ Gamemode: **${mode}**${player ? ' für ' + player : ''}`, ephemeral: true });
      }

      case 'mcdifficulty': {
        run(`/difficulty ${options.getString('level')}`);
        return interaction.reply({ content: `✅ Schwierigkeit: **${options.getString('level')}**`, ephemeral: true });
      }

      case 'mctime': {
        run(`/time ${options.getString('action')} ${options.getString('value')}`);
        return interaction.reply({ content: `✅ Zeit: **${options.getString('action')} ${options.getString('value')}**`, ephemeral: true });
      }

      case 'mcweather': {
        const type = options.getString('type');
        const dur = options.getInteger('duration');
        run(`/weather ${type}${dur ? ' ' + dur : ''}`);
        return interaction.reply({ content: `✅ Wetter: **${type}**${dur ? ` für ${dur}s` : ''}`, ephemeral: true });
      }

      case 'mcgamerule': {
        const rule = options.getString('rule');
        const value = options.getString('value') || '';
        run(`/gamerule ${rule}${value ? ' ' + value : ''}`);
        return interaction.reply({ content: `✅ Gamerule **${rule}**${value ? ' = ' + value : ''}`, ephemeral: true });
      }

      case 'mctp': {
        run(`/tp ${options.getString('target')} ${options.getString('destination')}`);
        return interaction.reply({ content: `✅ Teleportiert: **${options.getString('target')}** → **${options.getString('destination')}**`, ephemeral: true });
      }

      case 'mctphere': {
        const p = options.getString('player');
        run(`/tp ${p} @s`);
        return interaction.reply({ content: `✅ **${p}** zum Bot teleportiert.`, ephemeral: true });
      }

      case 'mckill': {
        const p = options.getString('player') || '@a';
        run(`/kill ${p}`);
        return interaction.reply({ content: `💀 Kill: **${p}**`, ephemeral: true });
      }

      case 'mcgive': {
        const p = options.getString('player');
        const item = options.getString('item');
        const amt = options.getInteger('amount') || 1;
        run(`/give ${p} ${item} ${amt}`);
        return interaction.reply({ content: `✅ **${amt}x ${item}** an **${p}** gegeben.`, ephemeral: true });
      }

      case 'mceffect': {
        const p = options.getString('player');
        const eff = options.getString('effect');
        const dur = options.getInteger('duration') || 30;
        const amp = options.getInteger('amplifier') || 0;
        run(`/effect give ${p} ${eff} ${dur} ${amp}`);
        return interaction.reply({ content: `✅ Effekt **${eff}** an **${p}**`, ephemeral: true });
      }

      case 'mcenchant': {
        const p = options.getString('player');
        const ench = options.getString('enchantment');
        const lvl = options.getInteger('level') || 1;
        run(`/enchant ${p} ${ench} ${lvl}`);
        return interaction.reply({ content: `✅ Verzauberung **${ench} ${lvl}** an **${p}**`, ephemeral: true });
      }

      case 'mcxp': {
        const p = options.getString('player');
        const amt = options.getInteger('amount');
        run(`/xp add ${p} ${amt}`);
        return interaction.reply({ content: `✅ **${amt} XP** an **${p}**`, ephemeral: true });
      }

      case 'mcclear': {
        const p = options.getString('player') || '@a';
        const item = options.getString('item') || '';
        run(`/clear ${p}${item ? ' ' + item : ''}`);
        return interaction.reply({ content: `✅ Inventar geleert: **${p}**`, ephemeral: true });
      }

      case 'mcstop': {
        await interaction.reply('⛔ Server wird gestoppt...');
        run('/stop');
        return;
      }

      case 'mcsaveall': {
        run('/save-all');
        return interaction.reply({ content: '✅ Welt wird gespeichert.', ephemeral: true });
      }

      case 'mcrang': {
        const p = options.getString('player');
        const rang = options.getString('rang');
        // LuckPerms command to set rank
        run(`/lp user ${p} parent set ${rang}`);
        return interaction.reply({ content: `✅ Rang **${rang}** an **${p}** vergeben!`, ephemeral: true });
      }

      default:
        return interaction.reply({ content: '❌ Unbekannter Befehl.', ephemeral: true });
    }
  } catch (err) {
    return interaction.reply({ content: `❌ Fehler: ${err.message}`, ephemeral: true });
  }
});

function start() {
  client.login(process.env.DISCORD_TOKEN);
  client.once(Events.ClientReady, () => {
    console.log(`Discord Bot eingeloggt als ${client.user.tag}`);
  });
}

module.exports = { start };
