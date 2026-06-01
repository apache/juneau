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
package org.apache.juneau.rest.ops;

import java.io.*;
import java.lang.management.*;
import java.util.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.guard.*;
import org.apache.juneau.rest.guard.RateLimitGuard.BucketState;

/**
 * Mixin that serves operational-introspection endpoints under {@code /admin/*}: thread dump,
 * heap statistics, configurable cache-flush hooks, and rate-limit-bucket inspection.
 *
 * <p>
 * Sibling of {@link EchoMixin} ({@code /echo/*} / {@code /debug/echo/*}) and
 * {@link RouteIndexMixin} ({@code /options} / {@code /routes}). All three classes live in
 * the {@code org.apache.juneau.rest.ops} ops/introspection mixin pack.
 *
 * <h5 class='section'>Default-deny security posture:</h5>
 *
 * <p>
 * The mixin is annotated with {@link Rest#guards() @Rest(guards=DenyAllGuard.class)}, so every
 * admin path returns {@code 403 Forbidden} until the importer registers a
 * {@link org.apache.juneau.commons.inject.Bean @Bean} {@link RestGuardList} factory on the host.
 * The framework's bean-store override seam <b>replaces</b> the entire annotation-derived guard
 * list (including this deny-all) with the user-supplied chain &mdash; pair the mixin with whatever
 * authentication / authorization story your service uses (bearer-token guard, API-key guard,
 * Spring Security adapter, etc.).
 *
 * <p>
 * <b>Why deny-all rather than a non-existent role-name placeholder?</b> The role-guard approach
 * (declaring a role that no real principal has) has clean semantics but harder ergonomics: the
 * importer overrides by setting the principal's roles to include the placeholder, which couples
 * the host's auth strategy to a framework-internal role name. Replacing a {@link RestGuardList}
 * via {@code @Bean} keeps the override surface narrow &mdash; one factory method on the host
 * &mdash; and matches the way every other Juneau op-context bean is wired.
 *
 * <h5 class='section'>Configurable mount prefix:</h5>
 *
 * <p>
 * The default mount prefix {@code /admin} can be overridden via the SVL variable
 * {@code ${juneau.admin.path:admin}} &mdash; a single override relocates all four endpoints
 * ({@code /threads}, {@code /heap}, {@code /cache/flush}, {@code /ratelimit}) under the new
 * prefix. Set via system property ({@code -Djuneau.admin.path=ops}), environment variable
 * ({@code JUNEAU_ADMIN_PATH=ops}), or {@code Config} key
 * ({@code juneau.admin.path = ops}) to change the runtime mount without subclassing.
 * Resolution happens once at {@link RestContext} construction time; see the FINISHED-99 archive
 * (SVL resolution in {@code @RestOp(path)}) for the full resolution chain.
 *
 * <p>
 * Override accepts bare token ({@code admin}), leading slash ({@code /admin}), trailing slash
 * ({@code admin/}), or wildcard suffix ({@code /admin/*}) &mdash; all resolve to the same mount.
 *
 * <h5 class='section'>Mixin-only deployment:</h5>
 *
 * <p>
 * This resource is designed for composition via {@code @Rest(mixins=...)}. The four mount paths
 * ({@code /admin/threads}, {@code /admin/heap}, {@code /admin/cache/flush},
 * {@code /admin/ratelimit}) are pinned at the op level by {@code @RestGet/@RestPost(path=...)}
 * on the handler methods; a class-level {@code @Rest(paths=...)} declaration would be silently
 * ignored under the mixin pattern (see {@link Rest#paths() @Rest(paths)} Javadoc).
 *
 * <h5 class='figure'>Composition example:</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/api"</js>, mixins=AdminMixin.<jk>class</jk>)
 * 	<jk>public class</jk> ApiResource <jk>extends</jk> RestServlet {
 *
 * 		<jc>// Required: register an auth guard chain to unlock the admin paths.</jc>
 * 		<ja>@Bean</ja>(name=<js>"guards"</js>)
 * 		<jk>public</jk> RestGuardList guards(BeanStore <jv>bs</jv>) {
 * 			<jk>return</jk> RestGuardList.<jsm>create</jsm>(<jv>bs</jv>)
 * 				.append(<jk>new</jk> MyBearerTokenGuard())
 * 				.build();
 * 		}
 *
 * 		<ja>@Bean</ja> AdminMixin admin() {
 * 			<jk>return</jk> AdminMixin.<jsm>create</jsm>()
 * 				.cacheFlush(<js>"primary"</js>, () -&gt; primaryCache.invalidateAll())
 * 				.build();
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>Endpoints:</h5>
 *
 * <ul class='spaced-list'>
 * 	<li><b>{@code GET /admin/threads}</b> &mdash; emits a JSON list of currently-live threads. The
 * 		default thread filter excludes framework noise (JVM internals, servlet container, Spring
 * 		Boot infrastructure); override via {@link Builder#threadNamePrefixExclude(String...)}.
 * 	<li><b>{@code GET /admin/heap}</b> &mdash; emits a JSON map with {@code Runtime} heap stats
 * 		({@code total}, {@code free}, {@code max}, {@code used}) plus
 * 		{@link MemoryMXBean#getNonHeapMemoryUsage() non-heap} memory usage. No heap-dump file
 * 		generation in v1 (security risk).
 * 	<li><b>{@code POST /admin/cache/flush}</b> &mdash; runs all registered cache-flush hooks, or
 * 		just a comma-separated {@code names} subset when supplied as a query parameter. Returns a
 * 		JSON map of the hooks that were invoked. Hooks are registered name-keyed via
 * 		{@link Builder#cacheFlush(String,Runnable)}; users that want async semantics own the
 * 		threading model.
 * 	<li><b>{@code GET /admin/ratelimit}</b> &mdash; emits a JSON map keyed by bean name listing the
 * 		registered {@link RateLimitGuard} configuration and live per-bucket state. Each entry has
 * 		two sub-fields: {@code config} (the guard's static configuration &mdash; {@code class},
 * 		{@code limit}, {@code permitsPerSecond}, {@code xForwardedForAware}, {@code exemptPaths})
 * 		and {@code snapshot} (a sorted array of {@link BucketState} entries describing every
 * 		per-key bucket the storage backend currently tracks). Returns {@code 404 Not Found} when
 * 		no {@code RateLimitGuard} bean is registered on the importer's bean store. Storage
 * 		backends that don't override {@link RateLimitGuard.Storage#snapshot()} (e.g. Redis-backed
 * 		impls that can't cheaply enumerate buckets) emit an empty {@code snapshot} array.
 * </ul>
 *
 * <p>
 * All four endpoints carry {@link OpSwagger#ignore() @OpSwagger(ignore=true)} so the admin surface
 * stays out of any generated Swagger / OpenAPI spec.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link EchoMixin}
 * 	<li class='jc'>{@link RouteIndexMixin}
 * 	<li class='jc'>{@link DenyAllGuard}
 * 	<li class='jc'>{@link RestGuardList}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 9.5.0
 */
