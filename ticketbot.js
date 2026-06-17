require('dotenv').config();
const { Client, GatewayIntentBits, Events, REST, Routes, ChannelType, PermissionsBitField, EmbedBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle } = require('discord.js');

const TOKEN = process.env.TICKET_BOT_TOKEN;
const GUILD_ID = process.env.DISCORD_GUILD_ID || '1508068507340771408';
const TICKET_CATEGORY_NAME = 'Tickets';
const TICKET_CLOSED_CATEGORY_NAME = 'Tickets-Geschlossen';

const ROLE_SUPPORTER = process.env.ROLE_SUPPORTER || 'Supporter';
const ROLE_MODERATOR = process.env.ROLE_MODERATOR || 'Moderator';
const ROLE_ADMIN = process.env.ROLE_ADMIN || 'Admin';

const client = new Client({
  intents: [GatewayIntentBits.Guilds, GatewayIntentBits.GuildMessages, GatewayIntentBits.MessageContent, GatewayIntentBits.GuildMembers],
});

const commands = [
  { name: 'ticket', description: 'Öffne ein Support-Ticket' },
  { name: 'ticket-close', description: 'Ticket schließen' },
  { name: 'ticket-add', description: 'User zum Ticket hinzufügen', options: [{ name: 'user', type: 6, description: 'User', required: true }] },
  { name: 'ticket-addstaff', description: 'Einen Admin/Moderator zum Ticket hinzufügen (nur Admin/Mod)', options: [{ name: 'user', type: 6, description: 'User', required: true }] },
  { name: 'ticket-remove', description: 'User aus Ticket entfernen', options: [{ name: 'user', type: 6, description: 'User', required: true }] },
  { name: 'ticket-setup', description: 'Ticket-Panel in diesem Channel posten (Admin)' },
  {
    name: 'ban', description: 'Banne einen User (Mod/Admin)',
    options: [
      { name: 'user', type: 6, description: 'User', required: true },
      { name: 'grund', type: 3, description: 'Grund', required: false },
    ],
  },
  {
    name: 'kick', description: 'Kicke einen User (Mod/Admin)',
    options: [
      { name: 'user', type: 6, description: 'User', required: true },
      { name: 'grund', type: 3, description: 'Grund', required: false },
    ],
  },
  {
    name: 'timeout', description: 'Timeout einen User (Mod/Admin)',
    options: [
      { name: 'user', type: 6, description: 'User', required: true },
      { name: 'minuten', type: 4, description: 'Dauer in Minuten', required: true },
      { name: 'grund', type: 3, description: 'Grund', required: false },
    ],
  },
];

client.once(Events.ClientReady, async () => {
  console.log(`[TicketBot] Eingeloggt als ${client.user.tag}`);
  const rest = new REST({ version: '10' }).setToken(TOKEN);
  await rest.put(Routes.applicationGuildCommands(client.user.id, GUILD_ID), { body: commands });
  console.log('[TicketBot] Commands registriert.');
});

function hasRole(member, roleName) {
  return member.roles.cache.some(r => r.name === roleName);
}

function isSupporterPlus(member) {
  return hasRole(member, ROLE_SUPPORTER) || hasRole(member, ROLE_MODERATOR) || hasRole(member, ROLE_ADMIN) || member.permissions.has(PermissionsBitField.Flags.Administrator);
}

function isModPlus(member) {
  return hasRole(member, ROLE_MODERATOR) || hasRole(member, ROLE_ADMIN) || member.permissions.has(PermissionsBitField.Flags.Administrator);
}

client.on(Events.InteractionCreate, async (interaction) => {
  try {
    if (interaction.isButton()) {
      if (interaction.customId === 'open_ticket') {
        await openTicket(interaction);
      }
      return;
    }

    if (!interaction.isChatInputCommand()) return;

    switch (interaction.commandName) {
      case 'ticket': return await openTicket(interaction);
      case 'ticket-close': return await closeTicket(interaction);
      case 'ticket-add': return await addUser(interaction);
      case 'ticket-addstaff': return await addStaff(interaction);
      case 'ticket-remove': return await removeUser(interaction);
      case 'ticket-setup': return await setupPanel(interaction);
      case 'ban': return await banUser(interaction);
      case 'kick': return await kickUser(interaction);
      case 'timeout': return await timeoutUser(interaction);
    }
  } catch (e) {
    console.error('[TicketBot] Fehler:', e);
    if (!interaction.replied) interaction.reply({ content: '❌ Fehler: ' + e.message, ephemeral: true });
  }
});

