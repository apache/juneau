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
package org.apache.juneau.rest.mixin;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Phase 1 — exercises the per-mixin {@link RestContext} sub-context construction infrastructure introduced for
 * {@code @Rest(mixins=...)}.
 */
class MixinContext_Construction_Test extends TestBase {

	@Rest
	public static class M1 {
		@RestGet(path="/m1") public String m1() { return "m1"; }
	}

	@Rest
	public static class M2 {
		@RestGet(path="/m2") public String m2() { return "m2"; }
	}

	@Rest(mixins={M1.class}, noInherit={"mixins"})
	public static class HostOne extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/host") public String host() { return "host"; }
	}

	@Rest(mixins={M1.class, M2.class}, noInherit={"mixins"})
	public static class HostMulti extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Rest(mixins={M1.class}, noInherit={"mixins"})
	public static class HostNoOps extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_hostHasOneMixinContext() throws Exception {
		var c = MockRestClient.buildLax(HostOne.class);
		c.get("/m1").run().assertStatus(200);
		var ctx = RestContext.getGlobalRegistry().get(HostOne.class);
		assertNotNull(ctx, "Host RestContext should be registered after MockRestClient build");
		var mixinContexts = ctx.getMixinContexts();
		assertEquals(1, mixinContexts.size(), "Expected exactly one mixin sub-context (noInherit cuts off BasicRestServlet's docs mixins)");
		var m1Ctx = mixinContexts.get(M1.class);
		assertNotNull(m1Ctx, "Missing mixin sub-context for M1");
		assertSame(ctx, m1Ctx.getParentContext(), "Mixin sub-context parent should be the host context");
		assertEquals(M1.class, m1Ctx.getResourceClass(), "Mixin sub-context resourceClass should be the mixin class");
		assertTrue(m1Ctx.isMixinContext(), "Sub-context should be flagged as a mixin context");
		assertFalse(ctx.isMixinContext(), "Host context must not be flagged as a mixin context");
	}

	@Test void a02_hostHasMultipleMixinContexts() throws Exception {
		var c = MockRestClient.buildLax(HostMulti.class);
		c.get("/m1").run().assertStatus(200);
		c.get("/m2").run().assertStatus(200);
		var ctx = RestContext.getGlobalRegistry().get(HostMulti.class);
		assertNotNull(ctx);
		var mixinContexts = ctx.getMixinContexts();
		assertEquals(2, mixinContexts.size(), "Expected exactly two mixin sub-contexts (noInherit cuts off BasicRestServlet's docs mixins)");
		assertNotNull(mixinContexts.get(M1.class));
		assertNotNull(mixinContexts.get(M2.class));
		assertNotSame(mixinContexts.get(M1.class), mixinContexts.get(M2.class),
			"Each mixin must get its own RestContext instance");
	}

	@Test void a03_mixinContextsAreEmptyOnMixinContextItself() {
		MockRestClient.buildLax(HostNoOps.class);
		var ctx = RestContext.getGlobalRegistry().get(HostNoOps.class);
		var m1Ctx = ctx.getMixinContexts().get(M1.class);
		assertNotNull(m1Ctx);
		assertTrue(m1Ctx.getMixinContexts().isEmpty(),
			"Mixin sub-contexts must not discover their own mixins (flat-inheritance rule)");
	}

	@Test void a04_mixinResourceInstanceIsConstructed() {
		MockRestClient.buildLax(HostOne.class);
		var ctx = RestContext.getGlobalRegistry().get(HostOne.class);
		var m1Ctx = ctx.getMixinContexts().get(M1.class);
		assertNotNull(m1Ctx.getResource(), "Mixin RestContext must own a non-null resource instance");
		assertTrue(m1Ctx.getResource() instanceof M1, "Mixin resource must be an instance of the mixin class");
	}
}
