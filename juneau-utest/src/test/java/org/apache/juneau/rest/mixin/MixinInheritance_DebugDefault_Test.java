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

import org.apache.juneau.*;
import org.apache.juneau.TestBase;
import org.apache.juneau.rest.RestContext;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Phase 2 regression matrix &mdash; verifies that {@code @Rest(debugDefault=...)} on the host is inherited as the
 * default {@link Enablement} on a mixin sub-context, that the mixin's own {@code debugDefault} replaces the
 * inherited value, and that {@code noInherit="debugDefault"} blocks the parent walk.
 *
 * <p>
 * The resolved {@code debugDefault} is published into the context's bean store as an {@link Enablement} bean
 * (see {@code RestContext.debugEnablement} memoizer). We trigger the memoizer via
 * {@link RestContext#getDebugEnablement()} and then read the bean to verify the resolved value.
 */
class MixinInheritance_DebugDefault_Test extends TestBase {

	@Rest
	public static class M_NoDebugDefault {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(debugDefault="ALWAYS")
	public static class M_OverridesAlways {
		@RestGet(path="/my") public String my() { return "my"; }
	}

	@Rest(noInherit="debugDefault", debugDefault="ALWAYS")
	public static class M_NoInheritAlways {
		@RestGet(path="/my") public String my() { return "my"; }
	}

	@Rest(debugDefault="CONDITIONAL", mixins={M_NoDebugDefault.class})
	public static class HostInheritsToMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Rest(debugDefault="CONDITIONAL", mixins={M_OverridesAlways.class})
	public static class HostWithMixinOverride extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Rest(debugDefault="CONDITIONAL", mixins={M_NoInheritAlways.class})
	public static class HostWithNoInherit extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	private static Enablement resolvedDebugDefault(RestContext c) {
		c.getDebugEnablement();
		return c.getBeanStore().getBean(Enablement.class).orElse(null);
	}

	@Test void a01_mixinInheritsHostDebugDefault() throws Exception {
		MockRestClient.buildLax(HostInheritsToMixin.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostInheritsToMixin.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_NoDebugDefault.class);
		assertNotNull(mixinCtx);

		assertEquals(Enablement.CONDITIONAL, resolvedDebugDefault(hostCtx),
			"Host must resolve its declared debugDefault=\"CONDITIONAL\"");
		assertEquals(Enablement.CONDITIONAL, resolvedDebugDefault(mixinCtx),
			"Mixin with no debugDefault declaration must inherit the host's CONDITIONAL");
	}

	@Test void a02_mixinDebugDefaultOverridesHost() throws Exception {
		MockRestClient.buildLax(HostWithMixinOverride.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostWithMixinOverride.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_OverridesAlways.class);
		assertNotNull(mixinCtx);

		assertEquals(Enablement.CONDITIONAL, resolvedDebugDefault(hostCtx),
			"Host endpoint must keep its CONDITIONAL — mixin override is scoped to mixin context");
		assertEquals(Enablement.ALWAYS, resolvedDebugDefault(mixinCtx),
			"Mixin endpoint must use mixin's debugDefault=\"ALWAYS\" (most-derived wins)");
	}

	@Test void a03_noInheritOnMixinUsesMixinOnly() throws Exception {
		MockRestClient.buildLax(HostWithNoInherit.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostWithNoInherit.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_NoInheritAlways.class);
		assertNotNull(mixinCtx);

		assertEquals(Enablement.ALWAYS, resolvedDebugDefault(mixinCtx),
			"Mixin with noInherit=\"debugDefault\" must use its own ALWAYS");
		assertEquals(Enablement.CONDITIONAL, resolvedDebugDefault(hostCtx),
			"Host must retain its CONDITIONAL regardless of mixin's noInherit");
	}
}
