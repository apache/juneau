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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.annotation.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.lang.*;
import org.junit.jupiter.api.*;

@Retention(RetentionPolicy.RUNTIME)
@interface TestAnnotation {
	String value() default "";
	int num() default 0;
}

class Utils_Test extends TestBase {

	//====================================================================================================
	// Constructor (line 56)
	//====================================================================================================
	@Test
	void a00_constructor() {
		// Test line 56: class declaration
		// Utils has a protected constructor, so it can be subclassed.
		// Create a subclass to access the protected constructor
		class TestUtils extends Utils {
			TestUtils() {
				super(); // This calls the protected Utils() constructor
			}
		}
		var testUtils = new TestUtils();
		assertNotNull(testUtils);
	}

	//====================================================================================================
	// b(Object)
	//====================================================================================================
	@Test
	void a001_b() {
		assertTrue(bool(true));
		assertTrue(bool("true"));
		assertTrue(bool("TRUE"));
		assertFalse(bool(false));
		assertFalse(bool("false"));
		assertFalse(bool("FALSE"));
		assertFalse(bool(null));
		assertFalse(bool(""));
		assertFalse(bool(123));
		assertFalse(bool(new Object()));
	}

	//====================================================================================================
	// cast(Class<T>, Object)
	//====================================================================================================
	@Test
	void a002_cast() {
		var obj = "Hello";
		assertEquals("Hello", cast(String.class, obj));
		assertNull(cast(Integer.class, obj));
		assertNull(cast(String.class, null));
		assertEquals(123, cast(Integer.class, 123));
		assertNull(cast(String.class, 123));
	}

	//====================================================================================================
	// cast(Class<T>, Object) - Additional tests for incorrect instance types
	//====================================================================================================
	@Test
	void a003_cast_additionalTests() {
		var obj = "Hello";
		// Test that incorrect instance types return null without throwing exception
		assertNull(cast(Integer.class, obj));
		assertNull(cast(String.class, 123));

		// Test with different types
		assertEquals(123, cast(Integer.class, 123));
		assertNull(cast(String.class, 123));

		// Test with null
		assertNull(cast(String.class, null));
	}

	//====================================================================================================
	// cn(Object)
	//====================================================================================================
	@Test
	void a004_cn() {
		assertEquals("java.lang.String", cn(String.class));
		assertEquals("java.util.HashMap", cn(new HashMap<>()));
		assertEquals("java.util.Map$Entry", cn(Map.Entry.class));
		assertEquals("int", cn(int.class));
		assertEquals("boolean", cn(boolean.class));
		assertEquals("[Ljava.lang.String;", cn(String[].class));
		assertEquals("[I", cn(int[].class));
		assertNull(cn(null));
	}

	//====================================================================================================
	// cns(Object)
	//====================================================================================================
	@Test
	void a005_cns() {
		assertEquals("String", cns(String.class));
		assertEquals("HashMap", cns(new HashMap<>()));
		assertEquals("Entry", cns(Map.Entry.class));
		assertEquals("int", cns(int.class));
		assertEquals("boolean", cns(boolean.class));
		assertEquals("String[]", cns(String[].class));
		assertEquals("int[]", cns(int[].class));
		assertNull(cns(null));
	}

	//====================================================================================================
	// cnsq(Object)
	//====================================================================================================
	@Test
	void a006_cnsq() {
		assertEquals("String", cnsq(String.class));
		assertEquals("HashMap", cnsq(new HashMap<>()));
		assertEquals("Map.Entry", cnsq(Map.Entry.class));
		assertEquals("int", cnsq(int.class));
		assertEquals("boolean", cnsq(boolean.class));
		assertEquals("String[]", cnsq(String[].class));
		assertEquals("int[]", cnsq(int[].class));
		assertNull(cnsq(null));
	}

	//====================================================================================================
	// cmp(Object, Object)
	//====================================================================================================
	@Test
	void a007_cmp() {
		assertTrue(cmp("apple", "banana") < 0);
		assertTrue(cmp("banana", "apple") > 0);
		assertEquals(0, cmp("apple", "apple"));
		assertEquals(0, cmp(null, null));
		assertTrue(cmp(null, "apple") < 0);
		assertTrue(cmp("apple", null) > 0);
		assertTrue(cmp(5, 10) < 0);
		assertTrue(cmp(10, 5) > 0);
		assertEquals(0, cmp(5, 5));
		assertEquals(0, cmp("apple", 5)); // Different types, cannot compare

		// Test line 285 branch: same class but not Comparable
		class NotComparable {}
		var nc1 = new NotComparable();
		var nc2 = new NotComparable();
		assertEquals(0, cmp(nc1, nc2)); // Same class, but not Comparable - covers missing branch
	}

	//====================================================================================================
	// ea(Class<T>)
	//====================================================================================================
	@Test
	void a008_ea() {
		var empty1 = ea(String.class);
		assertEquals(0, empty1.length);
		var empty2 = ea(Integer.class);
		assertEquals(0, empty2.length);
		var empty3 = ea(List.class);
		assertEquals(0, empty3.length);
	}

	//====================================================================================================
	// emptyIfNull(Object)
	//====================================================================================================
	@Test
	void a009_emptyIfNull() {
		assertEquals("Hello", emptyIfNull("Hello"));
		assertEquals("123", emptyIfNull(123));
		assertEquals("", emptyIfNull(null));
	}

	//====================================================================================================
	// env(String)
	//====================================================================================================
	@Test
	void a010_env() {
		// Test with system property
		System.setProperty("test.property", "testValue");
		var result = env("test.property");
		assertTrue(result.isPresent());
		assertEquals("testValue", result.get());
		System.clearProperty("test.property");

		// Test with non-existent property
		var missing = env("nonexistent.property.xyz");
		assertFalse(missing.isPresent());
	}

