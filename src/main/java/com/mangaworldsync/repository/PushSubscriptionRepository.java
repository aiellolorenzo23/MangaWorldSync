package com.mangaworldsync.repository;

import com.mangaworldsync.model.BrowserPushSubscription;
import java.util.Collection;
import java.util.Optional;

public interface PushSubscriptionRepository {
	BrowserPushSubscription save(BrowserPushSubscription subscription);
	Optional<BrowserPushSubscription> findById(String id);
	Collection<BrowserPushSubscription> findAll();
	void deleteById(String id);
}
