package com.mangaworldsync.service;

import com.mangaworldsync.config.MangaSyncProperties;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class MangaUrlParser {

	private static final Pattern READER_PATH = Pattern.compile("^/manga/(\\d+)/([^/]+)/read/([^/]+)/(\\d+)/?$");
	private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

	private final Set<String> allowedHosts;

	public MangaUrlParser(MangaSyncProperties properties) {
		this.allowedHosts = Set.copyOf(properties.allowedHosts());
	}

	public ParsedMangaUrl parse(String rawUrl) {
		URI uri = toUri(rawUrl);
		String scheme = normalized(uri.getScheme());
		String host = normalized(uri.getHost());

		if (!ALLOWED_SCHEMES.contains(scheme)) {
			throw new InvalidMangaUrlException("Only http and https MangaWorld URLs are allowed");
		}
		if (!allowedHosts.contains(host)) {
			throw new InvalidMangaUrlException("URL host is not allowed");
		}

		Matcher matcher = READER_PATH.matcher(uri.getPath());
		if (!matcher.matches()) {
			throw new InvalidMangaUrlException("URL is not a MangaWorld reader URL");
		}

		return new ParsedMangaUrl(
				matcher.group(1),
				matcher.group(2),
				matcher.group(3),
				Integer.parseInt(matcher.group(4)),
				uri.toString());
	}

	private static URI toUri(String rawUrl) {
		try {
			return new URI(rawUrl);
		}
		catch (URISyntaxException | NullPointerException ex) {
			throw new InvalidMangaUrlException("URL is not valid");
		}
	}

	private static String normalized(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT);
	}
}
