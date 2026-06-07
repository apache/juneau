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
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.utest.utils.*;
import org.junit.jupiter.api.*;

/**
 * Phase 2 &mdash; exercises {@code getRestAnnotationsForProperty(...)} parent-walk for the
 * {@code @Rest(partParser=...)} property.
 *
 * <p>
 * Acceptance:
 * <ul>
 * 	<li>Mixin without {@code partParser} declaration inherits the host's part parser class.</li>
 * 	<li>Mixin's own {@code @Rest(partParser=...)} overrides the inherited host part parser (most-derived
 * 		wins in parent-walk chain).</li>
 * 	<li>{@code @Rest(noInherit="partParser", partParser=...)} on a mixin shadows only the mixin's own
 * 		declaration &mdash; host endpoint remains unaffected.</li>
 * </ul>
 */
class MixinInheritance_PartParser_Test extends TestBase {

	public static class HostPP extends FakeReaderParser {
		public HostPP(FakeReaderParser.Builder b) { super(b); }
	}

	public static class MixinPP extends FakeReaderParser {
		public MixinPP(FakeReaderParser.Builder b) { super(b); }
	}

	@Rest
	public static class M_NoPPDeclared {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(partParser=MixinPP.class)
	public static class M_MixinPP {
		@RestGet(path="/my") public String my() { return "my"; }
	}

	@Rest(noInherit="partParser", partParser=MixinPP.class)
	public static class M_NoInheritPP {
		@RestGet(path="/my") public String my() { return "my"; }
	}

	@Rest(partParser=HostPP.class, mixins={M_NoPPDeclared.class})
	public static class HostInheritsToMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Rest(partParser=HostPP.class, mixins={M_MixinPP.class})
	public static class HostWithMixinOverride extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Rest(partParser=HostPP.class, mixins={M_NoInheritPP.class})
	public static class HostWithNoInherit extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Test void a01_mixinInheritsHostPartParser() {
		MockRestClient.buildLax(HostInheritsToMixin.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostInheritsToMixin.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_NoPPDeclared.class);
		assertNotNull(mixinCtx);

		assertEquals(HostPP.class, hostCtx.getPartParser().getClass(),
			"Host must use its declared HostPP part parser");
		assertEquals(HostPP.class, mixinCtx.getPartParser().getClass(),
			"Mixin with no partParser override must inherit the host's HostPP part parser");
	}

	@Test void a02_mixinOverridesHostPartParser() {
		MockRestClient.buildLax(HostWithMixinOverride.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostWithMixinOverride.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_MixinPP.class);
		assertNotNull(mixinCtx);

		assertEquals(HostPP.class, hostCtx.getPartParser().getClass(),
			"Host's partParser must remain HostPP &mdash; mixin contributions are scoped to mixin sub-context");
		assertEquals(MixinPP.class, mixinCtx.getPartParser().getClass(),
			"Mixin's own @Rest(partParser=MixinPP) must win as most-derived in walk");
	}

	@Test void a03_mixinNoInheritPartParser() {
		MockRestClient.buildLax(HostWithNoInherit.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostWithNoInherit.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_NoInheritPP.class);
		assertNotNull(mixinCtx);

		assertEquals(HostPP.class, hostCtx.getPartParser().getClass(),
			"Host's partParser must remain HostPP regardless of mixin's noInherit");
		assertEquals(MixinPP.class, mixinCtx.getPartParser().getClass(),
			"Mixin with noInherit=\"partParser\" must use its own MixinPP only");
	}
}
