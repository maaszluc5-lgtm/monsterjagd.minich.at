const express = require('express');
const router = express.Router();
const fs = require('fs');
const path = require('path');
const RconClient = require('../lib/rcon');

const DATA_FILE = path.join(__dirname, '../data/votes.json');

function loadVotes() {
  if (!fs.existsSync(DATA_FILE)) return {};
  try { return JSON.parse(fs.readFileSync(DATA_FILE, 'utf8')); } catch { return {}; }
}

function saveVotes(data) {
  fs.mkdirSync(path.dirname(DATA_FILE), { recursive: true });
  fs.writeFileSync(DATA_FILE, JSON.stringify(data, null, 2));
}

// Returns true if the player/IP can vote (24h cooldown)
function canVote(votes, key) {
  const last = votes[key];
  if (!last) return true;
  return Date.now() - last > 24 * 60 * 60 * 1000;
}

function timeUntilNextVote(votes, key) {
  const last = votes[key];
  if (!last) return 0;
  const ms = (24 * 60 * 60 * 1000) - (Date.now() - last);
  if (ms <= 0) return 0;
  const h = Math.floor(ms / 3600000);
  const m = Math.floor((ms % 3600000) / 60000);
  return `${h}h ${m}m`;
}

async function sendRcon(command) {
  const rcon = new RconClient(
    process.env.MC_HOST || 'localhost',
    parseInt(process.env.MC_RCON_PORT || '25575'),
    process.env.MC_RCON_PASSWORD || ''
  );
  try {
    await rcon.connect();
    const result = await rcon.send(command);
    rcon.disconnect();
    return result;
  } catch (e) {
    try { rcon.disconnect(); } catch {}
    throw e;
  }
}

// GET /vote
router.get('/vote', (req, res) => {
  res.send(renderVotePage({ error: null, success: null, username: '' }));
});

// POST /vote
router.post('/vote', async (req, res) => {
  const username = (req.body.username || '').trim();
  const ip = req.headers['x-forwarded-for']?.split(',')[0] || req.socket.remoteAddress;

  if (!username || !/^[a-zA-Z0-9_]{3,16}$/.test(username)) {
    return res.send(renderVotePage({ error: 'Ungültiger Spielername (3-16 Zeichen, nur Buchstaben/Zahlen/_).', success: null, username }));
  }

  const votes = loadVotes();
  const keyPlayer = `player:${username.toLowerCase()}`;
  const keyIp = `ip:${ip}`;

  if (!canVote(votes, keyPlayer)) {
    const wait = timeUntilNextVote(votes, keyPlayer);
    return res.send(renderVotePage({ error: `Du hast heute schon gevotet! Nächstes Vote in: ${wait}`, success: null, username }));
  }
  if (!canVote(votes, keyIp)) {
    const wait = timeUntilNextVote(votes, keyIp);
    return res.send(renderVotePage({ error: `Von dieser IP wurde heute schon gevotet! Nächstes Vote in: ${wait}`, success: null, username }));
  }

  // Record vote
  const now = Date.now();
  votes[keyPlayer] = now;
  votes[keyIp] = now;

  // Track total vote count per player
  const countKey = `count:${username.toLowerCase()}`;
  votes[countKey] = (votes[countKey] || 0) + 1;

  saveVotes(votes);

  // Send RCON command to give crate
  try {
    await sendRcon(`givecrate ${username} common 1`);
  } catch (e) {
    // RCON failed — still record vote, reward will be pending
    console.error('RCON error:', e.message);
  }

  const totalVotes = votes[countKey] || 1;
  return res.send(renderVotePage({
    error: null,
    success: `✅ Danke, ${username}! Du hast eine §Gewöhnliche Kiste erhalten! Du hast insgesamt ${totalVotes}x gevotet.`,
    username: ''
  }));
});

// GET /vote/top — top voters leaderboard
router.get('/vote/top', (req, res) => {
  const votes = loadVotes();
  const top = Object.entries(votes)
    .filter(([k]) => k.startsWith('count:'))
    .map(([k, v]) => ({ name: k.replace('count:', ''), votes: v }))
    .sort((a, b) => b.votes - a.votes)
    .slice(0, 10);
  res.send(renderTopPage(top));
});

