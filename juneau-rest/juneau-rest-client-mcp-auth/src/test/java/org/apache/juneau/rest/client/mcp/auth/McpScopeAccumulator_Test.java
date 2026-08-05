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
 * Tests for {@link McpScopeAccumulator} (SEP-2350 order-preserving, exact-string-dedup scope union).
 *
 * @since 10.0.0
 */
class McpScopeAccumulator_Test extends TestBase {

	private static final URI RES = URI.create("https://mcp.example.com");
	private static final URI ISS = URI.create("https://as.example.com");

	@Test void a01_mandatedMinimumUnion() {
		var u = McpScopeAccumulator.union(List.of("read"), List.of("write"));
		assertEquals(List.of("read", "write"), List.copyOf(u));
	}

	@Test void a02_orderPreservingDedup() {
		var u = McpScopeAccumulator.union(List.of("a", "b"), List.of("b", "c", "a"));
		assertEquals(List.of("a", "b", "c"), List.copyOf(u), "order preserved, exact-string dedup");
	}

	@Test void a03_noHierarchyDedup() {
		var u = McpScopeAccumulator.union(List.of("repo"), List.of("repo:read"));
		assertEquals(List.of("repo", "repo:read"), List.copyOf(u), "opaque strings: both retained, no hierarchy dedup");
	}

	@Test void a04_nullsTreatedAsEmpty() {
		assertEquals(List.of("x"), List.copyOf(McpScopeAccumulator.union(null, List.of("x"))));
		assertEquals(List.of("y"), List.copyOf(McpScopeAccumulator.union(List.of("y"), null)));
		assertTrue(McpScopeAccumulator.union(null, null).isEmpty());
	}

	@Test void b01_optInContributorsWidenUnion() {
		var u = McpScopeAccumulator.union(List.of("a"), List.of("b"), List.of("g"), List.of("p"), List.of("m"));
		assertEquals(List.of("a", "b", "g", "p", "m"), List.copyOf(u));
	}

	@Test void b02_optInContributorsDefaultOffViaMinimalOverload() {
		var u = McpScopeAccumulator.union(List.of("a"), List.of("b"));
		assertEquals(List.of("a", "b"), List.copyOf(u), "minimal overload must not add extra contributors");
	}

	@Test void c01_keyedAccumulationSeedAndGrow() {
		var acc = new McpScopeAccumulator();
		acc.seed(RES, ISS, List.of("read"));
		var afterFirst = acc.accumulate(RES, ISS, List.of("write"));
		assertEquals(List.of("read", "write"), List.copyOf(afterFirst));
		var afterSecond = acc.accumulate(RES, ISS, List.of("admin"));
		assertEquals(List.of("read", "write", "admin"), List.copyOf(afterSecond));
		assertEquals(List.of("read", "write", "admin"), List.copyOf(acc.current(RES, ISS)));
	}

	@Test void c02_keyedPerResourceAndIssuer() {
		var acc = new McpScopeAccumulator();
		acc.accumulate(RES, ISS, List.of("a"));
		var other = URI.create("https://other.example.com");
		acc.accumulate(other, ISS, List.of("b"));
		assertEquals(List.of("a"), List.copyOf(acc.current(RES, ISS)));
		assertEquals(List.of("b"), List.copyOf(acc.current(other, ISS)));
	}

	@Test void c03_currentReturnsCopy() {
		var acc = new McpScopeAccumulator();
		acc.accumulate(RES, ISS, List.of("a"));
		var snap = acc.current(RES, ISS);
		snap.add("mutated");
		assertEquals(List.of("a"), List.copyOf(acc.current(RES, ISS)), "returned set must be a defensive copy");
	}

	@Test void c04_unknownKeyIsEmpty() {
		assertTrue(new McpScopeAccumulator().current(RES, ISS).isEmpty());
	}

	// M3: seeding twice on a shared accumulator (or re-seeding after accumulation) must UNION, never replace, so no
	// previously-tracked scope is lost when Builder.build() re-seeds a shared accumulator.
	@Test void c05_seedTwiceUnionsRatherThanReplaces() {
		var acc = new McpScopeAccumulator();
		acc.seed(RES, ISS, List.of("read"));
		acc.seed(RES, ISS, List.of("write"));
		assertEquals(List.of("read", "write"), List.copyOf(acc.current(RES, ISS)), "second seed must union, not replace");
	}

	@Test void c06_seedAfterAccumulatePreservesAccumulated() {
		var acc = new McpScopeAccumulator();
		acc.seed(RES, ISS, List.of("read"));
		acc.accumulate(RES, ISS, List.of("write"));
		acc.seed(RES, ISS, List.of("admin"));  // a re-seed (e.g. a second authorizer build) must not drop "write".
		assertEquals(List.of("read", "write", "admin"), List.copyOf(acc.current(RES, ISS)));
	}
}
