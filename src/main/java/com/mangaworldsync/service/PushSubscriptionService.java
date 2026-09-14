package com.mangaworldsync.service;

import com.mangaworldsync.config.MangaSyncProperties;
import com.mangaworldsync.model.BrowserPushSubscription;
import com.mangaworldsync.repository.PushSubscriptionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PushSubscriptionService {
	private final MangaSyncProperties properties;
	private final PushSubscriptionRepository repository;

	public PushSubscriptionService(MangaSyncProperties properties, PushSubscriptionRepository repository) {
		this.properties = properties;
		this.repository = repository;
	}

	public PushConfig config(String token) {
		validate(token);
		boolean enabled = present(properties.vapidPublicKey()) && present(properties.vapidPrivateKey());
		return new PushConfig(enabled, enabled ? properties.vapidPublicKey() : null);
	}

	public void subscribe(String token, PushSubscriptionRequest request) {
		validate(token);
		if (request == null || !present(request.endpoint()) || request.keys() == null
				|| !present(request.keys().p256dh()) || !present(request.keys().auth())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Iscrizione push non valida");
		}
		repository.save(new BrowserPushSubscription(id(request.endpoint()), request.endpoint(), request.keys().p256dh(),
				request.keys().auth(), Instant.now()));
	}

	public void unsubscribe(String token, String endpoint) {
		validate(token);
		if (present(endpoint)) repository.deleteById(id(endpoint));
	}

	private void validate(String token) {
		if (!properties.token().equals(token)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
	}

	private static String id(String endpoint) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(endpoint.getBytes(StandardCharsets.UTF_8)));
		}
		catch (Exception ex) { throw new IllegalStateException(ex); }
	}
	private static boolean present(String value) { return value != null && !value.isBlank(); }

	public record PushConfig(boolean enabled, String publicKey) {}
	public record PushSubscriptionRequest(String endpoint, Keys keys) {}
	public record Keys(String p256dh, String auth) {}
}
