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

import org.apache.juneau.*;
import org.apache.juneau.rest.server.auth.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Tests that {@link OAuthIntrospectionValidator} binds introspected tokens to an expected audience/resource
 * when one is configured via {@link OAuthIntrospectionValidator.Builder#audience(String...)} /
 * {@link OAuthIntrospectionValidator.Builder#resource(String...)}, and leaves today's active/scope-only
 * behavior unchanged when no audience is configured.
 *
 * @since 10.0.0
 */
class OAuthIntrospectionValidator_Audience_Test extends TestBase {

	private HttpServer server;
	private volatile String nextResponse;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.createContext("/introspect", ex -> {
			var body = nextResponse.getBytes(StandardCharsets.UTF_8);
			ex.getResponseHeaders().add("Content-Type", "application/json");
			ex.sendResponseHeaders(200, body.length);
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
	// A: expected audience configured — mismatch rejected even though active=true and scopes match.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_audienceConfigured_mismatchedSingleAud_rejected() {
		nextResponse = "{\"active\":true,\"sub\":\"alice\",\"scope\":\"read\",\"aud\":\"https://other.example\"}";
		var v = validatorBuilder().audience("https://api.example").requiredScopes("read").build();
		var ex = assertThrows(AuthenticationException.class, () -> v.validate("tok-wrong-aud"));
		assertTrue(ex.getMessage().toLowerCase().contains("audience"), "Expected an audience-mismatch message, got: " + ex.getMessage());
	}

	@Test void a02_audienceConfigured_noAudInResponse_rejected() {
		// Token is active with matching scopes but the AS returned no aud claim at all.
		nextResponse = "{\"active\":true,\"sub\":\"alice\",\"scope\":\"read\"}";
		var v = validatorBuilder().audience("https://api.example").build();
		var ex = assertThrows(AuthenticationException.class, () -> v.validate("tok-no-aud"));
		assertTrue(ex.getMessage().toLowerCase().contains("audience"), "Expected an audience-mismatch message, got: " + ex.getMessage());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: expected audience configured — matching audience accepted.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_audienceConfigured_matchingSingleAud_accepted() throws Exception {
		nextResponse = "{\"active\":true,\"sub\":\"alice\",\"scope\":\"read\",\"aud\":\"https://api.example\"}";
		var v = validatorBuilder().audience("https://api.example").build();
		var p = v.validate("tok-right-aud");
		assertEquals("alice", p.getName());
	}

	@Test void b02_multiValuedAud_containingExpectedValue_accepted() throws Exception {
		// aud as a JSON array with multiple values; the expected value is one of several.
		nextResponse = "{\"active\":true,\"sub\":\"alice\",\"scope\":\"read\",\"aud\":[\"https://other.example\",\"https://api.example\"]}";
		var v = validatorBuilder().audience("https://api.example").build();
		var p = v.validate("tok-multi-aud");
		assertEquals("alice", p.getName());
	}

	@Test void b03_resourceAlias_matchesAudClaim() throws Exception {
		// resource(...) is an alias for audience(...) — validated against the same aud claim.
		nextResponse = "{\"active\":true,\"sub\":\"alice\",\"scope\":\"read\",\"aud\":\"https://api.example\"}";
		var v = validatorBuilder().resource("https://api.example").build();
		var p = v.validate("tok-resource-alias");
		assertEquals("alice", p.getName());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: no audience configured — today's active/scope-only behavior is unchanged (regression guard).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_noAudienceConfigured_mismatchedAudStillAccepted() throws Exception {
		// No audience bind configured — an "aud" for a completely different RS must NOT cause rejection.
		nextResponse = "{\"active\":true,\"sub\":\"alice\",\"scope\":\"read\",\"aud\":\"https://other.example\"}";
		var v = validatorBuilder().build();
		var p = v.validate("tok-no-bind");
		assertEquals("alice", p.getName());
	}

	@Test void c02_noAudienceConfigured_noAudClaimAtAll_stillAccepted() throws Exception {
		nextResponse = "{\"active\":true,\"sub\":\"alice\",\"scope\":\"read\"}";
		var v = validatorBuilder().build();
		var p = v.validate("tok-no-bind-no-aud");
		assertEquals("alice", p.getName());
	}
}
