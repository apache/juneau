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
package org.apache.juneau.rest.guard;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;

/**
 * Token-bucket {@link RestGuard} that throttles requests on a configurable per-request key.
 *
 * <p>
 * On each request the guard atomically attempts to acquire one token from a per-key bucket.  When tokens are
 * available the request proceeds and three advisory response headers are populated
 * ({@code X-RateLimit-Limit}, {@code X-RateLimit-Remaining}, {@code X-RateLimit-Reset}).  When the bucket is
 * empty a {@link TooManyRequests} exception is thrown with a {@code Retry-After} header set to the integer
 * number of seconds until the next token will be available.
 *
 * <h5 class='topic'>Example usage</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/api"</js>)
 * 	<jk>public class</jk> ApiResource <jk>extends</jk> BasicRestServlet {
 *
 * 		<jc>// Per-IP rate limit, 100 req/min, 200-burst.</jc>
 * 		<ja>@Bean</ja>(name=<js>"guards"</js>)
 * 		<jk>public</jk> RestGuardList rateLimits(BeanStore <jv>bs</jv>) {
 * 			<jk>return</jk> RestGuardList.<jsm>create</jsm>(<jv>bs</jv>)
 * 				.append(RateLimitGuard.<jsm>create</jsm>()
 * 					.permitsPerMinute(100)
 * 					.burst(200)
 * 					.build())
 * 				.build();
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='topic'>Keying</h5>
 *
 * <p>
 * The default key is the request's {@linkplain RestRequest#getRemoteAddr() remote address}.  Set
 * {@link Builder#xForwardedForAware(boolean) xForwardedForAware(true)} to honor the first hop of the
 * {@code X-Forwarded-For} header — required when running behind a trusted reverse proxy.
 *
 * <p>
 * <b>Trusted-proxy assumption.</b>  When {@code xForwardedForAware} is enabled without a trusted proxy in front
 * of the application, attackers can spoof the {@code X-Forwarded-For} header and defeat the IP-based key.  Only
 * opt in when the request edge enforces the header.
 *
 * <h5 class='topic'>Storage</h5>
 *
 * <p>
 * Bucket state is held by a pluggable {@link Storage} SPI.  The default {@link Storage#inMemory()} implementation
 * uses a {@link ConcurrentHashMap} with an LRU-style eviction policy (capped at 100 000 keys by default), suitable
 * for single-pod deployments.  Distributed deployments should provide a substrate-backed implementation
 * (e.g. Redis) so per-pod buckets do not split the shared rate envelope.
 *
 * <h5 class='topic'>Probe paths</h5>
 *
 * <p>
 * Built-in probe paths ({@code /healthz}, {@code /readyz}, {@code /livez}) are exempted from throttling by
 * default so a misconfigured limit cannot mask a healthy pod from the orchestrator.  Override via
 * {@link Builder#exemptPaths(String...)}.
 *
 * <h5 class='topic'>Time source</h5>
 *
 * <p>
 * Refill math uses {@link System#nanoTime()}.  This is monotonic and safe across wall-clock jumps but does not
 * map to a calendar instant, which slightly complicates debugging when the bucket is in an unexpected state.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerRateLimitAndRequestId">REST Server — Rate-Limiting and Request-Id Propagation</a>
 * </ul>
 *
 * @since 9.5.0
 */
public class RateLimitGuard extends RestGuard {

	/** Response header name for the advisory ceiling (tokens per refill window). */
	public static final String HEADER_LIMIT = "X-RateLimit-Limit";

	/** Response header name for the advisory tokens remaining after this call. */
	public static final String HEADER_REMAINING = "X-RateLimit-Remaining";

	/** Response header name for the advisory seconds until the bucket is fully refilled. */
	public static final String HEADER_RESET = "X-RateLimit-Reset";

	/** Standard HTTP {@code Retry-After} header name. */
	public static final String HEADER_RETRY_AFTER = "Retry-After";

