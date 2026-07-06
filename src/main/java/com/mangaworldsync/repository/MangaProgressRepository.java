package com.mangaworldsync.repository;

import com.mangaworldsync.model.MangaProgress;
import java.util.Collection;
import java.util.Optional;

public interface MangaProgressRepository {

	MangaProgress save(MangaProgress progress);

	Optional<MangaProgress> findByMangaId(String mangaId);

	Collection<MangaProgress> findAll();
}
