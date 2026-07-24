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

import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.MockServletRequest;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.debug.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Phase 4 &mdash; proves that {@code @Child(debug=...)} seeds each of {@code @Debug}'s three sub-fields
 * (value/format/level) INDEPENDENTLY (per {@code RestContext.debugConfig}), not as one atomic all-or-nothing
 * scalar: a child declaring only one sub-field still receives the seed's value for the others.
 */
class ChildInheritance_Debug_Test extends TestBase {

	public static class SeedFormat implements DebugFormat {
		@Override public String format(DebugFormatContext context) { return "seed-format"; }
	}

	public static class ChildFormat implements DebugFormat {
		@Override public String format(DebugFormatContext context) { return "child-format"; }
	}

	//------------------------------------------------------------------------------------------------------------
	// a01: host seeds value="always" + format=SeedFormat; child declares ONLY its own level="FINE".
	// Expected: child's own level wins (independent sub-field); the seed's value/format still apply because
	// the child left them at their sentinel.
	//------------------------------------------------------------------------------------------------------------

	@Rest(path="/subfields", debug=@Debug(level="FINE"))
	public static class ChildWithOnlyLevel {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(childrenDefs=@Child(type=ChildWithOnlyLevel.class, debug=@Debug(value="always", format=SeedFormat.class)))
	public static class HostSeedsValueAndFormat extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_subFieldsIndependentlySeeded() {
		MockRestClient.buildLax(HostSeedsValueAndFormat.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostSeedsValueAndFormat.class);
		var childCtx = hostCtx.getRestChildren().asMap().get("subfields");
		assertNotNull(childCtx);

		var debugConfig = childCtx.getBeanStore().getBean(DebugConfig.class).orElse(null);
		assertNotNull(debugConfig, "Child context must expose a resolved DebugConfig bean");

		var result = debugConfig.resolve(childCtx, MockServletRequest.create());
		assertTrue(result.enabled(), "Seed's value=\"always\" must enable debug even though the child never declared its own value");
		assertInstanceOf(SeedFormat.class, result.format(), "Seed's format must apply since the child left format at its sentinel");
		assertEquals(Level.FINE, result.level(), "Child's own explicit level must win over any (absent) seed level");
	}

	//------------------------------------------------------------------------------------------------------------
	// a02: child declares its own value="never" (opting out); the seed's format still applies because the child
	// left format at its sentinel -- proving independence runs both ways.
	//------------------------------------------------------------------------------------------------------------

	@Rest(path="/subfields2", debug=@Debug(value="never"))
	public static class ChildWithOnlyValue {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(childrenDefs=@Child(type=ChildWithOnlyValue.class, debug=@Debug(value="always", format=SeedFormat.class)))
	public static class HostSeedsValueButChildOptsOut extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a02_childsOwnSubFieldWinsOthersInherited() {
		MockRestClient.buildLax(HostSeedsValueButChildOptsOut.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostSeedsValueButChildOptsOut.class);
		var childCtx = hostCtx.getRestChildren().asMap().get("subfields2");
		assertNotNull(childCtx);

		var debugConfig = childCtx.getBeanStore().getBean(DebugConfig.class).orElse(null);
		assertNotNull(debugConfig);

		var result = debugConfig.resolve(childCtx, MockServletRequest.create());
		assertFalse(result.enabled(), "Child's own explicit value=\"never\" must win over the seed's value=\"always\"");
		assertInstanceOf(SeedFormat.class, result.format(), "Seed's format must still apply -- the child left format at its sentinel");
	}
}
