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
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the {@link McpProtectedResourceMetadata} record.
 *
 * @since 10.0.0
 */
class McpProtectedResourceMetadata_Test extends TestBase {

	@Test void a01_firstAuthorizationServerReturnsFirstEntry() {
		var prm = new McpProtectedResourceMetadata(
			URI.create("https://mcp.example.com"),
			List.of(URI.create("https://as1.example.com"), URI.create("https://as2.example.com")),
			Set.of("read"),
			Map.of());
		assertEquals(URI.create("https://as1.example.com"), prm.firstAuthorizationServer().orElseThrow());
	}

	@Test void a02_firstAuthorizationServerEmptyWhenNoneAdvertised() {
		var prm = new McpProtectedResourceMetadata(URI.create("https://mcp.example.com"), List.of(), Set.of(), Map.of());
		assertTrue(prm.firstAuthorizationServer().isEmpty());
	}

	@Test void a03_nullCollectionsBecomeEmpty() {
		var prm = new McpProtectedResourceMetadata(URI.create("https://mcp.example.com"), null, null, null);
		assertTrue(prm.authorizationServers().isEmpty());
		assertTrue(prm.scopesSupported().isEmpty());
		assertTrue(prm.extras().isEmpty());
	}

	@Test void a04_collectionsAreDefensivelyCopiedAndUnmodifiable() {
		var servers = new ArrayList<>(List.of(URI.create("https://as1.example.com")));
		var prm = new McpProtectedResourceMetadata(URI.create("https://mcp.example.com"), servers, Set.of(), Map.of());
		servers.add(URI.create("https://as2.example.com"));
		assertEquals(1, prm.authorizationServers().size());
		var authServers = prm.authorizationServers();
		var extra = URI.create("https://x");
		assertThrows(UnsupportedOperationException.class, () -> authServers.add(extra));
	}

	@Test void b01_nullResourceRejected() {
		List<URI> noServers = List.of();
		Set<String> noScopes = Set.of();
		Map<String,Object> noExtras = Map.of();
		assertThrows(NullPointerException.class,
			() -> new McpProtectedResourceMetadata(null, noServers, noScopes, noExtras));
	}
}
