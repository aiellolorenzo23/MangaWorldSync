# MangaWorldSync

MangaWorldSync is a small personal Spring Boot backend that saves and restores the last MangaWorld reader URL for each manga.

It does not scrape, download, cache, or proxy manga content. It only stores reading metadata:

- current URL
- manga id
- slug
- chapter id
- page
- optional browser title
- optional cover image URL
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

## Generate A Token

Use a long random token for `MANGA_SYNC_TOKEN`. On PowerShell:

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToHexString($bytes)
```

This prints a 64-character hexadecimal token. Use that value in Railway as:

```text
MANGA_SYNC_TOKEN=PASTE_THE_GENERATED_TOKEN_HERE
```

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
javascript:(()=>{const q=s=>document.querySelector(s);const img=q('meta[property="og:image"],meta[name="twitter:image"]')?.content||q('link[rel="image_src"]')?.href||q('.cover img,.thumb img,img[src*="cover"],img[src*="thumb"]')?.src||'';location.href='https://YOUR-RAILWAY-DOMAIN.up.railway.app/mw/save?token=YOUR_SECRET_TOKEN&url='+encodeURIComponent(location.href)+'&title='+encodeURIComponent(document.title)+'&coverUrl='+encodeURIComponent(img)})();
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

Optional query parameter:

```text
coverUrl={ENCODED_COVER_URL}
```

### Resume Progress

```http
GET /mw/go?token={TOKEN}&mangaId={MANGA_ID}
```

Returns `302` to the last saved URL for that manga.

### HTML List

```http
GET /mw/list?token={TOKEN}
```

Shows a responsive card list with saved progress, open links, and delete buttons.

### Delete Progress

```http
POST /mw/delete
Content-Type: application/x-www-form-urlencoded

token={TOKEN}&mangaId={MANGA_ID}
```

Deletes one saved progress entry and redirects back to `/mw/list`.

### JSON API

```http
GET /mw/api/progress?token={TOKEN}
```

Returns all saved progress entries as JSON.

## Sharing The App

This MVP has one library per deployed app/token. If someone else uses the same Railway URL and the same `MANGA_SYNC_TOKEN`, they can read, add, update, and delete entries in the same FakeDB JSON file.

To share the project without mixing libraries, the safest option is a separate deployment:

1. Fork or copy the repository.
2. Deploy it on another Railway project.
3. Use a different `MANGA_SYNC_TOKEN`.
4. Attach a separate volume and `MANGA_SYNC_STORAGE_FILE`.

Multi-user support with separate tokens/libraries can be added later, but it is not part of the current MVP.

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
