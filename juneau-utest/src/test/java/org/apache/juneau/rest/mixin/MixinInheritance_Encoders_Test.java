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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.TestBase;
import org.apache.juneau.encoders.*;
import org.apache.juneau.rest.RestContext;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Phase 2 regression matrix &mdash; verifies that {@code @Rest(encoders=...)} on a mixin class inherits the host's
 * encoder set via the {@link RestContext#getRestAnnotationsForProperty(String) annotation-property walk}.
 *
 * <p>
 * Uses uniquely-coded test encoders ({@code host-enc-1}, {@code mixin-enc-1}) so the implicit identity encoder
 * registered by every {@link EncoderSet} can't accidentally mask the assertion.
 */
class MixinInheritance_Encoders_Test extends TestBase {

	public static class HostEnc1 extends Encoder {
		@Override public String[] getCodings() { return a("host-enc-1"); }
		@Override public InputStream getInputStream(InputStream is) { return is; }
		@Override public OutputStream getOutputStream(OutputStream os) { return os; }
	}

	public static class MixinEnc1 extends Encoder {
		@Override public String[] getCodings() { return a("mixin-enc-1"); }
		@Override public InputStream getInputStream(InputStream is) { return is; }
		@Override public OutputStream getOutputStream(OutputStream os) { return os; }
	}

	@Rest
	public static class M_Empty {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(encoders={MixinEnc1.class})
	public static class M_AppendsMixinEnc {
		@RestGet(path="/my") public String my() { return "my"; }
	}

	@Rest(noInherit="encoders", encoders={MixinEnc1.class})
	public static class M_NoInheritEnc {
		@RestGet(path="/my") public String my() { return "my"; }
	}

	@Rest(encoders={HostEnc1.class}, mixins={M_Empty.class})
	public static class HostBasic extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Rest(encoders={HostEnc1.class}, mixins={M_AppendsMixinEnc.class})
	public static class HostWithMixinEnc extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Rest(encoders={HostEnc1.class}, mixins={M_NoInheritEnc.class})
	public static class HostWithNoInheritMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Test void a01_mixinInheritsHostEncoder() throws Exception {
		MockRestClient.buildLax(HostBasic.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostBasic.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_Empty.class);
		assertNotNull(mixinCtx);

		assertNotNull(hostCtx.getEncoders().getEncoder("host-enc-1"),
			"Host must register its declared HostEnc1 encoder");
		assertNotNull(mixinCtx.getEncoders().getEncoder("host-enc-1"),
			"Mixin with no encoder overrides must inherit the host's HostEnc1 encoder");
	}

	@Test void a02_mixinAppendsMixinEncOverInheritedHostSet() throws Exception {
		MockRestClient.buildLax(HostWithMixinEnc.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostWithMixinEnc.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_AppendsMixinEnc.class);
		assertNotNull(mixinCtx);

		assertNull(hostCtx.getEncoders().getEncoder("mixin-enc-1"),
			"Host endpoint must NOT have MixinEnc1 — mixin contributions are scoped to the mixin context");
		assertNotNull(mixinCtx.getEncoders().getEncoder("mixin-enc-1"),
			"Mixin endpoint must have MixinEnc1 via the mixin's own @Rest(encoders=)");
		assertNotNull(mixinCtx.getEncoders().getEncoder("host-enc-1"),
			"Mixin endpoint must still have the host's HostEnc1 (inheritance walk)");
	}

	@Test void a03_noInheritBlocksParentEncoderWalk() throws Exception {
		MockRestClient.buildLax(HostWithNoInheritMixin.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostWithNoInheritMixin.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_NoInheritEnc.class);
		assertNotNull(mixinCtx);

		assertNotNull(mixinCtx.getEncoders().getEncoder("mixin-enc-1"),
			"Mixin's own MixinEnc1 must be present");
		assertNull(mixinCtx.getEncoders().getEncoder("host-enc-1"),
			"Mixin with noInherit=\"encoders\" must NOT see the host's HostEnc1 (parent walk blocked)");

		assertNotNull(hostCtx.getEncoders().getEncoder("host-enc-1"),
			"Host must retain its HostEnc1 regardless of mixin's noInherit");
		assertNull(hostCtx.getEncoders().getEncoder("mixin-enc-1"),
			"Host must NOT pick up MixinEnc1 from a noInherit-isolated mixin");
	}
}
