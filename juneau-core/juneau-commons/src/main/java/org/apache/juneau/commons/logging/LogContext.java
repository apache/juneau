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
package org.apache.juneau.commons.logging;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

/**
 * A thread-confined, MDC-style key/value context whose entries are auto-attached to every {@link java.util.logging.LogRecord}
 * emitted by a {@link RichLogger} on the same thread while a scope is open.
 *
 * <p>
 * There is exactly one context per thread, shared across every {@link RichLogger} name &mdash; attachment happens in the
 * single central interception point ({@link RichLogger#log(java.util.logging.LogRecord)}), not per logger instance.  The
 * "on {@link RichLogger}" framing is about API home ({@link RichLogger#context()}), not about per-instance scoping.
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jk>try</jk> (LogContext.Scope <jv>c</jv> = RichLogger.<jsm>context</jsm>().with(<js>"requestId"</js>, <jv>id</jv>)) {
 * 		<jc>// Every record logged in this scope automatically carries requestId.</jc>
 * 		<jv>logger</jv>.info(<js>"processing"</js>);
 * 	}
 * </p>
 *
 * <h5 class='section'>Nesting semantics:</h5>
 * <p>
 * Classic MDC save/restore: {@link #with(String, Object)} records the key's <i>previous</i> value (or "absent") before
 * overwriting, and {@link Scope#close()} restores exactly that prior state &mdash; never a blanket clear.  Nested scopes on
 * the same key are safe: closing the inner scope restores the outer value, not the empty state.  {@link #with(Map)}
 * restores all entries in one {@link Scope#close()}.
 *
 * <h5 class='section'>Null policy:</h5>
 * <p>
 * {@link #with(String, Object)} rejects a <jk>null</jk> key.  A <jk>null</jk> value means "remove this key for the scope's
 * duration" &mdash; a <jk>null</jk> is never stored, so {@link #snapshot()} (an immutable {@link Map#copyOf(Map) copy}) never
 * blows up on a null key or value.
 *
 * <h5 class='section'>Thread confinement (v1):</h5>
 * <p>
 * This context is <b>thread-confined only</b>.  It is not inherited by child threads, not propagated across executor or
 * {@link java.util.concurrent.CompletableFuture} boundaries, and is <b>not</b> SLF4J MDC and is <b>not</b> restored by
 * {@code MdcAsyncListener}.  Apps that rely on SLF4J MDC on an async-completion thread keep it; apps that rely on
 * {@code LogContext} do not (except the REST debug-pipeline completion record, which carries it explicitly).
 *
 * <h5 class='section'>Scope discipline:</h5>
 * <p>
 * A {@link Scope} must be closed on the same thread that opened it (use try-with-resources).  A forgotten
 * {@link Scope#close()} leaves the entry live on that thread and is visible on the next {@link #get(String)} on the same
 * thread &mdash; the leak is the caller's, not the framework's.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link RichLogger#context()}
 * 	<li class='jc'>{@link LogRecordContext}
 * </ul>
 */
public final class LogContext {

	/** The single shared instance.  There is exactly one thread-local map; sharing the instance is harmless. */
	static final LogContext INSTANCE = new LogContext();

	private static final ThreadLocal<Map<String,Object>> CONTEXT = ThreadLocal.withInitial(LinkedHashMap::new);

	private LogContext() {}

	/**
	 * Opens a scope that sets a single entry.
	 *
	 * @param key The entry key.  Must not be <jk>null</jk>.
	 * @param value The entry value.  A <jk>null</jk> value removes the key for the scope's duration.
	 * @return A scope that restores the prior state of this key on {@link Scope#close()}.  Never <jk>null</jk>.
	 */
	@SuppressWarnings({
		"resource" // Returns a Scope the caller must close via try-with-resources; Eclipse JDT @Owning warning is by design.
	})
	public Scope with(String key, Object value) {
		if (key == null)
			throw iaex("Argument 'key' cannot be null.");
		var map = CONTEXT.get();
		var hadPrior = map.containsKey(key);
		var prior = map.get(key);
		apply(map, key, value);
		return new SingleScope(key, hadPrior, prior);
	}

	/**
	 * Opens a scope that sets a batch of entries atomically.
	 *
	 * <p>
	 * All entries are restored to their prior state in one {@link Scope#close()} (not N chained scopes), so a batch
	 * caller does not need nested try-with-resources for multiple keys set together.
	 *
	 * @param entries The entries to set.  Must not be <jk>null</jk> and must not contain a <jk>null</jk> key.  A
	 * 	<jk>null</jk> value removes that key for the scope's duration.
	 * @return A scope that restores the prior state of all supplied keys on {@link Scope#close()}.  Never <jk>null</jk>.
	 */
	@SuppressWarnings({
		"resource" // Returns a Scope the caller must close via try-with-resources; Eclipse JDT @Owning warning is by design.
	})
	public Scope with(Map<String,Object> entries) {
		if (entries == null)
			throw iaex("Argument 'entries' cannot be null.");
		for (var key : entries.keySet())
			if (key == null)
				throw iaex("Context key cannot be null.");
		var map = CONTEXT.get();
		var priors = new LinkedHashMap<String,Object>();
		var hadPrior = new LinkedHashMap<String,Boolean>();
		for (var e : entries.entrySet()) {
			var key = e.getKey();
			hadPrior.put(key, map.containsKey(key));
			priors.put(key, map.get(key));
		}
		for (var e : entries.entrySet())
			apply(map, e.getKey(), e.getValue());
		return new BatchScope(priors, hadPrior);
	}

	/**
	 * Returns the current value for the specified key on this thread.
	 *
	 * @param key The entry key.
	 * @return The value, or <jk>null</jk> if not set on this thread.
	 */
	public Object get(String key) {
		return CONTEXT.get().get(key);
	}

	/**
	 * Returns an immutable snapshot of this thread's context at this instant.
	 *
	 * @return An immutable copy of the current entries, or the shared empty {@link Map#of()} singleton when empty.
	 */
	public Map<String,Object> snapshot() {
		var map = CONTEXT.get();
		return map.isEmpty() ? Map.of() : Map.copyOf(map);
	}

	private static void apply(Map<String,Object> map, String key, Object value) {
		if (value == null)
			map.remove(key);
		else
			map.put(key, value);
	}

	private static void restore(Map<String,Object> map, String key, boolean hadPrior, Object prior) {
		if (hadPrior)
			map.put(key, prior);
		else
			map.remove(key);
	}

	/**
	 * An open context scope.  Closing it restores the thread's context to the state it had before the scope opened.
	 */
	public interface Scope extends AutoCloseable {
		@Override
		void close();
	}

	private static final class SingleScope implements Scope {
		private final String key;
		private final boolean hadPrior;
		private final Object prior;
		private boolean closed;

		SingleScope(String key, boolean hadPrior, Object prior) {
			this.key = key;
			this.hadPrior = hadPrior;
			this.prior = prior;
		}

		@Override
		public void close() {
			if (closed)
				return;
			closed = true;
			restore(CONTEXT.get(), key, hadPrior, prior);
		}
	}

	private static final class BatchScope implements Scope {
		private final Map<String,Object> priors;
		private final Map<String,Boolean> hadPrior;
		private boolean closed;

		BatchScope(Map<String,Object> priors, Map<String,Boolean> hadPrior) {
			this.priors = priors;
			this.hadPrior = hadPrior;
		}

		@Override
		public void close() {
			if (closed)
				return;
			closed = true;
			var map = CONTEXT.get();
			for (var key : priors.keySet())
				restore(map, key, hadPrior.get(key), priors.get(key));
		}
	}
}