	private final int capacity;
	private final double permitsPerSecond;
	private final Function<RestRequest,String> keyResolver;
	private final boolean xForwardedForAware;
	private final Set<String> exemptPaths;
	private final BiConsumer<RestRequest,RateLimitInfo> onLimitExceeded;
	private final Storage storage;

	/**
	 * Constructor.
	 *
	 * @param b The builder configuring this guard.  Must not be <jk>null</jk>.
	 */
	protected RateLimitGuard(Builder b) {
		assertArgNotNull("builder", b);
		if (b.permitsPerSecond <= 0.0)
			throw new IllegalArgumentException("Argument 'permitsPerSecond' must be > 0.");
		if (b.burst <= 0)
			throw new IllegalArgumentException("Argument 'burst' must be > 0.");
		this.capacity = b.burst;
		this.permitsPerSecond = b.permitsPerSecond;
		this.keyResolver = b.keyResolver != null ? b.keyResolver : RestRequest::getRemoteAddr;
		this.xForwardedForAware = b.xForwardedForAware;
		this.exemptPaths = Set.copyOf(b.exemptPaths);
		this.onLimitExceeded = b.onLimitExceeded;
		this.storage = b.storage != null ? b.storage : Storage.inMemory();
	}

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	@Override /* Overridden from RestGuard */
	public boolean guard(RestRequest req, RestResponse res) {
		if (isExempt(req))
			return true;
		var key = resolveKey(req);
		var result = storage.tryAcquire(key, capacity, permitsPerSecond);
		var info = new RateLimitInfo(key, capacity, result.remaining(), result.secondsUntilReset(), result.allowed());
		if (! result.allowed()) {
			setAdvisoryHeaders(res, info);
			var retry = Math.max(1L, info.secondsUntilReset());
			res.setHeader(HEADER_RETRY_AFTER, Long.toString(retry));
			if (onLimitExceeded != null)
				onLimitExceeded.accept(req, info);
			throw tooManyRequests(info);
		}
		setAdvisoryHeaders(res, info);
		return true;
	}

	@Override /* Overridden from RestGuard */
	public boolean isRequestAllowed(RestRequest req) {
		return true;
	}

	private boolean isExempt(RestRequest req) {
		if (exemptPaths.isEmpty())
			return false;
		var pi = Objects.toString(req.getPathInfo(), "");
		var sp = Objects.toString(req.getServletPath(), "");
		return exemptPaths.contains(pi) || exemptPaths.contains(sp);
	}

	private String resolveKey(RestRequest req) {
		if (xForwardedForAware) {
			var xff = req.getHeader("X-Forwarded-For");
			if (xff != null && ! xff.isBlank()) {
				var comma = xff.indexOf(',');
				return (comma < 0 ? xff : xff.substring(0, comma)).trim();
			}
		}
		var k = keyResolver.apply(req);
		return k != null ? k : "";
	}

	private static TooManyRequests tooManyRequests(RateLimitInfo info) {
		var retry = Math.max(1L, info.secondsUntilReset());
		var ex = new TooManyRequests("Rate limit exceeded for key ''{0}''. Retry after {1}s.", info.key(), retry);
		ex.setHeader(HEADER_LIMIT, Integer.toString(info.limit()));
		ex.setHeader(HEADER_REMAINING, "0");
		ex.setHeader(HEADER_RESET, Long.toString(info.secondsUntilReset()));
		ex.setHeader(HEADER_RETRY_AFTER, Long.toString(retry));
		return ex;
	}

	private static void setAdvisoryHeaders(RestResponse res, RateLimitInfo info) {
		res.setHeader(HEADER_LIMIT, Integer.toString(info.limit()));
		res.setHeader(HEADER_REMAINING, Integer.toString(info.remaining()));
		res.setHeader(HEADER_RESET, Long.toString(info.secondsUntilReset()));
	}

	/**
	 * Builder for {@link RateLimitGuard}.
	 */
	public static class Builder {

		private static final Set<String> DEFAULT_EXEMPT_PATHS = Set.of("/healthz", "/readyz", "/livez");

