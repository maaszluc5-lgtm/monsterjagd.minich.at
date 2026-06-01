const { SlashCommandBuilder } = require('discord.js');

const GAMEMODES = ['survival', 'creative', 'adventure', 'spectator'];
const DIFFICULTIES = ['peaceful', 'easy', 'normal', 'hard'];
const WEATHER_TYPES = ['clear', 'rain', 'thunder'];

module.exports = [
  // ── Raw command ──────────────────────────────────────────────────────────
  new SlashCommandBuilder()
    .setName('mc')
    .setDescription('Sende einen beliebigen Minecraft-Befehl')
    .addStringOption(o => o.setName('command').setDescription('Befehl (ohne /)').setRequired(true)),

  new SlashCommandBuilder()
    .setName('say')
    .setDescription('Broadcast eine Nachricht im Minecraft Chat')
    .addStringOption(o => o.setName('message').setDescription('Nachricht').setRequired(true)),

  // ── Status ────────────────────────────────────────────────────────────────
  new SlashCommandBuilder()
    .setName('status')
    .setDescription('Zeigt den Bot-Status an'),

  new SlashCommandBuilder()
    .setName('list')
    .setDescription('Zeigt alle Online-Spieler'),

  new SlashCommandBuilder()
    .setName('logs')
    .setDescription('Zeigt die letzten Bot-Logs')
    .addIntegerOption(o => o.setName('lines').setDescription('Anzahl Zeilen (Standard: 20)').setMinValue(1).setMaxValue(50)),

  new SlashCommandBuilder()
    .setName('restart')
    .setDescription('Startet den Minecraft Bot neu'),

  // ── Player management ─────────────────────────────────────────────────────
  new SlashCommandBuilder()
    .setName('op')
    .setDescription('Gibt einem Spieler Operator-Rechte')
    .addStringOption(o => o.setName('player').setDescription('Spieler').setRequired(true)),

  new SlashCommandBuilder()
    .setName('deop')
    .setDescription('Entfernt Operator-Rechte')
    .addStringOption(o => o.setName('player').setDescription('Spieler').setRequired(true)),

  new SlashCommandBuilder()
    .setName('kick')
    .setDescription('Kickt einen Spieler')
    .addStringOption(o => o.setName('player').setDescription('Spieler').setRequired(true))
    .addStringOption(o => o.setName('reason').setDescription('Grund')),

  new SlashCommandBuilder()
    .setName('ban')
    .setDescription('Bannt einen Spieler')
    .addStringOption(o => o.setName('player').setDescription('Spieler').setRequired(true))
    .addStringOption(o => o.setName('reason').setDescription('Grund')),

  new SlashCommandBuilder()
    .setName('banip')
    .setDescription('Bannt eine IP-Adresse')
    .addStringOption(o => o.setName('ip').setDescription('IP-Adresse').setRequired(true)),

  new SlashCommandBuilder()
    .setName('pardon')
    .setDescription('Entbannt einen Spieler')
    .addStringOption(o => o.setName('player').setDescription('Spieler').setRequired(true)),

  new SlashCommandBuilder()
    .setName('pardonip')
    .setDescription('Entbannt eine IP-Adresse')
    .addStringOption(o => o.setName('ip').setDescription('IP-Adresse').setRequired(true)),

  // ── Whitelist ─────────────────────────────────────────────────────────────
  new SlashCommandBuilder()
    .setName('whitelist')
    .setDescription('Whitelist verwalten')
    .addStringOption(o =>
      o.setName('action').setDescription('Aktion').setRequired(true)
        .addChoices(
          { name: 'add', value: 'add' },
          { name: 'remove', value: 'remove' },
          { name: 'list', value: 'list' },
          { name: 'on', value: 'on' },
          { name: 'off', value: 'off' },
        ))
    .addStringOption(o => o.setName('player').setDescription('Spieler (bei add/remove)')),

  // ── World ─────────────────────────────────────────────────────────────────
  new SlashCommandBuilder()
    .setName('gamemode')
    .setDescription('Ändert den Spielmodus')
    .addStringOption(o =>
      o.setName('mode').setDescription('Modus').setRequired(true)
        .addChoices(...GAMEMODES.map(m => ({ name: m, value: m }))))
    .addStringOption(o => o.setName('player').setDescription('Spieler (optional)')),

  new SlashCommandBuilder()
    .setName('difficulty')
    .setDescription('Ändert den Schwierigkeitsgrad')
    .addStringOption(o =>
      o.setName('level').setDescription('Schwierigkeit').setRequired(true)
        .addChoices(...DIFFICULTIES.map(d => ({ name: d, value: d })))),

  new SlashCommandBuilder()
    .setName('time')
    .setDescription('Ändert die Weltzeit')
    .addStringOption(o =>
      o.setName('action').setDescription('set oder add').setRequired(true)
        .addChoices({ name: 'set', value: 'set' }, { name: 'add', value: 'add' }))
    .addStringOption(o => o.setName('value').setDescription('Wert (z.B. day, night, 1000)').setRequired(true)),

  new SlashCommandBuilder()
    .setName('weather')
    .setDescription('Ändert das Wetter')
    .addStringOption(o =>
      o.setName('type').setDescription('Wettertyp').setRequired(true)
        .addChoices(...WEATHER_TYPES.map(w => ({ name: w, value: w }))))
    .addIntegerOption(o => o.setName('duration').setDescription('Dauer in Sekunden')),

  new SlashCommandBuilder()
    .setName('gamerule')
    .setDescription('Ändert eine Gamerule')
    .addStringOption(o => o.setName('rule').setDescription('Regel (z.B. keepInventory)').setRequired(true))
    .addStringOption(o => o.setName('value').setDescription('Wert (z.B. true/false)')),

  new SlashCommandBuilder()
    .setName('setworldspawn')
    .setDescription('Setzt den Weltspawnpunkt auf die aktuelle Bot-Position'),

  // ── Player actions ────────────────────────────────────────────────────────
  new SlashCommandBuilder()
    .setName('tp')
    .setDescription('Teleportiert einen Spieler')
    .addStringOption(o => o.setName('target').setDescription('Spieler der teleportiert wird').setRequired(true))
    .addStringOption(o => o.setName('destination').setDescription('Ziel (Spieler oder x y z)').setRequired(true)),

  new SlashCommandBuilder()
    .setName('tphere')
    .setDescription('Teleportiert einen Spieler zum Bot')
    .addStringOption(o => o.setName('player').setDescription('Spieler').setRequired(true)),

  new SlashCommandBuilder()
    .setName('kill')
    .setDescription('Tötet einen Spieler oder alle')
    .addStringOption(o => o.setName('player').setDescription('Spieler (leer = alle)')),

  new SlashCommandBuilder()
    .setName('give')
    .setDescription('Gibt einem Spieler einen Gegenstand')
    .addStringOption(o => o.setName('player').setDescription('Spieler').setRequired(true))
    .addStringOption(o => o.setName('item').setDescription('Item (z.B. minecraft:diamond)').setRequired(true))
    .addIntegerOption(o => o.setName('amount').setDescription('Menge').setMinValue(1).setMaxValue(64)),

  new SlashCommandBuilder()
    .setName('effect')
    .setDescription('Gibt einem Spieler einen Effekt')
    .addStringOption(o => o.setName('player').setDescription('Spieler').setRequired(true))
    .addStringOption(o => o.setName('effect').setDescription('Effekt (z.B. speed)').setRequired(true))
    .addIntegerOption(o => o.setName('duration').setDescription('Dauer in Sekunden').setMinValue(1))
    .addIntegerOption(o => o.setName('amplifier').setDescription('Stärke (0-255)').setMinValue(0).setMaxValue(255)),

  new SlashCommandBuilder()
    .setName('enchant')
    .setDescription('Verzaubert den Item in der Hand eines Spielers')
    .addStringOption(o => o.setName('player').setDescription('Spieler').setRequired(true))
    .addStringOption(o => o.setName('enchantment').setDescription('Verzauberung (z.B. sharpness)').setRequired(true))
    .addIntegerOption(o => o.setName('level').setDescription('Level').setMinValue(1)),

  new SlashCommandBuilder()
    .setName('xp')
    .setDescription('Gibt einem Spieler Erfahrungspunkte')
    .addStringOption(o => o.setName('player').setDescription('Spieler').setRequired(true))
    .addIntegerOption(o => o.setName('amount').setDescription('Menge').setRequired(true)),

  new SlashCommandBuilder()
    .setName('clear')
    .setDescription('Leert das Inventar eines Spielers')
    .addStringOption(o => o.setName('player').setDescription('Spieler'))
    .addStringOption(o => o.setName('item').setDescription('Item (optional)')),

  new SlashCommandBuilder()
    .setName('spawnpoint')
    .setDescription('Setzt den Spawnpunkt eines Spielers')
    .addStringOption(o => o.setName('player').setDescription('Spieler')),

  // ── Server management ─────────────────────────────────────────────────────
  new SlashCommandBuilder()
    .setName('stop')
    .setDescription('Stoppt den Minecraft Server'),

  new SlashCommandBuilder()
    .setName('saveall')
    .setDescription('Speichert die Welt'),

  new SlashCommandBuilder()
    .setName('saveon')
    .setDescription('Aktiviert automatisches Speichern'),

  new SlashCommandBuilder()
    .setName('saveoff')
    .setDescription('Deaktiviert automatisches Speichern'),
].map(c => c.toJSON());
