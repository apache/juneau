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

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.TestBase;
import org.apache.juneau.rest.RestContext;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Phase 4 — exercises {@code @RestDestroy} dual-firing across host + mixin sub-contexts.
 *
 * <p>
 * Host's destroy methods fire first; each mixin's destroy methods fire afterwards (in mixin declaration
 * order). Mixin sub-contexts never re-discover mixins, so the recursion is one level deep.
 */
class MixinHooks_DestroyOrder_Test extends TestBase {

	private static final List<String> EVENTS = new CopyOnWriteArrayList<>();

	@BeforeEach void clear() { EVENTS.clear(); }

	@Rest
	public static class MixinA {
		@RestDestroy public void mixinADestroy() { EVENTS.add("mixinA:destroy"); }
		@RestGet(path="/a") public String a() { return "a"; }
	}

	@Rest
	public static class MixinB {
		@RestDestroy public void mixinBDestroy() { EVENTS.add("mixinB:destroy"); }
		@RestGet(path="/b") public String b() { return "b"; }
	}

	@Rest(mixins={MixinA.class, MixinB.class})
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestDestroy public void hostDestroy() { EVENTS.add("host:destroy"); }
	}

	@Test void a01_destroyFiresHostThenEachMixin() throws Exception {
		MockRestClient.buildLax(Host.class);
		var ctx = RestContext.getGlobalRegistry().get(Host.class);
		assertNotNull(ctx, "Host RestContext should be registered after MockRestClient build");

		EVENTS.clear();
		ctx.destroy();

		// Host fires first; mixins follow. We don't assert mixin order between MixinA/MixinB beyond
		// "they both fired after host" — Map<Class<?>,RestContext> isn't ordered by declaration in
		// every implementation, so be tolerant of either {A,B} or {B,A} sequence.
		assertTrue(EVENTS.size() >= 3, () -> "Expected at least 3 destroy events, got: " + EVENTS);
		assertEquals("host:destroy", EVENTS.get(0), "Host destroy must fire first");
		var mixinEvents = new HashSet<>(EVENTS.subList(1, EVENTS.size()));
		assertTrue(mixinEvents.contains("mixinA:destroy"), () -> "MixinA destroy missing from: " + EVENTS);
		assertTrue(mixinEvents.contains("mixinB:destroy"), () -> "MixinB destroy missing from: " + EVENTS);
	}
}
