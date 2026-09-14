package com.mangaworldsync.service;

import com.mangaworldsync.model.MangaProgress;
import com.mangaworldsync.model.MangaStatus;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class MangaPageClient {

	private static final Pattern STATUS = Pattern.compile("[?&]status=([^&#]+)", Pattern.CASE_INSENSITIVE);

	public MangaSnapshot fetch(MangaProgress progress) throws IOException {
		URI reader = URI.create(progress.url());
		String mangaUrl = reader.getScheme() + "://" + reader.getAuthority() + "/manga/"
				+ progress.mangaId() + "/" + progress.slug();
		Document document = Jsoup.connect(mangaUrl)
				.userAgent("Mozilla/5.0 MangaWorldSync/1.0")
				.timeout(20_000)
				.get();
		return parse(document, mangaUrl, progress.mangaId(), progress.slug());
	}

	MangaSnapshot parse(Document document, String mangaUrl, String mangaId, String slug) {
		MangaStatus status = document.select("a[href*='/archive?status=']").stream()
				.map(link -> statusFromHref(link.attr("abs:href").isBlank() ? link.attr("href") : link.attr("abs:href")))
				.filter(java.util.Objects::nonNull)
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Stato del manga non trovato"));

		String quotedPath = Pattern.quote("/manga/" + mangaId + "/" + slug + "/read/");
		Pattern chapterPath = Pattern.compile(quotedPath + "([a-zA-Z0-9]{24})(?:/\\d+)?/?$");
		Map<String, RemoteChapter> chapters = new LinkedHashMap<>();
		for (Element link : document.select("a[href]")) {
			Element chapterName = link.selectFirst("span.d-inline-block");
			String label = (chapterName == null ? link.text() : chapterName.text()).trim();
			String lower = label.toLowerCase(Locale.ROOT);
			if (!lower.startsWith("capitolo") && !lower.startsWith("oneshot")) continue;
			String absolute = link.attr("abs:href");
			if (absolute.isBlank()) absolute = URI.create(mangaUrl).resolve(link.attr("href")).toString();
			Matcher matcher = chapterPath.matcher(URI.create(absolute).getPath());
			if (!matcher.find()) continue;
			String id = matcher.group(1);
			String firstPage = absolute.replaceFirst("/+$", "").replaceFirst("/\\d+$", "") + "/1";
			chapters.putIfAbsent(id, new RemoteChapter(id, label, firstPage));
		}
		if (chapters.isEmpty()) throw new IllegalStateException("Nessun capitolo trovato");
		return new MangaSnapshot(status, List.copyOf(chapters.values()));
	}

	private static MangaStatus statusFromHref(String href) {
		Matcher matcher = STATUS.matcher(href);
		if (!matcher.find()) return null;
		try {
			return MangaStatus.fromSlug(matcher.group(1));
		}
		catch (IllegalArgumentException ignored) {
			return null;
		}
	}
}
