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

import java.time.*;
import java.util.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

class JsonValueSafety_Test extends TestBase {
	private static Map<String,Object> nest(int depth) {
		Map<String,Object> a = new LinkedHashMap<>();
		for (var i = 1; i < depth; i++) {
			var b = new LinkedHashMap<String,Object>();
			b.put("child", a);
			a = b;
		}
		return a;
	}

	@Test void a01_depth64PassesAnd65Fails() {
		assertDoesNotThrow(() -> JsonValueSafety.check(nest(64), "Tool structuredContent"));
		assertThrowsWithMessage(IllegalArgumentException.class,
			"Tool structuredContent exceeds maximum nesting depth of 64",
			() -> JsonValueSafety.check(nest(65), "Tool structuredContent"));
	}

	@Test void a02_node10000PassesAnd10001Fails() {
		assertDoesNotThrow(() -> JsonValueSafety.check(Collections.nCopies(9_999, 1), "value"));
		assertThrowsWithMessage(IllegalArgumentException.class,
			"value exceeds maximum node count of 10000",
			() -> JsonValueSafety.check(Collections.nCopies(10_000, 1), "value"));
	}

	@Test void a03_arraysSharedAndCyclesTerminate() {
		var a = new ArrayList<Object>();
		a.add(a);
		var b = new Object[] { a, a, new int[] { 1, 2 } };
		assertTimeoutPreemptively(Duration.ofSeconds(1), () -> JsonValueSafety.check(b, "value"));
	}

	@Test void a04_expiredDeadlineFails() {
		assertThrowsWithMessage(IllegalArgumentException.class,
			"value traversal exceeded 100 ms",
			() -> JsonValueSafety.check(Map.of("x", 1), "value", System.nanoTime() - 1));
	}

	@Test void a05_nonJsonLeafAndMapKeyFail() {
		assertThrowsWithMessage(IllegalArgumentException.class,
			"value contains non-JSON value type java.lang.Object",
			() -> JsonValueSafety.check(new Object(), "value"));
		assertThrowsWithMessage(IllegalArgumentException.class,
			"value contains non-string JSON object key 1",
			() -> JsonValueSafety.check(Map.of(1, "x"), "value"));
	}

	@Test void a06_remainingNanos() {
		assertTrue(JsonValueSafety.remainingNanos(JsonValueSafety.deadlineNanos()) > 0);
		assertEquals(0, JsonValueSafety.remainingNanos(System.nanoTime() - 1));
	}
}
