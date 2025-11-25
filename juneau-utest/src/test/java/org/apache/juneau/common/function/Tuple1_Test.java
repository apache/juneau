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
package org.apache.juneau.common.function;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class Tuple1_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void a01_basic() {
		var x = Tuple1.of("foo");
		assertEquals("foo", x.getA());
	}

	@Test void a02_withNull() {
		var x = Tuple1.of((String)null);
		assertNull(x.getA());
	}

	@Test void a03_withInteger() {
		var x = Tuple1.of(42);
		assertEquals(42, x.getA());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Equality tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void a04_equality_sameValues() {
		var x1 = Tuple1.of("foo");
		var x2 = Tuple1.of("foo");
		assertEquals(x1, x2);
		assertEquals(x1.hashCode(), x2.hashCode());
	}

	@Test void a05_equality_differentValues() {
		var x1 = Tuple1.of("foo");
		var x2 = Tuple1.of("bar");
		assertNotEquals(x1, x2);
		// Hash codes may or may not be different, but values are different
	}

	@Test void a06_equality_withNull() {
		var x1 = Tuple1.of((String)null);
		var x2 = Tuple1.of((String)null);
		var x3 = Tuple1.of("foo");
		assertEquals(x1, x2);
		assertEquals(x1.hashCode(), x2.hashCode());
		assertNotEquals(x1, x3);
		assertNotEquals(x3, x1);
	}

	@Test void a07_equality_differentTypes() {
		var x1 = Tuple1.of("42");
		var x2 = Tuple1.of(42);
		assertNotEquals(x1, x2);
	}

	@Test void a08_equality_notTuple1() {
		var x1 = Tuple1.of("foo");
		assertNotEquals(x1, "foo");
		assertNotEquals(x1, null);
		assertNotEquals(x1, new Object());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Array support tests (as mentioned in Javadoc).
	//------------------------------------------------------------------------------------------------------------------
	@Test void a09_arrayAsKey() {
		Map<Tuple1<String[]>,Integer> map = new HashMap<>();
		var key1 = Tuple1.of(new String[]{"a", "b"});
		var key2 = Tuple1.of(new String[]{"a", "b"});
		var key3 = Tuple1.of(new String[]{"c", "d"});

		map.put(key1, 1);
		map.put(key2, 2);  // Should replace first entry
		map.put(key3, 3);

		assertEquals(2, map.size());
		assertEquals(2, map.get(key1));  // Should get the replaced value
		assertEquals(2, map.get(key2));  // Same key
		assertEquals(3, map.get(key3));
	}

	@Test void a10_arrayEquality() {
		var x1 = Tuple1.of(new String[]{"a", "b"});
		var x2 = Tuple1.of(new String[]{"a", "b"});
		var x3 = Tuple1.of(new String[]{"c", "d"});

		assertEquals(x1, x2);
		assertEquals(x1.hashCode(), x2.hashCode());
		assertNotEquals(x1, x3);
	}

	@Test void a11_arrayWithNullElements() {
		var x1 = Tuple1.of(new String[]{"a", null, "b"});
		var x2 = Tuple1.of(new String[]{"a", null, "b"});
		var x3 = Tuple1.of(new String[]{"a", "b"});

		assertEquals(x1, x2);
		assertEquals(x1.hashCode(), x2.hashCode());
		assertNotEquals(x1, x3);
	}

	@Test void a12_emptyArray() {
		var x1 = Tuple1.of(new String[0]);
		var x2 = Tuple1.of(new String[0]);
		var x3 = Tuple1.of(new String[]{"a"});

		assertEquals(x1, x2);
		assertEquals(x1.hashCode(), x2.hashCode());
		assertNotEquals(x1, x3);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Constructor vs static factory.
	//------------------------------------------------------------------------------------------------------------------
	@Test void a13_constructor() {
		var x1 = new Tuple1<>("foo");
		var x2 = Tuple1.of("foo");
		assertEquals(x1, x2);
		assertEquals(x1.hashCode(), x2.hashCode());
	}
}