	//====================================================================================================
	// eq(boolean, String, String)
	//====================================================================================================
	@Test
	void a012_eq_caseSensitive() {
		assertTrue(eq(false, "Hello", "Hello"));
		assertFalse(eq(false, "Hello", "hello"));
		assertTrue(eq(false, null, null));
		assertFalse(eq(false, "Hello", null));
		assertFalse(eq(false, null, "Hello"));
		assertTrue(eq(true, "Hello", "hello"));
		assertTrue(eq(true, "Hello", "HELLO"));
		assertTrue(eq(true, null, null));
	}

	//====================================================================================================
	// eq(T, T)
	//====================================================================================================
	@Test
	void a013_eq() {
		assertTrue(eq("Hello", "Hello"));
		assertFalse(eq("Hello", "World"));
		assertTrue(eq(null, null));
		assertFalse(eq("Hello", null));
		assertFalse(eq(null, "Hello"));
		assertTrue(eq(123, 123));
		assertFalse(eq(123, 456));

		// Test arrays - covers line 456
		var arr1 = a(1, 2, 3);
		var arr2 = a(1, 2, 3);
		var arr3 = a(1, 2, 4);
		var arr4 = a(1, 2);  // Different length
		// Test line 456: both are arrays
		assertTrue(eq(arr1, arr2));  // Both arrays, same content
		assertFalse(eq(arr1, arr3));  // Both arrays, different content
		assertFalse(eq(arr1, arr4));  // Different lengths - covers line 459
		// Test line 456 branch: one is array, other is not
		assertFalse(eq(arr1, "not an array"));  // o1 is Array, o2 is not
		assertFalse(eq("not an array", arr1));  // o1 is not, o2 is Array

		// Test annotations - use actual annotation instances from classes
		@TestAnnotation("test")
		class T1 {}
		@TestAnnotation("test")
		class T2 {}
		@TestAnnotation("different")
		class T3 {}

		var a1 = T1.class.getAnnotation(TestAnnotation.class);
		var a2 = T2.class.getAnnotation(TestAnnotation.class);
		var a3 = T3.class.getAnnotation(TestAnnotation.class);
		// Test annotation equality - covers line 453 (both are annotations)
		assertTrue(eq(a1, a2));
		assertFalse(eq(a1, a3));
		// Test line 453 branch: one is annotation, other is not
		assertFalse(eq(a1, "not an annotation"));  // o1 is Annotation, o2 is not
		assertFalse(eq("not an annotation", a1));  // o1 is not, o2 is Annotation
	}

	//====================================================================================================
	// eqAny(T, T...)
	//====================================================================================================
	@Test
	void a014_eqAny() {
		// Basic equality tests
		assertTrue(eqAny("apple", "apple", "banana", "cherry"));
		assertFalse(eqAny("apple", "banana", "cherry"));
		assertTrue(eqAny(123, 123, 456, 789));
		assertFalse(eqAny(123, 456, 789));

		// Null handling
		assertTrue(eqAny(null, "apple", null, "banana"));
		assertFalse(eqAny(null, "apple", "banana"));
		assertTrue(eqAny("apple", null, "apple", "banana"));
		assertFalse(eqAny("apple", "banana", "cherry"));

		// Empty varargs
		assertFalse(eqAny("apple"));
		assertFalse(eqAny(null));
		assertFalse(eqAny(123));

		// Null varargs array
		String[] nullArray = null;
		assertFalse(eqAny("apple", nullArray));

		// Test arrays
		var arr1 = a(1, 2, 3);
		var arr2 = a(1, 2, 3);
		var arr3 = a(1, 2, 4);
		assertTrue(eqAny(arr1, arr2, arr3));
		assertFalse(eqAny(arr1, (Serializable[])arr3));

		// Test annotations
		@TestAnnotation("test")
		class T1 {}
		@TestAnnotation("test")
		class T2 {}
		@TestAnnotation("different")
		class T3 {}

		var a1 = T1.class.getAnnotation(TestAnnotation.class);
		var a2 = T2.class.getAnnotation(TestAnnotation.class);
		var a3 = T3.class.getAnnotation(TestAnnotation.class);
		assertTrue(eqAny(a1, a2, a3));
		assertFalse(eqAny(a1, a3));
	}

	//====================================================================================================
	// eq(T, U, BiPredicate<T,U>)
	//====================================================================================================
	@Test
	void a014_eq_withPredicate() {
		class Role {
			int id;
			String name;
			Role(int id, String name) { this.id = id; this.name = name; }
		}

		var r1 = new Role(1, "admin");
		var r2 = new Role(1, "admin");
		var r3 = new Role(2, "user");

		assertTrue(eq(r1, r2, (x, y) -> x.id == y.id && x.name.equals(y.name)));
		assertFalse(eq(r1, r3, (x, y) -> x.id == y.id && x.name.equals(y.name)));
		assertTrue(eq(null, null, (x, y) -> false));
		assertFalse(eq(r1, null, (x, y) -> true));
		assertFalse(eq(null, r2, (x, y) -> true));
		assertTrue(eq(r1, r1, (x, y) -> false)); // Same reference
	}

	//====================================================================================================
	// eqic(Object, Object)
	//====================================================================================================
	@Test
	void a015_eqic_object() {
		assertTrue(eqic("Hello", "Hello"));
		assertTrue(eqic("Hello", "hello"));
		assertTrue(eqic("Hello", "HELLO"));
		assertTrue(eqic(null, null));
		assertFalse(eqic("Hello", null));
		assertFalse(eqic(null, "Hello"));
		assertTrue(eqic(123, 123));
		assertFalse(eqic(123, 456));
	}

	//====================================================================================================
	// eqic(String, String)
	//====================================================================================================
	@Test
	void a016_eqic_string() {
		assertTrue(eqic("Hello", "Hello"));
		assertTrue(eqic("Hello", "hello"));
		assertTrue(eqic("Hello", "HELLO"));
		assertTrue(eqic(null, null));
		assertFalse(eqic("Hello", null));
		assertFalse(eqic(null, "Hello"));
	}

