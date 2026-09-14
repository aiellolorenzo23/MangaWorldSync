package com.mangaworldsync.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mangaworldsync.config.MangaSyncProperties;
import com.mangaworldsync.model.BrowserPushSubscription;
import com.mangaworldsync.model.MangaNotification;
import com.mangaworldsync.repository.PushSubscriptionRepository;
import java.util.Map;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.springframework.stereotype.Service;

@Service
public class PushDeliveryService {
	private final MangaSyncProperties properties;
	private final PushSubscriptionRepository subscriptions;
	private final ObjectMapper objectMapper;

	public PushDeliveryService(MangaSyncProperties properties, PushSubscriptionRepository subscriptions, ObjectMapper objectMapper) {
		this.properties = properties;
		this.subscriptions = subscriptions;
		this.objectMapper = objectMapper;
	}

	public boolean configured() {
		return present(properties.vapidPublicKey()) && present(properties.vapidPrivateKey()) && present(properties.vapidSubject());
	}

	public void send(MangaNotification mangaNotification) {
		if (!configured()) return;
		String payload;
		try {
			payload = objectMapper.writeValueAsString(Map.of(
					"title", "Nuovo capitolo disponibile",
					"body", mangaNotification.mangaTitle() + " · " + mangaNotification.chapterLabel(),
					"url", mangaNotification.chapterUrl(),
					"icon", "/favicon.svg"));
		}
		catch (JsonProcessingException ex) {
			return;
		}
		for (BrowserPushSubscription subscription : subscriptions.findAll()) {
			try {
				PushService push = new PushService(properties.vapidPublicKey(), properties.vapidPrivateKey(), properties.vapidSubject());
				HttpResponse response = push.send(new Notification(subscription.endpoint(), subscription.p256dh(), subscription.auth(), payload));
				int status = response.getStatusLine().getStatusCode();
				if (status == 404 || status == 410) subscriptions.deleteById(subscription.id());
			}
			catch (Exception ignored) {
				// A failed browser endpoint must not stop the weekly scan.
			}
		}
	}

	private static boolean present(String value) { return value != null && !value.isBlank(); }
}