async function openTicket(interaction) {
  const guild = interaction.guild;
  const member = interaction.member;
  const safeName = member.user.username.toLowerCase().replace(/[^a-z0-9]/g, '');

  const existing = guild.channels.cache.find(c => c.name === `ticket-${safeName}`);
  if (existing) {
    return interaction.reply({ content: `❌ Du hast bereits ein offenes Ticket: ${existing}`, ephemeral: true });
  }

  let category = guild.channels.cache.find(c => c.name === TICKET_CATEGORY_NAME && c.type === ChannelType.GuildCategory);
  if (!category) {
    category = await guild.channels.create({ name: TICKET_CATEGORY_NAME, type: ChannelType.GuildCategory });
  }

  const supporterRole = guild.roles.cache.find(r => r.name === ROLE_SUPPORTER);
  const modRole = guild.roles.cache.find(r => r.name === ROLE_MODERATOR);
  const adminRole = guild.roles.cache.find(r => r.name === ROLE_ADMIN);

  const overwrites = [
    { id: guild.id, deny: [PermissionsBitField.Flags.ViewChannel] },
    { id: member.id, allow: [PermissionsBitField.Flags.ViewChannel, PermissionsBitField.Flags.SendMessages] },
    { id: client.user.id, allow: [PermissionsBitField.Flags.ViewChannel, PermissionsBitField.Flags.SendMessages, PermissionsBitField.Flags.ManageChannels] },
  ];
  if (supporterRole) overwrites.push({ id: supporterRole.id, allow: [PermissionsBitField.Flags.ViewChannel, PermissionsBitField.Flags.SendMessages] });
  if (modRole) overwrites.push({ id: modRole.id, allow: [PermissionsBitField.Flags.ViewChannel, PermissionsBitField.Flags.SendMessages, PermissionsBitField.Flags.ManageChannels, PermissionsBitField.Flags.ManageMessages] });
  if (adminRole) overwrites.push({ id: adminRole.id, allow: [PermissionsBitField.Flags.ViewChannel, PermissionsBitField.Flags.SendMessages, PermissionsBitField.Flags.ManageChannels, PermissionsBitField.Flags.ManageMessages] });

  const channel = await guild.channels.create({
    name: `ticket-${safeName}`,
    type: ChannelType.GuildText,
    parent: category.id,
    permissionOverwrites: overwrites,
  });

  const embed = new EmbedBuilder()
    .setTitle('🎫 Support Ticket')
    .setDescription(`Hallo ${member}, beschreibe dein Problem und ein Supporter wird sich melden!\n\nMit \`/ticket-close\` kannst du das Ticket schließen.`)
    .setColor(0x5865f2)
    .setTimestamp();

  await channel.send({ embeds: [embed] });
  return interaction.reply({ content: `✅ Ticket erstellt: ${channel}`, ephemeral: true });
}

async function closeTicket(interaction) {
  const channel = interaction.channel;
  const guild = interaction.guild;
  if (!channel.name.startsWith('ticket-')) {
    return interaction.reply({ content: '❌ Das ist kein Ticket-Channel!', ephemeral: true });
  }
  if (!isSupporterPlus(interaction.member)) {
    return interaction.reply({ content: '❌ Du hast keine Berechtigung, Tickets zu schließen.', ephemeral: true });
  }

  let closedCategory = guild.channels.cache.find(c => c.name === TICKET_CLOSED_CATEGORY_NAME && c.type === ChannelType.GuildCategory);
  if (!closedCategory) {
    closedCategory = await guild.channels.create({ name: TICKET_CLOSED_CATEGORY_NAME, type: ChannelType.GuildCategory });
  }

  const modRole = guild.roles.cache.find(r => r.name === ROLE_MODERATOR);
  const adminRole = guild.roles.cache.find(r => r.name === ROLE_ADMIN);
  const supporterRole = guild.roles.cache.find(r => r.name === ROLE_SUPPORTER);

  await channel.permissionOverwrites.set([
    { id: guild.id, deny: [PermissionsBitField.Flags.ViewChannel] },
    { id: client.user.id, allow: [PermissionsBitField.Flags.ViewChannel, PermissionsBitField.Flags.SendMessages, PermissionsBitField.Flags.ManageChannels] },
    ...(modRole ? [{ id: modRole.id, allow: [PermissionsBitField.Flags.ViewChannel], deny: [PermissionsBitField.Flags.SendMessages] }] : []),
    ...(adminRole ? [{ id: adminRole.id, allow: [PermissionsBitField.Flags.ViewChannel], deny: [PermissionsBitField.Flags.SendMessages] }] : []),
    ...(supporterRole ? [{ id: supporterRole.id, deny: [PermissionsBitField.Flags.ViewChannel] }] : []),
  ]);

  await channel.setParent(closedCategory.id, { lockPermissions: false }).catch(() => {});
  if (!channel.name.startsWith('closed-')) {
    await channel.setName(`closed-${channel.name.replace(/^ticket-/, '')}`).catch(() => {});
  }

  const embed = new EmbedBuilder()
    .setTitle('🔒 Ticket geschlossen')
    .setDescription(`Geschlossen von ${interaction.user}.\nNur noch für Moderatoren und Admins sichtbar.`)
    .setColor(0xed4245)
    .setTimestamp();

  return interaction.reply({ embeds: [embed] });
}

async function addUser(interaction) {
  const channel = interaction.channel;
  if (!channel.name.startsWith('ticket-')) {
    return interaction.reply({ content: '❌ Das ist kein Ticket-Channel!', ephemeral: true });
  }
  if (!isSupporterPlus(interaction.member)) {
    return interaction.reply({ content: '❌ Du hast keine Berechtigung dazu.', ephemeral: true });
  }
  const user = interaction.options.getUser('user');
  await channel.permissionOverwrites.edit(user.id, { ViewChannel: true, SendMessages: true });
  return interaction.reply({ content: `✅ ${user} zum Ticket hinzugefügt.` });
}

