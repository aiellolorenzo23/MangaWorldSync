package com.mangaworldsync.config;

import java.nio.file.Path;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "manga-sync")
public record MangaSyncProperties(
		String token,
		Path storageFile,
		List<String> allowedHosts,
		String vapidPublicKey,
		String vapidPrivateKey,
		String vapidSubject) {

	public MangaSyncProperties(String token, Path storageFile, List<String> allowedHosts) {
		this(token, storageFile, allowedHosts, null, null, null);
	}

	@org.springframework.boot.context.properties.bind.ConstructorBinding
	public MangaSyncProperties {
		if (token == null || token.isBlank()) {
			throw new IllegalArgumentException("manga-sync.token must not be blank");
		}
		if (storageFile == null) {
			storageFile = Path.of("./data/manga-progress.json");
		}
		if (allowedHosts == null || allowedHosts.isEmpty()) {
			throw new IllegalArgumentException("manga-sync.allowed-hosts must not be empty");
		}
		allowedHosts = allowedHosts.stream()
				.filter(host -> host != null && !host.isBlank())
				.map(host -> host.toLowerCase(java.util.Locale.ROOT))
				.toList();
		if (allowedHosts.isEmpty()) {
			throw new IllegalArgumentException("manga-sync.allowed-hosts must not be empty");
		}
	}
}
