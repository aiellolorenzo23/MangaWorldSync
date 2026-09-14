package com.mangaworldsync.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.mangaworldsync.model.MangaStatus;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

class MangaPageClientTests {
	private final MangaPageClient client = new MangaPageClient();

	@Test
	void parsesStatusAndNewestFirstChapterLinks() {
		String html = """
				<a href="/archive?status=ongoing">In corso</a>
				<a href="/manga/2423/four-knights-of-the-apocalypse/read/6aa59825ed61150c171e0956">Capitolo 252 - Futuro</a>
				<a href="/manga/2423/four-knights-of-the-apocalypse/read/123456789012345678901234">Capitolo 251</a>
				<a href="/manga/999/altro/read/aaaaaaaaaaaaaaaaaaaaaaaa">Capitolo 99</a>
				""";
		MangaSnapshot result = client.parse(Jsoup.parse(html, "https://www.mangaworld.mx"),
				"https://www.mangaworld.mx/manga/2423/four-knights-of-the-apocalypse", "2423",
				"four-knights-of-the-apocalypse");

		assertThat(result.status()).isEqualTo(MangaStatus.ONGOING);
		assertThat(result.chapters()).extracting(RemoteChapter::id)
				.containsExactly("6aa59825ed61150c171e0956", "123456789012345678901234");
		assertThat(result.chapters().getFirst().url()).endsWith("/6aa59825ed61150c171e0956/1");
	}

	@Test
	void mapsAllKnownStatusSlugs() {
		assertThat(MangaStatus.fromSlug("completed")).isEqualTo(MangaStatus.COMPLETED);
		assertThat(MangaStatus.fromSlug("dropped")).isEqualTo(MangaStatus.DROPPED);
		assertThat(MangaStatus.fromSlug("hiatus")).isEqualTo(MangaStatus.HIATUS);
		assertThat(MangaStatus.fromSlug("cancelled")).isEqualTo(MangaStatus.CANCELLED);
	}
}