	//====================================================================================================
	// f(String, Object...)
	//====================================================================================================
	@Test
	void a017_f() {
		// Basic formatting
		assertEquals("Hello John, you have 5 items", f("Hello %s, you have %d items", "John", 5));
		assertEquals("Hello world", f("Hello %s", "world"));

		// Floating point
		assertEquals("Price: $19.99", f("Price: $%.2f", 19.99));
		assertEquals("Value: 3.14", f("Value: %.2f", 3.14159));

		// Multiple arguments
		assertEquals("Name: John, Age: 30, Salary: $50000.00",
			f("Name: %s, Age: %d, Salary: $%.2f", "John", 30, 50000.0));

		// Null handling
		assertEquals("Value: null", f("Value: %s", (String)null));
		assertThrows(IllegalArgumentException.class, ()->f(null, "test"));
		assertEquals("test", f("test"));

		// MessageFormat style
		assertEquals("Hello John, you have 5 items", f("Hello {0}, you have {1} items", "John", 5));
	}

	//====================================================================================================
	// firstNonNull(T...)
	//====================================================================================================
	@Test
	void a018_firstNonNull() {
		assertEquals("Hello", firstNonNull(null, null, "Hello", "World"));
		assertEquals("Hello", firstNonNull("Hello", "World"));
		assertNull(firstNonNull(null, null));
		assertNull(firstNonNull());
		assertEquals(123, firstNonNull(null, null, 123, 456));

		// Test line 614 branch: null array (varargs array itself is null)
		// This can happen if called with explicit null cast
		String[] nullArray = null;
		assertNull(firstNonNull(nullArray));  // Covers the nn(t) == false branch
	}

	//====================================================================================================
	// fs(String, Object...)
	//====================================================================================================
	@Test
	void a019_fs() {
		// Basic supplier
		var supplier = fs("Hello %s, you have %d items", "John", 5);
		assertNotNull(supplier);
		assertEquals("Hello John, you have 5 items", supplier.get());

		// Lazy evaluation - format only when get() is called
		var lazySupplier = fs("Price: $%.2f", 19.99);
		assertEquals("Price: $19.99", lazySupplier.get());

		// Multiple calls return same result
		var supplier2 = fs("Value: %s", "test");
		assertEquals("Value: test", supplier2.get());
		assertEquals("Value: test", supplier2.get());

		// Null handling
		var nullSupplier = fs(null, "test");
		assertThrows(IllegalArgumentException.class, nullSupplier::get);

		// Empty pattern
		var emptySupplier = fs("");
		assertEquals("", emptySupplier.get());
	}

	//====================================================================================================
	// h(Object...)
	//====================================================================================================
	@Test
	void a020_hash() {
		var hash1 = h("Hello", 123, true);
		var hash2 = h("Hello", 123, true);
		assertEquals(hash1, hash2);

		var hash3 = h("Hello", 123, false);
		assertNotEquals(hash1, hash3);

		// Test with annotations
		@TestAnnotation("test")
		class T {}
		var a1 = T.class.getAnnotation(TestAnnotation.class);
		var hash4 = h(a1, "value");
		assertNotNull(hash4);
	}

	//====================================================================================================
	// id(Object)
	//====================================================================================================
	@Test
	void a021_identity() {
		var obj = "test";
		var identity = id(obj);
		assertNotNull(identity);
		assertTrue(identity.contains("String"));
		assertTrue(identity.contains("@"));
		assertNull(id(null));
		assertNotNull(id(Optional.of("test")));
	}

	//====================================================================================================
	// isArray(Object)
	//====================================================================================================
	@Test
	void a022_isArray() {
		assertTrue(isArray(new int[]{1, 2, 3}));
		assertTrue(isArray(a("a", "b")));
		assertTrue(isArray(a()));
		assertFalse(isArray("Hello"));
		assertFalse(isArray(null));
		assertFalse(isArray(123));
		assertFalse(isArray(new ArrayList<>()));
	}

	//====================================================================================================
	// isBetween(int, int, int)
	//====================================================================================================
	@Test
	void a023_isBetween() {
		assertTrue(isBetween(5, 1, 10));
		assertTrue(isBetween(1, 1, 10));
		assertTrue(isBetween(10, 1, 10));
		assertFalse(isBetween(0, 1, 10));
		assertFalse(isBetween(11, 1, 10));
	}

	//====================================================================================================
	// e(CharSequence)
	//====================================================================================================
	@Test
	void a024_isEmpty_CharSequence() {
		assertTrue(e((String)null));
		assertTrue(e(""));
		assertFalse(e("   "));
		assertFalse(e("hello"));
		assertFalse(e("a"));
	}

	//====================================================================================================
	// e(Collection<?>)
	//====================================================================================================
	@Test
	void a025_isEmpty_Collection() {
		assertTrue(e((Collection<?>)null));
		assertTrue(e(Collections.emptyList()));
		assertTrue(e(new ArrayList<>()));
		assertFalse(e(Arrays.asList(1, 2, 3)));
		assertFalse(e(Collections.singletonList("test")));
	}

	//====================================================================================================
	// e(Map<?,?>)
	//====================================================================================================
	@Test
	void a026_isEmpty_Map() {
		assertTrue(e((Map<?,?>)null));
		assertTrue(e(Collections.emptyMap()));
		assertTrue(e(new HashMap<>()));
		assertFalse(e(Map.of("key", "value")));
		assertFalse(e(Collections.singletonMap("key", "value")));
	}

	//====================================================================================================
	// e(Object)
	//====================================================================================================
	@Test
	void a027_isEmpty_Object() {
		assertTrue(e((Object)null));
		assertTrue(e((Object)""));
		// Test line 834: Collection branch
		assertTrue(e((Object)Collections.emptyList()));
		assertFalse(e((Object)Arrays.asList(1, 2)));  // Non-empty collection
		// Test line 836: Map branch
		assertTrue(e((Object)Collections.emptyMap()));
		assertFalse(e((Object)Map.of("key", "value")));  // Non-empty map
		assertTrue(e(new int[0]));
		assertTrue(e(new String[0]));
		assertFalse(e((Object)"hello"));
		assertFalse(e(a(1, 2)));
		assertTrue(e(new Object() {
			@Override public String toString() { return ""; }
		}));
	}

