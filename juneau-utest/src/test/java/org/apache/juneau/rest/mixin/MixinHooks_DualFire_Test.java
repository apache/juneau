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
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Phase 4 — exercises dual-firing of lifecycle hooks ({@code @RestStartCall}, {@code @RestPreCall},
 * {@code @RestPostCall}, {@code @RestEndCall}) on mixin-endpoint requests.
 *
 * <p>
 * For a mixin endpoint, the host's hooks must fire before the mixin's hooks (in declaration order: start →
 * pre → method → post → end). For a host endpoint, only the host's hooks fire.
 */
@SuppressWarnings({
	"java:S3415" // EVENTS is the actual runtime-collected event list; the expected List literal is correctly the first arg (the final-field heuristic misclassifies it).
})
class MixinHooks_DualFire_Test extends TestBase {

	private static final List<String> EVENTS = new CopyOnWriteArrayList<>();

	@BeforeEach void clear() { EVENTS.clear(); }

	@Rest
	public static class TheMixin {
		@RestStartCall public void mixinStart() { EVENTS.add("mixin:start"); }
		@RestPreCall public void mixinPre() { EVENTS.add("mixin:pre"); }
		@RestPostCall public void mixinPost() { EVENTS.add("mixin:post"); }
		@RestEndCall public void mixinEnd() { EVENTS.add("mixin:end"); }
		@RestGet(path="/mixin/echo") public String mixinEcho() { EVENTS.add("mixin:method"); return "mixin"; }
	}

	@Rest(mixins={TheMixin.class})
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestStartCall public void hostStart() { EVENTS.add("host:start"); }
		@RestPreCall public void hostPre() { EVENTS.add("host:pre"); }
		@RestPostCall public void hostPost() { EVENTS.add("host:post"); }
		@RestEndCall public void hostEnd() { EVENTS.add("host:end"); }
		@RestGet(path="/host/echo") public String hostEcho() { EVENTS.add("host:method"); return "host"; }
	}

	@Test void a01_hostEndpointFiresHostHooksOnly() throws Exception {
		var c = MockRestClient.buildLax(Host.class);
		c.get("/host/echo").run().assertStatus(200);
		assertEquals(List.of("host:start", "host:pre", "host:method", "host:post", "host:end"), EVENTS,
			"Host endpoint must fire only the host's hooks");
	}

	@Test void a02_mixinEndpointFiresHostThenMixinHooks() throws Exception {
		var c = MockRestClient.buildLax(Host.class);
		c.get("/mixin/echo").run().assertStatus(200);
		assertEquals(List.of(
				"host:start", "mixin:start",
				"host:pre", "mixin:pre",
				"mixin:method",
				"host:post", "mixin:post",
				"host:end", "mixin:end"
			), EVENTS,
			"Mixin endpoint must dual-fire each hook category with host first, mixin second");
	}
}
