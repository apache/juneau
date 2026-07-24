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
 * Phase 4 &mdash; proves the CHILD-WINS scalar shape for {@code @Child(partSerializer=...)}: the host-seeded
 * part serializer applies when the child declares none of its own, but the child's own explicit
 * {@code @Rest(partSerializer=...)} wins over the seed when present.
 */
class ChildInheritance_PartSerializer_Test extends TestBase {

	public static class SeedPS extends FakeWriterSerializer {
		public SeedPS(FakeWriterSerializer.Builder b) { super(b); }
	}

	public static class ChildPS extends FakeWriterSerializer {
		public ChildPS(FakeWriterSerializer.Builder b) { super(b); }
	}

	//------------------------------------------------------------------------------------------------------------
	// a01: child declares no partSerializer -> host's seed takes effect.
	//------------------------------------------------------------------------------------------------------------

	@Rest(path="/nops")
	public static class ChildNoPSDeclared {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(childrenDefs=@Child(type=ChildNoPSDeclared.class, partSerializer=SeedPS.class))
	public static class HostSeedsPS extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_childWithNoDeclarationReceivesSeed() {
		MockRestClient.buildLax(HostSeedsPS.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostSeedsPS.class);
		var childCtx = hostCtx.getRestChildren().asMap().get("nops");
		assertNotNull(childCtx);
		assertEquals(SeedPS.class, childCtx.getPartSerializer().getClass(),
			"Child with no partSerializer declaration must receive the host's seeded SeedPS");
	}

	//------------------------------------------------------------------------------------------------------------
	// a02: child's own explicit partSerializer wins over the host's seed.
	//------------------------------------------------------------------------------------------------------------

	@Rest(path="/ownps", partSerializer=ChildPS.class)
	public static class ChildWithOwnPS {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(childrenDefs=@Child(type=ChildWithOwnPS.class, partSerializer=SeedPS.class))
	public static class HostSeedsPSButChildWins extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a02_childsOwnDeclarationWins() {
		MockRestClient.buildLax(HostSeedsPSButChildWins.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostSeedsPSButChildWins.class);
		var childCtx = hostCtx.getRestChildren().asMap().get("ownps");
		assertNotNull(childCtx);
		assertEquals(ChildPS.class, childCtx.getPartSerializer().getClass(),
			"Child's own explicit partSerializer must win over the host's seeded SeedPS");
	}
}
