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

import java.net.*;
import java.security.*;
import java.util.*;

import org.apache.juneau.rest.server.auth.TokenValidator;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link McpResourceServerConfig} &mdash; defaults, fluent setters, validation, and audience
 * derivation.
 */
class McpResourceServerConfig_Test {

	private static final Principal ALICE = () -> "alice";
	private static final TokenValidator V_OK = token -> ALICE;

	@Test void a01_defaultsAreOffAndSafe() {
		var c = new McpResourceServerConfig();
		assertFalse(c.isEnabled());
		assertEquals("mcp", c.getRealm());
		assertNull(c.getResource());
		assertNull(c.getAudience());
		assertNull(c.getTokenValidator());
		assertTrue(c.getAuthorizationServers().isEmpty());
		assertTrue(c.getScopesSupported().isEmpty());
		assertTrue(c.getRequiredScopes().isEmpty());
		assertEquals(Set.of("header"), c.getBearerMethodsSupported());
		assertTrue(c.isRequireAudienceClaim());  // H2: fail-closed by default.
	}

	@Test void a05_requireAudienceClaimIsSettable() {
		var c = new McpResourceServerConfig();
		assertFalse(c.setRequireAudienceClaim(false).isRequireAudienceClaim());
		assertTrue(c.setRequireAudienceClaim(true).isRequireAudienceClaim());
	}

	@Test void a02_audienceDefaultsToResource() {
		var c = new McpResourceServerConfig().setResource(URI.create("https://mcp.example.com/mcp"));
		assertEquals("https://mcp.example.com/mcp", c.getAudience());
	}

	@Test void a03_explicitAudienceOverrides() {
		var c = new McpResourceServerConfig()
			.setResource(URI.create("https://mcp.example.com/mcp"))
			.setAudience("urn:mcp:aud");
		assertEquals("urn:mcp:aud", c.getAudience());
	}

	@Test void a04_requiredScopeAlsoAdvertisedAsSupported() {
		var c = new McpResourceServerConfig().addRequiredScope("mcp.read");
		assertTrue(c.getRequiredScopes().contains("mcp.read"));
		assertTrue(c.getScopesSupported().contains("mcp.read"));
	}

	@Test void b01_blankRealmRejected() {
		assertThrows(IllegalArgumentException.class, () -> new McpResourceServerConfig().setRealm("  "));
	}

	@Test void b02_nullAuthorizationServerRejected() {
		assertThrows(IllegalArgumentException.class, () -> new McpResourceServerConfig().addAuthorizationServer(null));
	}

	@Test void b03_blankScopeRejected() {
		assertThrows(IllegalArgumentException.class, () -> new McpResourceServerConfig().addScopeSupported(" "));
	}

	@Test void c01_validateDisabledIsNoOp() {
		assertDoesNotThrow(() -> new McpResourceServerConfig().validateEnabled());
	}

	@Test void c02_enabledWithoutResourceThrows() {
		var c = new McpResourceServerConfig().setEnabled(true).setTokenValidator(V_OK);
		assertThrows(IllegalStateException.class, c::validateEnabled);
	}

	@Test void c03_enabledWithoutValidatorThrows() {
		var c = new McpResourceServerConfig().setEnabled(true).setResource(URI.create("https://mcp.example.com/mcp"));
		assertThrows(IllegalStateException.class, c::validateEnabled);
	}

	@Test void c04_enabledWithRelativeResourceThrows() {
		var c = new McpResourceServerConfig().setEnabled(true).setResource(URI.create("/mcp")).setTokenValidator(V_OK);
		assertThrows(IllegalStateException.class, c::validateEnabled);
	}

	@Test void c05_enabledAndValidPasses() {
		var c = new McpResourceServerConfig()
			.setEnabled(true)
			.setResource(URI.create("https://mcp.example.com/mcp"))
			.setTokenValidator(V_OK);
		assertDoesNotThrow(c::validateEnabled);
	}

