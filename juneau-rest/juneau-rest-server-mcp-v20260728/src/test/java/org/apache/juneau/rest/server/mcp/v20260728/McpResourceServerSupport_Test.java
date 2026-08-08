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

import java.lang.reflect.*;
import java.net.*;
import java.security.*;
import java.util.*;

import org.apache.juneau.bean.jsonrpc.JsonRpcRequest;
import org.apache.juneau.http.HttpHeader;
import org.apache.juneau.http.response.Forbidden;
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.rest.mock.MockServletRequest;
import org.apache.juneau.rest.server.RestRequest;
import org.apache.juneau.rest.server.RestServerConstants;
import org.apache.juneau.rest.server.auth.TokenValidator;
import org.apache.juneau.rest.server.mcp.McpEndpointMixin;
import org.apache.juneau.rest.server.util.UrlPath;
import org.apache.juneau.rest.server.util.UrlPathMatcher;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link McpResourceServerSupport}'s pure helpers &mdash; PRM construction and challenge string
 * shapes, plus the security-boundary gate helpers ({@code isEffectivePost}, the operation-identity decision, and the
 * router-matcher path decision).  The full HTTP bearer gate is covered end-to-end by {@code McpResourceServerBinding_Test}.
 */
class McpResourceServerSupport_Test {

	private static final Principal ALICE = () -> "alice";
	private static final TokenValidator V_OK = token -> ALICE;

	private static McpResourceServerConfig cfg() {
		return new McpResourceServerConfig()
			.setEnabled(true)
			.setResource(URI.create("https://mcp.example.com/mcp"))
			.setTokenValidator(V_OK)
			.addAuthorizationServer(URI.create("https://as.example.com"))
			.addRequiredScope("mcp.read");
	}

	@Test void a01_buildMetadataShape() {
		var json = Json.of(McpResourceServerSupport.buildMetadata(cfg()));
		assertTrue(json.contains("\"resource\":\"https://mcp.example.com/mcp\""), json);
		assertTrue(json.contains("\"authorization_servers\":[\"https://as.example.com\"]"), json);
		assertTrue(json.contains("\"scopes_supported\":[\"mcp.read\"]"), json);
		assertTrue(json.contains("\"bearer_methods_supported\":[\"header\"]"), json);
	}

	@Test void a02_metadataUriIsPathInserted() {
		assertEquals(URI.create("https://mcp.example.com/.well-known/oauth-protected-resource/mcp"),
			McpResourceServerSupport.metadataUri(cfg()));
	}

	@Test void b01_missingTokenChallengeHasNoErrorCodeButHintsScope() {
		// M1/SEP-2350: the initial 401 carries no error code, but DOES hint the baseline required scope so the client
		// can request it on its first authorization.
		var s = McpResourceServerSupport.missingTokenChallenge(cfg());
		assertEquals("Bearer realm=\"mcp\", scope=\"mcp.read\", resource_metadata=\"https://mcp.example.com/.well-known/oauth-protected-resource/mcp\"", s);
	}

	@Test void b01b_missingTokenChallengeOmitsScopeWhenNoBaseline() {
		// Guard against emitting scope="" when no baseline scope is configured.
		var c = new McpResourceServerConfig()
			.setEnabled(true)
			.setResource(URI.create("https://mcp.example.com/mcp"))
			.setTokenValidator(V_OK)
			.addAuthorizationServer(URI.create("https://as.example.com"));
		var s = McpResourceServerSupport.missingTokenChallenge(c);
		assertEquals("Bearer realm=\"mcp\", resource_metadata=\"https://mcp.example.com/.well-known/oauth-protected-resource/mcp\"", s);
		assertFalse(s.contains("scope="), s);
	}

	@Test void b02_invalidTokenChallenge() {
		var s = McpResourceServerSupport.invalidTokenChallenge(cfg());
		assertEquals("Bearer realm=\"mcp\", error=\"invalid_token\", resource_metadata=\"https://mcp.example.com/.well-known/oauth-protected-resource/mcp\"", s);
	}

