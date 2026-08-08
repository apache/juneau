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
package org.apache.juneau.rest.server.mcp.v20260728;

import static org.apache.juneau.BasicTestUtils.assertThrowsWithMessage;
import static org.junit.jupiter.api.Assertions.*;

import java.security.*;
import java.util.*;

import org.apache.juneau.TestBase;
import org.apache.juneau.rest.server.auth.ClaimsPrincipal;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link McpAudienceValidator} &mdash; RFC 8707 audience matching against the resource identifier,
 * including the H2 {@code requireAudienceClaim} fail-closed default.
 */
class McpAudienceValidator_Test extends TestBase {

	private static final String RES = "https://mcp.example.com/mcp";

	private static ClaimsPrincipal cp(String claim, Object value) {
		return new ClaimsPrincipal("alice", Map.of(claim, value));
	}

	@Test void a01_audStringMatches() {
		assertTrue(McpAudienceValidator.matches(cp("aud", RES), RES, true));
	}

	@Test void a02_audListContainsMatch() {
		assertTrue(McpAudienceValidator.matches(cp("aud", List.of("https://other", RES)), RES, true));
	}

	@Test void a03_resourceClaimMatches() {
		assertTrue(McpAudienceValidator.matches(cp("resource", RES), RES, true));
	}

	@Test void b01_wrongAudienceRejected() {
		assertFalse(McpAudienceValidator.matches(cp("aud", "https://evil.example.com"), RES, true));
	}

	@Test void b02_audienceAbsentRejected() {
		assertFalse(McpAudienceValidator.matches(cp("sub", "alice"), RES, true));
	}

	@Test void b03_nullPrincipalRejected() {
		assertFalse(McpAudienceValidator.matches(null, RES, true));
	}

	// H2: a bare (claims-less) Principal is rejected by default (fail-closed) so the confused-deputy defense is not a
	// silent no-op.
	@Test void c01_nonClaimsPrincipalRejectedByDefault() {
		Principal bare = () -> "alice";
		assertFalse(McpAudienceValidator.matches(bare, RES, true));
	}

	// H2: the lenient behavior remains available only as an explicit opt-out (validator owns audience enforcement).
	@Test void c02_nonClaimsPrincipalPassesWhenClaimNotRequired() {
		Principal bare = () -> "alice";
		assertTrue(McpAudienceValidator.matches(bare, RES, false));
	}

	@Test void d01_blankExpectedAudienceRejected() {
		var principal = cp("aud", RES);
		assertThrowsWithMessage(IllegalArgumentException.class, "expectedAudience", () -> McpAudienceValidator.matches(principal, "  ", true));
	}
}
