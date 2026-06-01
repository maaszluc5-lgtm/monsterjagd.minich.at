require('dotenv').config();
const { Client, GatewayIntentBits, Events, EmbedBuilder } = require('discord.js');
const mcbot = require('./mcbot');

const client = new Client({ intents: [GatewayIntentBits.Guilds] });

// ── Helpers ───────────────────────────────────────────────────────────────

function hasPermission(interaction) {
  const roleId = process.env.DISCORD_ALLOWED_ROLE_ID;
  if (!roleId) {
    return interaction.guild?.ownerId === interaction.user.id;
  }
  return interaction.member?.roles?.cache?.has(roleId);
}

async function deny(interaction) {
  return interaction.reply({ content: 'Du hast keine Berechtigung fuer diesen Befehl.', ephemeral: true });
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

// ── Interaction handler ───────────────────────────────────────────────────

client.on(Events.InteractionCreate, async (interaction) => {
  if (!interaction.isChatInputCommand()) return;

  if (!hasPermission(interaction)) return deny(interaction);

  const { commandName, options } = interaction;

  try {
    switch (commandName) {

      // ── Raw / Chat ──────────────────────────────────────────────────────
      case 'mc': {
        const cmd = options.getString('command');
        run('/' + cmd);
        return interaction.reply({ content: `Ausgefuehrt: \`/${cmd}\``, ephemeral: true });
      }

      case 'say': {
        const msg = options.getString('message');
        run(`/say ${msg}`);
        return interaction.reply({ content: `Broadcast: **${msg}**`, ephemeral: true });
      }

      // ── Status ──────────────────────────────────────────────────────────
      case 'status': {
        const embed = new EmbedBuilder()
          .setTitle('Minecraft Bot Status')
          .addFields(
            { name: 'Online', value: mcbot.isOnline() ? 'Ja' : 'Nein', inline: true },
            { name: 'Server', value: `${process.env.MC_HOST}:${process.env.MC_PORT || 25565}`, inline: true },
            { name: 'Bot Name', value: process.env.MC_USERNAME || 'unbekannt', inline: true },
          )
          .setColor(mcbot.isOnline() ? 0x00ff00 : 0xff0000)
          .setTimestamp();
        return interaction.reply({ embeds: [embed] });
      }

      case 'list': {
        if (!mcbot.isOnline()) return interaction.reply('Bot ist offline.');
        run('/list');
        return interaction.reply({ content: 'Befehl gesendet — Antwort im Minecraft Chat.', ephemeral: true });
      }

      case 'logs': {
        const n = options.getInteger('lines') || 20;
        const text = mcbot.getLogs(n);
        return interaction.reply({ content: `\`\`\`\n${text || '(keine Logs)'}\n\`\`\``, ephemeral: true });
      }

      case 'restart': {
        await interaction.reply('Bot wird neu gestartet...');
        mcbot.reconnect();
        return;
      }

      // ── Player management ───────────────────────────────────────────────
      case 'op': {
        run(`/op ${options.getString('player')}`);
        return interaction.reply({ content: `OP gegeben: **${options.getString('player')}**`, ephemeral: true });
      }

      case 'deop': {
        run(`/deop ${options.getString('player')}`);
        return interaction.reply({ content: `OP entfernt: **${options.getString('player')}**`, ephemeral: true });
      }

      case 'kick': {
        const p = options.getString('player');
        const r = options.getString('reason') || '';
        run(`/kick ${p}${r ? ' ' + r : ''}`);
        return interaction.reply({ content: `Gekickt: **${p}**${r ? ` (${r})` : ''}`, ephemeral: true });
      }

      case 'ban': {
        const p = options.getString('player');
        const r = options.getString('reason') || '';
        run(`/ban ${p}${r ? ' ' + r : ''}`);
        return interaction.reply({ content: `Gebannt: **${p}**`, ephemeral: true });
      }

      case 'banip': {
        run(`/ban-ip ${options.getString('ip')}`);
        return interaction.reply({ content: `IP gebannt: **${options.getString('ip')}**`, ephemeral: true });
      }

      case 'pardon': {
        run(`/pardon ${options.getString('player')}`);
        return interaction.reply({ content: `Entbannt: **${options.getString('player')}**`, ephemeral: true });
      }

      case 'pardonip': {
        run(`/pardon-ip ${options.getString('ip')}`);
        return interaction.reply({ content: `IP entbannt: **${options.getString('ip')}**`, ephemeral: true });
      }

      case 'whitelist': {
        const action = options.getString('action');
        const player = options.getString('player') || '';
        if ((action === 'add' || action === 'remove') && !player) {
          return interaction.reply({ content: 'Bitte Spielername angeben.', ephemeral: true });
        }
        run(`/whitelist ${action}${player ? ' ' + player : ''}`);
        return interaction.reply({ content: `Whitelist ${action}${player ? ': **' + player + '**' : ''}`, ephemeral: true });
      }

      // ── World ───────────────────────────────────────────────────────────
      case 'gamemode': {
        const mode = options.getString('mode');
        const player = options.getString('player') || '';
        run(`/gamemode ${mode}${player ? ' ' + player : ''}`);
        return interaction.reply({ content: `Gamemode: **${mode}**${player ? ' fuer ' + player : ''}`, ephemeral: true });
      }

      case 'difficulty': {
        run(`/difficulty ${options.getString('level')}`);
        return interaction.reply({ content: `Schwierigkeit: **${options.getString('level')}**`, ephemeral: true });
      }

      case 'time': {
        run(`/time ${options.getString('action')} ${options.getString('value')}`);
        return interaction.reply({ content: `Zeit ${options.getString('action')}: **${options.getString('value')}**`, ephemeral: true });
      }

      case 'weather': {
        const type = options.getString('type');
        const dur = options.getInteger('duration');
        run(`/weather ${type}${dur ? ' ' + dur : ''}`);
        return interaction.reply({ content: `Wetter: **${type}**${dur ? ` fuer ${dur}s` : ''}`, ephemeral: true });
      }

      case 'gamerule': {
        const rule = options.getString('rule');
        const value = options.getString('value') || '';
        run(`/gamerule ${rule}${value ? ' ' + value : ''}`);
        return interaction.reply({ content: `Gamerule **${rule}**${value ? ' = ' + value : ''}`, ephemeral: true });
      }

      case 'setworldspawn': {
        run('/setworldspawn');
        return interaction.reply({ content: 'Weltspawn gesetzt.', ephemeral: true });
      }

      // ── Player actions ──────────────────────────────────────────────────
      case 'tp': {
        run(`/tp ${options.getString('target')} ${options.getString('destination')}`);
        return interaction.reply({ content: `Teleportiert: **${options.getString('target')}** -> **${options.getString('destination')}**`, ephemeral: true });
      }

      case 'tphere': {
        const p = options.getString('player');
        run(`/tp ${p} @s`);
        return interaction.reply({ content: `**${p}** zum Bot teleportiert.`, ephemeral: true });
      }

      case 'kill': {
        const p = options.getString('player') || '@a';
        run(`/kill ${p}`);
        return interaction.reply({ content: `Kill: **${p}**`, ephemeral: true });
      }

      case 'give': {
        const p = options.getString('player');
        const item = options.getString('item');
        const amt = options.getInteger('amount') || 1;
        run(`/give ${p} ${item} ${amt}`);
        return interaction.reply({ content: `Gegeben: **${amt}x ${item}** an **${p}**`, ephemeral: true });
      }

      case 'effect': {
        const p = options.getString('player');
        const eff = options.getString('effect');
        const dur = options.getInteger('duration') || 30;
        const amp = options.getInteger('amplifier') || 0;
        run(`/effect give ${p} ${eff} ${dur} ${amp}`);
        return interaction.reply({ content: `Effekt **${eff}** (${dur}s, Stärke ${amp}) an **${p}**`, ephemeral: true });
      }

      case 'enchant': {
        const p = options.getString('player');
        const ench = options.getString('enchantment');
        const lvl = options.getInteger('level') || 1;
        run(`/enchant ${p} ${ench} ${lvl}`);
        return interaction.reply({ content: `Verzauberung **${ench} ${lvl}** an **${p}**`, ephemeral: true });
      }

      case 'xp': {
        const p = options.getString('player');
        const amt = options.getInteger('amount');
        run(`/xp add ${p} ${amt}`);
        return interaction.reply({ content: `**${amt} XP** an **${p}** gegeben.`, ephemeral: true });
      }

      case 'clear': {
        const p = options.getString('player') || '@a';
        const item = options.getString('item') || '';
        run(`/clear ${p}${item ? ' ' + item : ''}`);
        return interaction.reply({ content: `Inventar geleert: **${p}**${item ? ` (${item})` : ''}`, ephemeral: true });
      }

      case 'spawnpoint': {
        const p = options.getString('player') || '@s';
        run(`/spawnpoint ${p}`);
        return interaction.reply({ content: `Spawnpunkt gesetzt fuer **${p}**`, ephemeral: true });
      }

      // ── Server management ───────────────────────────────────────────────
      case 'stop': {
        await interaction.reply('Server wird gestoppt...');
        run('/stop');
        return;
      }

      case 'saveall': {
        run('/save-all');
        return interaction.reply({ content: 'Welt wird gespeichert.', ephemeral: true });
      }

      case 'saveon': {
        run('/save-on');
        return interaction.reply({ content: 'Autosave aktiviert.', ephemeral: true });
      }

      case 'saveoff': {
        run('/save-off');
        return interaction.reply({ content: 'Autosave deaktiviert.', ephemeral: true });
      }

      default:
        return interaction.reply({ content: 'Unbekannter Befehl.', ephemeral: true });
    }
  } catch (err) {
    return interaction.reply({ content: `Fehler: ${err.message}`, ephemeral: true });
  }
});

// ── Watchdog notifications ─────────────────────────────────────────────────

mcbot.emitter.on('online', () => logChannel('Bot ist jetzt **online**.'));
mcbot.emitter.on('offline', (reason) => logChannel(`Bot ist **offline** gegangen: ${reason}`));
mcbot.emitter.on('kicked', (reason) => logChannel(`Bot wurde **gekickt**: ${reason}`));
mcbot.emitter.on('watchdog_reconnect', () => logChannel('Watchdog: Bot offline erkannt, verbinde neu...'));
mcbot.emitter.on('death', () => logChannel('Bot ist **gestorben**.'));

// ── Start ─────────────────────────────────────────────────────────────────

function start() {
  client.login(process.env.DISCORD_TOKEN);
  client.once(Events.ClientReady, () => {
    console.log(`Discord Bot eingeloggt als ${client.user.tag}`);
  });
}

module.exports = { start };
