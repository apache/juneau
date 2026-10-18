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

import static org.apache.juneau.BasicTestUtils.assertThrowsWithMessage;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.nio.charset.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.auth.oauth.oidc.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link McpClientRegistrationManager} (SEP-2352 selection + issuer-keying + migration).
 *
 * @since 10.0.0
 */
class McpClientRegistrationManager_Test extends TestBase {

	private static final URI A = URI.create("https://as-a.example.com");
	private static final URI B = URI.create("https://as-b.example.com");

	private static OidcMetadata as(URI issuer, boolean withRegEndpoint) {
		Map<String,Object> extras = withRegEndpoint
			? Map.of("registration_endpoint", issuer + "/register")
			: Map.of();
		return new OidcMetadata(issuer, URI.create(issuer + "/token"), URI.create(issuer + "/auth"),
			null, null, null, null, Set.of(), extras);
	}

	private static McpClientRegistration reg(URI issuer, String clientId) {
		return new McpClientRegistration(clientId, Optional.of("secret-" + clientId), Optional.empty(), Optional.empty(),
			Optional.empty(), issuer, List.of(URI.create("http://127.0.0.1/callback")), McpApplicationType.NATIVE, Map.of());
	}

	/** A counting DCR stub that stamps the issuer of the AS it is asked to register with. */
	private static AtomicInteger counter;
	private static Function<OidcMetadata,McpClientRegistration> stubDcr() {
		counter = new AtomicInteger();
		return md -> {
			var n = counter.incrementAndGet();
			return reg(md.issuer(), "dcr-" + n);
		};
	}

	@Test void a01_onDemandRegistersEveryCall() {
		var m = McpClientRegistrationManager.create().registrarFunction(stubDcr()).build();
		m.resolve(as(A, true));
		m.resolve(as(A, true));
		assertEquals(2, counter.get(), "on-demand mode (no store) must register on every resolve");
	}

	@Test void a02_storeHitReused() {
		var store = new InMemoryMcpClientRegistrationStore();
		var m = McpClientRegistrationManager.create().store(store).registrarFunction(stubDcr()).build();
		var first = m.resolve(as(A, true));
		var second = m.resolve(as(A, true));
		assertEquals(1, counter.get(), "store-hit must be reused without a second DCR round-trip");
		assertEquals(first.clientId(), second.clientId());
	}

	@Test void a03_migrationReregistersWithNewIssuer() {
		var store = new InMemoryMcpClientRegistrationStore();
		var m = McpClientRegistrationManager.create().store(store).registrarFunction(stubDcr()).build();
		var forA = m.resolve(as(A, true));
		var forB = m.resolve(as(B, true));
		assertEquals(2, counter.get(), "migration to a new issuer must trigger re-registration");
		assertEquals(A, forA.issuer());
		assertEquals(B, forB.issuer());
		// A's entry is untouched (never reused for B).
		assertEquals(forA.clientId(), store.find(A).orElseThrow().clientId());
		assertEquals(forB.clientId(), store.find(B).orElseThrow().clientId());
	}

	@Test void b01_preRegisteredMatchingIssuerUsed() {
		var pre = reg(A, "pre-a");
		var m = McpClientRegistrationManager.create().preRegistered(pre).registrarFunction(stubDcr()).build();
		assertEquals("pre-a", m.resolve(as(A, true)).clientId());
		assertEquals(0, counter.get(), "matching pre-registered creds must not trigger DCR");
	}

	@Test void b02_preRegisteredMismatchSurfacesError() {
		var pre = reg(A, "pre-a");
		var m = McpClientRegistrationManager.create().preRegistered(pre).registrarFunction(stubDcr()).build();
		assertThrowsWithMessage(McpAuthException.class, "different authorization server", () -> m.resolve(as(B, true)));
	}

	@Test void b03_noRegistrationEndpointAndNoPreRegisteredFails() {
		var m = McpClientRegistrationManager.create()
			.redirectUris(List.of(URI.create("http://127.0.0.1/callback")))
			.build();
		assertThrowsWithMessage(McpAuthException.class, "no registration_endpoint", () -> m.resolve(as(A, false)));
	}

