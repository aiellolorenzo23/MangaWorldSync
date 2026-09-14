package com.mangaworldsync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mangaworldsync.config.MangaSyncProperties;
import com.mangaworldsync.model.MangaNotification;
import com.mangaworldsync.model.MangaProgress;
import com.mangaworldsync.model.MangaStatus;
import com.mangaworldsync.repository.MangaNotificationRepository;
import com.mangaworldsync.repository.MangaProgressRepository;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MangaTrackingServiceTests {
	@Test
	void initializesWithoutAlertThenCreatesOneAlertForNewChapter() throws Exception {
		ProgressMemory progress = new ProgressMemory();
		NotificationMemory notifications = new NotificationMemory();
		MangaPageClient pageClient = mock(MangaPageClient.class);
		PushDeliveryService push = mock(PushDeliveryService.class);
		MangaProgress saved = legacy("1", MangaStatus.ONGOING, null);
		progress.save(saved);
		RemoteChapter old = chapter("aaaaaaaaaaaaaaaaaaaaaaaa", "Capitolo 10");
		RemoteChapter fresh = chapter("bbbbbbbbbbbbbbbbbbbbbbbb", "Capitolo 11");
		when(pageClient.fetch(saved)).thenReturn(new MangaSnapshot(MangaStatus.ONGOING, List.of(old)));
		MangaTrackingService service = service(progress, notifications, pageClient, push);

		assertThat(service.check("token").notificationsCreated()).isZero();
		MangaProgress initialized = progress.findByMangaId("1").orElseThrow();
		when(pageClient.fetch(initialized)).thenReturn(new MangaSnapshot(MangaStatus.ONGOING, List.of(fresh, old)));
		assertThat(service.check("token").notificationsCreated()).isOne();
		assertThat(notifications.findAll()).singleElement().extracting(MangaNotification::chapterId).isEqualTo(fresh.id());
	}

	@Test
	void skipsMangaThatIsNotOngoing() {
		ProgressMemory progress = new ProgressMemory();
		progress.save(legacy("2", MangaStatus.COMPLETED, "aaaaaaaaaaaaaaaaaaaaaaaa"));
		MangaTrackingService.ScanResult result = service(progress, new NotificationMemory(), mock(MangaPageClient.class),
				mock(PushDeliveryService.class)).check("token");
		assertThat(result.checked()).isZero();
		assertThat(result.skipped()).isOne();
	}

	private static MangaTrackingService service(ProgressMemory progress, NotificationMemory notifications,
			MangaPageClient client, PushDeliveryService push) {
		return new MangaTrackingService(new MangaSyncProperties("token", Path.of("x"), List.of("www.mangaworld.mx")),
				progress, notifications, client, push);
	}
	private static RemoteChapter chapter(String id, String label) {
		return new RemoteChapter(id, label, "https://www.mangaworld.mx/read/" + id + "/1");
	}
	private static MangaProgress legacy(String id, MangaStatus status, String latest) {
		return new MangaProgress(id, "slug", "reader", null, "Capitolo 1", 3, "Titolo", null,
				"https://www.mangaworld.mx/manga/" + id + "/slug/read/123456789012345678901234/3", Instant.EPOCH,
				status, latest, latest == null ? null : "Capitolo 10", latest == null ? null : "url", null, null);
	}

	private static class ProgressMemory implements MangaProgressRepository {
		private final Map<String, MangaProgress> values = new LinkedHashMap<>();
		public MangaProgress save(MangaProgress value) { values.put(value.mangaId(), value); return value; }
		public Optional<MangaProgress> findByMangaId(String id) { return Optional.ofNullable(values.get(id)); }
		public Collection<MangaProgress> findAll() { return values.values(); }
		public void deleteByMangaId(String id) { values.remove(id); }
	}
	private static class NotificationMemory implements MangaNotificationRepository {
		private final Map<String, MangaNotification> values = new LinkedHashMap<>();
		public MangaNotification save(MangaNotification value) { values.put(value.id(), value); return value; }
		public Optional<MangaNotification> findById(String id) { return Optional.ofNullable(values.get(id)); }
		public Collection<MangaNotification> findAll() { return values.values(); }
		public void deleteById(String id) { values.remove(id); }
	}
}
