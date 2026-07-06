# MangaWorldSync MVP

## Summary
Creare un backend Java + Spring Boot 3.5.16 con Maven Wrapper, che salva e recupera l’ultima posizione MangaWorld tramite bookmarklet e redirect sicuri. Storage iniziale su file JSON locale tramite FakeDBRepository, senza scraping/download/cache di contenuti.

## Key Changes
- Scaffold Spring Boot con Java 21, `spring-boot-starter-web`, `fakedb-spring-boot-starter`, validation/configuration binding, Jackson e test.
- Configurazione `manga-sync` in `application.yml`:
    - `token` placeholder committabile, sovrascrivibile via env var.
    - `storage-file: ./data/manga-progress.json`, usato anche come `fakedb.path`.
    - host consentiti: `mangaworld.mx`, `www.mangaworld.mx`.
- `.gitignore` per `target/`, `.idea/`, `data/`, file env/locali e artefatti temporanei.
- Mantenere/committare `MangaWorldSync_handoff.md` come documento di progetto, salvo tua diversa preferenza.

## Implementation
- Implementare modello `MangaProgress` con `mangaId`, `slug`, `chapterId`, `page`, `title`, `url`, `updatedAt`.
- Implementare parser URL con `URI` + regex path `/manga/{mangaId}/{slug}/read/{chapterId}/{page}`.
- Validare host e schema `http/https`; i redirect useranno solo URL già validati per evitare open redirect.
- Implementare repository file-based tramite FakeDBRepository:
    - entity `MangaProgress` mappata su tabella `manga_progress`;
    - `mangaId` come `@FakeDBId`;
    - adapter applicativo `MangaProgressRepository` sopra `FakeDBTemplate`.
- Endpoint:
    - `GET /mw/save?token=&url=&title=` salva e redirect 302 all’URL originale.
    - `GET /mw/go?token=&mangaId=` redirect 302 alla posizione salvata o 404.
    - `GET /mw/list?token=` pagina HTML semplice con tabella e link “Apri”.
    - `GET /mw/api/progress?token=` JSON completo.
- Gestione errori leggibile:
    - `401` token errato/mancante;
    - `400` URL non valido o non MangaWorld reader;
    - `404` progresso mancante;
    - `500` solo per errori inattesi.

## Test Plan
- Test unitari per parsing URL valido/non valido, host ammessi/rifiutati e path non reader.
- Test repository per salvataggio, aggiornamento e rilettura tramite FakeDB.
- Test controller con MockMvc:
    - `/mw/save` salva e redirige;
    - `/mw/go` redirige se esiste;
    - token errato restituisce `401`;
    - URL esterno viene rifiutato;
    - `/mw/list` produce HTML con dati attesi;
    - `/mw/api/progress` restituisce JSON.
- Verifica finale con `.\mvnw.cmd test`.

## Assumptions
- Useremo Spring Boot `3.5.16`, scelta conservativa rispetto alla default più nuova di start.spring.io.
- Nessun Spring Security per l’MVP: token guard semplice nei controller/service.
- Il token reale non verrà committato; si userà override locale/env.
- Il primo commit includerà progetto, `.gitignore`, wrapper Maven e handoff.
