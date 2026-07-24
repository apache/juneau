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
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.utest.utils.*;
import org.junit.jupiter.api.*;

/**
 * Phase 4 hardening &mdash; regression test for an edge case not called out explicitly by name in spec &sect;6.6:
 * a CHILD CLASS that itself transitively implements {@code DefaultConfig} (e.g. by extending
 * {@code BasicRestServlet}/{@code BasicRestServletGroup}, which implement {@code BasicUniversalConfig extends
 * DefaultConfig}), rather than being a plain POJO with no framework-default mixin.
 *
 * <p>
 * <b>Bug found and fixed by this test (Phase 4 hardening):</b> {@code computeRawRestAnnotations()}'s
 * {@code hasDefaultConfig}-true branch originally appended the seed annotation after the ENTIRE {@code raw}
 * chain unconditionally. For a plain child class (the common case exercised by every other Phase 2-4 test),
 * {@code raw} never embeds {@code DefaultConfig} itself, so appending after {@code raw} correctly placed the
 * seed above the framework's separately-synthesized {@code DefaultConfig} fallback. But when the child class
 * itself transitively implements {@code DefaultConfig}, that interface's own {@code @Rest} entry is already
 * PART OF {@code raw} (typically at its most-ancestor/least-derived slot) — appending the seed after the
 * <i>entire</i> {@code raw} list placed the seed BELOW that embedded {@code DefaultConfig} entry, so
 * {@code DefaultConfig}'s own concrete defaults (e.g. {@code partSerializer=OpenApiSerializer.class}) masked
 * the host's seed entirely, violating spec &sect;6.6's invariant that the seed ranks above the {@code
 * DefaultConfig} fallback. Fixed by splitting {@code raw} into "the child's own real declarations" and "the
 * (embedded) DefaultConfig entries" and inserting the seed between them, mirroring exactly how the
 * separately-synthesized case already worked.
 */
class ChildInheritance_DefaultConfigChild_Test extends TestBase {

	public static class SeedPS extends FakeWriterSerializer {
		public SeedPS(FakeWriterSerializer.Builder b) { super(b); }
	}

	//------------------------------------------------------------------------------------------------------------
	// a01: child class extends BasicRestServlet (transitively implements DefaultConfig) and declares no
	// partSerializer of its own -> the host's seed must still win over DefaultConfig's own baked-in
	// partSerializer=OpenApiSerializer.class default, not be masked by it.
	//------------------------------------------------------------------------------------------------------------

	@Rest(path="/dcchild")
	public static class ChildImplementingDefaultConfig extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(childrenDefs=@Child(type=ChildImplementingDefaultConfig.class, partSerializer=SeedPS.class))
	public static class HostSeedsIntoDefaultConfigChild extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_seedNotMaskedByChildsOwnEmbeddedDefaultConfig() {
		MockRestClient.buildLax(HostSeedsIntoDefaultConfigChild.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostSeedsIntoDefaultConfigChild.class);
		var childCtx = hostCtx.getRestChildren().asMap().get("dcchild");
		assertNotNull(childCtx);
		assertEquals(SeedPS.class, childCtx.getPartSerializer().getClass(),
			"Host-seeded partSerializer must win over DefaultConfig's own baked-in OpenApiSerializer default, "
			+ "even when the child class transitively implements DefaultConfig itself");
	}

	//------------------------------------------------------------------------------------------------------------
	// a02: same shape, but the child ALSO declares its own explicit partSerializer -> the child's own
	// declaration must still win over both the seed AND DefaultConfig (three-way precedence check).
	//------------------------------------------------------------------------------------------------------------

	public static class ChildPS extends FakeWriterSerializer {
		public ChildPS(FakeWriterSerializer.Builder b) { super(b); }
	}

	@Rest(path="/dcchild2", partSerializer=ChildPS.class)
	public static class ChildImplementingDefaultConfigWithOwnPS extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(childrenDefs=@Child(type=ChildImplementingDefaultConfigWithOwnPS.class, partSerializer=SeedPS.class))
	public static class HostSeedsIntoDefaultConfigChildWithOwnPS extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a02_childsOwnDeclarationStillWinsOverSeedAndDefaultConfig() {
		MockRestClient.buildLax(HostSeedsIntoDefaultConfigChildWithOwnPS.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostSeedsIntoDefaultConfigChildWithOwnPS.class);
		var childCtx = hostCtx.getRestChildren().asMap().get("dcchild2");
		assertNotNull(childCtx);
		assertEquals(ChildPS.class, childCtx.getPartSerializer().getClass(),
			"Child's own explicit partSerializer must win over both the host's seed and DefaultConfig's default");
	}
}
