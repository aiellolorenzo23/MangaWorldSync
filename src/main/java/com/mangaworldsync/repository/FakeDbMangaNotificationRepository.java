package com.mangaworldsync.repository;

import com.mangaworldsync.model.MangaNotification;
import io.github.aiellolorenzo23.fakedb.core.FakeDBTemplate;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class FakeDbMangaNotificationRepository implements MangaNotificationRepository {
	private final io.github.aiellolorenzo23.fakedb.core.FakeDBRepository<MangaNotification, String> notifications;

	public FakeDbMangaNotificationRepository(FakeDBTemplate template) {
		this.notifications = template.repository(MangaNotification.class, String.class);
	}

	@Override public MangaNotification save(MangaNotification notification) { return notifications.save(notification); }
	@Override public Optional<MangaNotification> findById(String id) { return notifications.findById(id); }
	@Override public Collection<MangaNotification> findAll() {
		return notifications.findAll().stream()
				.sorted(Comparator.comparing(MangaNotification::discoveredAt).reversed())
				.toList();
	}
	@Override public void deleteById(String id) { notifications.deleteById(id); }
}
