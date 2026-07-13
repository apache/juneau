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

import static org.apache.juneau.commons.utils.ObjectUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

@TestMethodOrder(MethodOrderer.MethodName.class)
class ObjectUtils_Test extends TestBase {

	//====================================================================================================
	// isNotNull / isNull
	//====================================================================================================
	@Test
	void a001_isNotNull() {
		assertTrue(isNotNull("x"));
		assertFalse(isNotNull(null));
	}

	@Test
	void a002_isNull_single() {
		assertTrue(isNull((Object)null));
		assertFalse(isNull("x"));
	}

	@Test
	void a003_isNull_varargs() {
		assertTrue(isNull((Object[])null));
		assertTrue(isNull(null, null, null));
		assertFalse(isNull(null, "x", null));
	}

	//====================================================================================================
	// nullObject
	//====================================================================================================
	@Test
	void a004_nullObject() {
		String s = nullObject(String.class);
		assertNull(s);
	}

	//====================================================================================================
	// equal / notEqual
	//====================================================================================================
	@Test
	void a010_equal_simple() {
		assertTrue(equal("a", "a"));
		assertFalse(equal("a", "b"));
		assertTrue(equal(null, null));
		assertFalse(equal("a", null));
	}

	@Test
	void a011_equal_caseInsensitive() {
		assertTrue(equal(true, "Hello", "HELLO"));
		assertFalse(equal(false, "Hello", "HELLO"));
		assertTrue(equal(false, "Hello", "Hello"));
	}

	@Test
	void a012_equal_biPredicate() {
		assertTrue(equal("Hello", "HELLO", String::equalsIgnoreCase));
		assertFalse(equal("Hello", "World", String::equalsIgnoreCase));
		assertTrue(equal(null, null, String::equalsIgnoreCase));
		assertFalse(equal("x", null, String::equalsIgnoreCase));
	}

	@Test
	void a013_equal_arrays() {
		assertTrue(equal(new int[]{1, 2}, new int[]{1, 2}));
		assertFalse(equal(new int[]{1, 2}, new int[]{1, 3}));
		assertFalse(equal(new int[]{1}, new int[]{1, 2}));
	}

	@Test
	void a014_equalsAny() {
		assertTrue(equalsAny("b", "a", "b", "c"));
		assertFalse(equalsAny("x", "a", "b", "c"));
		assertFalse(equalsAny("a", (String[])null));
		assertFalse(equalsAny("a"));
	}

	@Test
	void a015_equalIgnoreCase() {
		assertTrue(equalIgnoreCase("hello", "HELLO"));
		assertFalse(equalIgnoreCase("hello", "world"));
	}

	@Test
	void a016_notEqual() {
		assertTrue(notEqual("a", "b"));
		assertFalse(notEqual("a", "a"));
	}

	@Test
	void a017_notEqual_biPredicate() {
		assertFalse(notEqual("Hello", "HELLO", String::equalsIgnoreCase));
		assertTrue(notEqual("Hello", "World", String::equalsIgnoreCase));
	}

	//====================================================================================================
	// compare / lessThan / greaterThan / min / max
	//====================================================================================================
	@Test
	void a020_compare() {
		assertEquals(-1, compare(null, "x"));
		assertEquals(0, compare(null, null));
		assertEquals(1, compare("x", null));
		assertEquals(0, compare("a", "a"));
		assertTrue(compare("a", "b") < 0);
	}

	@Test
	void a021_lessThan() {
		assertTrue(lessThan(1, 2));
		assertFalse(lessThan(2, 1));
		assertFalse(lessThan(1, 1));
		assertTrue(lessThan(null, "x"));
		assertFalse(lessThan("x", null));
	}

	@Test
	void a022_lessThanOrEqual() {
		assertTrue(lessThanOrEqual(1, 1));
		assertTrue(lessThanOrEqual(1, 2));
		assertFalse(lessThanOrEqual(2, 1));
		assertTrue(lessThanOrEqual(null, "x"));
		assertTrue(lessThanOrEqual(null, null));
	}

	@Test
	void a023_greaterThan() {
		assertTrue(greaterThan(2, 1));
		assertFalse(greaterThan(1, 2));
		assertFalse(greaterThan(null, "x"));
		assertTrue(greaterThan("x", null));
	}

	@Test
	void a024_greaterThanOrEqual() {
		assertTrue(greaterThanOrEqual(2, 1));
		assertTrue(greaterThanOrEqual(1, 1));
		assertFalse(greaterThanOrEqual(1, 2));
	}

	@Test
	void a025_min() {
		assertEquals(1, min(1, 2));
		assertEquals(1, min(2, 1));
		assertNull(min(null, null));
		assertEquals("a", min(null, "a"));
	}

	@Test
	void a026_max() {
		assertEquals(2, max(1, 2));
		assertEquals(2, max(2, 1));
		assertNull(max(null, null));
		assertEquals("a", max(null, "a"));
	}

	//====================================================================================================
	// coalesce
	//====================================================================================================
	@Test
	void a030_coalesce_two() {
		assertEquals("a", coalesce("a", "b"));
		assertEquals("b", coalesce(null, "b"));
		assertNull(coalesce(null, null));
	}

