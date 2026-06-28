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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.config.*;
import org.apache.juneau.rest.server.guard.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that all three ops-pack mixins compose cleanly on a single host with no path
 * collisions, that each mixin's {@code RestContext} is registered, and that the route-index
 * mixin's {@code /options} endpoint sees ops from every other mixin (filtered by
 * {@link OpSwagger#ignore() @OpSwagger(ignore=true)}).
 *
 * <p>
 * Setup: a single {@link RestServlet} host mounts all three ops mixins
 * ({@link EchoMixin}, {@link AdminMixin}, {@link RouteIndexMixin}) plus a
 * vanilla {@code /items} op of its own. {@code @Rest(debug=@Debug("always"))} unlocks the echo endpoint;
 * an empty {@code @Bean RestGuardList} factory replaces the {@link DenyAllGuard} default to
 * unlock the admin endpoints.
 *
 * <p>
 * Acceptance:
 * <ul>
 * 	<li>All three mixins appear in {@link RestContext#getMixinContexts()}.
 * 	<li>{@code /echo/}, {@code /admin/threads}, {@code /admin/heap}, {@code /options}, and the
 * 		host's {@code /items} all resolve.
 * 	<li>{@code /options} lists {@code /items} but excludes the ops-pack endpoints (all carry
 * 		{@code @OpSwagger(ignore=true)}) and itself.
 * </ul>
 *
 * @since 10.0.0
 */
class BasicOps_ParentChain_Test extends TestBase {

	@Rest(
		mixins={EchoMixin.class, AdminMixin.class, RouteIndexMixin.class},
		debug=@Debug("always"))
	public static class A extends RestServlet implements BasicUniversalConfig {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items", summary="List items") public String items() { return "items"; }

		// Allow-all guard chain — replaces the AdminMixin deny-all default.
		@Bean public RestGuardList guards(BeanStore bs) { return RestGuardList.create(bs).build(); }
	}

	private static final MockRestClient c = MockRestClient.buildLax(A.class);

	@Test void a01_allThreeMixinContextsRegistered() {
		MockRestClient.buildLax(A.class);
		var hostCtx = RestContext.getGlobalRegistry().get(A.class);
		var ctxs = hostCtx.getMixinContexts();
		assertNotNull(ctxs.get(EchoMixin.class), "Echo mixin context registered");
		assertNotNull(ctxs.get(AdminMixin.class), "Admin mixin context registered");
		assertNotNull(ctxs.get(RouteIndexMixin.class), "RouteIndex mixin context registered");
		assertEquals(3, ctxs.size(),
			"Expected exactly three mixin contexts; got: " + ctxs.keySet());
	}

	@Test void a02_echoServesAtPrimaryMount() throws Exception {
		// FINISHED-101: /debug/echo/* is no longer a multi-path default. Migration to that
		// secondary alias is covered by EchoMixin_SvlPathOverride_Test#a02.
		c.get("/echo/x").run().assertStatus(200);
		c.get("/debug/echo/x").run().assertStatus(404);
	}

	@Test void a03_adminThreadsResolves() throws Exception {
		c.get("/admin/threads").run().assertStatus(200);
	}

	@Test void a04_adminHeapResolves() throws Exception {
		c.get("/admin/heap").run().assertStatus(200);
	}

	@Test void a05_routeIndexOptionsResolves() throws Exception {
		var body = c.get("/options").accept("application/json").run().assertStatus(200).getContent().asString();
		// Host's own endpoint must appear.
		assertTrue(body.contains("/items"), "host /items must be in the index; body: " + body);
		// Ops endpoints must NOT appear (all carry @OpSwagger(ignore=true)).
		assertFalse(body.contains("/echo/"),
			"echo endpoint must be excluded from index; body: " + body);
		assertFalse(body.contains("/admin/threads"),
			"admin endpoints must be excluded from index; body: " + body);
		assertFalse(body.contains("/admin/heap"),
			"admin endpoints must be excluded from index; body: " + body);
		assertFalse(body.contains("/admin/cache/flush"),
			"admin endpoints must be excluded from index; body: " + body);
		assertFalse(body.contains("/admin/ratelimit"),
			"admin endpoints must be excluded from index; body: " + body);
		assertFalse(body.contains("/options"),
			"route-index endpoint must not echo itself; body: " + body);
	}

	@Test void a06_legacyRoutesAliasNotMountedByDefault() throws Exception {
		// FINISHED-101: /routes is no longer a multi-path default. Migration covered by
		// RouteIndexMixin_SvlPathOverride_Test#a02.
		c.get("/routes").run().assertStatus(404);
	}

	@Test void a07_hostEndpointStillReachable() throws Exception {
		c.get("/items").run().assertStatus(200).assertContent().asString().isContains("items");
	}
}
