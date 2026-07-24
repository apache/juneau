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

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Phase 3 &mdash; proves the additive-security AND-STACK shape for {@code @Child(roleGuard=...)}: a
 * host-seeded {@code roleGuard} and the child's own explicit {@code roleGuard} both apply as independent,
 * ANDed {@code RoleBasedRestGuard} instances &mdash; the child can add restriction but never remove or
 * weaken the host's.
 */
class ChildInheritance_RoleGuard_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------
	// a01: host seeds roleGuard="admin"; child declares its own roleGuard="special" -> BOTH must be satisfied.
	//------------------------------------------------------------------------------------------------------------

	@Rest(path="/rg", roleGuard="special")
	public static class ChildWithOwnRoleGuard {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(childrenDefs=@Child(type=ChildWithOwnRoleGuard.class, roleGuard="admin"))
	public static class HostSeedsRoleGuard extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_bothRoleGuardsMustBeSatisfied() throws Exception {
		var c = MockRestClient.buildLax(HostSeedsRoleGuard.class);
		c.get("/rg/me").roles("admin").run().assertStatus(403);              // missing "special"
		c.get("/rg/me").roles("special").run().assertStatus(403);            // missing "admin"
		c.get("/rg/me").roles("admin", "special").accept("text/plain").run().assertStatus(200).assertContent("me");
	}

	//------------------------------------------------------------------------------------------------------------
	// a02: child declares no roleGuard of its own -> only the host-seeded roleGuard applies.
	//------------------------------------------------------------------------------------------------------------

	@Rest(path="/rg2")
	public static class ChildNoRoleGuard {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(childrenDefs=@Child(type=ChildNoRoleGuard.class, roleGuard="admin"))
	public static class HostSeedsRoleGuardOnly extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a02_seedOnlyAppliesWhenChildDeclaresNone() throws Exception {
		var c = MockRestClient.buildLax(HostSeedsRoleGuardOnly.class);
		c.get("/rg2/me").run().assertStatus(403);
		c.get("/rg2/me").roles("admin").accept("text/plain").run().assertStatus(200).assertContent("me");
	}

	//------------------------------------------------------------------------------------------------------------
	// a03: rolesDeclared accumulates across host seed AND child, into the single shared declared-role set used
	// by every RoleBasedRestGuard in the chain (RestOpContext.guards()) -- the child's own rolesDeclared="admin"
	// feeds the host-seeded PATTERN-based roleGuard="ad*", which has no declared-role names of its own.
	//------------------------------------------------------------------------------------------------------------

	@Rest(path="/rg3", rolesDeclared="admin")
	public static class ChildDeclaresRolesOnly {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(childrenDefs=@Child(type=ChildDeclaresRolesOnly.class, roleGuard="ad*"))
	public static class HostSeedsPatternRoleGuard extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a03_rolesDeclaredFromChildFeedsHostSeededPatternRoleGuard() throws Exception {
		var c = MockRestClient.buildLax(HostSeedsPatternRoleGuard.class);
		c.get("/rg3/me").run().assertStatus(403);
		c.get("/rg3/me").roles("admin").accept("text/plain").run().assertStatus(200).assertContent("me");
	}
}
