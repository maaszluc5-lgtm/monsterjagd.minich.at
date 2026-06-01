const RANKS = [
  {
    id: 'vip',
    name: 'VIP',
    price: 4.99,
    color: '#55FF55',
    colorName: 'Green',
    mcColor: '§a',
    prefix: '§a[VIP]§r',
    maxPlots: 4,
    permissions: ['opserver.kit.vip'],
    perks: [
      'Kit VIP (alle 24h)',
      'Max. 4 Grundstücke',
      'Grünes Namens-Prefix [VIP]',
      'Zugang zu VIP-Bereichen'
    ],
    luckpermsGroup: 'vip'
  },
  {
    id: 'vip_plus',
    name: 'VIP+',
    price: 9.99,
    color: '#55FFFF',
    colorName: 'Aqua',
    mcColor: '§b',
    prefix: '§b[VIP+]§r',
    maxPlots: 6,
    permissions: ['opserver.kit.vip', 'opserver.kit.vip_plus'],
    perks: [
      'Alle VIP-Vorteile',
      'Kit VIP+ (alle 24h)',
      'Max. 6 Grundstücke',
      'Aqua Namens-Prefix [VIP+]'
    ],
    luckpermsGroup: 'vip_plus'
  },
  {
    id: 'mvp',
    name: 'MVP',
    price: 19.99,
    color: '#FFAA00',
    colorName: 'Gold',
    mcColor: '§6',
    prefix: '§6[MVP]§r',
    maxPlots: 8,
    permissions: ['opserver.kit.vip', 'opserver.kit.vip_plus', 'opserver.kit.mvp'],
    perks: [
      'Alle VIP+-Vorteile',
      'Kit MVP (alle 24h)',
      'Max. 8 Grundstücke',
      'Goldenes Namens-Prefix [MVP]'
    ],
    luckpermsGroup: 'mvp'
  },
  {
    id: 'mvp_plus',
    name: 'MVP+',
    price: 34.99,
    color: '#FF5555',
    colorName: 'Red',
    mcColor: '§c',
    prefix: '§c[MVP+]§r',
    maxPlots: 10,
    permissions: ['opserver.kit.vip', 'opserver.kit.vip_plus', 'opserver.kit.mvp', 'opserver.kit.mvp_plus'],
    perks: [
      'Alle MVP-Vorteile',
      'Kit MVP+ (alle 24h)',
      'Max. 10 Grundstücke',
      'Rotes Namens-Prefix [MVP+]'
    ],
    luckpermsGroup: 'mvp_plus'
  },
  {
    id: 'elite',
    name: 'ELITE',
    price: 59.99,
    color: '#AA00AA',
    colorName: 'Purple',
    mcColor: '§5',
    prefix: '§5[ELITE]§r',
    maxPlots: 15,
    permissions: ['opserver.kit.vip', 'opserver.kit.vip_plus', 'opserver.kit.mvp', 'opserver.kit.mvp_plus', 'opserver.kit.elite'],
    perks: [
      'Alle MVP+-Vorteile',
      'Kit ELITE (alle 24h)',
      'Max. 15 Grundstücke',
      'Lila Namens-Prefix [ELITE]'
    ],
    luckpermsGroup: 'elite'
  },
  {
    id: 'god',
    name: 'GOD',
    price: 99.99,
    color: '#AA0000',
    colorName: 'Dark Red',
    mcColor: '§4',
    prefix: '§4[GOD]§r',
    maxPlots: -1, // unlimited
    permissions: ['opserver.kit.vip', 'opserver.kit.vip_plus', 'opserver.kit.mvp', 'opserver.kit.mvp_plus', 'opserver.kit.elite', 'opserver.admin'],
    perks: [
      'Alle ELITE-Vorteile',
      'Unbegrenzte Grundstücke',
      'Admin-Zugang (opserver.admin)',
      'Dunkelrotes Namens-Prefix [GOD]'
    ],
    luckpermsGroup: 'god'
  }
];

function getRankById(id) {
  return RANKS.find(r => r.id === id.toLowerCase()) || null;
}

module.exports = { RANKS, getRankById };
