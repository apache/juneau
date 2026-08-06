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
package org.apache.juneau.examples.mcp.secured;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.TestBase;
import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.examples.mcp.*;
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.rest.client.mcp.auth.*;
import org.apache.juneau.rest.client.mcp.v20260728.*;
import org.junit.jupiter.api.*;

import com.nimbusds.jwt.*;

/**
 * Proves the OAuth 2.1-secured variant actually works: boots {@link SecuredExampleServer} (which brings up
 * its own in-process {@link OfflineAuthorizationServer}) on an ephemeral port and drives both a raw
 * {@link HttpClient} (to inspect the exact {@code 401}/{@code 403}/{@code WWW-Authenticate} wire behavior) and
 * the real {@link McpClient} SDK (to prove a genuinely-acquired bearer token round-trips a tool call
 * end-to-end) against it.
 *
 * <p>
 * Follows the same fixture shape as {@code ExampleMcpEndToEnd_Test} (share one server/AS pair across the
 * class; each test that mutates note state uses its own distinctly-titled note) and the same assertion style
 * as {@code McpResourceServerBinding_Test} (challenge header parsing via {@link WwwAuthenticateChallenge}).
 *
 * <p>
 * Covers, in order: rejected calls (missing/garbage/wrong-audience/expired/wrong-issuer/{@code alg=none}
 * tokens, section {@code a}), RFC 9728 discovery (section {@code b}), H3's per-operation
 * {@code mcp.write} step-up (section {@code c}), a successfully-dispatched call plus the M7 token-caching
 * proof (section {@code d}), and a full {@link SecuredExampleClient#run} walkthrough (section {@code e}).
 */
class SecuredExampleMcpEndToEnd_Test extends TestBase {

	private static SecuredExampleServer server;
	private static HttpClient http;

	@BeforeAll
	static void setUp() throws Exception {
		server = SecuredExampleServer.start(0);
		http = HttpClient.newHttpClient();
	}

	@AfterAll
	static void tearDown() throws Exception {
		if (server != null)
			server.close();
	}

	/** A request builder pre-populated with the headers a real v2 MCP JSON-RPC POST requires. */
	private static HttpRequest.Builder discoverRequest() {
		return HttpRequest.newBuilder(server.getRootUrl())
			.header("Content-Type", "application/json")
			.header("Accept", "application/json")
			.header("Mcp-Method", "server/discover")
			.header("Mcp-Name", "");
	}

	/** A request builder for a {@code tools/call} POST invoking {@code toolName}. */
	private static HttpRequest.Builder toolsCallRequest(String toolName) {
		return HttpRequest.newBuilder(server.getRootUrl())
			.header("Content-Type", "application/json")
			.header("Accept", "application/json")
			.header("Mcp-Method", "tools/call")
			.header("Mcp-Name", toolName);
	}

