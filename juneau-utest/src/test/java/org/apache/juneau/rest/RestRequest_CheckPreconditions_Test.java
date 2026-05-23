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
package org.apache.juneau.rest;

import java.time.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link RestRequest#checkPreconditions(RestResponse)} — RFC 7232 conditional-request handling.
 *
 * <p>
 * The test resource exposes endpoints that set well-known {@code ETag} and {@code Last-Modified} values on the
 * response and then short-circuits on {@code checkPreconditions(res)}: if the returned {@code Optional} is non-empty
 * the helper exception is thrown (yielding 304 / 412); otherwise the handler returns 200 with body {@code "OK"}.
 */
class RestRequest_CheckPreconditions_Test extends TestBase {

	// Fixed reference values used across tests.
	private static final String ETAG_STRONG = "\"v1\"";
	private static final String ETAG_WEAK = "W/\"v1\"";
	private static final String LM = "Fri, 22 May 2026 00:00:00 GMT";
	private static final String LM_EARLIER = "Thu, 21 May 2026 00:00:00 GMT";
	private static final String LM_LATER = "Sat, 23 May 2026 00:00:00 GMT";

	@Rest
	public static class R {
		@RestGet("/strong")
		public String strong(RestRequest req, RestResponse res) {
			res.eTag(ETAG_STRONG).lastModified(Instant.parse("2026-05-22T00:00:00Z"));
			req.checkPreconditions(res).ifPresent(e -> { throw e; });
			return "OK";
		}

		@RestGet("/weak")
		public String weak(RestRequest req, RestResponse res) {
			res.eTag(ETAG_WEAK).lastModified(Instant.parse("2026-05-22T00:00:00Z"));
			req.checkPreconditions(res).ifPresent(e -> { throw e; });
			return "OK";
		}

		@RestGet("/lmOnly")
		public String lmOnly(RestRequest req, RestResponse res) {
			res.lastModified(Instant.parse("2026-05-22T00:00:00Z"));
			req.checkPreconditions(res).ifPresent(e -> { throw e; });
			return "OK";
		}

		@RestGet("/etagOnly")
		public String etagOnly(RestRequest req, RestResponse res) {
			res.eTag(ETAG_STRONG);
			req.checkPreconditions(res).ifPresent(e -> { throw e; });
			return "OK";
		}

		@RestGet("/bare")
		public String bare(RestRequest req, RestResponse res) {
			req.checkPreconditions(res).ifPresent(e -> { throw e; });
			return "OK";
		}

