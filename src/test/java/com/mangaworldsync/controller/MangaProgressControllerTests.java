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
						.param("coverUrl", COVER_URL))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", URL));

		mockMvc.perform(get("/mw/api/progress").param("token", "test-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].mangaId").value("404"))
				.andExpect(jsonPath("$[0].title").value("Nanatsu no Taizai"))
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
				.andExpect(content().string(containsString("Aggiornati di recente")))
				.andExpect(content().string(containsString("Titolo A-Z")))
				.andExpect(content().string(containsString("Mostra NSFW")))
				.andExpect(content().string(containsString("data-adult=\"false\"")))
				.andExpect(content().string(containsString("data-adult=\"true\"")))
				.andExpect(content().string(containsString("NSFW")))
				.andExpect(content().string(containsString(".manga-card[hidden]")))
				.andExpect(content().string(containsString("Elimina")))
				.andExpect(content().string(containsString("target=\"_blank\"")))
				.andExpect(content().string(containsString("Apri")));
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