// @formatter:off
@Rest(
	guards=DenyAllGuard.class
)
public class AdminMixin {

	/**
	 * Default thread-name-prefix exclude list: filter out JVM internals, servlet container, and
	 * Spring Boot infrastructure threads.
	 */
	public static final List<String> DEFAULT_THREAD_NAME_PREFIX_EXCLUDE = AdminProvider.DEFAULT_THREAD_NAME_PREFIX_EXCLUDE;

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	private final AdminProvider worker;

	/**
	 * No-arg constructor &mdash; delegates to a default {@link AdminProvider} worker (default thread filter
	 * and no cache-flush hooks).
	 */
	public AdminMixin() {
		this(new AdminProvider());
	}

	/**
	 * Worker constructor.
	 *
	 * @param worker The shared {@link AdminProvider} worker this flavor delegates to. Must not be
	 * 	<jk>null</jk>.
	 */
	protected AdminMixin(AdminProvider worker) {
		this.worker = worker;
	}

	/**
	 * [GET /admin/threads] &mdash; emit a JSON list of currently-live threads.
	 *
	 * @param res The current REST response.
	 * @throws IOException If an I/O error occurs while writing the response.
	 */
	@RestGet(
		path="/#{pathToken(${juneau.admin.path:admin})}/threads",
		summary="Thread dump",
		description="JSON list of currently-live threads (filtered to exclude framework noise by default).",
		swagger=@OpSwagger(ignore=true)
	)
	public void getThreads(RestResponse res) throws IOException {
		worker.serveThreads(res);
	}

