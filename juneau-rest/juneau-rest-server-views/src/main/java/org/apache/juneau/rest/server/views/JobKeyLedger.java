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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.time.*;
import java.util.*;

/**
 * The stock "record the minted key" store for row-bound job keys: issues an {@link IdempotencyKey} bound to
 * {@code (action, targetId[, principal])}, and verifies a submitted key value against that recorded binding.
 *
 * <p>
 * {@link IdempotencyKey} only <i>binds</i>; its consumer records what it minted.  This ledger is that record for the
 * row-job case: a key endpoint calls {@link #issue(String, String, String)} and answers {@link #response(IdempotencyKey)},
 * and the submit endpoint calls {@link #verify(String, String, String, String)} (usually through
 * {@link RowJobSubmit#verify(JobKeyLedger, String, String)}).
 *
 * <h5 class='section'>Rules</h5>
 * <ul class='spaced-list'>
 * 	<li><b>One refusal for every miss.</b>  Unknown, expired, evicted and mismatched keys all verify empty, and the
 * 		caller answers all of them with {@link #REFUSAL_CODE}, so the answer is no oracle for "this value exists for
 * 		another row".
 * 	<li><b>Spent after first use.</b>  On the first successful {@link AsyncJobRegistry#tryCreate(IdempotencyKey)} the
 * 		endpoint calls {@link #claim(IdempotencyKey, String)}.  A later verify returns the {@link Grant} with that job
 * 		id, and the endpoint replays the job's ref while the registry still has it, or answers {@link #SPENT_CODE}
 * 		&mdash; never a second {@code tryCreate}, so a replay after the registry's retention cannot start a second job.
 * 	<li><b>Bounded per principal.</b>  A principal holds at most {@code maxPerPrincipal} unspent keys per action; one
 * 		more evicts that principal's oldest.  {@code maxEntries} is a global backstop (oldest first).  Without auth,
 * 		every caller is one shared principal.
 * 	<li><b>Same actor on both endpoints.</b>  Resolve the principal the same way on the key endpoint and on submit, or
 * 		verify fails.
 * </ul>
 *
 * <p>
 * In-memory and single-process, like {@link AsyncJobRegistry}.  Safe for concurrent use.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>private final</jk> JobKeyLedger <jv>jobKeys</jv> = JobKeyLedger.<jsm>create</jsm>();
 *
 * 	<ja>@RestPost</ja>(path=<js>"/species/{id}/key"</js>)
 * 	<jk>public</jk> Map&lt;String,Object&gt; key(RestRequest <jv>req</jv>, <ja>@Path</ja>(<js>"id"</js>) String <jv>id</jv>) {
 * 		<jk>return</jk> JobKeyLedger.<jsm>response</jsm>(<jv>jobKeys</jv>.issue(<js>"restock-species"</js>, <jv>id</jv>, actor(<jv>req</jv>)));
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link IdempotencyKey}
 * 	<li class='jc'>{@link RowJobSubmit}
 * 	<li class='jc'>{@link AsyncJobRegistry}
 * </ul>
 *
 * @since 10.0.0
 */
public final class JobKeyLedger {

	/** The refusal code for every failed verify; the code {@link IdempotencyKey}'s Javadoc already uses. */
	public static final String REFUSAL_CODE = "write-guard:idempotency-binding-mismatch";

	/** The refusal code for a key that already started a job that is no longer in the registry. */
	public static final String SPENT_CODE = "write-guard:idempotency-key-spent";

	/** How long an issued key stays verifiable by default. */
	public static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

	/** The global entry cap by default. */
	public static final int DEFAULT_MAX_ENTRIES = 10_000;

	/** The per-principal, per-action cap on unspent keys by default. */
	public static final int DEFAULT_MAX_PER_PRINCIPAL = 64;

	/** The contract version of {@link #response(IdempotencyKey)}. */
	public static final String CONTRACT_VERSION = "1";

