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

import java.util.*;

import org.apache.juneau.TestBase;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.arg.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Phase 2 regression matrix &mdash; verifies that {@code @Rest(restOpArgs=...)} on a mixin class inherits the
 * host's REST-op-arg chain via the {@link RestContext#getRestAnnotationsForProperty(String) annotation-property
 * walk}.
 */
class MixinInheritance_RestOpArgs_Test extends TestBase {

	public static class HostArg implements RestOpArg {
		public static HostArg create(ParameterInfo pi) { return null; }
		@Override public Object resolve(RestOpSession s) { return null; }
	}

	public static class MixinArg implements RestOpArg {
		public static MixinArg create(ParameterInfo pi) { return null; }
		@Override public Object resolve(RestOpSession s) { return null; }
	}

	@Rest
	public static class M_Empty {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(restOpArgs={MixinArg.class})
	public static class M_AppendsMixinArg {
		@RestGet(path="/my") public String my() { return "my"; }
	}

	@Rest(noInherit="restOpArgs", restOpArgs={MixinArg.class})
	public static class M_NoInheritArg {
		@RestGet(path="/my") public String my() { return "my"; }
	}

	@Rest(restOpArgs={HostArg.class}, mixins={M_Empty.class})
	public static class HostBasic extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Rest(restOpArgs={HostArg.class}, mixins={M_AppendsMixinArg.class})
	public static class HostWithMixinArg extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Rest(restOpArgs={HostArg.class}, mixins={M_NoInheritArg.class})
	public static class HostWithNoInheritMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	private static List<Class<?>> argsOf(RestContext c) { return Arrays.asList(c.getRestOpArgs()); }

	@Test void a01_mixinInheritsHostRestOpArg() throws Exception {
		MockRestClient.buildLax(HostBasic.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostBasic.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_Empty.class);
		assertNotNull(mixinCtx);

		assertTrue(argsOf(hostCtx).contains(HostArg.class),
			"Host must register its declared HostArg in the restOpArgs chain");
		assertTrue(argsOf(mixinCtx).contains(HostArg.class),
			"Mixin with no restOpArgs overrides must inherit the host's HostArg");
	}

	@Test void a02_mixinAppendsMixinArgOverInheritedHostSet() throws Exception {
		MockRestClient.buildLax(HostWithMixinArg.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostWithMixinArg.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_AppendsMixinArg.class);
		assertNotNull(mixinCtx);

		assertFalse(argsOf(hostCtx).contains(MixinArg.class),
			"Host endpoint must NOT have MixinArg — mixin contributions are scoped to the mixin context");
		assertTrue(argsOf(mixinCtx).contains(MixinArg.class),
			"Mixin endpoint must have MixinArg via the mixin's own @Rest(restOpArgs=)");
		assertTrue(argsOf(mixinCtx).contains(HostArg.class),
			"Mixin endpoint must still have the host's HostArg (inheritance walk)");
	}

	@Test void a03_noInheritBlocksParentRestOpArgsWalk() throws Exception {
		MockRestClient.buildLax(HostWithNoInheritMixin.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostWithNoInheritMixin.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_NoInheritArg.class);
		assertNotNull(mixinCtx);

		assertTrue(argsOf(mixinCtx).contains(MixinArg.class),
			"Mixin's own MixinArg must be present");
		assertFalse(argsOf(mixinCtx).contains(HostArg.class),
			"Mixin with noInherit=\"restOpArgs\" must NOT see the host's HostArg");

		assertTrue(argsOf(hostCtx).contains(HostArg.class),
			"Host must retain its HostArg regardless of mixin's noInherit");
		assertFalse(argsOf(hostCtx).contains(MixinArg.class),
			"Host must NOT pick up MixinArg from a noInherit-isolated mixin");
	}
}
