# MangaWorldSync

MangaWorldSync is a small personal Spring Boot backend that saves and restores the last MangaWorld reader URL for each manga.

It does not scrape, download, cache, or proxy manga content. It only stores reading metadata:

- current URL
- manga id
- slug
- chapter id
- page
- optional browser title
- last update timestamp

Storage is handled by [FakeDBRepository](https://github.com/aiellolorenzo23/FakeDBRepository), using a local JSON file.

## How It Works

You deploy the backend somewhere reachable by both Android and PC, for example Railway.

Then:

1. Open a MangaWorld reader page on Android Brave.
2. Run the `MW Salva` bookmarklet.
3. The backend saves the current MangaWorld URL.
4. Open `/mw/list` or `/mw/go` from PC Chrome.
5. The backend redirects you to the saved MangaWorld page.

## Configuration

The app reads these environment variables:

| Variable | Required | Default | Description |
| --- | --- | --- | --- |
| `MANGA_SYNC_TOKEN` | Recommended | `cambia-questo-token-lungo-e-random` | Secret token used by all endpoints. Use a long random value. |
| `MANGA_SYNC_STORAGE_FILE` | Recommended in hosting | `./data/manga-progress.json` | FakeDB JSON file path. |
| `PORT` | Hosting only | `8080` | HTTP port, used by Railway and similar platforms. |

Allowed MangaWorld hosts are configured in `src/main/resources/application.yml`:

```yaml
manga-sync:
  allowed-hosts:
    - mangaworld.mx
    - www.mangaworld.mx
```

Redirects are only allowed to valid MangaWorld reader URLs, so the app is not an open redirect.

## Run Locally

PowerShell:

```powershell
$env:MANGA_SYNC_TOKEN="replace-with-a-long-random-token"
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\mvnw.cmd spring-boot:run
```

The app starts at:

```text
http://localhost:8080
```

Local data is saved by default to:

```text
./data/manga-progress.json
```

## Deploy On Railway Free

Railway works well for this app if you attach a persistent volume for the FakeDB JSON file.

1. Push this repository to GitHub.
2. In Railway, create a new project from the GitHub repo.
3. Open the service settings and generate a public domain.
4. Add a volume to the service.
5. Mount the volume at:

```text
/data
```

6. Set these Railway variables:

```text
MANGA_SYNC_TOKEN=replace-with-a-long-random-token
MANGA_SYNC_STORAGE_FILE=/data/manga-progress.json
```

Railway provides `PORT` automatically. The app uses it via:

```yaml
server:
  port: ${PORT:8080}
```

With this setup, FakeDB persists data in:

```text
/data/manga-progress.json
```

## Bookmarklet: Save Position

Create a bookmark called `MW Salva` in Brave Android and Chrome PC.

Use this as the bookmark URL, replacing the domain and token:

```js
javascript:(()=>{location.href='https://YOUR-RAILWAY-DOMAIN.up.railway.app/mw/save?token=YOUR_SECRET_TOKEN&url='+encodeURIComponent(location.href)+'&title='+encodeURIComponent(document.title)})();
```

On Android Brave:

1. Open a MangaWorld reader page.
2. Tap the address bar.
3. Type `MW Salva`.
4. Tap the bookmark suggestion.

The backend saves the page and redirects back to MangaWorld.

## Resume Reading

Open the saved list:

```text
https://YOUR-RAILWAY-DOMAIN.up.railway.app/mw/list?token=YOUR_SECRET_TOKEN
```

Or create a direct bookmark for a manga id:

```text
https://YOUR-RAILWAY-DOMAIN.up.railway.app/mw/go?token=YOUR_SECRET_TOKEN&mangaId=404
```

## Endpoints

### Save Progress

```http
GET /mw/save?token={TOKEN}&url={ENCODED_URL}&title={ENCODED_TITLE}
```

Returns `302` to the original MangaWorld URL when saved.

### Resume Progress

```http
GET /mw/go?token={TOKEN}&mangaId={MANGA_ID}
```

Returns `302` to the last saved URL for that manga.

### HTML List

```http
GET /mw/list?token={TOKEN}
```

Shows a simple table with saved progress and open links.

### JSON API

```http
GET /mw/api/progress?token={TOKEN}
```

Returns all saved progress entries as JSON.

## Tests

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\mvnw.cmd test
```

## Notes

- Use a strong token, especially if the service is public.
- Keep `MANGA_SYNC_STORAGE_FILE` on a persistent Railway volume.
- Do not run multiple replicas against the same FakeDB JSON file.
- This app stores only personal reading URLs and metadata.
