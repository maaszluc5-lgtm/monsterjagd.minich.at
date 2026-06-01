const express = require('express');
const router = express.Router();
const fs = require('fs');
const path = require('path');
const { getRankById } = require('../lib/ranks');
const { executeRconCommands } = require('../lib/rcon');

const PURCHASES_FILE = path.join(__dirname, '../data/purchases.json');

function readPurchases() {
  try {
    const raw = fs.readFileSync(PURCHASES_FILE, 'utf8');
    return JSON.parse(raw);
  } catch {
    return [];
  }
}

function writePurchases(data) {
  fs.writeFileSync(PURCHASES_FILE, JSON.stringify(data, null, 2), 'utf8');
}

// Simple session-like auth middleware using a cookie
function requireAuth(req, res, next) {
  if (req.cookies && req.cookies.admin_auth === process.env.ADMIN_PASSWORD) {
    return next();
  }
  res.redirect('/admin/login');
}

// GET /admin/login
router.get('/login', (req, res) => {
  res.send(`<!DOCTYPE html>
<html lang="de">
<head>
  <meta charset="UTF-8">
  <title>Admin Login – Monsterjagd Shop</title>
  <style>
    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
    body { background: #0d1117; color: #c9d1d9; font-family: 'Segoe UI', sans-serif; display: flex; align-items: center; justify-content: center; min-height: 100vh; }
    .card { background: #161b22; border: 1px solid #30363d; border-radius: 12px; padding: 2.5rem; width: 360px; }
    h1 { color: #f0a500; margin-bottom: 1.5rem; font-size: 1.5rem; text-align: center; }
    label { display: block; margin-bottom: 0.4rem; font-size: 0.9rem; color: #8b949e; }
    input { width: 100%; padding: 0.6rem 0.8rem; background: #0d1117; border: 1px solid #30363d; border-radius: 6px; color: #c9d1d9; font-size: 1rem; margin-bottom: 1rem; }
    input:focus { outline: none; border-color: #f0a500; }
    button { width: 100%; padding: 0.75rem; background: #f0a500; color: #0d1117; font-weight: 700; border: none; border-radius: 8px; font-size: 1rem; cursor: pointer; transition: background 0.2s; }
    button:hover { background: #e09400; }
    .error { color: #ff5555; text-align: center; margin-bottom: 1rem; font-size: 0.9rem; }
  </style>
</head>
<body>
  <div class="card">
    <h1>⚔ Admin Login</h1>
    ${req.query.error ? '<p class="error">Falsches Passwort.</p>' : ''}
    <form method="POST" action="/admin/login">
      <label for="password">Passwort</label>
      <input type="password" id="password" name="password" placeholder="Admin-Passwort" autofocus required>
      <button type="submit">Einloggen</button>
    </form>
  </div>
</body>
</html>`);
});

// POST /admin/login
router.post('/login', (req, res) => {
  const { password } = req.body;
  if (password === process.env.ADMIN_PASSWORD) {
    res.cookie('admin_auth', process.env.ADMIN_PASSWORD, { httpOnly: true, maxAge: 3600000 });
    res.redirect('/admin');
  } else {
    res.redirect('/admin/login?error=1');
  }
});

// GET /admin/logout
router.get('/logout', (req, res) => {
  res.clearCookie('admin_auth');
  res.redirect('/admin/login');
});

