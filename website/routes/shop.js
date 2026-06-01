const express = require('express');
const router = express.Router();
const { RANKS, getRankById } = require('../lib/ranks');
const path = require('path');

// Main shop page
router.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, '../public/index.html'));
});

// Rank detail page
router.get('/rank/:id', (req, res) => {
  const rank = getRankById(req.params.id);
  if (!rank) return res.status(404).send('Rang nicht gefunden');
  res.sendFile(path.join(__dirname, '../views/rank-detail.html'));
});

// Checkout page
router.get('/checkout', (req, res) => {
  const rank = getRankById(req.query.rank || '');
  if (!rank) return res.redirect('/');
  res.sendFile(path.join(__dirname, '../views/checkout.html'));
});

// Success page
router.get('/success', (req, res) => {
  res.sendFile(path.join(__dirname, '../views/success.html'));
});

// API: get ranks data (for frontend)
router.get('/api/ranks', (req, res) => {
  res.json(RANKS);
});

// API: get single rank data
router.get('/api/ranks/:id', (req, res) => {
  const rank = getRankById(req.params.id);
  if (!rank) return res.status(404).json({ error: 'Rang nicht gefunden' });
  res.json(rank);
});

module.exports = router;
