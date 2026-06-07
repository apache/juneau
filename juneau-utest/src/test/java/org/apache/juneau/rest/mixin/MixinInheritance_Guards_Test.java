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

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.guard.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Phase 2 regression matrix &mdash; verifies that {@code @Rest(guards=...)} on a mixin class inherits the host's
 * guard chain by default (security-conservative default: surprises around security err on "too strict") and that
 * {@code noInherit="guards"} removes host guards from the mixin context.
 *
 * <p>
 * Two scenarios per resource exercise the matrix:
 * <ul>
 * 	<li><b>Inherit-by-default protection.</b> A host with {@code @Rest(guards=AllowOnlyHostHeader)} automatically
 * 		protects mixin endpoints. A request without the host header receives 403 from the mixin endpoint just as
 * 		it would from the host endpoint.</li>
 * 	<li><b>noInherit opt-out.</b> A mixin with {@code noInherit="guards"} bypasses the host's guards entirely;
 * 		the mixin endpoint accepts the request regardless of the host header, while the host endpoint remains
 * 		guarded.</li>
 * </ul>
 */
class MixinInheritance_Guards_Test extends TestBase {

	public static class AllowOnlyHostHeader extends RestGuard {
		@Override public boolean isRequestAllowed(RestRequest req) {
			return "yes".equals(req.getHeaderParam("X-Host-Allowed").orElse(null));
		}
	}

	public static class AllowOnlyMixinHeader extends RestGuard {
		@Override public boolean isRequestAllowed(RestRequest req) {
			return "yes".equals(req.getHeaderParam("X-Mixin-Allowed").orElse(null));
		}
	}

	@Rest
	public static class M_InheritsHostGuard {
		@RestGet(path="/inh") public String inh() { return "inh"; }
	}

	@Rest(guards={AllowOnlyMixinHeader.class})
	public static class M_AppendsMixinGuard {
		@RestGet(path="/app") public String app() { return "app"; }
	}

	@Rest(noInherit="guards")
	public static class M_NoInheritGuard {
		@RestGet(path="/free") public String free() { return "free"; }
	}

	@Rest(guards={AllowOnlyHostHeader.class}, mixins={M_InheritsHostGuard.class, M_AppendsMixinGuard.class, M_NoInheritGuard.class})
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Test void a01_hostEndpointGuarded() throws Exception {
		var c = MockRestClient.buildLax(Host.class);
		c.get("/h").run().assertStatus(403);
		c.get("/h").header("X-Host-Allowed", "yes").accept("text/plain").run().assertStatus(200).assertContent("h");
	}

	@Test void a02_mixinInheritsHostGuardByDefault() throws Exception {
		var c = MockRestClient.buildLax(Host.class);
		c.get("/inh").run().assertStatus(403);
		c.get("/inh").header("X-Host-Allowed", "yes").accept("text/plain").run().assertStatus(200).assertContent("inh");
	}

	@Test void a03_mixinAppendsGuardAfterInheritedHost() throws Exception {
		var c = MockRestClient.buildLax(Host.class);
		c.get("/app").run().assertStatus(403);
		c.get("/app").header("X-Host-Allowed", "yes").run().assertStatus(403);
		c.get("/app").header("X-Mixin-Allowed", "yes").run().assertStatus(403);
		c.get("/app").header("X-Host-Allowed", "yes").header("X-Mixin-Allowed", "yes").accept("text/plain").run().assertStatus(200).assertContent("app");
	}

	@Test void a04_noInheritDropsHostGuardFromMixin() throws Exception {
		var c = MockRestClient.buildLax(Host.class);
		c.get("/free").accept("text/plain").run().assertStatus(200).assertContent("free");
		c.get("/h").run().assertStatus(403);
	}
}
