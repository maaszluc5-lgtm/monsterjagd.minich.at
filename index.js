require('dotenv').config();
const mcbot = require('./mcbot');
const discord = require('./discordbot');

mcbot.connect();
discord.start();
