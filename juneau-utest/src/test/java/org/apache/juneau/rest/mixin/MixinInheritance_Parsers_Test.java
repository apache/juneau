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
import org.apache.juneau.commons.http.MediaType;
import org.apache.juneau.rest.RestContext;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.utest.utils.*;
import org.junit.jupiter.api.*;

/**
 * Phase 2 regression matrix &mdash; verifies that {@code @Rest(parsers=...)} on a mixin class inherits the host's
 * parser set via the {@link RestContext#getRestAnnotationsForProperty(String) annotation-property walk}.
 *
 * <p>
 * Uses uniquely-named test parsers ({@code text/host-p1}, {@code text/mixin-p1}) so the default parser set inherited
 * by {@link BasicRestServlet} can't accidentally mask the assertion.
 */
class MixinInheritance_Parsers_Test extends TestBase {

	public static class HostP1 extends FakeReaderParser {
		public HostP1(FakeReaderParser.Builder b) { super(b.consumes("text/host-p1")); }
	}

	public static class MixinP1 extends FakeReaderParser {
		public MixinP1(FakeReaderParser.Builder b) { super(b.consumes("text/mixin-p1")); }
	}

	@Rest
	public static class M_Empty {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(parsers={MixinP1.class})
	public static class M_AppendsMixinP1 {
		@RestGet(path="/my") public String my() { return "my"; }
	}

	@Rest(parsers={HostP1.class}, mixins={M_Empty.class})
	public static class HostBasic extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Rest(parsers={HostP1.class}, mixins={M_AppendsMixinP1.class})
	public static class HostWithMixinP1 extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Rest(parsers={HostP1.class}, mixins={M_NoInheritP1.class})
	public static class HostWithNoInheritMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Rest(noInherit="parsers", parsers={MixinP1.class})
	public static class M_NoInheritP1 {
		@RestGet(path="/my") public String my() { return "my"; }
	}

	@Test void a01_mixinInheritsHostParser() {
		MockRestClient.buildLax(HostBasic.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostBasic.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_Empty.class);
		assertNotNull(mixinCtx);

		assertNotNull(hostCtx.getParsers().getParser(MediaType.of("text/host-p1")),
			"Host must register its declared HostP1 parser");
		assertNotNull(mixinCtx.getParsers().getParser(MediaType.of("text/host-p1")),
			"Mixin with no parser overrides must inherit the host's HostP1 parser");
	}

	@Test void a02_mixinAppendsMixinP1OverInheritedHostSet() {
		MockRestClient.buildLax(HostWithMixinP1.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostWithMixinP1.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_AppendsMixinP1.class);
		assertNotNull(mixinCtx);

		assertNull(hostCtx.getParsers().getParser(MediaType.of("text/mixin-p1")),
			"Host endpoint must NOT have MixinP1 — mixin contributions are scoped to the mixin context");
		assertNotNull(mixinCtx.getParsers().getParser(MediaType.of("text/mixin-p1")),
			"Mixin endpoint must have MixinP1 via the mixin's own @Rest(parsers=)");
		assertNotNull(mixinCtx.getParsers().getParser(MediaType.of("text/host-p1")),
			"Mixin endpoint must still have the host's HostP1 (inheritance walk)");
	}

	@Test void a03_noInheritBlocksParentParserWalk() {
		MockRestClient.buildLax(HostWithNoInheritMixin.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostWithNoInheritMixin.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_NoInheritP1.class);
		assertNotNull(mixinCtx);

		assertNotNull(mixinCtx.getParsers().getParser(MediaType.of("text/mixin-p1")),
			"Mixin's own MixinP1 must be present");
		assertNull(mixinCtx.getParsers().getParser(MediaType.of("text/host-p1")),
			"Mixin with noInherit=\"parsers\" must NOT see the host's HostP1 (parent walk blocked)");

		assertNotNull(hostCtx.getParsers().getParser(MediaType.of("text/host-p1")),
			"Host must retain its HostP1 regardless of mixin's noInherit");
		assertNull(hostCtx.getParsers().getParser(MediaType.of("text/mixin-p1")),
			"Host must NOT pick up MixinP1 from a noInherit-isolated mixin");
	}
}