	@Test void b04_missingRedirectUrisForDcrFails() {
		// Real DCR path (no override, no redirect URIs) must fail fast rather than attempt a malformed registration.
		var m = McpClientRegistrationManager.create().build();
		assertThrowsWithMessage(McpAuthException.class, "redirectUris", () -> m.resolve(as(A, true)));
	}

	@Test void c01_registrationEndpointExtractedFromExtras() {
		assertEquals(URI.create("https://as-a.example.com/register"),
			McpClientRegistrationManager.registrationEndpoint(as(A, true)).orElseThrow());
		assertTrue(McpClientRegistrationManager.registrationEndpoint(as(A, false)).isEmpty());
	}

	// LOW: a hostile/misconfigured AS advertising a plaintext (non-loopback) registration_endpoint yields the
	// documented McpAuthException rather than a raw IllegalStateException from the registrar's https gate.
	@Test void c02_plaintextRegistrationEndpointSurfacesMcpAuthException() {
		var as = new OidcMetadata(A, URI.create(A + "/token"), URI.create(A + "/auth"), null, null, null, null, Set.of(),
			Map.of("registration_endpoint", "http://as-a.example.com/register"));
		assertThrowsWithMessage(McpAuthException.class, "non-https registration_endpoint",
			() -> McpClientRegistrationManager.registrationEndpoint(as));
	}

	// LOW: a malformed registration_endpoint URI yields McpAuthException rather than a raw IllegalArgumentException.
	@Test void c03_malformedRegistrationEndpointSurfacesMcpAuthException() {
		var as = new OidcMetadata(A, URI.create(A + "/token"), URI.create(A + "/auth"), null, null, null, null, Set.of(),
			Map.of("registration_endpoint", "http://  bad uri"));
		assertThrowsWithMessage(McpAuthException.class, "malformed registration_endpoint",
			() -> McpClientRegistrationManager.registrationEndpoint(as));
	}

	// LOW: a buggy durable store returning an entry keyed under the wrong issuer must NOT cause cross-AS credential
	// reuse (SEP-2352); the manager defends against it.
	@Test void d01_buggyStoreCrossIssuerEntryRejected() {
		var buggy = new McpClientRegistrationStore() {
			@Override public Optional<McpClientRegistration> find(URI issuer) { return Optional.of(reg(B, "wrong-issuer")); }
			@Override public void put(URI issuer, McpClientRegistration registration) { /* no-op */ }
			@Override public void remove(URI issuer) { /* no-op */ }
		};
		var m = McpClientRegistrationManager.create().store(buggy).registrarFunction(stubDcr()).build();
		assertThrowsWithMessage(McpAuthException.class, "refusing to reuse credentials", () -> m.resolve(as(A, true)));
	}

	@Test void e01_preRegisteredCredentialsStoredWhenStorePresent() {
		var store = new InMemoryMcpClientRegistrationStore();
		var pre = reg(A, "pre-a");
		var m = McpClientRegistrationManager.create().preRegistered(pre).store(store).build();
		assertEquals("pre-a", m.resolve(as(A, false)).clientId());
		assertEquals("pre-a", store.find(A).orElseThrow().clientId());
	}

	@Test void e02_builderArgumentValidation() {
		var b = McpClientRegistrationManager.create();
		assertThrows(IllegalArgumentException.class, () -> b.redirectUris(List.of()));
		assertThrows(IllegalArgumentException.class, () -> b.scope(" "));
		assertThrows(IllegalArgumentException.class, () -> b.clientName(" "));
		assertThrows(IllegalArgumentException.class, () -> b.initialAccessToken(" "));
		assertThrows(IllegalArgumentException.class, () -> b.httpTimeout(Duration.ZERO));
		var negative = Duration.ofSeconds(-1);
		assertThrows(IllegalArgumentException.class, () -> b.httpTimeout(negative));
	}

