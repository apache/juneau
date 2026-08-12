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

import org.apache.juneau.rest.server.mcp.McpServerConfig;
import org.junit.jupiter.api.Test;

/**
 * Direct coverage of {@link McpEndpointOptionsCache.IdentityKey}'s {@code equals()}/{@code hashCode()}/
 * {@code resolveOptions()} branches, including the referent-cleared cases that {@link McpEndpointOptionsCache}'s
 * own {@code resolve()} path (via {@code ConcurrentHashMap}) does not reliably exercise: two distinct keys
 * normally land in distinct buckets (their hash is the referent's identity hash), so the map's internal key
 * probing rarely invokes {@code equals()}, and real GC timing cannot deterministically clear a referent either.
 */
class McpEndpointOptionsCache_IdentityKey_Test {

	private static class Fixture implements McpEndpoint {
		@Override public McpServerConfig getMcpConfig() { return new McpServerConfig(); }
	}

	@Test void a01_equals_sameInstance_true() {
		var key = new McpEndpointOptionsCache.IdentityKey(new Fixture());
		assertEquals(key, key);
	}

	@SuppressWarnings({
		"unlikely-arg-type" // Intentionally comparing to a mismatched type to cover the equals() type-guard branch.
	})
	@Test void a02_equals_notAnIdentityKey_false() {
		var key = new McpEndpointOptionsCache.IdentityKey(new Fixture());
		// Deliberately calls key.equals(...) directly (not assertNotEquals(key, "not a key")) so
		// IdentityKey.equals() -- not String.equals() -- is the method actually under test.
		assertFalse(key.equals("not a key"));
	}

	@Test void a03_equals_differentReferents_false() {
		var key1 = new McpEndpointOptionsCache.IdentityKey(new Fixture());
		var key2 = new McpEndpointOptionsCache.IdentityKey(new Fixture());
		assertNotEquals(key1, key2);
	}

	@Test void a04_equals_sameReferentDistinctKeyInstances_true() {
		var endpoint = new Fixture();
		var key1 = new McpEndpointOptionsCache.IdentityKey(endpoint);
		var key2 = new McpEndpointOptionsCache.IdentityKey(endpoint);
		assertEquals(key1, key2);
	}

	@Test void a05_equals_thisReferentCleared_false() {
		var key1 = new McpEndpointOptionsCache.IdentityKey(new Fixture());
		var key2 = new McpEndpointOptionsCache.IdentityKey(new Fixture());
		key1.clear();
		assertNotEquals(key1, key2, "a cleared referent (mine == null) must never compare equal, even to itself's own bucket-mate");
	}

	@Test void a06_resolveOptions_liveReferent_invokesGetMcpOptions() {
		var endpoint = new Fixture();
		var key = new McpEndpointOptionsCache.IdentityKey(endpoint);
		assertNotNull(key.resolveOptions());
	}

	@Test void a07_resolveOptions_clearedReferent_returnsFreshDefaultOptions() {
		var key = new McpEndpointOptionsCache.IdentityKey(new Fixture());
		key.clear();
		var options = key.resolveOptions();
		assertNotNull(options, "a cleared referent must fall back to a fresh default McpOptions rather than NPE");
	}
}
