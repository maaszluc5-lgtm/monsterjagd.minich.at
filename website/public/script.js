// ============================================================
// MONSTERJAGD SHOP — Main Frontend Script
// ============================================================

const RANK_SUBTITLES = {
  vip: 'Einstiegsrang',
  vip_plus: 'Erweiterter VIP',
  mvp: 'Premium Rang',
  mvp_plus: 'Premium Plus',
  elite: 'Elite Rang',
  god: 'Höchster Rang'
};

const POPULAR_RANK = 'mvp';

async function loadRanks() {
  const grid = document.getElementById('ranksGrid');
  if (!grid) return;

  try {
    const res = await fetch('/api/ranks');
    if (!res.ok) throw new Error('Failed to load ranks');
    const ranks = await res.json();
    renderRanks(ranks, grid);
  } catch (e) {
    grid.innerHTML = '<div style="grid-column:1/-1;text-align:center;color:#ff5555;padding:3rem;">Fehler beim Laden der Ränge. Bitte Seite neu laden.</div>';
    console.error(e);
  }
}

function formatPrice(price) {
  return price.toFixed(2).replace('.', ',');
}

function renderRanks(ranks, grid) {
  grid.innerHTML = '';
  ranks.forEach(rank => {
    const isPopular = rank.id === POPULAR_RANK;
    const card = document.createElement('div');
    card.className = 'rank-card animate-in' + (isPopular ? ' popular' : '');
    card.dataset.rank = rank.id;
    card.style.setProperty('--rank-color', rank.color);

    const perksHtml = rank.perks.map(p => `<li>${escHtml(p)}</li>`).join('');

    card.innerHTML = `
      ${isPopular ? '<div class="popular-badge">⭐ Beliebt</div>' : ''}
      <div class="rank-badge">${escHtml(rank.name)}</div>
      <div class="rank-subtitle">${escHtml(RANK_SUBTITLES[rank.id] || '')}</div>
      <div class="rank-price">
        <span class="currency">€</span>
        <span class="amount">${formatPrice(rank.price)}</span>
        <span class="period">einmalig</span>
      </div>
      <hr class="rank-divider">
      <ul class="rank-perks">${perksHtml}</ul>
      <a href="/checkout?rank=${encodeURIComponent(rank.id)}" class="btn-buy" style="background:${rank.color};color:${isLightColor(rank.color)?'#0d1117':'#0d1117'}">
        Kaufen
      </a>
    `;

    grid.appendChild(card);
  });
}

// Determine if a hex color is light enough to need dark text
function isLightColor(hex) {
  const r = parseInt(hex.slice(1,3), 16);
  const g = parseInt(hex.slice(3,5), 16);
  const b = parseInt(hex.slice(5,7), 16);
  const luminance = (0.299*r + 0.587*g + 0.114*b) / 255;
  return luminance > 0.45;
}

function escHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function copyIP(el) {
  navigator.clipboard.writeText('play.monsterjagd.at').then(() => {
    const orig = el.textContent;
    el.textContent = '✓ Kopiert!';
    setTimeout(() => { el.textContent = orig; }, 1500);
  }).catch(() => {});
}

// Init
document.addEventListener('DOMContentLoaded', () => {
  loadRanks();
});
