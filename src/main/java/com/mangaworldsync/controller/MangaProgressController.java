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
			@RequestParam(required = false) String coverUrl) {
		MangaProgress progress = service.save(token, url, title, coverUrl);
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

	private String renderList(String token, Collection<MangaProgress> progressItems) {
		StringBuilder html = new StringBuilder("""
				<!doctype html>
				<html lang="it">
				<head>
				  <meta charset="utf-8">
				  <meta name="viewport" content="width=device-width, initial-scale=1">
				  <title>mangaworld-sync</title>
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
				    .cover { width: 5rem; aspect-ratio: 2 / 3; object-fit: cover; border-radius: .35rem; background: var(--panel-strong); display: block; }
				    .cover-empty { width: 5rem; aspect-ratio: 2 / 3; border-radius: .35rem; background: var(--panel-strong); border: 1px solid var(--line); }
				    .details { min-width: 0; display: grid; gap: .35rem; }
				    .title { margin: 0; font-size: 1.05rem; line-height: 1.25; overflow-wrap: anywhere; }
				    .progress { color: var(--accent-strong); font-size: .95rem; }
				    .meta { display: flex; flex-wrap: wrap; gap: .45rem .9rem; color: var(--muted); font-size: .85rem; }
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
				      header { align-items: start; flex-direction: column; }
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
				.append("<div class=\"count\">").append(progressItems.size()).append(" salvati</div>")
				.append("""
				</header>
				<section class="library">
				""");

		for (MangaProgress progress : progressItems) {
			html.append("<article class=\"manga-card\">")
					.append(renderCover(progress))
					.append("<div class=\"details\">")
					.append("<h2 class=\"title\">").append(escape(displayTitle(progress))).append("</h2>")
					.append("<div class=\"progress\">").append(escape(displayProgress(progress))).append("</div>")
					.append("<div class=\"meta\"><span>").append(escape(progress.slug())).append("</span><span>Aggiornato ")
					.append(escape(UPDATED_AT_FORMATTER.format(progress.updatedAt()))).append("</span></div>")
					.append("</div>")
					.append("<div class=\"actions\">")
					.append("<a class=\"open\" href=\"/mw/go?token=").append(escape(token))
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
			html.append("<div class=\"empty\">Nessuna posizione salvata.</div>");
		}

		html.append("""
				</section>
				</main>
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

	private static String displayProgress(MangaProgress progress) {
		Matcher matcher = CHAPTER_PATTERN.matcher(progress.title() == null ? "" : progress.title());
		String chapter = matcher.find() ? "Capitolo " + matcher.group(1) : "Capitolo " + progress.chapterId();
		return chapter + " · Pagina " + progress.page();
	}

	private static String renderCover(MangaProgress progress) {
		if (progress.coverUrl() == null || progress.coverUrl().isBlank()) {
			return "<div class=\"cover-empty\"></div>";
		}
		return "<img class=\"cover\" src=\"" + escape(progress.coverUrl()) + "\" alt=\"Copertina "
				+ escape(displayTitle(progress)) + "\" loading=\"lazy\">";
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
