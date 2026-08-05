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

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.util.*;

import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link McpBearerChallenge} &mdash; RFC 6750 / RFC 9728 {@code WWW-Authenticate: Bearer} value
 * construction, ordering, sanitization, and the F3 extensibility seam.
 */
class McpBearerChallenge_Test {

	@Test void a01_emptyChallengeIsBareScheme() {
		assertEquals("Bearer", McpBearerChallenge.create().build());
	}

	@Test void a02_realmAndResourceMetadataInOrder() {
		var s = McpBearerChallenge.create()
			.realm("mcp")
			.resourceMetadata(URI.create("https://host/.well-known/oauth-protected-resource/mcp"))
			.build();
		assertEquals("Bearer realm=\"mcp\", resource_metadata=\"https://host/.well-known/oauth-protected-resource/mcp\"", s);
	}

	@Test void a03_invalidTokenChallenge() {
		var s = McpBearerChallenge.create()
			.error("invalid_token")
			.resourceMetadata(URI.create("https://host/prm"))
			.build();
		assertEquals("Bearer error=\"invalid_token\", resource_metadata=\"https://host/prm\"", s);
	}

	@Test void a04_insufficientScopeChallengeIncludesScope() {
		var s = McpBearerChallenge.create()
			.error("insufficient_scope")
			.scope(new LinkedHashSet<>(List.of("mcp.read", "mcp.write")))
			.resourceMetadata(URI.create("https://host/prm"))
			.build();
		assertEquals("Bearer error=\"insufficient_scope\", scope=\"mcp.read mcp.write\", resource_metadata=\"https://host/prm\"", s);
	}

	@Test void b01_nullValuesSkipped() {
		var s = McpBearerChallenge.create().realm(null).error(null).resourceMetadata(null).build();
		assertEquals("Bearer", s);
	}

	@Test void b02_emptyScopeSkipped() {
		var s = McpBearerChallenge.create().error("insufficient_scope").scope(Set.of()).build();
		assertEquals("Bearer error=\"insufficient_scope\"", s);
	}

	// M4: quotes and backslashes are RFC 7235 backslash-escaped (not mangled to spaces) so the value round-trips.
	@Test void b03_embeddedQuotesAndBackslashEscaped() {
		var s = McpBearerChallenge.create().errorDescription("bad \"token\" a\\b").build();
		assertEquals("Bearer error_description=\"bad \\\"token\\\" a\\\\b\"", s);
	}

	// M4: control characters (incl. CR/LF header-injection vectors and DEL) are stripped entirely.
	@Test void b04_controlCharsStripped() {
		var s = McpBearerChallenge.create().errorDescription("a\r\nb\tc\u007fd").build();
		assertEquals("Bearer error_description=\"abcd\"", s);
	}

	@Test void c01_genericParamSeamPreservesOrder() {
		var s = McpBearerChallenge.create().realm("mcp").param("max_age", "60").build();
		assertEquals("Bearer realm=\"mcp\", max_age=\"60\"", s);
	}

	@Test void c02_blankParamNameRejected() {
		assertThrows(IllegalArgumentException.class, () -> McpBearerChallenge.create().param("  ", "x"));
	}
}
