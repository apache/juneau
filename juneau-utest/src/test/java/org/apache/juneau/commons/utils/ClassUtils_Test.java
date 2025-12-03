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

import static org.apache.juneau.TestUtils.assertThrowsWithMessage;
import static org.apache.juneau.commons.utils.ClassUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.reflect.ClassInfo;
import org.apache.juneau.commons.utils.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({"serial","unused"})
class ClassUtils_Test {

	//====================================================================================================
	// Constructor (line 35)
	//====================================================================================================
	@Test
	void a00_constructor() {
		// Test line 35: class instantiation
		// ClassUtils has an implicit public no-arg constructor
		var instance = new ClassUtils();
		assertNotNull(instance);
	}

	//====================================================================================================
	// canAddTo(Collection<?>)
	//====================================================================================================
	@Test
	void a001_canAddTo() {
		// Modifiable collections
		assertTrue(canAddTo(new ArrayList<>()));
		assertTrue(canAddTo(new LinkedList<>()));
		assertTrue(canAddTo(new HashSet<>()));
		assertTrue(canAddTo(new LinkedHashSet<>()));
		assertTrue(canAddTo(new TreeSet<>()));
		assertTrue(canAddTo(new Vector<>()));
		
		// Unmodifiable collections
		assertFalse(canAddTo(Collections.unmodifiableList(new ArrayList<>())));
		assertFalse(canAddTo(Collections.unmodifiableSet(new HashSet<>())));
		assertFalse(canAddTo(Collections.unmodifiableCollection(new ArrayList<>())));
		assertFalse(canAddTo(Arrays.asList("a", "b"))); // Arrays$ArrayList
		
		// Java 9+ immutable collections
		if (List.of("a").getClass().getName().contains("Immutable")) {
			assertFalse(canAddTo(List.of("a")));
			assertFalse(canAddTo(Set.of("a")));
		}
		
		// Should throw when null
		assertThrows(IllegalArgumentException.class, () -> {
			canAddTo((Collection<?>)null);
		});
	}

	//====================================================================================================
	// canPutTo(Map<?,?>)
	//====================================================================================================
	@Test
	void a002_canPutTo() {
		// Modifiable maps
		assertTrue(canPutTo(new HashMap<>()));
		assertTrue(canPutTo(new LinkedHashMap<>()));
		assertTrue(canPutTo(new TreeMap<>()));
		assertTrue(canPutTo(new Hashtable<>()));
		
		// Unmodifiable maps
		assertFalse(canPutTo(Collections.unmodifiableMap(new HashMap<>())));
		
		// Java 9+ immutable maps
		if (Map.of("a", "b").getClass().getName().contains("Immutable")) {
			assertFalse(canPutTo(Map.of("a", "b")));
		}
		
		// Should throw when null
		assertThrows(IllegalArgumentException.class, () -> {
			canPutTo((Map<?,?>)null);
		});
	}

	//====================================================================================================
	// className(Object)
	//====================================================================================================
	@Test
	void a003_className() {
		// With Class
		assertEquals("java.lang.String", cn(String.class));
		assertEquals("java.util.ArrayList", cn(ArrayList.class));
		
		// With Object
		assertEquals("java.util.HashMap", cn(new HashMap<>()));
		assertEquals("java.lang.String", cn("test"));
		
		// With primitive
		assertEquals("int", cn(int.class));
		assertEquals("boolean", cn(boolean.class));
		
		// With array
		assertEquals("[Ljava.lang.String;", cn(String[].class));
		assertEquals("[I", cn(int[].class));
		assertEquals("[[Ljava.lang.String;", cn(String[][].class));
		
		// With inner class
		assertEquals("java.util.Map$Entry", cn(Map.Entry.class));
		
		// With null
		assertNull(cn(null));
	}

	//====================================================================================================
	// classNameSimple(Object)
	//====================================================================================================
	@Test
	void a004_classNameSimple() {
		// With Class
		assertEquals("String", cns(String.class));
		assertEquals("ArrayList", cns(ArrayList.class));
		
		// With Object
		assertEquals("HashMap", cns(new HashMap<>()));
		assertEquals("String", cns("test"));
		
		// With primitive
		assertEquals("int", cns(int.class));
		assertEquals("boolean", cns(boolean.class));
		
		// With array
		assertEquals("String[]", cns(String[].class));
		assertEquals("int[]", cns(int[].class));
		assertEquals("String[][]", cns(String[][].class));
		
		// With inner class
		assertEquals("Entry", cns(Map.Entry.class));
		
		// With null
		assertNull(cns(null));
		
		// With ClassInfo (line 188-189)
		var classInfo = ClassInfo.of(String.class);
		assertEquals("String", cns(classInfo));
		
		var listClassInfo = ClassInfo.of(ArrayList.class);
		assertEquals("ArrayList", cns(listClassInfo));
	}

