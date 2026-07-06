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
		int page,
		String title,
		String coverUrl,
		String url,
		Instant updatedAt) {
}
