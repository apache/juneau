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

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.util.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.examples.mcp.*;
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.rest.client.mcp.auth.*;
import org.apache.juneau.rest.client.mcp.v20260728.*;

/**
 * A guided walkthrough of calling {@link SecuredExampleMcpServer}, showing the secured request/response
 * cycle a newcomer to MCP OAuth 2.1 needs to see: a rejected unauthenticated call (twice &mdash; once at the
 * raw wire level, once through the real {@link McpClient} SDK), the full RFC 9728 &rarr; RFC 8414 discovery
 * chain, a successful read-only call, a scoped step-up rejection, and finally a successful write once a token
 * carrying the write scope has been acquired.
 *
 * <p>
 * Run {@link #main(String[]) main} with three arguments &mdash; the secured server's endpoint and the demo
 * client id/secret &mdash; all three of which {@link SecuredExampleServer#main(String[])} prints on startup
 * (they cannot be hardcoded here: the demo client secret is freshly randomly generated on every server run, by
 * design; see {@link OfflineAuthorizationServer}). Unlike an earlier version of this walkthrough, the token
 * endpoint is no longer a fourth argument (M10): this client discovers it itself, the same way a real client
 * would.
 *
 * @serial exclude
 */
@SuppressWarnings({
	"java:S106" // Example walkthrough intentionally prints to stdout; console output is the demo's deliverable.
})
public final class SecuredExampleClient {

	/** The demo client's advertised {@code clientInfo.name}, shared by every {@link McpClient} connection below. */
	private static final String CLIENT_NAME = "juneau-secured-notes-example-client";

	/** The demo client's advertised {@code clientInfo.version}, shared by every {@link McpClient} connection below. */
	private static final String CLIENT_VERSION = "1.0.0";

	// Shared across calls rather than one HttpClient per request (S2095): HttpClient instances are heavyweight
	// (they own a connection pool and a selector thread), and the JDK explicitly recommends reusing a single
	// instance for the lifetime of the application rather than churning through short-lived ones.
	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

	private SecuredExampleClient() {}

	/**
	 * Runs the walkthrough against a running {@link SecuredExampleServer}.
	 *
	 * @param args Three required arguments: endpoint, client id, client secret &mdash; copy these from
	 * 	{@link SecuredExampleServer#main(String[])}'s startup banner.
	 * @throws Exception If any step fails unexpectedly (a REJECTED call is expected and handled, not thrown).
	 */
	@SuppressWarnings({
		"java:S112" // throws Exception intentional - example main() kept simple for demo readability
	})
	public static void main(String[] args) throws Exception {
		if (args.length < 3) {
			System.out.println("Usage: SecuredExampleClient <endpoint> <clientId> <clientSecret>");
			System.out.println("Copy these three values from the SecuredExampleServer startup banner.");
			return;
		}
		run(args[0], args[1], args[2]);
	}

	/**
	 * Executes each numbered step of the walkthrough.
	 *
	 * @param endpoint The secured MCP server's endpoint URL.
	 * @param clientId The demo OAuth client id.
	 * @param clientSecret The demo OAuth client secret.
	 * @throws Exception If an unexpected (non-auth-related) failure occurs.
	 */
	@SuppressWarnings({
		"java:S112" // throws Exception intentional - example walkthrough kept simple for demo readability
	})
	public static void run(String endpoint, String clientId, String clientSecret) throws Exception {

		section("1. Unauthenticated call, at the raw wire level — a 401 challenge");
		var challenge = rawUnauthenticatedCall(endpoint);

		section("2. The same unauthenticated call through the real McpClient SDK");
		mcpClientUnauthenticatedCall(endpoint);

		section("3. RFC 9728 discovery — fetch the Protected Resource Metadata the 401 pointed at");
		// Capture the Optional ONCE and guard/read the SAME instance, rather than calling
		// challenge.resourceMetadata() a second time inside the if-block to call get() on a fresh,
		// unguarded Optional. Without a discovered resource_metadata pointer there is no PRM to feed step 4,
		// so the walkthrough stops here instead of calling into step 4 with nothing to discover a token
		// endpoint from.
		var resourceMetadata = challenge == null ? Optional.<URI>empty() : challenge.resourceMetadata();
		if (resourceMetadata.isEmpty()) {
			System.out.println("   (no resource_metadata pointer found on the challenge; walkthrough cannot continue)");
			return;
		}
		var prm = discoverProtectedResourceMetadata(endpoint, resourceMetadata.get());

		section("4. RFC 8414 discovery — resolve the authorization server's token endpoint");
		var tokenEndpoint = discoverTokenEndpoint(prm);

		section("5. Acquire a read-only (mcp.read) bearer token and call again — success");
		readOnlyCall(endpoint, tokenEndpoint, clientId, clientSecret);

		section("6. The SAME read-only token attempting a write — a scoped 403 step-up challenge");
		insufficientScopeCall(endpoint, tokenEndpoint, clientId, clientSecret);

		section("7. Step up to mcp.read + mcp.write and publish a note — success");
		writeCall(endpoint, tokenEndpoint, clientId, clientSecret);

		System.out.println("\nWalkthrough complete.");
	}