	//====================================================================================================
	// ne(CharSequence)
	//====================================================================================================
	@Test
	void a028_isNotEmpty_CharSequence() {
		assertFalse(ne((String)null));
		assertFalse(ne(""));
		assertTrue(ne("   "));
		assertTrue(ne("hello"));
		assertTrue(ne("a"));
	}

	//====================================================================================================
	// ne(Collection<?>)
	//====================================================================================================
	@Test
	void a029_isNotEmpty_Collection() {
		assertFalse(ne((Collection<?>)null));
		assertFalse(ne(Collections.emptyList()));
		assertFalse(ne(new ArrayList<>()));
		assertTrue(ne(Arrays.asList(1, 2, 3)));
		assertTrue(ne(Collections.singletonList("test")));
	}

	//====================================================================================================
	// ne(Map<?,?>)
	//====================================================================================================
	@Test
	void a030_isNotEmpty_Map() {
		assertFalse(ne((Map<?,?>)null));
		assertFalse(ne(Collections.emptyMap()));
		assertFalse(ne(new HashMap<>()));
		assertTrue(ne(Map.of("key", "value")));
		assertTrue(ne(Collections.singletonMap("key", "value")));
	}

	//====================================================================================================
	// ne(Object)
	//====================================================================================================
	@Test
	void a031_isNotEmpty_Object() {
		assertFalse(ne((Object)null));
		// Test line 939: CharSequence branch
		assertFalse(ne((Object)""));
		assertTrue(ne((Object)"hello"));  // Non-empty CharSequence
		// Test line 941: Collection branch
		assertFalse(ne((Object)Collections.emptyList()));
		assertTrue(ne((Object)Arrays.asList(1, 2)));  // Non-empty collection
		// Test line 943: Map branch
		assertFalse(ne((Object)Collections.emptyMap()));
		assertTrue(ne((Object)Map.of("key", "value")));  // Non-empty map
		assertFalse(ne(new int[0]));
		assertFalse(ne(new String[0]));
		assertTrue(ne(a(1, 2)));
		// Test line 946: fallback case (non-String, non-Collection, non-Map, non-Array)
		assertTrue(ne(new Object() {
			@Override public String toString() { return "test"; }
		}));
		assertFalse(ne(new Object() {
			@Override public String toString() { return ""; }
		}));
	}

	//====================================================================================================
	// nm1(T)
	//====================================================================================================
	@Test
	void a032_isNotMinusOne() {
		assertTrue(nm1(5));
		assertTrue(nm1(0));
		assertTrue(nm1(100));
		assertFalse(nm1(-1));
		assertFalse(nm1((Integer)null));
		assertTrue(nm1(5L));
		assertFalse(nm1(-1L));
	}

	//====================================================================================================
	// isTrue(Boolean)
	//====================================================================================================
	@Test
	void a034_isTrue() {
		assertTrue(isTrue(true));
		assertFalse(isTrue(false));
		assertFalse(isTrue(null));
	}

	//====================================================================================================
	// lc(String)
	//====================================================================================================
	@Test
	void a035_lc() {
		assertEquals("hello", lc("Hello"));
		assertEquals("hello", lc("HELLO"));
		assertEquals("hello world", lc("Hello World"));
		assertNull(lc(null));
		assertEquals("", lc(""));
	}

	//====================================================================================================
	// mem(Supplier<T>)
	//====================================================================================================
	@Test
	void a036_memoize() {
		var callCount = new AtomicInteger(0);
		var supplier = mem(() -> {
			callCount.incrementAndGet();
			return "result";
		});

		var result1 = supplier.get();
		var result2 = supplier.get();
		var result3 = supplier.get();

		assertEquals("result", result1);
		assertEquals("result", result2);
		assertEquals("result", result3);
		assertEquals(1, callCount.get()); // Should only be called once
	}

	//====================================================================================================
	// memr(Supplier<T>)
	//====================================================================================================
	@Test
	void a037_memoizeResettable() {
		var callCount = new AtomicInteger(0);
		var supplier = memr(() -> {
			callCount.incrementAndGet();
			return "result";
		});

		var result1 = supplier.get();
		var result2 = supplier.get();
		assertEquals(1, callCount.get());

		supplier.reset();
		var result3 = supplier.get();
		assertEquals(2, callCount.get());

		assertEquals("result", result1);
		assertEquals("result", result2);
		assertEquals("result", result3);
	}

	//====================================================================================================
	// n(Class<T>)
	//====================================================================================================
	@Test
	void a038_n() {
		assertNull(no(String.class));
		assertNull(no(Integer.class));
		assertNull(no(List.class));
	}

	//====================================================================================================
	// ne(T, T)
	//====================================================================================================
	@Test
	void a039_ne() {
		assertTrue(neq("Hello", "World"));
		assertFalse(neq("Hello", "Hello"));
		assertFalse(neq(null, null));
		assertTrue(neq("Hello", null));
		assertTrue(neq(null, "Hello"));
		assertTrue(neq(123, 456));
		assertFalse(neq(123, 123));
	}

	//====================================================================================================
	// ne(T, U, BiPredicate<T,U>)
	//====================================================================================================
	@Test
	void a040_ne_withPredicate() {
		class Role {
			int id;
			String name;
			Role(int id, String name) { this.id = id; this.name = name; }
		}

		var r1 = new Role(1, "admin");
		var r2 = new Role(1, "admin");
		var r3 = new Role(2, "user");

		assertFalse(neq(r1, r2, (x, y) -> x.id == y.id && x.name.equals(y.name)));
		assertTrue(neq(r1, r3, (x, y) -> x.id == y.id && x.name.equals(y.name)));
		assertFalse(neq(null, null, (x, y) -> true));
		assertTrue(neq(r1, null, (x, y) -> false));
		assertTrue(neq(null, r2, (x, y) -> false));
		assertFalse(neq(r1, r1, (x, y) -> true)); // Same reference
	}

	//====================================================================================================
	// neic(String, String)
	//====================================================================================================
	@Test
	void a041_neic() {
		assertTrue(neqic("Hello", "World"));
		assertFalse(neqic("Hello", "hello"));
		assertFalse(neqic("Hello", "HELLO"));
		assertFalse(neqic(null, null));
		assertTrue(neqic("Hello", null));
		assertTrue(neqic(null, "Hello"));
	}