	@Test void b03_insufficientScopeChallengeIncludesScope() {
		var s = McpResourceServerSupport.insufficientScopeChallenge(cfg());
		assertEquals("Bearer realm=\"mcp\", error=\"insufficient_scope\", scope=\"mcp.read\", resource_metadata=\"https://mcp.example.com/.well-known/oauth-protected-resource/mcp\"", s);
	}

	// ---------------------------------------------------------------------------------------------
	// isEffectivePost() - method-override channel handling (H3).  Mirrors RestSession.getMethod(): the gate must
	// treat any override to POST as a POST, and must read the FIRST value of a duplicated method parameter (as the
	// router does).
	// ---------------------------------------------------------------------------------------------

	@Test void c01_isEffectivePost_explicitPost() {
		assertTrue(McpResourceServerSupport.isEffectivePost(MockServletRequest.create().method("POST")));
	}

	@Test void c02_isEffectivePost_plainGet_false() {
		assertFalse(McpResourceServerSupport.isEffectivePost(MockServletRequest.create().method("GET")));
	}

	@Test void c03_isEffectivePost_methodParamOverride() {
		assertTrue(McpResourceServerSupport.isEffectivePost(MockServletRequest.create().method("GET").queryString("method=POST")));
	}

	@Test void c04_isEffectivePost_methodParamOverride_lowercase() {
		// The router compares case-insensitively; a lowercase ?method=post must still be treated as an override.
		assertTrue(McpResourceServerSupport.isEffectivePost(MockServletRequest.create().method("GET").queryString("method=post")));
	}

	@Test void c05_isEffectivePost_duplicateMethodParam_firstWins() {
		// Duplicated method params: the FIRST value is authoritative (exactly as RestSession.getMethod() reads it).
		assertTrue(McpResourceServerSupport.isEffectivePost(MockServletRequest.create().method("GET").queryString("method=POST&method=GET")));
		assertFalse(McpResourceServerSupport.isEffectivePost(MockServletRequest.create().method("GET").queryString("method=GET&method=POST")));
	}

	@Test void c06_isEffectivePost_methodHeaderOverride() {
		assertTrue(McpResourceServerSupport.isEffectivePost(MockServletRequest.create().method("GET").header("X-Method", "POST")));
	}

	// ---------------------------------------------------------------------------------------------
	// isMcpDispatchMethod() - the operation-identity gate decision (mixin sub-context case).  Gates iff the resolved
	// Java method IS McpEndpointMixin#handleMcpRequest (or an override), with zero path logic.
	// ---------------------------------------------------------------------------------------------

	private static Method mcpDispatchMethod() throws Exception {
		return McpEndpointMixin.class.getMethod("handleMcpRequest", JsonRpcRequest.class, RestRequest.class);
	}

	@Test void d01_isMcpDispatchMethod_theDispatchMethod_true() throws Exception {
		assertTrue(McpResourceServerSupport.isMcpDispatchMethod(mcpDispatchMethod()));
	}

	@Test void d02_isMcpDispatchMethod_unrelatedMethod_false() throws Exception {
		assertFalse(McpResourceServerSupport.isMcpDispatchMethod(Object.class.getMethod("toString")));
	}

	@Test void d03_isMcpDispatchMethod_nameCollisionOnUnrelatedType_false() throws Exception {
		// A method NAMED handleMcpRequest but declared on a type that is NOT an McpEndpointMixin must NOT be gated.
		assertFalse(McpResourceServerSupport.isMcpDispatchMethod(Decoy.class.getMethod("handleMcpRequest")));
	}

	@Test void d04_isMcpDispatchMethod_null_false() {
		assertFalse(McpResourceServerSupport.isMcpDispatchMethod(null));
	}

	public static class Decoy {
		public void handleMcpRequest() { /* name collision only; not an McpEndpointMixin */ }
	}

