require('dotenv').config();
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const appId = process.env.MICROSOFT_APP_ID;
const hostname = process.env.BOT_HOSTNAME || 'bot.monsterjagd.minich.at';

if (!appId || appId === 'your-app-id-here') {
  console.error('Setze MICROSOFT_APP_ID in der .env Datei!');
  process.exit(1);
}

const manifestPath = path.join(__dirname, 'manifest', 'manifest.json');
let manifest = fs.readFileSync(manifestPath, 'utf-8');
manifest = manifest.replace(/\{\{MICROSOFT_APP_ID\}\}/g, appId);
manifest = manifest.replace(/\{\{BOT_HOSTNAME\}\}/g, hostname);

const outDir = path.join(__dirname, 'manifest-out');
if (!fs.existsSync(outDir)) fs.mkdirSync(outDir);

fs.writeFileSync(path.join(outDir, 'manifest.json'), manifest);

const colorIcon = path.join(__dirname, 'manifest', 'color.png');
const outlineIcon = path.join(__dirname, 'manifest', 'outline.png');
if (!fs.existsSync(colorIcon)) {
  console.warn('⚠️  color.png fehlt in manifest/ - lege ein 192x192 PNG Icon dort ab');
}
if (!fs.existsSync(outlineIcon)) {
  console.warn('⚠️  outline.png fehlt in manifest/ - lege ein 32x32 PNG Icon dort ab');
}

if (fs.existsSync(colorIcon)) fs.copyFileSync(colorIcon, path.join(outDir, 'color.png'));
if (fs.existsSync(outlineIcon)) fs.copyFileSync(outlineIcon, path.join(outDir, 'outline.png'));

const zipPath = path.join(__dirname, 'monsterjagd-bot.zip');
execSync(`cd "${outDir}" && zip -r "${zipPath}" .`);

console.log(`\n✅ Teams App Manifest erstellt: ${zipPath}`);
console.log('   Lade diese ZIP-Datei in Teams hoch:');
console.log('   Teams → Apps → App hochladen → Benutzerdefinierte App hochladen');
