const token = process.env.MANGA_SYNC_TOKEN;
if (!token) throw new Error("MANGA_SYNC_TOKEN is missing");

const response = await fetch(
  `https://mangaworldsync-production.up.railway.app/mw/api/notifications/check?token=${encodeURIComponent(token)}`,
  { method: "POST" },
);

if (!response.ok) {
  throw new Error(`Manga update check failed: HTTP ${response.status} ${await response.text()}`);
}

console.log(await response.text());