		double permitsPerSecond = 10.0;
		int burst = 10;
		Function<RestRequest,String> keyResolver;
		boolean xForwardedForAware;
		Set<String> exemptPaths = new LinkedHashSet<>(DEFAULT_EXEMPT_PATHS);
		BiConsumer<RestRequest,RateLimitInfo> onLimitExceeded;
		Storage storage;

		/**
		 * Constructor.
		 */
		protected Builder() {}

		/**
		 * Sets the steady-state refill rate in permits per second.
		 *
		 * @param value Permits per second.  Must be {@code > 0}.
		 * @return This object.
		 */
		public Builder permitsPerSecond(int value) {
			permitsPerSecond = value;
			return this;
		}

		/**
		 * Sets the steady-state refill rate in permits per minute.
		 *
		 * <p>
		 * Equivalent to {@code permitsPerSecond(value / 60.0)}.
		 *
		 * @param value Permits per minute.  Must be {@code > 0}.
		 * @return This object.
		 */
		public Builder permitsPerMinute(int value) {
			permitsPerSecond = value / 60.0;
			return this;
		}

		/**
		 * Sets the steady-state refill rate in permits per hour.
		 *
		 * <p>
		 * Equivalent to {@code permitsPerSecond(value / 3600.0)}.
		 *
		 * @param value Permits per hour.  Must be {@code > 0}.
		 * @return This object.
		 */
		public Builder permitsPerHour(int value) {
			permitsPerSecond = value / 3600.0;
			return this;
		}

		/**
		 * Sets the burst capacity (maximum tokens a bucket can hold).
		 *
		 * @param value Burst capacity.  Must be {@code > 0}.
		 * @return This object.
		 */
		public Builder burst(int value) {
			burst = value;
			return this;
		}

		/**
		 * Sets the per-request key resolver.
		 *
		 * <p>
		 * Default is {@link RestRequest#getRemoteAddr()}.  Common alternatives include the user-principal name,
		 * an API-key header, or a tenant id.  When the resolver returns <jk>null</jk> the empty string is used.
		 *
		 * @param value Resolver function.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder keyBy(Function<RestRequest,String> value) {
			assertArgNotNull("value", value);
			keyResolver = value;
			return this;
		}

		/**
		 * Enables {@code X-Forwarded-For}-aware key resolution.
		 *
		 * <p>
		 * When enabled, the first comma-separated hop of the {@code X-Forwarded-For} header takes precedence over
		 * the configured {@link #keyBy(Function)} resolver.  Only opt in when a trusted reverse proxy strips and
		 * rewrites the header; otherwise clients can spoof it to defeat IP-based throttling.
		 *
		 * @param value <jk>true</jk> to honor {@code X-Forwarded-For}.
		 * @return This object.
		 */
		public Builder xForwardedForAware(boolean value) {
			xForwardedForAware = value;
			return this;
		}

		/**
		 * Sets the set of request paths that bypass throttling.
		 *
		 * <p>
		 * Matched against {@link RestRequest#getPathInfo()} and {@link RestRequest#getServletPath()} (exact
		 * match).  Defaults to {@code /healthz}, {@code /readyz}, {@code /livez} — the built-in probe paths.
		 * Pass an empty array to clear the defaults.
		 *
		 * @param values Exempt paths.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder exemptPaths(String...values) {
			assertArgNotNull("values", values);
			exemptPaths = new LinkedHashSet<>(Arrays.asList(values));
			return this;
		}

		/**
		 * Sets a callback invoked when a request is rejected due to an empty bucket.
		 *
		 * <p>
		 * Useful for structured logging or metrics emission.  The callback runs before {@link TooManyRequests} is
		 * thrown and must not block.  Exceptions raised inside the callback propagate to the caller.
		 *
		 * @param value Callback.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder whenLimitExceeded(BiConsumer<RestRequest,RateLimitInfo> value) {
			assertArgNotNull("value", value);
			onLimitExceeded = value;
			return this;
		}

		/**
		 * Sets the bucket-state storage backend.
		 *
		 * <p>
		 * Default is {@link Storage#inMemory()}.  Substitute a distributed backend (Redis, DynamoDB, etc.) when
		 * running multiple pods that share a single rate envelope.
		 *
		 * @param value Storage backend.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder storage(Storage value) {
			assertArgNotNull("value", value);
			storage = value;
			return this;
		}

		/**
		 * Builds the guard.
		 *
		 * @return A new {@link RateLimitGuard}.
		 */
		public RateLimitGuard build() {
			return new RateLimitGuard(this);
		}
	}

