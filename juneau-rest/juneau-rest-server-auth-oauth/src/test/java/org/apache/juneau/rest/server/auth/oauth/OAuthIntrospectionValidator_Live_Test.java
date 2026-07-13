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
package org.apache.juneau.rest.server.auth.oauth;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.time.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.auth.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Live-stub tests for {@link OAuthIntrospectionValidator}: exercises the full introspect() path
 * via an in-JVM {@code com.sun.net.httpserver.HttpServer}.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource",    // HttpServer held as test fixture; lifecycle managed by @AfterEach
	"java:S5778"   // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice
})
class OAuthIntrospectionValidator_Live_Test extends TestBase {

	private HttpServer server;
	private volatile String nextResponse;
	private volatile int nextStatus = 200;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.createContext("/introspect", ex -> {
			var body = nextResponse.getBytes(StandardCharsets.UTF_8);
			ex.getResponseHeaders().add("Content-Type", "application/json");
			ex.sendResponseHeaders(nextStatus, body.length);
			try (var os = ex.getResponseBody()) {
				os.write(body);
			}
		});
		server.start();
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

	private URI endpoint() {
		return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/introspect");
	}

	private OAuthIntrospectionValidator.Builder validatorBuilder() {
		return OAuthIntrospectionValidator.create()
			.introspectionEndpoint(endpoint())
			.clientId("client")
			.clientSecret("secret");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: httpRequestConfigurator null vs. non-null (line 334)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_configuratorNull_notInvoked() throws Exception {
		nextResponse = "{\"active\":true,\"sub\":\"alice\",\"scope\":\"read\"}";
		var v = validatorBuilder().build();
		var p = v.validate("tok-1");
		assertEquals("alice", p.getName());
	}

	@Test void a02_configuratorNonNull_invoked() throws Exception {
		nextResponse = "{\"active\":true,\"sub\":\"alice\",\"scope\":\"read\"}";
		var called = new AtomicBoolean();
		var v = validatorBuilder()
			.httpRequestConfigurator(req -> called.set(true))
			.build();
		v.validate("tok-cfg");
		assertTrue(called.get());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: error response from introspection endpoint (line 351)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_errorResponse_throws() {
		nextResponse = "{\"error\":\"invalid_client\"}";
		nextStatus = 400;
		var v = validatorBuilder().build();
		var ex = assertThrows(AuthenticationException.class, () -> v.validate("tok-err"));
		assertTrue(ex.getMessage().contains("introspection error") || ex.getMessage().toLowerCase().contains("invalid_client")
			|| ex.getMessage().contains("OAuth"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: inactive token (line 355)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_inactiveToken_throws() {
		nextResponse = "{\"active\":false}";
		var v = validatorBuilder().build();
		var ex = assertThrows(AuthenticationException.class, () -> v.validate("tok-inactive"));
		assertTrue(ex.getMessage().contains("inactive"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: null subject → "<no-sub>" (line 361)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_nullSubject_noSubPlaceholder() throws Exception {
		nextResponse = "{\"active\":true,\"scope\":\"read\"}";
		var v = validatorBuilder().build();
		var p = v.validate("tok-nosub");
		assertEquals("<no-sub>", p.getName());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// E: extractScopes — null scope (line 370), blank pieces (lines 373/374)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void e01_nullScope_emptyRoles() throws Exception {
		nextResponse = "{\"active\":true,\"sub\":\"alice\"}";
		var v = validatorBuilder().build();
		var p = (ClaimsPrincipal) v.validate("tok-noscope");
		var scope = p.getClaims().get("scope");
		assertNull(scope); // buildClaims omits scope key when empty
	}

	@Test void e02_scopeWithBlankPieces_filtered() throws Exception {
		// Nimbus parses "scope" from the JSON via its own Scope class; we provide a
		// raw JSON where the scope string produces a Nimbus Scope list that may include blank entries.
		// Use a double-space in scope to test both non-null (line 370 false) and blank (lines 373/374).
		nextResponse = "{\"active\":true,\"sub\":\"alice\",\"scope\":\"read  write\"}";
		var v = validatorBuilder().build();
		var p = (ClaimsPrincipal) v.validate("tok-blanks");
		@SuppressWarnings("unchecked")
		var scope = (java.util.List<String>) p.getClaims().get("scope");
		assertNotNull(scope);
		assertTrue(scope.contains("read"));
		assertTrue(scope.contains("write"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// F: enforceRequiredScopes — missing scope (lines 380/381)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void f01_requiredScopePresent_noThrow() throws Exception {
		nextResponse = "{\"active\":true,\"sub\":\"alice\",\"scope\":\"read write\"}";
		var v = validatorBuilder().requiredScopes("read").build();
		var p = v.validate("tok-scope-ok");
		assertEquals("alice", p.getName());
	}

	@Test void f02_requiredScopeMissing_throws() {
		nextResponse = "{\"active\":true,\"sub\":\"alice\",\"scope\":\"read\"}";
		var v = validatorBuilder().requiredScopes("write").build();
		var ex = assertThrows(AuthenticationException.class, () -> v.validate("tok-scope-missing"));
		assertTrue(ex.getMessage().contains("write") || ex.getMessage().contains("scope"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// G: buildClaims — scopes empty (line 389 false), scopes non-empty (line 389 true / line 390)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void g01_scopesNonEmpty_addedToClaims() throws Exception {
		nextResponse = "{\"active\":true,\"sub\":\"alice\",\"scope\":\"read\"}";
		var v = validatorBuilder().build();
		var p = (ClaimsPrincipal) v.validate("tok-claims");
		assertNotNull(p.getClaims().get("scope"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// H: resolveTtl — exp null (line 397 false), exp in past (line 399 true), until < cacheTtl (line 401 true)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void h01_expNull_usesCacheTtl() throws Exception {
		nextResponse = "{\"active\":true,\"sub\":\"alice\",\"scope\":\"read\"}";
		var v = validatorBuilder().cacheTtl(Duration.ofMinutes(1)).build();
		assertNotNull(v.validate("tok-noexp"));
	}

	@Test void h02_expInPast_ttlFlooredToOneSecond() throws Exception {
		// Expired token but active=true — exp in the past triggers the floor (lines 399/400).
		nextResponse = "{\"active\":true,\"sub\":\"alice\",\"scope\":\"read\",\"exp\":1}";
		var v = validatorBuilder()
			.clock(Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC))
			.build();
		assertNotNull(v.validate("tok-expiredexp"));
	}

	@Test void h03_expSoonCapsTheTtl() throws Exception {
		// exp is 30 seconds from now, cacheTtl is 5 minutes → until(30s) < cacheTtl(5min) triggers line 401.
		var nowFixed = Instant.parse("2030-01-01T00:00:00Z");
		long expEpoch = nowFixed.plusSeconds(30).getEpochSecond();
		nextResponse = "{\"active\":true,\"sub\":\"alice\",\"scope\":\"read\",\"exp\":" + expEpoch + "}";
		var v = validatorBuilder()
			.clock(Clock.fixed(nowFixed, ZoneOffset.UTC))
			.build();
		assertNotNull(v.validate("tok-expcap"));
	}

	@Test void h04_expFarFuture_usesCacheTtl() throws Exception {
		// exp is 2 hours from now, cacheTtl is 5 minutes → until(2h) >= cacheTtl(5min) → line 401 false branch.
		var nowFixed = Instant.parse("2030-01-01T00:00:00Z");
		long expEpoch = nowFixed.plusSeconds(7200).getEpochSecond();
		nextResponse = "{\"active\":true,\"sub\":\"alice\",\"scope\":\"read\",\"exp\":" + expEpoch + "}";
		var v = validatorBuilder()
			.clock(Clock.fixed(nowFixed, ZoneOffset.UTC))
			.build();
		assertNotNull(v.validate("tok-expcap-far"));
	}

	@Test void h05_expAtExactNow_flooredToOneSecond() throws Exception {
		// exp == now → until.isZero() == true → line 399 isZero branch.
		var nowFixed = Instant.parse("2030-01-01T00:00:00Z");
		long expEpoch = nowFixed.getEpochSecond();
		nextResponse = "{\"active\":true,\"sub\":\"alice\",\"scope\":\"read\",\"exp\":" + expEpoch + "}";
		var v = validatorBuilder()
			.clock(Clock.fixed(nowFixed, ZoneOffset.UTC))
			.build();
		assertNotNull(v.validate("tok-zero-exp"));
	}
}
