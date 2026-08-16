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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.auth.*;
import org.apache.juneau.rest.server.mcp.McpServerConfig;
import org.apache.juneau.rest.server.mcp.McpToolHandler;
import org.apache.juneau.rest.server.mcp.McpToolOutcome;
import org.apache.juneau.rest.server.mcp.McpToolSpec;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * End-to-end HTTP coverage for the READY-312f F2 OAuth 2.1 resource-server baseline on the {@code 2026-07-28}
 * {@link McpRestServlet} and {@link McpEndpoint} bindings: RFC 9728 PRM serving + SEP-2351 well-known routing, bearer
 * validation, the {@code 401 WWW-Authenticate} challenge with {@code resource_metadata}, RFC 8707 audience enforcement,
 * baseline insufficient-scope handling, and the auth-disabled default leaving behavior unchanged.
 */
@SuppressWarnings({
	"resource" // Test helpers return Closeables; Eclipse JDT @Owning warning is by design.
})
class McpResourceServerBinding_Test extends TestBase {

	private static final String RESOURCE = "http://localhost/mcp";
	private static final String PRM_URL = "http://localhost/.well-known/oauth-protected-resource/mcp";
	private static final String ROOT_PRM_URL = "http://localhost/.well-known/oauth-protected-resource";

	/**
	 * Fake validator: {@code good} = correct audience + required scope; {@code noscope} = correct audience, wrong scope;
	 * {@code wrongaud} = wrong audience; anything else throws.
	 */
	private static final TokenValidator VALIDATOR = token -> switch (token) {
		case "good" -> new ClaimsPrincipal("alice", Map.of("aud", RESOURCE, "scope", "mcp.read mcp.write"));
		// Cross-identity fixture: a DIFFERENT authenticated principal (bob) with the same audience + baseline
		// scope, so it clears the gate but binds a different requestState identity than 'good' (alice).
		case "good2" -> new ClaimsPrincipal("bob", Map.of("aud", RESOURCE, "scope", "mcp.read mcp.write"));
		// Real iss|sub claim fixtures: 'good'/'good2' above carry no iss/sub claims, so the requestState
		// binding they exercise end-to-end is only the getName() fallback. 'good3'/'good4' carry the SAME subject
		// (alice) but a DIFFERENT issuer, so they exercise the actual, primary iss|sub claim-read path (settled
		// decision 1) end-to-end, not the fallback.
		case "good3" -> new ClaimsPrincipal("alice", Map.of("aud", RESOURCE, "scope", "mcp.read mcp.write", "iss", "https://idp-a", "sub", "alice"));
		case "good4" -> new ClaimsPrincipal("alice", Map.of("aud", RESOURCE, "scope", "mcp.read mcp.write", "iss", "https://idp-b", "sub", "alice"));
		case "noscope" -> new ClaimsPrincipal("alice", Map.of("aud", RESOURCE, "scope", "other"));
		case "wrongaud" -> new ClaimsPrincipal("alice", Map.of("aud", "http://evil.example.com", "scope", "mcp.read"));
		// SEP-2350 step-up fixtures: baseline mcp.read + the per-operation tools.exec (exact grant vs. a
		// broader-but-not-exact grant that must NOT satisfy it).
		case "opok" -> new ClaimsPrincipal("alice", Map.of("aud", RESOURCE, "scope", "mcp.read tools.exec"));
		case "broad" -> new ClaimsPrincipal("alice", Map.of("aud", RESOURCE, "scope", "mcp.read tools"));
		// Baseline fixture: granted ONLY the broader "mcp" scope, which does NOT satisfy the exact-match baseline
		// requirement "mcp.read" (OAuth scopes have no universal hierarchy).
		case "basehier" -> new ClaimsPrincipal("alice", Map.of("aud", RESOURCE, "scope", "mcp"));
		default -> throw new AuthenticationException("bad token");
	};

	private static Object validMeta() {
		return JsonMap.of(
			RequestMeta.KEY_PROTOCOL_VERSION, "2026-07-28",
			RequestMeta.KEY_CLIENT_INFO, JsonMap.of("name", "fixture-client", "version", "1.0"),
			RequestMeta.KEY_CLIENT_CAPABILITIES, JsonMap.of());
	}

	private static Object withMeta(Object baseParams) {
		var p = baseParams instanceof Map<?,?> m ? new JsonMap(m) : new JsonMap();
		p.put("_meta", validMeta());
		return p;
	}

	private static String body(Object id, String method, Object params) {
		return org.apache.juneau.marshall.marshaller.Json.of(new JsonRpcRequest().setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(id).setMethod(method).setParams(withMeta(params)));
	}