	/**
	 * Snapshot of rate-limit state passed to the {@link Builder#whenLimitExceeded(BiConsumer) onLimitExceeded}
	 * callback and used to populate advisory response headers.
	 *
	 * @param key The per-request key (e.g. remote address, principal name).
	 * @param limit The bucket capacity (also reported as {@code X-RateLimit-Limit}).
	 * @param remaining Tokens remaining after this acquisition attempt.  Zero when the request was rejected.
	 * @param secondsUntilReset Seconds until the bucket is full again (also reported as {@code X-RateLimit-Reset}).
	 * @param allowed <jk>true</jk> if the request was admitted; <jk>false</jk> if the bucket was empty.
	 */
	public record RateLimitInfo(String key, int limit, int remaining, long secondsUntilReset, boolean allowed) {}

	/**
	 * Point-in-time view of a single token bucket as exposed by {@link Storage#snapshot()}.
	 *
	 * <p>
	 * Used by operator-facing tooling (e.g. {@code AdminMixin}'s {@code /admin/ratelimit} endpoint) to
	 * surface live per-key bucket state alongside the static configuration.  Token-bucket vocabulary
	 * intentionally — there is no discrete refill window because the bucket refills continuously at
	 * {@code permitsPerSecond}, so this record reports the current fill level ({@link #tokens()}), the integer
	 * tokens currently available ({@link #remaining()}), a convenience throttled flag, and the wall-clock instant
	 * of the bucket's last activity.
	 *
	 * @param key The per-request key (e.g. remote address, principal name).
	 * @param tokens The current fractional token count in the bucket.  Continuously refilled at the
	 * 	configured {@code permitsPerSecond} rate up to the configured capacity.
	 * @param remaining The integer number of tokens currently available — {@code floor(tokens)}.
	 * 	Mirrors the {@code X-RateLimit-Remaining} advisory header.
	 * @param throttled <jk>true</jk> when the bucket is empty enough that the next request would be rejected
	 * 	({@code tokens < 1.0}).  Convenience flag for at-a-glance operator dashboards.
	 * @param lastRequest The wall-clock {@link Instant} at which the bucket was last touched.
	 *
	 * @since 9.5.0
	 */
	public record BucketState(String key, double tokens, int remaining, boolean throttled, Instant lastRequest) {}

	/**
	 * Returns the bucket capacity (the maximum number of tokens a bucket can hold).
	 *
	 * @return The bucket capacity.
	 * @since 9.5.0
	 */
	public int getCapacity() {
		return capacity;
	}

	/**
	 * Returns the steady-state refill rate in permits per second.
	 *
	 * @return The refill rate.
	 * @since 9.5.0
	 */
	public double getPermitsPerSecond() {
		return permitsPerSecond;
	}

	/**
	 * Returns whether {@code X-Forwarded-For}-aware key resolution is enabled.
	 *
	 * @return <jk>true</jk> if {@code X-Forwarded-For} resolution is enabled.
	 * @since 9.5.0
	 */
	public boolean isXForwardedForAware() {
		return xForwardedForAware;
	}

	/**
	 * Returns the set of request paths that bypass throttling.
	 *
	 * @return The exempt paths.  Never <jk>null</jk>.
	 * @since 9.5.0
	 */
	public Set<String> getExemptPaths() {
		return exemptPaths;
	}

	/**
	 * Returns the bucket-state storage backend in use by this guard.
	 *
	 * @return The storage backend.  Never <jk>null</jk>.
	 * @since 9.5.0
	 */
	public Storage getStorage() {
		return storage;
	}

