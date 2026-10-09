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
package org.apache.juneau.rest.server.bus;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link BusPolicy}: default deny, topic patterns, E-57, {@code check(contract)} E-55, defaults (spec §11.7).
 */
class BusPolicy_Test extends TestBase {

	private static final UpstreamHandler NOOP = (s, t, p) -> {};

	//------------------------------------------------------------------------------------------------------------------
	// a) Default deny and pattern matching
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_emptyPolicyDeniesEverything() {
		var p = BusPolicy.create().build();
		assertTrue(p.downstreamRetain("ops.jobs").isEmpty());
		assertTrue(p.downstreamRetain("job:abc").isEmpty());
		assertTrue(p.downstreamRetain("cmd:jobs").isEmpty());
		assertFalse(p.allowsUpstream("ops.cancel-all"));
		assertTrue(p.downstreamRetain(null).isEmpty());
		assertFalse(p.allowsUpstream(null));
	}

	@Test void a02_exactAndKeyedPatterns() {
		var p = BusPolicy.create()
			.downstream("ops.jobs", true)
			.downstream("ops.alert:*", false)
			.downstream("job:*", true)
			.downstream("cmd:jobs", false)
			.build();
		assertEquals(Optional.of(true), p.downstreamRetain("ops.jobs"));
		assertEquals(Optional.of(false), p.downstreamRetain("ops.alert:east"));
		assertEquals(Optional.of(false), p.downstreamRetain("ops.alert:*"));
		assertTrue(p.downstreamRetain("ops.alert").isEmpty(), "a keyed pattern does not cover the bare family");
		assertEquals(Optional.of(true), p.downstreamRetain("job:9f00"));
		assertEquals(Optional.of(false), p.downstreamRetain("cmd:jobs"));
		assertTrue(p.downstreamRetain("cmd:other").isEmpty());
		assertTrue(p.downstreamRetain("ops.jobs:x").isEmpty(), "an exact topic does not cover its keys");
	}

