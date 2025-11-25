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
package org.apache.juneau.common.utils;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link HashCode}.
 */
class HashCode_Test extends TestBase {

	//====================================================================================================
	// create() tests
	//====================================================================================================

	@Test
	void a01_create_returnsNewInstance() {
		var hc1 = HashCode.create();
		var hc2 = HashCode.create();
		assertNotSame(hc1, hc2);
	}

	@Test
	void a02_create_initialHashCode() {
		var hc = HashCode.create();
		assertEquals(1, hc.get());
	}

	//====================================================================================================
	// of(Object...) tests
	//====================================================================================================

	@Test
	void b01_of_empty() {
		var hashCode = HashCode.of();
		assertEquals(1, hashCode);
	}

	@Test
	void b02_of_singleObject() {
		var hashCode = HashCode.of("test");
		assertEquals(31 * 1 + "test".hashCode(), hashCode);
	}

	@Test
	void b03_of_multipleObjects() {
		var hashCode = HashCode.of("a", "b", "c");
		var expected = HashCode.create().add("a").add("b").add("c").get();
		assertEquals(expected, hashCode);
	}

	@Test
	void b04_of_withNull() {
		var hashCode = HashCode.of("a", null, "c");
		var expected = HashCode.create().add("a").add(null).add("c").get();
		assertEquals(expected, hashCode);
	}

	@Test
	void b05_of_withArray() {
		var arr = a("a", "b");
		// When passing an array to varargs, it's treated as a single array object
		var hashCode = HashCode.of((Object)arr);
		var expected = HashCode.create().add(arr).get();
		assertEquals(expected, hashCode);
	}

	@Test
	void b06_of_withPrimitives() {
		var hashCode = HashCode.of(1, 2, 3);
		var expected = HashCode.create().add(1).add(2).add(3).get();
		assertEquals(expected, hashCode);
	}

	//====================================================================================================
	// add(int) tests
	//====================================================================================================

	@Test
	void c01_addInt_single() {
		var hc = HashCode.create();
		hc.add(42);
		assertEquals(31 * 1 + 42, hc.get());
	}

	@Test
	void c02_addInt_multiple() {
		var hc = HashCode.create();
		hc.add(1).add(2).add(3);
		var expected = 31 * (31 * (31 * 1 + 1) + 2) + 3;
		assertEquals(expected, hc.get());
	}

	@Test
	void c03_addInt_zero() {
		var hc = HashCode.create();
		hc.add(0);
		assertEquals(31 * 1 + 0, hc.get());
	}

	@Test
	void c04_addInt_negative() {
		var hc = HashCode.create();
		hc.add(-1);
		assertEquals(31 * 1 + (-1), hc.get());
	}

	@Test
	void c05_addInt_returnsThis() {
		var hc = HashCode.create();
		var result = hc.add(42);
		assertSame(hc, result);
	}

	@Test
	void c06_addInt_largeValue() {
		var hc = HashCode.create();
		hc.add(Integer.MAX_VALUE);
		assertEquals(31 * 1 + Integer.MAX_VALUE, hc.get());
	}

	@Test
	void c07_addInt_minValue() {
		var hc = HashCode.create();
		hc.add(Integer.MIN_VALUE);
		assertEquals(31 * 1 + Integer.MIN_VALUE, hc.get());
	}

	//====================================================================================================
	// add(Object) - null tests
	//====================================================================================================

	@Test
	void d01_addObject_null() {
		var hc = HashCode.create();
		hc.add((Object)null);
		assertEquals(31 * 1 + 0, hc.get());
	}

	@Test
	void d02_addObject_nullMultiple() {
		var hc = HashCode.create();
		hc.add(null).add(null).add(null);
		var expected = 31 * (31 * (31 * 1 + 0) + 0) + 0;
		assertEquals(expected, hc.get());
	}

	//====================================================================================================
	// add(Object) - String tests
	//====================================================================================================

	@Test
	void e01_addObject_string() {
		var hc = HashCode.create();
		hc.add("test");
		assertEquals(31 * 1 + "test".hashCode(), hc.get());
	}

	@Test
	void e02_addObject_stringEmpty() {
		var hc = HashCode.create();
		hc.add("");
		assertEquals(31 * 1 + "".hashCode(), hc.get());
	}

	@Test
	void e03_addObject_stringMultiple() {
		var hc = HashCode.create();
		hc.add("a").add("b").add("c");
		var expected = 31 * (31 * (31 * 1 + "a".hashCode()) + "b".hashCode()) + "c".hashCode();
		assertEquals(expected, hc.get());
	}

