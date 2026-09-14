package com.mangaworldsync.repository;

import com.mangaworldsync.model.BrowserPushSubscription;
import io.github.aiellolorenzo23.fakedb.core.FakeDBTemplate;
import java.util.Collection;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class FakeDbPushSubscriptionRepository implements PushSubscriptionRepository {
	private final io.github.aiellolorenzo23.fakedb.core.FakeDBRepository<BrowserPushSubscription, String> subscriptions;

	public FakeDbPushSubscriptionRepository(FakeDBTemplate template) {
		this.subscriptions = template.repository(BrowserPushSubscription.class, String.class);
	}

	@Override public BrowserPushSubscription save(BrowserPushSubscription subscription) { return subscriptions.save(subscription); }
	@Override public Optional<BrowserPushSubscription> findById(String id) { return subscriptions.findById(id); }
	@Override public Collection<BrowserPushSubscription> findAll() { return subscriptions.findAll(); }
	@Override public void deleteById(String id) { subscriptions.deleteById(id); }
}
