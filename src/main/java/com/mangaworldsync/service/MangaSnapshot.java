package com.mangaworldsync.service;

import com.mangaworldsync.model.MangaStatus;
import java.util.List;

public record MangaSnapshot(MangaStatus status, List<RemoteChapter> chapters) {
}
