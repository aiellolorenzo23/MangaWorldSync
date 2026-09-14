package com.mangaworldsync.model;

import io.github.aiellolorenzo23.fakedb.annotation.FakeDBId;
import io.github.aiellolorenzo23.fakedb.annotation.FakeDBTable;
import java.time.Instant;

@FakeDBTable(value = "manga_notifications", schema = "main")
public record MangaNotification(
		@FakeDBId String id,
		String mangaId,
		String mangaTitle,
		String coverUrl,
		String chapterId,
		String chapterLabel,
		String chapterUrl,
		Instant discoveredAt,
		Instant readAt) {
}
