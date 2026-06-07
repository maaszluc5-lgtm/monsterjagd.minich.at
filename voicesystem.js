const { PermissionsBitField, ChannelType } = require('discord.js');

const VOICE_COMMAND_CHANNEL = '1513174326168260698';
const CREATE_CHANNEL_NAME = 'sprachkanäle-erstellen';

// Map: voiceChannelId -> { ownerId, coOwners: Set, mods: Set, banned: Set, trusted: Set }
const voiceChannels = new Map();

function isVoiceChannel(channelId) {
    return voiceChannels.has(channelId);
}

function getVoiceData(channelId) {
    return voiceChannels.get(channelId);
}

function isOwnerOrMod(data, userId) {
    return data.ownerId === userId || data.coOwners.has(userId) || data.mods.has(userId);
}

async function handleVoiceCreate(member, channel) {
    if (channel.name !== CREATE_CHANNEL_NAME) return;
    const guild = member.guild;
    const category = channel.parent;

    try {
        const newChannel = await guild.channels.create({
            name: `🎮 ${member.displayName}`,
            type: ChannelType.GuildVoice,
            parent: category,
            userLimit: 0,
            permissionOverwrites: [
                { id: guild.id, allow: [PermissionsBitField.Flags.Connect] },
                { id: member.id, allow: [PermissionsBitField.Flags.Connect, PermissionsBitField.Flags.MoveMembers, PermissionsBitField.Flags.MuteMembers, PermissionsBitField.Flags.DeafenMembers] }
            ]
        });

        voiceChannels.set(newChannel.id, {
            ownerId: member.id,
            coOwners: new Set(),
            mods: new Set(),
            banned: new Set(),
            trusted: new Set(),
            hidden: false,
            locked: false
        });

        await member.voice.setChannel(newChannel);
    } catch (e) {
        console.error('Voice create error:', e.message);
    }
}

async function handleVoiceLeave(member, channel) {
    if (!voiceChannels.has(channel.id)) return;
    if (channel.members.size === 0) {
        voiceChannels.delete(channel.id);
        try { await channel.delete(); } catch (_) {}
    }
}

