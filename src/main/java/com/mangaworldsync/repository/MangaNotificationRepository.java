package com.mangaworldsync.repository;

import com.mangaworldsync.model.MangaNotification;
import java.util.Collection;
import java.util.Optional;

public interface MangaNotificationRepository {
	MangaNotification save(MangaNotification notification);
	Optional<MangaNotification> findById(String id);
	Collection<MangaNotification> findAll();
	void deleteById(String id);
}
