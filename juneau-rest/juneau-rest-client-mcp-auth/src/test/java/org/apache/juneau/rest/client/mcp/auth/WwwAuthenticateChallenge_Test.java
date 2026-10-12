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
package org.apache.juneau.rest.client.mcp.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link WwwAuthenticateChallenge} (RFC 6750 / RFC 9728 challenge parsing).
 *
 * @since 10.0.0
 */
class WwwAuthenticateChallenge_Test extends TestBase {

	@Test void a01_parsesResourceMetadataPointer() {
		var c = WwwAuthenticateChallenge.parse(
			"Bearer realm=\"mcp\", resource_metadata=\"https://mcp.example.com/.well-known/oauth-protected-resource\"").orElseThrow();
		assertTrue(c.isBearer());
		assertEquals("Bearer", c.scheme());
		assertEquals(URI.create("https://mcp.example.com/.well-known/oauth-protected-resource"), c.resourceMetadata().orElseThrow());
	}

	@Test void a02_parsesScopeAndError() {
		var c = WwwAuthenticateChallenge.parse("Bearer error=\"insufficient_scope\", scope=\"read write admin\"").orElseThrow();
		assertEquals("insufficient_scope", c.error().orElseThrow());
		assertEquals(List.of("read", "write", "admin"), List.copyOf(c.scopes()));
	}

	@Test void a03_paramKeysAreCaseInsensitive() {
		var c = WwwAuthenticateChallenge.parse("Bearer Resource_Metadata=\"https://x/prm\"").orElseThrow();
		assertEquals(URI.create("https://x/prm"), c.resourceMetadata().orElseThrow());
		assertEquals("https://x/prm", c.parameter("RESOURCE_METADATA").orElseThrow());
	}

	@Test void a04_bareTokenValuesAccepted() {
		var c = WwwAuthenticateChallenge.parse("Bearer error=invalid_token, scope=read").orElseThrow();
		assertEquals("invalid_token", c.error().orElseThrow());
		assertEquals(Set.of("read"), c.scopes());
	}

	@Test void b01_nullReturnsEmpty() {
		assertTrue(WwwAuthenticateChallenge.parse(null).isEmpty());
	}

	@Test void b02_blankReturnsEmpty() {
		assertTrue(WwwAuthenticateChallenge.parse("   ").isEmpty());
	}

	@Test void b03_schemeOnlyHasNoParams() {
		var c = WwwAuthenticateChallenge.parse("Bearer").orElseThrow();
		assertTrue(c.isBearer());
		assertTrue(c.parameters().isEmpty());
		assertTrue(c.resourceMetadata().isEmpty());
		assertTrue(c.scopes().isEmpty());
		assertTrue(c.error().isEmpty());
	}

	@Test void b04_nonBearerScheme() {
		var c = WwwAuthenticateChallenge.parse("Basic realm=\"x\"").orElseThrow();
		assertFalse(c.isBearer());
		assertEquals("Basic", c.scheme());
	}

	@Test void b05_missingResourceMetadataIsEmpty() {
		var c = WwwAuthenticateChallenge.parse("Bearer realm=\"mcp\"").orElseThrow();
		assertTrue(c.resourceMetadata().isEmpty());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: malformed / edge-case challenge parameters
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_bareTokenWithoutValueIsSkipped() {
		// A token68-style bare token (no '=value') is skipped; parsing resumes at the next param.
		var c = WwwAuthenticateChallenge.parse("Bearer abc123, error=\"invalid_token\"").orElseThrow();
		assertEquals(Map.of("error", "invalid_token"), c.parameters());
	}

	@Test void c02_trailingBareTokenIsSkipped() {
		var c = WwwAuthenticateChallenge.parse("Bearer abc123").orElseThrow();
		assertTrue(c.parameters().isEmpty());
	}

	@Test void c03_emptyKeyIsSkipped() {
		var c = WwwAuthenticateChallenge.parse("Bearer =orphan, error=x").orElseThrow();
		assertEquals(Map.of("error", "x"), c.parameters());
	}

	@Test void c04_leadingAndRepeatedSeparatorsTolerated() {
		var c = WwwAuthenticateChallenge.parse("Bearer ,, ,error=x").orElseThrow();
		assertEquals("x", c.error().orElseThrow());
	}

	@Test void c05_emptyBareValueAtEndOfInput() {
		var c = WwwAuthenticateChallenge.parse("Bearer error=").orElseThrow();
		assertEquals("", c.parameter("error").orElseThrow());
	}

	@Test void c06_firstDuplicateParamWins() {
		var c = WwwAuthenticateChallenge.parse("Bearer error=first, error=second").orElseThrow();
		assertEquals("first", c.error().orElseThrow());
	}

	@Test void c07_quotedValueHonorsEscapes() {
		var c = WwwAuthenticateChallenge.parse("Bearer error_description=\"say \\\"hi\\\", ok\", error=x").orElseThrow();
		assertEquals("say \"hi\", ok", c.parameter("error_description").orElseThrow());
		assertEquals("x", c.error().orElseThrow());
	}

	@Test void c08_trailingBackslashInQuotedValueKeptLiterally() {
		// A backslash as the very last char has nothing to escape, so it is appended as-is (and the quote is unterminated).
		var c = WwwAuthenticateChallenge.parse("Bearer a=\"x\\").orElseThrow();
		assertEquals("x\\", c.parameter("a").orElseThrow());
	}

	@Test void c09_unterminatedQuotedValueTakesRemainder() {
		var c = WwwAuthenticateChallenge.parse("Bearer a=\"abc").orElseThrow();
		assertEquals("abc", c.parameter("a").orElseThrow());
	}

	@Test void c10_unparseableResourceMetadataIsEmpty() {
		var c = WwwAuthenticateChallenge.parse("Bearer resource_metadata=\"not a uri\"").orElseThrow();
		assertTrue(c.resourceMetadata().isEmpty());
	}

	@Test void c11_blankResourceMetadataIsEmpty() {
		var c = WwwAuthenticateChallenge.parse("Bearer resource_metadata=\"\"").orElseThrow();
		assertTrue(c.resourceMetadata().isEmpty());
	}

	@Test void c12_blankScopeIsEmptySet() {
		var c = WwwAuthenticateChallenge.parse("Bearer scope=\"  \"").orElseThrow();
		assertTrue(c.scopes().isEmpty());
	}

	@Test void c13_scopesSplitOnAnyWhitespaceRun() {
		var c = WwwAuthenticateChallenge.parse("Bearer scope=\"  read   write\t admin \"").orElseThrow();
		assertEquals(List.of("read", "write", "admin"), List.copyOf(c.scopes()));
	}
}
