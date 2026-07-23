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
package org.apache.juneau.rest.server.ops;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.io.*;
import java.lang.management.*;
import java.util.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.guard.*;
import org.apache.juneau.rest.server.guard.RateLimitGuard.*;

/**
 * Flavor-neutral operational-introspection worker bean shared by the admin {@code @Rest} flavors.
 *
 * <p>
 * Holds the capability state (the name-keyed cache-flush hooks + the thread-name-prefix exclude list)
 * and the serve logic for the four admin endpoints (thread dump, heap statistics, cache flush, and
 * rate-limit-bucket inspection). The {@link AdminMixin} (mixin), {@link AdminResource} (child), and
 * {@link AdminServlet} (servlet) flavors are independent {@code @Rest} classes that each hold an
 * {@code AdminProvider} worker and delegate to it &mdash; so the three deployment forms cannot drift, and no
 * flavor is another flavor's worker.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link AdminMixin}
 * 	<li class='jc'>{@link AdminResource}
 * 	<li class='jc'>{@link AdminServlet}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 10.0.0
 */
// @formatter:off
public class AdminProvider {

	/**
	 * Default thread-name-prefix exclude list: filter out JVM internals, servlet container, and
	 * Spring Boot infrastructure threads.
	 */
	public static final List<String> DEFAULT_THREAD_NAME_PREFIX_EXCLUDE = List.of(
		"Reference Handler", "Finalizer", "Signal Dispatcher", "Common-Cleaner",
		"Notification Thread", "Attach Listener",
		"jetty-", "qtp",
		"spring-",
		"GC ", "G1 ");

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	private final Map<String,Runnable> cacheFlushHooks;
	private final List<String> threadNamePrefixExclude;

	/** No-arg constructor &mdash; uses the default thread filter and no cache-flush hooks. */
	public AdminProvider() {
		this(create());
	}

	/**
	 * Builder constructor.
	 *
	 * @param builder The builder. Must not be {@code null}.
	 */
	protected AdminProvider(Builder builder) {
		cacheFlushHooks = u(cp(builder.cacheFlushHooks));
		threadNamePrefixExclude = List.copyOf(builder.threadNamePrefixExclude);
	}

	/**
	 * Serves the thread dump as a JSON list of currently-live threads.
	 *
	 * @param res The current REST response. Must not be {@code null}.
	 * @throws IOException If an I/O error occurs while writing the response.
	 */
	@SuppressWarnings({
		"java:S1874" // Thread.getId() is not deprecated on the Java 17 baseline; its replacement Thread.threadId() was only added in Java 19, so getId() is the correct call here.
	})
	public void serveThreads(RestResponse res) throws IOException {
		var allTraces = Thread.getAllStackTraces();
		var out = new ArrayList<Map<String,Object>>();
		for (var e : allTraces.entrySet()) {
			var t = e.getKey();
			if (isExcludedThread(t.getName()))
				continue;
			var entry = new LinkedHashMap<String,Object>();
			entry.put("name", t.getName());
			entry.put("id", t.getId());
			entry.put("state", t.getState().toString());
			entry.put("daemon", t.isDaemon());
			entry.put("priority", t.getPriority());
			var stack = new ArrayList<String>();
			for (var f : e.getValue())
				stack.add(f.toString());
			entry.put("stack", stack);
			out.add(entry);
		}
		writeJson(res, out);
	}

