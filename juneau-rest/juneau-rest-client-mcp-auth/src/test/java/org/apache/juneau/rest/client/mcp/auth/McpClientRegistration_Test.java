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
import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link McpClientRegistration} (redaction + defensive copies) and {@link LoopbackRedirectUris}.
 *
 * @since 10.0.0
 */
class McpClientRegistration_Test extends TestBase {

	private static final URI ISSUER = URI.create("https://as.example.com");

	private static McpClientRegistration confidential() {
		return new McpClientRegistration(
			"client-123",
			Optional.of("s3cr3t"),
			Optional.of(Instant.parse("2027-01-01T00:00:00Z")),
			Optional.of("reg-access-tok"),
			Optional.of(URI.create("https://as.example.com/register/client-123")),
			ISSUER,
			List.of(URI.create("http://127.0.0.1:3000/callback"), URI.create("http://localhost:3000/callback")),
			McpApplicationType.NATIVE,
			Map.of("extra", "x"));
	}

	@Test void a01_construction() {
		var r = confidential();
		assertEquals("client-123", r.clientId());
		assertEquals(ISSUER, r.issuer());
		assertEquals("s3cr3t", r.clientSecret().orElseThrow());
		assertEquals(McpApplicationType.NATIVE, r.applicationType());
		assertTrue(r.isConfidential());
		assertEquals(2, r.redirectUris().size());
	}

	@Test void a02_publicClientHasNoSecret() {
		var r = new McpClientRegistration("pub", Optional.empty(), Optional.empty(), Optional.empty(),
			Optional.empty(), ISSUER, List.of(URI.create("http://127.0.0.1/callback")), McpApplicationType.NATIVE, Map.of());
		assertFalse(r.isConfidential());
		assertTrue(r.clientSecret().isEmpty());
	}

	@Test void a03_toStringRedactsSecrets() {
		var s = confidential().toString();
		assertFalse(s.contains("s3cr3t"), "toString must not leak clientSecret");
		assertFalse(s.contains("reg-access-tok"), "toString must not leak registrationAccessToken");
		assertTrue(s.contains("<redacted>"));
		assertTrue(s.contains("client-123"));
		assertTrue(s.contains("as.example.com"));
	}

	@Test void a04_toStringShowsNoneForAbsentSecret() {
		var r = new McpClientRegistration("pub", Optional.empty(), Optional.empty(), Optional.empty(),
			Optional.empty(), ISSUER, List.of(), McpApplicationType.WEB, Map.of());
		var s = r.toString();
		assertTrue(s.contains("clientSecret=<none>"));
		assertTrue(s.contains("registrationAccessToken=<none>"));
	}

	@Test void b01_redirectUrisAreDefensivelyCopiedAndUnmodifiable() {
		var src = new ArrayList<>(List.of(URI.create("http://127.0.0.1/callback")));
		var r = new McpClientRegistration("c", Optional.empty(), Optional.empty(), Optional.empty(),
			Optional.empty(), ISSUER, src, McpApplicationType.NATIVE, Map.of());
		src.add(URI.create("http://evil/callback"));
		assertEquals(1, r.redirectUris().size(), "mutating the source list must not affect the record");
		var uris = r.redirectUris();
		var extra = URI.create("http://x/y");
		assertThrows(UnsupportedOperationException.class, () -> uris.add(extra));
	}

	@Test void b02_extrasAreDefensivelyCopiedAndUnmodifiable() {
		var src = new LinkedHashMap<String,Object>();
		src.put("a", 1);
		var r = new McpClientRegistration("c", Optional.empty(), Optional.empty(), Optional.empty(),
			Optional.empty(), ISSUER, List.of(), McpApplicationType.NATIVE, src);
		src.put("b", 2);
		assertEquals(1, r.extras().size());
		var extras = r.extras();
		assertThrows(UnsupportedOperationException.class, () -> extras.put("c", 3));
	}

	@Test void b03_nullRequiredFieldsRejected() {
		Optional<String> noSecret = Optional.empty();
		Optional<Instant> noExpiry = Optional.empty();
		Optional<String> noRegToken = Optional.empty();
		Optional<URI> noRegUri = Optional.empty();
		List<URI> noRedirects = List.of();
		Map<String,Object> noExtras = Map.of();
		assertThrows(NullPointerException.class, () -> new McpClientRegistration(null, noSecret, noExpiry,
			noRegToken, noRegUri, ISSUER, noRedirects, McpApplicationType.NATIVE, noExtras));
		assertThrows(NullPointerException.class, () -> new McpClientRegistration("c", noSecret, noExpiry,
			noRegToken, noRegUri, null, noRedirects, McpApplicationType.NATIVE, noExtras));
	}

	// LoopbackRedirectUris

	@Test void c01_portAgnostic() {
		var uris = LoopbackRedirectUris.portAgnostic("/callback");
		assertEquals(List.of(URI.create("http://127.0.0.1/callback"), URI.create("http://localhost/callback")), uris);
	}

	@Test void c02_forPort() {
		var uris = LoopbackRedirectUris.forPort(3000, "/callback");
		assertEquals(List.of(URI.create("http://127.0.0.1:3000/callback"), URI.create("http://localhost:3000/callback")), uris);
	}

	@Test void c03_pathMustStartWithSlash() {
		assertThrows(IllegalArgumentException.class, () -> LoopbackRedirectUris.portAgnostic("callback"));
		assertThrows(IllegalArgumentException.class, () -> LoopbackRedirectUris.forPort(3000, "callback"));
	}

	@Test void c04_blankPathRejected() {
		assertThrows(IllegalArgumentException.class, () -> LoopbackRedirectUris.portAgnostic("  "));
	}

	@Test void c05_portRangeValidated() {
		assertThrows(IllegalArgumentException.class, () -> LoopbackRedirectUris.forPort(0, "/callback"));
		assertThrows(IllegalArgumentException.class, () -> LoopbackRedirectUris.forPort(70000, "/callback"));
	}
}
