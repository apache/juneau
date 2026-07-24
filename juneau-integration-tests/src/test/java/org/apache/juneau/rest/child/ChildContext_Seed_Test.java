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
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.guard.*;
import org.apache.juneau.rest.server.logger.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Phase 2 canary &mdash; proves the {@code @Child} seed-injection mechanism end-to-end using {@code callLogger}
 * (a single child-wins scalar) as a witness, mirroring {@code MixinInheritance_CallLogger_Test}.
 *
 * <p>
 * {@code callLogger} is chosen because {@link RestContext#getCallLogger()} is a direct, public, class-level
 * getter with no request/op machinery needed to observe it.
 */
class ChildContext_Seed_Test extends TestBase {

	public static class SeedLogger extends CallLogger {
		public SeedLogger(BeanStore bs) { super(bs); }
	}

	public static class ChildLogger extends CallLogger {
		public ChildLogger(BeanStore bs) { super(bs); }
	}

	public static class BlockAllGuard extends RestGuard {
		@Override public boolean isRequestAllowed(RestRequest req) { return false; }
	}

	//------------------------------------------------------------------------------------------------------------
	// a01: child declares no callLogger -> host's @Child(callLogger=...) seed takes effect.
	//------------------------------------------------------------------------------------------------------------

	@Rest(path="/nologger")
	public static class ChildNoLoggerDeclared {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(childrenDefs=@Child(type=ChildNoLoggerDeclared.class, callLogger=SeedLogger.class))
	public static class HostSeedsLogger extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_childWithNoDeclarationReceivesSeed() {
		MockRestClient.buildLax(HostSeedsLogger.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostSeedsLogger.class);
		var childCtx = hostCtx.getRestChildren().asMap().get("nologger");
		assertNotNull(childCtx);
		assertInstanceOf(SeedLogger.class, childCtx.getCallLogger(),
			"Child with no callLogger declaration must receive the host's seeded SeedLogger");
	}

	//------------------------------------------------------------------------------------------------------------
	// a02: child's own explicit callLogger declaration wins over the host's seed.
	//------------------------------------------------------------------------------------------------------------

	@Rest(path="/ownlogger", callLogger=ChildLogger.class)
	public static class ChildWithOwnLogger {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(childrenDefs=@Child(type=ChildWithOwnLogger.class, callLogger=SeedLogger.class))
	public static class HostSeedsLoggerButChildWins extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a02_childsOwnDeclarationWins() {
		MockRestClient.buildLax(HostSeedsLoggerButChildWins.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostSeedsLoggerButChildWins.class);
		var childCtx = hostCtx.getRestChildren().asMap().get("ownlogger");
		assertNotNull(childCtx);
		assertInstanceOf(ChildLogger.class, childCtx.getCallLogger(),
			"Child's own explicit callLogger must win over the host's seeded SeedLogger");
	}

	//------------------------------------------------------------------------------------------------------------
	// a03: isolation preserved -- host's OWN (non-seeded) @Rest(guards=...) must not leak to the child, even
	// though the child receives a callLogger seed.  If guards leaked, the child endpoint would 403.
	//------------------------------------------------------------------------------------------------------------

	@Rest(path="/isocheck")
	public static class ChildForIsolation {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(guards=BlockAllGuard.class, childrenDefs=@Child(type=ChildForIsolation.class, callLogger=SeedLogger.class))
	public static class HostWithGuardsAndSeed extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a03_isolationPreserved() throws Exception {
		var c = MockRestClient.buildLax(HostWithGuardsAndSeed.class);
		// Host's own (non-seeded) guards must not leak into the child's isolated context.
		c.get("/isocheck/me").accept("text/plain").run().assertStatus(200).assertContent("me");
		var hostCtx = RestContext.getGlobalRegistry().get(HostWithGuardsAndSeed.class);
		var childCtx = hostCtx.getRestChildren().asMap().get("isocheck");
		assertInstanceOf(SeedLogger.class, childCtx.getCallLogger(),
			"The seeded callLogger must still apply alongside isolation from the host's own guards");
	}

	//------------------------------------------------------------------------------------------------------------
	// a04: the seed is not masked by the synthesized DefaultConfig fallback -- DefaultConfig sets
	// maxInput="1000000"; a host-seeded @Child(maxInput=...) must still win (seed ranks above DefaultConfig).
	//------------------------------------------------------------------------------------------------------------

	@Rest(path="/maxcheck")
	public static class ChildForMaxInput {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(childrenDefs=@Child(type=ChildForMaxInput.class, maxInput="12345"))
	public static class HostSeedsMaxInput extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a04_seedNotMaskedByDefaultConfigFallback() {
		MockRestClient.buildLax(HostSeedsMaxInput.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostSeedsMaxInput.class);
		var childCtx = hostCtx.getRestChildren().asMap().get("maxcheck");
		assertNotNull(childCtx);
		var opCtx = childCtx.getRestOperations().getOpContexts().get(0);
		assertEquals(12345L, opCtx.getMaxInput(),
			"Host-seeded maxInput must win over the framework's DefaultConfig fallback (1000000)");
	}
}
