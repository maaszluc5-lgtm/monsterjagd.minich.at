const express = require('express');
const router = express.Router();
const fs = require('fs');
const path = require('path');
const { getRankById } = require('../lib/ranks');

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

// POST /api/buy — save a new pending purchase
router.post('/api/buy', (req, res) => {
  const { rank: rankId, username, email, paymentMethod } = req.body;

  if (!rankId || !username || !email || !paymentMethod) {
    return res.status(400).json({ error: 'Alle Felder müssen ausgefüllt sein.' });
  }

  const rank = getRankById(rankId);
  if (!rank) {
    return res.status(400).json({ error: 'Ungültiger Rang.' });
  }

  // Basic validation
  const usernameRegex = /^[a-zA-Z0-9_]{3,16}$/;
  if (!usernameRegex.test(username)) {
    return res.status(400).json({ error: 'Ungültiger Minecraft-Benutzername (3-16 Zeichen, nur Buchstaben, Ziffern und _).' });
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email)) {
    return res.status(400).json({ error: 'Ungültige E-Mail-Adresse.' });
  }

  if (!['paypal', 'bank'].includes(paymentMethod)) {
    return res.status(400).json({ error: 'Ungültige Zahlungsmethode.' });
  }

  const purchases = readPurchases();
  const purchase = {
    id: Date.now().toString() + Math.random().toString(36).slice(2, 7),
    rankId: rank.id,
    rankName: rank.name,
    price: rank.price,
    username,
    email,
    paymentMethod,
    status: 'pending',
    createdAt: new Date().toISOString(),
    completedAt: null
  };

  purchases.push(purchase);
  writePurchases(purchases);

  res.json({ success: true, purchaseId: purchase.id });
});

module.exports = router;
