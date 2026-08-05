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

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * SEP-2350 cross-role wire-agreement integration coverage: the exact {@code WWW-Authenticate: Bearer ...} step-up
 * challenge string that the {@code 2026-07-28} resource server emits from its POST-parse enforcement point (see
 * {@code McpResourceServerSupport.stepUpChallenge} / the {@code d01_stepUp_insufficientOperationScope_403} HTTP
 * round-trip in {@code McpResourceServerBinding_Test}) is parsed by this client's {@link WwwAuthenticateChallenge} and
 * flows correctly into {@link McpScopeAccumulator}, so both roles agree on the wire without a cross-module test harness.
 *
 * @since 10.0.0
 */
class McpStepUpWireAgreement_Test extends TestBase {

	/**
	 * The literal challenge the v2 server emits for a {@code tools/call}→{@code echo} operation requiring
	 * {@code tools.exec} (character-for-character identical to the string asserted server-side).
	 */
	private static final String SERVER_CHALLENGE =
		"Bearer realm=\"mcp\", error=\"insufficient_scope\", scope=\"tools.exec\", "
		+ "resource_metadata=\"http://localhost/.well-known/oauth-protected-resource/mcp\"";

	@Test void a01_clientParsesServerStepUpChallenge() {
		var c = WwwAuthenticateChallenge.parse(SERVER_CHALLENGE).orElseThrow();
		assertEquals("insufficient_scope", c.error().orElseThrow());
		assertEquals(Set.of("tools.exec"), c.scopes());
		assertEquals("http://localhost/.well-known/oauth-protected-resource/mcp", c.resourceMetadata().orElseThrow().toString());
	}

	@Test void a02_challengeScopesUnionOntoPriorGrant() {
		// The client re-authorizes with the union of {previously-requested} ∪ {challenge}, preserving the prior grant.
		var challenge = WwwAuthenticateChallenge.parse(SERVER_CHALLENGE).orElseThrow();
		var union = McpScopeAccumulator.union(List.of("mcp.read"), challenge.scopes());
		assertEquals(List.of("mcp.read", "tools.exec"), List.copyOf(union), "union preserves prior grant + adds challenged scope");
	}
}
