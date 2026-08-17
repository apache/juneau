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
import java.util.logging.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link LogRecordContext} (the primitive, exercised directly — not via {@link RichLogger#log}).
 */
@SuppressWarnings({
	"java:S117" // Local variable name intentional for test readability.
})
class LogRecordContext_Test extends TestBase {

	//====================================================================================================
	// Empty context inserts no side-table entry (perf-critical short-circuit; white-box)
	//====================================================================================================

	@Test void a01_emptyContextNoTableEntry() {
		var record = new java.util.logging.LogRecord(Level.INFO, "msg");
		var putBefore = LogRecordContext.putCount();
		LogRecordContext.attachIfAbsent(record);
		// No live context → must return before touching the synchronized table.
		assertEquals(putBefore, LogRecordContext.putCount());
		assertSame(Map.of(), LogRecordContext.of(record));
	}

	//====================================================================================================
	// Non-empty context attaches an immutable snapshot (not a live view)
	//====================================================================================================

	@Test void a02_nonEmptyContextAttachesSnapshot() {
		var c = RichLogger.context();
		var record = new java.util.logging.LogRecord(Level.INFO, "msg");
		try (var s = c.with("k", "v")) {
			LogRecordContext.attachIfAbsent(record);
			assertEquals("v", LogRecordContext.of(record).get("k"));
		}
		// After the scope closes, the already-attached snapshot is unchanged (point-in-time fact).
		assertEquals("v", LogRecordContext.of(record).get("k"));
	}

	//====================================================================================================
	// Two-arg pre-seed wins over a later one-arg call made with an empty live context
	//====================================================================================================

	@Test void a03_preseedWinsOverLaterEmptyAttach() {
		var record = new java.util.logging.LogRecord(Level.INFO, "msg");
		LogRecordContext.attachIfAbsent(record, Map.of("requestId", "abc"));
		// Live context is empty here — the one-arg call must not clobber the pre-seed.
		LogRecordContext.attachIfAbsent(record);
		assertEquals("abc", LogRecordContext.of(record).get("requestId"));
	}

	@Test void a04_twoArgEmptyMapSkipsTable() {
		var record = new java.util.logging.LogRecord(Level.INFO, "msg");
		var putBefore = LogRecordContext.putCount();
		LogRecordContext.attachIfAbsent(record, Map.of());
		assertEquals(putBefore, LogRecordContext.putCount());
		assertSame(Map.of(), LogRecordContext.of(record));
	}

	//====================================================================================================
	// Identity-key invariant: neither record type overrides equals/hashCode
	//====================================================================================================

	@Test void a05_identityKeyInvariant() {
		assertNoEqualsHashCode(java.util.logging.LogRecord.class);
		assertNoEqualsHashCode(org.apache.juneau.commons.logging.LogRecord.class);

		// Two content-identical commons records must be distinct keys (Object identity).
		var r1 = new org.apache.juneau.commons.logging.LogRecord("n", Level.INFO, "msg", null, null);
		var r2 = new org.apache.juneau.commons.logging.LogRecord("n", Level.INFO, "msg", null, null);
		assertNotEquals(r1, r2);
		LogRecordContext.attachIfAbsent(r1, Map.of("k", "1"));
		LogRecordContext.attachIfAbsent(r2, Map.of("k", "2"));
		assertEquals("1", LogRecordContext.of(r1).get("k"));
		assertEquals("2", LogRecordContext.of(r2).get("k"));
	}

	private static void assertNoEqualsHashCode(Class<?> c) {
		assertThrows(NoSuchMethodException.class, () -> c.getDeclaredMethod("equals", Object.class),
			c.getName() + " must not declare equals(Object) — WeakHashMap identity-key invariant.");
		assertThrows(NoSuchMethodException.class, () -> c.getDeclaredMethod("hashCode"),
			c.getName() + " must not declare hashCode() — WeakHashMap identity-key invariant.");
	}
}
