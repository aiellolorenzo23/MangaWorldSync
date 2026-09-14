package com.mangaworldsync.service;

import com.mangaworldsync.config.MangaSyncProperties;
import com.mangaworldsync.model.MangaNotification;
import com.mangaworldsync.model.MangaProgress;
import com.mangaworldsync.model.MangaStatus;
import com.mangaworldsync.repository.MangaNotificationRepository;
import com.mangaworldsync.repository.MangaProgressRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MangaTrackingService {
	private final MangaSyncProperties properties;
	private final MangaProgressRepository progressRepository;
	private final MangaNotificationRepository notificationRepository;
	private final MangaPageClient pageClient;
	private final PushDeliveryService pushDelivery;
	private final Clock clock = Clock.systemUTC();

	public MangaTrackingService(MangaSyncProperties properties, MangaProgressRepository progressRepository,
			MangaNotificationRepository notificationRepository, MangaPageClient pageClient,
			PushDeliveryService pushDelivery) {
		this.properties = properties;
		this.progressRepository = progressRepository;
		this.notificationRepository = notificationRepository;
		this.pageClient = pageClient;
		this.pushDelivery = pushDelivery;
	}

	public ScanResult check(String token) {
		validateToken(token);
		int checked = 0, initialized = 0, created = 0, skipped = 0, errors = 0;
		for (MangaProgress progress : List.copyOf(progressRepository.findAll())) {
			boolean initial = progress.status() == null || progress.latestChapterId() == null;
			if (!initial && progress.status() != MangaStatus.ONGOING) {
				skipped++;
				continue;
			}
			checked++;
			try {
				MangaSnapshot snapshot = pageClient.fetch(progress);
				RemoteChapter latest = snapshot.chapters().getFirst();
				if (initial) {
					initialized++;
				}
				else {
					for (RemoteChapter chapter : newChapters(snapshot.chapters(), progress.latestChapterId())) {
						MangaNotification notification = createNotification(progress, chapter);
						if (notificationRepository.findById(notification.id()).isEmpty()) {
							notificationRepository.save(notification);
							pushDelivery.send(notification);
							created++;
						}
					}
				}
				progressRepository.save(withTracking(progress, snapshot.status(), latest, null));
			}
			catch (Exception ex) {
				errors++;
				progressRepository.save(withError(progress, conciseMessage(ex)));
			}
		}
		return new ScanResult(checked, initialized, created, skipped, errors);
	}

	public Collection<MangaNotification> notifications(String token) {
		validateToken(token);
		return notificationRepository.findAll();
	}

	public void markRead(String token, String id) {
		validateToken(token);
		MangaNotification current = notificationRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notifica non trovata"));
		if (current.readAt() == null) notificationRepository.save(new MangaNotification(current.id(), current.mangaId(),
				current.mangaTitle(), current.coverUrl(), current.chapterId(), current.chapterLabel(),
				current.chapterUrl(), current.discoveredAt(), Instant.now(clock)));
	}

	public void markAllRead(String token) {
		validateToken(token);
		Instant now = Instant.now(clock);
		for (MangaNotification current : notificationRepository.findAll()) {
			if (current.readAt() == null) notificationRepository.save(new MangaNotification(current.id(), current.mangaId(),
					current.mangaTitle(), current.coverUrl(), current.chapterId(), current.chapterLabel(),
					current.chapterUrl(), current.discoveredAt(), now));
		}
	}

	private List<RemoteChapter> newChapters(List<RemoteChapter> chapters, String previousId) {
		List<RemoteChapter> result = new ArrayList<>();
		for (RemoteChapter chapter : chapters) {
			if (chapter.id().equals(previousId)) return result.reversed();
			result.add(chapter);
		}
		return result.isEmpty() ? result : List.of(result.getFirst());
	}

	private MangaNotification createNotification(MangaProgress progress, RemoteChapter chapter) {
		String title = progress.title() == null || progress.title().isBlank() ? progress.slug() : progress.title();
		title = title.replaceFirst("(?i)\\s+-\\s*MangaWorld$", "")
				.replaceFirst("(?i)\\s+Capitolo\\s+[\\w.-]+.*$", "").trim();
		return new MangaNotification(progress.mangaId() + ":" + chapter.id(), progress.mangaId(), title,
				progress.coverUrl(), chapter.id(), chapter.label(), chapter.url(), Instant.now(clock), null);
	}

	private MangaProgress withTracking(MangaProgress p, MangaStatus status, RemoteChapter latest, String error) {
		return new MangaProgress(p.mangaId(), p.slug(), p.chapterId(), p.volumeLabel(), p.chapterLabel(), p.page(),
				p.title(), p.coverUrl(), p.url(), p.updatedAt(), status, latest.id(), latest.label(), latest.url(),
				Instant.now(clock), error);
	}

	private MangaProgress withError(MangaProgress p, String error) {
		return new MangaProgress(p.mangaId(), p.slug(), p.chapterId(), p.volumeLabel(), p.chapterLabel(), p.page(),
				p.title(), p.coverUrl(), p.url(), p.updatedAt(), p.status(), p.latestChapterId(), p.latestChapterLabel(),
				p.latestChapterUrl(), Instant.now(clock), error);
	}

	private static String conciseMessage(Exception ex) {
		String message = ex.getMessage();
		return (message == null || message.isBlank() ? ex.getClass().getSimpleName() : message).substring(0,
				Math.min(300, message == null || message.isBlank() ? ex.getClass().getSimpleName().length() : message.length()));
	}

	private void validateToken(String token) {
		if (!properties.token().equals(token)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
	}

	public record ScanResult(int checked, int initialized, int notificationsCreated, int skipped, int errors) {}
}
