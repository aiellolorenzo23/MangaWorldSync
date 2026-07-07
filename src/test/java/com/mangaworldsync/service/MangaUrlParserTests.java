package com.mangaworldsync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mangaworldsync.config.MangaSyncProperties;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class MangaUrlParserTests {

	private final MangaUrlParser parser = new MangaUrlParser(new MangaSyncProperties(
			"secret",
			Path.of("progress.json"),
			List.of("mangaworld.mx", "www.mangaworld.mx", "mangaworldadult.net", "www.mangaworldadult.net")));

	@Test
	void parsesValidReaderUrl() {
		ParsedMangaUrl parsed = parser.parse("https://www.mangaworld.mx/manga/404/nanatsu-no-taizai/read/5f74d960d165b15bc7740472/9");

		assertThat(parsed.mangaId()).isEqualTo("404");
		assertThat(parsed.slug()).isEqualTo("nanatsu-no-taizai");
		assertThat(parsed.chapterId()).isEqualTo("5f74d960d165b15bc7740472");
		assertThat(parsed.page()).isEqualTo(9);
	}

	@Test
	void acceptsAllowedBareHost() {
		ParsedMangaUrl parsed = parser.parse("https://mangaworld.mx/manga/404/nanatsu-no-taizai/read/chapter/1");

		assertThat(parsed.mangaId()).isEqualTo("404");
	}

	@Test
	void parsesAllowedAdultReaderUrl() {
		ParsedMangaUrl parsed = parser.parse("https://www.mangaworldadult.net/manga/4032/ane-no-himitsu-to-boku-no-jisatsu/read/67fa56ef1ce4d750c0cdac6b/11");

		assertThat(parsed.mangaId()).isEqualTo("4032");
		assertThat(parsed.slug()).isEqualTo("ane-no-himitsu-to-boku-no-jisatsu");
		assertThat(parsed.chapterId()).isEqualTo("67fa56ef1ce4d750c0cdac6b");
		assertThat(parsed.page()).isEqualTo(11);
	}

	@Test
	void rejectsExternalHost() {
		assertThatThrownBy(() -> parser.parse("https://example.com/manga/404/nanatsu-no-taizai/read/chapter/1"))
				.isInstanceOf(InvalidMangaUrlException.class)
				.hasMessageContaining("host");
	}

	@Test
	void rejectsNonReaderPath() {
		assertThatThrownBy(() -> parser.parse("https://www.mangaworld.mx/manga/404/nanatsu-no-taizai"))
				.isInstanceOf(InvalidMangaUrlException.class)
				.hasMessageContaining("reader");
	}

	@Test
	void rejectsInvalidUrl() {
		assertThatThrownBy(() -> parser.parse("not a url"))
				.isInstanceOf(InvalidMangaUrlException.class);
	}
}
