package com.mangaworldsync.controller;

import com.mangaworldsync.model.MangaProgress;
import com.mangaworldsync.service.MangaProgressService;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@RestController
@RequestMapping("/mw")
public class MangaProgressController {

	private static final Pattern CHAPTER_PATTERN = Pattern.compile("(?i)\\bcapitolo\\s+([\\w.-]+)");
	private static final Pattern ONESHOT_PATTERN = Pattern.compile("(?i)\\boneshot(?:\\s+(\\d+[\\w.-]*))?");
	private static final DateTimeFormatter UPDATED_AT_FORMATTER = DateTimeFormatter
			.ofPattern("dd/MM/yyyy HH:mm", Locale.ITALY)
			.withZone(ZoneId.of("Europe/Rome"));

	private final MangaProgressService service;

	public MangaProgressController(MangaProgressService service) {
		this.service = service;
	}

	@GetMapping("/save")
	public ResponseEntity<Void> save(
			@RequestParam String token,
			@RequestParam String url,
			@RequestParam(required = false) String title,
			@RequestParam(required = false) String coverUrl,
			@RequestParam(required = false) String volumeLabel,
			@RequestParam(required = false) String chapterLabel) {
		MangaProgress progress = service.save(token, url, title, coverUrl, volumeLabel, chapterLabel);
		return ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create(progress.url()))
				.build();
	}

	@GetMapping("/go")
	public ResponseEntity<Void> go(@RequestParam String token, @RequestParam String mangaId) {
		MangaProgress progress = service.findByMangaId(token, mangaId);
		return ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create(progress.url()))
				.build();
	}

	@GetMapping(value = { "", "/", "/list" }, produces = MediaType.TEXT_HTML_VALUE)
	public ResponseEntity<String> list(@RequestParam String token) {
		Collection<MangaProgress> progress = service.findAll(token);
		return ResponseEntity.ok(renderList(token, progress));
	}

	@PostMapping("/delete")
	public ResponseEntity<Void> delete(@RequestParam String token, @RequestParam String mangaId) {
		service.delete(token, mangaId);
		return ResponseEntity.status(HttpStatus.SEE_OTHER)
				.location(URI.create("/mw/list?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)))
				.build();
	}

	@GetMapping("/api/progress")
	public Collection<MangaProgress> apiProgress(@RequestParam String token) {
		return service.findAll(token);
	}

	@GetMapping(value = "/api/database/download", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<byte[]> downloadDatabase(@RequestParam String token) {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename("manga-progress.json")
						.build()
						.toString())
				.contentType(MediaType.APPLICATION_JSON)
				.body(service.downloadDatabase(token));
	}

	private String renderList(String token, Collection<MangaProgress> progressItems) {
		boolean hasAdultProgress = progressItems.stream().anyMatch(MangaProgressController::isAdult);
		StringBuilder html = new StringBuilder("""
				<!doctype html>
				<html lang="it">
				<head>
				  <meta charset="utf-8">
				  <meta name="viewport" content="width=device-width, initial-scale=1">
				  <title>mangaworld-sync</title>
				  <link rel="icon" href="/favicon.svg" type="image/svg+xml">
				  <link rel="shortcut icon" href="/favicon.svg" type="image/svg+xml">
				  <link rel="apple-touch-icon" href="/favicon.svg">
				  <link rel="manifest" href="/manifest.webmanifest">
				  <style>
				    :root {
				      color-scheme: dark;
				      --bg: #101413;
				      --panel: #171d1b;
				      --panel-strong: #1d2522;
				      --text: #f4f1ea;
				      --muted: #a9b2ad;
				      --line: #2a3430;
				      --accent: #4fd1b2;
				      --accent-strong: #7ce7cc;
				    }
				    * { box-sizing: border-box; }
				    body {
				      margin: 0;
				      min-height: 100vh;
				      background: var(--bg);
				      color: var(--text);
				      font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
				    }
				    main { width: min(1120px, 100%); margin: 0 auto; padding: 2rem 1rem 3rem; }
				    header { display: flex; align-items: end; justify-content: space-between; gap: 1rem; margin-bottom: 1.25rem; }
				    .brand { display: block; width: min(30rem, 84vw); height: auto; }
				    .brand-text { fill: #3d5a80; font: 900 82px Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; letter-spacing: 0; }
				    .count { color: var(--muted); font-size: .95rem; white-space: nowrap; }
				    .header-actions { display: flex; align-items: center; gap: .75rem; position: relative; }
				    .bell-button {
				      position: relative; width: 2.75rem; height: 2.75rem; display: grid; place-items: center;
				      border: 1px solid var(--line); border-radius: 50%; background: var(--panel); color: var(--text);
				      cursor: pointer; transition: border-color .15s, background .15s, transform .15s;
				    }
				    .bell-button:hover { border-color: var(--accent); background: var(--panel-strong); transform: translateY(-1px); }
				    .bell-button svg { width: 1.25rem; height: 1.25rem; fill: none; stroke: currentColor; stroke-width: 1.8; }
				    .bell-count {
				      position: absolute; top: -.35rem; right: -.25rem; min-width: 1.25rem; height: 1.25rem; padding: 0 .3rem;
				      display: grid; place-items: center; border: 2px solid var(--bg); border-radius: 999px;
				      background: #ee6c4d; color: white; font-size: .67rem; font-weight: 850;
				    }
				    .bell-count[hidden], .notification-panel[hidden] { display: none; }
				    .notification-panel {
				      position: absolute; z-index: 20; top: 3.25rem; right: 0; width: min(25rem, calc(100vw - 1.5rem));
				      max-height: min(34rem, 75vh); display: flex; flex-direction: column; overflow: hidden;
				      border: 1px solid #394640; border-radius: .7rem; background: #171d1bf7;
				      box-shadow: 0 18px 45px rgb(0 0 0 / .42); backdrop-filter: blur(12px);
				    }
				    .notification-head { display: flex; align-items: center; justify-content: space-between; gap: .75rem; padding: .9rem 1rem; border-bottom: 1px solid var(--line); }
				    .notification-head h2 { margin: 0; font-size: 1rem; }
				    .text-button { padding: .25rem 0; border: 0; background: none; color: var(--accent-strong); font: inherit; font-size: .8rem; cursor: pointer; }
				    .notification-list { overflow: auto; }
				    .notification-item { display: grid; grid-template-columns: 2.65rem 1fr; gap: .75rem; padding: .8rem 1rem; color: var(--text); text-decoration: none; border-bottom: 1px solid var(--line); }
				    .notification-item:hover { background: var(--panel-strong); }
				    .notification-item.unread { background: #16302a80; }
				    .notification-cover { width: 2.65rem; aspect-ratio: 2 / 3; object-fit: cover; border-radius: .25rem; background: var(--panel-strong); }
				    .notification-copy { min-width: 0; display: grid; gap: .18rem; align-content: center; }
				    .notification-title { font-size: .88rem; font-weight: 750; overflow-wrap: anywhere; }
				    .notification-chapter { color: var(--accent-strong); font-size: .8rem; overflow-wrap: anywhere; }
				    .notification-date { color: var(--muted); font-size: .72rem; }
				    .notification-empty { padding: 1.4rem 1rem; color: var(--muted); text-align: center; font-size: .9rem; }
				    .push-button { margin: .75rem; min-height: 2.4rem; border: 1px solid #38506f; border-radius: .45rem; background: #1b2738; color: #b9d2f5; font: inherit; font-weight: 700; cursor: pointer; }
				    .toolbar {
				      display: grid;
				      grid-template-columns: minmax(0, 1fr) 13rem auto;
				      gap: .75rem;
				      align-items: center;
				      margin-bottom: 1rem;
				    }
				    .search, .sort {
				      min-height: 2.75rem;
				      width: 100%;
				      border: 1px solid var(--line);
				      border-radius: .45rem;
				      background: var(--panel);
				      color: var(--text);
				      font: inherit;
				      outline: none;
				    }
				    .search { padding: 0 .9rem; }
				    .sort { padding: 0 .7rem; cursor: pointer; }
				    .search:focus, .sort:focus { border-color: var(--accent); }
				    .nsfw-toggle {
				      min-height: 2.75rem;
				      display: inline-flex;
				      align-items: center;
				      gap: .55rem;
				      padding: 0 .8rem;
				      border: 1px solid var(--line);
				      border-radius: .45rem;
				      background: var(--panel);
				      color: var(--muted);
				      white-space: nowrap;
				      cursor: pointer;
				      user-select: none;
				    }
				    .nsfw-toggle input {
				      width: 1.05rem;
				      height: 1.05rem;
				      accent-color: var(--accent);
				      cursor: pointer;
				    }
				    .nsfw-toggle[hidden] { display: none; }
				    .library { display: grid; gap: .85rem; }
				    .manga-card {
				      display: grid;
				      grid-template-columns: 5rem minmax(0, 1fr) auto;
				      gap: 1rem;
				      align-items: center;
				      padding: .8rem;
				      border: 1px solid var(--line);
				      border-radius: .5rem;
				      background: var(--panel);
				    }
				    .manga-card[hidden], .empty[hidden] { display: none; }
				    .cover { width: 5rem; aspect-ratio: 2 / 3; object-fit: cover; border-radius: .35rem; background: var(--panel-strong); display: block; }
				    .cover-empty { width: 5rem; aspect-ratio: 2 / 3; border-radius: .35rem; background: var(--panel-strong); border: 1px solid var(--line); }
				    .details { min-width: 0; display: grid; gap: .35rem; }
				    .title { margin: 0; font-size: 1.05rem; line-height: 1.25; overflow-wrap: anywhere; }
				    .progress { display: flex; flex-wrap: wrap; gap: .4rem; align-items: center; }
				    .progress-badge {
				      display: inline-flex;
				      align-items: center;
				      min-height: 1.65rem;
				      padding: .2rem .65rem;
				      border: 1px solid transparent;
				      border-radius: 999px;
				      font-size: .78rem;
				      font-weight: 750;
				      line-height: 1.35;
				      max-width: 100%;
				      overflow-wrap: anywhere;
				      letter-spacing: .015em;
				      box-shadow: inset 0 1px 0 rgb(255 255 255 / .06), 0 2px 8px rgb(0 0 0 / .16);
				    }
				    .progress-volume { background: #2c2519; border-color: #675332; color: #f3ca86; }
				    .progress-chapter { background: #16302a; border-color: #2d6759; color: var(--accent-strong); }
				    .progress-page { background: #1b2738; border-color: #38506f; color: #b9d2f5; }
				    .meta { display: flex; flex-wrap: wrap; gap: .45rem .9rem; color: var(--muted); font-size: .85rem; }
				    .badge {
				      width: fit-content;
				      padding: .15rem .45rem;
				      border-radius: .35rem;
				      background: #7f1d1d;
				      color: #fecaca;
				      font-size: .75rem;
				      font-weight: 800;
				    }
				    .actions { display: grid; gap: .45rem; }
				    .delete-form { margin: 0; }
				    .open, .delete {
				      display: inline-flex;
				      align-items: center;
				      justify-content: center;
				      min-height: 2.5rem;
				      padding: 0 1rem;
				      border-radius: .4rem;
				      border: 0;
				      font: inherit;
				      background: var(--accent);
				      color: #061411;
				      font-weight: 700;
				      text-decoration: none;
				      cursor: pointer;
				    }
				    .delete {
				      width: 100%;
				      background: #dc2626;
				      color: #ffffff;
				      border: 1px solid #ef4444;
				    }
				    .empty {
				      padding: 1rem;
				      border: 1px solid var(--line);
				      border-radius: .5rem;
				      background: var(--panel);
				      color: var(--muted);
				    }
				    @media (max-width: 640px) {
				      main { padding: 1.25rem .75rem 2rem; }
				      header { align-items: start; }
				      .brand { width: min(25rem, calc(100vw - 5rem)); }
				      .count { display: none; }
				      .notification-panel { position: fixed; top: 4.5rem; right: .75rem; }
				      .toolbar { grid-template-columns: 1fr; }
				      .manga-card {
				        grid-template-columns: 4.5rem minmax(0, 1fr);
				        gap: .8rem;
				      }
				      .cover, .cover-empty { width: 4.5rem; }
				      .actions {
				        grid-column: 1 / -1;
				        grid-template-columns: 1fr 1fr;
				      }
				      .open, .delete {
				        width: 100%;
				      }
				    }
				  </style>
				</head>
				<body>
				<main>
				<header>
				""");

		html.append(renderBrandLogo())
				.append("<div class=\"header-actions\"><div class=\"count\"><span id=\"visible-count\">").append(progressItems.size())
				.append("</span> / <span id=\"total-count\">")
				.append(progressItems.size()).append("</span> salvati</div>")
				.append("""
				<button class="bell-button" id="bell-button" type="button" aria-label="Notifiche" aria-expanded="false">
				  <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4"/></svg>
				  <span class="bell-count" id="bell-count" hidden>0</span>
				</button>
				<aside class="notification-panel" id="notification-panel" hidden>
				  <div class="notification-head"><h2>Nuovi capitoli</h2><button class="text-button" id="read-all" type="button">Segna tutte come lette</button></div>
				  <div class="notification-list" id="notification-list"><div class="notification-empty">Nessuna notifica.</div></div>
				  <button class="push-button" id="push-button" type="button" hidden>Attiva notifiche sul telefono</button>
				</aside></div>
				</header>
				<section class="toolbar" aria-label="Filtri libreria">
				  <input class="search" id="search" type="search" placeholder="Cerca manga" autocomplete="off">
				  <select class="sort" id="sort">
				    <option value="updated-desc">Aggiornati di recente</option>
				    <option value="updated-asc">Aggiornati meno recenti</option>
				    <option value="title-asc">Titolo A-Z</option>
				    <option value="title-desc">Titolo Z-A</option>
				    <option value="page-desc">Pagina piu alta</option>
				    <option value="page-asc">Pagina piu bassa</option>
				  </select>
				  <label class="nsfw-toggle" id='nsfw-toggle'""")
				.append(hasAdultProgress ? "" : " hidden")
				.append("><input id=\"show-nsfw\" type=\"checkbox\">Mostra NSFW</label>")
				.append("""
				</section>
				<section class="library" id="library">
				""");

		for (MangaProgress progress : progressItems) {
			boolean adult = isAdult(progress);
			html.append("<article class=\"manga-card\" data-title=\"").append(escape(displayTitle(progress)))
					.append("\" data-slug=\"").append(escape(progress.slug()))
					.append("\" data-updated=\"").append(progress.updatedAt().toEpochMilli())
					.append("\" data-page=\"").append(progress.page())
					.append("\" data-chapter=\"").append(escape(chapterLabel(progress)))
					.append("\" data-adult=\"").append(adult)
					.append("\">")
					.append(renderCover(progress))
					.append("<div class=\"details\">")
					.append("<h2 class=\"title\">").append(escape(displayTitle(progress))).append("</h2>")
					.append(renderProgressBadges(progress))
					.append("<div class=\"meta\"><span>").append(escape(progress.slug())).append("</span><span>Aggiornato ")
					.append(escape(UPDATED_AT_FORMATTER.format(progress.updatedAt()))).append("</span></div>");
			if (adult) {
				html.append("<span class=\"badge\">NSFW</span>");
			}
			html.append("</div>")
					.append("<div class=\"actions\">")
					.append("<a class=\"open\" target=\"_blank\" rel=\"noopener\" href=\"/mw/go?token=").append(escape(token))
					.append("&amp;mangaId=").append(escape(progress.mangaId()))
					.append("\">Apri</a>")
					.append("<form class=\"delete-form\" method=\"post\" action=\"/mw/delete\" onsubmit=\"return confirm('Eliminare ")
					.append(escapeJs(displayTitle(progress)))
					.append(" dalla lista?')\">")
					.append("<input type=\"hidden\" name=\"token\" value=\"").append(escape(token)).append("\">")
					.append("<input type=\"hidden\" name=\"mangaId\" value=\"").append(escape(progress.mangaId())).append("\">")
					.append("<button class=\"delete\" type=\"submit\">Elimina</button>")
					.append("</form>")
					.append("</div>")
					.append("</article>");
		}

		if (progressItems.isEmpty()) {
			html.append("<div class=\"empty\" id=\"empty-message\">Nessuna posizione salvata.</div>");
		}
		else {
			html.append("<div class=\"empty\" id=\"empty-message\" hidden>Nessun manga trovato.</div>");
		}

		html.append("""
				</section>
				</main>
				<script>
				(() => {
				  const library = document.querySelector('#library');
				  const search = document.querySelector('#search');
				  const sort = document.querySelector('#sort');
				  const nsfwToggle = document.querySelector('#nsfw-toggle');
				  const showNsfw = document.querySelector('#show-nsfw');
				  const visibleCount = document.querySelector('#visible-count');
				  const totalCount = document.querySelector('#total-count');
				  const emptyMessage = document.querySelector('#empty-message');
				  const token = new URLSearchParams(window.location.search).get('token') || '';
				  const bellButton = document.querySelector('#bell-button');
				  const bellCount = document.querySelector('#bell-count');
				  const notificationPanel = document.querySelector('#notification-panel');
				  const notificationList = document.querySelector('#notification-list');
				  const readAllButton = document.querySelector('#read-all');
				  const pushButton = document.querySelector('#push-button');
				  let notifications = [];
				  let cards = Array.from(document.querySelectorAll('.manga-card'));
				  let lastRefreshAt = Date.now();
				  const compareText = (a, b) => a.localeCompare(b, 'it', { sensitivity: 'base' });
				  const htmlEscapeMap = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
				  const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, char => htmlEscapeMap[char]);
				  const api = path => `${path}${path.includes('?') ? '&' : '?'}token=${encodeURIComponent(token)}`;
				  function renderNotifications() {
				    const unread = notifications.filter(item => !item.readAt).length;
				    bellCount.textContent = unread > 99 ? '99+' : String(unread);
				    bellCount.hidden = unread === 0;
				    readAllButton.hidden = unread === 0;
				    if (!notifications.length) {
				      notificationList.innerHTML = '<div class="notification-empty">Nessun nuovo capitolo.</div>';
				      return;
				    }
				    notificationList.innerHTML = notifications.map(item => {
				      const cover = item.coverUrl
				        ? `<img class="notification-cover" src="${escapeHtml(item.coverUrl)}" alt="" loading="lazy">`
				        : '<div class="notification-cover"></div>';
				      const discoveredAt = typeof item.discoveredAt === 'number' ? item.discoveredAt * 1000 : item.discoveredAt;
				      const date = new Date(discoveredAt).toLocaleString('it-IT', { dateStyle: 'short', timeStyle: 'short' });
				      return `<a class="notification-item ${item.readAt ? '' : 'unread'}" data-notification-id="${escapeHtml(item.id)}" href="${escapeHtml(item.chapterUrl)}" target="_blank" rel="noopener">
				        ${cover}<span class="notification-copy"><span class="notification-title">${escapeHtml(item.mangaTitle)}</span>
				        <span class="notification-chapter">${escapeHtml(item.chapterLabel)}</span><span class="notification-date">${escapeHtml(date)}</span></span></a>`;
				    }).join('');
				  }
				  async function refreshNotifications() {
				    try {
				      const response = await fetch(api('/mw/api/notifications'), { cache: 'no-store' });
				      if (response.ok) { notifications = await response.json(); renderNotifications(); }
				    } catch { /* Retry when the page becomes visible. */ }
				  }
				  async function markNotificationRead(id) {
				    const item = notifications.find(entry => entry.id === id);
				    if (item && !item.readAt) { item.readAt = new Date().toISOString(); renderNotifications(); }
				    try { await fetch(api(`/mw/api/notifications/read?id=${encodeURIComponent(id)}`), { method: 'POST' }); } catch {}
				  }
				  function urlBase64ToUint8Array(value) {
				    const padding = '='.repeat((4 - value.length % 4) % 4);
				    const base64 = (value + padding).replace(/-/g, '+').replace(/_/g, '/');
				    return Uint8Array.from(atob(base64), char => char.charCodeAt(0));
				  }
				  async function savePushSubscription(subscription) {
				    const response = await fetch(api('/mw/api/push/subscribe'), {
				      method: 'POST',
				      headers: { 'Content-Type': 'application/json' },
				      body: JSON.stringify(subscription)
				    });
				    if (!response.ok) throw new Error(`Registrazione non riuscita (${response.status})`);
				  }
				  async function updatePushButton() {
				    if (!('serviceWorker' in navigator) || !('PushManager' in window) || !('Notification' in window)) return;
				    try {
				      const configResponse = await fetch(api('/mw/api/push/config'));
				      if (!configResponse.ok) return;
				      const config = await configResponse.json();
				      if (!config.enabled) return;
				      const registration = await navigator.serviceWorker.register('/service-worker.js');
				      const subscription = await registration.pushManager.getSubscription();
				      if (subscription) await savePushSubscription(subscription);
				      pushButton.hidden = false;
				      pushButton.dataset.publicKey = config.publicKey;
				      pushButton.textContent = subscription ? 'Disattiva notifiche sul telefono' : 'Attiva notifiche sul telefono';
				    } catch { /* Push remains optional. */ }
				  }
				  async function togglePush() {
				    pushButton.disabled = true;
				    try {
				      const registration = await navigator.serviceWorker.ready;
				      let subscription = await registration.pushManager.getSubscription();
				      if (subscription) {
				        await fetch(api(`/mw/api/push/unsubscribe?endpoint=${encodeURIComponent(subscription.endpoint)}`), { method: 'POST' });
				        await subscription.unsubscribe();
				      } else {
				        const permission = await Notification.requestPermission();
				        if (permission !== 'granted') throw new Error('Permesso notifiche non concesso');
				        subscription = await registration.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: urlBase64ToUint8Array(pushButton.dataset.publicKey) });
				        await savePushSubscription(subscription);
				      }
				      await updatePushButton();
				    } catch (error) { alert(error.message || 'Impossibile modificare le notifiche push.'); }
				    finally { pushButton.disabled = false; }
				  }
				  const displayTitle = item => {
				    const title = item.title && item.title.trim() ? item.title : item.slug;
				    return title
				      .replace(/\\s+-\\s*MangaWorld$/i, '')
				      .replace(/\\s+Capitolo\\s+[\\w.-]+.*$/i, '')
				      .trim();
				  };
				  const chapterLabel = item => {
				    if (item.chapterLabel && item.chapterLabel.trim()) return item.chapterLabel.trim();
				    const match = String(item.title || '').match(/\\bcapitolo\\s+([\\w.-]+)/i);
				    if (match) return `Capitolo ${match[1]}`;
				    const oneshotMatch = String(item.title || '').match(/\\boneshot(?:\\s+(\\d+[\\w.-]*))?/i);
				    if (oneshotMatch) return oneshotMatch[1] ? `Oneshot ${oneshotMatch[1]}` : 'Oneshot';
				    return `Capitolo ${item.chapterId}`;
				  };
				  const progressBadges = item => {
				    const chapter = chapterLabel(item);
				    const volume = String(item.volumeLabel || '').trim();
				    const volumeBadge = volume
				      ? `<span class="progress-badge progress-volume">${escapeHtml(volume)}</span>`
				      : '';
				    const chapterBadge = !volume || volume.localeCompare(chapter, 'it', { sensitivity: 'base' }) !== 0
				      ? `<span class="progress-badge progress-chapter">${escapeHtml(chapter)}</span>`
				      : '';
				    return `<div class="progress">${volumeBadge}${chapterBadge}<span class="progress-badge progress-page">Pagina ${escapeHtml(item.page)}</span></div>`;
				  };
				  const itemDate = item => {
				    if (typeof item.updatedAt === 'number') return new Date(item.updatedAt * 1000);
				    if (Array.isArray(item.updatedAt)) return new Date(Date.UTC(
				      item.updatedAt[0],
				      (item.updatedAt[1] || 1) - 1,
				      item.updatedAt[2] || 1,
				      item.updatedAt[3] || 0,
				      item.updatedAt[4] || 0,
				      item.updatedAt[5] || 0,
				      Math.floor((item.updatedAt[6] || 0) / 1000000)
				    ));
				    return new Date(item.updatedAt);
				  };
				  const isAdult = item => {
				    try {
				      return new URL(item.url).hostname.toLocaleLowerCase('it').includes('mangaworldadult.');
				    } catch {
				      return false;
				    }
				  };
				  const formatUpdatedAt = item => new Intl.DateTimeFormat('it-IT', {
				    day: '2-digit',
				    month: '2-digit',
				    year: 'numeric',
				    hour: '2-digit',
				    minute: '2-digit'
				  }).format(itemDate(item));
				  const sorters = {
				    'updated-desc': (a, b) => Number(b.dataset.updated) - Number(a.dataset.updated),
				    'updated-asc': (a, b) => Number(a.dataset.updated) - Number(b.dataset.updated),
				    'title-asc': (a, b) => compareText(a.dataset.title, b.dataset.title),
				    'title-desc': (a, b) => compareText(b.dataset.title, a.dataset.title),
				    'page-desc': (a, b) => Number(b.dataset.page) - Number(a.dataset.page),
				    'page-asc': (a, b) => Number(a.dataset.page) - Number(b.dataset.page)
				  };
				  function createCard(item) {
				    const title = displayTitle(item);
				    const chapter = chapterLabel(item);
				    const adult = isAdult(item);
				    const updated = itemDate(item).getTime() || 0;
				    const article = document.createElement('article');
				    article.className = 'manga-card';
				    article.dataset.title = title;
				    article.dataset.slug = item.slug || '';
				    article.dataset.updated = String(updated);
				    article.dataset.page = String(item.page ?? 0);
				    article.dataset.chapter = chapter || '';
				    article.dataset.adult = String(adult);
				    const cover = item.coverUrl
				      ? `<img class="cover" src="${escapeHtml(item.coverUrl)}" alt="Copertina ${escapeHtml(title)}" loading="lazy">`
				      : '<div class="cover-empty"></div>';
				    const badge = adult ? '<span class="badge">NSFW</span>' : '';
				    article.innerHTML = `
				      ${cover}
				      <div class="details">
				        <h2 class="title">${escapeHtml(title)}</h2>
				        ${progressBadges(item)}
				        <div class="meta"><span>${escapeHtml(item.slug)}</span><span>Aggiornato ${escapeHtml(formatUpdatedAt(item))}</span></div>
				        ${badge}
				      </div>
				      <div class="actions">
				        <a class="open" target="_blank" rel="noopener" href="/mw/go?token=${encodeURIComponent(token)}&amp;mangaId=${encodeURIComponent(item.mangaId)}">Apri</a>
				        <form class="delete-form" method="post" action="/mw/delete">
				          <input type="hidden" name="token" value="${escapeHtml(token)}">
				          <input type="hidden" name="mangaId" value="${escapeHtml(item.mangaId)}">
				          <button class="delete" type="submit">Elimina</button>
				        </form>
				      </div>`;
				    article.querySelector('.delete-form').addEventListener('submit', event => {
				      if (!confirm(`Eliminare ${title} dalla lista?`)) event.preventDefault();
				    });
				    return article;
				  }
				  function renderCards(items) {
				    cards.forEach(card => card.remove());
				    cards = items.map(createCard);
				    cards.forEach(card => library.insertBefore(card, emptyMessage));
				    totalCount.textContent = String(cards.length);
				    updateNsfwToggle();
				    applyFilters();
				  }
				  function updateNsfwToggle() {
				    const hasAdult = cards.some(card => card.dataset.adult === 'true');
				    nsfwToggle.hidden = !hasAdult;
				    if (!hasAdult) showNsfw.checked = false;
				  }
				  async function refreshLibrary() {
				    try {
				      const response = await fetch(`/mw/api/progress?token=${encodeURIComponent(token)}`, {
				        headers: { Accept: 'application/json' },
				        cache: 'no-store'
				      });
				      if (!response.ok) return;
				      renderCards(await response.json());
				      lastRefreshAt = Date.now();
				    } catch {
				      // The next tab focus will try again.
				    }
				  }
				  function applyFilters() {
				    const query = search.value.trim().toLocaleLowerCase('it');
				    const nsfwEnabled = showNsfw.checked;
				    let shown = 0;
				    cards.sort(sorters[sort.value] || sorters['updated-desc']).forEach(card => {
				      const text = `${card.dataset.title} ${card.dataset.slug}`.toLocaleLowerCase('it');
				      const matchesSearch = !query || text.includes(query);
				      const matchesNsfw = nsfwEnabled || card.dataset.adult !== 'true';
				      const visible = matchesSearch && matchesNsfw;
				      card.hidden = !visible;
				      if (visible) shown += 1;
				      library.insertBefore(card, emptyMessage);
				    });
				    visibleCount.textContent = shown;
				    emptyMessage.hidden = cards.length === 0 ? false : shown !== 0;
				    if (cards.length === 0) emptyMessage.textContent = 'Nessuna posizione salvata.';
				    else emptyMessage.textContent = 'Nessun manga trovato.';
				  }
				  search.addEventListener('input', applyFilters);
				  sort.addEventListener('change', applyFilters);
				  showNsfw.addEventListener('change', applyFilters);
				  bellButton.addEventListener('click', () => {
				    notificationPanel.hidden = !notificationPanel.hidden;
				    bellButton.setAttribute('aria-expanded', String(!notificationPanel.hidden));
				  });
				  document.addEventListener('click', event => {
				    if (!notificationPanel.hidden && !event.target.closest('.header-actions')) {
				      notificationPanel.hidden = true;
				      bellButton.setAttribute('aria-expanded', 'false');
				    }
				  });
				  notificationList.addEventListener('click', event => {
				    const link = event.target.closest('[data-notification-id]');
				    if (link) markNotificationRead(link.dataset.notificationId);
				  });
				  readAllButton.addEventListener('click', async () => {
				    notifications.forEach(item => { if (!item.readAt) item.readAt = new Date().toISOString(); });
				    renderNotifications();
				    try { await fetch(api('/mw/api/notifications/read-all'), { method: 'POST' }); } catch {}
				  });
				  pushButton.addEventListener('click', togglePush);
				  document.addEventListener('visibilitychange', () => {
				    if (!document.hidden && Date.now() - lastRefreshAt > 1000) { refreshLibrary(); refreshNotifications(); }
				  });
				  window.addEventListener('pageshow', event => {
				    if (event.persisted) refreshLibrary();
				  });
				  updateNsfwToggle();
				  applyFilters();
				  refreshNotifications();
				  updatePushButton();
				})();
				</script>
				</body>
				</html>
				""");
		return html.toString();
	}

	private static String renderBrandLogo() {
		return """
				<svg class="brand" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1040 120" role="img" aria-label="mangaworld-sync">
				  <title>mangaworld-sync</title>
				  <text class="brand-text" x="0" y="84">mangaworld-sync</text>
				  <g transform="translate(805 2) scale(.34) translate(-2165 0)">
				    <path fill="#ee6c4d" d="M2348.77,323.73h0a27,27,0,0,1-36.94-9.9L2219.14,153.3v147a27,27,0,1,1-54.09,0V54.71q0-.73,0-1.45A27,27,0,0,1,2178.61,29h0a27,27,0,0,1,36.94,9.9L2358.67,286.8A27,27,0,0,1,2348.77,323.73Z"/>
				    <rect fill="#ee6c4d" x="2333.94" y="6.22" width="54.09" height="340.31" rx="27.04" ry="27.04" transform="translate(228.12 1204.12) rotate(-30)"/>
				    <rect fill="#3d5a80" x="2431.29" y="6.22" width="54.09" height="340.31" rx="27.04" ry="27.04" transform="translate(241.16 1252.8) rotate(-30)"/>
				    <rect fill="#3d5a80" x="2528.46" y="6.22" width="54.09" height="340.31" rx="27.04" ry="27.04" transform="translate(254.18 1301.38) rotate(-30)"/>
				    <rect fill="#3d5a80" x="2625.55" y="6.22" width="54.09" height="340.31" rx="27.04" ry="27.04" transform="translate(267.19 1349.93) rotate(-30)"/>
				  </g>
				</svg>
				""";
	}

	private static String displayTitle(MangaProgress progress) {
		String title = progress.title() == null || progress.title().isBlank() ? progress.slug() : progress.title();
		return title
				.replaceFirst("(?i)\\s+-\\s*MangaWorld$", "")
				.replaceFirst("(?i)\\s+Capitolo\\s+[\\w.-]+.*$", "")
				.trim();
	}

	private static String renderProgressBadges(MangaProgress progress) {
		String chapter = chapterLabel(progress);
		String volume = progress.volumeLabel() == null ? "" : progress.volumeLabel().trim();
		StringBuilder html = new StringBuilder("<div class=\"progress\">");
		if (!volume.isEmpty()) {
			html.append("<span class=\"progress-badge progress-volume\">").append(escape(volume)).append("</span>");
		}
		if (volume.isEmpty() || !volume.equalsIgnoreCase(chapter)) {
			html.append("<span class=\"progress-badge progress-chapter\">").append(escape(chapter)).append("</span>");
		}
		return html.append("<span class=\"progress-badge progress-page\">Pagina ")
				.append(progress.page())
				.append("</span></div>")
				.toString();
	}

	private static String chapterLabel(MangaProgress progress) {
		if (progress.chapterLabel() != null && !progress.chapterLabel().isBlank()) {
			return progress.chapterLabel().trim();
		}
		String title = progress.title() == null ? "" : progress.title();
		Matcher matcher = CHAPTER_PATTERN.matcher(title);
		if (matcher.find()) {
			return "Capitolo " + matcher.group(1);
		}
		Matcher oneshotMatcher = ONESHOT_PATTERN.matcher(title);
		if (oneshotMatcher.find()) {
			String number = oneshotMatcher.group(1);
			return number == null || number.isBlank() ? "Oneshot" : "Oneshot " + number;
		}
		return "Capitolo " + progress.chapterId();
	}

	private static String renderCover(MangaProgress progress) {
		if (progress.coverUrl() == null || progress.coverUrl().isBlank()) {
			return "<div class=\"cover-empty\"></div>";
		}
		return "<img class=\"cover\" src=\"" + escape(progress.coverUrl()) + "\" alt=\"Copertina "
				+ escape(displayTitle(progress)) + "\" loading=\"lazy\">";
	}

	private static boolean isAdult(MangaProgress progress) {
		try {
			String host = URI.create(progress.url()).getHost();
			return host != null && host.toLowerCase(Locale.ROOT).contains("mangaworldadult.");
		}
		catch (IllegalArgumentException ex) {
			return false;
		}
	}

	private static String escape(String value) {
		return HtmlUtils.htmlEscape(value == null ? "" : value);
	}

	private static String escapeJs(String value) {
		String escaped = (value == null ? "" : value)
				.replace("\\", "\\\\")
				.replace("'", "\\'")
				.replace("\r", " ")
				.replace("\n", " ");
		return escape(escaped);
	}
}
