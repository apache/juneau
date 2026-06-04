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

import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.guard.*;
import org.apache.juneau.rest.servlet.*;

/**
 * Standalone servlet companion of the {@link AdminMixin} mixin.
 *
 * <p>
 * Mounts as a <b>sibling top-level servlet</b> at {@code /admin/*} and serves the same operational
 * endpoints ({@code /threads}, {@code /heap}, {@code /cache/flush}, {@code /ratelimit}) as the
 * mixin by delegating to a shared flavor-neutral {@link AdminProvider} worker bean &mdash; so the two forms
 * cannot drift.
 *
 * <p>
 * Whereas the {@link AdminMixin} mixin pins its ops at {@code /admin/<endpoint>} for
 * composition into a host at {@code /}, this companion declares the mount at the class level
 * ({@link Rest#paths() @Rest(paths="/admin/*")}) and pins the ops at the bare endpoint names
 * (e.g. {@code /threads}) &mdash; eliminating the path-doubling that mixin-shaped ops would produce
 * when the servlet itself is mounted at {@code /admin/*}.
 *
 * <h5 class='section'>Default-deny security posture:</h5>
 *
 * <p>
 * Like the mixin, this servlet is annotated {@link Rest#guards() @Rest(guards=DenyAllGuard.class)},
 * so every admin path returns {@code 403 Forbidden} until the importer registers a
 * {@link RestGuardList} factory. Pair the servlet with whatever authentication / authorization
 * story your service uses.
 *
 * <h5 class='figure'>Microservice registration:</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Bean</ja> Servlet admin() {
 * 		<jk>return new</jk> AdminServlet();  <jc>// auto-mounted at /admin/* by JettyServerComponent</jc>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link AdminMixin}
 * 	<li class='jc'>{@link AdminProvider}
 * 	<li class='jc'>{@link DenyAllGuard}
 * 	<li class='jc'>{@link RestGuardList}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @serial exclude
 * @since 10.0.0
 */
// @formatter:off
@Rest(
	paths="/admin/*",
	guards=DenyAllGuard.class
)
public class AdminServlet extends RestServlet {

	private static final long serialVersionUID = 1L;

	private final transient AdminProvider worker;

	/** No-arg constructor &mdash; uses a default {@link AdminProvider} worker. */
	public AdminServlet() {
		this(new AdminProvider());
	}

	/**
	 * Worker constructor.
	 *
	 * @param worker The shared flavor-neutral admin worker this servlet delegates to. Must not be
	 * 	{@code null}.
	 */
	protected AdminServlet(AdminProvider worker) {
		this.worker = worker;
	}

	/**
	 * [GET /threads] &mdash; emit a JSON list of currently-live threads.
	 *
	 * @param res The current REST response.
	 * @throws IOException If an I/O error occurs while writing the response.
	 */
	@RestGet(
		path="/threads",
		summary="Thread dump",
		description="JSON list of currently-live threads (filtered to exclude framework noise by default).",
		swagger=@OpSwagger(ignore=true)
	)
	public void getThreads(RestResponse res) throws IOException {
		worker.serveThreads(res);
	}

	/**
	 * [GET /heap] &mdash; emit JVM heap and non-heap memory statistics.
	 *
	 * @param res The current REST response.
	 * @throws IOException If an I/O error occurs while writing the response.
	 */
	@RestGet(
		path="/heap",
		summary="Heap statistics",
		description="JVM heap + non-heap memory statistics (Runtime + MemoryMXBean).",
		swagger=@OpSwagger(ignore=true)
	)
	public void getHeap(RestResponse res) throws IOException {
		worker.serveHeap(res);
	}

	/**
	 * [POST /cache/flush] &mdash; run all registered cache-flush hooks (or a subset).
	 *
	 * @param req The current REST request &mdash; {@code names} query parameter is read off it.
	 * @param res The current REST response.
	 * @throws IOException If an I/O error occurs while writing the response.
	 */
	@RestPost(
		path="/cache/flush",
		summary="Cache flush",
		description="Runs the registered cache-flush hooks (all by default; ?names=foo,bar for a subset).",
		swagger=@OpSwagger(ignore=true)
	)
	public void postCacheFlush(RestRequest req, RestResponse res) throws IOException {
		worker.serveCacheFlush(req, res);
	}

	/**
	 * [GET /ratelimit] &mdash; emit registered {@link RateLimitGuard} bean configuration.
	 *
	 * @param req The current REST request &mdash; supplies the bean store for guard lookup.
	 * @param res The current REST response.
	 * @throws IOException If an I/O error occurs while writing the response.
	 * @throws NotFound If no {@link RateLimitGuard} bean is registered on the importer's bean
	 * 	store.
	 */
	@RestGet(
		path="/ratelimit",
		summary="Rate-limit inspection",
		description="JSON map of registered RateLimitGuard beans keyed by bean name.",
		swagger=@OpSwagger(ignore=true)
	)
	public void getRateLimit(RestRequest req, RestResponse res) throws IOException {
		worker.serveRateLimit(req, res);
	}
}
