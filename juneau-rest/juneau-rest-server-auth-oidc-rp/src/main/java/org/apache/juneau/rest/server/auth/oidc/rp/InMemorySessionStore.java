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
package org.apache.juneau.rest.server.auth.oidc.rp;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.time.*;
import java.util.*;

/**
 * In-memory {@link SessionStore} backed by a bounded-LRU map, indexed by {@code sub} and {@code sid} so
 * it can satisfy back-channel logout.
 *
 * <p>
 * This is the simpler single-instance / development option.  It is lost on restart and breaks under
 * horizontal scaling without sticky sessions; for production multi-instance deployments use
 * {@link SignedCookieSessionStore} (stateless, the documented default) or a caller-supplied distributed
 * store.
 *
 * <p>
 * Eviction is the conjunction of a size cap (LRU) and per-session TTL (an expired session is treated as
 * a miss and removed on lookup).  Because it retains sessions server-side, it
 * {@link #supportsServerSideRevocation() supports} {@link #invalidateBySubject(String)} and
 * {@link #invalidateBySessionId(String)}.
 *
 * @since 10.0.0
 */
public class InMemorySessionStore implements SessionStore {

	/** Default maximum number of live sessions retained. */
	public static final int DEFAULT_MAX_ENTRIES = 10_000;

	/**
	 * Static creator with the default size cap.
	 *
	 * @return A new store.
	 */
	public static InMemorySessionStore create() {
		return new InMemorySessionStore(DEFAULT_MAX_ENTRIES, Clock.systemUTC());
	}

	/**
	 * Static creator with a custom size cap.
	 *
	 * @param maxEntries The maximum number of live sessions.  Must be positive.
	 * @return A new store.
	 */
	public static InMemorySessionStore create(int maxEntries) {
		return new InMemorySessionStore(maxEntries, Clock.systemUTC());
	}

	private final int maxEntries;
	private final Clock clock;
	private final Map<String,OidcSession> byId;
	private final Map<String,Set<String>> bySubject = new HashMap<>();
	private final Map<String,Set<String>> bySid = new HashMap<>();
	private final Object lock = new Object();

	/**
	 * Constructor.
	 *
	 * @param maxEntries The maximum number of live sessions.  Must be positive.
	 * @param clock The clock used for TTL comparisons.  Must not be <jk>null</jk>.
	 */
	public InMemorySessionStore(int maxEntries, Clock clock) {
		assertArg(maxEntries > 0, "maxEntries must be positive (was {0})", maxEntries);
		this.maxEntries = maxEntries;
		this.clock = assertArgNotNull("clock", clock);
		this.byId = new LinkedHashMap<>(16, 0.75f, true) {
			private static final long serialVersionUID = 1L;
			@Override
			protected boolean removeEldestEntry(Map.Entry<String,OidcSession> eldest) {
				if (size() > InMemorySessionStore.this.maxEntries) {
					unindex(eldest.getValue());
					return true;
				}
				return false;
			}
		};
	}

	@Override /* Overridden from SessionStore */
	public String createSessionCookieValue(OidcSession session) {
		assertArgNotNull("session", session);
		synchronized (lock) {
			byId.put(session.id(), session);
			bySubject.computeIfAbsent(session.subject(), k -> new HashSet<>()).add(session.id());
			session.sid().ifPresent(sid -> bySid.computeIfAbsent(sid, k -> new HashSet<>()).add(session.id()));
		}
		return session.id();
	}

	@Override /* Overridden from SessionStore */
	public Optional<OidcSession> lookup(String cookieValue) {
		assertArgNotNull("cookieValue", cookieValue);
		synchronized (lock) {
			var s = byId.get(cookieValue);
			if (s == null)
				return opte();
			if (s.isExpired(clock.instant())) {
				removeById(cookieValue);
				return opte();
			}
			return opt(s);
		}
	}

	@Override /* Overridden from SessionStore */
	public void invalidate(String cookieValue) {
		assertArgNotNull("cookieValue", cookieValue);
		synchronized (lock) {
			removeById(cookieValue);
		}
	}

	@Override /* Overridden from SessionStore */
	public boolean supportsServerSideRevocation() {
		return true;
	}

	@Override /* Overridden from SessionStore */
	public int invalidateBySubject(String subject) {
		assertArgNotNullOrBlank("subject", subject);
		synchronized (lock) {
			var ids = bySubject.get(subject);
			if (ids == null)
				return 0;
			var count = 0;
			for (var id : new ArrayList<>(ids))
				count += removeById(id) ? 1 : 0;
			return count;
		}
	}

	@Override /* Overridden from SessionStore */
	public int invalidateBySessionId(String sid) {
		assertArgNotNullOrBlank("sid", sid);
		synchronized (lock) {
			var ids = bySid.get(sid);
			if (ids == null)
				return 0;
			var count = 0;
			for (var id : new ArrayList<>(ids))
				count += removeById(id) ? 1 : 0;
			return count;
		}
	}

	/**
	 * Returns the current live-session count.  Primarily for tests and metrics.
	 *
	 * @return The session count.
	 */
	public int size() {
		synchronized (lock) {
			return byId.size();
		}
	}

	private boolean removeById(String id) {
		var removed = byId.remove(id);
		if (removed == null)
			return false;
		unindex(removed);
		return true;
	}

	private void unindex(OidcSession s) {
		var subjects = bySubject.get(s.subject());
		if (subjects != null) {
			subjects.remove(s.id());
			if (subjects.isEmpty())
				bySubject.remove(s.subject());
		}
		s.sid().ifPresent(sid -> {
			var sids = bySid.get(sid);
			if (sids != null) {
				sids.remove(s.id());
				if (sids.isEmpty())
					bySid.remove(sid);
			}
		});
	}
}