function renderVotePage({ error, success, username }) {
  return `<!DOCTYPE html>
<html lang="de">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Monsterjagd — Vote</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { background: #0d1117; color: #c9d1d9; font-family: 'Segoe UI', sans-serif; min-height: 100vh; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 2rem; }
    .card { background: #161b22; border: 1px solid #30363d; border-radius: 12px; padding: 2.5rem 2rem; max-width: 480px; width: 100%; text-align: center; box-shadow: 0 8px 32px rgba(0,0,0,0.5); }
    h1 { color: #f0a500; font-size: 2rem; margin-bottom: 0.4rem; }
    .subtitle { color: #8b949e; margin-bottom: 2rem; font-size: 0.95rem; }
    .reward-box { background: #1c2128; border: 1px solid #f0a500; border-radius: 8px; padding: 1rem; margin-bottom: 1.5rem; }
    .reward-box p { color: #f0a500; font-weight: bold; font-size: 1.1rem; }
    .reward-box small { color: #8b949e; font-size: 0.85rem; }
    label { display: block; text-align: left; color: #8b949e; font-size: 0.85rem; margin-bottom: 0.4rem; }
    input[type=text] { width: 100%; padding: 0.75rem 1rem; background: #0d1117; border: 1px solid #30363d; border-radius: 8px; color: #c9d1d9; font-size: 1rem; margin-bottom: 1rem; outline: none; transition: border 0.2s; }
    input[type=text]:focus { border-color: #f0a500; }
    button { width: 100%; padding: 0.85rem; background: linear-gradient(135deg, #f0a500, #e05000); border: none; border-radius: 8px; color: #fff; font-size: 1.1rem; font-weight: bold; cursor: pointer; transition: opacity 0.2s; }
    button:hover { opacity: 0.85; }
    .error { background: #3d1c1c; border: 1px solid #f85149; border-radius: 8px; padding: 0.75rem 1rem; color: #f85149; margin-bottom: 1rem; font-size: 0.9rem; }
    .success { background: #1c3d1c; border: 1px solid #3fb950; border-radius: 8px; padding: 0.75rem 1rem; color: #3fb950; margin-bottom: 1rem; font-size: 0.95rem; }
    .links { margin-top: 1.5rem; display: flex; gap: 1rem; justify-content: center; }
    .links a { color: #8b949e; text-decoration: none; font-size: 0.85rem; }
    .links a:hover { color: #f0a500; }
    .server-info { margin-top: 1rem; color: #8b949e; font-size: 0.85rem; }
    .server-info span { color: #f0a500; font-weight: bold; }
  </style>
</head>
<body>
  <div class="card">
    <h1>⚔ Monsterjagd</h1>
    <p class="subtitle">Stimme für unseren Server und erhalte eine Belohnung!</p>

    <div class="reward-box">
      <p>🎁 Belohnung: Gewöhnliche Kiste</p>
      <small>Enthält Coins, Kristalle und Items — einmal täglich</small>
    </div>

    ${error ? `<div class="error">❌ ${error}</div>` : ''}
    ${success ? `<div class="success">${success}</div>` : ''}

    <form method="POST" action="/vote">
      <label for="username">Dein Minecraft-Name</label>
      <input type="text" id="username" name="username" placeholder="z.B. GrassGlas7797" value="${username}" maxlength="16" autocomplete="off" required>
      <button type="submit">✅ Jetzt voten & Kiste erhalten!</button>
    </form>

    <p class="server-info">Server: <span>monsterjagd.minich.at</span></p>

    <div class="links">
      <a href="/">🏪 Shop</a>
      <a href="/vote/top">🏆 Top Voter</a>
    </div>
  </div>
</body>
</html>`;
}

function renderTopPage(top) {
  const medals = ['🥇', '🥈', '🥉'];
  const rows = top.map((p, i) =>
    `<tr><td>${medals[i] || (i + 1)}</td><td>${p.name}</td><td>${p.votes}</td></tr>`
  ).join('');

  return `<!DOCTYPE html>
<html lang="de">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Top Voter — Monsterjagd</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { background: #0d1117; color: #c9d1d9; font-family: 'Segoe UI', sans-serif; display: flex; flex-direction: column; align-items: center; padding: 3rem 1rem; }
    h1 { color: #f0a500; font-size: 2rem; margin-bottom: 2rem; }
    table { border-collapse: collapse; width: 100%; max-width: 480px; }
    th, td { padding: 0.75rem 1rem; text-align: left; border-bottom: 1px solid #30363d; }
    th { color: #8b949e; font-weight: normal; font-size: 0.85rem; }
    td:last-child { color: #f0a500; font-weight: bold; }
    .back { margin-top: 2rem; color: #8b949e; text-decoration: none; }
    .back:hover { color: #f0a500; }
    .empty { color: #8b949e; margin-top: 2rem; }
  </style>
</head>
<body>
  <h1>🏆 Top Voter</h1>
  ${top.length ? `<table><tr><th>#</th><th>Spieler</th><th>Votes</th></tr>${rows}</table>` : '<p class="empty">Noch keine Votes.</p>'}
  <a class="back" href="/vote">← Zurück zum Voten</a>
</body>
</html>`;
}

module.exports = router;
