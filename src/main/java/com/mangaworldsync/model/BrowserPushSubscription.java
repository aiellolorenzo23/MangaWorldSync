package com.mangaworldsync.model;

import io.github.aiellolorenzo23.fakedb.annotation.FakeDBId;
import io.github.aiellolorenzo23.fakedb.annotation.FakeDBTable;
import java.time.Instant;

@FakeDBTable(value = "push_subscriptions", schema = "main")
public record BrowserPushSubscription(
		@FakeDBId String id,
		String endpoint,
		String p256dh,
		String auth,
		Instant createdAt) {
}
