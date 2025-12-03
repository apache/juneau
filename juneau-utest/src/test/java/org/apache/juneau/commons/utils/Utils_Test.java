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

import java.lang.annotation.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.function.*;
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
		assertTrue(b(true));
		assertTrue(b("true"));
		assertTrue(b("TRUE"));
		assertFalse(b(false));
		assertFalse(b("false"));
		assertFalse(b("FALSE"));
		assertFalse(b(null));
		assertFalse(b(""));
		assertFalse(b(123));
		assertFalse(b(new Object()));
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
	// castOrNull(Object, Class<T>)
	//====================================================================================================
	@Test
	void a003_castOrNull() {
		var obj = "Hello";
		assertEquals("Hello", castOrNull(obj, String.class));
		assertNull(castOrNull(obj, Integer.class));
		assertNull(castOrNull(null, String.class));
		assertEquals(123, castOrNull(123, Integer.class));
		assertNull(castOrNull(123, String.class));
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
	// compare(Object, Object)
	//====================================================================================================
	@Test
	void a007_compare() {
		assertTrue(compare("apple", "banana") < 0);
		assertTrue(compare("banana", "apple") > 0);
		assertEquals(0, compare("apple", "apple"));
		assertEquals(0, compare(null, null));
		assertTrue(compare(null, "apple") < 0);
		assertTrue(compare("apple", null) > 0);
		assertTrue(compare(5, 10) < 0);
		assertTrue(compare(10, 5) > 0);
		assertEquals(0, compare(5, 5));
		assertEquals(0, compare("apple", 5)); // Different types, cannot compare

		// Test line 285 branch: same class but not Comparable
		class NotComparable {}
		var nc1 = new NotComparable();
		var nc2 = new NotComparable();
		assertEquals(0, compare(nc1, nc2)); // Same class, but not Comparable - covers missing branch
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
	// env(String, T)
	//====================================================================================================
	@Test
	void a011_env_withDefault() {
		// Test with system property - covers toType line 1618-1619 (String)
		System.setProperty("test.property2", "testValue2");
		var result = env("test.property2", "default");
		assertEquals("testValue2", result);
		System.clearProperty("test.property2");

		// Test with default value
		var defaultValue = env("nonexistent.property.xyz2", "default");
		assertEquals("default", defaultValue);

		// Test with Boolean type - covers toType line 1622-1625 (ENV_FUNCTIONS)
		System.setProperty("test.boolean", "true");
		var boolResult = env("test.boolean", false);
		assertTrue(boolResult);
		System.clearProperty("test.boolean");

		// Test with Enum type - covers toType line 1620-1621 (Enum)
		enum TestEnum { VALUE1, VALUE2 }
		System.setProperty("test.enum", "VALUE1");
		var enumResult = env("test.enum", TestEnum.VALUE2);
		assertEquals(TestEnum.VALUE1, enumResult);
		System.clearProperty("test.enum");

		// Test null default - covers toType line 1615-1616 (def == null)
		// When property doesn't exist, toType is never called, so test via reflection
		// Also test with property set to ensure toType is called with s != null && def == null
		System.setProperty("test.null.def", "value");
		var nullResult = env("test.null.def", (String)null);
		assertNull(nullResult); // toType returns null when def is null
		System.clearProperty("test.null.def");
		
		// Also test when property doesn't exist (returns def directly without calling toType)
		var nullResult2 = env("nonexistent.property.null", (String)null);
		assertNull(nullResult2);

		// Test null string - covers toType line 1615-1616 (s == null, def != null)
		// Use reflection to call private toType method directly
		try {
			var toTypeMethod = Utils.class.getDeclaredMethod("toType", String.class, Object.class);
			toTypeMethod.setAccessible(true);
			var nullStringResult = (String)toTypeMethod.invoke(null, (String)null, "default");
			assertNull(nullStringResult);
		} catch (Exception e) {
			fail("Failed to test toType with null string: " + e.getMessage());
		}

		// Test both null - covers toType line 1615-1616 (s == null && def == null)
		// Use reflection to call private toType method directly
		try {
			var toTypeMethod = Utils.class.getDeclaredMethod("toType", String.class, Object.class);
			toTypeMethod.setAccessible(true);
			var bothNullResult = (String)toTypeMethod.invoke(null, (String)null, (String)null);
			assertNull(bothNullResult);
		} catch (Exception e) {
			fail("Failed to test toType with both null: " + e.getMessage());
		}
		
		// Test s != null && def == null via reflection to ensure line 1615 is fully covered
		try {
			var toTypeMethod = Utils.class.getDeclaredMethod("toType", String.class, Object.class);
			toTypeMethod.setAccessible(true);
			var nonNullStringNullDef = (String)toTypeMethod.invoke(null, "value", (String)null);
			assertNull(nonNullStringNullDef); // toType returns null when def is null
		} catch (Exception e) {
			fail("Failed to test toType with non-null string and null def: " + e.getMessage());
		}

		// Test invalid type - covers toType line 1623-1624 (exception)
		// Note: Charset doesn't work because def.getClass() returns concrete implementation class
		System.setProperty("test.invalid", "value");
		assertThrows(RuntimeException.class, () -> env("test.invalid", 123)); // Integer not in ENV_FUNCTIONS
		System.clearProperty("test.invalid");
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
		assertThrows(IllegalArgumentException.class, ()->nullSupplier.get());

		// Empty pattern
		var emptySupplier = fs("");
		assertEquals("", emptySupplier.get());
	}

	//====================================================================================================
	// hash(Object...)
	//====================================================================================================
	@Test
	void a020_hash() {
		var hash1 = hash("Hello", 123, true);
		var hash2 = hash("Hello", 123, true);
		assertEquals(hash1, hash2);

		var hash3 = hash("Hello", 123, false);
		assertNotEquals(hash1, hash3);

		// Test with annotations
		@TestAnnotation("test")
		class T {}
		var a1 = T.class.getAnnotation(TestAnnotation.class);
		var hash4 = hash(a1, "value");
		assertNotNull(hash4);
	}

	//====================================================================================================
	// identity(Object)
	//====================================================================================================
	@Test
	void a021_identity() {
		var obj = "test";
		var identity = identity(obj);
		assertNotNull(identity);
		assertTrue(identity.contains("String"));
		assertTrue(identity.contains("@"));
		assertNull(identity(null));
		assertNotNull(identity(Optional.of("test")));
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
	// isEmpty(CharSequence)
	//====================================================================================================
	@Test
	void a024_isEmpty_CharSequence() {
		assertTrue(isEmpty((String)null));
		assertTrue(isEmpty(""));
		assertFalse(isEmpty("   "));
		assertFalse(isEmpty("hello"));
		assertFalse(isEmpty("a"));
	}

	//====================================================================================================
	// isEmpty(Collection<?>)
	//====================================================================================================
	@Test
	void a025_isEmpty_Collection() {
		assertTrue(isEmpty((Collection<?>)null));
		assertTrue(isEmpty(Collections.emptyList()));
		assertTrue(isEmpty(new ArrayList<>()));
		assertFalse(isEmpty(Arrays.asList(1, 2, 3)));
		assertFalse(isEmpty(Collections.singletonList("test")));
	}

	//====================================================================================================
	// isEmpty(Map<?,?>)
	//====================================================================================================
	@Test
	void a026_isEmpty_Map() {
		assertTrue(isEmpty((Map<?,?>)null));
		assertTrue(isEmpty(Collections.emptyMap()));
		assertTrue(isEmpty(new HashMap<>()));
		assertFalse(isEmpty(Map.of("key", "value")));
		assertFalse(isEmpty(Collections.singletonMap("key", "value")));
	}

	//====================================================================================================
	// isEmpty(Object)
	//====================================================================================================
	@Test
	void a027_isEmpty_Object() {
		assertTrue(isEmpty((Object)null));
		assertTrue(isEmpty((Object)""));
		// Test line 834: Collection branch
		assertTrue(isEmpty((Object)Collections.emptyList()));
		assertFalse(isEmpty((Object)Arrays.asList(1, 2)));  // Non-empty collection
		// Test line 836: Map branch
		assertTrue(isEmpty((Object)Collections.emptyMap()));
		assertFalse(isEmpty((Object)Map.of("key", "value")));  // Non-empty map
		assertTrue(isEmpty(new int[0]));
		assertTrue(isEmpty(new String[0]));
		assertFalse(isEmpty((Object)"hello"));
		assertFalse(isEmpty(a(1, 2)));
		assertTrue(isEmpty(new Object() {
			@Override public String toString() { return ""; }
		}));
	}

	//====================================================================================================
	// isNotEmpty(CharSequence)
	//====================================================================================================
	@Test
	void a028_isNotEmpty_CharSequence() {
		assertFalse(isNotEmpty((String)null));
		assertFalse(isNotEmpty(""));
		assertTrue(isNotEmpty("   "));
		assertTrue(isNotEmpty("hello"));
		assertTrue(isNotEmpty("a"));
	}

	//====================================================================================================
	// isNotEmpty(Collection<?>)
	//====================================================================================================
	@Test
	void a029_isNotEmpty_Collection() {
		assertFalse(isNotEmpty((Collection<?>)null));
		assertFalse(isNotEmpty(Collections.emptyList()));
		assertFalse(isNotEmpty(new ArrayList<>()));
		assertTrue(isNotEmpty(Arrays.asList(1, 2, 3)));
		assertTrue(isNotEmpty(Collections.singletonList("test")));
	}

	//====================================================================================================
	// isNotEmpty(Map<?,?>)
	//====================================================================================================
	@Test
	void a030_isNotEmpty_Map() {
		assertFalse(isNotEmpty((Map<?,?>)null));
		assertFalse(isNotEmpty(Collections.emptyMap()));
		assertFalse(isNotEmpty(new HashMap<>()));
		assertTrue(isNotEmpty(Map.of("key", "value")));
		assertTrue(isNotEmpty(Collections.singletonMap("key", "value")));
	}

	//====================================================================================================
	// isNotEmpty(Object)
	//====================================================================================================
	@Test
	void a031_isNotEmpty_Object() {
		assertFalse(isNotEmpty((Object)null));
		// Test line 939: CharSequence branch
		assertFalse(isNotEmpty((Object)""));
		assertTrue(isNotEmpty((Object)"hello"));  // Non-empty CharSequence
		// Test line 941: Collection branch
		assertFalse(isNotEmpty((Object)Collections.emptyList()));
		assertTrue(isNotEmpty((Object)Arrays.asList(1, 2)));  // Non-empty collection
		// Test line 943: Map branch
		assertFalse(isNotEmpty((Object)Collections.emptyMap()));
		assertTrue(isNotEmpty((Object)Map.of("key", "value")));  // Non-empty map
		assertFalse(isNotEmpty(new int[0]));
		assertFalse(isNotEmpty(new String[0]));
		assertTrue(isNotEmpty(a(1, 2)));
		// Test line 946: fallback case (non-String, non-Collection, non-Map, non-Array)
		assertTrue(isNotEmpty(new Object() {
			@Override public String toString() { return "test"; }
		}));
		assertFalse(isNotEmpty(new Object() {
			@Override public String toString() { return ""; }
		}));
	}

	//====================================================================================================
	// isNotMinusOne(T)
	//====================================================================================================
	@Test
	void a032_isNotMinusOne() {
		assertTrue(isNotMinusOne(5));
		assertTrue(isNotMinusOne(0));
		assertTrue(isNotMinusOne(100));
		assertFalse(isNotMinusOne(-1));
		assertFalse(isNotMinusOne((Integer)null));
		assertTrue(isNotMinusOne(5L));
		assertFalse(isNotMinusOne(-1L));
	}

	//====================================================================================================
	// isNotNull(T)
	//====================================================================================================
	@Test
	void a033_isNotNull() {
		assertTrue(isNotNull("Hello"));
		assertTrue(isNotNull(123));
		assertTrue(isNotNull(new Object()));
		assertFalse(isNotNull(null));
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
	// memoize(Supplier<T>)
	//====================================================================================================
	@Test
	void a036_memoize() {
		var callCount = new AtomicInteger(0);
		var supplier = memoize(() -> {
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
	// memoizeResettable(Supplier<T>)
	//====================================================================================================
	@Test
	void a037_memoizeResettable() {
		var callCount = new AtomicInteger(0);
		var supplier = memoizeResettable(() -> {
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
		assertNull(n(String.class));
		assertNull(n(Integer.class));
		assertNull(n(List.class));
	}

	//====================================================================================================
	// ne(T, T)
	//====================================================================================================
	@Test
	void a039_ne() {
		assertTrue(ne("Hello", "World"));
		assertFalse(ne("Hello", "Hello"));
		assertFalse(ne(null, null));
		assertTrue(ne("Hello", null));
		assertTrue(ne(null, "Hello"));
		assertTrue(ne(123, 456));
		assertFalse(ne(123, 123));
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

		assertFalse(ne(r1, r2, (x, y) -> x.id == y.id && x.name.equals(y.name)));
		assertTrue(ne(r1, r3, (x, y) -> x.id == y.id && x.name.equals(y.name)));
		assertFalse(ne(null, null, (x, y) -> true));
		assertTrue(ne(r1, null, (x, y) -> false));
		assertTrue(ne(null, r2, (x, y) -> false));
		assertFalse(ne(r1, r1, (x, y) -> true)); // Same reference
	}

	//====================================================================================================
	// neic(String, String)
	//====================================================================================================
	@Test
	void a041_neic() {
		assertTrue(neic("Hello", "World"));
		assertFalse(neic("Hello", "hello"));
		assertFalse(neic("Hello", "HELLO"));
		assertFalse(neic(null, null));
		assertTrue(neic("Hello", null));
		assertTrue(neic(null, "Hello"));
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
		printLines(a("Line 1", "Line 2", "Line 3"));
		printLines(a());
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
		safe((Snippet)() -> {
			count.incrementAndGet();
		});
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
		safe((Snippet)() -> {
			count.incrementAndGet();
		}, e -> new RuntimeException("mapped: " + e.getMessage()));
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
}

