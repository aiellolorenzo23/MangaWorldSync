package com.mangaworldsync.controller;

import com.mangaworldsync.model.MangaNotification;
import com.mangaworldsync.service.MangaTrackingService;
import com.mangaworldsync.service.PushSubscriptionService;
import java.util.Collection;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mw/api")
public class NotificationController {
	private final MangaTrackingService tracking;
	private final PushSubscriptionService push;

	public NotificationController(MangaTrackingService tracking, PushSubscriptionService push) {
		this.tracking = tracking;
		this.push = push;
	}

	@PostMapping("/notifications/check")
	public MangaTrackingService.ScanResult check(@RequestParam String token) { return tracking.check(token); }

	@GetMapping("/notifications")
	public Collection<MangaNotification> notifications(@RequestParam String token) { return tracking.notifications(token); }

	@PostMapping("/notifications/read")
	public ResponseEntity<Void> read(@RequestParam String token, @RequestParam String id) {
		tracking.markRead(token, id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/notifications/read-all")
	public ResponseEntity<Void> readAll(@RequestParam String token) {
		tracking.markAllRead(token);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/push/config")
	public PushSubscriptionService.PushConfig pushConfig(@RequestParam String token) { return push.config(token); }

	@PostMapping("/push/subscribe")
	public ResponseEntity<Void> subscribe(@RequestParam String token,
			@RequestBody PushSubscriptionService.PushSubscriptionRequest request) {
		push.subscribe(token, request);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/push/unsubscribe")
	public ResponseEntity<Void> unsubscribe(@RequestParam String token, @RequestParam String endpoint) {
		push.unsubscribe(token, endpoint);
		return ResponseEntity.noContent().build();
	}
}