	//====================================================================================================
	// nn(Object)
	//====================================================================================================
	@Test
	void a042_nn() {
		assertTrue(nn("test"));
		assertTrue(nn(123));
		assertTrue(nn(new Object()));
		assertTrue(nn(""));
		assertTrue(nn(0));
		assertTrue(nn(false));
		assertFalse(nn(null));
	}

	//====================================================================================================
	// opt(T)
	//====================================================================================================
	@Test
	void a043_opt() {
		var opt1 = opt("Hello");
		assertTrue(opt1.isPresent());
		assertEquals("Hello", opt1.get());

		var opt2 = opt(null);
		assertFalse(opt2.isPresent());
	}

	//====================================================================================================
	// opte()
	//====================================================================================================
	@Test
	void a044_opte() {
		var empty = opte();
		assertFalse(empty.isPresent());
	}

	//====================================================================================================
	// printLines(String[])
	//====================================================================================================
	@Test
	void a045_printLines() {
		// This test just verifies the method doesn't throw
		assertDoesNotThrow(() -> {
			printLines(a("Line 1", "Line 2", "Line 3"));
			printLines(a());
		});
	}

	//====================================================================================================
	// r(Object)
	//====================================================================================================
	@Test
	void a046_r() {
		assertEquals("hello", r("hello"));
		assertEquals("123", r(123));
		assertEquals("[1,2,3]", r(Arrays.asList(1, 2, 3)));
		assertNull(r(null));
		var bytes = new byte[]{1, 2};
		assertEquals("0102", r(bytes));
	}

	//====================================================================================================
	// s(Object)
	//====================================================================================================
	@Test
	void a047_s() {
		assertEquals("Hello", s("Hello"));
		assertEquals("123", s(123));
		assertEquals("true", s(true));
		assertNull(s(null));
	}

	//====================================================================================================
	// safe(Snippet)
	//====================================================================================================
	@Test
	void a048_safe_Snippet() {
		// Test normal execution - covers line 1371
		AtomicInteger count = new AtomicInteger(0);
		safe((Snippet)count::incrementAndGet);
		assertEquals(1, count.get());

		// Test RuntimeException is rethrown - covers lines 1372-1373
		var re = assertThrows(RuntimeException.class, () -> safe((Snippet)() -> {
			throw new RuntimeException("test");
		}));
		assertEquals("test", re.getMessage());

		// Test checked exception is wrapped - covers lines 1374-1375
		var wrapped = assertThrows(RuntimeException.class, () -> safe((Snippet)() -> {
			throw new Exception("test");
		}));
		assertNotNull(wrapped.getCause());
		assertEquals(Exception.class, wrapped.getCause().getClass());
		assertEquals("test", wrapped.getCause().getMessage());

		// Test Error is wrapped - covers lines 1374-1375 with Error
		var wrappedError = assertThrows(RuntimeException.class, () -> safe((Snippet)() -> {
			throw new Error("test error");
		}));
		assertNotNull(wrappedError.getCause());
		assertEquals(Error.class, wrappedError.getCause().getClass());
		assertEquals("test error", wrappedError.getCause().getMessage());
	}

	//====================================================================================================
	// safe(Snippet, Function<Throwable, RuntimeException>)
	//====================================================================================================
	@Test
	void a049_safe_Snippet_withExceptionMapper() {
		// Test normal execution
		AtomicInteger count = new AtomicInteger(0);
		safe((Snippet)count::incrementAndGet, e -> new RuntimeException("mapped: " + e.getMessage()));
		assertEquals(1, count.get());

		// Test RuntimeException is rethrown (not mapped)
		var re = assertThrows(RuntimeException.class, () -> safe((Snippet)() -> {
			throw new RuntimeException("original");
		}, e -> new RuntimeException("mapped: " + e.getMessage())));
		assertEquals("original", re.getMessage());

		// Test checked exception is mapped using the provided function
		var mapped = assertThrows(RuntimeException.class, () -> safe((Snippet)() -> {
			throw new Exception("test exception");
		}, e -> new IllegalArgumentException("custom: " + e.getMessage())));
		assertEquals("custom: test exception", mapped.getMessage());
		assertTrue(mapped instanceof IllegalArgumentException);

		// Test Error is mapped
		var mappedError = assertThrows(RuntimeException.class, () -> safe((Snippet)() -> {
			throw new Error("test error");
		}, e -> new IllegalStateException("error: " + e.getMessage())));
		assertEquals("error: test error", mappedError.getMessage());
		assertTrue(mappedError instanceof IllegalStateException);

		// Test with custom exception type
		@SuppressWarnings("serial")
		class CustomRuntimeException extends RuntimeException {
			CustomRuntimeException(String message, Throwable cause) {
				super(message, cause);
			}
		}
		var custom = assertThrows(CustomRuntimeException.class, () -> safe((Snippet)() -> {
			throw new Exception("test");
		}, e -> new CustomRuntimeException("wrapped", e)));
		assertEquals("wrapped", custom.getMessage());
		assertNotNull(custom.getCause());
		assertEquals(Exception.class, custom.getCause().getClass());
	}

	//====================================================================================================
	// safe(ThrowingSupplier<T>)
	//====================================================================================================
	@Test
	void a050_safe_ThrowingSupplier() {
		// Test normal execution
		var result = safe(() -> "result");
		assertEquals("result", result);

		// Test RuntimeException is rethrown
		assertThrows(RuntimeException.class, () -> safe(() -> {
			throw new RuntimeException("test");
		}));

		// Test checked exception is wrapped
		assertThrows(RuntimeException.class, () -> safe(() -> {
			throw new Exception("test");
		}));
	}

