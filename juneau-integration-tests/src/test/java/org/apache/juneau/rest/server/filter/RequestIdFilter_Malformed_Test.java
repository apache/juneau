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
 * Incoming-id handling under the always-on resolver, exercised through the {@link RequestIdFilter} façade.
 *
 * <p>
 * The resolver defaults to <b>sanitize-and-accept</b> (not the old reject-and-remint regex): a cleaned id within the
 * length cap is honored verbatim; only an oversize/truncated or empty candidate is reminted.  The filter's per-instance
 * validator / idSupplier / attributeKey knobs are documented no-ops, and {@code apply()} is idempotent (re-echoes the
 * session-cached id, immune to later attribute tampering).
 *
 * @since 10.0.0
 */
class RequestIdFilter_Malformed_Test extends TestBase {

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

	@Test void a01_spaceContainingIdIsSanitizeAccepted() throws Exception {
		// A space is printable (the sanitizer leaves it) so sanitize-and-accept honors this cleaned id verbatim,
		// where the old reject-and-remint regex would have discarded it.
		var c = MockRestClient.create(A.class).ignoreErrors().json().build();
		var res = c.get("/a").header("X-Request-Id", "abc xyz").run().assertStatus(200);
		res.assertHeader("X-Request-Id").is("abc xyz");
		res.assertContent().asString().isContains("abc xyz");
	}

	@Test void a02_oversizeIdIsReminted() throws Exception {
		var c = MockRestClient.create(A.class).ignoreErrors().json().build();
		var oversize = "a".repeat(200);  // exceeds MAX_LEN=128 → sanitize truncates → reminted, not echoed truncated
		var res = c.get("/a").header("X-Request-Id", oversize).run().assertStatus(200);
		var echoed = res.getHeader("X-Request-Id").asString().orElseThrow();
		assertNotEquals(oversize, echoed);
		assertFalse(echoed.contains("\u2026"), "reminted id must not carry the truncation marker");
	}

	@Test void a03_emptyIdIsReminted() throws Exception {
		var c = MockRestClient.create(A.class).ignoreErrors().json().build();
		var res = c.get("/a").header("X-Request-Id", "").run().assertStatus(200);
		var echoed = res.getHeader("X-Request-Id").asString().orElseThrow();
		assertFalse(echoed.isEmpty());
	}

	//------------------------------------------------------------------------------------------------------------------
	// The validator / idSupplier knobs are no-ops: a valid incoming id is honored, not replaced by the filter supplier.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		private static final RequestIdFilter FILTER = RequestIdFilter.create()
			.validator(s -> false)
			.idSupplier(() -> "always-minted")
			.build();
		@RestStartCall
		public void stamp(HttpServletRequest req, HttpServletResponse res) {
			FILTER.apply(req, res);
		}
		@RestGet(path="/b")
		public String b() { return "ok"; }
	}

	@Test void b01_validatorAndSupplierKnobsAreNoOps() throws Exception {
		var c = MockRestClient.buildLax(B.class);
		// The resolver's sanitize-and-accept honors the valid incoming id; the filter's reject-all validator and
		// custom supplier had no effect (id is neither reminted nor "always-minted").
		c.get("/b").header("X-Request-Id", "550e8400-e29b-41d4-a716-446655440000").run()
			.assertStatus(200)
			.assertHeader("X-Request-Id").is("550e8400-e29b-41d4-a716-446655440000");
	}

	//------------------------------------------------------------------------------------------------------------------
	// apply() is idempotent: two calls re-echo the same session-cached id under the default key (attributeKey no-op).
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		private static final RequestIdFilter FILTER = RequestIdFilter.create()
			.attributeKey("customReqId")
			.idSupplier(() -> "minted-once")
			.build();
		@RestStartCall
		public void stamp(HttpServletRequest req, HttpServletResponse res) {
			FILTER.apply(req, res);
			FILTER.apply(req, res);
		}
		@RestGet(path="/c")
		public String c(RestRequest req) {
			var underDefault = req.getAttribute(RestServerConstants.REQUEST_ID).asString().orElse("");
			var underCustom = req.getAttribute("customReqId").asString().orElse("");
			return underDefault + "|" + underCustom;
		}
	}

	@Test void c01_applyIsIdempotentAndAttributeKeyKnobIsNoOp() throws Exception {
		var c = MockRestClient.buildLax(C.class);
		var res = c.get("/c").run().assertStatus(200);
		var echoed = res.getHeader("X-Request-Id").asString().orElseThrow();
		// Idempotent re-entry: the echoed id is neither the filter supplier's value nor blank...
		assertFalse(echoed.isEmpty());
		assertNotEquals("minted-once", echoed);
		// ...and it lives under the default key, not the (no-op) custom key (body renders "<default>|<custom>").
		res.assertContent().asString().isContains(echoed + "|");
	}

	//------------------------------------------------------------------------------------------------------------------
	// apply() reads the session cache, not the request attribute: tampering with the attribute cannot change the echo.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		private static final RequestIdFilter FILTER = RequestIdFilter.create()
			.idSupplier(() -> "fresh-after-empty")
			.build();
		@RestStartCall
		public void stamp(HttpServletRequest req, HttpServletResponse res) {
			req.setAttribute(RestServerConstants.REQUEST_ID, "");
			FILTER.apply(req, res);
		}
		@RestGet(path="/d")
		public String d() { return "ok"; }
	}

	@Test void d01_applyReadsSessionCacheNotAttribute() throws Exception {
		var c = MockRestClient.buildLax(D.class);
		var echoed = c.get("/d").run()
			.assertStatus(200)
			.getHeader("X-Request-Id").asString().orElseThrow();
		// Even though the app blanked the attribute, apply() re-echoed the session-cached id (knob still a no-op).
		assertFalse(echoed.isEmpty());
		assertNotEquals("fresh-after-empty", echoed);
	}
}
