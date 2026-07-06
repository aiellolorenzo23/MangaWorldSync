package com.mangaworldsync.controller;

import com.mangaworldsync.model.MangaProgress;
import com.mangaworldsync.service.MangaProgressService;
import java.net.URI;
import java.util.Collection;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@RestController
@RequestMapping("/mw")
public class MangaProgressController {

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
				  <title>MangaWorldSync</title>
				  <style>
				    body { font-family: system-ui, sans-serif; margin: 2rem; color: #1f2937; }
				    table { border-collapse: collapse; width: 100%; }
				    th, td { border-bottom: 1px solid #d1d5db; padding: .6rem; text-align: left; vertical-align: middle; }
				    th { background: #f3f4f6; }
				    a { color: #0f766e; }
				    .cover-cell { width: 5rem; }
				    .cover { width: 3.5rem; height: 5rem; object-fit: cover; border-radius: .25rem; background: #e5e7eb; display: block; }
				    .cover-empty { width: 3.5rem; height: 5rem; border-radius: .25rem; background: #e5e7eb; }
				  </style>
				</head>
				<body>
				<h1>MangaWorldSync</h1>
				<table>
				<thead><tr><th>Copertina</th><th>Titolo</th><th>mangaId</th><th>slug</th><th>chapterId</th><th>page</th><th>Aggiornato</th><th></th></tr></thead>
				<tbody>
				""");

		for (MangaProgress progress : progressItems) {
			html.append("<tr>")
					.append("<td class=\"cover-cell\">").append(renderCover(progress)).append("</td>")
					.append("<td>").append(escape(displayTitle(progress))).append("</td>")
					.append("<td>").append(escape(progress.mangaId())).append("</td>")
					.append("<td>").append(escape(progress.slug())).append("</td>")
					.append("<td>").append(escape(progress.chapterId())).append("</td>")
					.append("<td>").append(progress.page()).append("</td>")
					.append("<td>").append(escape(progress.updatedAt().toString())).append("</td>")
					.append("<td><a href=\"/mw/go?token=").append(escape(token))
					.append("&amp;mangaId=").append(escape(progress.mangaId()))
					.append("\">Apri</a></td>")
					.append("</tr>");
		}

		if (progressItems.isEmpty()) {
			html.append("<tr><td colspan=\"8\">Nessuna posizione salvata.</td></tr>");
		}

		html.append("""
				</tbody>
				</table>
				</body>
				</html>
				""");
		return html.toString();
	}

	private static String displayTitle(MangaProgress progress) {
		return progress.title() == null || progress.title().isBlank() ? progress.slug() : progress.title();
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
}