	//====================================================================================================
	// safe(ThrowingSupplier<T>, Function<Exception, RuntimeException>)
	//====================================================================================================
	@Test
	void a051_safe_ThrowingSupplier_withExceptionMapper() {
		// Test normal execution
		ThrowingSupplier<String> supplier1 = () -> "result";
		Function<Exception, RuntimeException> mapper1 = e -> new RuntimeException("mapped: " + e.getMessage());
		String result = Utils.safe(supplier1, mapper1);
		assertEquals("result", result);

		// Test RuntimeException is rethrown (not mapped)
		ThrowingSupplier<String> supplier2 = () -> {
			throw new RuntimeException("original");
		};
		Function<Exception, RuntimeException> mapper2 = e -> new RuntimeException("mapped: " + e.getMessage());
		var re = assertThrows(RuntimeException.class, () -> Utils.safe(supplier2, mapper2));
		assertEquals("original", re.getMessage());

		// Test checked exception is mapped using the provided function
		ThrowingSupplier<String> supplier3 = () -> {
			throw new Exception("test exception");
		};
		Function<Exception, RuntimeException> mapper3 = e -> new IllegalArgumentException("custom: " + e.getMessage());
		var mapped = assertThrows(RuntimeException.class, () -> Utils.safe(supplier3, mapper3));
		assertEquals("custom: test exception", mapped.getMessage());
		assertTrue(mapped instanceof IllegalArgumentException);

		// Test with custom exception type
		@SuppressWarnings("serial")
		class CustomRuntimeException extends RuntimeException {
			CustomRuntimeException(String message, Throwable cause) {
				super(message, cause);
			}
		}
		var custom = assertThrows(CustomRuntimeException.class, () -> Utils.<String>safeSupplier(() -> {
			throw new Exception("test");
		}, e -> new CustomRuntimeException("wrapped", e)));
		assertEquals("wrapped", custom.getMessage());
		assertNotNull(custom.getCause());
		assertEquals(Exception.class, custom.getCause().getClass());
	}

	//====================================================================================================
	// safeOpt(ThrowingSupplier<T>)
	//====================================================================================================
	@Test
	void a052_safeOpt() {
		// Test normal execution
		var result = safeOpt(() -> "result");
		assertTrue(result.isPresent());
		assertEquals("result", result.get());

		// Test exception returns empty
		var empty = safeOpt(() -> {
			throw new Exception("test");
		});
		assertFalse(empty.isPresent());
	}

	//====================================================================================================
	// safeSupplier(ThrowableUtils.SupplierWithThrowable<T>)
	//====================================================================================================
	@Test
	void a053_safeSupplier() {
		// Test normal execution
		var result = safeSupplier(() -> "result");
		assertEquals("result", result);

		// Test RuntimeException is rethrown
		assertThrows(RuntimeException.class, () -> safeSupplier(() -> {
			throw new RuntimeException("test");
		}));

		// Test checked exception is wrapped
		assertThrows(RuntimeException.class, () -> safeSupplier(() -> {
			throw new Exception("test");
		}));
	}

	//====================================================================================================
	// safeSupplier(ThrowableUtils.SupplierWithThrowable<T>, Function<Throwable, RuntimeException>)
	//====================================================================================================
	@Test
	void a054_safeSupplier_withExceptionMapper() {
		// Test normal execution
		String result = Utils.<String>safeSupplier(() -> "result", e -> new RuntimeException("mapped: " + e.getMessage()));
		assertEquals("result", result);

		// Test RuntimeException is rethrown (not mapped)
		var re = assertThrows(RuntimeException.class, () -> Utils.<String>safeSupplier(() -> {
			throw new RuntimeException("original");
		}, e -> new RuntimeException("mapped: " + e.getMessage())));
		assertEquals("original", re.getMessage());

		// Test checked exception is mapped using the provided function
		var mapped = assertThrows(RuntimeException.class, () -> Utils.<String>safeSupplier(() -> {
			throw new Exception("test exception");
		}, e -> new IllegalArgumentException("custom: " + e.getMessage())));
		assertEquals("custom: test exception", mapped.getMessage());
		assertTrue(mapped instanceof IllegalArgumentException);

		// Test Error is mapped
		var mappedError = assertThrows(RuntimeException.class, () -> Utils.<String>safeSupplier(() -> {
			throw new Error("test error");
		}, e -> new IllegalStateException("error: " + e.getMessage())));
		assertEquals("error: test error", mappedError.getMessage());
		assertTrue(mappedError instanceof IllegalStateException);

		// Test with custom exception type
		@SuppressWarnings("serial")
		class CustomRuntimeException extends RuntimeException {
			CustomRuntimeException(String message, Throwable cause) {
				super(message, cause);
			}
		}
		var custom = assertThrows(CustomRuntimeException.class, () -> Utils.<String>safeSupplier(() -> {
			throw new Exception("test");
		}, e -> new CustomRuntimeException("wrapped", e)));
		assertEquals("wrapped", custom.getMessage());
		assertNotNull(custom.getCause());
		assertEquals(Exception.class, custom.getCause().getClass());
	}

	//====================================================================================================
	// sb(String)
	//====================================================================================================
	@Test
	void a055_sb() {
		var sb = sb("Hello");
		assertNotNull(sb);
		assertEquals("Hello", sb.toString());
	}

	//====================================================================================================
	// ss(Supplier<?>)
	//====================================================================================================
	@Test
	void a056_ss() {
		var stringSupplier = ss(() -> "test");
		assertEquals("test", stringSupplier.get());

		var intSupplier = ss(() -> 123);
		assertEquals("123", intSupplier.get());
	}

	//====================================================================================================
	// uc(String)
	//====================================================================================================
	@Test
	void a057_uc() {
		assertEquals("HELLO", uc("Hello"));
		assertEquals("HELLO", uc("hello"));
		assertEquals("HELLO WORLD", uc("Hello World"));
		assertNull(uc(null));
		assertEquals("", uc(""));
	}