	/**
	 * Serves JVM heap and non-heap memory statistics.
	 *
	 * @param res The current REST response. Must not be {@code null}.
	 * @throws IOException If an I/O error occurs while writing the response.
	 */
	public void serveHeap(RestResponse res) throws IOException {
		var rt = Runtime.getRuntime();
		var heap = new LinkedHashMap<String,Object>();
		heap.put("total", rt.totalMemory());
		heap.put("free", rt.freeMemory());
		heap.put("max", rt.maxMemory());
		heap.put("used", rt.totalMemory() - rt.freeMemory());

		var mx = ManagementFactory.getMemoryMXBean();
		var nonHeapUsage = mx.getNonHeapMemoryUsage();
		var nonHeap = new LinkedHashMap<String,Object>();
		nonHeap.put("init", nonHeapUsage.getInit());
		nonHeap.put("used", nonHeapUsage.getUsed());
		nonHeap.put("committed", nonHeapUsage.getCommitted());
		nonHeap.put("max", nonHeapUsage.getMax());

		var out = new LinkedHashMap<String,Object>();
		out.put("heap", heap);
		out.put("nonHeap", nonHeap);
		out.put("availableProcessors", rt.availableProcessors());
		writeJson(res, out);
	}

	/**
	 * Runs all registered cache-flush hooks (or a subset).
	 *
	 * <p>
	 * When {@code names} is supplied, only the named hooks are invoked. Unknown names are
	 * silently ignored (404-on-unknown would leak the registered hook set). Hook execution is
	 * synchronous; long-running hooks block the request thread.
	 *
	 * @param req The current REST request &mdash; {@code names} query parameter is read off it. Must not be {@code null}.
	 * @param res The current REST response. Must not be {@code null}.
	 * @throws IOException If an I/O error occurs while writing the response.
	 */
	public void serveCacheFlush(RestRequest req, RestResponse res) throws IOException {
		var namesParam = req.getQueryParams().get("names").asString().orElse(null);
		Set<String> selected = null;
		if (namesParam != null && ! namesParam.isBlank()) {
			selected = new LinkedHashSet<>();
			for (var n : namesParam.split(","))
				if (! n.isBlank())
					selected.add(n.trim());
		}
		var executed = new ArrayList<String>();
		for (var e : cacheFlushHooks.entrySet()) {
			if (selected != null && ! selected.contains(e.getKey()))
				continue;
			e.getValue().run();
			executed.add(e.getKey());
		}
		var out = new LinkedHashMap<String,Object>();
		out.put("registered", new ArrayList<>(cacheFlushHooks.keySet()));
		out.put("executed", executed);
		writeJson(res, out);
	}

	/**
	 * Serves the registered {@link RateLimitGuard} bean configuration.
	 *
	 * <p>
	 * Returns {@code 404 Not Found} when no {@code RateLimitGuard} bean is registered.
	 *
	 * @param req The current REST request &mdash; supplies the bean store for guard lookup. Must not be {@code null}.
	 * @param res The current REST response. Must not be {@code null}.
	 * @throws IOException If an I/O error occurs while writing the response.
	 * @throws NotFound If no {@link RateLimitGuard} bean is registered on the importer's bean store.
	 */
	public void serveRateLimit(RestRequest req, RestResponse res) throws IOException {
		var bs = req.getContext().getBeanStore();
		var guards = collectRateLimitGuards(bs);
		if (guards.isEmpty())
			throw new NotFound("No RateLimitGuard bean is registered.");
		var entries = new LinkedHashMap<String,Object>();
		for (var e : guards.entrySet()) {
			var g = e.getValue();
			var bucket = new LinkedHashMap<String,Object>();
			bucket.put("config", describeRateLimitGuard(g));
			bucket.put("snapshot", g.snapshot().values().stream()
				.sorted(Comparator.comparing(BucketState::key))
				.toList());
			entries.put(e.getKey(), bucket);
		}
		var out = new LinkedHashMap<String,Object>();
		out.put("guards", entries);
		writeJson(res, out);
	}

	/**
	 * Returns the registered cache-flush hooks (test/inspection helper).
	 *
	 * @return The hooks, keyed by registration name. Never {@code null}.
	 */
	public Map<String,Runnable> getCacheFlushHooks() {
		return cacheFlushHooks;
	}

	/**
	 * Returns the configured thread-name-prefix exclude list (test/inspection helper).
	 *
	 * @return The exclude list. Never {@code null}.
	 */
	public List<String> getThreadNamePrefixExclude() {
		return threadNamePrefixExclude;
	}