	/**
	 * [GET /admin/heap] &mdash; emit JVM heap and non-heap memory statistics.
	 *
	 * @param res The current REST response.
	 * @throws IOException If an I/O error occurs while writing the response.
	 */
	@RestGet(
		path="/#{pathToken(${juneau.admin.path:admin})}/heap",
		summary="Heap statistics",
		description="JVM heap + non-heap memory statistics (Runtime + MemoryMXBean).",
		swagger=@OpSwagger(ignore=true)
	)
	public void getHeap(RestResponse res) throws IOException {
		worker.serveHeap(res);
	}

	/**
	 * [POST /admin/cache/flush] &mdash; run all registered cache-flush hooks (or a subset).
	 *
	 * @param req The current REST request &mdash; {@code names} query parameter is read off it.
	 * @param res The current REST response.
	 * @throws IOException If an I/O error occurs while writing the response.
	 */
	@RestPost(
		path="/#{pathToken(${juneau.admin.path:admin})}/cache/flush",
		summary="Cache flush",
		description="Runs the registered cache-flush hooks (all by default; ?names=foo,bar for a subset).",
		swagger=@OpSwagger(ignore=true)
	)
	public void postCacheFlush(RestRequest req, RestResponse res) throws IOException {
		worker.serveCacheFlush(req, res);
	}

	/**
	 * [GET /admin/ratelimit] &mdash; emit registered {@link RateLimitGuard} bean configuration.
	 *
	 * <p>
	 * Returns {@code 404 Not Found} when no {@code RateLimitGuard} bean is registered.
	 *
	 * @param req The current REST request &mdash; supplies the bean store for guard lookup.
	 * @param res The current REST response.
	 * @throws IOException If an I/O error occurs while writing the response.
	 */
	@RestGet(
		path="/#{pathToken(${juneau.admin.path:admin})}/ratelimit",
		summary="Rate-limit inspection",
		description="JSON map of registered RateLimitGuard beans keyed by bean name.",
		swagger=@OpSwagger(ignore=true)
	)
	public void getRateLimit(RestRequest req, RestResponse res) throws IOException {
		worker.serveRateLimit(req, res);
	}

	/**
	 * Returns the registered cache-flush hooks (test/inspection helper).
	 *
	 * @return The hooks, keyed by registration name. Never {@code null}.
	 */
	public Map<String,Runnable> getCacheFlushHooks() {
		return worker.getCacheFlushHooks();
	}

	/**
	 * Returns the configured thread-name-prefix exclude list (test/inspection helper).
	 *
	 * @return The exclude list. Never {@code null}.
	 */
	public List<String> getThreadNamePrefixExclude() {
		return worker.getThreadNamePrefixExclude();
	}

	/**
	 * Builder for {@link AdminMixin} instances.
	 *
	 * <p>
	 * Mirrors {@link AdminProvider.Builder}'s configuration methods on the mixin's own surface and forwards
	 * each call to an underlying {@link AdminProvider.Builder}, which builds the shared worker the mixin
	 * delegates to (TODO-145 &sect;2.3.1 / OQ-11).
	 */
	public static class Builder {

		private final AdminProvider.Builder worker = AdminProvider.create();

		/** Constructor &mdash; package access for {@link AdminMixin#create()}. */
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
			worker.cacheFlush(name, hook);
			return this;
		}

		/**
		 * Registers multiple cache-flush hooks at once.
		 *
		 * @param hooks The hooks, keyed by registration name.
		 * @return This object.
		 */
		public Builder cacheFlushAll(Map<String,Runnable> hooks) {
			worker.cacheFlushAll(hooks);
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
		 * @param values The thread-name prefixes to exclude. Must not be {@code null}.
		 * @return This object.
		 */
		public Builder threadNamePrefixExclude(String...values) {
			worker.threadNamePrefixExclude(values);
			return this;
		}

		/**
		 * Builds an {@link AdminMixin} instance.
		 *
		 * @return A configured instance.
		 */
		public AdminMixin build() {
			return new AdminMixin(worker.build());
		}
	}
}
