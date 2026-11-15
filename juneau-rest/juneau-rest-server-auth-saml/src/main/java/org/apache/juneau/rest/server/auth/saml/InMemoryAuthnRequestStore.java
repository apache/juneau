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
package org.apache.juneau.rest.server.auth.saml;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.security.MessageDigest;
import java.util.*;

/**
 * Per-process, in-memory implementation of {@link AuthnRequestStore}.
 *
 * <p>
 * Holds at most {@link #DEFAULT_MAX_ENTRIES} unexpired request IDs; each ID is accepted once.  Not shared
 * between nodes.
 *
 * @since 10.0.0
 */
public class InMemoryAuthnRequestStore implements AuthnRequestStore {

	/** Default upper bound on outstanding request IDs. */
	public static final int DEFAULT_MAX_ENTRIES = 10_000;

	private final int maxEntries;
	private final Map<String,Pending> pending = new HashMap<>();

	private record Pending(byte[] bindingDigest, long expiresAtMs) {}

	/** Constructor using {@link #DEFAULT_MAX_ENTRIES}. */
	public InMemoryAuthnRequestStore() {
		this(DEFAULT_MAX_ENTRIES);
	}

	/**
	 * Constructor.
	 *
	 * @param maxEntries The maximum number of outstanding request IDs.  Must be positive.
	 */
	public InMemoryAuthnRequestStore(int maxEntries) {
		if (maxEntries <= 0)
			throw iaex("maxEntries must be positive (was %s)", maxEntries);
		this.maxEntries = maxEntries;
	}

	@Override /* Overridden from AuthnRequestStore */
	public void remember(String id, byte[] bindingDigest, long expiresAtMs, long nowMs) {
		reqnb("id", id);
		reqnn("bindingDigest", bindingDigest);
		synchronized (pending) {
			if (pending.size() >= maxEntries)
				pending.values().removeIf(p -> p.expiresAtMs() <= nowMs);
			if (pending.size() >= maxEntries)
				throw new IllegalStateException("Too many outstanding SAML authentication requests");
			pending.put(id, new Pending(bindingDigest, expiresAtMs));
		}
	}

	@Override /* Overridden from AuthnRequestStore */
	public boolean consume(String id, byte[] bindingDigest, long nowMs) {
		reqnb("id", id);
		synchronized (pending) {
			var p = pending.remove(id);
			return p != null && p.expiresAtMs() > nowMs && bindingDigest != null && MessageDigest.isEqual(p.bindingDigest(), bindingDigest);
		}
	}
}