async function addStaff(interaction) {
  const channel = interaction.channel;
  if (!channel.name.startsWith('ticket-') && !channel.name.startsWith('closed-')) {
    return interaction.reply({ content: '❌ Das ist kein Ticket-Channel!', ephemeral: true });
  }
  if (!isModPlus(interaction.member)) {
    return interaction.reply({ content: '❌ Nur Moderatoren/Admins können Staff hinzufügen.', ephemeral: true });
  }
  const user = interaction.options.getUser('user');
  await channel.permissionOverwrites.edit(user.id, { ViewChannel: true, SendMessages: true, ManageChannels: true, ManageMessages: true });
  return interaction.reply({ content: `✅ ${user} als Staff zum Ticket hinzugefügt.` });
}

async function removeUser(interaction) {
  const channel = interaction.channel;
  if (!channel.name.startsWith('ticket-')) {
    return interaction.reply({ content: '❌ Das ist kein Ticket-Channel!', ephemeral: true });
  }
  if (!isSupporterPlus(interaction.member)) {
    return interaction.reply({ content: '❌ Du hast keine Berechtigung dazu.', ephemeral: true });
  }
  const user = interaction.options.getUser('user');
  await channel.permissionOverwrites.edit(user.id, { ViewChannel: false });
  return interaction.reply({ content: `✅ ${user} aus Ticket entfernt.` });
}

async function setupPanel(interaction) {
  if (!isModPlus(interaction.member)) {
    return interaction.reply({ content: '❌ Nur Moderatoren/Admins können das Panel posten.', ephemeral: true });
  }
  const embed = new EmbedBuilder()
    .setTitle('🎫 Support Tickets')
    .setDescription('Hast du ein Problem oder eine Frage?\nKlicke auf den Button um ein Support-Ticket zu öffnen!\n\nEin Supporter wird sich so schnell wie möglich melden.')
    .setColor(0x5865f2)
    .setFooter({ text: 'Monsterjagd Support' });

  const row = new ActionRowBuilder().addComponents(
    new ButtonBuilder().setCustomId('open_ticket').setLabel('🎫 Ticket öffnen').setStyle(ButtonStyle.Primary)
  );

  await interaction.channel.send({ embeds: [embed], components: [row] });
  return interaction.reply({ content: '✅ Panel gepostet!', ephemeral: true });
}

async function banUser(interaction) {
  if (!isModPlus(interaction.member)) {
    return interaction.reply({ content: '❌ Du hast keine Berechtigung dazu.', ephemeral: true });
  }
  const user = interaction.options.getUser('user');
  const grund = interaction.options.getString('grund') || 'Kein Grund angegeben';
  const member = await interaction.guild.members.fetch(user.id).catch(() => null);
  if (!member) return interaction.reply({ content: '❌ User nicht gefunden.', ephemeral: true });
  if (!member.bannable) return interaction.reply({ content: '❌ Ich kann diesen User nicht bannen.', ephemeral: true });
  await member.ban({ reason: grund });
  return interaction.reply({ content: `🔨 ${user.tag} wurde gebannt. Grund: ${grund}` });
}

async function kickUser(interaction) {
  if (!isModPlus(interaction.member)) {
    return interaction.reply({ content: '❌ Du hast keine Berechtigung dazu.', ephemeral: true });
  }
  const user = interaction.options.getUser('user');
  const grund = interaction.options.getString('grund') || 'Kein Grund angegeben';
  const member = await interaction.guild.members.fetch(user.id).catch(() => null);
  if (!member) return interaction.reply({ content: '❌ User nicht gefunden.', ephemeral: true });
  if (!member.kickable) return interaction.reply({ content: '❌ Ich kann diesen User nicht kicken.', ephemeral: true });
  await member.kick(grund);
  return interaction.reply({ content: `👢 ${user.tag} wurde gekickt. Grund: ${grund}` });
}

async function timeoutUser(interaction) {
  if (!isModPlus(interaction.member)) {
    return interaction.reply({ content: '❌ Du hast keine Berechtigung dazu.', ephemeral: true });
  }
  const user = interaction.options.getUser('user');
  const minuten = interaction.options.getInteger('minuten');
  const grund = interaction.options.getString('grund') || 'Kein Grund angegeben';
  const member = await interaction.guild.members.fetch(user.id).catch(() => null);
  if (!member) return interaction.reply({ content: '❌ User nicht gefunden.', ephemeral: true });
  if (!member.moderatable) return interaction.reply({ content: '❌ Ich kann diesen User nicht timeouten.', ephemeral: true });
  await member.timeout(minuten * 60 * 1000, grund);
  return interaction.reply({ content: `⏱️ ${user.tag} wurde für ${minuten} Minuten getimeoutet. Grund: ${grund}` });
}

process.on('uncaughtException', e => console.error('[TicketBot]', e));
process.on('unhandledRejection', e => console.error('[TicketBot]', e));

client.login(TOKEN);