	//====================================================================================================
	// classNameSimpleQualified(Object)
	//====================================================================================================
	@Test
	void a005_classNameSimpleQualified() {
		// Top-level class
		assertEquals("String", cnsq(String.class));
		assertEquals("ArrayList", cnsq(ArrayList.class));
		
		// Inner class
		assertEquals("Map.Entry", cnsq(Map.Entry.class));
		
		// Nested inner class
		class Outer {
			class Inner {
				class Deep {}
			}
		}
		var deepClass = Outer.Inner.Deep.class;
		var result = cnsq(deepClass);
		// Result will be something like "ClassUtils_2_Test.1Outer.Inner.Deep"
		assertTrue(result.endsWith("Outer.Inner.Deep"), result);
		assertFalse(result.contains("$"), result);
		
		// With Object
		var obj = new HashMap<>();
		assertEquals("HashMap", cnsq(obj));
		
		// With null
		assertNull(cnsq(null));
		
		// Anonymous class
		var anon = new Object() {};
		var anonResult = cnsq(anon);
		// Anonymous classes have names like "ClassUtils_2_Test$1"
		// After conversion should be like "ClassUtils_2_Test.1"
		assertNotNull(anonResult);
		assertFalse(anonResult.contains("$"), anonResult);
		
		// Array types
		assertEquals("String[]", cnsq(String[].class));
		assertEquals("String[][]", cnsq(String[][].class));
		assertEquals("int[]", cnsq(int[].class));
		assertEquals("Map.Entry[]", cnsq(Map.Entry[].class));
		
		// Array objects
		var stringArray = new String[]{"a", "b"};
		assertEquals("String[]", cnsq(stringArray));
		
		var intArray = new int[]{1, 2, 3};
		assertEquals("int[]", cnsq(intArray));
		
		var multiDimArray = new String[][]{{"a"}};
		assertEquals("String[][]", cnsq(multiDimArray));
	}

	//====================================================================================================
	// getClasses(Object...)
	//====================================================================================================
	@Test
	void a006_getClasses() {
		// Basic usage
		Class<?>[] classes = getClasses("test", 123, new HashMap<>());
		assertEquals(3, classes.length);
		assertEquals(String.class, classes[0]);
		assertEquals(Integer.class, classes[1]);
		assertEquals(HashMap.class, classes[2]);
		
		// With null
		Class<?>[] classes2 = getClasses("test", null, 123);
		assertEquals(3, classes2.length);
		assertEquals(String.class, classes2[0]);
		assertNull(classes2[1]);
		assertEquals(Integer.class, classes2[2]);
		
		// Empty
		var classes3 = getClasses();
		assertEquals(0, classes3.length);
		
		// All null
		var classes4 = getClasses(null, null, null);
		assertEquals(3, classes4.length);
		assertNull(classes4[0]);
		assertNull(classes4[1]);
		assertNull(classes4[2]);
	}

	//====================================================================================================
	// getMatchingArgs(Class<?>[], Object...)
	//====================================================================================================
	@Test
	void a007_getMatchingArgs() {
		// Exact match - fast path returns original array
		var paramTypes = a(String.class, Integer.class);
		var args = a("test", 123);
		var result = getMatchingArgs(paramTypes, (Object[])args);
		assertSame(args, result);
		assertEquals("test", result[0]);
		assertEquals(123, result[1]);
		
		// Wrong order - method reorders them
		var paramTypes2 = a(Integer.class, String.class);
		var args2 = a("test", 123);
		var result2 = getMatchingArgs(paramTypes2, (Object[])args2);
		assertEquals(2, result2.length);
		assertEquals(123, result2[0]);
		assertEquals("test", result2[1]);
		
		// Extra args - ignored
		var paramTypes3 = a(String.class);
		var args3 = a("test", 123, true);
		var result3 = getMatchingArgs(paramTypes3, (Object[])args3);
		assertEquals(1, result3.length);
		assertEquals("test", result3[0]);
		
		// Missing args - become null
		var paramTypes4 = a(String.class, Integer.class, Boolean.class);
		var args4 = a("test");
		var result4 = getMatchingArgs(paramTypes4, (Object[])args4);
		assertEquals(3, result4.length);
		assertEquals("test", result4[0]);
		assertNull(result4[1]);
		assertNull(result4[2]);
		
		// Primitive types
		var paramTypes5 = a(int.class, String.class);
		var args5 = a("test", 123);
		var result5 = getMatchingArgs(paramTypes5, (Object[])args5);
		assertEquals(2, result5.length);
		assertEquals(123, result5[0]);
		assertEquals("test", result5[1]);
		
		// Type hierarchy
		var paramTypes6 = a(Number.class, String.class);
		var args6 = a("test", 123);
		var result6 = getMatchingArgs(paramTypes6, (Object[])args6);
		assertEquals(2, result6.length);
		assertEquals(123, result6[0]);
		assertEquals("test", result6[1]);
		
		// Null args
		var paramTypes7 = a(String.class, Integer.class);
		var args7 = new Object[] {null, null};
		var result7 = getMatchingArgs(paramTypes7, args7);
		assertEquals(2, result7.length);
		assertNull(result7[0]);
		assertNull(result7[1]);
		
		// Null paramTypes - should throw
		assertThrows(NullPointerException.class, () -> {
			getMatchingArgs(null, "test");
		});
	}

