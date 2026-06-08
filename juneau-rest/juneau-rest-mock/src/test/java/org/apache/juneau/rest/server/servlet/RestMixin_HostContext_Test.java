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
package org.apache.juneau.rest.server.servlet;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link RestMixin#getHostContext()}.
 *
 * <p>
 * Verifies the opt-in host-context accessor: it returns the host {@link RestContext} when the mixin is
 * composed via {@code @Rest(mixins=...)}, returns {@code null} when the mixin is used standalone, and
 * resolves to the single flat top-level host under nested mixins (FINISHED-81 flat-inheritance rule).
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient instances are short-lived test fixtures; the mock framework manages lifecycle.
})
class RestMixin_HostContext_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// a01: Composed as a mixin -> getHostContext() returns the host context.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A_Mixin extends RestMixin {
		@RestGet(path="/a") public String a() { return "a"; }
	}

	@Rest(mixins=A_Mixin.class)
	public static class A_Host extends RestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_getHostContext_returnsHost_whenComposedAsMixin() {
		var host = new A_Host();
		MockRestClient.buildLax(host);
		var hostCtx = host.getContext();
		var mixinInstance = (RestMixin) hostCtx.getMixinContexts().get(A_Mixin.class).getResource();
		assertSame(hostCtx, mixinInstance.getHostContext(),
			"getHostContext() must return the host context when composed as a mixin.");
	}

	//------------------------------------------------------------------------------------------------------------------
	// a02: Standalone (never composed) -> getHostContext() returns null.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A02_Mixin extends RestMixin {
		@RestGet(path="/a") public String a() { return "a"; }
	}

	@Test void a02_getHostContext_returnsNull_standalone() {
		assertNull(new A02_Mixin().getHostContext(),
			"getHostContext() must return null when the mixin is never composed into a host.");
	}

	//------------------------------------------------------------------------------------------------------------------
	// a03: Nested mixins -> every mixin resolves to the same flat top-level host (no chaining).
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A03_Inner extends RestMixin {
		@RestGet(path="/inner") public String inner() { return "inner"; }
	}

	@Rest(mixins=A03_Inner.class)
	public static class A03_Outer extends RestMixin {
		@RestGet(path="/outer") public String outer() { return "outer"; }
	}

	@Rest(mixins=A03_Outer.class)
	public static class A03_Host extends RestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a03_getHostContext_flatTopHost_underNestedMixins() {
		var host = new A03_Host();
		MockRestClient.buildLax(host);
		var hostCtx = host.getContext();
		var outer = (RestMixin) hostCtx.getMixinContexts().get(A03_Outer.class).getResource();
		var inner = (RestMixin) hostCtx.getMixinContexts().get(A03_Inner.class).getResource();
		assertSame(hostCtx, outer.getHostContext(), "Outer mixin must resolve to the top-level host.");
		assertSame(hostCtx, inner.getHostContext(),
			"Nested inner mixin must resolve to the SAME flat top-level host (not the outer mixin).");
	}
}