	/**
	 * A successful verify.
	 *
	 * @param key The rehydrated key, with its recorded binding.
	 * @param jobId The job this key already started, if {@link #claim(IdempotencyKey, String) claimed}.
	 */
	public record Grant(IdempotencyKey key, Optional<String> jobId) {}

	private record Entry(String action, String targetId, String principal, Instant expiresAt, String jobId) {
		Entry claimed(String id) { return new Entry(action, targetId, principal, expiresAt, id); }
	}

	private Duration ttl = DEFAULT_TTL;
	private int maxEntries = DEFAULT_MAX_ENTRIES;
	private int maxPerPrincipal = DEFAULT_MAX_PER_PRINCIPAL;
	private Clock clock = Clock.systemUTC();

	/** Value to entry, in access order: the eldest is the least-recently used. */
	private final LinkedHashMap<String,Entry> entries = new LinkedHashMap<>(16, 0.75f, true);

	private JobKeyLedger() {}

	/**
	 * Creates a ledger with the default limits.
	 *
	 * @return A new ledger.
	 */
	public static JobKeyLedger create() {
		return new JobKeyLedger();
	}

	/**
	 * @param value How long an issued key stays verifiable.  Must be positive.
	 * @return This object.
	 */
	public synchronized JobKeyLedger ttl(Duration value) {
		if (value == null || value.isNegative() || value.isZero())
			throw iaex("JobKeyLedger ttl must be positive.");
		ttl = value;
		return this;
	}

	/**
	 * @param value The global entry cap.  Must be positive.
	 * @return This object.
	 */
	public synchronized JobKeyLedger maxEntries(int value) {
		if (value < 1)
			throw iaex("JobKeyLedger maxEntries must be positive.");
		maxEntries = value;
		return this;
	}

	/**
	 * @param value The per-principal, per-action cap on unspent keys.  Must be positive.
	 * @return This object.
	 */
	public synchronized JobKeyLedger maxPerPrincipal(int value) {
		if (value < 1)
			throw iaex("JobKeyLedger maxPerPrincipal must be positive.");
		maxPerPrincipal = value;
		return this;
	}

	/**
	 * @param value The clock, for tests.
	 * @return This object.
	 */
	public synchronized JobKeyLedger clock(Clock value) {
		clock = reqnn("clock", value);
		return this;
	}

	/**
	 * Mints and records a key bound to {@code (action, targetId)} with no principal.
	 *
	 * @param action The action id.  Must not be <jk>null</jk> or blank.
	 * @param targetId The row id.  Must not be <jk>null</jk> or blank.
	 * @return The new key.
	 * @throws IllegalArgumentException If an argument is <jk>null</jk> or blank.
	 */
	public IdempotencyKey issue(String action, String targetId) {
		return record(action, targetId, null);
	}

	/**
	 * Mints and records a key bound to {@code (action, targetId)} and to the actor.
	 *
	 * @param action The action id.  Must not be <jk>null</jk> or blank.
	 * @param targetId The row id.  Must not be <jk>null</jk> or blank.
	 * @param principal The actor.  Must not be <jk>null</jk> or blank.
	 * @return The new key.
	 * @throws IllegalArgumentException If an argument is <jk>null</jk> or blank.
	 */
	public IdempotencyKey issue(String action, String targetId, String principal) {
		if (principal == null || principal.isBlank())
			throw iaex("Argument 'principal' must not be blank.");
		return record(action, targetId, principal);
	}

	private synchronized IdempotencyKey record(String action, String targetId, String principal) {
		var key = IdempotencyKey.mint(action, targetId);
		var now = clock.instant();
		sweep(now);
		capPrincipal(action, principal);
		entries.put(key.value(), new Entry(action, targetId, principal, now.plus(ttl), null));
		while (entries.size() > maxEntries)
			removeEldest();
		return key;
	}

	/** Evicts this principal's oldest unspent keys for this action until one more fits under the cap. */
	private void capPrincipal(String action, String principal) {
		var mine = new ArrayList<String>();
		for (var e : entries.entrySet()) {
			var v = e.getValue();
			if (v.jobId() == null && action.equals(v.action()) && Objects.equals(principal, v.principal()))
				mine.add(e.getKey());
		}
		// Access order puts the least-recently-used first.
		for (var i = 0; i <= mine.size() - maxPerPrincipal; i++)
			entries.remove(mine.get(i));
	}