	// ---------------------------------------------------------------------------------------------
	// Router-matcher path decision (host implements-McpEndpoint case).  The gate reuses the router's OWN
	// UrlPathMatcher for the /mcp dispatch op, so it agrees with the router on every path edge case.  These pin the
	// matcher decisions the gate delegates to: trailing slash (router-tolerant) matches, a doubled-slash prefix does
	// not (the router 404s it too, so there is no gap to bypass).
	// ---------------------------------------------------------------------------------------------

	private static final UrlPathMatcher MCP_MATCHER = UrlPathMatcher.of("/mcp");

	@Test void e01_matcher_exactPath_matches() {
		assertNotNull(MCP_MATCHER.match(UrlPath.of("/mcp")));
	}

	@Test void e02_matcher_trailingSlash_matches() {
		// POST /mcp/ still routes to the MCP handler, so the reused matcher must match it too (B1).
		assertNotNull(MCP_MATCHER.match(UrlPath.of("/mcp/")));
	}

	@Test void e03_matcher_doubledSlashPrefix_agreesWithRouter() {
		// //mcp parses to parts ["","mcp"]; the router does not route it to /mcp, and neither does the reused
		// matcher — so gate and router agree (both 404), leaving no bypass gap.
		assertNull(MCP_MATCHER.match(UrlPath.of("//mcp")));
	}

	@Test void e04_matcher_innerDoubledSlash_agreesWithRouter() {
		// /mcp// parses to parts ["mcp","",""] (two trailing empties); the router does not route it to /mcp, and
		// neither does the reused matcher — so gate and router agree (both 404), leaving no bypass gap.
		assertNull(MCP_MATCHER.match(UrlPath.of("/mcp//")));
	}

	// ---------------------------------------------------------------------------------------------
	// SEP-2350 per-operation step-up: scope satisfaction (hierarchy-aware), scoped challenge shape, and the
	// POST-parse enforcement decision (throws 403 insufficient_scope + WWW-Authenticate, or is a no-op).
	// ---------------------------------------------------------------------------------------------

	private static McpOperationContext op(String method, String name) {
		return new McpOperationContext(method, name, Map.of());
	}

	@Test void f01_satisfies_exactMatch() {
		assertTrue(McpResourceServerSupport.satisfies(Set.of("repo.delete"), Set.of("repo.delete")));
	}

	@Test void f02_satisfies_broaderAncestorImpliesNarrower() {
		// Granted "repo" implies required "repo.delete" (dot) and "repo:delete" (colon).
		assertTrue(McpResourceServerSupport.satisfies(Set.of("repo"), Set.of("repo.delete")));
		assertTrue(McpResourceServerSupport.satisfies(Set.of("repo"), Set.of("repo:delete")));
	}

	@Test void f03_satisfies_narrowerDoesNotImplyBroader() {
		// Granted "repo.read" must NOT satisfy required "repo" (no privilege escalation).
		assertFalse(McpResourceServerSupport.satisfies(Set.of("repo.read"), Set.of("repo")));
	}

	@Test void f04_satisfies_partialTokenIsNotAncestor() {
		// "rep" is a string prefix of "repo.read" but NOT a hierarchical ancestor (no delimiter), so it must not satisfy.
		assertFalse(McpResourceServerSupport.satisfies(Set.of("rep"), Set.of("repo.read")));
	}

	@Test void f05_satisfies_allRequiredMustBeMet() {
		assertFalse(McpResourceServerSupport.satisfies(Set.of("a"), Set.of("a", "b")));
		assertTrue(McpResourceServerSupport.satisfies(Set.of("a", "b"), Set.of("a", "b")));
	}

	@Test void f05b_satisfies_baselineParity_grantedAncestorSatisfiesBaseline() {
		// H1 parity: the baseline required-scope gate uses satisfies(...) exactly like the per-operation gate, so a
		// token granted "mcp" satisfies a baseline of "mcp.read" hierarchically (no exact-string containsAll needed).
		assertTrue(McpResourceServerSupport.satisfies(Set.of("mcp"), Set.of("mcp.read")));
		assertFalse(Set.of("mcp").containsAll(Set.of("mcp.read")));  // the OLD containsAll gate would have 403'd this.
	}