	//====================================================================================================
	// getProxyFor(Object)
	//====================================================================================================
	@Test
	void a008_getProxyFor() {
		// Null
		assertNull(getProxyFor(null));
		
		// Regular object
		var obj = "test";
		assertNull(getProxyFor(obj));
		
		// JDK dynamic proxy
		var proxy = Proxy.newProxyInstance(
			Thread.currentThread().getContextClassLoader(),
			new Class[]{List.class},
			(proxy1, method, args) -> null
		);
		var result = getProxyFor(proxy);
		assertEquals(List.class, result);
		
		// JDK dynamic proxy with no interfaces
		var proxy2 = Proxy.newProxyInstance(
			Thread.currentThread().getContextClassLoader(),
			new Class[0],
			(proxy1, method, args) -> null
		);
		var result2 = getProxyFor(proxy2);
		assertNull(result2);
	}

	//====================================================================================================
	// isNotVoid(Class)
	//====================================================================================================
	@Test
	void a009_isNotVoid() {
		// Void classes
		assertFalse(isNotVoid(void.class));
		assertFalse(isNotVoid(Void.class));
		assertFalse(isNotVoid(null));
		
		// Non-void classes
		assertTrue(isNotVoid(String.class));
		assertTrue(isNotVoid(int.class));
		assertTrue(isNotVoid(Object.class));
		
		// NOT_VOID predicate
		assertFalse(NOT_VOID.test(void.class));
		assertFalse(NOT_VOID.test(Void.class));
		assertTrue(NOT_VOID.test(String.class));
		assertTrue(NOT_VOID.test(int.class));
	}

	//====================================================================================================
	// isVoid(Class)
	//====================================================================================================
	@Test
	void a010_isVoid() {
		// Void classes
		assertTrue(isVoid(void.class));
		assertTrue(isVoid(Void.class));
		assertTrue(isVoid(null));
		
		// Non-void classes
		assertFalse(isVoid(String.class));
		assertFalse(isVoid(int.class));
		assertFalse(isVoid(Object.class));
	}

	//====================================================================================================
	// setAccessible(Constructor<?>)
	//====================================================================================================
	@Test
	void a011_setAccessible_constructor() throws Exception {
		var ctor = String.class.getDeclaredConstructor();
		// Should succeed (no security manager in tests typically)
		assertTrue(setAccessible(ctor));
		
		// Should throw when null
		assertThrowsWithMessage(IllegalArgumentException.class, l("x", "cannot be null"), () -> {
			setAccessible((Constructor<?>)null);
		});
	}

	//====================================================================================================
	// setAccessible(Field)
	//====================================================================================================
	@Test
	void a012_setAccessible_field() throws Exception {
		// Use a field from a test class, not from java.lang (which has module restrictions)
		class TestClass {
			private String field;
		}
		var field = TestClass.class.getDeclaredField("field");
		// Should succeed (no security manager in tests typically)
		assertTrue(setAccessible(field));
		
		// Should throw when null
		assertThrowsWithMessage(IllegalArgumentException.class, l("x", "cannot be null"), () -> {
			setAccessible((Field)null);
		});
	}

	//====================================================================================================
	// setAccessible(Method)
	//====================================================================================================
	@Test
	void a013_setAccessible_method() throws Exception {
		var method = String.class.getDeclaredMethod("indexOf", String.class, int.class);
		// Should succeed (no security manager in tests typically)
		assertTrue(setAccessible(method));
		
		// Should throw when null
		assertThrowsWithMessage(IllegalArgumentException.class, l("x", "cannot be null"), () -> {
			setAccessible((Method)null);
		});
	}

	//====================================================================================================
	// toClass(Type)
	//====================================================================================================
	@Test
	void a014_toClass() throws Exception {
		// With Class
		assertSame(String.class, toClass(String.class));
		assertSame(Integer.class, toClass(Integer.class));
		
		// With ParameterizedType
		class TestClass {
			List<String> field;
		}
		var field = TestClass.class.getDeclaredField("field");
		var genericType = field.getGenericType();
		var result = toClass(genericType);
		assertEquals(List.class, result);
		
		// With TypeVariable - cannot be converted
		class TestClass2<T> {
			T field;
		}
		var field2 = TestClass2.class.getDeclaredField("field");
		var genericType2 = field2.getGenericType();
		var result2 = toClass(genericType2);
		assertNull(result2);
	}
}

