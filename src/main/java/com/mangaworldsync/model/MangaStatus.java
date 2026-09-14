package com.mangaworldsync.model;

public enum MangaStatus {
	ONGOING("In corso"),
	COMPLETED("Finito"),
	DROPPED("Droppato"),
	HIATUS("In pausa"),
	CANCELLED("Cancellato");

	private final String label;

	MangaStatus(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	public static MangaStatus fromSlug(String slug) {
		return switch (slug == null ? "" : slug.trim().toLowerCase(java.util.Locale.ROOT)) {
			case "ongoing" -> ONGOING;
			case "completed" -> COMPLETED;
			case "dropped" -> DROPPED;
			case "hiatus", "paused" -> HIATUS;
			case "cancelled", "canceled" -> CANCELLED;
			default -> throw new IllegalArgumentException("Stato MangaWorld sconosciuto: " + slug);
		};
	}
}
