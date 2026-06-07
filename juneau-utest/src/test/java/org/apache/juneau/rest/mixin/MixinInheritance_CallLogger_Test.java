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
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.logger.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Phase 2 regression matrix &mdash; verifies that {@code @Rest(callLogger=...)} on a mixin class is resolved
 * through the {@link RestContext#getRestAnnotationsForProperty(String) annotation-property walk} so the host's
 * callLogger declaration is inherited by default, the mixin's declaration overrides it, and
 * {@code noInherit="callLogger"} blocks the parent walk.
 *
 * <p>
 * The {@code callLogger} property uses reduce-last (most-derived non-Void wins) semantics; the parent walk
 * prepends the host's annotations, so the mixin's local declaration always wins when present, the host's wins
 * when the mixin doesn't declare its own, and {@code noInherit} cuts off the host's declaration entirely from
 * the resolution chain.
 */
class MixinInheritance_CallLogger_Test extends TestBase {

	public static class HostLogger extends CallLogger {
		public HostLogger(BeanStore bs) { super(bs); }
	}

	public static class MixinLogger extends CallLogger {
		public MixinLogger(BeanStore bs) { super(bs); }
	}

	@Rest
	public static class M_NoLoggerDeclared {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(callLogger=MixinLogger.class)
	public static class M_MixinLogger {
		@RestGet(path="/my") public String my() { return "my"; }
	}

	@Rest(noInherit="callLogger", callLogger=MixinLogger.class)
	public static class M_NoInheritLogger {
		@RestGet(path="/my") public String my() { return "my"; }
	}

	@Rest(callLogger=HostLogger.class, mixins={M_NoLoggerDeclared.class})
	public static class HostInheritsToMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Rest(callLogger=HostLogger.class, mixins={M_MixinLogger.class})
	public static class HostWithMixinOverride extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Rest(callLogger=HostLogger.class, mixins={M_NoInheritLogger.class})
	public static class HostWithNoInherit extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Test void a01_mixinInheritsHostCallLogger() {
		MockRestClient.buildLax(HostInheritsToMixin.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostInheritsToMixin.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_NoLoggerDeclared.class);
		assertNotNull(mixinCtx);

		assertInstanceOf(HostLogger.class, hostCtx.getCallLogger(),
			"Host must use its declared HostLogger");
		assertInstanceOf(HostLogger.class, mixinCtx.getCallLogger(),
			"Mixin with no callLogger declaration must inherit the host's HostLogger");
	}

	@Test void a02_mixinOverridesHostCallLogger() {
		MockRestClient.buildLax(HostWithMixinOverride.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostWithMixinOverride.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_MixinLogger.class);
		assertNotNull(mixinCtx);

		assertInstanceOf(HostLogger.class, hostCtx.getCallLogger(),
			"Host endpoint must keep using HostLogger — mixin override is scoped to mixin context");
		assertInstanceOf(MixinLogger.class, mixinCtx.getCallLogger(),
			"Mixin endpoint must use the mixin's MixinLogger (most-derived wins in resolution chain)");
	}

	@Test void a03_noInheritOnMixinUsesMixinOnly() {
		MockRestClient.buildLax(HostWithNoInherit.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostWithNoInherit.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_NoInheritLogger.class);
		assertNotNull(mixinCtx);

		assertInstanceOf(MixinLogger.class, mixinCtx.getCallLogger(),
			"Mixin with noInherit=\"callLogger\" must use the mixin's MixinLogger");
		assertInstanceOf(HostLogger.class, hostCtx.getCallLogger(),
			"Host must retain its HostLogger regardless of mixin's noInherit");
	}
}
