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
package org.apache.juneau.rest.server.mcp.v20260728;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-endpoint-instance memoized {@link McpOptions} resolution for the {@link McpEndpoint} mixin path.
 *
 * <p>
 * The mixin path has no per-instance field to memoize onto (it is an interface, not a class), and
 * {@link McpEndpoint#getMcpOptions()} is deliberately the raw, unmemoized user override point &mdash; its
 * default returns a fresh {@link McpOptions} on every call, and an overriding implementation is free to do the
 * same (see that method's Javadoc example). Internal plumbing that needs a <b>stable</b>, per-binding
 * {@link McpOptions} ({@link McpEndpoint#revision()}, {@link McpEndpoint#subscriptionBroker()}, and the
 * {@code @Bean}-published accessor) resolves it through this cache instead of calling
 * {@link McpEndpoint#getMcpOptions()} directly: the first resolution for a given endpoint instance invokes
 * {@link McpEndpoint#getMcpOptions()} exactly once and publishes the result; every later resolution for that
 * <i>same</i> instance returns the identical object, while a <i>different</i> endpoint instance resolves to a
 * distinct {@link McpOptions} &mdash; giving per-binding memoization "for free" regardless of whether
 * {@link McpEndpoint#getMcpOptions()} itself was overridden, and regardless of whether a {@code RestContext} has
 * been initialized (unlike the {@code RestContext} bean store, this cache works against a bare-constructed
 * mixin instance too, which is what lets direct-construction unit tests exercise {@link McpEndpoint#revision()}
 * without standing up a full REST context).
 *
 * <p>
 * Keyed by <b>reference identity</b>, not {@code equals()}/{@code hashCode()}: a resource class that overrides
 * those (a record, a Lombok {@code @Data} class, or any other value-ish endpoint type) must still get its own
 * cache entry per distinct instance, or two unrelated bindings would collapse onto one shared {@link McpOptions}
 * &mdash; silently reintroducing the JVM-wide MRTR-key/broker sharing this whole cache exists to eliminate. The
 * key type is a small {@link WeakReference} subclass whose {@code hashCode()} is the referent's
 * {@link System#identityHashCode(Object) identity hash} (captured at construction, so it survives the referent
 * being cleared) and whose {@code equals()} is reference equality (<code>==</code>) on the referent, never
 * delegating to the endpoint's own {@code equals()}/{@code hashCode()}.
 *
 * <p>
 * Backed by a lock-free {@link ConcurrentHashMap} (no shared monitor serializing mixin traffic across the JVM,
 * unlike a {@code synchronized}-guarded {@code WeakHashMap}): the read/populate path is a single
 * {@link ConcurrentHashMap#computeIfAbsent} call. An endpoint instance that becomes otherwise unreachable does
 * not leak its cache entry: stale keys (whose referent has been garbage-collected) are enqueued on a
 * {@link ReferenceQueue} and drained &mdash; removing themselves from the map &mdash; at the start of every
 * {@link #resolve} call. (As a side effect of using {@code computeIfAbsent}, a {@link McpEndpoint#getMcpOptions()}
 * override that re-enters {@link #resolve} for the same endpoint instance fails fast with an
 * {@code IllegalStateException} ("Recursive update"), rather than the unbounded recursion / {@code StackOverflowError}
 * a {@code WeakHashMap}-backed implementation would suffer.)
 */
final class McpEndpointOptionsCache {

	private McpEndpointOptionsCache() {}

	private static final ReferenceQueue<McpEndpoint> QUEUE = new ReferenceQueue<>();
	private static final Map<IdentityKey,McpOptions> CACHE = new ConcurrentHashMap<>();

	/**
	 * Resolves {@code endpoint}'s memoized {@link McpOptions}, invoking {@link McpEndpoint#getMcpOptions()}
	 * exactly once per endpoint instance.
	 *
	 * @param endpoint The endpoint instance to resolve. Must not be <jk>null</jk>.
	 * @return The memoized options. Never <jk>null</jk>.
	 */
	static McpOptions resolve(McpEndpoint endpoint) {
		expungeStaleEntries();
		return CACHE.computeIfAbsent(new IdentityKey(endpoint), IdentityKey::resolveOptions);
	}

	/**
	 * Drains {@link #QUEUE} of keys whose referent has been garbage-collected, removing the corresponding
	 * (now-orphaned) cache entry. Called on every {@link #resolve} so the cache never grows unbounded with
	 * entries for endpoint instances nothing references anymore.
	 */
	private static void expungeStaleEntries() {
		Reference<? extends McpEndpoint> ref;
		while ((ref = QUEUE.poll()) != null)
			CACHE.remove(ref);
	}

	/**
	 * A weak, identity-keyed {@link McpEndpoint} reference usable as a {@link Map} key: {@code hashCode()} is
	 * the referent's identity hash (captured up front, so it remains stable even after the referent is
	 * cleared), and {@code equals()} is reference equality on the referent &mdash; deliberately never
	 * delegating to the endpoint's own {@code equals()}/{@code hashCode()}, since those may be value-based.
	 */
	static final class IdentityKey extends WeakReference<McpEndpoint> {
		private final int hash;

		IdentityKey(McpEndpoint referent) {
			super(referent, QUEUE);
			hash = System.identityHashCode(referent);
		}

		McpOptions resolveOptions() {
			var endpoint = get();
			return endpoint == null ? new McpOptions() : endpoint.getMcpOptions();
		}

		@Override
		public int hashCode() {
			return hash;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof IdentityKey other))
				return false;
			var mine = get();
			return mine != null && mine == other.get();
		}
	}
}