	/** Runs the real DCR path (no override) against a loopback stub, capturing the registration request. */
	@Test void e03_realDcrAppliesEveryBuilderOption() throws Exception {
		var server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		var body = new AtomicReference<String>();
		var auth = new AtomicReference<String>();
		server.createContext("/register", ex -> {
			auth.set(ex.getRequestHeaders().getFirst("Authorization"));
			try (var is = ex.getRequestBody()) { body.set(new String(is.readAllBytes(), StandardCharsets.UTF_8)); }
			var b = ("{\"client_id\":\"live\",\"redirect_uris\":[\"http://127.0.0.1:3000/callback\"],"
				+ "\"application_type\":\"web\"}").getBytes(StandardCharsets.UTF_8);
			ex.getResponseHeaders().add("Content-Type", "application/json");
			ex.sendResponseHeaders(201, b.length);
			try (var os = ex.getResponseBody()) { os.write(b); }
		});
		server.start();
		try {
			var endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/register";
			var md = new OidcMetadata(A, URI.create(A + "/token"), URI.create(A + "/auth"),
				null, null, null, null, Set.of(), Map.of("registration_endpoint", endpoint));
			var configured = new AtomicInteger();
			var store = new InMemoryMcpClientRegistrationStore();
			var m = McpClientRegistrationManager.create()
				.store(store)
				.applicationType(McpApplicationType.WEB)
				.redirectUris(List.of(URI.create("http://127.0.0.1:3000/callback")))
				.scope("mcp:read", "mcp:write")
				.confidential(true)
				.clientName("Demo Client")
				.initialAccessToken("init-tok")
				.httpTimeout(Duration.ofSeconds(5))
				.httpRequestConfigurator(r -> configured.incrementAndGet())
				.build();
			var result = m.resolve(md);
			assertEquals("live", result.clientId());
			assertEquals(A, result.issuer());
			assertEquals("live", store.find(A).orElseThrow().clientId(), "freshly registered credentials are stored");
			assertEquals(1, configured.get());
			assertEquals("Bearer init-tok", auth.get());
			var sent = body.get();
			assertTrue(sent.contains("\"application_type\":\"web\""), sent);
			assertTrue(sent.contains("Demo Client"), sent);
			assertTrue(sent.contains("mcp:read mcp:write"), sent);
			assertTrue(sent.contains("client_secret_basic"), sent);
		} finally {
			server.stop(0);
		}
	}

	@Test void e04_missingRegistrationEndpointFailsBeforeRedirectUrisSupplierConsulted() {
		var supplied = new AtomicInteger();
		var m = McpClientRegistrationManager.create()
			.redirectUrisSupplier(() -> { supplied.incrementAndGet(); return List.of(URI.create("http://127.0.0.1:3000/callback")); })
			.build();
		// No registration_endpoint, so DCR fails before the supplier is consulted.
		assertThrowsWithMessage(McpAuthException.class, "no registration_endpoint", () -> m.resolve(as(A, false)));
		assertEquals(0, supplied.get());
	}

	@Test void e05_realDcrWithDefaultsOmitsOptionalSettings() throws Exception {
		var server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		var auth = new AtomicReference<String>();
		server.createContext("/register", ex -> {
			auth.set(ex.getRequestHeaders().getFirst("Authorization"));
			try (var in = ex.getRequestBody()) { in.readAllBytes(); }
			var b = "{\"client_id\":\"plain\",\"redirect_uris\":[\"http://127.0.0.1:3000/callback\"]}"
				.getBytes(StandardCharsets.UTF_8);
			ex.getResponseHeaders().add("Content-Type", "application/json");
			ex.sendResponseHeaders(201, b.length);
			try (var os = ex.getResponseBody()) { os.write(b); }
		});
		server.start();
		try {
			var endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/register";
			var md = new OidcMetadata(A, URI.create(A + "/token"), URI.create(A + "/auth"),
				null, null, null, null, Set.of(), Map.of("registration_endpoint", endpoint));
			var m = McpClientRegistrationManager.create()
				.redirectUris(List.of(URI.create("http://127.0.0.1:3000/callback")))
				.build();
			assertEquals("plain", m.resolve(md).clientId());
			assertNull(auth.get(), "no initial access token configured");
		} finally {
			server.stop(0);
		}
	}
}
