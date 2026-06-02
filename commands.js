const { SlashCommandBuilder } = require('discord.js');

const GAMEMODES = ['survival', 'creative', 'adventure', 'spectator'];
const DIFFICULTIES = ['peaceful', 'easy', 'normal', 'hard'];
const WEATHER_TYPES = ['clear', 'rain', 'thunder'];
const RANKS = ['Neuling', 'Spieler', 'Veteran', 'Elite', 'Legende', 'GOTT', 'VIP', 'VIP+', 'MVP', 'MVP+', 'ELITE', 'GOD'];

module.exports = [
  new SlashCommandBuilder()
    .setName('mc')
    .setDescription('Sende einen beliebigen Minecraft-Befehl')
    .addStringOption(o => o.setName('command').setDescription('Befehl (ohne /)').setRequired(true)),

  new SlashCommandBuilder()
    .setName('mcsay')
    .setDescription('Broadcast eine Nachricht im Minecraft Chat')
    .addStringOption(o => o.setName('message').setDescription('Nachricht').setRequired(true)),

  new SlashCommandBuilder()
    .setName('mcstatus')
    .setDescription('Zeigt den Bot-Status an'),

  new SlashCommandBuilder()
    .setName('mclist')
    .setDescription('Zeigt alle Online-Spieler'),

  new SlashCommandBuilder()
    .setName('mclogs')
    .setDescription('Zeigt die letzten Bot-Logs')
    .addIntegerOption(o => o.setName('lines').setDescription('Anzahl Zeilen').setMinValue(1).setMaxValue(50)),

  new SlashCommandBuilder()
    .setName('mcrestart')
    .setDescription('Startet den Minecraft Bot neu'),

  new SlashCommandBuilder()
    .setName('mcop')
    .setDescription('Gibt einem Spieler Operator-Rechte')
    .addStringOption(o => o.setName('player').setDescription('Spieler').setRequired(true)),

  new SlashCommandBuilder()
    .setName('mcdeop')
    .setDescription('Entfernt Operator-Rechte')
    .addStringOption(o => o.setName('player').setDescription('Spieler').setRequired(true)),

  new SlashCommandBuilder()
    .setName('mckick')
    .setDescription('Kickt einen Spieler')
    .addStringOption(o => o.setName('player').setDescription('Spieler').setRequired(true))
    .addStringOption(o => o.setName('reason').setDescription('Grund')),

  new SlashCommandBuilder()
    .setName('mcban')
    .setDescription('Bannt einen Spieler')
    .addStringOption(o => o.setName('player').setDescription('Spieler').setRequired(true))
    .addStringOption(o => o.setName('reason').setDescription('Grund')),

  new SlashCommandBuilder()
    .setName('mcbanip')
    .setDescription('Bannt eine IP-Adresse')
    .addStringOption(o => o.setName('ip').setDescription('IP-Adresse').setRequired(true)),

  new SlashCommandBuilder()
    .setName('mcpardon')
    .setDescription('Entbannt einen Spieler')
    .addStringOption(o => o.setName('player').setDescription('Spieler').setRequired(true)),

  new SlashCommandBuilder()
    .setName('mcpardonip')
    .setDescription('Entbannt eine IP-Adresse')
    .addStringOption(o => o.setName('ip').setDescription('IP-Adresse').setRequired(true)),

  new SlashCommandBuilder()
    .setName('mcwhitelist')
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

  new SlashCommandBuilder()
    .setName('mcgamemode')
    .setDescription('Ändert den Spielmodus')
    .addStringOption(o =>
      o.setName('mode').setDescription('Modus').setRequired(true)
        .addChoices(...GAMEMODES.map(m => ({ name: m, value: m }))))
    .addStringOption(o => o.setName('player').setDescription('Spieler')),

  new SlashCommandBuilder()
    .setName('mcdifficulty')
    .setDescription('Ändert den Schwierigkeitsgrad')
    .addStringOption(o =>
      o.setName('level').setDescription('Schwierigkeit').setRequired(true)
        .addChoices(...DIFFICULTIES.map(d => ({ name: d, value: d })))),

  new SlashCommandBuilder()
    .setName('mctime')
    .setDescription('Ändert die Weltzeit')
    .addStringOption(o =>
      o.setName('action').setDescription('set oder add').setRequired(true)
        .addChoices({ name: 'set', value: 'set' }, { name: 'add', value: 'add' }))
    .addStringOption(o => o.setName('value').setDescription('Wert (z.B. day, night, 1000)').setRequired(true)),

  new SlashCommandBuilder()
    .setName('mcweather')
    .setDescription('Ändert das Wetter')
    .addStringOption(o =>
      o.setName('type').setDescription('Wettertyp').setRequired(true)
        .addChoices(...WEATHER_TYPES.map(w => ({ name: w, value: w }))))
    .addIntegerOption(o => o.setName('duration').setDescription('Dauer in Sekunden')),

  new SlashCommandBuilder()
    .setName('mcgamerule')
    .setDescription('Ändert eine Gamerule')
    .addStringOption(o => o.setName('rule').setDescription('Regel (z.B. keepInventory)').setRequired(true))
    .addStringOption(o => o.setName('value').setDescription('Wert (true/false)')),

  new SlashCommandBuilder()
    .setName('mctp')
    .setDescription('Teleportiert einen Spieler')
    .addStringOption(o => o.setName('target').setDescription('Spieler').setRequired(true))
    .addStringOption(o => o.setName('destination').setDescription('Ziel (Spieler oder x y z)').setRequired(true)),

  new SlashCommandBuilder()
    .setName('mctphere')
    .setDescription('Teleportiert einen Spieler zum Bot')
    .addStringOption(o => o.setName('player').setDescription('Spieler').setRequired(true)),

  new SlashCommandBuilder()
    .setName('mckill')
    .setDescription('Tötet einen Spieler')
    .addStringOption(o => o.setName('player').setDescription('Spieler (leer = alle)')),

  new SlashCommandBuilder()
    .setName('mcgive')
    .setDescription('Gibt einem Spieler einen Gegenstand')
    .addStringOption(o => o.setName('player').setDescription('Spieler').setRequired(true))
    .addStringOption(o => o.setName('item').setDescription('Item (z.B. minecraft:diamond)').setRequired(true))
    .addIntegerOption(o => o.setName('amount').setDescription('Menge').setMinValue(1).setMaxValue(64)),

  new SlashCommandBuilder()
    .setName('mceffect')
    .setDescription('Gibt einem Spieler einen Effekt')
    .addStringOption(o => o.setName('player').setDescription('Spieler').setRequired(true))
    .addStringOption(o => o.setName('effect').setDescription('Effekt (z.B. speed)').setRequired(true))
    .addIntegerOption(o => o.setName('duration').setDescription('Dauer in Sekunden').setMinValue(1))
    .addIntegerOption(o => o.setName('amplifier').setDescription('Stärke (0-255)').setMinValue(0).setMaxValue(255)),

  new SlashCommandBuilder()
    .setName('mcenchant')
    .setDescription('Verzaubert Item in der Hand eines Spielers')
    .addStringOption(o => o.setName('player').setDescription('Spieler').setRequired(true))
    .addStringOption(o => o.setName('enchantment').setDescription('Verzauberung').setRequired(true))
    .addIntegerOption(o => o.setName('level').setDescription('Level').setMinValue(1)),

  new SlashCommandBuilder()
    .setName('mcxp')
    .setDescription('Gibt einem Spieler XP')
    .addStringOption(o => o.setName('player').setDescription('Spieler').setRequired(true))
    .addIntegerOption(o => o.setName('amount').setDescription('Menge').setRequired(true)),

  new SlashCommandBuilder()
    .setName('mcclear')
    .setDescription('Leert das Inventar eines Spielers')
    .addStringOption(o => o.setName('player').setDescription('Spieler'))
    .addStringOption(o => o.setName('item').setDescription('Item (optional)')),

  new SlashCommandBuilder()
    .setName('mcstop')
    .setDescription('Stoppt den Minecraft Server'),

  new SlashCommandBuilder()
    .setName('mcsaveall')
    .setDescription('Speichert die Welt'),

  new SlashCommandBuilder()
    .setName('mcrang')
    .setDescription('Gibt einem Spieler einen Rang')
    .addStringOption(o => o.setName('player').setDescription('Spieler').setRequired(true))
    .addStringOption(o =>
      o.setName('rang').setDescription('Rang').setRequired(true)
        .addChoices(...RANKS.map(r => ({ name: r, value: r.toLowerCase() })))),
].map(c => c.toJSON());
