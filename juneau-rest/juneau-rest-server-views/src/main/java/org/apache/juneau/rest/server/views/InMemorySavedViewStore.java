/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.server.views;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.f;
import static org.apache.juneau.commons.utils.Shorts.iaex;

import java.util.*;
import java.util.concurrent.*;

/**
 * The framework's non-durable, in-memory default {@link SavedViewStore} (TODO-444 §3.3, owner decision item 8).
 *
 * <p>
 * <b>Not persistent:</b> everything lives in heap and is lost on JVM restart.  It exists so a host composing
 * {@link SavedViewsMixin} works out-of-the-box for dev/demo and the framework's own tests/canary with zero setup; a
 * consumer overrides {@link SavedViewsMixin#savedViewStore()} to supply a durable implementation against its own
 * datastore.
 *
 * <h5 class='section'>Thread-safety + race-safe quotas</h5>
 * <p>
 * Each user's data is guarded by its own monitor: every read and mutation for a given {@code principalName}
 * serializes on that user's {@code UserData} instance.  This is what makes the per-user aggregate quota race-safe
 * &mdash; two parallel creates in DIFFERENT {@code (page,view)} scopes for the same user still run one-at-a-time
 * through the aggregate check, so they cannot both slip past {@link SavedViewStore#MAX_VIEWS_PER_USER} (§3.2).
 * Different users proceed concurrently.  The blob-and-active flip of {@link #saveAndActivate} happens inside that
 * same monitor, so it is atomic.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link SavedViewStore}
 * 	<li class='jc'>{@link SavedViewsMixin}
 * </ul>
 *
 * @since 10.0.0
 */
public class InMemorySavedViewStore implements SavedViewStore {

	private static volatile InMemorySavedViewStore shared;

	/**
	 * The process-wide shared default instance returned by {@link SavedViewsMixin#savedViewStore()}.
	 *
	 * <p>
	 * Lazily created.  Shared across every host that composes {@link SavedViewsMixin} without overriding the seam;
	 * because every entry is keyed principal-first, users stay isolated even in this shared instance.  Consumers that
	 * want isolation between resources (or durability) override {@code savedViewStore()} with their own instance.
	 *
	 * @return The shared in-memory store.  Never <jk>null</jk>.
	 */
	static synchronized InMemorySavedViewStore shared() {
		if (shared == null)
			shared = new InMemorySavedViewStore();
		return shared;
	}

	private final int maxViewsPerScope;
	private final int maxViewsPerUser;

	private final ConcurrentMap<String,UserData> users = new ConcurrentHashMap<>();

	/**
	 * Creates a store with the default {@link SavedViewStore#MAX_VIEWS_PER_SCOPE} / {@link SavedViewStore#MAX_VIEWS_PER_USER}
	 * count caps.
	 */
	public InMemorySavedViewStore() {
		this(MAX_VIEWS_PER_SCOPE, MAX_VIEWS_PER_USER);
	}

	/**
	 * Creates a store with explicit count caps (chiefly for tests that need to trip a quota without minting hundreds
	 * of views).
	 *
	 * @param maxViewsPerScope The per-{@code (user,page,view)} count cap.  Must be positive.
	 * @param maxViewsPerUser The per-user aggregate count cap.  Must be positive.
	 */
	public InMemorySavedViewStore(int maxViewsPerScope, int maxViewsPerUser) {
		if (maxViewsPerScope <= 0)
			throw iaex("Argument 'maxViewsPerScope' must be positive: %s.", maxViewsPerScope);
		if (maxViewsPerUser <= 0)
			throw iaex("Argument 'maxViewsPerUser' must be positive: %s.", maxViewsPerUser);
		this.maxViewsPerScope = maxViewsPerScope;
		this.maxViewsPerUser = maxViewsPerUser;
	}

	@Override /* SavedViewStore */
	public Listing list(String principalName, String pageId, String viewId) {
		requireKey(principalName, viewId);
		var ud = users.get(principalName);
		if (ud == null)
			return new Listing(null, List.of());
		synchronized (ud) {
			var s = ud.scopes.get(new ScopeKey(pageId, viewId));
			if (s == null)
				return new Listing(null, List.of());
			var names = List.copyOf(s.views.keySet());
			// Dangling-active resolution (§3.2): an active pointer at a name no longer present resolves to Default.
			var active = (s.active != null && ! s.views.containsKey(s.active)) ? null : s.active;
			return new Listing(active, names);
		}
	}

