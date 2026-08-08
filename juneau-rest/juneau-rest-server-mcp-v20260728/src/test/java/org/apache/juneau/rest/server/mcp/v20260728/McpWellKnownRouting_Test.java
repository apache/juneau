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

import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link McpWellKnownRouting} &mdash; RFC 9728 / SEP-2351 well-known path-insertion and the ordered
 * root fallback.
 */
class McpWellKnownRouting_Test {

	@Test void a01_pathInsertion() {
		assertEquals(URI.create("https://host/.well-known/oauth-protected-resource/mcp"),
			McpWellKnownRouting.metadataUri(URI.create("https://host/mcp")));
	}

	@Test void a02_rootResourceHasNoPathSuffix() {
		assertEquals(URI.create("https://host/.well-known/oauth-protected-resource"),
			McpWellKnownRouting.metadataUri(URI.create("https://host")));
	}

	@Test void a03_trailingSlashTreatedAsRoot() {
		assertEquals(URI.create("https://host/.well-known/oauth-protected-resource"),
			McpWellKnownRouting.metadataUri(URI.create("https://host/")));
	}

	@Test void a04_multiSegmentPathPreserved() {
		assertEquals(URI.create("https://host/.well-known/oauth-protected-resource/a/b"),
			McpWellKnownRouting.metadataUri(URI.create("https://host/a/b")));
	}

	@Test void a05_trailingSlashStrippedFromPath() {
		assertEquals(URI.create("https://host/.well-known/oauth-protected-resource/mcp"),
			McpWellKnownRouting.metadataUri(URI.create("https://host/mcp/")));
	}

	@Test void a06_portPreservedInAuthority() {
		assertEquals(URI.create("https://host:8443/.well-known/oauth-protected-resource/mcp"),
			McpWellKnownRouting.metadataUri(URI.create("https://host:8443/mcp")));
	}

	@Test void b01_rootMetadataUriAlwaysDropsPath() {
		assertEquals(URI.create("https://host/.well-known/oauth-protected-resource"),
			McpWellKnownRouting.rootMetadataUri(URI.create("https://host/mcp")));
	}

	@Test void c01_candidatesOrderedPathInsertedThenRoot() {
		var c = McpWellKnownRouting.candidates(URI.create("https://host/mcp"));
		assertEquals(List.of(
			URI.create("https://host/.well-known/oauth-protected-resource/mcp"),
			URI.create("https://host/.well-known/oauth-protected-resource")), c);
	}

	@Test void c02_candidatesCollapseForRootResource() {
		var c = McpWellKnownRouting.candidates(URI.create("https://host"));
		assertEquals(List.of(URI.create("https://host/.well-known/oauth-protected-resource")), c);
	}

	@Test void d01_wellKnownRequestPath() {
		assertEquals("/.well-known/oauth-protected-resource/mcp",
			McpWellKnownRouting.wellKnownRequestPath(URI.create("https://host/mcp")));
		assertEquals("/.well-known/oauth-protected-resource",
			McpWellKnownRouting.wellKnownRequestPath(URI.create("https://host")));
	}

	@Test void e01_nullResourceRejected() {
		assertThrows(IllegalArgumentException.class, () -> McpWellKnownRouting.metadataUri(null));
	}

	@Test void e02_nonAbsoluteResourceRejected() {
		var relative = URI.create("/mcp");
		assertThrows(IllegalArgumentException.class, () -> McpWellKnownRouting.metadataUri(relative));
	}
}
