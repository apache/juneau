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

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.processor.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Phase 2 regression matrix &mdash; verifies that {@code @Rest(responseProcessors=...)} on a mixin class inherits
 * the host's response-processor chain via the {@link RestContext#getRestAnnotationsForProperty(String)
 * annotation-property walk}.
 */
class MixinInheritance_ResponseProcessors_Test extends TestBase {

	public static class HostRp1 implements ResponseProcessor {
		@Override public int process(RestOpSession s) throws IOException, NotAcceptable, BasicHttpException { return NEXT; }
	}

	public static class MixinRp1 implements ResponseProcessor {
		@Override public int process(RestOpSession s) throws IOException, NotAcceptable, BasicHttpException { return NEXT; }
	}

	@Rest
	public static class M_Empty {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(responseProcessors={MixinRp1.class})
	public static class M_AppendsMixinRp {
		@RestGet(path="/my") public String my() { return "my"; }
	}

	@Rest(noInherit="responseProcessors", responseProcessors={MixinRp1.class})
	public static class M_NoInheritRp {
		@RestGet(path="/my") public String my() { return "my"; }
	}

	@Rest(responseProcessors={HostRp1.class}, mixins={M_Empty.class})
	public static class HostBasic extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Rest(responseProcessors={HostRp1.class}, mixins={M_AppendsMixinRp.class})
	public static class HostWithMixinRp extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Rest(responseProcessors={HostRp1.class}, mixins={M_NoInheritRp.class})
	public static class HostWithNoInheritMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	private static List<Class<?>> classesOf(ResponseProcessor[] rps) {
		var l = new ArrayList<Class<?>>(rps.length);
		for (var rp : rps) l.add(rp.getClass());
		return l;
	}

	@Test void a01_mixinInheritsHostResponseProcessor() {
		MockRestClient.buildLax(HostBasic.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostBasic.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_Empty.class);
		assertNotNull(mixinCtx);

		assertTrue(classesOf(hostCtx.getResponseProcessors()).contains(HostRp1.class),
			"Host must register its declared HostRp1 response processor");
		assertTrue(classesOf(mixinCtx.getResponseProcessors()).contains(HostRp1.class),
			"Mixin with no responseProcessors overrides must inherit the host's HostRp1");
	}

	@Test void a02_mixinAppendsMixinRpOverInheritedHostSet() {
		MockRestClient.buildLax(HostWithMixinRp.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostWithMixinRp.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_AppendsMixinRp.class);
		assertNotNull(mixinCtx);

		assertFalse(classesOf(hostCtx.getResponseProcessors()).contains(MixinRp1.class),
			"Host endpoint must NOT have MixinRp1 — mixin contributions are scoped to the mixin context");
		assertTrue(classesOf(mixinCtx.getResponseProcessors()).contains(MixinRp1.class),
			"Mixin endpoint must have MixinRp1 via the mixin's own @Rest(responseProcessors=)");
		assertTrue(classesOf(mixinCtx.getResponseProcessors()).contains(HostRp1.class),
			"Mixin endpoint must still have the host's HostRp1 (inheritance walk)");
	}

	@Test void a03_noInheritBlocksParentRpWalk() {
		MockRestClient.buildLax(HostWithNoInheritMixin.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostWithNoInheritMixin.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_NoInheritRp.class);
		assertNotNull(mixinCtx);

		assertTrue(classesOf(mixinCtx.getResponseProcessors()).contains(MixinRp1.class),
			"Mixin's own MixinRp1 must be present");
		assertFalse(classesOf(mixinCtx.getResponseProcessors()).contains(HostRp1.class),
			"Mixin with noInherit=\"responseProcessors\" must NOT see the host's HostRp1");

		assertTrue(classesOf(hostCtx.getResponseProcessors()).contains(HostRp1.class),
			"Host must retain its HostRp1 regardless of mixin's noInherit");
		assertFalse(classesOf(hostCtx.getResponseProcessors()).contains(MixinRp1.class),
			"Host must NOT pick up MixinRp1 from a noInherit-isolated mixin");
	}
}
