require('dotenv').config();
const { Client, GatewayIntentBits, Events, REST, Routes, ChannelType, PermissionsBitField, EmbedBuilder } = require('discord.js');

const TOKEN = process.env.TICKET_BOT_TOKEN;
const GUILD_ID = process.env.DISCORD_GUILD_ID || '1508068507340771408';
const TICKET_CATEGORY_NAME = 'Tickets';
const SUPPORT_CHANNEL_ID = process.env.TICKET_CHANNEL_ID || '';
const ADMIN_ROLE_NAME = 'Moderator';

const client = new Client({
  intents: [GatewayIntentBits.Guilds, GatewayIntentBits.GuildMessages, GatewayIntentBits.MessageContent],
});

const commands = [
  { name: 'ticket', description: 'Öffne ein Support-Ticket' },
  { name: 'ticket-close', description: 'Ticket schließen' },
  { name: 'ticket-add', description: 'User zum Ticket hinzufügen', options: [{ name: 'user', type: 6, description: 'User', required: true }] },
  { name: 'ticket-remove', description: 'User aus Ticket entfernen', options: [{ name: 'user', type: 6, description: 'User', required: true }] },
  { name: 'ticket-setup', description: 'Ticket-Panel in diesem Channel posten (Admin)' },
];

client.once(Events.ClientReady, async () => {
  console.log(`[TicketBot] Eingeloggt als ${client.user.tag}`);
  const rest = new REST({ version: '10' }).setToken(TOKEN);
  await rest.put(Routes.applicationGuildCommands(client.user.id, GUILD_ID), { body: commands });
  console.log('[TicketBot] Commands registriert.');
});

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
      case 'ticket-remove': return await removeUser(interaction);
      case 'ticket-setup': return await setupPanel(interaction);
    }
  } catch (e) {
    console.error('[TicketBot] Fehler:', e);
    if (!interaction.replied) interaction.reply({ content: '❌ Fehler: ' + e.message, ephemeral: true });
  }
});

async function openTicket(interaction) {
  const guild = interaction.guild;
  const member = interaction.member;

  // Check if user already has a ticket
  const existing = guild.channels.cache.find(c => c.name === `ticket-${member.user.username.toLowerCase().replace(/[^a-z0-9]/g, '')}`);
  if (existing) {
    return interaction.reply({ content: `❌ Du hast bereits ein offenes Ticket: ${existing}`, ephemeral: true });
  }

  // Find or create category
  let category = guild.channels.cache.find(c => c.name === TICKET_CATEGORY_NAME && c.type === ChannelType.GuildCategory);
  if (!category) {
    category = await guild.channels.create({ name: TICKET_CATEGORY_NAME, type: ChannelType.GuildCategory });
  }

  const modRole = guild.roles.cache.find(r => r.name === ADMIN_ROLE_NAME);

  const channel = await guild.channels.create({
    name: `ticket-${member.user.username.toLowerCase().replace(/[^a-z0-9]/g, '')}`,
    type: ChannelType.GuildText,
    parent: category.id,
    permissionOverwrites: [
      { id: guild.id, deny: [PermissionsBitField.Flags.ViewChannel] },
      { id: member.id, allow: [PermissionsBitField.Flags.ViewChannel, PermissionsBitField.Flags.SendMessages] },
      ...(modRole ? [{ id: modRole.id, allow: [PermissionsBitField.Flags.ViewChannel, PermissionsBitField.Flags.SendMessages] }] : []),
      { id: client.user.id, allow: [PermissionsBitField.Flags.ViewChannel, PermissionsBitField.Flags.SendMessages] },
    ],
  });

  const embed = new EmbedBuilder()
    .setTitle('🎫 Support Ticket')
    .setDescription(`Hallo ${member}, beschreibe dein Problem und ein Moderator wird sich melden!\n\nMit \`/ticket-close\` kannst du das Ticket schließen.`)
    .setColor(0x5865f2)
    .setTimestamp();

  await channel.send({ embeds: [embed] });
  return interaction.reply({ content: `✅ Ticket erstellt: ${channel}`, ephemeral: true });
}

async function closeTicket(interaction) {
  const channel = interaction.channel;
  if (!channel.name.startsWith('ticket-')) {
    return interaction.reply({ content: '❌ Das ist kein Ticket-Channel!', ephemeral: true });
  }
  await interaction.reply('🔒 Ticket wird geschlossen...');
  setTimeout(() => channel.delete().catch(() => {}), 3000);
}

async function addUser(interaction) {
  const channel = interaction.channel;
  if (!channel.name.startsWith('ticket-')) {
    return interaction.reply({ content: '❌ Das ist kein Ticket-Channel!', ephemeral: true });
  }
  const user = interaction.options.getUser('user');
  await channel.permissionOverwrites.edit(user.id, { ViewChannel: true, SendMessages: true });
  return interaction.reply({ content: `✅ ${user} zum Ticket hinzugefügt.` });
}

async function removeUser(interaction) {
  const channel = interaction.channel;
  if (!channel.name.startsWith('ticket-')) {
    return interaction.reply({ content: '❌ Das ist kein Ticket-Channel!', ephemeral: true });
  }
  const user = interaction.options.getUser('user');
  await channel.permissionOverwrites.edit(user.id, { ViewChannel: false });
  return interaction.reply({ content: `✅ ${user} aus Ticket entfernt.` });
}

async function setupPanel(interaction) {
  const embed = new EmbedBuilder()
    .setTitle('🎫 Support Tickets')
    .setDescription('Hast du ein Problem oder eine Frage?\nNutze `/ticket` um ein Support-Ticket zu öffnen!\n\nEin Moderator wird sich so schnell wie möglich melden.')
    .setColor(0x5865f2)
    .setFooter({ text: 'Monsterjagd Support' });

  await interaction.channel.send({ embeds: [embed] });
  return interaction.reply({ content: '✅ Panel gepostet!', ephemeral: true });
}

process.on('uncaughtException', e => console.error('[TicketBot]', e));
process.on('unhandledRejection', e => console.error('[TicketBot]', e));

client.login(TOKEN);