// GET /admin — dashboard
router.get('/', requireAuth, (req, res) => {
  const purchases = readPurchases();
  const pending = purchases.filter(p => p.status === 'pending');
  const completed = purchases.filter(p => p.status === 'completed');

  const paymentLabels = { paypal: 'PayPal', bank: 'Überweisung' };

  const rowHtml = (p, isPending) => `
    <tr>
      <td>${new Date(p.createdAt).toLocaleString('de-AT')}</td>
      <td><strong>${escHtml(p.username)}</strong></td>
      <td>${escHtml(p.rankName)}</td>
      <td>€${p.price.toFixed(2)}</td>
      <td>${paymentLabels[p.paymentMethod] || p.paymentMethod}</td>
      <td>${escHtml(p.email)}</td>
      <td>
        ${isPending
          ? `<button class="btn-grant" onclick="grantRank('${p.id}')">Rang vergeben</button>`
          : '<span class="badge-done">✓ Aktiviert</span>'
        }
        ${isPending ? '' : p.completedAt ? `<br><small>${new Date(p.completedAt).toLocaleString('de-AT')}</small>` : ''}
      </td>
    </tr>`;

  function escHtml(str) {
    return String(str).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  }

  res.send(`<!DOCTYPE html>
<html lang="de">
<head>
  <meta charset="UTF-8">
  <title>Admin – Monsterjagd Shop</title>
  <style>
    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
    body { background: #0d1117; color: #c9d1d9; font-family: 'Segoe UI', sans-serif; padding: 2rem; }
    h1 { color: #f0a500; font-size: 1.8rem; margin-bottom: 0.25rem; }
    .subtitle { color: #8b949e; margin-bottom: 2rem; }
    .stats { display: flex; gap: 1rem; margin-bottom: 2rem; }
    .stat-card { background: #161b22; border: 1px solid #30363d; border-radius: 8px; padding: 1rem 1.5rem; }
    .stat-card .num { font-size: 2rem; font-weight: 700; color: #f0a500; }
    .stat-card .label { color: #8b949e; font-size: 0.85rem; }
    h2 { color: #c9d1d9; font-size: 1.2rem; margin-bottom: 1rem; border-bottom: 1px solid #30363d; padding-bottom: 0.5rem; }
    table { width: 100%; border-collapse: collapse; margin-bottom: 2rem; background: #161b22; border-radius: 8px; overflow: hidden; border: 1px solid #30363d; }
    th { background: #1f2937; color: #8b949e; font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.05em; padding: 0.75rem 1rem; text-align: left; }
    td { padding: 0.75rem 1rem; border-top: 1px solid #30363d; font-size: 0.9rem; vertical-align: middle; }
    tr:hover td { background: rgba(240,165,0,0.04); }
    .btn-grant { background: #f0a500; color: #0d1117; font-weight: 700; border: none; border-radius: 6px; padding: 0.4rem 0.85rem; cursor: pointer; font-size: 0.85rem; transition: background 0.2s; }
    .btn-grant:hover { background: #e09400; }
    .badge-done { color: #3fb950; font-weight: 600; font-size: 0.85rem; }
    .empty { color: #8b949e; text-align: center; padding: 2rem; }
    nav { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; }
    .nav-links a { color: #8b949e; text-decoration: none; margin-left: 1rem; font-size: 0.9rem; }
    .nav-links a:hover { color: #f0a500; }
    #toast { position: fixed; bottom: 2rem; right: 2rem; background: #161b22; border: 1px solid #30363d; border-radius: 8px; padding: 1rem 1.5rem; color: #c9d1d9; font-size: 0.9rem; display: none; z-index: 100; }
    #toast.success { border-color: #3fb950; }
    #toast.error { border-color: #ff5555; }
  </style>
</head>
<body>
  <nav>
    <div><h1>⚔ Admin Panel</h1><p class="subtitle">Monsterjagd Shop – Bestellverwaltung</p></div>
    <div class="nav-links">
      <a href="/">← Zur Shop-Seite</a>
      <a href="/admin/logout">Ausloggen</a>
    </div>
  </nav>

  <div class="stats">
    <div class="stat-card">
      <div class="num">${pending.length}</div>
      <div class="label">Ausstehende Bestellungen</div>
    </div>
    <div class="stat-card">
      <div class="num">${completed.length}</div>
      <div class="label">Abgeschlossene Bestellungen</div>
    </div>
    <div class="stat-card">
      <div class="num">€${completed.reduce((s,p) => s + p.price, 0).toFixed(2)}</div>
      <div class="label">Gesamtumsatz (abgeschlossen)</div>
    </div>
  </div>

  <h2>Ausstehende Bestellungen</h2>
  ${pending.length === 0
    ? '<p class="empty">Keine ausstehenden Bestellungen.</p>'
    : `<table>
        <thead><tr><th>Datum</th><th>Spieler</th><th>Rang</th><th>Preis</th><th>Zahlung</th><th>E-Mail</th><th>Aktion</th></tr></thead>
        <tbody>${pending.map(p => rowHtml(p, true)).join('')}</tbody>
      </table>`
  }

  <h2>Abgeschlossene Bestellungen</h2>
  ${completed.length === 0
    ? '<p class="empty">Keine abgeschlossenen Bestellungen.</p>'
    : `<table>
        <thead><tr><th>Datum</th><th>Spieler</th><th>Rang</th><th>Preis</th><th>Zahlung</th><th>E-Mail</th><th>Status</th></tr></thead>
        <tbody>${completed.map(p => rowHtml(p, false)).join('')}</tbody>
      </table>`
  }

  <div id="toast"></div>

  <script>
    function showToast(msg, type) {
      const t = document.getElementById('toast');
      t.textContent = msg;
      t.className = type;
      t.style.display = 'block';
      setTimeout(() => { t.style.display = 'none'; }, 5000);
    }

    async function grantRank(purchaseId) {
      const btn = event.target;
      btn.disabled = true;
      btn.textContent = 'Wird vergeben…';
      try {
        const res = await fetch('/admin/api/grant', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ purchaseId })
        });
        const data = await res.json();
        if (data.success) {
          showToast('Rang erfolgreich vergeben! RCON-Antwort: ' + (data.rconResults || []).map(r => r.response || r.error).join(' | '), 'success');
          setTimeout(() => location.reload(), 2000);
        } else {
          showToast('Fehler: ' + (data.error || 'Unbekannter Fehler'), 'error');
          btn.disabled = false;
          btn.textContent = 'Rang vergeben';
        }
      } catch (e) {
        showToast('Netzwerkfehler: ' + e.message, 'error');
        btn.disabled = false;
        btn.textContent = 'Rang vergeben';
      }
    }
  </script>
</body>
</html>`);
});

// POST /admin/api/grant — grant rank via RCON
router.post('/api/grant', requireAuth, async (req, res) => {
  const { purchaseId } = req.body;
  if (!purchaseId) return res.status(400).json({ error: 'Fehlende Purchase-ID.' });

  const purchases = readPurchases();
  const purchase = purchases.find(p => p.id === purchaseId);
  if (!purchase) return res.status(404).json({ error: 'Bestellung nicht gefunden.' });
  if (purchase.status === 'completed') return res.status(400).json({ error: 'Rang wurde bereits vergeben.' });

  const rank = getRankById(purchase.rankId);
  if (!rank) return res.status(400).json({ error: 'Rang-Konfiguration nicht gefunden.' });

  const commands = [
    `lp user ${purchase.username} parent set ${rank.luckpermsGroup}`,
    `lp user ${purchase.username} meta setprefix 100 ${rank.prefix}`
  ];

  let rconResults = [];
  let rconError = null;
  try {
    rconResults = await executeRconCommands(commands);
  } catch (e) {
    rconError = e.message;
  }

  // Mark as completed regardless (admin can retry RCON manually)
  purchase.status = 'completed';
  purchase.completedAt = new Date().toISOString();
  purchase.rconResults = rconResults;
  writePurchases(purchases);

  res.json({ success: true, rconResults, rconError });
});

module.exports = router;
