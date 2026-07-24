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
 * Phase 4 &mdash; proves the CHILD-WINS scalar shape for {@code @Child(partParser=...)}: the host-seeded
 * part parser applies when the child declares none of its own, but the child's own explicit
 * {@code @Rest(partParser=...)} wins over the seed when present.
 */
class ChildInheritance_PartParser_Test extends TestBase {

	public static class SeedPP extends FakeReaderParser {
		public SeedPP(FakeReaderParser.Builder b) { super(b); }
	}

	public static class ChildPP extends FakeReaderParser {
		public ChildPP(FakeReaderParser.Builder b) { super(b); }
	}

	//------------------------------------------------------------------------------------------------------------
	// a01: child declares no partParser -> host's seed takes effect.
	//------------------------------------------------------------------------------------------------------------

	@Rest(path="/nopp")
	public static class ChildNoPPDeclared {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(childrenDefs=@Child(type=ChildNoPPDeclared.class, partParser=SeedPP.class))
	public static class HostSeedsPP extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_childWithNoDeclarationReceivesSeed() {
		MockRestClient.buildLax(HostSeedsPP.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostSeedsPP.class);
		var childCtx = hostCtx.getRestChildren().asMap().get("nopp");
		assertNotNull(childCtx);
		assertEquals(SeedPP.class, childCtx.getPartParser().getClass(),
			"Child with no partParser declaration must receive the host's seeded SeedPP");
	}

	//------------------------------------------------------------------------------------------------------------
	// a02: child's own explicit partParser wins over the host's seed.
	//------------------------------------------------------------------------------------------------------------

	@Rest(path="/ownpp", partParser=ChildPP.class)
	public static class ChildWithOwnPP {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(childrenDefs=@Child(type=ChildWithOwnPP.class, partParser=SeedPP.class))
	public static class HostSeedsPPButChildWins extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a02_childsOwnDeclarationWins() {
		MockRestClient.buildLax(HostSeedsPPButChildWins.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostSeedsPPButChildWins.class);
		var childCtx = hostCtx.getRestChildren().asMap().get("ownpp");
		assertNotNull(childCtx);
		assertEquals(ChildPP.class, childCtx.getPartParser().getClass(),
			"Child's own explicit partParser must win over the host's seeded SeedPP");
	}
}