async function handleVoiceCommand(interaction) {
    if (interaction.channelId !== VOICE_COMMAND_CHANNEL) {
        return interaction.reply({ content: `❌ Benutze diesen Befehl nur in <#${VOICE_COMMAND_CHANNEL}>!`, ephemeral: true });
    }

    const member = interaction.member;
    const voiceState = member.voice;
    const voiceChannel = voiceState?.channel;

    const cmd = interaction.commandName;
    const target = interaction.options.getMember?.('user') || interaction.options.getMember?.('member');
    const value = interaction.options.getString?.('name') || interaction.options.getInteger?.('limit') || interaction.options.getInteger?.('bitrate') || interaction.options.getString?.('region');

    // Commands that don't require being in a voice channel
    if (cmd === 'v-info') {
        if (!voiceChannel || !voiceChannels.has(voiceChannel.id)) {
            return interaction.reply({ content: '❌ Du bist in keinem verwalteten Voice-Channel!', ephemeral: true });
        }
        const data = voiceChannels.get(voiceChannel.id);
        const owner = await interaction.guild.members.fetch(data.ownerId).catch(() => null);
        return interaction.reply({
            ephemeral: true,
            embeds: [{
                title: `🎙️ ${voiceChannel.name}`,
                color: 0x5865F2,
                fields: [
                    { name: 'Owner', value: owner ? `<@${owner.id}>` : 'Unbekannt', inline: true },
                    { name: 'Mitglieder', value: `${voiceChannel.members.size}`, inline: true },
                    { name: 'Limit', value: voiceChannel.userLimit > 0 ? `${voiceChannel.userLimit}` : 'Kein Limit', inline: true },
                    { name: 'Status', value: `${data.locked ? '🔒 Gesperrt' : '🔓 Offen'} | ${data.hidden ? '👁️ Versteckt' : '👁️ Sichtbar'}`, inline: false },
                    { name: 'Co-Owner', value: data.coOwners.size > 0 ? [...data.coOwners].map(id => `<@${id}>`).join(', ') : 'Keine', inline: false },
                    { name: 'Mods', value: data.mods.size > 0 ? [...data.mods].map(id => `<@${id}>`).join(', ') : 'Keine', inline: false },
                    { name: 'Gebannt', value: data.banned.size > 0 ? [...data.banned].map(id => `<@${id}>`).join(', ') : 'Keine', inline: false },
                ]
            }]
        });
    }

    // All other commands require being in a managed channel
    if (!voiceChannel || !voiceChannels.has(voiceChannel.id)) {
        return interaction.reply({ content: '❌ Du bist in keinem verwalteten Voice-Channel!', ephemeral: true });
    }

    const data = voiceChannels.get(voiceChannel.id);
    const isOwner = data.ownerId === member.id;
    const isModOrOwner = isOwnerOrMod(data, member.id);

    switch (cmd) {
        case 'v-ban': {
            if (!isModOrOwner) return interaction.reply({ content: '❌ Keine Berechtigung!', ephemeral: true });
            if (!target) return interaction.reply({ content: '❌ Kein User angegeben!', ephemeral: true });
            if (target.id === data.ownerId) return interaction.reply({ content: '❌ Du kannst den Owner nicht bannen!', ephemeral: true });
            data.banned.add(target.id);
            await voiceChannel.permissionOverwrites.edit(target.id, { Connect: false });
            if (target.voice?.channelId === voiceChannel.id) await target.voice.disconnect();
            return interaction.reply({ content: `🔨 <@${target.id}> wurde aus dem Channel gebannt!` });
        }
        case 'v-unban': {
            if (!isModOrOwner) return interaction.reply({ content: '❌ Keine Berechtigung!', ephemeral: true });
            if (!target) return interaction.reply({ content: '❌ Kein User angegeben!', ephemeral: true });
            data.banned.delete(target.id);
            await voiceChannel.permissionOverwrites.delete(target.id).catch(() => {});
            return interaction.reply({ content: `✅ <@${target.id}> wurde entbannt!` });
        }
        case 'v-kick': {
            if (!isModOrOwner) return interaction.reply({ content: '❌ Keine Berechtigung!', ephemeral: true });
            if (!target) return interaction.reply({ content: '❌ Kein User angegeben!', ephemeral: true });
            if (target.id === data.ownerId) return interaction.reply({ content: '❌ Du kannst den Owner nicht kicken!', ephemeral: true });
            if (target.voice?.channelId === voiceChannel.id) await target.voice.disconnect();
            return interaction.reply({ content: `👟 <@${target.id}> wurde gekickt!` });
        }
        case 'v-limit': {
            if (!isModOrOwner) return interaction.reply({ content: '❌ Keine Berechtigung!', ephemeral: true });
            const limit = interaction.options.getInteger('limit');
            await voiceChannel.setUserLimit(limit);
            return interaction.reply({ content: `✅ Limit auf **${limit === 0 ? 'unbegrenzt' : limit}** gesetzt!` });
        }
        case 'v-name': {
            if (!isModOrOwner) return interaction.reply({ content: '❌ Keine Berechtigung!', ephemeral: true });
            const name = interaction.options.getString('name');
            await voiceChannel.setName(name);
            return interaction.reply({ content: `✅ Channel umbenannt zu **${name}**!` });
        }
        case 'v-lock': {
            if (!isModOrOwner) return interaction.reply({ content: '❌ Keine Berechtigung!', ephemeral: true });
            data.locked = true;
            await voiceChannel.permissionOverwrites.edit(interaction.guild.id, { Connect: false });
            return interaction.reply({ content: '🔒 Channel gesperrt! Niemand kann mehr joinen.' });
        }
        case 'v-unlock': {
            if (!isModOrOwner) return interaction.reply({ content: '❌ Keine Berechtigung!', ephemeral: true });
            data.locked = false;
            await voiceChannel.permissionOverwrites.edit(interaction.guild.id, { Connect: true });
            return interaction.reply({ content: '🔓 Channel entsperrt!' });
        }
        case 'v-hide': {
            if (!isModOrOwner) return interaction.reply({ content: '❌ Keine Berechtigung!', ephemeral: true });
            data.hidden = true;
            await voiceChannel.permissionOverwrites.edit(interaction.guild.id, { ViewChannel: false });
            return interaction.reply({ content: '👁️ Channel versteckt!' });
        }
        case 'v-show': {
            if (!isModOrOwner) return interaction.reply({ content: '❌ Keine Berechtigung!', ephemeral: true });
            data.hidden = false;
            await voiceChannel.permissionOverwrites.edit(interaction.guild.id, { ViewChannel: true });
            return interaction.reply({ content: '👁️ Channel sichtbar!' });
        }
        case 'v-owner': {
            if (!isOwner) return interaction.reply({ content: '❌ Nur der Owner kann das!', ephemeral: true });
            if (!target) return interaction.reply({ content: '❌ Kein User angegeben!', ephemeral: true });
            data.ownerId = target.id;
            data.coOwners.delete(target.id);
            await voiceChannel.permissionOverwrites.edit(target.id, { Connect: true, MoveMembers: true, MuteMembers: true, DeafenMembers: true });
            return interaction.reply({ content: `👑 <@${target.id}> ist jetzt der neue Owner!` });
        }
        case 'v-co-owner': {
            if (!isOwner) return interaction.reply({ content: '❌ Nur der Owner kann das!', ephemeral: true });
            if (!target) return interaction.reply({ content: '❌ Kein User angegeben!', ephemeral: true });
            data.coOwners.add(target.id);
            await voiceChannel.permissionOverwrites.edit(target.id, { Connect: true, MoveMembers: true, MuteMembers: true });
            return interaction.reply({ content: `⭐ <@${target.id}> ist jetzt Co-Owner!` });
        }
        case 'v-mod': {
            if (!isOwner) return interaction.reply({ content: '❌ Nur der Owner kann das!', ephemeral: true });
            if (!target) return interaction.reply({ content: '❌ Kein User angegeben!', ephemeral: true });
            data.mods.add(target.id);
            await voiceChannel.permissionOverwrites.edit(target.id, { Connect: true, MuteMembers: true });
            return interaction.reply({ content: `🛡️ <@${target.id}> ist jetzt Mod im Channel!` });
        }
        case 'v-unmod': {
            if (!isOwner) return interaction.reply({ content: '❌ Nur der Owner kann das!', ephemeral: true });
            if (!target) return interaction.reply({ content: '❌ Kein User angegeben!', ephemeral: true });
            data.mods.delete(target.id);
            data.coOwners.delete(target.id);
            await voiceChannel.permissionOverwrites.delete(target.id).catch(() => {});
            return interaction.reply({ content: `✅ <@${target.id}> Rechte entfernt!` });
        }
        case 'v-trust': {
            if (!isModOrOwner) return interaction.reply({ content: '❌ Keine Berechtigung!', ephemeral: true });
            if (!target) return interaction.reply({ content: '❌ Kein User angegeben!', ephemeral: true });
            data.trusted.add(target.id);
            await voiceChannel.permissionOverwrites.edit(target.id, { Connect: true });
            return interaction.reply({ content: `✅ <@${target.id}> darf jetzt joinen!` });
        }
        case 'v-untrust': {
            if (!isModOrOwner) return interaction.reply({ content: '❌ Keine Berechtigung!', ephemeral: true });
            if (!target) return interaction.reply({ content: '❌ Kein User angegeben!', ephemeral: true });
            data.trusted.delete(target.id);
            await voiceChannel.permissionOverwrites.delete(target.id).catch(() => {});
            return interaction.reply({ content: `✅ <@${target.id}> Vertrauen entfernt!` });
        }
        case 'v-mute': {
            if (!isModOrOwner) return interaction.reply({ content: '❌ Keine Berechtigung!', ephemeral: true });
            if (!target) return interaction.reply({ content: '❌ Kein User angegeben!', ephemeral: true });
            await target.voice.setMute(true).catch(() => {});
            return interaction.reply({ content: `🔇 <@${target.id}> wurde gemutet!` });
        }
        case 'v-unmute': {
            if (!isModOrOwner) return interaction.reply({ content: '❌ Keine Berechtigung!', ephemeral: true });
            if (!target) return interaction.reply({ content: '❌ Kein User angegeben!', ephemeral: true });
            await target.voice.setMute(false).catch(() => {});
            return interaction.reply({ content: `🔊 <@${target.id}> wurde entmutet!` });
        }
        case 'v-deafen': {
            if (!isModOrOwner) return interaction.reply({ content: '❌ Keine Berechtigung!', ephemeral: true });
            if (!target) return interaction.reply({ content: '❌ Kein User angegeben!', ephemeral: true });
            await target.voice.setDeaf(true).catch(() => {});
            return interaction.reply({ content: `🔕 <@${target.id}> wurde gedeafened!` });
        }
        case 'v-undeafen': {
            if (!isModOrOwner) return interaction.reply({ content: '❌ Keine Berechtigung!', ephemeral: true });
            if (!target) return interaction.reply({ content: '❌ Kein User angegeben!', ephemeral: true });
            await target.voice.setDeaf(false).catch(() => {});
            return interaction.reply({ content: `🔔 <@${target.id}> wurde undeafened!` });
        }
        case 'v-bitrate': {
            if (!isModOrOwner) return interaction.reply({ content: '❌ Keine Berechtigung!', ephemeral: true });
            const bitrate = interaction.options.getInteger('bitrate') * 1000;
            await voiceChannel.setBitrate(bitrate).catch(() => {});
            return interaction.reply({ content: `✅ Bitrate auf **${bitrate/1000}kbps** gesetzt!` });
        }
        case 'v-region': {
            if (!isModOrOwner) return interaction.reply({ content: '❌ Keine Berechtigung!', ephemeral: true });
            const region = interaction.options.getString('region');
            await voiceChannel.setRTCRegion(region === 'auto' ? null : region).catch(() => {});
            return interaction.reply({ content: `✅ Region auf **${region}** gesetzt!` });
        }
        case 'v-private': {
            if (!isModOrOwner) return interaction.reply({ content: '❌ Keine Berechtigung!', ephemeral: true });
            data.locked = true;
            data.hidden = true;
            await voiceChannel.permissionOverwrites.edit(interaction.guild.id, { Connect: false, ViewChannel: false });
            return interaction.reply({ content: '🔐 Channel ist jetzt komplett privat!' });
        }
        case 'v-public': {
            if (!isModOrOwner) return interaction.reply({ content: '❌ Keine Berechtigung!', ephemeral: true });
            data.locked = false;
            data.hidden = false;
            await voiceChannel.permissionOverwrites.edit(interaction.guild.id, { Connect: true, ViewChannel: true });
            return interaction.reply({ content: '🌐 Channel ist jetzt öffentlich!' });
        }
        case 'v-invite': {
            if (!isModOrOwner) return interaction.reply({ content: '❌ Keine Berechtigung!', ephemeral: true });
            if (!target) return interaction.reply({ content: '❌ Kein User angegeben!', ephemeral: true });
            data.trusted.add(target.id);
            await voiceChannel.permissionOverwrites.edit(target.id, { Connect: true, ViewChannel: true });
            try {
                await target.send(`📨 Du wurdest in den Voice-Channel **${voiceChannel.name}** eingeladen!`);
            } catch (_) {}
            return interaction.reply({ content: `📨 <@${target.id}> eingeladen!` });
        }
        case 'v-move': {
            if (!isModOrOwner) return interaction.reply({ content: '❌ Keine Berechtigung!', ephemeral: true });
            if (!target) return interaction.reply({ content: '❌ Kein User angegeben!', ephemeral: true });
            await target.voice.setChannel(voiceChannel).catch(() => {});
            return interaction.reply({ content: `➡️ <@${target.id}> in den Channel verschoben!` });
        }
        case 'v-claim': {
            if (voiceChannel.members.has(data.ownerId)) {
                return interaction.reply({ content: '❌ Der Owner ist noch im Channel!', ephemeral: true });
            }
            data.ownerId = member.id;
            await voiceChannel.permissionOverwrites.edit(member.id, { Connect: true, MoveMembers: true, MuteMembers: true, DeafenMembers: true });
            return interaction.reply({ content: `👑 Du hast den Channel geclaimt!` });
        }
        case 'v-members': {
            const members = [...voiceChannel.members.values()].map(m => m.displayName).join(', ');
            return interaction.reply({ content: `👥 **Mitglieder:** ${members || 'Niemand'}`, ephemeral: true });
        }
        case 'v-permit': {
            if (!isModOrOwner) return interaction.reply({ content: '❌ Keine Berechtigung!', ephemeral: true });
            if (!target) return interaction.reply({ content: '❌ Kein User angegeben!', ephemeral: true });
            await voiceChannel.permissionOverwrites.edit(target.id, { Connect: true });
            return interaction.reply({ content: `✅ <@${target.id}> darf joinen!` });
        }
        case 'v-reject': {
            if (!isModOrOwner) return interaction.reply({ content: '❌ Keine Berechtigung!', ephemeral: true });
            if (!target) return interaction.reply({ content: '❌ Kein User angegeben!', ephemeral: true });
            await voiceChannel.permissionOverwrites.edit(target.id, { Connect: false });
            if (target.voice?.channelId === voiceChannel.id) await target.voice.disconnect();
            return interaction.reply({ content: `🚫 <@${target.id}> abgelehnt!` });
        }
        case 'v-reset': {
            if (!isOwner) return interaction.reply({ content: '❌ Nur der Owner kann das!', ephemeral: true });
            data.mods.clear();
            data.coOwners.clear();
            data.banned.clear();
            data.trusted.clear();
            data.locked = false;
            data.hidden = false;
            await voiceChannel.permissionOverwrites.set([
                { id: interaction.guild.id, allow: [PermissionsBitField.Flags.Connect, PermissionsBitField.Flags.ViewChannel] },
                { id: member.id, allow: [PermissionsBitField.Flags.Connect, PermissionsBitField.Flags.MoveMembers, PermissionsBitField.Flags.MuteMembers] }
            ]);
            await voiceChannel.setUserLimit(0);
            return interaction.reply({ content: '🔄 Channel zurückgesetzt!' });
        }
        case 'v-transfer': {
            if (!isOwner) return interaction.reply({ content: '❌ Nur der Owner kann das!', ephemeral: true });
            if (!target) return interaction.reply({ content: '❌ Kein User angegeben!', ephemeral: true });
            data.ownerId = target.id;
            await voiceChannel.permissionOverwrites.edit(target.id, { Connect: true, MoveMembers: true, MuteMembers: true, DeafenMembers: true });
            await voiceChannel.permissionOverwrites.edit(member.id, { Connect: true });
            return interaction.reply({ content: `👑 Channel an <@${target.id}> übertragen!` });
        }
        case 'v-delete': {
            if (!isOwner) return interaction.reply({ content: '❌ Nur der Owner kann das!', ephemeral: true });
            voiceChannels.delete(voiceChannel.id);
            await interaction.reply({ content: '🗑️ Channel wird gelöscht...' });
            await voiceChannel.delete().catch(() => {});
            return;
        }
        case 'v-status': {
            const locked = data.locked ? '🔒 Gesperrt' : '🔓 Offen';
            const hidden = data.hidden ? '👁️ Versteckt' : '👁️ Sichtbar';
            return interaction.reply({ content: `📊 Status: ${locked} | ${hidden} | 👥 ${voiceChannel.members.size} Mitglieder | Limit: ${voiceChannel.userLimit || '∞'}`, ephemeral: true });
        }
        case 'v-banlist': {
            if (!isModOrOwner) return interaction.reply({ content: '❌ Keine Berechtigung!', ephemeral: true });
            const banned = data.banned.size > 0 ? [...data.banned].map(id => `<@${id}>`).join(', ') : 'Niemand';
            return interaction.reply({ content: `🔨 **Gebannte:** ${banned}`, ephemeral: true });
        }
        case 'v-modlist': {
            const mods = data.mods.size > 0 ? [...data.mods].map(id => `<@${id}>`).join(', ') : 'Keine';
            const cos = data.coOwners.size > 0 ? [...data.coOwners].map(id => `<@${id}>`).join(', ') : 'Keine';
            return interaction.reply({ content: `🛡️ **Mods:** ${mods}\n⭐ **Co-Owner:** ${cos}`, ephemeral: true });
        }
        case 'v-help': {
            return interaction.reply({
                ephemeral: true,
                embeds: [{
                    title: '🎙️ Voice Channel Befehle',
                    color: 0x5865F2,
                    description: [
                        '**👑 Owner:**',
                        '`/v-name` `/v-owner` `/v-co-owner` `/v-mod` `/v-unmod` `/v-reset` `/v-transfer` `/v-delete`',
                        '',
                        '**🛡️ Mod/Owner:**',
                        '`/v-ban` `/v-unban` `/v-kick` `/v-limit` `/v-lock` `/v-unlock` `/v-hide` `/v-show`',
                        '`/v-trust` `/v-untrust` `/v-mute` `/v-unmute` `/v-deafen` `/v-undeafen`',
                        '`/v-bitrate` `/v-region` `/v-private` `/v-public` `/v-invite` `/v-move`',
                        '`/v-permit` `/v-reject` `/v-banlist` `/v-modlist`',
                        '',
                        '**👥 Alle:**',
                        '`/v-info` `/v-members` `/v-status` `/v-claim` `/v-help`'
                    ].join('\n')
                }]
            });
        }
        default:
            return interaction.reply({ content: '❌ Unbekannter Voice-Befehl.', ephemeral: true });
    }
}