	// ---------------------------------------------------------------------------------------------
	// SEP-2350 per-operation scope map + resolver seam.
	// ---------------------------------------------------------------------------------------------

	private static McpOperationContext op(String method, String name) {
		return new McpOperationContext(method, name, Map.of());
	}

	@Test void d01_operationScope_nameSpecific() {
		var c = new McpResourceServerConfig().addOperationScope("tools/call", "delete_repo", "repo.delete");
		assertEquals(Set.of("repo.delete"), c.requiredScopesFor(op("tools/call", "delete_repo")));
	}

	@Test void d02_operationScope_alsoAdvertisedAsSupported() {
		var c = new McpResourceServerConfig().addOperationScope("tools/call", "delete_repo", "repo.delete", "repo.admin");
		assertTrue(c.getScopesSupported().containsAll(Set.of("repo.delete", "repo.admin")));
	}

	@Test void d03_operationScope_wildcardMethodWide() {
		// A null/blank name registers a method-wide entry matching every operation of that method.
		var c = new McpResourceServerConfig().addOperationScope("resources/read", null, "res.read");
		assertEquals(Set.of("res.read"), c.requiredScopesFor(op("resources/read", "file:///x")));
		assertEquals(Set.of("res.read"), c.requiredScopesFor(op("resources/read", null)));
	}

	@Test void d04_operationScope_nameSpecificWinsOverWildcard() {
		var c = new McpResourceServerConfig()
			.addOperationScope("tools/call", null, "tools.base")
			.addOperationScope("tools/call", "delete_repo", "repo.delete");
		assertEquals(Set.of("repo.delete"), c.requiredScopesFor(op("tools/call", "delete_repo")));
		assertEquals(Set.of("tools.base"), c.requiredScopesFor(op("tools/call", "other_tool")));
	}

	@Test void d05_operationScope_noEntryIsEmpty() {
		var c = new McpResourceServerConfig().addOperationScope("tools/call", "delete_repo", "repo.delete");
		assertTrue(c.requiredScopesFor(op("prompts/get", "greeting")).isEmpty());
	}

	@Test void d06_resolverOverridesStaticMap() {
		var c = new McpResourceServerConfig()
			.addOperationScope("tools/call", "delete_repo", "repo.delete")
			.setOperationScopeResolver(ctx -> Set.of("dynamic." + ctx.name()));
		assertEquals(Set.of("dynamic.delete_repo"), c.requiredScopesFor(op("tools/call", "delete_repo")));
	}

	@Test void d07_resolverReturningNullIsEmpty() {
		var c = new McpResourceServerConfig().setOperationScopeResolver(ctx -> null);
		assertTrue(c.requiredScopesFor(op("tools/call", "x")).isEmpty());
	}

	@Test void d08_addOperationScope_validation() {
		var c = new McpResourceServerConfig();
		assertThrows(IllegalArgumentException.class, () -> c.addOperationScope(" ", "n", "s"));
		assertThrows(IllegalArgumentException.class, () -> c.addOperationScope("tools/call", "n"));
		assertThrows(IllegalArgumentException.class, () -> c.addOperationScope("tools/call", "n", "ok", " "));
	}

	@Test void d09_requiredScopesForNullCtxRejected() {
		assertThrows(IllegalArgumentException.class, () -> new McpResourceServerConfig().requiredScopesFor(null));
	}

	// LOW: addOperationScope validates the WHOLE scopes array before mutating scopesSupported, so a throw on a later
	// blank element does not leave earlier scopes half-advertised.
	@Test void d10_addOperationScope_throwOnLaterBlankLeavesNothingAdvertised() {
		var c = new McpResourceServerConfig();
		assertThrows(IllegalArgumentException.class, () -> c.addOperationScope("tools/call", "n", "repo.read", " "));
		assertFalse(c.getScopesSupported().contains("repo.read"), "earlier scope must not be advertised after a throw");
		assertTrue(c.requiredScopesFor(op("tools/call", "n")).isEmpty(), "no operation entry must be recorded after a throw");
	}
}