	/**
	 * Step 1: a raw HTTP POST with no {@code Authorization} header at all, bypassing the {@link McpClient} SDK
	 * entirely so the actual {@code 401} status and {@code WWW-Authenticate} response header are directly
	 * visible. This is necessary because the resource-server gate's {@code 401}/{@code 403} rejections are
	 * plain-text bodies, not JSON-RPC envelopes — {@code AbstractMcpClient.send(...)} cannot parse one into a
	 * typed result and instead surfaces the failure as a bare {@link IOException} whose message embeds the
	 * status code as text. There is no way to read the status code, let alone the challenge header, through
	 * the SDK itself; only at the wire level.
	 */
	private static WwwAuthenticateChallenge rawUnauthenticatedCall(String endpoint) throws IOException, InterruptedException {
		var request = HttpRequest.newBuilder(URI.create(endpoint))
			.header("Content-Type", "application/json")
			.header("Accept", "application/json")
			.header("Mcp-Method", "server/discover")
			.header("Mcp-Name", "")
			.POST(HttpRequest.BodyPublishers.ofString(discoverRequestBody()))
			.build();
		var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
		System.out.println("   HTTP status: " + response.statusCode());
		var header = response.headers().firstValue("WWW-Authenticate").orElse(null);
		System.out.println("   WWW-Authenticate: " + header);
		var challenge = WwwAuthenticateChallenge.parse(header).orElse(null);
		if (challenge != null) {
			System.out.println("   parsed scope hint:       " + challenge.scopes());
			System.out.println("   parsed resource_metadata: " + challenge.resourceMetadata().orElse(null));
		}
		return challenge;
	}

	/**
	 * Step 2: the same unauthenticated call, but through {@link McpClient#connect(String)} — the ergonomic
	 * failure mode a real caller actually sees: a thrown {@link IOException} (see {@link #rawUnauthenticatedCall}
	 * above for why it is a bare {@link IOException} and not a typed exception) out of the mandatory
	 * {@code server/discover} handshake.
	 */
	private static void mcpClientUnauthenticatedCall(String endpoint) {
		try (var ignored = McpClient.connect(endpoint)) {
			throw new IllegalStateException("expected the unauthenticated connect() to fail, but it succeeded");
		} catch (IOException e) {
			System.out.println("   rejected as expected: " + e.getMessage());
		}
	}

	/**
	 * Step 3: fetches the RFC 9728 Protected Resource Metadata document the challenge pointed at, showing what
	 * a client learns from it — notably WHICH authorization server to acquire a token from, discovered from
	 * the response rather than hardcoded into this client.
	 */
	private static McpProtectedResourceMetadata discoverProtectedResourceMetadata(String endpoint, URI resourceMetadataUrl) {
		var prm = McpProtectedResourceMetadataClient.create()
			.expectedResource(URI.create(endpoint))
			.build()
			.fetch(resourceMetadataUrl);
		System.out.println("   resource:               " + prm.resource());
		System.out.println("   authorization_servers:  " + prm.authorizationServers());
		System.out.println("   scopes_supported:       " + prm.scopesSupported());
		return prm;
	}

	/**
	 * Step 4 (M10): performs real RFC 8414 discovery against the authorization server the PRM document
	 * advertised, via {@link McpProtectedResourceMetadataClient#discoverAuthorizationServer}, and returns the
	 * discovered {@code token_endpoint} — nothing here is hardcoded or passed in on the command line. This
	 * completes the full 401 &rarr; {@code resource_metadata} &rarr; PRM &rarr; {@code authorization_servers}
	 * &rarr; RFC 8414 &rarr; {@code token_endpoint} chain a compliant client walks end-to-end.
	 */
	private static URI discoverTokenEndpoint(McpProtectedResourceMetadata prm) {
		var as = McpProtectedResourceMetadataClient.create().build().discoverAuthorizationServer(prm);
		System.out.println("   issuer:         " + as.issuer());
		System.out.println("   token_endpoint: " + as.tokenEndpoint());
		return as.tokenEndpoint();
	}