	//====================================================================================================
	// unwrap(Object)
	//====================================================================================================
	@Test
	void a058_unwrap() {
		// Test with Supplier
		Supplier<String> supplier = () -> "test";
		assertEquals("test", unwrap(supplier));

		// Test with Value
		var value = Value.of("test");
		assertEquals("test", unwrap(value));

		// Test with Optional
		var optional = Optional.of("test");
		assertEquals("test", unwrap(optional));

		// Test nested unwrapping
		Supplier<Optional<String>> nested = () -> Optional.of("test");
		assertEquals("test", unwrap(nested));

		// Test regular object
		assertEquals("test", unwrap("test"));
		assertEquals(123, unwrap(123));
	}

	//====================================================================================================
	// nullIfEmpty(String)
	//====================================================================================================
	@Test
	void a059_nullIfEmpty_String() {
		assertNull(nullIfEmpty((String)null));
		assertNull(nullIfEmpty(""));
		assertNotNull(nullIfEmpty("x"));
		assertEquals("test", nullIfEmpty("test"));
	}

	//====================================================================================================
	// nullIfEmpty(Map)
	//====================================================================================================
	@Test
	void a060_nullIfEmpty_Map() {
		// Null map
		assertNull(nullIfEmpty((Map<String,Integer>)null));

		// Empty map
		Map<String,Integer> empty = new HashMap<>();
		assertNull(nullIfEmpty(empty));

		// Non-empty map
		Map<String,Integer> nonEmpty = new HashMap<>();
		nonEmpty.put("a", 1);
		assertNotNull(nullIfEmpty(nonEmpty));
		assertSame(nonEmpty, nullIfEmpty(nonEmpty));
		assertEquals(1, nullIfEmpty(nonEmpty).size());
	}

	//====================================================================================================
	// nullIfEmpty(List)
	//====================================================================================================
	@Test
	void a061_nullIfEmpty_List() {
		// Null list
		assertNull(nullIfEmpty((List<String>)null));

		// Empty list
		List<String> empty = new ArrayList<>();
		assertNull(nullIfEmpty(empty));

		// Non-empty list
		List<String> nonEmpty = new ArrayList<>();
		nonEmpty.add("a");
		assertNotNull(nullIfEmpty(nonEmpty));
		assertSame(nonEmpty, nullIfEmpty(nonEmpty));
		assertEquals(1, nullIfEmpty(nonEmpty).size());
	}

	//====================================================================================================
	// nullIfEmpty(Set)
	//====================================================================================================
	@Test
	void a062_nullIfEmpty_Set() {
		// Null set
		assertNull(nullIfEmpty((Set<String>)null));

		// Empty set
		Set<String> empty = new HashSet<>();
		assertNull(nullIfEmpty(empty));

		// Non-empty set
		Set<String> nonEmpty = new HashSet<>();
		nonEmpty.add("a");
		assertNotNull(nullIfEmpty(nonEmpty));
		assertSame(nonEmpty, nullIfEmpty(nonEmpty));
		assertEquals(1, nullIfEmpty(nonEmpty).size());
	}

	//====================================================================================================
	// e(Object) - Empty check
	//====================================================================================================
	@Test
	void a063_e() {
		assertTrue(e((String)null));
		assertTrue(e(""));
		assertTrue(e(Collections.emptyList()));
		assertTrue(e(Collections.emptyMap()));
		assertTrue(e(new String[0]));
		assertFalse(e("Hello"));
		assertFalse(e(Arrays.asList(1, 2)));
		assertFalse(e(Map.of("key", "value")));
		assertFalse(e(new String[]{"a"}));
	}

	//====================================================================================================
	// ne(Object) - Not-empty check
	//====================================================================================================
	@Test
	void a064_ne() {
		assertFalse(ne((String)null));
		assertFalse(ne(""));
		assertFalse(ne(Collections.emptyList()));
		assertFalse(ne(Collections.emptyMap()));
		assertFalse(ne(new String[0]));
		assertTrue(ne("Hello"));
		assertTrue(ne(Arrays.asList(1, 2)));
		assertTrue(ne(Map.of("key", "value")));
		assertTrue(ne(new String[]{"a"}));
	}

	//====================================================================================================
	// n(Object) - Null check
	//====================================================================================================
	@Test
	void a065_n() {
		assertTrue(n((Object)null));
		assertFalse(n("Hello"));
		assertFalse(n(123));
		assertFalse(n(new Object()));
	}


	//====================================================================================================
	// lt(T, T) - Less than
	//====================================================================================================
	@Test
	void a067_lt() {
		assertTrue(lt(5, 10));
		assertTrue(lt("apple", "banana"));
		assertFalse(lt(10, 5));
		assertFalse(lt(5, 5));
		assertFalse(lt("banana", "apple"));
		assertTrue(lt(null, "apple")); // null is less than non-null
		assertFalse(lt("apple", null));
	}

	//====================================================================================================
	// lte(T, T) - Less than or equal
	//====================================================================================================
	@Test
	void a068_lte() {
		assertTrue(lte(5, 10));
		assertTrue(lte(5, 5));
		assertFalse(lte(10, 5));
		assertTrue(lte("apple", "banana"));
		assertTrue(lte("apple", "apple"));
		assertTrue(lte(null, null)); // null equals null
		assertTrue(lte(null, "apple")); // null is less than non-null
		assertFalse(lte("apple", null));
	}

	//====================================================================================================
	// gt(T, T) - Greater than
	//====================================================================================================
	@Test
	void a069_gt() {
		assertTrue(gt(10, 5));
		assertTrue(gt("banana", "apple"));
		assertFalse(gt(5, 10));
		assertFalse(gt(5, 5));
		assertFalse(gt("apple", "banana"));
		assertFalse(gt(null, "apple")); // null is not greater
		assertTrue(gt("apple", null)); // non-null is greater than null
	}

	//====================================================================================================
	// gte(T, T) - Greater than or equal
	//====================================================================================================
	@Test
	void a070_gte() {
		assertTrue(gte(10, 5));
		assertTrue(gte(5, 5));
		assertFalse(gte(5, 10));
		assertTrue(gte("banana", "apple"));
		assertTrue(gte("apple", "apple"));
		assertTrue(gte(null, null)); // null equals null
		assertFalse(gte(null, "apple")); // null is not greater
		assertTrue(gte("apple", null)); // non-null is greater than null
	}