	private static McpToolHandler echo() {
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() { return new McpToolSpec().setName("echo").setDescription("Echoes back"); }
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) { return McpToolOutcome.text(String.valueOf(arguments.get("text"))); }
		};
	}

	private static McpOptions rsEnabled() {
		return new McpOptions().resourceServer(rs -> rs
			.setEnabled(true)
			.setResource(URI.create(RESOURCE))
			.setTokenValidator(VALIDATOR)
			.addAuthorizationServer(URI.create("http://as.example.com"))
			.addRequiredScope("mcp.read"));
	}

	// ---------------------------------------------------------------------------------------------
	// Servlet path (POST /) with RS auth ENABLED.
	// ---------------------------------------------------------------------------------------------

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class A extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().setName("test").setVersion("1.0.0").addTool(echo());
		}
		@Override protected McpOptions createMcpOptions() { return rsEnabled(); }

		// M1 probe: a non-MCP POST op that is still gated (the servlet gates every effective POST); asserts the
		// authenticated principal reached the framework-standard PRINCIPAL_ATTR so @Auth Principal resolves.
		@RestPost(path = "/whoami")
		public String whoami(@Auth Principal p) {
			return p == null ? "<none>" : p.getName();
		}
	}

	private MockRestClient clientA() {
		return MockRestClient.create(A.class).json().contentType("application/json").accept("application/json").ignoreErrors().build();
	}

	@Test void a01_validToken_dispatches() throws Exception {
		clientA().post("/").contentString(body(1, "server/discover", null))
			.header("Mcp-Method", "server/discover").header("Mcp-Name", "")
			.header("Authorization", "Bearer good")
			.run().assertStatus(200);
	}

	@Test void a02_missingToken_401WithResourceMetadataAndScopeHint() throws Exception {
		// M1/SEP-2350: the initial 401 carries no error code but DOES hint the baseline scope.
		clientA().post("/").contentString(body(1, "server/discover", null))
			.header("Mcp-Method", "server/discover").header("Mcp-Name", "")
			.run()
			.assertStatus(401)
			.assertHeader("WWW-Authenticate").is("Bearer realm=\"mcp\", scope=\"mcp.read\", resource_metadata=\"" + PRM_URL + "\"");
	}

	@Test void a03_invalidToken_401WithInvalidTokenError() throws Exception {
		clientA().post("/").contentString(body(1, "server/discover", null))
			.header("Mcp-Method", "server/discover").header("Mcp-Name", "")
			.header("Authorization", "Bearer nope")
			.run()
			.assertStatus(401)
			.assertHeader("WWW-Authenticate").is("Bearer realm=\"mcp\", error=\"invalid_token\", resource_metadata=\"" + PRM_URL + "\"");
	}

	@Test void a04_wrongAudience_rejected() throws Exception {
		clientA().post("/").contentString(body(1, "server/discover", null))
			.header("Mcp-Method", "server/discover").header("Mcp-Name", "")
			.header("Authorization", "Bearer wrongaud")
			.run()
			.assertStatus(401)
			.assertHeader("WWW-Authenticate").is("Bearer realm=\"mcp\", error=\"invalid_token\", resource_metadata=\"" + PRM_URL + "\"");
	}

	@Test void a05_insufficientScope_403WithScope() throws Exception {
		clientA().post("/").contentString(body(1, "server/discover", null))
			.header("Mcp-Method", "server/discover").header("Mcp-Name", "")
			.header("Authorization", "Bearer noscope")
			.run()
			.assertStatus(403)
			.assertHeader("WWW-Authenticate").is("Bearer realm=\"mcp\", error=\"insufficient_scope\", scope=\"mcp.read\", resource_metadata=\"" + PRM_URL + "\"");
	}

	@Test void a06_wellKnownPathInserted_servesPrmUnauthenticated() throws Exception {
		var json = clientA().get("/.well-known/oauth-protected-resource/mcp").run().assertStatus(200).getContent().asString();
		assertContains("\"resource\":\"" + RESOURCE + "\"", json);
		assertContains("\"authorization_servers\":[\"http://as.example.com\"]", json);
		assertContains("\"scopes_supported\":[\"mcp.read\"]", json);
		assertContains("\"bearer_methods_supported\":[\"header\"]", json);
	}

	@Test void a07_wellKnownRootFallback_servesPrm() throws Exception {
		var json = clientA().get(ROOT_PRM_URL.substring("http://localhost".length())).run().assertStatus(200).getContent().asString();
		assertContains("\"resource\":\"" + RESOURCE + "\"", json);
	}

	// M1: the authenticated principal reaches the framework-standard PRINCIPAL_ATTR so @Auth Principal resolves on a
	// gated request.
	@Test void a09_principalResolvesViaAuthArg() throws Exception {
		var s = clientA().post("/whoami").contentString("").header("Authorization", "Bearer good").run().assertStatus(200).getContent().asString();
		assertContains("alice", s);
	}

	// A token granted only the broader "mcp" scope does NOT satisfy the exact-match baseline requirement
	// "mcp.read": a broad-but-differently-named scope must never authorize a differently named privileged operation.
	@Test void a10_baselineBroaderScopeDoesNotSatisfy_403() throws Exception {
		clientA().post("/").contentString(body(1, "server/discover", null))
			.header("Mcp-Method", "server/discover").header("Mcp-Name", "")
			.header("Authorization", "Bearer basehier")
			.run()
			.assertStatus(403)
			.assertHeader("WWW-Authenticate").is("Bearer realm=\"mcp\", error=\"insufficient_scope\", scope=\"mcp.read\", resource_metadata=\"" + PRM_URL + "\"");
	}

	// M5: the wildcard well-known route must not serve this endpoint's PRM for an arbitrary suffix.
	@Test void a08_wellKnownWrongSuffix_404() throws Exception {
		clientA().get("/.well-known/oauth-protected-resource/some-other-resource").run().assertStatus(404);
	}

	// ---------------------------------------------------------------------------------------------
	// SEP-2350 (F3) per-operation step-up: an end-to-end HTTP round-trip exercising the POST-parse enforcement
	// point in McpRevision.dispatch (a token satisfying the endpoint-wide baseline but not the operation's scope
	// yields a scoped 403 insufficient_scope challenge; only the exact required scope dispatches).
	// ---------------------------------------------------------------------------------------------

	private static McpOptions rsEnabledWithOpScope() {
		return new McpOptions().resourceServer(rs -> rs
			.setEnabled(true)
			.setResource(URI.create(RESOURCE))
			.setTokenValidator(VALIDATOR)
			.addAuthorizationServer(URI.create("http://as.example.com"))
			.addRequiredScope("mcp.read")
			.addOperationScope("tools/call", "echo", "tools.exec"));
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class D extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().setName("test").setVersion("1.0.0").addTool(echo());
		}
		@Override protected McpOptions createMcpOptions() { return rsEnabledWithOpScope(); }
	}

	private MockRestClient clientD() {
		return MockRestClient.create(D.class).json().contentType("application/json").accept("application/json").ignoreErrors().build();
	}

	private static Object callEcho() {
		return JsonMap.of("name", "echo", "arguments", JsonMap.of("text", "hi"));
	}

	// Baseline-only token (mcp.read) cannot invoke the tools.exec-gated echo tool: scoped 403 step-up challenge.
	@Test void d01_stepUp_insufficientOperationScope_403() throws Exception {
		clientD().post("/").contentString(body(1, "tools/call", callEcho()))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "echo")
			.header("Authorization", "Bearer good")
			.run()
			.assertStatus(403)
			.assertHeader("WWW-Authenticate").is("Bearer realm=\"mcp\", error=\"insufficient_scope\", scope=\"tools.exec\", resource_metadata=\"" + PRM_URL + "\"");
	}

	// A token carrying the exact operation scope dispatches.
	@Test void d02_stepUp_exactOperationScope_dispatches() throws Exception {
		clientD().post("/").contentString(body(1, "tools/call", callEcho()))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "echo")
			.header("Authorization", "Bearer opok")
			.run().assertStatus(200);
	}

	// A broader scope (tools) does NOT satisfy the exact required scope tools.exec: OAuth scopes have no universal
	// hierarchy, so this must be a scoped 403 step-up challenge, not a dispatch.
	@Test void d03_stepUp_broaderScopeDoesNotSatisfy_403() throws Exception {
		clientD().post("/").contentString(body(1, "tools/call", callEcho()))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "echo")
			.header("Authorization", "Bearer broad")
			.run()
			.assertStatus(403)
			.assertHeader("WWW-Authenticate").is("Bearer realm=\"mcp\", error=\"insufficient_scope\", scope=\"tools.exec\", resource_metadata=\"" + PRM_URL + "\"");
	}

	// An operation with no per-operation scope configured is unaffected: baseline mcp.read alone dispatches it.
	@Test void d04_stepUp_unconfiguredOperation_dispatches() throws Exception {
		clientD().post("/").contentString(body(1, "server/discover", null))
			.header("Mcp-Method", "server/discover").header("Mcp-Name", "")
			.header("Authorization", "Bearer good")
			.run().assertStatus(200);
	}

	// The baseline @RestStartCall bearer gate still fires first: a token-less step-up-gated call is a clean 401.
	@Test void d05_stepUp_missingToken_401BeforeDispatch() throws Exception {
		clientD().post("/").contentString(body(1, "tools/call", callEcho()))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "echo")
			.run()
			.assertStatus(401)
			.assertHeader("WWW-Authenticate").is("Bearer realm=\"mcp\", scope=\"mcp.read\", resource_metadata=\"" + PRM_URL + "\"");
	}

	// ---------------------------------------------------------------------------------------------
	// M6: the per-operation resolver seam sees the RAW JSON-RPC params (for tools/call that is
	// {name:..., arguments:{...}}), NOT the unwrapped tool arguments.  A resolver capturing the context proves
	// exactly what McpRevision hands a future dynamic resolver.
	// ---------------------------------------------------------------------------------------------

	static final AtomicReference<McpOperationContext> CAPTURED = new AtomicReference<>();

	private static McpOptions rsEnabledWithCapturingResolver() {
		CAPTURED.set(null);
		return new McpOptions().resourceServer(rs -> rs
			.setEnabled(true)
			.setResource(URI.create(RESOURCE))
			.setTokenValidator(VALIDATOR)
			.addAuthorizationServer(URI.create("http://as.example.com"))
			.addRequiredScope("mcp.read")
			.setOperationScopeResolver(ctx -> { CAPTURED.set(ctx); return Set.of(); }));
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class E extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().setName("test").setVersion("1.0.0").addTool(echo());
		}
		@Override protected McpOptions createMcpOptions() { return rsEnabledWithCapturingResolver(); }
	}

	private MockRestClient clientE() {
		return MockRestClient.create(E.class).json().contentType("application/json").accept("application/json").ignoreErrors().build();
	}

	@Test void e01_resolverSeesRawJsonRpcParamsForToolsCall() throws Exception {
		clientE().post("/").contentString(body(1, "tools/call", callEcho()))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "echo")
			.header("Authorization", "Bearer good")
			.run().assertStatus(200);
		var ctx = CAPTURED.get();
		assertNotNull(ctx);
		assertEquals("tools/call", ctx.method());
		assertEquals("echo", ctx.name());
		// The raw JSON-RPC params for tools/call: {name:echo, arguments:{text:hi}} — NOT {text:hi}.
		assertEquals("echo", ctx.params().get("name"));
		assertEquals(Map.of("text", "hi"), ctx.params().get("arguments"));
		assertNull(ctx.params().get("text"));  // the footgun: reading an argument off params directly is null.
	}

	// ---------------------------------------------------------------------------------------------
	// F4 (READY-312f): the F2-authenticated principal is threaded into the RequestStateCodec seal/unseal seam so
	// a hardened codec can bind the requestState to who requested it.  A capturing codec records the principal it
	// receives; an end-to-end pause (seal) then resume (unseal), both under the same bearer token, proves the
	// authenticated principal reaches the codec at BOTH points.
	// ---------------------------------------------------------------------------------------------

	static final class J_CapturingCodec implements RequestStateCodec {
		final RequestStateCodec delegate = new AeadRequestStateCodec();
		final AtomicReference<String> sealPrincipal = new AtomicReference<>("<unset>");
		final AtomicReference<String> unsealPrincipal = new AtomicReference<>("<unset>");

		@Override public String seal(McpRequestState state, String aad, Principal principal) {
			sealPrincipal.set(principal == null ? null : principal.getName());
			return delegate.seal(state, aad, principal);
		}

		@Override public Optional<McpRequestState> unseal(String token, String aad, Principal principal) {
			unsealPrincipal.set(principal == null ? null : principal.getName());
			return delegate.unseal(token, aad, principal);
		}
	}

	static final J_CapturingCodec J_CODEC = new J_CapturingCodec();

	private static Object validMetaElicit() {
		return JsonMap.of(
			RequestMeta.KEY_PROTOCOL_VERSION, "2026-07-28",
			RequestMeta.KEY_CLIENT_INFO, JsonMap.of("name", "fixture-client", "version", "1.0"),
			RequestMeta.KEY_CLIENT_CAPABILITIES, JsonMap.of("elicitation", JsonMap.of()));
	}

	private static String bodyElicit(Object id, String method, Object params) {
		var p = params instanceof Map<?,?> m ? new JsonMap(m) : new JsonMap();
		p.put("_meta", validMetaElicit());
		return org.apache.juneau.marshall.marshaller.Json.of(new JsonRpcRequest().setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(id).setMethod(method).setParams(p));
	}

	// Pauses on the first call (emits input_required -> seals) and completes on resume (unseals first, then returns).
	private static McpToolHandler pausingAsk() {
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() { return new McpToolSpec().setName("ask"); }
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) {
				if (ctx.getBean(McpMrtrResumeContext.class).isPresent())
					return McpToolOutcome.text("done");
				throw new McpInputRequiredSignal(Map.of("q1", Map.of("type", "elicitation")), "cont-1");
			}
		};
	}

	private static McpOptions rsEnabledWithCapturingCodec() {
		return rsEnabled().mrtr(m -> m.setCodec(J_CODEC));
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class J extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().setName("test").setVersion("1.0.0").addTool(pausingAsk());
		}
		@Override protected McpOptions createMcpOptions() { return rsEnabledWithCapturingCodec(); }
	}

	private MockRestClient clientJ() {
		return MockRestClient.create(J.class).json().contentType("application/json").accept("application/json").ignoreErrors().build();
	}

	@Test void j01_authenticatedPrincipalReachesCodecAtSealAndUnseal() throws Exception {
		J_CODEC.sealPrincipal.set("<unset>");
		J_CODEC.unsealPrincipal.set("<unset>");

		// Round 1: pause -> seal.  The bearer 'good' authenticates as principal 'alice' (see VALIDATOR).
		var pauseJson = clientJ().post("/").contentString(bodyElicit(1, "tools/call", JsonMap.of("name", "ask")))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "ask")
			.header("Authorization", "Bearer good")
			.run().assertStatus(200).getContent().asString();
		var token = org.apache.juneau.marshall.marshaller.Json.to(pauseJson, JsonMap.class).getMap("result").getString("requestState");
		assertNotNull(token);
		assertEquals("alice", J_CODEC.sealPrincipal.get());  // the authenticated principal reached seal

		// Round 2: resume -> unseal, same bearer token / same principal.
		var resumeParams = JsonMap.of("name", "ask", "requestState", token, "inputResponses", JsonMap.of("q1", "answer"));
		clientJ().post("/").contentString(bodyElicit(2, "tools/call", resumeParams))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "ask")
			.header("Authorization", "Bearer good")
			.run().assertStatus(200);
		assertEquals("alice", J_CODEC.unsealPrincipal.get());  // the authenticated principal reached unseal
	}

	// A requestState sealed under one authenticated identity (alice, bearer 'good') cannot be resumed under
	// a DIFFERENT authenticated identity (bob, bearer 'good2'); the principal is folded into the AAD, so unseal fails
	// GCM tag verification and surfaces as a JSON-RPC -32602 invalid-params error rather than re-invoking the handler.
	@Test void j02_crossIdentityResumeIsRejected() throws Exception {
		// Round 1: pause -> seal under alice.
		var pauseJson = clientJ().post("/").contentString(bodyElicit(1, "tools/call", JsonMap.of("name", "ask")))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "ask")
			.header("Authorization", "Bearer good")
			.run().assertStatus(200).getContent().asString();
		var token = org.apache.juneau.marshall.marshaller.Json.to(pauseJson, JsonMap.class).getMap("result").getString("requestState");
		assertNotNull(token);

		// Round 2: resume the SAME token under a different bearer -> different principal (bob) -> unseal rejects it.
		var resumeParams = JsonMap.of("name", "ask", "requestState", token, "inputResponses", JsonMap.of("q1", "answer"));
		var resumeJson = clientJ().post("/").contentString(bodyElicit(2, "tools/call", resumeParams))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "ask")
			.header("Authorization", "Bearer good2")
			.run().assertStatus(200).getContent().asString();
		var resp = org.apache.juneau.marshall.marshaller.Json.to(resumeJson, JsonMap.class);
		assertNull(resp.get("result"), "a cross-identity resume must not produce a successful result");
		assertEquals(-32602, resp.getMap("error").getInt("code"), "a cross-identity resume must be rejected as invalid params");
	}

	// End-to-end proof of the PRIMARY iss|sub claim-bound path (not the getName() fallback j01/j02
	// exercise). 'good3' (iss-a|alice) and 'good4' (iss-b|alice) are real ClaimsPrincipal fixtures carrying iss/sub
	// claims: same subject, different issuer. A requestState sealed under good3 must not be resumable under good4
	// (cross-IdP), proving the codec actually reads and binds iss|sub over real HTTP dispatch, and the SAME token
	// still round-trips when resumed under the original issuer/subject.
	@Test void j03_crossIssuerResumeIsRejected() throws Exception {
		// Round 1: pause -> seal under good3 (iss-a|alice).
		var pauseJson = clientJ().post("/").contentString(bodyElicit(1, "tools/call", JsonMap.of("name", "ask")))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "ask")
			.header("Authorization", "Bearer good3")
			.run().assertStatus(200).getContent().asString();
		var token = org.apache.juneau.marshall.marshaller.Json.to(pauseJson, JsonMap.class).getMap("result").getString("requestState");
		assertNotNull(token);

		// Round 2: resume the SAME token under good4 -> same sub (alice) but a DIFFERENT iss (idp-b) -> a different
		// bound identity -> unseal rejects it (GCM tag mismatch surfaced as -32602 invalid params).
		var resumeParams = JsonMap.of("name", "ask", "requestState", token, "inputResponses", JsonMap.of("q1", "answer"));
		var resumeJson = clientJ().post("/").contentString(bodyElicit(2, "tools/call", resumeParams))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "ask")
			.header("Authorization", "Bearer good4")
			.run().assertStatus(200).getContent().asString();
		var resp = org.apache.juneau.marshall.marshaller.Json.to(resumeJson, JsonMap.class);
		assertNull(resp.get("result"), "a cross-issuer resume must not produce a successful result");
		assertEquals(-32602, resp.getMap("error").getInt("code"), "a cross-issuer resume must be rejected as invalid params");

		// Round 3: the SAME token resumed under the ORIGINAL issuer/subject (good3) still round-trips cleanly -
		// the rejection above is specific to the mismatched iss, not a side effect on the token itself.
		clientJ().post("/").contentString(bodyElicit(3, "tools/call", resumeParams))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "ask")
			.header("Authorization", "Bearer good3")
			.run().assertStatus(200);
	}

	// ---------------------------------------------------------------------------------------------
	// Servlet path with RS auth DISABLED (default) - behavior unchanged.
	// ---------------------------------------------------------------------------------------------

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class B extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().setName("test").setVersion("1.0.0").addTool(echo());
		}
	}

	private MockRestClient clientB() {
		return MockRestClient.create(B.class).json().contentType("application/json").accept("application/json").ignoreErrors().build();
	}

	@Test void b01_authDisabled_postSucceedsWithoutToken() throws Exception {
		clientB().post("/").contentString(body(1, "server/discover", null))
			.header("Mcp-Method", "server/discover").header("Mcp-Name", "")
			.run().assertStatus(200);
	}

	@Test void b02_authDisabled_wellKnownReturns404() throws Exception {
		clientB().get("/.well-known/oauth-protected-resource/mcp").run().assertStatus(404);
	}

	// ---------------------------------------------------------------------------------------------
	// Mixin path (POST /mcp) with RS auth ENABLED - parity.
	//
	// Root-mounted (no @Rest(path=...)) so the advertised origin-root PRM URL genuinely resolves; see the H1
	// origin-root-mount constraint enforced by McpResourceServerSupport.assertOriginRootMount and exercised by
	// fixture G below.
	// ---------------------------------------------------------------------------------------------

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class C extends BasicRestServlet implements McpEndpoint {
		private static final long serialVersionUID = 1L;
		@Override public McpServerConfig getMcpConfig() {
			return new McpServerConfig().addTool(echo());
		}
		@Override public McpOptions getMcpOptions() { return rsEnabled(); }
	}

	private MockRestClient clientC() {
		return MockRestClient.create(C.class).json().contentType("application/json").accept("application/json").ignoreErrors().build();
	}

	@Test void c01_mixinValidToken_dispatches() throws Exception {
		clientC().post("/mcp").contentString(body(1, "server/discover", null))
			.header("Mcp-Method", "server/discover").header("Mcp-Name", "")
			.header("Authorization", "Bearer good")
			.run().assertStatus(200);
	}

	@Test void c02_mixinMissingToken_401WithResourceMetadata() throws Exception {
		clientC().post("/mcp").contentString(body(1, "server/discover", null))
			.header("Mcp-Method", "server/discover").header("Mcp-Name", "")
			.run()
			.assertStatus(401)
			.assertHeader("WWW-Authenticate").is("Bearer realm=\"mcp\", scope=\"mcp.read\", resource_metadata=\"" + PRM_URL + "\"");
	}

	@Test void c03_mixinWellKnown_servesPrm() throws Exception {
		var json = clientC().get("/.well-known/oauth-protected-resource/mcp").run().assertStatus(200).getContent().asString();
		assertContains("\"resource\":\"" + RESOURCE + "\"", json);
	}

	// B1 - BLOCKER: the bearer gate must not be bypassable via a trailing slash on the resolved MCP path.
	// The router is trailing-slash tolerant, so POST /mcp/ still dispatches to the MCP JSON-RPC handler and MUST be
	// gated.  (Sent as an absolute URL because MockPathResolver trims trailing slashes off relative paths.)
	@Test void c04_mixinTrailingSlash_gated() throws Exception {
		clientC().post("http://localhost/mcp/").contentString(body(1, "server/discover", null))
			.header("Mcp-Method", "server/discover").header("Mcp-Name", "")
			.run()
			.assertStatus(401)
			.assertHeader("WWW-Authenticate").is("Bearer realm=\"mcp\", scope=\"mcp.read\", resource_metadata=\"" + PRM_URL + "\"");
	}

	// B1 - BLOCKER: the gate must not be bypassable via percent-encoding.  The router decodes %6D to 'm', so
	// POST /%6Dcp dispatches to the MCP handler and MUST be gated.
	@Test void c05_mixinPercentEncoded_gated() throws Exception {
		clientC().post("/%6Dcp").contentString(body(1, "server/discover", null))
			.header("Mcp-Method", "server/discover").header("Mcp-Name", "")
			.run()
			.assertStatus(401)
			.assertHeader("WWW-Authenticate").is("Bearer realm=\"mcp\", scope=\"mcp.read\", resource_metadata=\"" + PRM_URL + "\"");
	}

	// ---------------------------------------------------------------------------------------------
	// H3 - HIGH: method-override channels must not bypass the gate.  A host allowing ?method=POST lets
	// GET /?method=POST route to the MCP POST endpoint; the gate must see the RESOLVED method, not the raw GET.
	// ---------------------------------------------------------------------------------------------

	@Rest(allowedMethodParams = "*", allowedMethodHeaders = "*", serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().setName("test").setVersion("1.0.0").addTool(echo());
		}
		@Override protected McpOptions createMcpOptions() { return rsEnabled(); }
	}

	private MockRestClient clientF() {
		return MockRestClient.create(F.class).json().contentType("application/json").accept("application/json").ignoreErrors().build();
	}

	// No body: the gate must reject on the RESOLVED method before dispatch, so a token-less override request is a
	// clean 401 regardless of body (and the classic client refuses to attach a body to a GET anyway).
	@Test void f01_methodParamOverride_gated() throws Exception {
		clientF().get("/?method=POST")
			.run()
			.assertStatus(401)
			.assertHeader("WWW-Authenticate").is("Bearer realm=\"mcp\", scope=\"mcp.read\", resource_metadata=\"" + PRM_URL + "\"");
	}

	@Test void f02_methodHeaderOverride_gated() throws Exception {
		clientF().get("/")
			.header("X-Method", "POST")
			.run()
			.assertStatus(401)
			.assertHeader("WWW-Authenticate").is("Bearer realm=\"mcp\", scope=\"mcp.read\", resource_metadata=\"" + PRM_URL + "\"");
	}

	// ---------------------------------------------------------------------------------------------
	// H1 - HIGH: on a non-origin-root mount the advertised origin-root PRM URL cannot be served, so RS auth must
	// fail fast with a clear 500 rather than silently 404 the discovery document.
	// ---------------------------------------------------------------------------------------------

	@Rest(path = "/api", serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class G extends BasicRestServlet implements McpEndpoint {
		private static final long serialVersionUID = 1L;
		@Override public McpServerConfig getMcpConfig() {
			return new McpServerConfig().addTool(echo());
		}
		@Override public McpOptions getMcpOptions() { return rsEnabled(); }
	}

	private MockRestClient clientG() {
		return MockRestClient.create(G.class).json().contentType("application/json").accept("application/json").ignoreErrors().build();
	}

	@Test void g01_nonRootMount_mcpPostFailsFast() throws Exception {
		var s = clientG().post("/mcp").contentString(body(1, "server/discover", null))
			.header("Mcp-Method", "server/discover").header("Mcp-Name", "")
			.header("Authorization", "Bearer good")
			.run().assertStatus(500).getContent().asString();
		assertContains("origin-root", s);
	}

	@Test void g02_nonRootMount_wellKnownFailsFast() throws Exception {
		var s = clientG().get("/.well-known/oauth-protected-resource/mcp").run().assertStatus(500).getContent().asString();
		assertContains("origin-root", s);
	}

	// ---------------------------------------------------------------------------------------------
	// H - BLOCKER: @Mixin(path=...) re-mount must not bypass the bearer gate.
	//
	// The host is itself ROOT-mounted (no @Rest(path=...)) but re-mounts the MCP mixin under a host-chosen
	// prefix, so POST /api/mcp routes to the MCP JSON-RPC handler while contextPath/servletPath stay empty.  A
	// gate that reconstructs a hardcoded "/mcp" path constant sees ["api","mcp"] != ["mcp"], returns WITHOUT
	// authenticating, and serves full unauthenticated MCP dispatch.  The router-aligned gate must identify the
	// RESOLVED operation instead.
	//
	// H1 UNIFICATION: identifying the resolved operation is not enough on its own - the same re-mount that
	// defeats a hardcoded path also moves the well-known PRM route off the origin root (h03), so a 401 here would
	// advertise a resource_metadata URL that itself 500s.  The gate therefore also consults the resolved op's
	// mixin mount prefixes (the same signal assertWellKnownMountReachable uses) and fails fast with the SAME
	// origin-root 500 as the well-known route, instead of authenticating, so the two routes never disagree.
	// ---------------------------------------------------------------------------------------------

	// A plain class implementing McpEndpoint, used as a re-mountable @Mixin (models McpEndpointOptionsCache_Test's
	// mixin shape).
	public static class McpMixinFixture implements McpEndpoint {
		@Override public McpServerConfig getMcpConfig() { return new McpServerConfig().addTool(echo()); }
		@Override public McpOptions getMcpOptions() { return rsEnabled(); }
	}

	@Rest(mixinDefs = @Mixin(type = McpMixinFixture.class, path = "/api"), serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class H extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	private MockRestClient clientH() {
		return MockRestClient.create(H.class).json().contentType("application/json").accept("application/json").ignoreErrors().build();
	}

	// B1/H1 - BLOCKER repro, now unified with the well-known route: a @Mixin re-mount is not merely unauthenticated
	// (401) but UNSUPPORTED for MCP RS auth - the gate must fail fast with the same origin-root 500 diagnostic as
	// the well-known route (h03), because the resource_metadata URL a 401 would advertise is itself unreachable
	// under the re-mount (see h03). Was 401 (it authenticated) before the gate consulted the mixin mount prefixes.
	@Test void h01_mixinRemount_missingToken_gated() throws Exception {
		var s = clientH().post("/api/mcp").contentString(body(1, "server/discover", null))
			.header("Mcp-Method", "server/discover").header("Mcp-Name", "")
			.run()
			.assertStatus(500).getContent().asString();
		assertContains("origin-root", s);
	}

	// The origin-root mount check runs BEFORE authentication, so even a valid token cannot dispatch through a
	// re-mounted MCP RS-auth endpoint - it is simply unsupported and fails fast exactly like a token-less request
	// (h01) and the well-known route (h03). Was 200 (dispatched) before the gate consulted the mixin mount prefixes.
	@Test void h02_mixinRemount_validToken_stillFailsFast() throws Exception {
		var s = clientH().post("/api/mcp").contentString(body(1, "server/discover", null))
			.header("Mcp-Method", "server/discover").header("Mcp-Name", "")
			.header("Authorization", "Bearer good")
			.run()
			.assertStatus(500).getContent().asString();
		assertContains("origin-root", s);
	}

	// Secondary MEDIUM: under re-mount the well-known PRM route also moves to /api/.well-known/..., so the advertised
	// origin-root PRM URL is unreachable; discovery must fail fast rather than serve from an inconsistent location.
	@Test void h03_mixinRemount_wellKnownFailsFast() throws Exception {
		var s = clientH().get("/api/.well-known/oauth-protected-resource/mcp").run().assertStatus(500).getContent().asString();
		assertContains("origin-root", s);
	}

	// ---------------------------------------------------------------------------------------------
	// I - BLOCKER: @Mixin(paths={...}) multi-mount gates every mounted copy of the MCP endpoint.
	// A single hardcoded path constant can never cover more than one mount.
	// ---------------------------------------------------------------------------------------------

	@Rest(mixinDefs = @Mixin(type = McpMixinFixture.class, paths = {"/a", "/b"}), serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class I extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	private MockRestClient clientI() {
		return MockRestClient.create(I.class).json().contentType("application/json").accept("application/json").ignoreErrors().build();
	}

	// Same H1 unification as h01: a multi-mount @Mixin(paths=...) re-mount fails fast with the origin-root 500
	// diagnostic rather than authenticating. Was 401 before the gate consulted the mixin mount prefixes.
	@Test void i01_multiMount_pathA_gated() throws Exception {
		var s = clientI().post("/a/mcp").contentString(body(1, "server/discover", null))
			.header("Mcp-Method", "server/discover").header("Mcp-Name", "")
			.run()
			.assertStatus(500).getContent().asString();
		assertContains("origin-root", s);
	}

	@Test void i02_multiMount_pathB_gated() throws Exception {
		var s = clientI().post("/b/mcp").contentString(body(1, "server/discover", null))
			.header("Mcp-Method", "server/discover").header("Mcp-Name", "")
			.run()
			.assertStatus(500).getContent().asString();
		assertContains("origin-root", s);
	}
}
