const PHISHING_PATTERNS = [
  // Dringende Handlungsaufforderungen
  /dein.*(konto|account).*(gesperrt|deaktiviert|eingeschr[äa]nkt)/i,
  /your.*(account|password).*(expired|suspended|locked|compromised)/i,
  /sofort.*(handeln|reagieren|bestätigen|verifizieren)/i,
  /immediate.*(action|response|verification).*(required|needed)/i,
  /within\s+\d+\s*(hours?|minutes?|days?)/i,
  /innerhalb\s+von\s+\d+\s*(stunden?|minuten?|tagen?)/i,

  // Fake Login/Verifizierung
  /klicke?\s+(hier|unten|auf den link).*(verifizier|bestätig|anmeld)/i,
  /click\s+(here|below|the link).*(verify|confirm|sign.?in|log.?in)/i,
  /passwort.*(?:zurücksetzen|ändern|bestätigen|aktualisieren)/i,
  /password.*(?:reset|change|confirm|update|expire)/i,

  // Gewinn/Geld-Betrug
  /(?:gewonnen|gewinn|preis|lotteri|erbe|erbschaft)/i,
  /(?:congratulations|winner|prize|lottery|inheritance)/i,
  /(?:million|tausend|thousand).*(?:dollar|euro|usd|eur)/i,

  // CEO/Chef-Betrug
  /(?:dringend|urgent|asap).*(?:überweis|transfer|zahlung|payment|wire)/i,
  /(?:geschenkkarte|gift\s*card|gutschein|voucher).*(?:kauf|buy|send)/i,

  // Verdächtige Absender-Muster
  /(?:microsoft|google|apple|amazon|paypal|bank).*(?:support|security|team|service)/i,

  // Malware/Attachment-Warnzeichen
  /(?:rechnung|invoice|dokument|document).*(?:anhang|attachment|öffn|open)/i,
  /(?:makro|macro).*(?:aktivier|enable|erlauben|allow)/i,

  // Verdächtige URLs
  /bit\.ly|tinyurl|t\.co|goo\.gl|shorturl/i,
  /(?:login|signin|verify|secure|account)\.[a-z]+\.[a-z]{2,}/i,
];

const SUSPICIOUS_DOMAINS = [
  /microsoft\d+\.com/i,
  /micros0ft\.com/i,
  /m1crosoft\.com/i,
  /gooogle\.com/i,
  /g00gle\.com/i,
  /amaz0n\.com/i,
  /paypa1\.com/i,
  /app1e\.com/i,
  /0utlook\.com/i,
  /0ffice365\.com/i,
  /sharepo1nt\.com/i,
];

const URGENCY_WORDS_DE = [
  'dringend', 'sofort', 'unverzüglich', 'warnung', 'achtung',
  'letzte mahnung', 'letzte warnung', 'frist', 'ablauf',
];

const URGENCY_WORDS_EN = [
  'urgent', 'immediately', 'warning', 'alert', 'final notice',
  'last warning', 'deadline', 'expires', 'suspended',
];

function analyzeMessage(text, sender) {
  const threats = [];
  let score = 0;

  for (const pattern of PHISHING_PATTERNS) {
    if (pattern.test(text)) {
      threats.push({ type: 'pattern', detail: pattern.source.substring(0, 60) });
      score += 20;
    }
  }

  const urlRegex = /https?:\/\/[^\s<>"{}|\\^`[\]]+/gi;
  const urls = text.match(urlRegex) || [];
  for (const url of urls) {
    for (const domainPattern of SUSPICIOUS_DOMAINS) {
      if (domainPattern.test(url)) {
        threats.push({ type: 'suspicious_url', detail: url });
        score += 30;
      }
    }
    if (url.includes('@') || url.match(/%[0-9a-f]{2}/gi)?.length > 3) {
      threats.push({ type: 'obfuscated_url', detail: url });
      score += 25;
    }
  }

  const allUrgency = [...URGENCY_WORDS_DE, ...URGENCY_WORDS_EN];
  let urgencyCount = 0;
  for (const word of allUrgency) {
    if (text.toLowerCase().includes(word)) urgencyCount++;
  }
  if (urgencyCount >= 2) {
    threats.push({ type: 'urgency', detail: `${urgencyCount} Dringlichkeits-Wörter` });
    score += urgencyCount * 10;
  }

  if (sender) {
    for (const domainPattern of SUSPICIOUS_DOMAINS) {
      if (domainPattern.test(sender)) {
        threats.push({ type: 'suspicious_sender', detail: sender });
        score += 35;
      }
    }
  }

  let level = 'safe';
  if (score >= 60) level = 'danger';
  else if (score >= 30) level = 'warning';
  else if (score > 0) level = 'suspicious';

  return { score, level, threats };
}

function buildPhishingAlert(analysis, originalText, sender) {
  const levelEmoji = {
    danger: '🚨',
    warning: '⚠️',
    suspicious: '🔍',
  };

  const levelText = {
    danger: 'GEFAHR - Sehr wahrscheinlich Phishing/Hacking!',
    warning: 'WARNUNG - Verdächtige Nachricht erkannt',
    suspicious: 'VERDÄCHTIG - Bitte genauer prüfen',
  };

  const emoji = levelEmoji[analysis.level] || '✅';
  const text = levelText[analysis.level] || 'Sicher';

  const threatList = analysis.threats
    .map(t => `- **${t.type}**: ${t.detail}`)
    .join('\n');

  return {
    type: 'AdaptiveCard',
    $schema: 'http://adaptivecards.io/schemas/adaptive-card.json',
    version: '1.4',
    body: [
      {
        type: 'TextBlock',
        text: `${emoji} ${text}`,
        weight: 'Bolder',
        size: 'Large',
        color: analysis.level === 'danger' ? 'Attention' : 'Warning',
      },
      {
        type: 'TextBlock',
        text: `**Risiko-Score:** ${analysis.score}/100`,
        wrap: true,
      },
      {
        type: 'TextBlock',
        text: `**Absender:** ${sender || 'Unbekannt'}`,
        wrap: true,
      },
      {
        type: 'TextBlock',
        text: '**Erkannte Bedrohungen:**',
        weight: 'Bolder',
      },
      {
        type: 'TextBlock',
        text: threatList || 'Keine',
        wrap: true,
      },
      {
        type: 'TextBlock',
        text: '---',
      },
      {
        type: 'TextBlock',
        text: '**Tipps:**\n- Klicke NICHT auf verdächtige Links\n- Gib KEINE Passwörter ein\n- Melde die Nachricht deinem IT-Admin\n- Leite verdächtige Mails an die IT weiter',
        wrap: true,
        isSubtle: true,
      },
    ],
  };
}

module.exports = { analyzeMessage, buildPhishingAlert };