	private void removeEldest() {
		var it = entries.entrySet().iterator();
		it.next();
		it.remove();
	}

	private void sweep(Instant now) {
		entries.values().removeIf(e -> ! now.isBefore(e.expiresAt()));
	}

	/**
	 * Verifies a submitted key against a binding with no principal.
	 *
	 * @param value The submitted key value.  Can be <jk>null</jk>.
	 * @param action The action the submit is for.  Can be <jk>null</jk>.
	 * @param targetId The row the submit is for.  Can be <jk>null</jk>.
	 * @return The grant, or empty for every kind of miss.
	 */
	public Optional<Grant> verify(String value, String action, String targetId) {
		return check(value, action, targetId, null);
	}

	/**
	 * Verifies a submitted key against a binding and an actor.
	 *
	 * @param value The submitted key value.  Can be <jk>null</jk>.
	 * @param action The action the submit is for.  Can be <jk>null</jk>.
	 * @param targetId The row the submit is for.  Can be <jk>null</jk>.
	 * @param principal The actor.  Can be <jk>null</jk> (then only a key issued without a principal verifies).
	 * @return The grant, or empty for every kind of miss.
	 */
	public Optional<Grant> verify(String value, String action, String targetId, String principal) {
		return check(value, action, targetId, principal);
	}

	private synchronized Optional<Grant> check(String value, String action, String targetId, String principal) {
		if (isBlank(value) || isBlank(action) || isBlank(targetId))
			return Optional.empty();
		sweep(clock.instant());
		var e = entries.get(value);
		if (e == null)
			return Optional.empty();
		var key = IdempotencyKey.of(value, e.action(), e.targetId());
		if (! key.matches(action, targetId) || ! Objects.equals(e.principal(), principal))
			return Optional.empty();
		return Optional.of(new Grant(key, Optional.ofNullable(e.jobId())));
	}

	private static boolean isBlank(String s) {
		return s == null || s.isBlank();
	}

	/**
	 * Records the job a key started, so a later verify carries it and the key can never start a second one.
	 *
	 * <p>
	 * Idempotent for the same job id.  A key that is no longer recorded is ignored.
	 *
	 * @param key The verified key.  Must not be <jk>null</jk>.
	 * @param jobId The job it started.  Must not be <jk>null</jk> or blank.
	 * @throws IllegalArgumentException If an argument is <jk>null</jk> or blank.
	 * @throws IllegalStateException If the key already started a different job.
	 */
	public synchronized void claim(IdempotencyKey key, String jobId) {
		reqnn("key", key);
		if (isBlank(jobId))
			throw iaex("Argument 'jobId' must not be blank.");
		var e = entries.get(key.value());
		if (e == null)
			return;
		if (e.jobId() != null && ! e.jobId().equals(jobId))
			throw new IllegalStateException("JobKeyLedger key already claimed by another job.");
		entries.put(key.value(), e.claimed(jobId));
	}

	/**
	 * The number of recorded keys, for tests and diagnostics.
	 *
	 * @return The entry count after a sweep.
	 */
	public synchronized int size() {
		sweep(clock.instant());
		return entries.size();
	}

	/**
	 * The key endpoint's response body: {@code {contractVersion, action, targetId, idempotencyKey}}.
	 *
	 * @param key The issued key.  Must not be <jk>null</jk>.
	 * @return An ordered map.
	 */
	public static Map<String,Object> response(IdempotencyKey key) {
		reqnn("key", key);
		var m = new LinkedHashMap<String,Object>();
		m.put("contractVersion", CONTRACT_VERSION);
		m.put("action", key.action());
		m.put("targetId", key.targetId());
		m.put("idempotencyKey", key.value());
		return m;
	}

	@Override /* Object */
	public synchronized String toString() {
		return "JobKeyLedger(size=" + entries.size() + ")";
	}
}
