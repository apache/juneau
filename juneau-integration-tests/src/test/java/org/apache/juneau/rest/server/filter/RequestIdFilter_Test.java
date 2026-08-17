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
package org.apache.juneau.rest.server.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * {@link RequestIdFilter} reconciled as a thin façade over the always-on {@link RestSession} resolver.
 *
 * <p>
 * Proves the filter no longer owns the lifecycle: its {@code apply()} re-echoes the id already resolved at session
 * build (idempotent), its builder still rejects null/blank arguments (retained contracts), and its per-instance tuning
 * knobs are documented no-ops (the real knobs live on {@link RequestIdSettings}).
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S5778"  // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice
})
class RequestIdFilter_Test extends TestBase {

	@Rest
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		private static final RequestIdFilter FILTER = RequestIdFilter.create().build();
		@RestStartCall
		public void stamp(HttpServletRequest req, HttpServletResponse res) {
			FILTER.apply(req, res);
		}
		@RestGet(path="/a")
		public String a(RestRequest req) {
			return req.getAttribute(RestServerConstants.REQUEST_ID).asString().orElse("");
		}
	}

	@Test void a01_alwaysOnMintsRequestId() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.get("/a").run()
			.assertStatus(200)
			.assertHeader("X-Request-Id").isExists();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Builder still rejects null/blank arguments (retained contracts even though the knobs are no-ops).
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_builderRejectsNullSupplier() {
		assertThrows(IllegalArgumentException.class, () -> RequestIdFilter.create().idSupplier(null));
	}

	@Test void b02_builderRejectsNullValidator() {
		assertThrows(IllegalArgumentException.class, () -> RequestIdFilter.create().validator(null));
	}

	@Test void b03_builderRejectsNullAttributeKey() {
		assertThrows(IllegalArgumentException.class, () -> RequestIdFilter.create().attributeKey(null));
	}

	@Test void b04_builderRejectsBlankAttributeKey() {
		assertThrows(IllegalArgumentException.class, () -> RequestIdFilter.create().attributeKey("   "));
	}

	//------------------------------------------------------------------------------------------------------------------
	// apply() rejects null arguments.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_applyRejectsNullRequest() {
		assertThrows(IllegalArgumentException.class, () -> RequestIdFilter.create().build().apply(null, null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// The per-instance idSupplier knob is a documented no-op — the always-on resolver mints the id, not the filter.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		private static final RequestIdFilter FILTER = RequestIdFilter.create()
			.idSupplier(() -> "fixed-id-123")
			.build();
		@RestStartCall
		public void stamp(HttpServletRequest req, HttpServletResponse res) {
			FILTER.apply(req, res);
		}
		@RestGet(path="/d")
		public String d() { return "ok"; }
	}

	@Test void d01_idSupplierKnobIsNoOp() throws Exception {
		var c = MockRestClient.buildLax(D.class);
		var echoed = c.get("/d").run()
			.assertStatus(200)
			.getHeader("X-Request-Id").asString().orElseThrow();
		// The resolver minted the id; the filter's idSupplier knob had no effect.
		assertFalse(echoed.isEmpty());
		assertNotEquals("fixed-id-123", echoed);
	}
}
