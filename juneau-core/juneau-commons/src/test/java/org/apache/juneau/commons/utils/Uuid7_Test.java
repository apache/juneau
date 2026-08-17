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
package org.apache.juneau.commons.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link Uuid7}.
 */
@SuppressWarnings({
	"java:S117" // Local variable name intentional for test readability.
})
class Uuid7_Test extends TestBase {

	//====================================================================================================
	// RFC 9562 version + variant conformance
	//====================================================================================================

	@Test void a01_versionAndVariant() {
		for (var i = 0; i < 1000; i++) {
			var u = Uuid7.create();
			assertEquals(7, u.version(), "version nibble must be 7");
			assertEquals(2, u.variant(), "variant must be IETF (binary 10)");
		}
	}

	//====================================================================================================
	// Time-ordering: encoded timestamps are non-decreasing across successive calls
	//====================================================================================================

	@Test void a02_timestampsNonDecreasing() {
		var prev = Uuid7.timestamp(Uuid7.create());
		for (var i = 0; i < 1000; i++) {
			var t = Uuid7.timestamp(Uuid7.create());
			assertTrue(t >= prev, "timestamp must be non-decreasing");
			prev = t;
		}
	}

	@Test void a03_timestampTracksWallClock() {
		var before = System.currentTimeMillis();
		var t = Uuid7.timestamp(Uuid7.create());
		var after = System.currentTimeMillis();
		assertTrue(t >= before && t <= after, "encoded timestamp must be the current wall-clock ms");
	}

	//====================================================================================================
	// SecureRandom is reused per thread (no fresh instantiation per call)
	//====================================================================================================

	@Test void a04_secureRandomReusedPerThread() {
		var r1 = Uuid7.currentRandom();
		Uuid7.create();
		Uuid7.create();
		var r2 = Uuid7.currentRandom();
		assertSame(r1, r2, "the same SecureRandom must serve repeated calls on one thread");
	}

	@Test void a05_uniqueness() {
		var seen = new HashSet<UUID>();
		for (var i = 0; i < 10000; i++)
			assertTrue(seen.add(Uuid7.create()), "generated UUIDs must be unique");
	}
}
