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
import java.util.*;

import org.apache.juneau.marshall.marshaller.Json;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link McpProtectedResourceMetadata} serializes to the exact RFC 9728 snake-case wire field names the
 * client-side {@code McpProtectedResourceMetadataClient} parser reads.
 */
class McpProtectedResourceMetadata_Test {

	@Test void a01_snakeCaseWireFieldNames() {
		var m = new McpProtectedResourceMetadata()
			.setResource(URI.create("https://mcp.example.com/mcp"))
			.setAuthorizationServers(List.of(URI.create("https://as.example.com")))
			.setScopesSupported(new LinkedHashSet<>(List.of("mcp.read", "mcp.write")))
			.setBearerMethodsSupported(new LinkedHashSet<>(List.of("header")));
		var json = Json.of(m);
		assertTrue(json.contains("\"resource\":\"https://mcp.example.com/mcp\""), json);
		assertTrue(json.contains("\"authorization_servers\":[\"https://as.example.com\"]"), json);
		assertTrue(json.contains("\"mcp.read\""), json);
		assertTrue(json.contains("\"scopes_supported\":"), json);
		assertTrue(json.contains("\"bearer_methods_supported\":[\"header\"]"), json);
	}

	@Test void a02_camelCaseNamesNeverLeakToWire() {
		var m = new McpProtectedResourceMetadata()
			.setResource(URI.create("https://mcp.example.com"))
			.setAuthorizationServers(List.of(URI.create("https://as.example.com")))
			.setScopesSupported(new LinkedHashSet<>(List.of("mcp.read")))
			.setBearerMethodsSupported(new LinkedHashSet<>(List.of("header")));
		var json = Json.of(m);
		assertFalse(json.contains("authorizationServers"), json);
		assertFalse(json.contains("scopesSupported"), json);
		assertFalse(json.contains("bearerMethodsSupported"), json);
	}

	@Test void a03_unsetPropertiesOmitted() {
		var json = Json.of(new McpProtectedResourceMetadata().setResource(URI.create("https://mcp.example.com")));
		assertTrue(json.contains("\"resource\""), json);
		assertFalse(json.contains("authorization_servers"), json);
		assertFalse(json.contains("scopes_supported"), json);
		assertFalse(json.contains("bearer_methods_supported"), json);
	}
}