	@Test
	void a031_coalesce_varargs() {
		assertEquals("b", coalesce(null, "b", "c"));
		assertNull(coalesce((String[])null));
		assertNull(coalesce(null, null));
	}

	//====================================================================================================
	// allTrue / anyTrue / negate
	//====================================================================================================
	@Test
	void a040_allTrue() {
		assertTrue(allTrue(true, true));
		assertFalse(allTrue(true, false));
		assertTrue(allTrue());
		assertTrue(allTrue((boolean[])null));
	}

	@Test
	void a041_anyTrue() {
		assertTrue(anyTrue(false, true));
		assertFalse(anyTrue(false, false));
		assertFalse(anyTrue());
	}

	@Test
	void a042_negate() {
		assertTrue(negate(false));
		assertFalse(negate(true));
	}

	//====================================================================================================
	// isTrue / isFalse / toBoolean
	//====================================================================================================
	@Test
	void a043_isTrue() {
		assertTrue(isTrue(Boolean.TRUE));
		assertFalse(isTrue(null));
		assertFalse(isTrue(Boolean.FALSE));
	}

	@Test
	void a044_isFalse() {
		assertTrue(isFalse(Boolean.FALSE));
		assertFalse(isFalse(null));
		assertFalse(isFalse(Boolean.TRUE));
	}

	@Test
	void a045_toBoolean() {
		assertTrue(toBoolean("true"));
		assertFalse(toBoolean("false"));
		assertFalse(toBoolean(null));
	}

	//====================================================================================================
	// stringify / identity / unwrap
	//====================================================================================================
	@Test
	void a050_stringify() {
		assertEquals("42", stringify(42));
		assertNull(stringify(null));
	}

	@Test
	void a051_identity() {
		assertNotNull(identity("x"));
		assertNull(identity(null));
		assertNull(identity(Optional.empty()));
	}

	@Test
	void a052_unwrap() {
		assertEquals("x", unwrap(Optional.of("x")));
		assertNull(unwrap(Optional.empty()));
		assertEquals("x", unwrap((java.util.function.Supplier<?>)() -> "x"));
	}

	//====================================================================================================
	// abs / isBetween / isNotMinusOne
	//====================================================================================================
	@Test
	void a060_abs() {
		assertEquals(5, abs(-5));
		assertEquals(5L, abs(-5L));
		assertNull(abs((Integer)null));
	}

	@Test
	void a061_isBetween() {
		assertTrue(isBetween(3, 1, 5));
		assertTrue(isBetween(1, 1, 5));
		assertTrue(isBetween(5, 1, 5));
		assertFalse(isBetween(0, 1, 5));
		assertFalse(isBetween(6, 1, 5));
	}

	@Test
	void a062_isNotMinusOne() {
		assertTrue(isNotMinusOne(0));
		assertTrue(isNotMinusOne(1));
		assertFalse(isNotMinusOne(-1));
		assertFalse(isNotMinusOne(null));
	}

	//====================================================================================================
	// hash
	//====================================================================================================
	@Test
	void a070_hash() {
		int h1 = hash("a", "b");
		int h2 = hash("a", "b");
		assertEquals(h1, h2);
		assertNotEquals(hash("a", "b"), hash("a", "c"));
	}

	//====================================================================================================
	// isEmpty / isNotEmpty
	//====================================================================================================
	@Test
	void a080_isEmpty() {
		assertTrue(isEmpty(null));
		assertTrue(isEmpty(""));
		assertTrue(isEmpty(new ArrayList<>()));
		assertTrue(isEmpty(new HashMap<>()));
		assertTrue(isEmpty(new int[0]));
		assertFalse(isEmpty("x"));
		assertFalse(isEmpty(List.of("a")));
	}

	@Test
	void a081_isNotEmpty() {
		assertFalse(isNotEmpty(null));
		assertFalse(isNotEmpty(""));
		assertTrue(isNotEmpty("x"));
		assertTrue(isNotEmpty(List.of("a")));
	}

	//====================================================================================================
	// optional / emptyOptional / orElse
	//====================================================================================================
	@Test
	void a090_optional() {
		assertEquals(Optional.of("x"), optional("x"));
		assertEquals(Optional.empty(), optional(null));
	}

	@Test
	void a091_emptyOptional() {
		assertEquals(Optional.empty(), emptyOptional());
	}

	@Test
	void a092_orElse() {
		assertEquals("x", orElse(Optional.of("x"), "default"));
		assertEquals("default", orElse(Optional.empty(), "default"));
	}

	//====================================================================================================
	// requireNonNull / size
	//====================================================================================================
	@Test
	void a100_requireNonNull() {
		assertEquals("x", requireNonNull("x"));
		assertThrows(NullPointerException.class, () -> requireNonNull(null));
	}

	@Test
	void a101_size() {
		assertEquals(0, size(null));
		assertEquals(3, size("abc"));
		assertEquals(2, size(List.of("a", "b")));
		assertEquals(1, size(Map.of("k", "v")));
		assertEquals(2, size(new int[]{1, 2}));
		assertEquals(1, size(new Object()));
	}
}
