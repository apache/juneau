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
package org.apache.juneau.rest.mock;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Always-on request-id correlation resolver (built into {@link RestSession} at session-build time).
 *
 * <p>
 * Proves the resolver is primary and filter-independent: a resource with <b>no</b> {@code RequestIdFilter} still mints,
 * honors, sanitizes, caches, and echoes a correlation id, reachable through the {@code RestSession.fromRequest(req)}
 * session-handle seam.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient instances are short-lived test fixtures.
})
class RestSessionRequestId_Test {

	@Rest
	public static class A_Resource {
		@RestGet(path="/echo")
		public String echo(HttpServletRequest req) {
			// Exercises the session-handle seam + the cached getRequestId() (never a live attribute read).
			return RestSession.fromRequest(req).getRequestId();
		}
	}

	@Test void a01_alwaysOnMintsAndEchoes_noFilter() throws Exception {
		var c = org.apache.juneau.rest.mock.classic.MockRestClient.buildLax(A_Resource.class);
		var res = c.get("/echo").run().assertStatus(200);
		var echoed = res.getHeader("X-Request-Id").asString().orElseThrow();
		assertFalse(echoed.isEmpty());
		// The response body is session.getRequestId() — proves the cached id equals the echoed header.
		res.assertContent().is(echoed);
	}

	@Test void a02_honorsValidIncomingHeader() throws Exception {
		var c = org.apache.juneau.rest.mock.classic.MockRestClient.buildLax(A_Resource.class);
		var id = "550e8400-e29b-41d4-a716-446655440000";
		var res = c.get("/echo").header("X-Request-Id", id).run().assertStatus(200);
		res.assertHeader("X-Request-Id").is(id);
		res.assertContent().is(id);
	}

	@Test void a03_mintsOnOversizeTruncated() throws Exception {
		var c = org.apache.juneau.rest.mock.classic.MockRestClient.buildLax(A_Resource.class);
		var oversize = "a".repeat(200);  // exceeds MAX_LEN=128 → sanitize truncates → reminted, not echoed truncated
		var res = c.get("/echo").header("X-Request-Id", oversize).run().assertStatus(200);
		var echoed = res.getHeader("X-Request-Id").asString().orElseThrow();
		assertNotEquals(oversize, echoed);
		assertFalse(echoed.contains("\u2026"), "reminted id must not carry the truncation marker");
	}

	@Test void a04_sanitizeAndAccept_honorsNonRegexShape() throws Exception {
		// '.' and ':' are printable (sanitizer leaves them) but were rejected by the old reject-and-remint regex.
		// Sanitize-and-accept honors this cleaned, HTTP-safe id verbatim rather than discarding it.
		var c = org.apache.juneau.rest.mock.classic.MockRestClient.buildLax(A_Resource.class);
		var id = "trace.123:abc-DEF_456";
		var res = c.get("/echo").header("X-Request-Id", id).run().assertStatus(200);
		res.assertHeader("X-Request-Id").is(id);
		res.assertContent().is(id);
	}
}