	/**
	 * Convenience accessor for {@link Storage#snapshot()} on the underlying storage backend.
	 *
	 * <p>
	 * Returns an empty map when the configured {@link Storage} does not override
	 * {@link Storage#snapshot()} (the default behavior for storages — e.g. Redis-backed — that can't cheaply
	 * enumerate every bucket).
	 *
	 * @return A point-in-time map of per-key bucket state.  Never <jk>null</jk>.
	 * @since 9.5.0
	 */
	public Map<String,BucketState> snapshot() {
		return storage.snapshot();
	}

	/**
	 * SPI for storing per-key token-bucket state.
	 *
	 * <p>
	 * Implementations must be thread-safe.  The default {@link #inMemory()} implementation is suitable for
	 * single-pod deployments.  Multi-pod deployments that share a single rate envelope should substitute a
	 * distributed backend.
	 *
	 * @since 9.5.0
	 */
	public interface Storage {

		/**
		 * Attempts to acquire one token from the bucket associated with the given key.
		 *
		 * @param key The bucket key.  Never <jk>null</jk>.
		 * @param capacity The maximum tokens the bucket can hold.
		 * @param permitsPerSecond The steady-state refill rate.
		 * @return The outcome of the attempt.  Never <jk>null</jk>.
		 */
		AcquireResult tryAcquire(String key, int capacity, double permitsPerSecond);

		/**
		 * Evicts buckets idle for longer than the given TTL.
		 *
		 * <p>
		 * The default {@link #inMemory()} implementation evicts opportunistically when its size cap is exceeded;
		 * call this method to force an eager sweep (typically from a scheduled task).
		 *
		 * @param ttl The idle threshold.  Buckets last touched longer than {@code ttl} ago are removed.
		 */
		void evict(Duration ttl);

		/**
		 * Returns a point-in-time view of every per-key bucket currently held by this storage backend.
		 *
		 * <p>
		 * Optional operation for operational visibility.  The default implementation returns an empty map so
		 * that external storage backends (Redis, DynamoDB, etc.) that can't cheaply enumerate every bucket
		 * stay backwards-compatible without code changes.  In-memory implementations <b>SHOULD</b> override
		 * this to expose live bucket state — see the bundled {@link #inMemory()} implementation, which walks
		 * its internal map and returns one {@link BucketState} per entry.
		 *
		 * <p>
		 * The returned map is a snapshot, not a live view — concurrent modifications to the underlying
		 * storage after this method returns are not reflected.  Per-bucket reads are individually consistent
		 * (the in-memory implementation reads each bucket under its own monitor), but the snapshot as a whole
		 * is not a global point-in-time consistent view.
		 *
		 * <p>
		 * <b>Cardinality warning:</b> on storages that hold many buckets (the bundled in-memory storage caps
		 * at 100&nbsp;000 keys by default) the returned map can be large.  Operator-facing callers should
		 * either filter client-side or use a paginated alternative if one is offered by the backend.
		 *
		 * @return A point-in-time map of per-key bucket state, keyed by per-request key.  Never <jk>null</jk>.
		 * @since 9.5.0
		 */
		default Map<String,BucketState> snapshot() {
			return Map.of();
		}

		/**
		 * Creates a new in-memory storage backend with the default size cap (100 000 keys).
		 *
		 * @return A new in-memory storage backend.
		 */
		static Storage inMemory() {
			return new InMemoryStorage(100_000);
		}

		/**
		 * Creates a new in-memory storage backend with a custom size cap.
		 *
		 * @param maxKeys The maximum number of keys held before LRU-style eviction kicks in.  Must be {@code > 0}.
		 * @return A new in-memory storage backend.
		 */
		static Storage inMemory(int maxKeys) {
			return new InMemoryStorage(maxKeys);
		}