	private boolean isExcludedThread(String threadName) {
		for (var prefix : threadNamePrefixExclude)
			if (threadName != null && threadName.startsWith(prefix))
				return true;
		return false;
	}

	private static Map<String,RateLimitGuard> collectRateLimitGuards(BeanStore bs) {
		// Multi-bean lookup so users running per-tier guards (free / paid / etc.) see every
		// registered bean keyed by its bean name. Falls through to the single-bean path when no
		// per-tier configuration is registered.
		var byName = bs.getBeansOfType(RateLimitGuard.class);
		if (byName != null && ! byName.isEmpty())
			return new LinkedHashMap<>(byName);
		var out = new LinkedHashMap<String,RateLimitGuard>();
		bs.getBean(RateLimitGuard.class).ifPresent(g -> out.put("rateLimit", g));
		return out;
	}

	private static Map<String,Object> describeRateLimitGuard(RateLimitGuard g) {
		var m = new LinkedHashMap<String,Object>();
		m.put("class", cn(g));
		m.put("limit", g.getCapacity());
		m.put("permitsPerSecond", g.getPermitsPerSecond());
		m.put("xForwardedForAware", g.isXForwardedForAware());
		m.put("exemptPaths", g.getExemptPaths().stream().sorted().toList());
		return m;
	}

	private static void writeJson(RestResponse res, Object payload) throws IOException {
		try (var w = res.getDirectWriter("application/json")) {
			JsonSerializer.DEFAULT_READABLE.write(payload, w);
		}
	}

	/**
	 * Builder for {@link AdminProvider} instances.
	 */
	public static class Builder {

		private final Map<String,Runnable> cacheFlushHooks = m();
		private final List<String> threadNamePrefixExclude = new ArrayList<>(DEFAULT_THREAD_NAME_PREFIX_EXCLUDE);

		/** Constructor &mdash; package access for {@link AdminProvider#create()}. */
		protected Builder() {}

		/**
		 * Registers a cache-flush hook under a name.
		 *
		 * <p>
		 * The hook is invoked synchronously when {@code POST /admin/cache/flush} is called (with
		 * no {@code names} parameter, or with a {@code names} parameter that includes this name).
		 *
		 * @param name The hook name. Must not be {@code null} or blank.
		 * @param hook The hook. Must not be {@code null}.
		 * @return This object.
		 */
		public Builder cacheFlush(String name, Runnable hook) {
			if (isBlank(name))
				throw new IllegalArgumentException("Argument 'name' must not be null or blank");
			if (hook == null)
				throw new IllegalArgumentException("Argument 'hook' must not be null");
			cacheFlushHooks.put(name, hook);
			return this;
		}

		/**
		 * Registers multiple cache-flush hooks at once.
		 *
		 * @param hooks The hooks, keyed by registration name. Can be {@code null} (no-op).
		 * @return This object.
		 */
		public Builder cacheFlushAll(Map<String,Runnable> hooks) {
			if (hooks != null)
				hooks.forEach(this::cacheFlush);
			return this;
		}

		/**
		 * Replaces the thread-name-prefix exclude list.
		 *
		 * <p>
		 * Threads whose {@link Thread#getName() name} starts with any of the supplied prefixes are
		 * omitted from the {@code /admin/threads} output. Pass an empty array to disable
		 * filtering entirely.
		 *
		 * @param values The thread-name prefixes to exclude. Can be {@code null} (equivalent to an empty
		 * 	array &mdash; disables filtering); {@code null} or empty elements are skipped.
		 * @return This object.
		 */
		public Builder threadNamePrefixExclude(String...values) {
			threadNamePrefixExclude.clear();
			if (values != null)
				for (var v : values)
					if (v != null && ! v.isEmpty())
						threadNamePrefixExclude.add(v);
			return this;
		}

		/**
		 * Builds an {@link AdminProvider} instance.
		 *
		 * @return A configured instance.
		 */
		public AdminProvider build() {
			return new AdminProvider(this);
		}
	}
}
