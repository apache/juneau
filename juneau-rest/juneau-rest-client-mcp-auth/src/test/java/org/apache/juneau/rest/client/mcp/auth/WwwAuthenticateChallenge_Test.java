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
		assertEquals(java.util.List.of("read", "write", "admin"), java.util.List.copyOf(c.scopes()));
	}

	@Test void a03_paramKeysAreCaseInsensitive() {
		var c = WwwAuthenticateChallenge.parse("Bearer Resource_Metadata=\"https://x/prm\"").orElseThrow();
		assertEquals(URI.create("https://x/prm"), c.resourceMetadata().orElseThrow());
		assertEquals("https://x/prm", c.parameter("RESOURCE_METADATA").orElseThrow());
	}

	@Test void a04_bareTokenValuesAccepted() {
		var c = WwwAuthenticateChallenge.parse("Bearer error=invalid_token, scope=read").orElseThrow();
		assertEquals("invalid_token", c.error().orElseThrow());
		assertEquals(java.util.Set.of("read"), c.scopes());
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
}