		// Non-safe method to verify If-Modified-Since is ignored.
		@RestPost("/postStrong")
		public String postStrong(RestRequest req, RestResponse res) {
			res.eTag(ETAG_STRONG).lastModified(Instant.parse("2026-05-22T00:00:00Z"));
			req.checkPreconditions(res).ifPresent(e -> { throw e; });
			return "OK";
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// A: No conditional headers → 200 OK (baseline).
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_noConditionalHeadersPasses() throws Exception {
		var c = MockRestClient.buildLax(R.class);
		c.get("/strong").run().assertStatus(200).assertContent("OK");
	}

	@Test void a02_bareNoEtagNoLmPasses() throws Exception {
		var c = MockRestClient.buildLax(R.class);
		c.get("/bare").run().assertStatus(200).assertContent("OK");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// B: If-Match (strong comparison; 412 on failure).
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_ifMatchExactStrongMatches() throws Exception {
		var c = MockRestClient.buildLax(R.class);
		c.get("/strong").header("If-Match", ETAG_STRONG).run().assertStatus(200);
	}

	@Test void b02_ifMatchMismatchYields412() throws Exception {
		var c = MockRestClient.buildLax(R.class);
		c.get("/strong").header("If-Match", "\"v2\"").run().assertStatus(412);
	}

	@Test void b03_ifMatchWildcardWithEtagPresentMatches() throws Exception {
		var c = MockRestClient.buildLax(R.class);
		c.get("/strong").header("If-Match", "*").run().assertStatus(200);
	}

	@Test void b04_ifMatchWildcardWithNoEtagFails() throws Exception {
		var c = MockRestClient.buildLax(R.class);
		c.get("/lmOnly").header("If-Match", "*").run().assertStatus(412);
	}

	@Test void b05_ifMatchAgainstWeakEtagFails() throws Exception {
		// RFC 7232 §3.1: If-Match requires strong comparison; weak server ETag never matches.
		var c = MockRestClient.buildLax(R.class);
		c.get("/weak").header("If-Match", ETAG_WEAK).run().assertStatus(412);
		c.get("/weak").header("If-Match", "\"v1\"").run().assertStatus(412);
	}

	@Test void b06_ifMatchMultipleTagsAnyMatches() throws Exception {
		var c = MockRestClient.buildLax(R.class);
		c.get("/strong").header("If-Match", "\"v2\", \"v1\", \"v3\"").run().assertStatus(200);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// C: If-Unmodified-Since (only when If-Match absent; 412 if resource newer than supplied).
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_ifUnmodifiedSinceLaterPasses() throws Exception {
		var c = MockRestClient.buildLax(R.class);
		c.get("/lmOnly").header("If-Unmodified-Since", LM_LATER).run().assertStatus(200);
	}

	@Test void c02_ifUnmodifiedSinceEqualPasses() throws Exception {
		var c = MockRestClient.buildLax(R.class);
		c.get("/lmOnly").header("If-Unmodified-Since", LM).run().assertStatus(200);
	}

	@Test void c03_ifUnmodifiedSinceEarlierFails() throws Exception {
		var c = MockRestClient.buildLax(R.class);
		c.get("/lmOnly").header("If-Unmodified-Since", LM_EARLIER).run().assertStatus(412);
	}

	@Test void c04_ifUnmodifiedSinceIgnoredWhenIfMatchPresent() throws Exception {
		// Per RFC 7232 §6, If-Unmodified-Since is ignored when If-Match is set. Even if If-Unmodified-Since
		// would fail in isolation, the If-Match must win (and here, succeed).
		var c = MockRestClient.buildLax(R.class);
		c.get("/strong")
			.header("If-Match", ETAG_STRONG)
			.header("If-Unmodified-Since", LM_EARLIER)  // would fail standalone
			.run().assertStatus(200);
	}

	@Test void c05_ifUnmodifiedSinceWithoutResourceLastModifiedIgnored() throws Exception {
		// No Last-Modified set on the response, so the date comparison is skipped.
		var c = MockRestClient.buildLax(R.class);
		c.get("/etagOnly").header("If-Unmodified-Since", LM_EARLIER).run().assertStatus(200);
	}

	@Test void c06_ifUnmodifiedSinceMalformedIgnored() throws Exception {
		// Unparseable date is treated as not-set per Postel's law and the RFC ignores garbled dates.
		var c = MockRestClient.buildLax(R.class);
		c.get("/lmOnly").header("If-Unmodified-Since", "not-a-date").run().assertStatus(200);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// D: If-None-Match (weak comparison; 304 on match).
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_ifNoneMatchExactMatches304() throws Exception {
		var c = MockRestClient.buildLax(R.class);
		c.get("/strong").header("If-None-Match", ETAG_STRONG).run().assertStatus(304);
	}

	@Test void d02_ifNoneMatchMismatchPasses200() throws Exception {
		var c = MockRestClient.buildLax(R.class);
		c.get("/strong").header("If-None-Match", "\"v2\"").run().assertStatus(200);
	}

	@Test void d03_ifNoneMatchWildcardWithEtag304() throws Exception {
		var c = MockRestClient.buildLax(R.class);
		c.get("/strong").header("If-None-Match", "*").run().assertStatus(304);
	}

	@Test void d04_ifNoneMatchWildcardWithoutEtagPasses() throws Exception {
		var c = MockRestClient.buildLax(R.class);
		c.get("/lmOnly").header("If-None-Match", "*").run().assertStatus(200);
	}

	@Test void d05_ifNoneMatchWeakStrongCompareSucceeds() throws Exception {
		// RFC 7232 §3.2: If-None-Match uses weak comparison; weak vs strong both count as a match.
		var c = MockRestClient.buildLax(R.class);
		c.get("/strong").header("If-None-Match", ETAG_WEAK).run().assertStatus(304);
		c.get("/weak").header("If-None-Match", ETAG_STRONG).run().assertStatus(304);
		c.get("/weak").header("If-None-Match", ETAG_WEAK).run().assertStatus(304);
	}

	@Test void d06_ifNoneMatchMultipleAnyMatches304() throws Exception {
		var c = MockRestClient.buildLax(R.class);
		c.get("/strong").header("If-None-Match", "\"v2\", \"v1\", \"v3\"").run().assertStatus(304);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// E: If-Modified-Since (only when If-None-Match absent; only on GET/HEAD; 304 when not modified).
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_ifModifiedSinceEqualReturns304() throws Exception {
		var c = MockRestClient.buildLax(R.class);
		c.get("/lmOnly").header("If-Modified-Since", LM).run().assertStatus(304);
	}

	@Test void e02_ifModifiedSinceLaterReturns304() throws Exception {
		var c = MockRestClient.buildLax(R.class);
		c.get("/lmOnly").header("If-Modified-Since", LM_LATER).run().assertStatus(304);
	}

	@Test void e03_ifModifiedSinceEarlierPasses200() throws Exception {
		var c = MockRestClient.buildLax(R.class);
		c.get("/lmOnly").header("If-Modified-Since", LM_EARLIER).run().assertStatus(200);
	}

	@Test void e04_ifModifiedSinceIgnoredWhenIfNoneMatchPresent() throws Exception {
		// Per RFC 7232 §6, If-Modified-Since is ignored when If-None-Match is set.
		var c = MockRestClient.buildLax(R.class);
		c.get("/strong")
			.header("If-None-Match", "\"v2\"")          // mismatch → would pass standalone
			.header("If-Modified-Since", LM)             // would 304 standalone
			.run().assertStatus(200);                    // If-None-Match wins, no 304
	}

	@Test void e05_ifModifiedSinceIgnoredOnPost() throws Exception {
		// Only GET / HEAD honor If-Modified-Since per RFC 7232 §6.
		var c = MockRestClient.buildLax(R.class);
		c.post("/postStrong", "x").header("If-Modified-Since", LM).run().assertStatus(200);
	}

	@Test void e06_ifModifiedSinceWithoutResourceLastModifiedPasses() throws Exception {
		var c = MockRestClient.buildLax(R.class);
		c.get("/etagOnly").header("If-Modified-Since", LM).run().assertStatus(200);
	}

	@Test void e07_ifModifiedSinceMalformedIgnored() throws Exception {
		var c = MockRestClient.buildLax(R.class);
		c.get("/lmOnly").header("If-Modified-Since", "not-a-date").run().assertStatus(200);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// F: Combinations of all four conditional headers — 16-cell precedence matrix.
	//
	// Layout: (M=If-Match, U=If-Unmodified-Since, N=If-None-Match, S=If-Modified-Since)
	//   - present-with-pass and present-with-fail are exercised in B/C/D/E.
	//   - here we exercise the cross-products that depend on RFC 7232 §6 ordering.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_allFourPass_returns200() throws Exception {
		var c = MockRestClient.buildLax(R.class);
		c.get("/strong")
			.header("If-Match", ETAG_STRONG)
			.header("If-Unmodified-Since", LM_LATER)
			.header("If-None-Match", "\"v2\"")
			.header("If-Modified-Since", LM_EARLIER)
			.run().assertStatus(200);
	}

	@Test void f02_ifMatchFails_returns412_evenWithNoneMatchPass() throws Exception {
		// If-Match short-circuits at step 1; If-None-Match steps are not consulted.
		var c = MockRestClient.buildLax(R.class);
		c.get("/strong")
			.header("If-Match", "\"v2\"")
			.header("If-None-Match", ETAG_STRONG)  // would also 304 standalone
			.run().assertStatus(412);
	}

	@Test void f03_ifMatchPasses_thenIfNoneMatchTriggers304() throws Exception {
		// If-Match passes (step 1), If-Unmodified-Since skipped (step 2), If-None-Match triggers (step 3).
		var c = MockRestClient.buildLax(R.class);
		c.get("/strong")
			.header("If-Match", ETAG_STRONG)
			.header("If-None-Match", ETAG_STRONG)
			.run().assertStatus(304);
	}

	@Test void f04_unmodifiedSincePassed_thenModifiedSinceTriggers304() throws Exception {
		// If-Match absent → If-Unmodified-Since checked (passes). If-None-Match absent → If-Modified-Since
		// triggers 304.
		var c = MockRestClient.buildLax(R.class);
		c.get("/lmOnly")
			.header("If-Unmodified-Since", LM_LATER)
			.header("If-Modified-Since", LM)
			.run().assertStatus(304);
	}

	@Test void f05_unmodifiedSinceFails_412_beforeModifiedSinceEvaluated() throws Exception {
		// If-Unmodified-Since fails at step 2; If-Modified-Since never consulted.
		var c = MockRestClient.buildLax(R.class);
		c.get("/lmOnly")
			.header("If-Unmodified-Since", LM_EARLIER)
			.header("If-Modified-Since", LM_LATER)  // would 304 standalone
			.run().assertStatus(412);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// G: Null-argument guard.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class G {
		@RestGet("/nullResponse")
		public String nullResponse(RestRequest req) {
			req.checkPreconditions(null);
			return "OK";
		}
	}

	@Test void g01_nullResponseRejected() throws Exception {
		var c = MockRestClient.buildLax(G.class);
		c.get("/nullResponse").run().assertStatus(500);
	}
}