	/** A real {@code tools/call} JSON-RPC request body for {@code toolName}, using the actual wire beans. */
	private static String toolsCallBody(String toolName, Map<String,Object> arguments) {
		var params = new CallToolRequest()
			.setName(toolName)
			.setArguments(arguments)
			.setMeta(new RequestMeta()
				.setProtocolVersion(McpProtocol.VERSION_2026_07_28)
				.setClientInfo(new Implementation().setName("test-client").setVersion("1.0.0"))
				.setClientCapabilities(new ClientCapabilities()));
		var req = new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0)
			.setId(1)
			.setMethod("tools/call")
			.setParams(params);
		return Json.of(req);
	}

	private static WwwAuthenticateChallenge challenge(HttpResponse<?> response) {
		return WwwAuthenticateChallenge.parse(response.headers().firstValue("WWW-Authenticate").orElse(null))
			.orElseThrow(() -> new AssertionError("no WWW-Authenticate header on response: " + response));
	}

	// -------- a: rejected - no token / garbage / wrong-audience / expired / wrong-issuer / alg=none --------

	@Test
	void a01_missingToken_401WithBearerChallengeAndScopeHint() throws Exception {
		var response = http.send(discoverRequest().POST(HttpRequest.BodyPublishers.ofString("{}")).build(),
			HttpResponse.BodyHandlers.ofString());
		assertEquals(401, response.statusCode());
		var c = challenge(response);
		assertTrue(c.isBearer());
		assertEquals(Set.of(SecuredExampleMcpServer.READ_SCOPE), c.scopes());
		// N3 (RFC 6750 §3): no credentials were presented at all, so the challenge must carry no error code -
		// that is reserved for a credential that WAS presented but rejected (a02/a03 below).
		assertTrue(c.error().isEmpty(), "a credential-less challenge must not carry an error code");
		// N2: assert the actual resource_metadata value, not merely that one is present.
		assertEquals(Optional.of(server.getRootUrl().resolve(".well-known/oauth-protected-resource")), c.resourceMetadata());
	}

	@Test
	void a02_garbageToken_401WithInvalidTokenError() throws Exception {
		var response = http.send(discoverRequest().header("Authorization", "Bearer not-a-real-jwt")
			.POST(HttpRequest.BodyPublishers.ofString("{}")).build(), HttpResponse.BodyHandlers.ofString());
		assertEquals(401, response.statusCode());
		assertEquals(Optional.of("invalid_token"), challenge(response).error());
	}

	@Test
	void a03_wrongAudienceToken_401WithInvalidTokenError() throws Exception {
		// A token minted for a DIFFERENT resource must be rejected even though it is otherwise perfectly
		// valid and correctly signed by the trusted offline authorization server - RFC 8707 audience
		// enforcement (the confused-deputy defense).
		var auth = server.getAuthServer();
		var wrongAudienceToken = McpTokenProvider.clientCredentials()
			.tokenEndpoint(auth.tokenEndpoint())
			.clientId(auth.clientId())
			.clientSecret(auth.clientSecret())
			.resource(URI.create("http://localhost:1/a-different-resource"))
			.build()
			.get();
		var response = http.send(discoverRequest().header("Authorization", "Bearer " + wrongAudienceToken)
			.POST(HttpRequest.BodyPublishers.ofString("{}")).build(), HttpResponse.BodyHandlers.ofString());
		assertEquals(401, response.statusCode());
		assertEquals(Optional.of("invalid_token"), challenge(response).error());
	}

	@Test
	void a04_expiredToken_401WithInvalidTokenError() throws Exception {
		// H4 test seam: mintAccessToken(...) lets us mint an already-expired token directly, without a real
		// clock needing to actually elapse five minutes.
		var auth = server.getAuthServer();
		var expiredToken = auth.mintAccessToken(server.getRootUrl().toString(), SecuredExampleMcpServer.READ_SCOPE,
			Instant.now().minus(Duration.ofHours(1)), Duration.ofMinutes(5));
		var response = http.send(discoverRequest().header("Authorization", "Bearer " + expiredToken)
			.POST(HttpRequest.BodyPublishers.ofString("{}")).build(), HttpResponse.BodyHandlers.ofString());
		assertEquals(401, response.statusCode());
		assertEquals(Optional.of("invalid_token"), challenge(response).error());
	}

	@Test
	void a05_wrongIssuerToken_401WithInvalidTokenError() throws Exception {
		// H4: the issuer is now a constructor-supplied instance field (not a shared static constant), so a
		// second, independently-started offline AS naturally has both a different issuer AND a different
		// signing key from the one this server's JwtTokenValidator trusts - a token it mints must be rejected
		// exactly like any other untrusted issuer's token.
		try (var otherAuth = OfflineAuthorizationServer.start()) {
			var wrongIssuerToken = McpTokenProvider.clientCredentials()
				.tokenEndpoint(otherAuth.tokenEndpoint())
				.clientId(otherAuth.clientId())
				.clientSecret(otherAuth.clientSecret())
				.resource(server.getRootUrl())
				.scope(SecuredExampleMcpServer.READ_SCOPE)
				.build()
				.get();
			var response = http.send(discoverRequest().header("Authorization", "Bearer " + wrongIssuerToken)
				.POST(HttpRequest.BodyPublishers.ofString("{}")).build(), HttpResponse.BodyHandlers.ofString());
			assertEquals(401, response.statusCode());
			assertEquals(Optional.of("invalid_token"), challenge(response).error());
		}
	}

	@Test
	void a06_algNoneToken_401WithInvalidTokenError() throws Exception {
		// N5 (teaching artifact): JwtTokenValidator explicitly rejects unsigned/alg=none JWTs outright (it
		// never reaches signature verification), regardless of how plausible the claims otherwise look.
		var auth = server.getAuthServer();
		var claims = new JWTClaimsSet.Builder()
			.subject(auth.clientId())
			.issuer(auth.issuerUri().toString())
			.audience(server.getRootUrl().toString())
			.claim("scope", SecuredExampleMcpServer.READ_SCOPE)
			.issueTime(Date.from(Instant.now()))
			.expirationTime(Date.from(Instant.now().plus(Duration.ofMinutes(5))))
			.build();
		var algNoneToken = new PlainJWT(claims).serialize();
		var response = http.send(discoverRequest().header("Authorization", "Bearer " + algNoneToken)
			.POST(HttpRequest.BodyPublishers.ofString("{}")).build(), HttpResponse.BodyHandlers.ofString());
		assertEquals(401, response.statusCode());
		assertEquals(Optional.of("invalid_token"), challenge(response).error());
	}

	// -------- b: RFC 9728 Protected Resource Metadata --------

	@Test
	void b01_wellKnownPrm_advertisesResourceAndOfflineAuthorizationServer() throws Exception {
		var prmUrl = server.getRootUrl().resolve(".well-known/oauth-protected-resource");
		var response = http.send(HttpRequest.newBuilder(prmUrl).GET().build(), HttpResponse.BodyHandlers.ofString());
		assertEquals(200, response.statusCode());
		var prm = McpProtectedResourceMetadataClient.create().build().parse(response.body(), prmUrl);
		assertEquals(server.getRootUrl(), prm.resource());
		assertEquals(List.of(server.getAuthServer().issuerUri()), prm.authorizationServers());
		// H3: addOperationScope(...) also advertises mcp.write as supported, on top of the mcp.read baseline.
		assertEquals(Set.of(SecuredExampleMcpServer.READ_SCOPE, SecuredExampleMcpServer.WRITE_SCOPE), prm.scopesSupported());
	}

	// -------- c: H3/H4 - baseline scope and per-operation mcp.write step-up enforcement --------

	@Test
	void c01_insufficientBaselineScope_403WithInsufficientScopeError() throws Exception {
		// A token that is otherwise perfectly valid but was never granted the endpoint-wide mcp.read
		// baseline is 403'd before any JSON-RPC method (even server/discover) dispatches.
		var auth = server.getAuthServer();
		var noBaselineToken = McpTokenProvider.clientCredentials()
			.tokenEndpoint(auth.tokenEndpoint())
			.clientId(auth.clientId())
			.clientSecret(auth.clientSecret())
			.resource(server.getRootUrl())
			.scope(SecuredExampleMcpServer.WRITE_SCOPE)
			.build()
			.get();
		var response = http.send(discoverRequest().header("Authorization", "Bearer " + noBaselineToken)
			.POST(HttpRequest.BodyPublishers.ofString("{}")).build(), HttpResponse.BodyHandlers.ofString());
		assertEquals(403, response.statusCode());
		var c = challenge(response);
		assertEquals(Optional.of("insufficient_scope"), c.error());
		assertEquals(Set.of(SecuredExampleMcpServer.READ_SCOPE), c.scopes());
	}

	@Test
	void c02_readOnlyToken_canReadButCannotWrite() throws Exception {
		var auth = server.getAuthServer();

		// The mcp.read baseline alone is enough to discover the server and read a resource.
		var readTokens = McpTokenProvider.clientCredentials()
			.tokenEndpoint(auth.tokenEndpoint())
			.clientId(auth.clientId())
			.clientSecret(auth.clientSecret())
			.resource(server.getRootUrl())
			.scope(SecuredExampleMcpServer.READ_SCOPE)
			.build();
		try (var client = McpClient.connect(McpClient.builder()
				.endpoint(server.getRootUrl().toString())
				.interceptor(readTokens.interceptor()))) {
			assertNotNull(client.readResource(NoteStore.SCHEME + "index"));
		}

		// H3/H4: but that SAME mcp.read-only token is 403'd, naming mcp.write specifically, on EITHER
		// step-up-gated mutating tool.
		var readOnlyToken = readTokens.get();
		for (var tool : List.of("publishNote", "deleteNote")) {
			var response = http.send(toolsCallRequest(tool)
				.header("Authorization", "Bearer " + readOnlyToken)
				.POST(HttpRequest.BodyPublishers.ofString(toolsCallBody(tool, Map.of("title", "step-up-probe", "body", "x"))))
				.build(), HttpResponse.BodyHandlers.ofString());
			assertEquals(403, response.statusCode(), tool + " must require mcp.write on top of the mcp.read baseline");
			var c = challenge(response);
			assertEquals(Optional.of("insufficient_scope"), c.error());
			assertEquals(Set.of(SecuredExampleMcpServer.WRITE_SCOPE), c.scopes());
		}
	}

	@Test
	void c03_readWriteToken_dispatchesWrites() throws Exception {
		// A token carrying BOTH the baseline and the step-up scope dispatches publishNote/deleteNote normally.
		// deleteNote advertises an elicitation (confirm) round trip, so the client must both advertise the
		// elicitation capability and answer it - unrelated to the OAuth scoping this test targets, but
		// required for the call to reach a successful outcome at all.
		var auth = server.getAuthServer();
		var tokens = McpTokenProvider.clientCredentials()
			.tokenEndpoint(auth.tokenEndpoint())
			.clientId(auth.clientId())
			.clientSecret(auth.clientSecret())
			.resource(server.getRootUrl())
			.scope(SecuredExampleMcpServer.READ_SCOPE, SecuredExampleMcpServer.WRITE_SCOPE)
			.build();
		try (var client = McpClient.connect(McpClient.builder()
				.endpoint(server.getRootUrl().toString())
				.clientCapabilities(new ClientCapabilities().setElicitation(new ElicitationCapability()))
				.interceptor(tokens.interceptor()))) {
			var stored = client.callTool("publishNote", Map.of("title", "step-up-note", "body", "hello"));
			assertEquals("Stored note 'step-up-note' (5 chars).", stored.firstText());
			var deleted = client.callToolWithElicitation("deleteNote", Map.of("title", "step-up-note"),
				requests -> {
					var answers = new LinkedHashMap<String,ElicitResult>();
					requests.keySet().forEach(id -> answers.put(id,
						new ElicitResult().setAction(ElicitAction.ACCEPT).putContent("confirm", true)));
					return answers;
				});
			assertNotNull(deleted);
		}
	}

	// -------- d: accepted - a real token acquired from the offline authorization server --------

	@Test
	void d01_validToken_dispatchesAndRoundTripsANote() throws Exception {
		var auth = server.getAuthServer();
		var tokens = McpTokenProvider.clientCredentials()
			.tokenEndpoint(auth.tokenEndpoint())
			.clientId(auth.clientId())
			.clientSecret(auth.clientSecret())
			.resource(server.getRootUrl())
			.scope(SecuredExampleMcpServer.READ_SCOPE, SecuredExampleMcpServer.WRITE_SCOPE)
			.build();

		try (var client = McpClient.connect(McpClient.builder()
				.endpoint(server.getRootUrl().toString())
				.interceptor(tokens.interceptor()))) {
			assertBean(client.discoveredServer(), "serverInfo{name}", "{juneau-notes-example}");

			var stored = client.callTool("publishNote", Map.of("title", "secured-note", "body", "hello"));
			assertEquals("Stored note 'secured-note' (5 chars).", stored.firstText());

			var read = client.readResource(NoteStore.uriFor("secured-note"));
			assertEquals("hello", ((TextResourceContents) read.getContents().get(0)).getText());
		}
	}

	@Test
	void d02_validToken_singleTokenRequestReusedAcrossCalls() throws Exception {
		// M7: proves the caching McpTokenProvider genuinely reuses ONE acquired token across multiple
		// dispatches on the same connection, by counting actual /token HTTP round trips on the offline AS -
		// not merely asserting the calls happen to succeed (which would also be true of a provider that
		// re-requested a token on every single call).
		var auth = server.getAuthServer();
		var before = auth.tokenRequestCount();
		var tokens = McpTokenProvider.clientCredentials()
			.tokenEndpoint(auth.tokenEndpoint())
			.clientId(auth.clientId())
			.clientSecret(auth.clientSecret())
			.resource(server.getRootUrl())
			.scope(SecuredExampleMcpServer.READ_SCOPE, SecuredExampleMcpServer.WRITE_SCOPE)
			.build();

		try (var client = McpClient.connect(McpClient.builder()
				.endpoint(server.getRootUrl().toString())
				.interceptor(tokens.interceptor()))) {
			client.callTool("publishNote", Map.of("title", "cached-note", "body", "hi"));
			var contents = client.readResource(NoteStore.SCHEME + "index").getContents();
			assertTrue(((TextResourceContents) contents.get(0)).getText().contains("cached-note"));
			client.readResource(NoteStore.uriFor("cached-note"));
		}

		assertEquals(before + 1, auth.tokenRequestCount(),
			"a single cached token must be reused across all three dispatches above, not re-requested per call");
	}

	// -------- e: M8 - SecuredExampleClient's own end-to-end walkthrough --------

	@Test
	void e01_run_completesWithoutThrowing() throws Exception {
		// A dedicated, independently-started server/AS pair (own port, own notes, own demo credentials) so
		// this walkthrough - which publishes/reads its own notes and deliberately triggers a rejected call -
		// cannot collide with any test above sharing the class-level fixture.
		try (var standalone = SecuredExampleServer.start(0)) {
			var auth = standalone.getAuthServer();
			assertDoesNotThrow(() -> SecuredExampleClient.run(standalone.getRootUrl().toString(), auth.clientId(), auth.clientSecret()));
		}
	}
}
