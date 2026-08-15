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

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.guard.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.utest.utils.*;
import org.junit.jupiter.api.*;

/**
 * Phase 5 &mdash; proves that a child's own {@code @Rest(noInherit="&lt;property&gt;")} cuts the corresponding
 * {@code @Child} seed too, per spec &sect;4's "{@code noInherit} interaction" subsection: because the seed is
 * injected as an ordinary least-derived entry in the child's own {@code getRestAnnotations()} chain
 * (&sect;6.6), the existing generic {@code noInherit} cutoff scan in {@code getRestAnnotationsForProperty}
 * naturally truncates it too, with no seed-aware special-casing required. Verified uniformly across BOTH
 * seedable-property buckets: an additive-security member ({@code guards}) and a child-wins scalar
 * ({@code partSerializer}).
 */
class ChildInheritance_NoInherit_Test extends TestBase {

	public static class BlockAllGuard extends RestGuard {
		@Override public boolean isRequestAllowed(RestRequest req) { return false; }
	}

	public static class SeedPS extends FakeWriterSerializer {
		public SeedPS(FakeWriterSerializer.Builder b) { super(b); }
	}

	//------------------------------------------------------------------------------------------------------------
	// a01: additive-security member (guards) -- child declares noInherit="guards" -> receives NONE of the
	// host's seeded guards (the endpoint must be reachable WITHOUT satisfying the seeded guard at all, not
	// merely "the child's own guards are present").
	//------------------------------------------------------------------------------------------------------------

	@Rest(path="/noinheritguards", noInherit="guards")
	public static class ChildNoInheritGuards {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(childrenDefs=@Child(type=ChildNoInheritGuards.class, guards=BlockAllGuard.class))
	public static class HostSeedsGuardButChildOptsOut extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_noInheritCutsSeededGuards() throws Exception {
		var c = MockRestClient.buildLax(HostSeedsGuardButChildOptsOut.class);
		// If the seeded BlockAllGuard were still applied, this request would always 403. noInherit="guards"
		// must cut it entirely, leaving the child with zero guards.
		c.get("/noinheritguards/me").accept("text/plain").run().assertStatus(200).assertContent("me");
	}

	//------------------------------------------------------------------------------------------------------------
	// a02: child-wins scalar (partSerializer) -- child declares noInherit="partSerializer" -> receives NONE of
	// the host's seeded partSerializer (falls back to the framework default, not the seed).
	//------------------------------------------------------------------------------------------------------------

	@Rest(path="/noinheritps", noInherit="partSerializer")
	public static class ChildNoInheritPS {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(childrenDefs=@Child(type=ChildNoInheritPS.class, partSerializer=SeedPS.class))
	public static class HostSeedsPSButChildOptsOut extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a02_noInheritCutsSeededPartSerializer() {
		MockRestClient.buildLax(HostSeedsPSButChildOptsOut.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostSeedsPSButChildOptsOut.class);
		var childCtx = hostCtx.getRestChildren().asMap().get("noinheritps");
		assertNotNull(childCtx);
		assertFalse(childCtx.getPartSerializer() instanceof SeedPS,
			"Child's own noInherit=\"partSerializer\" must cut the host's seeded SeedPS entirely, "
			+ "falling back to the framework default rather than the seed");
	}
}