	/**
	 * Step 5: acquires a real bearer token scoped to {@code mcp.read} only via
	 * {@link McpTokenProvider#clientCredentials()} (a genuine RFC 6749 &sect;4.4 HTTP round trip to the
	 * discovered token endpoint), wires it into a fresh {@link McpClient} via
	 * {@link McpTokenProvider#interceptor()}, and shows a read succeeding transparently.
	 */
	private static void readOnlyCall(String endpoint, URI tokenEndpoint, String clientId, String clientSecret) throws IOException {
		var tokens = readOnlyTokenProvider(endpoint, tokenEndpoint, clientId, clientSecret);
		try (var client = McpClient.connect(McpClient.builder()
				.endpoint(endpoint)
				.clientInfo(new Implementation().setName(CLIENT_NAME).setVersion(CLIENT_VERSION))
				.interceptor(tokens.interceptor()))) {
			System.out.println("   server/discover succeeded: " + Json.of(client.discoveredServer().getServerInfo()));
			var read = client.readResource(NoteStore.SCHEME + "index");
			System.out.println("   " + Json.of(read.getContents()));
		}
	}

	/**
	 * Step 6 (H3): the SAME read-only-scoped token from step 5's flavor, now attempting {@code publishNote} —
	 * a mutating tool that {@link SecuredExampleMcpServer} step-up-gates behind {@code mcp.write}. Demonstrates
	 * that holding a merely-valid, correctly-scoped-for-reads token is deliberately NOT enough: the call is
	 * rejected with a {@code 403} naming the missing {@code mcp.write} scope, not silently allowed. As with
	 * step 1/2 above, the 403's plain-text body surfaces as a bare {@link IOException}, not a typed exception.
	 */
	private static void insufficientScopeCall(String endpoint, URI tokenEndpoint, String clientId, String clientSecret) {
		var tokens = readOnlyTokenProvider(endpoint, tokenEndpoint, clientId, clientSecret);
		try (var client = McpClient.connect(McpClient.builder()
				.endpoint(endpoint)
				.clientInfo(new Implementation().setName(CLIENT_NAME).setVersion(CLIENT_VERSION))
				.interceptor(tokens.interceptor()))) {
			client.callToolText("publishNote", Map.of("title", "should-fail", "body", "should never be stored"));
			throw new IllegalStateException("expected the write with a read-only token to fail, but it succeeded");
		} catch (IOException e) {
			System.out.println("   rejected as expected: " + e.getMessage());
		}
	}

	/**
	 * Step 7: acquires a SECOND token, this time carrying both {@code mcp.read} and {@code mcp.write}, and
	 * shows the previously-rejected {@code publishNote} call (and a confirming read) now succeeding.
	 */
	private static void writeCall(String endpoint, URI tokenEndpoint, String clientId, String clientSecret) throws IOException {
		var tokens = McpTokenProvider.clientCredentials()
			.tokenEndpoint(tokenEndpoint)
			.clientId(clientId)
			.clientSecret(clientSecret)
			.resource(URI.create(endpoint))
			.scope(SecuredExampleMcpServer.READ_SCOPE, SecuredExampleMcpServer.WRITE_SCOPE)
			.build();

		try (var client = McpClient.connect(McpClient.builder()
				.endpoint(endpoint)
				.clientInfo(new Implementation().setName(CLIENT_NAME).setVersion(CLIENT_VERSION))
				.interceptor(tokens.interceptor()))) {
			System.out.println("   -> " + client.callToolText("publishNote",
				Map.of("title", "secured", "body", "Hello from behind OAuth 2.1")));

			var read = client.readResource(NoteStore.uriFor("secured"));
			System.out.println("   " + Json.of(read.getContents()));
		}
	}

	/** Builds a token provider scoped to {@link SecuredExampleMcpServer#READ_SCOPE} only. */
	private static McpTokenProvider readOnlyTokenProvider(String endpoint, URI tokenEndpoint, String clientId, String clientSecret) {
		return McpTokenProvider.clientCredentials()
			.tokenEndpoint(tokenEndpoint)
			.clientId(clientId)
			.clientSecret(clientSecret)
			.resource(URI.create(endpoint))
			.scope(SecuredExampleMcpServer.READ_SCOPE)
			.build();
	}

	/**
	 * Builds a plausible (but not necessarily complete) {@code server/discover} JSON-RPC request body using
	 * the real wire beans, purely so step 1's raw HTTP call looks like a genuine MCP request on the wire. The
	 * resource-server bearer gate rejects it before the body is ever parsed, so its exact contents do not
	 * affect the outcome — but a reader should see real MCP traffic here, not an arbitrary placeholder.
	 */
	private static String discoverRequestBody() {
		var params = new RequestParamsOnly().setMeta(new RequestMeta()
			.setProtocolVersion(McpProtocol.VERSION_2026_07_28)
			.setClientInfo(new Implementation().setName(CLIENT_NAME).setVersion(CLIENT_VERSION))
			.setClientCapabilities(new ClientCapabilities()));
		var req = new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0)
			.setId(1)
			.setMethod("server/discover")
			.setParams(params);
		return Json.of(req);
	}

	private static void section(String title) {
		System.out.println("\n=== " + title + " ===");
	}
}