	//====================================================================================================
	// b(String) - Blank check
	//====================================================================================================
	@Test
	void a071_b() {
		assertTrue(b(null));
		assertTrue(b(""));
		assertTrue(b("   "));
		assertTrue(b("\t\n"));
		assertFalse(b("hello"));
		assertFalse(b("  hello  "));
	}

	//====================================================================================================
	// nb(String) - Not-blank check
	//====================================================================================================
	@Test
	void a072_nb() {
		assertFalse(nb(null));
		assertFalse(nb(""));
		assertFalse(nb("   "));
		assertFalse(nb("\t\n"));
		assertTrue(nb("hello"));
		assertTrue(nb("  hello  "));
	}

	//====================================================================================================
	// tr(String) - Trim
	//====================================================================================================
	@Test
	void a073_tr() {
		assertEquals("hello", tr("  hello  "));
		assertEquals("hello", tr("hello"));
		assertNull(tr(null));
		assertEquals("", tr("   "));
		assertEquals("a b", tr("  a b  "));
	}

	//====================================================================================================
	// sw(String, String) - Starts with
	//====================================================================================================
	@Test
	void a074_sw() {
		assertTrue(sw("hello", "he"));
		assertTrue(sw("hello", "hello"));
		assertFalse(sw("hello", "lo"));
		assertFalse(sw("hello", "xyz"));
		assertFalse(sw(null, "he"));
		assertFalse(sw("hello", null));
		assertFalse(sw(null, null));
	}

	//====================================================================================================
	// ew(String, String) - Ends with
	//====================================================================================================
	@Test
	void a075_ew() {
		assertTrue(ew("hello", "lo"));
		assertTrue(ew("hello", "hello"));
		assertFalse(ew("hello", "he"));
		assertFalse(ew("hello", "xyz"));
		assertFalse(ew(null, "lo"));
		assertFalse(ew("hello", null));
		assertFalse(ew(null, null));
	}

	//====================================================================================================
	// co(String, String) - Contains
	//====================================================================================================
	@Test
	void a076_co() {
		assertTrue(co("hello", "ell"));
		assertTrue(co("hello", "hello"));
		assertTrue(co("hello", "h"));
		assertTrue(co("hello", "o"));
		assertFalse(co("hello", "xyz"));
		assertFalse(co(null, "ell"));
		assertFalse(co("hello", null));
		assertFalse(co(null, null));
	}

	//====================================================================================================
	// or(T...) - First non-null
	//====================================================================================================
	@Test
	void a077_or() {
		// Use explicit type parameters to avoid ambiguity with or(boolean...)
		assertEquals("Hello", Utils.<String>or(null, null, "Hello", "World"));
		assertEquals("Hello", Utils.<String>or("Hello", "World"));
		assertNull(Utils.<String>or(null, null));
		assertNull(Utils.<String>or((String[])null));
		assertEquals("First", Utils.<String>or("First", "Second"));
		assertEquals(123, Utils.<Integer>or(null, 123, 456));
	}

	//====================================================================================================
	// def(T, T) - Default value
	//====================================================================================================
	@Test
	void a078_def() {
		assertEquals("Hello", def("Hello", "World"));
		assertEquals("World", def(null, "World"));
		assertEquals(123, def(123, 456));
		assertEquals(456, def(null, 456));
		assertNull(def(null, null));
	}

	//====================================================================================================
	// and(boolean...) - Boolean AND
	//====================================================================================================
	@Test
	void a079_and() {
		assertTrue(and(true, true, true));
		assertFalse(and(true, false, true));
		assertFalse(and(false, false, false));
		assertTrue(and()); // Empty array (vacuous truth)
		assertTrue(and(true));
		assertFalse(and(false));
	}

	//====================================================================================================
	// or(boolean...) - Boolean OR
	//====================================================================================================
	@Test
	void a080_or_boolean() {
		// Use explicit boolean array to avoid ambiguity with or(T...)
		boolean[] arr1 = {true, false, false};
		assertTrue(or(arr1));
		boolean[] arr2 = {false, true, false};
		assertTrue(or(arr2));
		boolean[] arr3 = {false, false, false};
		assertFalse(or(arr3));
		boolean[] arr4 = {};
		assertFalse(or(arr4)); // Empty array
		boolean[] arr5 = {true};
		assertTrue(or(arr5));
		boolean[] arr6 = {false};
		assertFalse(or(arr6));
	}

	//====================================================================================================
	// not(boolean) - Boolean NOT
	//====================================================================================================
	@Test
	void a081_not() {
		assertFalse(not(true));
		assertTrue(not(false));
	}

	//====================================================================================================
	// min(T, T) - Minimum
	//====================================================================================================
	@Test
	void a082_min() {
		assertEquals(5, min(5, 10));
		assertEquals(5, min(10, 5));
		assertEquals(5, min(5, 5));
		assertEquals("apple", min("apple", "banana"));
		assertEquals("apple", min("banana", "apple"));
		assertEquals(10, min(null, 10));
		assertEquals(10, min(10, null));
		assertNull(min(null, null));
	}

	//====================================================================================================
	// max(T, T) - Maximum
	//====================================================================================================
	@Test
	void a083_max() {
		assertEquals(10, max(5, 10));
		assertEquals(10, max(10, 5));
		assertEquals(10, max(10, 10));
		assertEquals("banana", max("apple", "banana"));
		assertEquals("banana", max("banana", "apple"));
		assertEquals(10, max(null, 10));
		assertEquals(10, max(10, null));
		assertNull(max(null, null));
	}

	//====================================================================================================
	// abs(Number) - Absolute value
	//====================================================================================================
	@Test
	void a084_abs() {
		assertEquals(5, abs(-5));
		assertEquals(5, abs(5));
		assertEquals(3.14, abs(-3.14));
		assertEquals(3.14, abs(3.14));
		assertEquals(10L, abs(-10L));
		assertEquals(10L, abs(10L));
		assertEquals((short)5, abs((short)-5));
		assertEquals((byte)5, abs((byte)-5));
		assertNull(abs(null));
	}
}

