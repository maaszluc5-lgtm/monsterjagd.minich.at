require('dotenv').config();
const express = require('express');
const path = require('path');
const cookieParser = require('cookie-parser');

const shopRoutes = require('./routes/shop');
const purchaseRoutes = require('./routes/purchase');
const adminRoutes = require('./routes/admin');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(cookieParser());
app.use(express.static(path.join(__dirname, 'public')));

// Routes
app.use('/', shopRoutes);
app.use('/', purchaseRoutes);
app.use('/admin', adminRoutes);

// 404
app.use((req, res) => {
  res.status(404).send(`<!DOCTYPE html>
<html lang="de">
<head>
  <meta charset="UTF-8">
  <title>404 – Monsterjagd</title>
  <style>
    body { background: #0d1117; color: #c9d1d9; font-family: 'Segoe UI', sans-serif; display: flex; align-items: center; justify-content: center; min-height: 100vh; text-align: center; }
    h1 { color: #f0a500; font-size: 4rem; }
    p { color: #8b949e; margin: 1rem 0; }
    a { color: #f0a500; text-decoration: none; }
    a:hover { text-decoration: underline; }
  </style>
</head>
<body>
  <div>
    <h1>404</h1>
    <p>Seite nicht gefunden.</p>
    <a href="/">← Zurück zum Shop</a>
  </div>
</body>
</html>`);
});

app.listen(PORT, () => {
  console.log(`Monsterjagd Shop läuft auf http://localhost:${PORT}`);
});
