package com.mangaworldsync.service;

import com.mangaworldsync.config.MangaSyncProperties;
import com.mangaworldsync.model.MangaProgress;
import com.mangaworldsync.repository.MangaProgressRepository;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MangaProgressService {

	private final MangaSyncProperties properties;
	private final MangaUrlParser parser;
	private final MangaProgressRepository repository;
	private final Clock clock;

	@Autowired
	public MangaProgressService(
			MangaSyncProperties properties,
			MangaUrlParser parser,
			MangaProgressRepository repository) {
		this(properties, parser, repository, Clock.systemUTC());
	}

	MangaProgressService(
			MangaSyncProperties properties,
			MangaUrlParser parser,
			MangaProgressRepository repository,
			Clock clock) {
		this.properties = properties;
		this.parser = parser;
		this.repository = repository;
		this.clock = clock;
	}

	public MangaProgress save(String token, String url, String title, String coverUrl, String volumeLabel, String chapterLabel) {
		validateToken(token);
		ParsedMangaUrl parsedUrl = parser.parse(url);
		MangaProgress existing = repository.findByMangaId(parsedUrl.mangaId()).orElse(null);
		MangaProgress progress = new MangaProgress(
				parsedUrl.mangaId(),
				parsedUrl.slug(),
				parsedUrl.chapterId(),
				normalizeLabel(volumeLabel),
				normalizeLabel(chapterLabel),
				parsedUrl.page(),
				title,
				normalizeCoverUrl(coverUrl),
				parsedUrl.url(),
				Instant.now(clock),
				existing == null ? null : existing.status(),
				existing == null ? null : existing.latestChapterId(),
				existing == null ? null : existing.latestChapterLabel(),
				existing == null ? null : existing.latestChapterUrl(),
				existing == null ? null : existing.lastCheckedAt(),
				existing == null ? null : existing.trackingError());
		return repository.save(progress);
	}

	private static String normalizeLabel(String label) {
		return label == null || label.isBlank() ? null : label.trim();
	}

	public MangaProgress findByMangaId(String token, String mangaId) {
		validateToken(token);
		return repository.findByMangaId(mangaId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No progress saved for mangaId " + mangaId));
	}

	public Collection<MangaProgress> findAll(String token) {
		validateToken(token);
		return repository.findAll();
	}

	public byte[] downloadDatabase(String token) {
		validateToken(token);
		try {
			return Files.readAllBytes(properties.storageFile());
		}
		catch (java.nio.file.NoSuchFileException ex) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Database file not found", ex);
		}
		catch (IOException ex) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read database file", ex);
		}
	}

	public void delete(String token, String mangaId) {
		validateToken(token);
		if (repository.findByMangaId(mangaId).isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No progress saved for mangaId " + mangaId);
		}
		repository.deleteByMangaId(mangaId);
	}

	private void validateToken(String token) {
		if (!properties.token().equals(token)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
		}
	}

	private static String normalizeCoverUrl(String coverUrl) {
		if (coverUrl == null || coverUrl.isBlank()) {
			return null;
		}
		try {
			URI uri = new URI(coverUrl.trim());
			String scheme = uri.getScheme();
			if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
				return null;
			}
			return uri.toString();
		}
		catch (URISyntaxException ex) {
			return null;
		}
	}
}