	@Test void a03_upstreamIsExactOnly() {
		var p = BusPolicy.create().upstream("ops.cancel-all", NOOP).build();
		assertTrue(p.allowsUpstream("ops.cancel-all"));
		assertFalse(p.allowsUpstream("ops.cancel-all:x"));
		assertSame(NOOP, p.upstreamHandler("ops.cancel-all"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) E-57
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_badDownstreamPatterns() {
		for (var bad : List.of("ops", "Ops.jobs", "card:t1", "selection:t1", "cmd:*", "ops.jobs:*:*", "ops.jobs:", "a.b.c", "")) {
			var e = assertThrows(IllegalArgumentException.class, () -> BusPolicy.create().downstream(bad, true), bad);
			assertEquals("E-57: BusPolicy: '" + bad + "' is not a valid topic pattern", e.getMessage());
		}
		var e = assertThrows(IllegalArgumentException.class, () -> BusPolicy.create().downstream(null, true));
		assertEquals("E-57: BusPolicy: 'null' is not a valid topic pattern", e.getMessage());
	}

	@Test void b02_duplicateDownstream() {
		var b = BusPolicy.create().downstream("ops.jobs", true);
		var e = assertThrows(IllegalArgumentException.class, () -> b.downstream("ops.jobs", false));
		assertEquals("E-57: BusPolicy: 'ops.jobs' is declared twice", e.getMessage());
	}

	@Test void b03_upstreamWithoutHandler() {
		var e = assertThrows(IllegalArgumentException.class, () -> BusPolicy.create().upstream("ops.cancel-all", null));
		assertEquals("E-57: BusPolicy: upstream topic 'ops.cancel-all' has no handler", e.getMessage());
	}

	@Test void b04_badUpstreamTopic() {
		for (var bad : List.of("ops.x:*", "job:*", "cmd:jobs", "ops")) {
			var e = assertThrows(IllegalArgumentException.class, () -> BusPolicy.create().upstream(bad, NOOP), bad);
			assertEquals("E-57: BusPolicy: '" + bad + "' is not a valid topic pattern", e.getMessage());
		}
	}

	@Test void b05_nonPositiveLimits() {
		assertThrows(IllegalArgumentException.class, () -> BusPolicy.create().sessionGrace(Duration.ZERO));
		assertThrows(IllegalArgumentException.class, () -> BusPolicy.create().maxSessionAge(Duration.ofSeconds(-1)));
		assertThrows(IllegalArgumentException.class, () -> BusPolicy.create().heartbeat(null));
		assertThrows(IllegalArgumentException.class, () -> BusPolicy.create().maxSessions(0));
		assertThrows(IllegalArgumentException.class, () -> BusPolicy.create().maxQueuedFrames(0));
		assertThrows(IllegalArgumentException.class, () -> BusPolicy.create().maxFrameBytes(0));
		assertThrows(IllegalArgumentException.class, () -> BusPolicy.create().maxUpstreamPerSecond(0));
		assertThrows(IllegalArgumentException.class, () -> BusPolicy.create().allowedOrigins(" "));
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) Defaults
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_defaults() {
		var p = BusPolicy.create().build();
		assertEquals(Duration.ofSeconds(60), p.sessionGrace());
		assertEquals(Duration.ofHours(12), p.maxSessionAge());
		assertEquals(Duration.ofSeconds(15), p.heartbeat());
		assertEquals(256, p.maxSessions());
		assertEquals(1024, p.maxQueuedFrames());
		assertEquals(65536, p.maxFrameBytes());
		assertEquals(20, p.maxUpstreamPerSecond());
		assertNull(p.boundary());
		assertEquals(List.of(), p.allowedOrigins());
		assertTrue(p.authorizer().test(null, "ops.jobs"));
		assertEquals(Map.of(), p.sessionAttributes().apply(null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) check(contract): E-55
	//------------------------------------------------------------------------------------------------------------------

	private static JsonMap contract(Object... bridges) {
		return JsonMap.of("bridges", List.of(bridges));
	}

	@Test void d01_checkPassesWhenEveryRequestedTopicIsGranted() {
		var p = BusPolicy.create().downstream("ops.jobs", true).downstream("cmd:jobs", false)
			.upstream("ops.cancel-all", NOOP).build();
		assertDoesNotThrow(() -> p.check(contract(JsonMap.of("id", "ops", "transport", "websocket",
			"downstream", List.of("ops.jobs", "cmd:jobs"), "upstream", List.of("ops.cancel-all")))));
		assertDoesNotThrow(() -> p.check(JsonMap.of()), "a page with no bridges requests nothing");
	}

	@Test void d02_checkListsEveryDenial() {
		var p = BusPolicy.create().downstream("ops.jobs", true).build();
		var e = assertThrows(IllegalStateException.class, () -> p.check(contract(
			JsonMap.of("id", "ops", "transport", "websocket",
				"downstream", List.of("ops.jobs", "ops.secret"), "upstream", List.of("ops.cancel-all")))));
		assertEquals("E-55: BusPolicy denies downstream topic 'ops.secret' that bridge 'ops' requests\n"
			+ "E-55: BusPolicy denies upstream topic 'ops.cancel-all' that bridge 'ops' requests", e.getMessage());
	}

	@Test void d03_checkListsEveryDenialAcrossTopicsAndBridges() {
		var p = BusPolicy.create().downstream("ops.jobs", true).build();
		var e = assertThrows(IllegalStateException.class, () -> p.check(contract(
			JsonMap.of("id", "a", "transport", "sse", "downstream", List.of("ops.x", "ops.y")),
			JsonMap.of("id", "b", "transport", "sse", "downstream", List.of("ops.z")))));
		assertEquals("E-55: BusPolicy denies downstream topic 'ops.x' that bridge 'a' requests\n"
			+ "E-55: BusPolicy denies downstream topic 'ops.y' that bridge 'a' requests\n"
			+ "E-55: BusPolicy denies downstream topic 'ops.z' that bridge 'b' requests", e.getMessage());
	}

	//------------------------------------------------------------------------------------------------------------------
	// e) Malformed topics, builder null checks, keyed upstream
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_malformedTopicIsNeverCoveredByAWildcard() {
		var p = BusPolicy.create().downstream("ops.alert:*", false).downstream("job:*", true).build();
		for (var bad : List.of("ops.alert:a:b", "ops.alert:", "ops.alert:a b", "job:a:b", "OPS.alert:x", "ops.alert:" + "x".repeat(129)))
			assertTrue(p.downstreamRetain(bad).isEmpty(), bad);
		assertEquals(Optional.of(false), p.downstreamRetain("ops.alert:" + "x".repeat(128)));
	}

	@Test void e02_allowedOriginsRejectsNullAndBlank() {
		assertThrows(NullPointerException.class, () -> BusPolicy.create().allowedOrigins((String[]) null));
		assertThrows(IllegalArgumentException.class, () -> BusPolicy.create().allowedOrigins("https://a.example", null));
		assertThrows(IllegalArgumentException.class, () -> BusPolicy.create().allowedOrigins("https://a.example", ""));
		assertEquals(List.of("https://a.example"), BusPolicy.create().allowedOrigins("https://a.example").build().allowedOrigins());
	}

	@Test void e03_authorizerAndSessionAttributesRejectNull() {
		assertThrows(NullPointerException.class, () -> BusPolicy.create().authorizer(null));
		assertThrows(NullPointerException.class, () -> BusPolicy.create().sessionAttributes(null));
	}

	@Test void e04_keyedUpstreamIsExact() {
		var p = BusPolicy.create().upstream("ops.cancel:one", NOOP).build();
		assertTrue(p.allowsUpstream("ops.cancel:one"));
		assertFalse(p.allowsUpstream("ops.cancel"));
		assertFalse(p.allowsUpstream("ops.cancel:two"));
		assertSame(NOOP, p.upstreamHandler("ops.cancel:one"));
	}
}
