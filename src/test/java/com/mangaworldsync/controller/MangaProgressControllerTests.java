package com.mangaworldsync.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
		"manga-sync.token=test-token",
		"manga-sync.storage-file=target/test-data/controller-progress.json",
		"fakedb.path=target/test-data/controller-progress.json"
})
class MangaProgressControllerTests {

	private static final String URL = "https://www.mangaworld.mx/manga/404/nanatsu-no-taizai/read/5f74d960d165b15bc7740472/9";
	private static final String ADULT_URL = "https://www.mangaworldadult.net/manga/4032/ane-no-himitsu-to-boku-no-jisatsu/read/67fa56ef1ce4d750c0cdac6b/11";
	private static final String ONESHOT_URL = "https://www.mangaworld.mx/manga/3309/naruto-gaiden-uzu-no-naka-no-tsumujikaze/read/64b7ab79d302a93f4b9dabc0/52";
	private static final String NUMBERED_ONESHOT_URL = "https://www.mangaworld.mx/manga/1570/fullmetal-alchemist-prototype/read/5f9a29f43963233d4e6f4f57/12?d=4";
	private static final String COVER_URL = "https://www.mangaworld.mx/covers/nanatsu.jpg";

	@Autowired
	MockMvc mockMvc;

	@BeforeEach
	void deleteStorageFile() throws Exception {
		Files.deleteIfExists(Path.of("target/test-data/controller-progress.json"));
	}

