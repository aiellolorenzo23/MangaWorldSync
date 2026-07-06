package com.mangaworldsync.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.mangaworldsync.model.MangaProgress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"fakedb.path=target/test-data/repository-fakedb.json"
})
class FakeDbMangaProgressRepositoryTests {

	@Autowired
	MangaProgressRepository repository;

	@BeforeEach
	void deleteStorageFile() throws Exception {
		Files.deleteIfExists(Path.of("target/test-data/repository-fakedb.json"));
	}

	@Test
	void savesUpdatesAndReadsProgress() {
		MangaProgress first = new MangaProgress(
				"404",
				"nanatsu-no-taizai",
				"chapter-1",
				9,
				"Nanatsu",
				"https://www.mangaworld.mx/manga/404/nanatsu-no-taizai/read/chapter-1/9",
				Instant.parse("2026-07-06T21:30:00Z"));
		MangaProgress updated = new MangaProgress(
				"404",
				"nanatsu-no-taizai",
				"chapter-2",
				10,
				"Nanatsu",
				"https://www.mangaworld.mx/manga/404/nanatsu-no-taizai/read/chapter-2/10",
				Instant.parse("2026-07-06T22:30:00Z"));

		repository.save(first);
		repository.save(updated);

		assertThat(repository.findByMangaId("404")).contains(updated);
		assertThat(repository.findAll()).containsExactly(updated);
		assertThat(Path.of("target/test-data/repository-fakedb.json")).exists();
	}
}