	//====================================================================================================
	// add(Object) - Array tests
	//====================================================================================================

	@Test
	void f01_addObject_objectArray() {
		var arr = a("a", "b", "c");
		var hc = HashCode.create();
		hc.add(arr);
		assertEquals(31 * 1 + Arrays.hashCode(arr), hc.get());
	}

	@Test
	void f02_addObject_intArray() {
		var arr = new int[] {1, 2, 3};
		var hc = HashCode.create();
		hc.add(arr);
		assertEquals(31 * 1 + Arrays.hashCode(arr), hc.get());
	}

	@Test
	void f03_addObject_longArray() {
		var arr = new long[] {1L, 2L, 3L};
		var hc = HashCode.create();
		hc.add(arr);
		assertEquals(31 * 1 + Arrays.hashCode(arr), hc.get());
	}

	@Test
	void f04_addObject_shortArray() {
		var arr = new short[] {1, 2, 3};
		var hc = HashCode.create();
		hc.add(arr);
		assertEquals(31 * 1 + Arrays.hashCode(arr), hc.get());
	}

	@Test
	void f05_addObject_byteArray() {
		var arr = new byte[] {1, 2, 3};
		var hc = HashCode.create();
		hc.add(arr);
		assertEquals(31 * 1 + Arrays.hashCode(arr), hc.get());
	}

	@Test
	void f06_addObject_charArray() {
		var arr = new char[] {'a', 'b', 'c'};
		var hc = HashCode.create();
		hc.add(arr);
		assertEquals(31 * 1 + Arrays.hashCode(arr), hc.get());
	}

	@Test
	void f07_addObject_booleanArray() {
		var arr = new boolean[] {true, false, true};
		var hc = HashCode.create();
		hc.add(arr);
		assertEquals(31 * 1 + Arrays.hashCode(arr), hc.get());
	}

	@Test
	void f08_addObject_floatArray() {
		var arr = new float[] {1.0f, 2.0f, 3.0f};
		var hc = HashCode.create();
		hc.add(arr);
		assertEquals(31 * 1 + Arrays.hashCode(arr), hc.get());
	}

	@Test
	void f09_addObject_doubleArray() {
		var arr = new double[] {1.0, 2.0, 3.0};
		var hc = HashCode.create();
		hc.add(arr);
		assertEquals(31 * 1 + Arrays.hashCode(arr), hc.get());
	}

	@Test
	void f10_addObject_emptyArray() {
		var arr = new String[0];
		var hc = HashCode.create();
		hc.add(arr);
		assertEquals(31 * 1 + Arrays.hashCode(arr), hc.get());
	}

	@Test
	void f11_addObject_arrayWithNulls() {
		var arr = new String[] {"a", null, "c"};
		var hc = HashCode.create();
		hc.add(arr);
		assertEquals(31 * 1 + Arrays.hashCode(arr), hc.get());
	}

	@Test
	void f12_addObject_nestedArray() {
		var arr = new int[][] {{1, 2}, {3, 4}};
		var hc = HashCode.create();
		hc.add(arr);
		// Nested arrays are treated as Object[], so use Arrays.hashCode
		assertEquals(31 * 1 + Arrays.hashCode(arr), hc.get());
	}

	//====================================================================================================
	// add(Object) - Regular object tests
	//====================================================================================================

	@Test
	void g01_addObject_integer() {
		var hc = HashCode.create();
		hc.add(Integer.valueOf(42));
		assertEquals(31 * 1 + Integer.valueOf(42).hashCode(), hc.get());
	}

	@Test
	void g02_addObject_list() {
		var list = Arrays.asList("a", "b", "c");
		var hc = HashCode.create();
		hc.add(list);
		assertEquals(31 * 1 + list.hashCode(), hc.get());
	}

	@Test
	void g03_addObject_map() {
		var map = new HashMap<String, String>();
		map.put("key", "value");
		var hc = HashCode.create();
		hc.add(map);
		assertEquals(31 * 1 + map.hashCode(), hc.get());
	}

	@Test
	void g04_addObject_customObject() {
		class TestObject {
			private final String value;
			TestObject(String value) { this.value = value; }
			@Override
			public int hashCode() { return value.hashCode(); }
		}
		var obj = new TestObject("test");
		var hc = HashCode.create();
		hc.add(obj);
		assertEquals(31 * 1 + obj.hashCode(), hc.get());
	}

	//====================================================================================================
	// get() tests
	//====================================================================================================

	@Test
	void h01_get_initialValue() {
		var hc = HashCode.create();
		assertEquals(1, hc.get());
	}

