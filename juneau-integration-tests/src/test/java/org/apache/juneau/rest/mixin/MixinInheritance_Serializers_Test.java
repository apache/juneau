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
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.utest.utils.*;
import org.junit.jupiter.api.*;

/**
 * Phase 2 — exercises the {@code getRestAnnotationsForProperty(...)} parent-context walk that drives
 * inheritance of {@code @Rest(serializers=...)} from a mixin's host context.
 *
 * <p>
 * Uses uniquely-named test serializers ({@code text/host-s1}, {@code text/mixin-s1}) so the BasicUniversalConfig
 * default set (HTML, JSON, YAML, ...) inherited by {@link BasicRestServlet} can't accidentally mask the assertion
 * &mdash; the test serializers are the only ones with those media types.
 *
 * <p>
 * Acceptance:
 * <ul>
 * 	<li>Mixin without serializer declarations inherits the host's full serializer set (host-s1 visible from mixin).</li>
 * 	<li>A mixin's {@code @Rest(serializers=...)} appends to the inherited host set; host endpoints remain
 * 		unaffected by mixin contributions (mixin-s1 visible from mixin, NOT visible from host).</li>
 * </ul>
 */
class MixinInheritance_Serializers_Test extends TestBase {

	public static class HostS1 extends FakeWriterSerializer {
		public HostS1(FakeWriterSerializer.Builder b) { super(b.produces("text/host-s1")); }
	}

	public static class MixinS1 extends FakeWriterSerializer {
		public MixinS1(FakeWriterSerializer.Builder b) { super(b.produces("text/mixin-s1")); }
	}

	@Rest
	public static class M_Empty {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(serializers={MixinS1.class})
	public static class M_AppendsMixinS1 {
		@RestGet(path="/my") public String my() { return "my"; }
	}

	@Rest(serializers={HostS1.class}, mixins={M_Empty.class})
	public static class HostBasic extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Rest(serializers={HostS1.class}, mixins={M_AppendsMixinS1.class})
	public static class HostWithMixinS1 extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Test void a01_mixinInheritsHostSerializer() {
		MockRestClient.buildLax(HostBasic.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostBasic.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_Empty.class);
		assertNotNull(mixinCtx);

		assertTrue(hostCtx.getSerializers().getSerializer("text/host-s1").isPresent(),
			"Host must register its declared HostS1 serializer");
		assertTrue(mixinCtx.getSerializers().getSerializer("text/host-s1").isPresent(),
			"Mixin with no serializer overrides must inherit the host's HostS1 serializer");
	}

	@Test void a02_mixinAppendsMixinS1OverInheritedHostSet() {
		MockRestClient.buildLax(HostWithMixinS1.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostWithMixinS1.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_AppendsMixinS1.class);
		assertNotNull(mixinCtx);

		assertTrue(hostCtx.getSerializers().getSerializer("text/mixin-s1").isEmpty(),
			"Host endpoint must NOT have MixinS1 — mixin contributions are scoped to the mixin context");
		assertTrue(mixinCtx.getSerializers().getSerializer("text/mixin-s1").isPresent(),
			"Mixin endpoint must have MixinS1 via the mixin's own @Rest(serializers=)");
		assertTrue(mixinCtx.getSerializers().getSerializer("text/host-s1").isPresent(),
			"Mixin endpoint must still have the host's HostS1 (inheritance walk)");
	}
}
