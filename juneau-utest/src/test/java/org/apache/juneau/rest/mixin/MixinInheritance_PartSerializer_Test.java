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
import org.apache.juneau.rest.RestContext;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.utest.utils.*;
import org.junit.jupiter.api.*;

/**
 * Phase 2 &mdash; exercises {@code getRestAnnotationsForProperty(...)} parent-walk for the
 * {@code @Rest(partSerializer=...)} property.
 *
 * <p>
 * Acceptance:
 * <ul>
 * 	<li>Mixin without {@code partSerializer} declaration inherits the host's part serializer class.</li>
 * 	<li>Mixin's own {@code @Rest(partSerializer=...)} overrides the inherited host part serializer (most-derived
 * 		wins in parent-walk chain).</li>
 * 	<li>{@code @Rest(noInherit="partSerializer", partSerializer=...)} on a mixin shadows only the mixin's own
 * 		declaration &mdash; host endpoint remains unaffected.</li>
 * </ul>
 */
class MixinInheritance_PartSerializer_Test extends TestBase {

	public static class HostPS extends FakeWriterSerializer {
		public HostPS(FakeWriterSerializer.Builder b) { super(b); }
	}

	public static class MixinPS extends FakeWriterSerializer {
		public MixinPS(FakeWriterSerializer.Builder b) { super(b); }
	}

	@Rest
	public static class M_NoPSDeclared {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(partSerializer=MixinPS.class)
	public static class M_MixinPS {
		@RestGet(path="/my") public String my() { return "my"; }
	}

	@Rest(noInherit="partSerializer", partSerializer=MixinPS.class)
	public static class M_NoInheritPS {
		@RestGet(path="/my") public String my() { return "my"; }
	}

	@Rest(partSerializer=HostPS.class, mixins={M_NoPSDeclared.class})
	public static class HostInheritsToMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Rest(partSerializer=HostPS.class, mixins={M_MixinPS.class})
	public static class HostWithMixinOverride extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Rest(partSerializer=HostPS.class, mixins={M_NoInheritPS.class})
	public static class HostWithNoInherit extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Test void a01_mixinInheritsHostPartSerializer() throws Exception {
		MockRestClient.buildLax(HostInheritsToMixin.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostInheritsToMixin.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_NoPSDeclared.class);
		assertNotNull(mixinCtx);

		assertEquals(HostPS.class, hostCtx.getPartSerializer().getClass(),
			"Host must use its declared HostPS part serializer");
		assertEquals(HostPS.class, mixinCtx.getPartSerializer().getClass(),
			"Mixin with no partSerializer override must inherit the host's HostPS part serializer");
	}

	@Test void a02_mixinOverridesHostPartSerializer() throws Exception {
		MockRestClient.buildLax(HostWithMixinOverride.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostWithMixinOverride.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_MixinPS.class);
		assertNotNull(mixinCtx);

		assertEquals(HostPS.class, hostCtx.getPartSerializer().getClass(),
			"Host's partSerializer must remain HostPS &mdash; mixin contributions are scoped to mixin sub-context");
		assertEquals(MixinPS.class, mixinCtx.getPartSerializer().getClass(),
			"Mixin's own @Rest(partSerializer=MixinPS) must win as most-derived in walk");
	}

	@Test void a03_mixinNoInheritPartSerializer() throws Exception {
		MockRestClient.buildLax(HostWithNoInherit.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostWithNoInherit.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_NoInheritPS.class);
		assertNotNull(mixinCtx);

		assertEquals(HostPS.class, hostCtx.getPartSerializer().getClass(),
			"Host's partSerializer must remain HostPS regardless of mixin's noInherit");
		assertEquals(MixinPS.class, mixinCtx.getPartSerializer().getClass(),
			"Mixin with noInherit=\"partSerializer\" must use its own MixinPS only");
	}
}
