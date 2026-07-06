package com.mangaworldsync.service;

public record ParsedMangaUrl(
		String mangaId,
		String slug,
		String chapterId,
		int page,
		String url) {
}