	@Test void f06_stepUpChallengeShape() {
		var s = McpResourceServerSupport.stepUpChallenge(cfg(), List.of("repo.delete", "repo.admin"));
		assertEquals("Bearer realm=\"mcp\", error=\"insufficient_scope\", scope=\"repo.delete repo.admin\", "
			+ "resource_metadata=\"https://mcp.example.com/.well-known/oauth-protected-resource/mcp\"", s);
	}

	@Test void f07_enforce_disabledIsNoOp() {
		var c = new McpResourceServerConfig().addOperationScope("tools/call", "x", "repo.delete");  // enabled=false
		assertDoesNotThrow(() -> McpResourceServerSupport.enforceOperationScopes(c, Set.of(), op("tools/call", "x")));
	}

	@Test void f08_enforce_noOperationScopeIsNoOp() {
		assertDoesNotThrow(() -> McpResourceServerSupport.enforceOperationScopes(cfg(), Set.of(), op("tools/call", "unconfigured")));
	}

	@Test void f09_enforce_satisfiedIsNoOp() {
		var c = cfg().addOperationScope("tools/call", "delete_repo", "repo.delete");
		assertDoesNotThrow(() -> McpResourceServerSupport.enforceOperationScopes(c, Set.of("repo.delete"), op("tools/call", "delete_repo")));
	}

	@Test void f10_enforce_insufficientThrows403WithScopedChallenge() {
		var c = cfg().addOperationScope("tools/call", "delete_repo", "repo.delete");
		var grantedScopes = Set.of("mcp.read");
		var ctx = op("tools/call", "delete_repo");
		var e = assertThrows(Forbidden.class,
			() -> McpResourceServerSupport.enforceOperationScopes(c, grantedScopes, ctx));
		assertEquals(403, e.getStatusCode());
		var challenge = e.getHeaders().stream()
			.filter(h -> "WWW-Authenticate".equalsIgnoreCase(h.getName()))
			.map(HttpHeader::getValue)
			.findFirst().orElseThrow();
		assertTrue(challenge.contains("error=\"insufficient_scope\""), challenge);
		assertTrue(challenge.contains("scope=\"repo.delete\""), challenge);
	}

	@Test void f11_grantedScopes_absentAttributeIsEmpty() {
		assertTrue(McpResourceServerSupport.grantedScopes(MockServletRequest.create()).isEmpty());
		assertTrue(McpResourceServerSupport.grantedScopes(null).isEmpty());
	}

	// ---------------------------------------------------------------------------------------------
	// F4: principal(req) exposes the F2-authenticated principal (stashed under PRINCIPAL_ATTR by
	// authenticate(...)) so the dispatcher can thread it into the RequestStateCodec seal/unseal seam, enabling
	// principal-bound AAD.  Mirrors grantedScopes(req): present -> the principal; absent/null/wrong-type
	// -> null (the anonymous / RS-auth-disabled path).
	// ---------------------------------------------------------------------------------------------

	@Test void g01_principal_presentReturnsStashedPrincipal() {
		Principal p = () -> "bob";
		var req = MockServletRequest.create().attribute(RestServerConstants.PRINCIPAL_ATTR, p);
		assertSame(p, McpResourceServerSupport.principal(req));
	}

	@Test void g02_principal_absentAttributeIsNull() {
		assertNull(McpResourceServerSupport.principal(MockServletRequest.create()));
	}

	@Test void g03_principal_nullRequestIsNull() {
		assertNull(McpResourceServerSupport.principal(null));
	}

	@Test void g04_principal_nonPrincipalAttributeIsNull() {
		// Defensive: a stashed value that is not a Principal must not be cast/returned.
		var req = MockServletRequest.create().attribute(RestServerConstants.PRINCIPAL_ATTR, "not-a-principal");
		assertNull(McpResourceServerSupport.principal(req));
	}
}
