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
package org.apache.juneau.commons.logging;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link LogContext}.
 */
@SuppressWarnings({
	"java:S117", // Local variable name intentional for test readability.
	"resource" // Test-fixture AutoCloseables are managed by the test lifecycle, not real leaks (mixed-module resource analysis on test code).
})
class LogContext_Test extends TestBase {

	private static LogContext ctx() {
		return RichLogger.context();
	}

	//====================================================================================================
	// Single-key set / get / clear
	//====================================================================================================

	@Test void a01_singleKeySetGetClear() {
		var c = ctx();
		assertNull(c.get("k"));
		try (var s = c.with("k", "v")) {
			assertEquals("v", c.get("k"));
		}
		assertNull(c.get("k"));
	}

	//====================================================================================================
	// Nested scope on same key restores OUTER value (not a blanket clear)
	//====================================================================================================

	@Test void a02_nestedSameKeyRestoresOuter() {
		var c = ctx();
		try (var outer = c.with("k", "outer")) {
			assertEquals("outer", c.get("k"));
			try (var inner = c.with("k", "inner")) {
				assertEquals("inner", c.get("k"));
			}
			// A naive "always clear on close" would drop the key here — must restore the outer value instead.
			assertEquals("outer", c.get("k"));
		}
		assertNull(c.get("k"));
	}

	//====================================================================================================
	// Batch with(Map): all entries restored on one close, including a pre-existing overlapping key
	//====================================================================================================

	@Test void a03_batchRestoreIsComplete() {
		var c = ctx();
		try (var outer = c.with("k1", "outer")) {
			assertEquals("outer", c.get("k1"));
			try (var batch = c.with(new LinkedHashMap<>(Map.of("k1", "b1", "k2", "b2")))) {
				assertEquals("b1", c.get("k1"));
				assertEquals("b2", c.get("k2"));
			}
			// One close must restore BOTH keys: k1 to its overlapping outer value, k2 to absent.  A partial-apply or
			// wrong-order restore would leave one of these wrong.
			assertEquals("outer", c.get("k1"));
			assertNull(c.get("k2"));
		}
		assertNull(c.get("k1"));
	}

	//====================================================================================================
	// Cross-thread isolation
	//====================================================================================================

	@Test void a04_crossThreadIsolation() throws Exception {
		var c = ctx();
		var seen = new AtomicReference<Object>("sentinel");
		try (var s = c.with("k", "v")) {
			var t = new Thread(() -> seen.set(c.get("k")));
			t.start();
			t.join();
		}
		// The value set on this thread must be invisible on the other thread.
		assertNull(seen.get());
	}

	//====================================================================================================
	// Null policy
	//====================================================================================================

	@Test void a05_nullKeyRejected() {
		var c = ctx();
		assertThrows(IllegalArgumentException.class, () -> c.with(null, "v"));
		var m = new HashMap<String,Object>();
		m.put(null, "v");
		assertThrows(IllegalArgumentException.class, () -> c.with(m));
	}

	@Test void a06_nullValueRemovesForScopeAndSnapshotSurvives() {
		var c = ctx();
		try (var outer = c.with("k", "x")) {
			assertEquals("x", c.get("k"));
			try (var remove = c.with("k", null)) {
				assertNull(c.get("k"));
				// Must not store a null value — snapshot() uses Map.copyOf which throws on null values.
				assertDoesNotThrow(c::snapshot);
				assertFalse(c.snapshot().containsKey("k"));
			}
			assertEquals("x", c.get("k"));
		}
	}

	//====================================================================================================
	// Snapshot semantics
	//====================================================================================================

	@Test void a07_snapshotEmptyIsSharedSingleton() {
		var c = ctx();
		assertSame(Map.of(), c.snapshot());
	}

	@Test void a08_snapshotIsImmutableCopy() {
		var c = ctx();
		try (var s = c.with("k", "v")) {
			var snap = c.snapshot();
			assertEquals("v", snap.get("k"));
			assertThrows(UnsupportedOperationException.class, () -> snap.put("k2", "v2"));
		}
	}

	@Test void a09_doubleCloseIsIdempotent() {
		var c = ctx();
		try (var outer = c.with("k", "outer")) {
			var inner = c.with("k", "inner");
			inner.close();
			inner.close(); // second close must be a no-op, not re-restore over the outer value
			assertEquals("outer", c.get("k"));
		}
	}
}