		/**
		 * Outcome of a {@link #tryAcquire(String, int, double) tryAcquire} attempt.
		 *
		 * @param allowed <jk>true</jk> if the request was admitted.
		 * @param remaining Tokens left in the bucket after the attempt.
		 * @param secondsUntilReset Seconds until the bucket is full again.
		 */
		record AcquireResult(boolean allowed, int remaining, long secondsUntilReset) {}
	}

	/**
	 * Default in-memory {@link Storage} implementation.
	 *
	 * <p>
	 * Backed by a {@link ConcurrentHashMap} keyed by the per-request key.  Each bucket tracks token count and last
	 * touch time.  When the map exceeds {@code maxKeys} entries the least-recently-touched bucket is removed.
	 */
	static final class InMemoryStorage implements Storage {

		private final ConcurrentHashMap<String,Bucket> buckets = new ConcurrentHashMap<>();
		private final int maxKeys;

		InMemoryStorage(int maxKeys) {
			if (maxKeys <= 0)
				throw new IllegalArgumentException("Argument 'maxKeys' must be > 0.");
			this.maxKeys = maxKeys;
		}

		@Override
		public AcquireResult tryAcquire(String key, int capacity, double permitsPerSecond) {
			var bucket = buckets.computeIfAbsent(key, k -> new Bucket(capacity));
			var result = bucket.tryAcquire(capacity, permitsPerSecond);
			if (buckets.size() > maxKeys)
				evictOldest();
			return result;
		}

		@Override
		public void evict(Duration ttl) {
			var threshold = System.nanoTime() - ttl.toNanos();
			buckets.entrySet().removeIf(e -> e.getValue().lastTouchedNanos() < threshold);
		}

		@Override
		public Map<String,BucketState> snapshot() {
			var out = new LinkedHashMap<String,BucketState>();
			for (var e : buckets.entrySet()) {
				var b = e.getValue();
				var tokens = b.tokens();
				out.put(e.getKey(), new BucketState(
					e.getKey(),
					tokens,
					(int) Math.floor(tokens),
					tokens < 1.0,
					Instant.ofEpochMilli(b.lastWallMillis())
				));
			}
			return Map.copyOf(out);
		}

		int size() {
			return buckets.size();
		}

		private void evictOldest() {
			buckets.entrySet().stream()
				.min(Comparator.comparingLong(e -> e.getValue().lastTouchedNanos()))
				.map(Map.Entry::getKey)
				.ifPresent(buckets::remove);
		}
	}

	/**
	 * Per-key token-bucket state.
	 *
	 * <p>
	 * Holds a fractional token count and the {@link System#nanoTime()} value of the last touch.  The
	 * {@code synchronized} block keeps the read-refill-write sequence atomic without the overhead of a lock-free
	 * CAS loop on this hot path.
	 */
	static final class Bucket {

		private double tokens;
		private long lastNanos;
		private long lastWallMillis;

		Bucket(int capacity) {
			this.tokens = capacity;
			this.lastNanos = System.nanoTime();
			this.lastWallMillis = System.currentTimeMillis();
		}

		synchronized Storage.AcquireResult tryAcquire(int capacity, double permitsPerSecond) {
			var now = System.nanoTime();
			var elapsedSeconds = (now - lastNanos) / 1_000_000_000.0;
			tokens = Math.min(capacity, tokens + elapsedSeconds * permitsPerSecond);
			lastNanos = now;
			lastWallMillis = System.currentTimeMillis();
			if (tokens >= 1.0) {
				tokens -= 1.0;
				return new Storage.AcquireResult(true, (int) Math.floor(tokens), secondsUntilFull(capacity, permitsPerSecond));
			}
			return new Storage.AcquireResult(false, 0, secondsUntilFull(capacity, permitsPerSecond));
		}

		synchronized long lastTouchedNanos() {
			return lastNanos;
		}

		synchronized double tokens() {
			return tokens;
		}

		synchronized long lastWallMillis() {
			return lastWallMillis;
		}

		private long secondsUntilFull(int capacity, double permitsPerSecond) {
			var needed = Math.max(0.0, capacity - tokens);
			return (long) Math.ceil(needed / permitsPerSecond);
		}
	}
}
