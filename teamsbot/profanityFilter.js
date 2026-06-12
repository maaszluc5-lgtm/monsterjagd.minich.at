const BADWORDS_DE = [
  'scheiße', 'scheisse', 'scheiß', 'scheiss',
  'fick', 'ficken', 'gefickt', 'ficker',
  'arschloch', 'arsch',
  'hurensohn', 'hure', 'nutte',
  'wichser', 'wichsen',
  'missgeburt', 'misgeburt',
  'bastard',
  'fotze',
  'schwuchtel',
  'spast', 'spasti',
  'behindert',
  'idiot',
  'depp',
  'vollidiot',
  'drecksau',
  'dreckig',
  'penner',
  'trottel',
  'dummschwätzer',
];

const BADWORDS_EN = [
  'fuck', 'fucking', 'fucked', 'fucker',
  'shit', 'shitty',
  'bitch', 'bitches',
  'asshole', 'ass',
  'damn', 'dammit',
  'bastard',
  'dick', 'dickhead',
  'cunt',
  'piss',
  'retard', 'retarded',
  'whore',
  'slut',
];

const ALL_BADWORDS = [...BADWORDS_DE, ...BADWORDS_EN];

function containsProfanity(text) {
  const lower = text.toLowerCase();
  const found = [];

  for (const word of ALL_BADWORDS) {
    const regex = new RegExp(`\\b${escapeRegex(word)}\\b`, 'gi');
    if (regex.test(lower)) {
      found.push(word);
    }
  }

  return { hasProfanity: found.length > 0, words: found };
}

function censorText(text) {
  let censored = text;
  for (const word of ALL_BADWORDS) {
    const regex = new RegExp(`\\b${escapeRegex(word)}\\b`, 'gi');
    censored = censored.replace(regex, (match) => match[0] + '*'.repeat(match.length - 1));
  }
  return censored;
}

function escapeRegex(string) {
  return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

module.exports = { containsProfanity, censorText, ALL_BADWORDS };