const voiceCommands = [
    { name: 'v-ban', description: 'User aus dem Voice-Channel bannen', options: [{ name: 'user', type: 6, description: 'User', required: true }] },
    { name: 'v-unban', description: 'User entbannen', options: [{ name: 'user', type: 6, description: 'User', required: true }] },
    { name: 'v-kick', description: 'User aus dem Channel kicken', options: [{ name: 'user', type: 6, description: 'User', required: true }] },
    { name: 'v-limit', description: 'Mitglieder-Limit setzen', options: [{ name: 'limit', type: 4, description: 'Limit (0 = unbegrenzt)', required: true }] },
    { name: 'v-name', description: 'Channel umbenennen', options: [{ name: 'name', type: 3, description: 'Neuer Name', required: true }] },
    { name: 'v-lock', description: 'Channel sperren' },
    { name: 'v-unlock', description: 'Channel entsperren' },
    { name: 'v-hide', description: 'Channel verstecken' },
    { name: 'v-show', description: 'Channel sichtbar machen' },
    { name: 'v-owner', description: 'Owner übertragen', options: [{ name: 'user', type: 6, description: 'Neuer Owner', required: true }] },
    { name: 'v-co-owner', description: 'Co-Owner ernennen', options: [{ name: 'user', type: 6, description: 'User', required: true }] },
    { name: 'v-mod', description: 'Mod ernennen', options: [{ name: 'user', type: 6, description: 'User', required: true }] },
    { name: 'v-unmod', description: 'Mod-Rechte entfernen', options: [{ name: 'user', type: 6, description: 'User', required: true }] },
    { name: 'v-trust', description: 'User erlauben zu joinen', options: [{ name: 'user', type: 6, description: 'User', required: true }] },
    { name: 'v-untrust', description: 'Vertrauen entziehen', options: [{ name: 'user', type: 6, description: 'User', required: true }] },
    { name: 'v-mute', description: 'User muten', options: [{ name: 'user', type: 6, description: 'User', required: true }] },
    { name: 'v-unmute', description: 'User entmuten', options: [{ name: 'user', type: 6, description: 'User', required: true }] },
    { name: 'v-deafen', description: 'User deafenen', options: [{ name: 'user', type: 6, description: 'User', required: true }] },
    { name: 'v-undeafen', description: 'User undeafenen', options: [{ name: 'user', type: 6, description: 'User', required: true }] },
    { name: 'v-bitrate', description: 'Bitrate setzen', options: [{ name: 'bitrate', type: 4, description: 'Bitrate in kbps (8-384)', required: true }] },
    { name: 'v-region', description: 'Region setzen', options: [{ name: 'region', type: 3, description: 'Region (auto, europe, us-east, ...)', required: true }] },
    { name: 'v-private', description: 'Channel komplett privat machen' },
    { name: 'v-public', description: 'Channel öffentlich machen' },
    { name: 'v-invite', description: 'User einladen', options: [{ name: 'user', type: 6, description: 'User', required: true }] },
    { name: 'v-move', description: 'User in den Channel verschieben', options: [{ name: 'user', type: 6, description: 'User', required: true }] },
    { name: 'v-claim', description: 'Channel claimen wenn Owner weg ist' },
    { name: 'v-members', description: 'Mitglieder anzeigen' },
    { name: 'v-permit', description: 'User Zutritt erlauben', options: [{ name: 'user', type: 6, description: 'User', required: true }] },
    { name: 'v-reject', description: 'User Zutritt verweigern', options: [{ name: 'user', type: 6, description: 'User', required: true }] },
    { name: 'v-reset', description: 'Channel auf Standard zurücksetzen' },
    { name: 'v-transfer', description: 'Ownership übertragen', options: [{ name: 'user', type: 6, description: 'Neuer Owner', required: true }] },
    { name: 'v-delete', description: 'Channel löschen' },
    { name: 'v-status', description: 'Channel Status anzeigen' },
    { name: 'v-banlist', description: 'Gebannte anzeigen' },
    { name: 'v-modlist', description: 'Mods und Co-Owner anzeigen' },
    { name: 'v-info', description: 'Channel Info anzeigen' },
    { name: 'v-help', description: 'Alle Voice-Befehle anzeigen' },
];

module.exports = { handleVoiceCreate, handleVoiceLeave, handleVoiceCommand, voiceCommands, isVoiceCommand: (name) => name.startsWith('v-') };
