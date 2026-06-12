const https = require('https');

function fetchUserData(username) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: 'www.duolingo.com',
      path: `/2017-06-30/users?username=${encodeURIComponent(username)}&fields=streak,totalXp,currentCourseId,courses,streakData`,
      method: 'GET',
      headers: {
        'User-Agent': 'MonsterjagdTeamsBot/1.0',
        'Accept': 'application/json',
      },
    };

    const req = https.request(options, (res) => {
      let data = '';
      res.on('data', (chunk) => { data += chunk; });
      res.on('end', () => {
        try {
          const json = JSON.parse(data);
          if (json.users && json.users.length > 0) {
            resolve(json.users[0]);
          } else {
            reject(new Error(`User "${username}" nicht gefunden`));
          }
        } catch (e) {
          reject(new Error(`Fehler beim Parsen der Duolingo-Daten: ${e.message}`));
        }
      });
    });

    req.on('error', reject);
    req.end();
  });
}

async function fetchAllUsers(usernames) {
  const results = [];
  for (const username of usernames) {
    try {
      const data = await fetchUserData(username.trim());
      results.push({
        username: username.trim(),
        streak: data.streak || 0,
        totalXp: data.totalXp || 0,
        courses: (data.courses || []).map(c => ({
          title: c.title,
          xp: c.xp,
          crowns: c.crowns,
        })),
        error: null,
      });
    } catch (err) {
      results.push({
        username: username.trim(),
        streak: 0,
        totalXp: 0,
        courses: [],
        error: err.message,
      });
    }
  }
  return results;
}

function buildDuolingoCard(usersData) {
  const rows = usersData.map(u => {
    if (u.error) {
      return `| ${u.username} | ❌ Fehler | - | - |`;
    }
    const courseList = u.courses.map(c => c.title).join(', ') || '-';
    return `| ${u.username} | 🔥 ${u.streak} Tage | ${u.totalXp} XP | ${courseList} |`;
  });

  const table = [
    '| User | Streak | Total XP | Kurse |',
    '|------|--------|----------|-------|',
    ...rows,
  ].join('\n');

  return {
    type: 'AdaptiveCard',
    $schema: 'http://adaptivecards.io/schemas/adaptive-card.json',
    version: '1.4',
    body: [
      {
        type: 'TextBlock',
        text: '🦉 Duolingo Tages-Update',
        weight: 'Bolder',
        size: 'Large',
      },
      {
        type: 'TextBlock',
        text: new Date().toLocaleDateString('de-AT', {
          weekday: 'long',
          year: 'numeric',
          month: 'long',
          day: 'numeric',
        }),
        isSubtle: true,
      },
      {
        type: 'TextBlock',
        text: table,
        wrap: true,
        fontType: 'Default',
      },
    ],
  };
}

module.exports = { fetchUserData, fetchAllUsers, buildDuolingoCard };