	@Test
	void saveStoresProgressAndRedirectsToOriginalUrl() throws Exception {
		mockMvc.perform(get("/mw/save")
						.param("token", "test-token")
						.param("url", URL)
						.param("title", "Nanatsu no Taizai")
						.param("coverUrl", COVER_URL)
						.param("volumeLabel", "Volume 05"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", URL));

		mockMvc.perform(get("/mw/api/progress").param("token", "test-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].mangaId").value("404"))
				.andExpect(jsonPath("$[0].title").value("Nanatsu no Taizai"))
				.andExpect(jsonPath("$[0].volumeLabel").value("Volume 05"))
				.andExpect(jsonPath("$[0].coverUrl").value(COVER_URL));
	}

	@Test
	void goRedirectsToSavedProgress() throws Exception {
		mockMvc.perform(get("/mw/save")
						.param("token", "test-token")
						.param("url", URL));

		mockMvc.perform(get("/mw/go")
						.param("token", "test-token")
						.param("mangaId", "404"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", URL));
	}

	@Test
	void wrongTokenReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/mw/api/progress").param("token", "wrong"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().string("Invalid token"));
	}

	@Test
	void pushSubscriptionAcceptsBrowserExpirationTime() throws Exception {
		mockMvc.perform(post("/mw/api/push/subscribe")
						.param("token", "test-token")
						.contentType("application/json")
						.content("""
								{
								  "endpoint": "https://push.example.test/subscription",
								  "expirationTime": null,
								  "keys": {
								    "p256dh": "browser-public-key",
								    "auth": "browser-auth-secret"
								  }
								}
								"""))
				.andExpect(status().isNoContent());
	}

	@Test
	void databaseDownloadReturnsStorageFileAsAttachment() throws Exception {
		mockMvc.perform(get("/mw/save")
						.param("token", "test-token")
						.param("url", URL)
						.param("title", "Nanatsu no Taizai"));

		mockMvc.perform(get("/mw/api/database/download").param("token", "test-token"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition", "attachment; filename=\"manga-progress.json\""))
				.andExpect(content().contentType("application/json"))
				.andExpect(content().bytes(Files.readAllBytes(Path.of("target/test-data/controller-progress.json"))));
	}

	@Test
	void databaseDownloadRejectsWrongToken() throws Exception {
		mockMvc.perform(get("/mw/api/database/download").param("token", "wrong"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().string("Invalid token"));
	}

	@Test
	void databaseDownloadReturnsNotFoundWhenStorageFileDoesNotExist() throws Exception {
		mockMvc.perform(get("/mw/api/database/download").param("token", "test-token"))
				.andExpect(status().isNotFound())
				.andExpect(content().string("Database file not found"));
	}

	@Test
	void externalUrlIsRejected() throws Exception {
		mockMvc.perform(get("/mw/save")
						.param("token", "test-token")
						.param("url", "https://example.com/manga/404/nanatsu-no-taizai/read/chapter/1"))
				.andExpect(status().isBadRequest())
				.andExpect(content().string(containsString("host")));
	}

	@Test
	void listRendersSavedProgress() throws Exception {
		mockMvc.perform(get("/mw/save")
						.param("token", "test-token")
						.param("url", URL)
						.param("title", "Nanatsu no Taizai")
						.param("coverUrl", COVER_URL));
		mockMvc.perform(get("/mw/save")
						.param("token", "test-token")
						.param("url", ADULT_URL)
						.param("title", "Ane no Himitsu"));

		mockMvc.perform(get("/mw/list").param("token", "test-token"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("href=\"/favicon.svg\"")))
				.andExpect(content().string(containsString("Copertina")))
				.andExpect(content().string(containsString(COVER_URL)))
				.andExpect(content().string(containsString("Nanatsu no Taizai")))
				.andExpect(content().string(containsString("404")))
				.andExpect(content().string(containsString("Cerca manga")))
				.andExpect(content().string(containsString("id=\"total-count\"")))
				.andExpect(content().string(containsString("Aggiornati di recente")))
				.andExpect(content().string(containsString("Titolo A-Z")))
				.andExpect(content().string(containsString("Mostra NSFW")))
				.andExpect(content().string(containsString("data-adult=\"false\"")))
				.andExpect(content().string(containsString("data-adult=\"true\"")))
				.andExpect(content().string(containsString("NSFW")))
				.andExpect(content().string(containsString(".manga-card[hidden]")))
				.andExpect(content().string(containsString("visibilitychange")))
				.andExpect(content().string(containsString("/mw/api/progress?token=")))
				.andExpect(content().string(containsString("Elimina")))
				.andExpect(content().string(containsString("target=\"_blank\"")))
				.andExpect(content().string(containsString("id=\"bell-button\"")))
				.andExpect(content().string(containsString("/mw/api/notifications")))
				.andExpect(content().string(containsString("Attiva notifiche sul telefono")))
				.andExpect(content().string(containsString("Apri")));
	}

	@Test
	void listRendersOneshotChapterLabels() throws Exception {
		mockMvc.perform(get("/mw/save")
						.param("token", "test-token")
						.param("url", ONESHOT_URL)
						.param("title", "Naruto Gaiden: Uzu no Naka no Tsumujikaze Oneshot Scan ITA"));
		mockMvc.perform(get("/mw/save")
						.param("token", "test-token")
						.param("url", NUMBERED_ONESHOT_URL)
						.param("title", "Fullmetal Alchemist Prototype Oneshot 02 Scan ITA"));

		mockMvc.perform(get("/mw/list").param("token", "test-token"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("progress-chapter\">Oneshot</span>")))
				.andExpect(content().string(containsString("progress-chapter\">Oneshot 02</span>")))
				.andExpect(content().string(containsString("progress-page\">Pagina 52</span>")))
				.andExpect(content().string(containsString("progress-page\">Pagina 12</span>")))
				.andExpect(content().string(containsString("item.updatedAt * 1000")));
	}

	@Test
	void savesAndEscapesOriginalReaderLabels() throws Exception {
		mockMvc.perform(get("/mw/save")
				.param("token", "test-token").param("url", URL)
				.param("title", "Nanatsu Capitolo 34")
				.param("volumeLabel", "  Raccolta <Extra>  ")
				.param("chapterLabel", "  Speciale <01>  "))
				.andExpect(status().isFound());
		mockMvc.perform(get("/mw/api/progress").param("token", "test-token"))
				.andExpect(jsonPath("$[0].volumeLabel").value("Raccolta <Extra>"))
				.andExpect(jsonPath("$[0].chapterLabel").value("Speciale <01>"));
		mockMvc.perform(get("/mw/list").param("token", "test-token"))
				.andExpect(content().string(containsString("progress-volume\">Raccolta &lt;Extra&gt;</span>")))
				.andExpect(content().string(containsString("progress-chapter\">Speciale &lt;01&gt;</span>")));
	}

	@Test
	void listRendersVolumeChapterAndPageBadges() throws Exception {
		mockMvc.perform(get("/mw/save")
						.param("token", "test-token")
						.param("url", URL)
						.param("title", "Nanatsu no Taizai Capitolo 34 Scan ITA")
						.param("volumeLabel", "Volume 05"));

		mockMvc.perform(get("/mw/list").param("token", "test-token"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("progress-volume\">Volume 05</span>")))
				.andExpect(content().string(containsString("progress-chapter\">Capitolo 34</span>")))
				.andExpect(content().string(containsString("progress-page\">Pagina 9</span>")))
				.andExpect(content().string(containsString("progressBadges(item)")));
	}

	@Test
	void listHidesNsfwToggleWhenNoAdultProgressExists() throws Exception {
		mockMvc.perform(get("/mw/save")
						.param("token", "test-token")
						.param("url", URL)
						.param("title", "Nanatsu no Taizai"));

		mockMvc.perform(get("/mw/list").param("token", "test-token"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("id='nsfw-toggle' hidden")))
				.andExpect(content().string(containsString("updateNsfwToggle()")));
	}

	@Test
	void deleteRemovesSavedProgressAndRedirectsToList() throws Exception {
		mockMvc.perform(get("/mw/save")
						.param("token", "test-token")
						.param("url", URL)
						.param("title", "Nanatsu no Taizai"));

		mockMvc.perform(post("/mw/delete")
						.param("token", "test-token")
						.param("mangaId", "404"))
				.andExpect(status().isSeeOther())
				.andExpect(header().string("Location", "/mw/list?token=test-token"));

		mockMvc.perform(get("/mw/api/progress").param("token", "test-token"))
				.andExpect(status().isOk())
				.andExpect(content().json("[]"));
	}
}
