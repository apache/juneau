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
 * Phase 2 — exercises {@code @Rest(noInherit={"serializers"})} on a mixin class to verify it blocks the
 * parent-context walk for that property.
 *
 * <p>
 * Uses uniquely-named test serializers so the {@link BasicRestServlet} default set can't accidentally mask the
 * assertion that the host's serializers are excluded from the mixin's chain.
 *
 * <p>
 * Acceptance:
 * <ul>
 * 	<li>A mixin declaring {@code noInherit={"serializers"}} sees ONLY its own serializers; the host's declared
 * 		serializer (HostS1) does NOT bleed through.</li>
 * 	<li>The host's serializer set remains unchanged by the mixin's {@code noInherit} declaration (host still has
 * 		HostS1; mixin's MixinS1 does NOT leak into the host's set).</li>
 * </ul>
 */
class MixinInheritance_NoInherit_Test extends TestBase {

	public static class HostS1 extends FakeWriterSerializer {
		public HostS1(FakeWriterSerializer.Builder b) { super(b.produces("text/host-s1")); }
	}

	public static class MixinS1 extends FakeWriterSerializer {
		public MixinS1(FakeWriterSerializer.Builder b) { super(b.produces("text/mixin-s1")); }
	}

	@Rest(
		noInherit = "serializers",
		serializers = {MixinS1.class}
	)
	public static class M_MixinS1Only {
		@RestGet(path="/my") public String my() { return "my"; }
	}

	@Rest(serializers={HostS1.class}, mixins={M_MixinS1Only.class})
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Test void a01_noInheritBlocksParentSerializerWalk() {
		MockRestClient.buildLax(Host.class);
		var hostCtx = RestContext.getGlobalRegistry().get(Host.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_MixinS1Only.class);
		assertNotNull(mixinCtx);

		assertNotNull(mixinCtx.getSerializers().getSerializer("text/mixin-s1"),
			"Mixin's own MixinS1 must be present");
		assertNull(mixinCtx.getSerializers().getSerializer("text/host-s1"),
			"Mixin with noInherit=\"serializers\" must NOT see the host's HostS1 (parent walk blocked)");
	}

	@Test void a02_hostUnaffectedByMixinNoInherit() {
		MockRestClient.buildLax(Host.class);
		var hostCtx = RestContext.getGlobalRegistry().get(Host.class);

		assertNotNull(hostCtx.getSerializers().getSerializer("text/host-s1"),
			"Host must retain its declared HostS1 serializer regardless of mixin's noInherit");
		assertNull(hostCtx.getSerializers().getSerializer("text/mixin-s1"),
			"Host must NOT pick up MixinS1 from a noInherit-isolated mixin");
	}
}
