package com.mangaworldsync.model;

import io.github.aiellolorenzo23.fakedb.annotation.FakeDBId;
import io.github.aiellolorenzo23.fakedb.annotation.FakeDBTable;
import java.time.Instant;

@FakeDBTable(value = "manga_progress", schema = "main")
public record MangaProgress(
		@FakeDBId
		String mangaId,
		String slug,
		String chapterId,
		String volumeLabel,
		String chapterLabel,
		int page,
		String title,
		String coverUrl,
		String url,
		Instant updatedAt,
		MangaStatus status,
		String latestChapterId,
		String latestChapterLabel,
		String latestChapterUrl,
		Instant lastCheckedAt,
		String trackingError) {

	public MangaProgress(
			String mangaId, String slug, String chapterId, String volumeLabel, String chapterLabel,
			int page, String title, String coverUrl, String url, Instant updatedAt) {
		this(mangaId, slug, chapterId, volumeLabel, chapterLabel, page, title, coverUrl, url, updatedAt,
				null, null, null, null, null, null);
	}
}
