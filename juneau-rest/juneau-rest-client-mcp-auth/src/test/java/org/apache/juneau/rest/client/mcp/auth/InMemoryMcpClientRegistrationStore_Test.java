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
 * Tests for {@link InMemoryMcpClientRegistrationStore} (SEP-2352 issuer-keyed persistence).
 *
 * @since 10.0.0
 */
class InMemoryMcpClientRegistrationStore_Test extends TestBase {

	private static final URI A = URI.create("https://as-a.example.com");
	private static final URI B = URI.create("https://as-b.example.com");

	private static McpClientRegistration reg(URI issuer, String clientId, String secret) {
		return new McpClientRegistration(clientId, Optional.of(secret), Optional.empty(), Optional.empty(),
			Optional.empty(), issuer, List.of(URI.create("http://127.0.0.1/callback")), McpApplicationType.NATIVE, Map.of());
	}

	@Test void a01_putAndFind() {
		var s = new InMemoryMcpClientRegistrationStore();
		s.put(A, reg(A, "ca", "sa"));
		assertEquals("ca", s.find(A).orElseThrow().clientId());
	}

	@Test void a02_findUnknownIsEmpty() {
		var s = new InMemoryMcpClientRegistrationStore();
		assertTrue(s.find(A).isEmpty());
	}

	@Test void a03_remove() {
		var s = new InMemoryMcpClientRegistrationStore();
		s.put(A, reg(A, "ca", "sa"));
		s.remove(A);
		assertTrue(s.find(A).isEmpty());
	}

	@Test void a04_separateStatePerIssuer() {
		var s = new InMemoryMcpClientRegistrationStore();
		s.put(A, reg(A, "ca", "sa"));
		s.put(B, reg(B, "cb", "sb"));
		assertEquals("ca", s.find(A).orElseThrow().clientId());
		assertEquals("cb", s.find(B).orElseThrow().clientId());
		assertEquals(2, s.size());
	}

	@Test void a05_putReplaces() {
		var s = new InMemoryMcpClientRegistrationStore();
		s.put(A, reg(A, "ca", "sa"));
		s.put(A, reg(A, "ca2", "sa2"));
		assertEquals("ca2", s.find(A).orElseThrow().clientId());
		assertEquals(1, s.size());
	}

	@Test void b01_toStringDoesNotLeakSecrets() {
		var s = new InMemoryMcpClientRegistrationStore();
		s.put(A, reg(A, "ca", "sup3rsecret"));
		assertFalse(s.toString().contains("sup3rsecret"));
	}

	@Test void b02_nullArgsRejected() {
		var s = new InMemoryMcpClientRegistrationStore();
		assertThrows(IllegalArgumentException.class, () -> s.find(null));
		assertThrows(IllegalArgumentException.class, () -> s.put(null, reg(A, "c", "s")));
		assertThrows(IllegalArgumentException.class, () -> s.put(A, null));
		assertThrows(IllegalArgumentException.class, () -> s.remove(null));
	}
}
