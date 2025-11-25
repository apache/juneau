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
package org.apache.juneau.common.collections;

import static org.apache.juneau.common.utils.ClassUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Type;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class Value_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Value defined on parent class.
	//-----------------------------------------------------------------------------------------------------------------

	public static class A extends Value<A1>{}
	public static class A1 {}

	@Test void a01_testSubclass() {
		assertEquals(A1.class, getValueParameterType(A.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getAndSet(T)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_getAndSet_withExistingValue() {
		var v = Value.of("old");
		var old = v.getAndSet("new");
		assertEquals("old", old, "Should return old value");
		assertEquals("new", v.get(), "Should have new value");
	}

	@Test
	void b02_getAndSet_withNullValue() {
		var v = Value.of(null);
		var old = v.getAndSet("new");
		assertNull(old, "Should return null");
		assertEquals("new", v.get(), "Should have new value");
	}

	@Test
	void b03_getAndSet_toNull() {
		var v = Value.of("old");
		var old = v.getAndSet(null);
		assertEquals("old", old, "Should return old value");
		assertNull(v.get(), "Should have null value");
	}

	@Test
	void b04_getAndSet_multiple() {
		var v = Value.of(1);
		assertEquals(1, v.getAndSet(2), "First getAndSet");
		assertEquals(2, v.getAndSet(3), "Second getAndSet");
		assertEquals(3, v.getAndSet(4), "Third getAndSet");
		assertEquals(4, v.get(), "Final value");
	}

	@Test
	void b05_getAndSet_withEmptyValue() {
		var v = Value.empty();
		var old = v.getAndSet("new");
		assertNull(old, "Should return null");
		assertEquals("new", v.get(), "Should have new value");
	}

	@Test
	void b06_getAndSet_chainability() {
		var v = Value.of("old");
		v.getAndSet("new");
		assertEquals("new", v.get(), "Value should be set after getAndSet");
	}

	@Test
	void b07_getAndSet_withListener() {
		var v = Value.of("old");
		var sb = new StringBuilder();
		v.listener((val) -> sb.append(val));

		var old = v.getAndSet("new");

		assertEquals("old", old, "Should return old value");
		assertEquals("new", v.get(), "Should have new value");
		assertEquals("new", sb.toString(), "Listener should be called with new value");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// is(T)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_is_equalStrings() {
		var v = Value.of("hello");
		assertTrue(v.is("hello"), "Should be equal to same string");
		assertFalse(v.is("world"), "Should not be equal to different string");
	}

	@Test
	void c02_is_withNull() {
		var v = Value.empty();
		assertTrue(v.is(null), "Empty value should equal null");
		assertFalse(v.is("test"), "Empty value should not equal non-null");

		v.set("test");
		assertFalse(v.is(null), "Non-null value should not equal null");
	}

	@Test
	void c03_is_equalIntegers() {
		var v = Value.of(42);
		assertTrue(v.is(42), "Should be equal to same integer");
		assertFalse(v.is(43), "Should not be equal to different integer");
	}

	@Test
	void c04_is_equalBooleans() {
		var v = Value.of(true);
		assertTrue(v.is(true), "Should be equal to true");
		assertFalse(v.is(false), "Should not be equal to false");
	}

	@Test
	void c05_is_equalArrays() {
		var v = Value.of(ints(1, 2, 3));
		assertTrue(v.is(ints(1, 2, 3)), "Should be equal to same array content");
		assertFalse(v.is(ints(1, 2, 4)), "Should not be equal to different array content");
		assertFalse(v.is(ints(1, 2)), "Should not be equal to shorter array");
	}

	@Test
	void c06_is_equalLists() {
		var v = Value.of(l("a", "b", "c"));
		assertTrue(v.is(l("a", "b", "c")), "Should be equal to same list content");
		assertFalse(v.is(l("a", "b", "d")), "Should not be equal to different list content");
	}

	@Test
	void c07_is_emptyVsEmpty() {
		var v1 = Value.empty();
		var v2 = Value.empty();
		assertTrue(v1.is(v2.get()), "Two empty values should be equal");
	}

	@Test
	void c08_is_differentTypes() {
		Value<Object> v = Value.of("42");
		assertFalse(v.is(42), "String '42' should not equal Integer 42");
	}

	@Test
	void c09_is_sameObject() {
		var s = "test";
		var v = Value.of(s);
		assertTrue(v.is(s), "Should be equal to same object reference");
	}

	@Test
	void c10_is_equalCustomObjects() {
		// Using A1 class defined at the top
		var obj1 = new A1();
		var obj2 = new A1();

		var v = Value.of(obj1);
		assertTrue(v.is(obj1), "Should be equal to same object");
		// Note: A1 doesn't override equals(), so different instances won't be equal
		assertFalse(v.is(obj2), "Should not be equal to different instance without equals override");
	}

	@Test
	void c11_is_afterSet() {
		var v = Value.of("initial");
		assertTrue(v.is("initial"));

		v.set("updated");
		assertFalse(v.is("initial"), "Should not equal old value after set");
		assertTrue(v.is("updated"), "Should equal new value after set");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// filter(Predicate)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_filter_passing() {
		var v = Value.of("hello");
		var filtered = v.filter(s -> s.length() > 3);
		assertTrue(filtered.isPresent());
		assertEquals("hello", filtered.get());
	}

	@Test
	void d02_filter_failing() {
		var v = Value.of("hello");
		var filtered = v.filter(s -> s.length() > 10);
		assertFalse(filtered.isPresent());
	}

	@Test
	void d03_filter_empty() {
		Value<String> v = Value.empty();
		var filtered = v.filter(s -> s.length() > 3);
		assertFalse(filtered.isPresent());
	}

	@Test
	void d04_filter_chain() {
		var v = Value.of(100);
		var filtered = v.filter(x -> x > 50).filter(x -> x < 150);
		assertTrue(filtered.isPresent());
		assertEquals(100, filtered.get());
	}

	@Test
	void d05_filter_chainFails() {
		var v = Value.of(100);
		var filtered = v.filter(x -> x > 50).filter(x -> x < 80);
		assertFalse(filtered.isPresent());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// flatMap(Function)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_flatMap_basic() {
		var v = Value.of("hello");
		var mapped = v.flatMap(s -> Value.of(s.length()));
		assertTrue(mapped.isPresent());
		assertEquals(5, mapped.get());
	}

	@Test
	void e02_flatMap_toEmpty() {
		var v = Value.of("hello");
		var mapped = v.flatMap(s -> Value.empty());
		assertFalse(mapped.isPresent());
	}

	@Test
	void e03_flatMap_fromEmpty() {
		Value<String> v = Value.empty();
		var mapped = v.flatMap(s -> Value.of(s.length()));
		assertFalse(mapped.isPresent());
	}

	@Test
	void e04_flatMap_chain() {
		var v = Value.of("hello");
		var mapped = v.flatMap(s -> Value.of(s.length()))
			.flatMap(n -> Value.of(n * 2));
		assertTrue(mapped.isPresent());
		assertEquals(10, mapped.get());
	}

	@Test
	void e05_flatMap_withFilter() {
		var v = Value.of("hello");
		var result = v.filter(s -> s.length() > 3)
			.flatMap(s -> Value.of(s.toUpperCase()));
		assertTrue(result.isPresent());
		assertEquals("HELLO", result.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// equals(Object)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void f01_equals_sameValue() {
		var v1 = Value.of("hello");
		var v2 = Value.of("hello");
		assertEquals(v1, v2);
	}

	@Test
	void f02_equals_differentValue() {
		var v1 = Value.of("hello");
		var v2 = Value.of("world");
		assertNotEquals(v1, v2);
	}

	@Test
	void f03_equals_bothEmpty() {
		var v1 = Value.empty();
		var v2 = Value.empty();
		assertEquals(v1, v2);
	}

	@Test
	void f04_equals_oneEmpty() {
		var v1 = Value.of("hello");
		var v2 = Value.empty();
		assertNotEquals(v1, v2);
	}

	@Test
	void f05_equals_sameReference() {
		var v = Value.of("hello");
		assertEquals(v, v);
	}

	@Test
	void f06_equals_null() {
		var v = Value.of("hello");
		assertNotEquals(null, v);
	}

	@Test
	void f07_equals_differentType() {
		var v = Value.of("hello");
		assertNotEquals("hello", v);
	}

	@Test
	void f08_equals_integers() {
		var v1 = Value.of(42);
		var v2 = Value.of(42);
		var v3 = Value.of(43);
		assertEquals(v1, v2);
		assertNotEquals(v1, v3);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// hashCode()
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void g01_hashCode_consistent() {
		var v = Value.of("hello");
		assertEquals(v.hashCode(), v.hashCode());
	}

	@Test
	void g02_hashCode_equalValues() {
		var v1 = Value.of("hello");
		var v2 = Value.of("hello");
		assertEquals(v1.hashCode(), v2.hashCode());
	}

	@Test
	void g03_hashCode_empty() {
		var v = Value.empty();
		assertEquals(0, v.hashCode());
	}

	@Test
	void g04_hashCode_afterSet() {
		var v = Value.of("hello");
		var hash1 = v.hashCode();
		v.set("world");
		var hash2 = v.hashCode();
		assertNotEquals(hash1, hash2);
	}

	@Test
	void g05_hashCode_nullValue() {
		var v = Value.of(null);
		assertEquals(0, v.hashCode());
	}

	@Test
	void g06_hashCode_useInHashMap() {
		var map = new java.util.HashMap<Value<String>, String>();
		var key1 = Value.of("key");
		var key2 = Value.of("key");
		map.put(key1, "value");
		assertEquals("value", map.get(key2), "HashMap should work with Value's hashCode and equals");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// isType(Type)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void h01_isType_parameterizedType() {
		java.lang.reflect.Type type = new java.lang.reflect.ParameterizedType() {
			@Override
			public Type[] getActualTypeArguments() {
				return new Type[]{String.class};
			}
			@Override
			public Type getRawType() {
				return Value.class;
			}
			@Override
			public Type getOwnerType() {
				return null;
			}
		};
		assertTrue(Value.isType(type), "ParameterizedType with Value.class as raw type should return true");
	}

	@Test
	void h02_isType_parameterizedType_differentRawType() {
		java.lang.reflect.Type type = new java.lang.reflect.ParameterizedType() {
			@Override
			public Type[] getActualTypeArguments() {
				return new Type[]{String.class};
			}
			@Override
			public Type getRawType() {
				return String.class; // Not Value.class
			}
			@Override
			public Type getOwnerType() {
				return null;
			}
		};
		assertFalse(Value.isType(type), "ParameterizedType with different raw type should return false");
	}

	@Test
	void h03_isType_valueClass() {
		assertTrue(Value.isType(Value.class), "Value.class should return true");
	}

	@Test
	void h04_isType_valueSubclass() {
		assertTrue(Value.isType(IntegerValue.class), "IntegerValue (subclass of Value) should return true");
		assertTrue(Value.isType(StringValue.class), "StringValue (subclass of Value) should return true");
	}

	@Test
	void h05_isType_nonValueClass() {
		assertFalse(Value.isType(String.class), "String.class should return false");
		assertFalse(Value.isType(Integer.class), "Integer.class should return false");
	}

	@Test
	void h06_isType_null() {
		// null should be handled gracefully
		assertFalse(Value.isType(null), "null type should return false");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// unwrap(Type)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void i01_unwrap_parameterizedValueType() throws Exception {
		java.lang.reflect.Type valueType = new java.lang.reflect.ParameterizedType() {
			@Override
			public Type[] getActualTypeArguments() {
				return new Type[]{String.class};
			}
			@Override
			public Type getRawType() {
				return Value.class;
			}
			@Override
			public Type getOwnerType() {
				return null;
			}
		};
		Type unwrapped = Value.unwrap(valueType);
		assertEquals(String.class, unwrapped, "Should unwrap Value<String> to String");
	}

	@Test
	void i02_unwrap_valueSubclass() {
		Type unwrapped = Value.unwrap(IntegerValue.class);
		assertEquals(Integer.class, unwrapped, "Should unwrap IntegerValue to Integer");
	}

	@Test
	void i03_unwrap_nonValueType() {
		Type unwrapped = Value.unwrap(String.class);
		assertEquals(String.class, unwrapped, "Non-Value type should be returned as-is");
	}

	@Test
	void i04_unwrap_valueClass() {
		// Value.class itself (without parameter) cannot be unwrapped because it has no type parameter
		// getParameterType throws IllegalArgumentException when the class is not a subclass
		assertThrows(IllegalArgumentException.class, () -> {
			Value.unwrap(Value.class);
		}, "Value.class without parameter should throw IllegalArgumentException");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// toString()
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void j01_toString_withValue() {
		var v = Value.of("hello");
		assertEquals("Value(hello)", v.toString());
	}

	@Test
	void j02_toString_withNull() {
		var v = Value.empty();
		assertEquals("Value(null)", v.toString());
	}

	@Test
	void j03_toString_withInteger() {
		var v = Value.of(42);
		assertEquals("Value(42)", v.toString());
	}

	@Test
	void j04_toString_afterSet() {
		var v = Value.of("initial");
		assertEquals("Value(initial)", v.toString());
		v.set("updated");
		assertEquals("Value(updated)", v.toString());
	}

	@Test
	void j05_toString_withCustomObject() {
		var obj = new A1();
		var v = Value.of(obj);
		String result = v.toString();
		assertTrue(result.startsWith("Value("), "Should start with 'Value('");
		assertTrue(result.endsWith(")"), "Should end with ')'");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Value() - default constructor
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void k01_defaultConstructor_createsEmpty() {
		var v = new Value<String>();
		assertNull(v.get(), "Default constructor should create empty Value");
		assertTrue(v.isEmpty(), "Default constructor should create empty Value");
		assertFalse(v.isPresent(), "Default constructor should create empty Value");
	}

	@Test
	void k02_defaultConstructor_canSetValue() {
		var v = new Value<String>();
		v.set("test");
		assertEquals("test", v.get(), "Should be able to set value after default constructor");
	}

	@Test
	void k03_defaultConstructor_equalsEmpty() {
		var v1 = new Value<String>();
		var v2 = Value.empty();
		assertEquals(v1, v2, "Default constructor should equal Value.empty()");
	}
}