	@Test
	void h02_get_afterAdd() {
		var hc = HashCode.create();
		hc.add(42);
		assertEquals(31 * 1 + 42, hc.get());
	}

	@Test
	void h03_get_multipleCalls() {
		var hc = HashCode.create();
		hc.add(42);
		var first = hc.get();
		var second = hc.get();
		assertEquals(first, second);
	}

	//====================================================================================================
	// Chaining tests
	//====================================================================================================

	@Test
	void i01_chaining_mixedTypes() {
		var hc = HashCode.create();
		hc.add(1).add("test").add(2).add(null).add(3);
		var expected = HashCode.create()
			.add(1)
			.add("test")
			.add(2)
			.add(null)
			.add(3)
			.get();
		assertEquals(expected, hc.get());
	}

	@Test
	void i02_chaining_returnsThis() {
		var hc = HashCode.create();
		var result1 = hc.add(1);
		var result2 = result1.add("test");
		assertSame(hc, result1);
		assertSame(hc, result2);
	}

	//====================================================================================================
	// Order matters tests
	//====================================================================================================

	@Test
	void j01_orderMatters_differentOrder() {
		var hc1 = HashCode.create().add("a").add("b");
		var hc2 = HashCode.create().add("b").add("a");
		assertNotEquals(hc1.get(), hc2.get());
	}

	@Test
	void j02_orderMatters_sameOrder() {
		var hc1 = HashCode.create().add("a").add("b").add("c");
		var hc2 = HashCode.create().add("a").add("b").add("c");
		assertEquals(hc1.get(), hc2.get());
	}

	//====================================================================================================
	// Consistency tests
	//====================================================================================================

	@Test
	void k01_consistency_sameInputs() {
		var hc1 = HashCode.create().add("test").add(42).add("foo");
		var hc2 = HashCode.create().add("test").add(42).add("foo");
		assertEquals(hc1.get(), hc2.get());
	}

	@Test
	void k02_consistency_differentInstances() {
		var hc1 = HashCode.create().add("test");
		var hc2 = HashCode.create().add("test");
		assertEquals(hc1.get(), hc2.get());
	}

	//====================================================================================================
	// Edge cases
	//====================================================================================================

	@Test
	void l01_edgeCase_emptyHashCode() {
		var hc = HashCode.create();
		assertEquals(1, hc.get());
	}

	@Test
	void l02_edgeCase_veryLongChain() {
		var hc = HashCode.create();
		for (var i = 0; i < 100; i++) {
			hc.add(i);
		}
		// Just verify it doesn't throw and produces a value
		assertNotNull(hc.get());
	}

	@Test
	void l03_edgeCase_mixedNullsAndValues() {
		var hc = HashCode.create();
		hc.add("a").add(null).add("b").add(null).add("c");
		var expected = HashCode.create()
			.add("a")
			.add(null)
			.add("b")
			.add(null)
			.add("c")
			.get();
		assertEquals(expected, hc.get());
	}

	@Test
	void l04_edgeCase_arraysOfDifferentTypes() {
		var hc1 = HashCode.create().add(new int[] {1, 2, 3});
		var hc2 = HashCode.create().add(new long[] {1L, 2L, 3L});
		// Different array types should produce different hashcodes (usually, but not guaranteed)
		// Arrays.hashCode for int[] and long[] with same values might coincidentally be the same
		// So we just verify both produce valid hashcodes
		assertNotNull(hc1.get());
		assertNotNull(hc2.get());
		// Verify they use Arrays.hashCode correctly
		assertEquals(31 * 1 + Arrays.hashCode(new int[] {1, 2, 3}), hc1.get());
		assertEquals(31 * 1 + Arrays.hashCode(new long[] {1L, 2L, 3L}), hc2.get());
	}

	@Test
	void l05_edgeCase_sameArrayContent() {
		var arr1 = new int[] {1, 2, 3};
		var arr2 = new int[] {1, 2, 3};
		var hc1 = HashCode.create().add(arr1);
		var hc2 = HashCode.create().add(arr2);
		// Same content should produce same hashcode
		assertEquals(hc1.get(), hc2.get());
	}

	@Test
	void l06_edgeCase_differentArrayContent() {
		var arr1 = new int[] {1, 2, 3};
		var arr2 = new int[] {1, 2, 4};
		var hc1 = HashCode.create().add(arr1);
		var hc2 = HashCode.create().add(arr2);
		// Different content should produce different hashcodes
		assertNotEquals(hc1.get(), hc2.get());
	}
}

