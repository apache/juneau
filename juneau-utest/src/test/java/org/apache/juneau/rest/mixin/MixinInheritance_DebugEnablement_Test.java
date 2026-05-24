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

import org.apache.juneau.TestBase;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.RestContext;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.debug.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Phase 2 regression matrix &mdash; verifies that {@code @Rest(debugEnablement=...)} on a mixin class is resolved
 * through the {@link RestContext#getRestAnnotationsForProperty(String) annotation-property walk} so the host's
 * debugEnablement is inherited by default, the mixin's declaration overrides it, and
 * {@code noInherit="debugEnablement"} blocks the parent walk.
 */
class MixinInheritance_DebugEnablement_Test extends TestBase {

	public static class HostDebug extends BasicDebugEnablement {
		public HostDebug(BeanStore bs) { super(bs); }
	}

	public static class MixinDebug extends BasicDebugEnablement {
		public MixinDebug(BeanStore bs) { super(bs); }
	}

	@Rest
	public static class M_NoDebugDeclared {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(debugEnablement=MixinDebug.class)
	public static class M_MixinDebug {
		@RestGet(path="/my") public String my() { return "my"; }
	}

	@Rest(noInherit="debugEnablement", debugEnablement=MixinDebug.class)
	public static class M_NoInheritDebug {
		@RestGet(path="/my") public String my() { return "my"; }
	}

	@Rest(debugEnablement=HostDebug.class, mixins={M_NoDebugDeclared.class})
	public static class HostInheritsToMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Rest(debugEnablement=HostDebug.class, mixins={M_MixinDebug.class})
	public static class HostWithMixinOverride extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Rest(debugEnablement=HostDebug.class, mixins={M_NoInheritDebug.class})
	public static class HostWithNoInherit extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Test void a01_mixinInheritsHostDebugEnablement() throws Exception {
		MockRestClient.buildLax(HostInheritsToMixin.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostInheritsToMixin.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_NoDebugDeclared.class);
		assertNotNull(mixinCtx);

		assertInstanceOf(HostDebug.class, hostCtx.getDebugEnablement(),
			"Host must use its declared HostDebug");
		assertInstanceOf(HostDebug.class, mixinCtx.getDebugEnablement(),
			"Mixin with no debugEnablement declaration must inherit the host's HostDebug");
	}

	@Test void a02_mixinOverridesHostDebugEnablement() throws Exception {
		MockRestClient.buildLax(HostWithMixinOverride.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostWithMixinOverride.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_MixinDebug.class);
		assertNotNull(mixinCtx);

		assertInstanceOf(HostDebug.class, hostCtx.getDebugEnablement(),
			"Host endpoint must keep using HostDebug — mixin override is scoped to mixin context");
		assertInstanceOf(MixinDebug.class, mixinCtx.getDebugEnablement(),
			"Mixin endpoint must use the mixin's MixinDebug (most-derived wins in resolution chain)");
	}

	@Test void a03_noInheritOnMixinUsesMixinOnly() throws Exception {
		MockRestClient.buildLax(HostWithNoInherit.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostWithNoInherit.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_NoInheritDebug.class);
		assertNotNull(mixinCtx);

		assertInstanceOf(MixinDebug.class, mixinCtx.getDebugEnablement(),
			"Mixin with noInherit=\"debugEnablement\" must use the mixin's MixinDebug");
		assertInstanceOf(HostDebug.class, hostCtx.getDebugEnablement(),
			"Host must retain its HostDebug regardless of mixin's noInherit");
	}
}
