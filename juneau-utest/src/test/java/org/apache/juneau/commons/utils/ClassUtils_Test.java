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

import static org.apache.juneau.commons.utils.ClassUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.utils.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link ClassUtils}.
 */
@SuppressWarnings({"serial","unused"})
class ClassUtils_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// simpleQualifiedClassName tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_simpleQualifiedClassName_topLevelClass() {
		assertEquals("String", cnsq(String.class));
		assertEquals("ArrayList", cnsq(ArrayList.class));
	}

	@Test
	public void a02_simpleQualifiedClassName_innerClass() {
		assertEquals("Map.Entry", cnsq(Map.Entry.class));
	}

	@Test
	public void a03_simpleQualifiedClassName_nestedInnerClass() {
		class Outer {
			class Inner {
				class Deep {}
			}
		}
		var deepClass = Outer.Inner.Deep.class;
		var result = cnsq(deepClass);
		// Result will be something like "ClassUtils_Test.1Outer.Inner.Deep"
		assertTrue(result.endsWith("Outer.Inner.Deep"), result);
		assertFalse(result.contains("$"), result);
	}

	@Test
	public void a04_simpleQualifiedClassName_withObject() {
		var obj = new HashMap<>();
		assertEquals("HashMap", cnsq(obj));
	}

	@Test
	public void a05_simpleQualifiedClassName_null() {
		assertNull(cnsq(null));
	}

	@Test
	public void a06_simpleQualifiedClassName_noPackage() {
		// Test with a class that has no package (unlikely in practice, but good to test)
		var name = cnsq(String.class);
		assertFalse(name.contains(".java.lang"), name);
	}

	@Test
	public void a07_simpleQualifiedClassName_anonymousClass() {
		var anon = new Object() {};
		var result = cnsq(anon);
		// Anonymous classes have names like "ClassUtils_Test$1"
		// After conversion should be like "ClassUtils_Test.1"
		assertNotNull(result);
		assertFalse(result.contains("$"), result);
	}

	@Test
	public void a08_simpleQualifiedClassName_arrayTypes() {
		assertEquals("String[]", cnsq(String[].class));
		assertEquals("String[][]", cnsq(String[][].class));
		assertEquals("int[]", cnsq(int[].class));
		assertEquals("Map.Entry[]", cnsq(Map.Entry[].class));
	}

	@Test
	public void a09_simpleQualifiedClassName_arrayObjects() {
		var stringArray = new String[]{"a", "b"};
		assertEquals("String[]", cnsq(stringArray));

		var intArray = new int[]{1, 2, 3};
		assertEquals("int[]", cnsq(intArray));

		var multiDimArray = new String[][]{{"a"}};
		assertEquals("String[][]", cnsq(multiDimArray));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// sqcn shortcut tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_sqcn_shortcut() {
		// Test that the shortcut method works the same as the full method
		assertEquals("String", cnsq(String.class));
		assertEquals("Map.Entry", cnsq(Map.Entry.class));
		assertNull(cnsq(null));
	}

	@Test
	public void b02_sqcn_withObject() {
		var obj = new HashMap<>();
		assertEquals("HashMap", cnsq(obj));
		assertEquals(cnsq(obj), cnsq(obj));
	}

	@Test
	public void b03_sqcn_withArrays() {
		assertEquals("String[]", cnsq(String[].class));
		assertEquals("int[][]", cnsq(int[][].class));
		assertEquals("Map.Entry[]", cnsq(Map.Entry[].class));

		var arr = new String[]{"test"};
		assertEquals("String[]", cnsq(arr));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// canAddTo(Collection<?>) tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void c01_canAddTo_modifiableCollections() {
		assertTrue(canAddTo(new ArrayList<>()));
		assertTrue(canAddTo(new LinkedList<>()));
		assertTrue(canAddTo(new HashSet<>()));
		assertTrue(canAddTo(new LinkedHashSet<>()));
		assertTrue(canAddTo(new TreeSet<>()));
		assertTrue(canAddTo(new Vector<>()));
	}

	@Test
	public void c02_canAddTo_unmodifiableCollections() {
		assertFalse(canAddTo(Collections.unmodifiableList(new ArrayList<>())));
		assertFalse(canAddTo(Collections.unmodifiableSet(new HashSet<>())));
		assertFalse(canAddTo(Collections.unmodifiableCollection(new ArrayList<>())));
		assertFalse(canAddTo(Arrays.asList("a", "b"))); // Arrays$ArrayList
	}

	@Test
	public void c03_canAddTo_immutableCollections() {
		// Java 9+ immutable collections
		if (List.of("a").getClass().getName().contains("Immutable")) {
			assertFalse(canAddTo(List.of("a")));
			assertFalse(canAddTo(Set.of("a")));
		}
	}

	@Test
	public void c04_canAddTo_null() {
		assertThrows(IllegalArgumentException.class, () -> {
			canAddTo((Collection<?>)null);
		});
	}

	//-----------------------------------------------------------------------------------------------------------------
	// canPutTo(Map<?,?>) tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void d01_canPutTo_modifiableMaps() {
		assertTrue(canPutTo(new HashMap<>()));
		assertTrue(canPutTo(new LinkedHashMap<>()));
		assertTrue(canPutTo(new TreeMap<>()));
		assertTrue(canPutTo(new Hashtable<>()));
	}

	@Test
	public void d02_canPutTo_unmodifiableMaps() {
		assertFalse(canPutTo(Collections.unmodifiableMap(new HashMap<>())));
	}

	@Test
	public void d03_canPutTo_immutableMaps() {
		// Java 9+ immutable maps
		if (Map.of("a", "b").getClass().getName().contains("Immutable")) {
			assertFalse(canPutTo(Map.of("a", "b")));
		}
	}

	@Test
	public void d04_canPutTo_null() {
		assertThrows(IllegalArgumentException.class, () -> {
			canPutTo((Map<?,?>)null);
		});
	}

	//-----------------------------------------------------------------------------------------------------------------
	// className(Object) tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void e01_className_withClass() {
		assertEquals("java.lang.String", cn(String.class));
		assertEquals("java.util.ArrayList", cn(ArrayList.class));
	}

	@Test
	public void e02_className_withObject() {
		assertEquals("java.util.HashMap", cn(new HashMap<>()));
		assertEquals("java.lang.String", cn("test"));
	}

	@Test
	public void e03_className_withPrimitive() {
		assertEquals("int", cn(int.class));
		assertEquals("boolean", cn(boolean.class));
	}

	@Test
	public void e04_className_withArray() {
		assertEquals("[Ljava.lang.String;", cn(String[].class));
		assertEquals("[I", cn(int[].class));
		assertEquals("[[Ljava.lang.String;", cn(String[][].class));
	}

	@Test
	public void e05_className_withInnerClass() {
		assertEquals("java.util.Map$Entry", cn(Map.Entry.class));
	}

	@Test
	public void e06_className_null() {
		assertNull(cn(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// classNameSimple(Object) tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void f01_classNameSimple_withClass() {
		assertEquals("String", cns(String.class));
		assertEquals("ArrayList", cns(ArrayList.class));
	}

	@Test
	public void f02_classNameSimple_withObject() {
		assertEquals("HashMap", cns(new HashMap<>()));
		assertEquals("String", cns("test"));
	}

	@Test
	public void f03_classNameSimple_withPrimitive() {
		assertEquals("int", cns(int.class));
		assertEquals("boolean", cns(boolean.class));
	}

	@Test
	public void f04_classNameSimple_withArray() {
		assertEquals("String[]", cns(String[].class));
		assertEquals("int[]", cns(int[].class));
		assertEquals("String[][]", cns(String[][].class));
	}

	@Test
	public void f05_classNameSimple_withInnerClass() {
		assertEquals("Entry", cns(Map.Entry.class));
	}

	@Test
	public void f06_classNameSimple_null() {
		assertNull(cns(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getClasses(Object...) tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void g01_getClasses_basic() {
		var classes = getClasses("test", 123, new HashMap<>());
		assertEquals(3, classes.length);
		assertEquals(String.class, classes[0]);
		assertEquals(Integer.class, classes[1]);
		assertEquals(HashMap.class, classes[2]);
	}

	@Test
	public void g02_getClasses_withNull() {
		var classes = getClasses("test", null, 123);
		assertEquals(3, classes.length);
		assertEquals(String.class, classes[0]);
		assertNull(classes[1]);
		assertEquals(Integer.class, classes[2]);
	}

	@Test
	public void g03_getClasses_empty() {
		var classes = getClasses();
		assertEquals(0, classes.length);
	}

	@Test
	public void g04_getClasses_allNull() {
		var classes = getClasses(null, null, null);
		assertEquals(3, classes.length);
		assertNull(classes[0]);
		assertNull(classes[1]);
		assertNull(classes[2]);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getMatchingArgs(Class<?>[], Object...) tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void h01_getMatchingArgs_exactMatch() {
		var paramTypes = a(String.class, Integer.class);
		var args = a("test", 123);
		var result = getMatchingArgs(paramTypes, (Object[])args);
		assertSame(args, result); // Should return original array (fast path)
		assertEquals("test", result[0]);
		assertEquals(123, result[1]);
	}

	@Test
	public void h02_getMatchingArgs_wrongOrder() {
		var paramTypes = a(Integer.class, String.class);
		var args = a("test", 123);
		var result = getMatchingArgs(paramTypes, (Object[])args);
		assertEquals(2, result.length);
		assertEquals(123, result[0]);
		assertEquals("test", result[1]);
	}

	@Test
	public void h03_getMatchingArgs_extraArgs() {
		var paramTypes = a(String.class);
		var args = a("test", 123, true);
		var result = getMatchingArgs(paramTypes, (Object[])args);
		assertEquals(1, result.length);
		assertEquals("test", result[0]);
	}

	@Test
	public void h04_getMatchingArgs_missingArgs() {
		var paramTypes = a(String.class, Integer.class, Boolean.class);
		var args = a("test");
		var result = getMatchingArgs(paramTypes, (Object[])args);
		assertEquals(3, result.length);
		assertEquals("test", result[0]);
		assertNull(result[1]);
		assertNull(result[2]);
	}

	@Test
	public void h05_getMatchingArgs_primitiveTypes() {
		var paramTypes = a(int.class, String.class);
		var args = a("test", 123);
		var result = getMatchingArgs(paramTypes, (Object[])args);
		assertEquals(2, result.length);
		assertEquals(123, result[0]);
		assertEquals("test", result[1]);
	}

	@Test
	public void h06_getMatchingArgs_typeHierarchy() {
		var paramTypes = a(Number.class, String.class);
		var args = a("test", 123);
		var result = getMatchingArgs(paramTypes, (Object[])args);
		assertEquals(2, result.length);
		assertEquals(123, result[0]);
		assertEquals("test", result[1]);
	}

	@Test
	public void h07_getMatchingArgs_nullArgs() {
		var paramTypes = a(String.class, Integer.class);
		var args = new Object[] {null, null};
		var result = getMatchingArgs(paramTypes, args);
		assertEquals(2, result.length);
		assertNull(result[0]);
		assertNull(result[1]);
	}

	@Test
	public void h08_getMatchingArgs_nullParamTypes() {
		// getMatchingArgs checks args first, then paramTypes
		// If paramTypes is null, it will throw NullPointerException when accessing paramTypes.length
		assertThrows(NullPointerException.class, () -> {
			getMatchingArgs(null, "test");
		});
	}

	//-----------------------------------------------------------------------------------------------------------------
	// isVoid(Class) and isNotVoid(Class) tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void i01_isVoid_voidClass() {
		assertTrue(isVoid(void.class));
		assertTrue(isVoid(Void.class));
	}

	@Test
	public void i02_isVoid_null() {
		assertTrue(isVoid(null));
	}

	@Test
	public void i03_isVoid_nonVoid() {
		assertFalse(isVoid(String.class));
		assertFalse(isVoid(int.class));
		assertFalse(isVoid(Object.class));
	}

	@Test
	public void i04_isNotVoid_voidClass() {
		assertFalse(isNotVoid(void.class));
		assertFalse(isNotVoid(Void.class));
	}

	@Test
	public void i05_isNotVoid_null() {
		assertFalse(isNotVoid(null));
	}

	@Test
	public void i06_isNotVoid_nonVoid() {
		assertTrue(isNotVoid(String.class));
		assertTrue(isNotVoid(int.class));
		assertTrue(isNotVoid(Object.class));
	}

	@Test
	public void i07_NOT_VOID_predicate() {
		assertFalse(NOT_VOID.test(void.class));
		assertFalse(NOT_VOID.test(Void.class));
		assertTrue(NOT_VOID.test(String.class));
		assertTrue(NOT_VOID.test(int.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// setAccessible(Constructor<?>), setAccessible(Field), setAccessible(Method) tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void j01_setAccessible_constructor() throws Exception {
		var ctor = String.class.getDeclaredConstructor();
		// Should succeed (no security manager in tests typically)
		assertTrue(setAccessible(ctor));
	}

	@Test
	public void j02_setAccessible_constructor_null() {
		assertTrue(setAccessible((Constructor<?>)null));
	}

	@Test
	public void j03_setAccessible_field() throws Exception {
		// Use a field from a test class, not from java.lang (which has module restrictions)
		class TestClass {
			private String field;
		}
		var field = TestClass.class.getDeclaredField("field");
		// Should succeed (no security manager in tests typically)
		assertTrue(setAccessible(field));
	}

	@Test
	public void j04_setAccessible_field_null() {
		assertTrue(setAccessible((Field)null));
	}

	@Test
	public void j05_setAccessible_method() throws Exception {
		var method = String.class.getDeclaredMethod("indexOf", String.class, int.class);
		// Should succeed (no security manager in tests typically)
		assertTrue(setAccessible(method));
	}

	@Test
	public void j06_setAccessible_method_null() {
		assertTrue(setAccessible((Method)null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// toClass(Type) tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void k01_toClass_withClass() {
		assertSame(String.class, toClass(String.class));
		assertSame(Integer.class, toClass(Integer.class));
	}

	@Test
	public void k02_toClass_withParameterizedType() throws Exception {
		// Get a ParameterizedType from a generic field
		class TestClass {
			List<String> field;
		}
		var field = TestClass.class.getDeclaredField("field");
		var genericType = field.getGenericType();
		var result = toClass(genericType);
		assertEquals(List.class, result);
	}

	@Test
	public void k03_toClass_withTypeVariable() throws Exception {
		// TypeVariable cannot be converted to Class
		class TestClass<T> {
			T field;
		}
		var field = TestClass.class.getDeclaredField("field");
		var genericType = field.getGenericType();
		var result = toClass(genericType);
		assertNull(result);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// isInnerClass(GenericDeclaration, GenericDeclaration) tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void l01_isInnerClass_true() {
		assertTrue(isInnerClass(Map.class, Map.Entry.class));
	}

	@Test
	public void l02_isInnerClass_false() {
		assertFalse(isInnerClass(Map.Entry.class, Map.class));
		assertFalse(isInnerClass(String.class, Integer.class));
	}

	@Test
	public void l03_isInnerClass_sameClass() {
		assertFalse(isInnerClass(String.class, String.class));
	}

	@Test
	public void l04_isInnerClass_nestedInner() {
		class Outer {
			class Inner {
				class Deep {}
			}
		}
		assertTrue(isInnerClass(Outer.class, Outer.Inner.class));
		assertTrue(isInnerClass(Outer.class, Outer.Inner.Deep.class));
		assertTrue(isInnerClass(Outer.Inner.class, Outer.Inner.Deep.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getProxyFor(Object) tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void m01_getProxyFor_null() {
		assertNull(getProxyFor(null));
	}

	@Test
	public void m02_getProxyFor_regularObject() {
		var obj = "test";
		assertNull(getProxyFor(obj));
	}

	@Test
	public void m03_getProxyFor_jdkProxy() {
		// Create a JDK dynamic proxy
		var proxy = Proxy.newProxyInstance(
			Thread.currentThread().getContextClassLoader(),
			new Class[]{List.class},
			(proxy1, method, args) -> null
		);
		var result = getProxyFor(proxy);
		assertEquals(List.class, result);
	}

	@Test
	public void m04_getProxyFor_jdkProxy_noInterfaces() {
		// Create a JDK dynamic proxy with no interfaces (edge case)
		var proxy = Proxy.newProxyInstance(
			Thread.currentThread().getContextClassLoader(),
			new Class[0],
			(proxy1, method, args) -> null
		);
		var result = getProxyFor(proxy);
		assertNull(result);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// extractTypes(Map<Type,Type>, Class<?>) tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void n01_extractTypes_basic() {
		class MyList<T> extends ArrayList<T> {}
		class StringList extends MyList<String> {}

		var typeMap = new HashMap<Type,Type>();
		extractTypes(typeMap, StringList.class);
		// Should extract T -> String mapping
		assertFalse(typeMap.isEmpty());
	}

	@Test
	public void n02_extractTypes_noGenericSuperclass() {
		var typeMap = new HashMap<Type,Type>();
		extractTypes(typeMap, String.class);
		// String doesn't have a generic superclass, so typeMap should remain empty
		assertTrue(typeMap.isEmpty());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getValueParameterType(Type) tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void o01_getValueParameterType_parameterizedType() throws Exception {
		class TestClass {
			Value<String> field;
		}
		var field = TestClass.class.getDeclaredField("field");
		var genericType = field.getGenericType();
		var result = getValueParameterType(genericType);
		assertEquals(String.class, result);
	}

	@Test
	public void o02_getValueParameterType_class() {
		class StringValue extends Value<String> {}
		var result = getValueParameterType(StringValue.class);
		assertEquals(String.class, result);
	}

	@Test
	public void o03_getValueParameterType_notValue() {
		var result = getValueParameterType(String.class);
		assertNull(result);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getParameterType(Class<?>, int, Class<?>) tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void p01_getParameterType_basic() {
		class MyList<T> extends ArrayList<T> {}
		class StringList extends MyList<String> {}

		var result = getParameterType(StringList.class, 0, MyList.class);
		assertEquals(String.class, result);
	}

	@Test
	public void p02_getParameterType_invalidIndex() {
		class MyList<T> extends ArrayList<T> {}
		class StringList extends MyList<String> {}

		assertThrows(IllegalArgumentException.class, () -> {
			getParameterType(StringList.class, 1, MyList.class);
		});
	}

	@Test
	public void p03_getParameterType_notSubclass() {
		assertThrows(IllegalArgumentException.class, () -> {
			getParameterType(String.class, 0, ArrayList.class);
		});
	}

	@Test
	public void p04_getParameterType_nullArgs() {
		assertThrows(IllegalArgumentException.class, () -> {
			getParameterType(null, 0, String.class);
		});
		assertThrows(IllegalArgumentException.class, () -> {
			getParameterType(String.class, 0, null);
		});
	}
}

