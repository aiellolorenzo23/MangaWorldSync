package com.mangaworldsync.repository;

import com.mangaworldsync.model.MangaProgress;
import io.github.aiellolorenzo23.fakedb.core.FakeDBTemplate;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class FakeDbMangaProgressRepository implements MangaProgressRepository {

	private final io.github.aiellolorenzo23.fakedb.core.FakeDBRepository<MangaProgress, String> progress;

	public FakeDbMangaProgressRepository(FakeDBTemplate fakeDBTemplate) {
		this.progress = fakeDBTemplate.repository(MangaProgress.class, String.class);
	}

	@Override
	public MangaProgress save(MangaProgress progress) {
		return this.progress.save(progress);
	}

	@Override
	public Optional<MangaProgress> findByMangaId(String mangaId) {
		return progress.findById(mangaId);
	}

	@Override
	public Collection<MangaProgress> findAll() {
		return progress.findAll().stream()
				.sorted(Comparator.comparing(MangaProgress::updatedAt).reversed())
				.toList();
	}

	@Override
	public void deleteByMangaId(String mangaId) {
		progress.deleteById(mangaId);
	}
}
