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
package org.apache.juneau.rest.child;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.guard.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Phase 3 &mdash; proves the additive-security PREPEND shape for {@code @Child(guards=...)}: the host-seeded
 * guard applies when the child declares none of its own (degrades to a plain "set"), the seeded guard runs
 * outermost (first) when the child also declares its own guard, and the child's own guard is never dropped.
 */
class ChildInheritance_Guards_Test extends TestBase {

	static final List<String> ORDER = new CopyOnWriteArrayList<>();
	private static final List<String> EXPECTED_ORDER = List.of("seed", "child");

	public static class SeedGuard extends RestGuard {
		@Override public boolean isRequestAllowed(RestRequest req) { ORDER.add("seed"); return true; }
	}

	public static class ChildGuard extends RestGuard {
		@Override public boolean isRequestAllowed(RestRequest req) { ORDER.add("child"); return true; }
	}

	public static class BlockingSeedGuard extends RestGuard {
		@Override public boolean isRequestAllowed(RestRequest req) {
			return "yes".equals(req.getHeaderParam("X-Seed-Allowed").orElse(null));
		}
	}

	public static class AllowOnlyChildHeader extends RestGuard {
		@Override public boolean isRequestAllowed(RestRequest req) {
			return "yes".equals(req.getHeaderParam("X-Child-Allowed").orElse(null));
		}
	}

	//------------------------------------------------------------------------------------------------------------
	// a01: child with no guards of its own -> the seeded guard applies (degrades to a plain "set").
	//------------------------------------------------------------------------------------------------------------

	@Rest(path="/noguard")
	public static class ChildNoGuard {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(childrenDefs=@Child(type=ChildNoGuard.class, guards=BlockingSeedGuard.class))
	public static class HostSeedsGuardOnly extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_seededGuardAppliesWhenChildHasNone() throws Exception {
		var c = MockRestClient.buildLax(HostSeedsGuardOnly.class);
		c.get("/noguard/me").run().assertStatus(403);
		c.get("/noguard/me").header("X-Seed-Allowed", "yes").accept("text/plain").run().assertStatus(200).assertContent("me");
	}

	//------------------------------------------------------------------------------------------------------------
	// a02: child declares its own guard too -> BOTH apply, and the seed's guard runs first (outermost).
	//------------------------------------------------------------------------------------------------------------

	@Rest(path="/bothguards", guards=ChildGuard.class)
	public static class ChildWithOwnGuard {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(childrenDefs=@Child(type=ChildWithOwnGuard.class, guards=SeedGuard.class))
	public static class HostSeedsGuardOrder extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a02_bothApplySeedRunsFirst() throws Exception {
		ORDER.clear();
		var c = MockRestClient.buildLax(HostSeedsGuardOrder.class);
		c.get("/bothguards/me").accept("text/plain").run().assertStatus(200).assertContent("me");
		assertEquals(EXPECTED_ORDER, ORDER,
			"Host-seeded guard must run outermost (first), before the child's own declared guard");
	}

	//------------------------------------------------------------------------------------------------------------
	// a03: the child's own guard is never dropped -- the seed's guard alone would allow the request, but the
	// child's own (stricter) guard must still gate it.
	//------------------------------------------------------------------------------------------------------------

	@Rest(path="/dropcheck", guards=AllowOnlyChildHeader.class)
	public static class ChildWithOwnGuard2 {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(childrenDefs=@Child(type=ChildWithOwnGuard2.class, guards=SeedGuard.class))
	public static class HostSeedsGuardKeepsChildGuard extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a03_childsOwnGuardNeverDropped() throws Exception {
		var c = MockRestClient.buildLax(HostSeedsGuardKeepsChildGuard.class);
		// SeedGuard always allows, but the child's own AllowOnlyChildHeader guard must still gate the request.
		c.get("/dropcheck/me").run().assertStatus(403);
		c.get("/dropcheck/me").header("X-Child-Allowed", "yes").accept("text/plain").run().assertStatus(200).assertContent("me");
	}
}