	@Override /* SavedViewStore */
	public String load(String principalName, String pageId, String viewId, String name) {
		requireKey(principalName, viewId);
		assertArgNotNull("name", name);
		var ud = users.get(principalName);
		if (ud == null)
			return null;
		synchronized (ud) {
			var s = ud.scopes.get(new ScopeKey(pageId, viewId));
			return s == null ? null : s.views.get(name);
		}
	}

	@Override /* SavedViewStore */
	public void save(String principalName, String pageId, String viewId, String name, String blob) {
		write(principalName, pageId, viewId, name, blob, false);
	}

	@Override /* SavedViewStore */
	public void saveAndActivate(String principalName, String pageId, String viewId, String name, String blob) {
		write(principalName, pageId, viewId, name, blob, true);
	}

	@Override /* SavedViewStore */
	public void setActive(String principalName, String pageId, String viewId, String name) {
		requireKey(principalName, viewId);
		var ud = users.computeIfAbsent(principalName, k -> new UserData());
		synchronized (ud) {
			// A set-active never counts against a quota (it writes no blob), so the scope is created on demand and the
			// pointer stored as-is; a dangling name is resolved back to Default on the next list().
			ud.scopes.computeIfAbsent(new ScopeKey(pageId, viewId), k -> new ScopeData()).active = name;
		}
	}

	@Override /* SavedViewStore */
	public void delete(String principalName, String pageId, String viewId, String name) {
		requireKey(principalName, viewId);
		assertArgNotNull("name", name);
		var ud = users.get(principalName);
		if (ud == null)
			return;
		synchronized (ud) {
			var s = ud.scopes.get(new ScopeKey(pageId, viewId));
			if (s == null)
				return;
			s.views.remove(name);
			if (name.equals(s.active))
				s.active = null;
		}
	}

	private void write(String principalName, String pageId, String viewId, String name, String blob, boolean activate) {
		requireKey(principalName, viewId);
		assertArgNotNull("name", name);
		assertArgNotNull("blob", blob);
		var ud = users.computeIfAbsent(principalName, k -> new UserData());
		synchronized (ud) {
			var s = ud.scopes.computeIfAbsent(new ScopeKey(pageId, viewId), k -> new ScopeData());
			var isReplace = s.views.containsKey(name);
			if (! isReplace) {
				// The count applies to a CREATE only; a replace never trips it.  Both checks run under the user's
				// monitor, so the aggregate check is serialized across all of this user's scopes (race-safe).
				if (s.views.size() >= maxViewsPerScope)
					throw new SavedViewQuotaException(SavedViewQuotaException.Kind.SCOPE,
						f("scope already has %s saved views (MAX_VIEWS_PER_SCOPE)", maxViewsPerScope));
				if (totalViews(ud) >= maxViewsPerUser)
					throw new SavedViewQuotaException(SavedViewQuotaException.Kind.AGGREGATE,
						f("aggregate saved-view count reached MAX_VIEWS_PER_USER (%s)", maxViewsPerUser));
			}
			s.views.put(name, blob);
			if (activate)
				s.active = name;
		}
	}

	/** Total saved views for a user across every scope.  Caller must hold the user's monitor. */
	private static int totalViews(UserData ud) {
		var total = 0;
		for (var s : ud.scopes.values())
			total += s.views.size();
		return total;
	}

	private static void requireKey(String principalName, String viewId) {
		assertArgNotNull("principalName", principalName);
		assertArgNotNull("viewId", viewId);
	}

	/** Per-user data; every access is guarded by {@code synchronized(this-instance)} (see the class javadoc). */
	private static final class UserData {
		final Map<ScopeKey,ScopeData> scopes = new HashMap<>();
	}

	/** One {@code (page,view)} scope's saved-view blobs (insertion-ordered) plus its active pointer. */
	private static final class ScopeData {
		final Map<String,String> views = new LinkedHashMap<>();
		String active;
	}

	/** The structured scope key &mdash; separate fields, never a delimiter-joined string ({@code pageId} may be null). */
	private record ScopeKey(String pageId, String viewId) {}
}
