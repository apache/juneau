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
package org.apache.juneau.commons.conversion;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"unused" // Test helper classes have fields read only via assertions
})

class BasicConverter_Test extends TestBase {

	private static final BasicConverter C = BasicConverter.INSTANCE;

	//====================================================================================================
	// a - Number conversions
	//====================================================================================================

	@Test void a01_numberToNumber() {
		assertEquals(42, C.to(42L, Integer.class));
		assertEquals(42L, C.to(42, Long.class));
		assertEquals((short) 42, C.to(42, Short.class));
		assertEquals(42f, C.to(42, Float.class));
		assertEquals(42d, C.to(42, Double.class));
		assertEquals((byte) 42, C.to(42, Byte.class));
		assertEquals(42, C.to(42L, AtomicInteger.class).get());
		assertEquals(42L, C.to(42, AtomicLong.class).get());
	}

	@Test void a02_booleanToNumber() {
		assertEquals(1, C.to(true, Integer.class));
		assertEquals(0, C.to(false, Integer.class));
		assertEquals(1L, C.to(true, Long.class));
		assertEquals(0L, C.to(false, Long.class));
		assertEquals((short) 1, C.to(true, Short.class));
		assertEquals((short) 0, C.to(false, Short.class));
		assertEquals(1f, C.to(true, Float.class));
		assertEquals(0f, C.to(false, Float.class));
		assertEquals(1d, C.to(true, Double.class));
		assertEquals(0d, C.to(false, Double.class));
		assertEquals((byte) 1, C.to(true, Byte.class));
		assertEquals((byte) 0, C.to(false, Byte.class));
		assertEquals(1, C.to(true, AtomicInteger.class).get());
		assertEquals(0, C.to(false, AtomicInteger.class).get());
		assertEquals(1L, C.to(true, AtomicLong.class).get());
		assertEquals(0L, C.to(false, AtomicLong.class).get());
	}

	@Test void a03_stringToNumber() {
		assertEquals(42, C.to("42", Integer.class));
		assertEquals(42L, C.to("42", Long.class));
		assertEquals((short) 42, C.to("42", Short.class));
		assertEquals(42f, C.to("42", Float.class));
		assertEquals(42d, C.to("42", Double.class));
		assertEquals((byte) 42, C.to("42", Byte.class));
	}

	@Test void a04_primitiveTargets() {
		assertEquals(42, C.to("42", int.class));
		assertEquals(42L, C.to("42", long.class));
		assertEquals((short) 42, C.to("42", short.class));
		assertEquals(42f, C.to("42", float.class));
		assertEquals(42d, C.to("42", double.class));
		assertEquals((byte) 42, C.to("42", byte.class));
	}

	@Test void a05_numberAndBooleanToUnsupportedNumericType() {
		// Number → BigDecimal: hits findNumberFromNumber line 234 false branch (not AtomicLong), returns null
		assertThrows(InvalidConversionException.class, () -> C.to(42, java.math.BigDecimal.class));
		// Boolean → BigDecimal: hits findNumberFromBoolean line 246 false branch (not AtomicLong), returns null
		assertThrows(InvalidConversionException.class, () -> C.to(true, java.math.BigDecimal.class));
	}

	//====================================================================================================
	// b - Boolean conversions
	//====================================================================================================

	@Test void b01_numberToBoolean() {
		assertEquals(true, C.to(1, Boolean.class));
		assertEquals(false, C.to(0, Boolean.class));
		assertEquals(true, C.to(-1, Boolean.class));
		assertEquals(true, C.to(42, Boolean.class));
	}

	@Test void b02_stringToBoolean() {
		assertEquals(true, C.to("true", Boolean.class));
		assertEquals(false, C.to("false", Boolean.class));
		assertEquals(false, C.to("xyz", Boolean.class));
		assertNull(C.to("", Boolean.class));
		assertNull(C.to("null", Boolean.class));
	}

	@Test void b03_primitiveBoolean() {
		assertEquals(true, C.to("true", boolean.class));
		assertEquals(false, C.to(0, boolean.class));
	}

	@Test void b04_unconvertibleToBoolean() {
		assertThrows(InvalidConversionException.class, () -> C.to(new Object(), Boolean.class));
	}

	//====================================================================================================
	// c - Character conversions
	//====================================================================================================

	@Test void c01_stringToChar() {
		assertEquals('A', C.to("A", Character.class));
		assertNull(C.to("AB", Character.class));
	}

	@Test void c02_numberToChar() {
		assertEquals('A', C.to(65, Character.class));
	}

	@Test void c03_primitiveChar() {
		assertEquals('X', C.to("X", char.class));
	}

	@Test void c04_unconvertibleToCharacter() {
		assertThrows(InvalidConversionException.class, () -> C.to(true, Character.class));
	}

	//====================================================================================================
	// d - String conversions
	//====================================================================================================

	@Test void d01_objectToString() {
		assertEquals("42", C.to(42, String.class));
		assertEquals("true", C.to(true, String.class));
		assertEquals("3.14", C.to(3.14, String.class));
	}

	@Test void d02_arrayToString() {
		assertEquals("[1, 2, 3]", C.to(new int[]{1, 2, 3}, String.class));
		assertEquals("[1, 2, 3]", C.to(new long[]{1L, 2L, 3L}, String.class));
		assertEquals("[1.0, 2.0]", C.to(new double[]{1.0, 2.0}, String.class));
		assertEquals("[1.0, 2.0]", C.to(new float[]{1.0f, 2.0f}, String.class));
		assertEquals("[true, false]", C.to(new boolean[]{true, false}, String.class));
		assertEquals("[1, 2, 3]", C.to(new byte[]{1, 2, 3}, String.class));
		assertEquals("[1, 2, 3]", C.to(new short[]{1, 2, 3}, String.class));
		assertEquals("[a, b, c]", C.to(new char[]{'a', 'b', 'c'}, String.class));
		assertEquals("[a, b, c]", C.to(new String[]{"a", "b", "c"}, String.class));
	}

	@Test void d03_collectionToString() {
		assertEquals("[a, b]", C.to(List.of("a", "b"), String.class));
	}

	//====================================================================================================
	// e - Enum conversions
	//====================================================================================================

	enum E01_TestEnum { FOO, BAR, BAZ }

	@Test void e01_stringToEnum() {
		assertEquals(E01_TestEnum.FOO, C.to("FOO", E01_TestEnum.class));
		assertEquals(E01_TestEnum.BAR, C.to("BAR", E01_TestEnum.class));
		assertEquals(E01_TestEnum.BAZ, C.to("BAZ", E01_TestEnum.class));
	}

	@Test void e02_unconvertibleToEnum() {
		assertThrows(InvalidConversionException.class, () -> C.to(42, E01_TestEnum.class));
	}

	//====================================================================================================
	// f - Collection conversions
	//====================================================================================================

	@Test void f01_listToList() {
		var a = C.to(List.of("a", "b", "c"), List.class);
		assertNotNull(a);
		assertEquals(3, a.size());
		assertEquals("a", a.get(0));
	}

	@Test void f02_listToListTyped() {
		var a = C.to(List.of("1", "2", "3"), (Type) List.class, new Type[]{Integer.class});
		assertNotNull(a);
		assertEquals(3, ((List<?>) a).size());
		assertEquals(1, ((List<?>) a).get(0));
		assertInstanceOf(Integer.class, ((List<?>) a).get(0));
	}

	@Test void f03_arrayToList() {
		var a = C.to(new String[]{"x", "y"}, List.class);
		assertNotNull(a);
		assertEquals(2, ((List<?>) a).size());
		assertEquals("x", ((List<?>) a).get(0));
	}

	@Test void f03b_arrayToTypedList() {
		// array branch with elemType != null — covers line 326 true branch of ternary
		var a = C.to(new String[]{"1", "2", "3"}, (Type) List.class, new Type[]{Integer.class});
		assertNotNull(a);
		assertEquals(List.of(1, 2, 3), a);
	}

	@Test void f04_listToSet() {
		var a = C.to(List.of("a", "b", "a"), Set.class);
		assertNotNull(a);
		assertInstanceOf(Set.class, a);
		assertEquals(2, ((Set<?>) a).size());
	}

	@Test void f05_listToSortedSet() {
		var a = C.to(List.of("c", "a", "b"), SortedSet.class);
		assertNotNull(a);
		assertInstanceOf(SortedSet.class, a);
		assertEquals("a", ((SortedSet<?>) a).first());
	}

	@Test void f06_primitiveArrayToList() {
		var a = C.to(new int[]{1, 2, 3}, List.class);
		assertNotNull(a);
		assertEquals(3, ((List<?>) a).size());
		assertEquals(1, ((List<?>) a).get(0));
	}

	@Test void f07_collectionTargetVariants() {
		var src = List.of("a", "b");
		assertInstanceOf(Collection.class, C.to(src, Collection.class));
		assertInstanceOf(List.class, C.to(src, Iterable.class));
		assertInstanceOf(List.class, C.to(src, AbstractList.class));
		assertInstanceOf(LinkedHashSet.class, C.to(src, LinkedHashSet.class));
		assertInstanceOf(Set.class, C.to(src, AbstractSet.class));
		assertInstanceOf(NavigableSet.class, C.to(src, NavigableSet.class));
		assertInstanceOf(TreeSet.class, C.to(src, TreeSet.class));
	}

	@Test void f09_listToQueue() {
		var a = C.to(List.of("a", "b"), Queue.class);
		assertNotNull(a);
		assertInstanceOf(Queue.class, a);
		assertEquals(2, ((Queue<?>) a).size());
	}

	@Test void f10_listToConcreteCollection() {
		var a = C.to(List.of("a", "b"), ArrayDeque.class);
		assertNotNull(a);
		assertInstanceOf(ArrayDeque.class, a);
		assertEquals(2, ((ArrayDeque<?>) a).size());
	}

	public static class F11_NoDefaultCtorCollection extends ArrayList<Object> {
		private static final long serialVersionUID = 1L;
		public F11_NoDefaultCtorCollection(int initialCapacity) { super(initialCapacity); }
	}

	@Test void f11_listToCollectionWithNoDefaultCtor() {
		var a = (Collection<?>) C.to(List.of("a", "b"), F11_NoDefaultCtorCollection.class);
		assertNotNull(a);
		assertInstanceOf(ArrayList.class, a);
		assertEquals(2, a.size());
	}

	@Test void f12_arrayToCollectionInterface() {
		// Array input forces newCollection(Collection.class) — hits line 336 B=true branch
		var a = C.to(new String[]{"a", "b"}, Collection.class);
		assertNotNull(a);
		assertInstanceOf(Collection.class, a);
		assertEquals(2, ((Collection<?>) a).size());
	}

	@Test void f13_arrayToAbstractList() {
		// Array input forces newCollection(AbstractList.class) — hits line 336 D=true branch
		var a = C.to(new String[]{"a", "b"}, AbstractList.class);
		assertNotNull(a);
		assertInstanceOf(AbstractList.class, a);
		assertEquals(2, ((AbstractList<?>) a).size());
	}

	@Test void f14_listToDeque() {
		// Hits line 342 B=true branch (Deque.class)
		var a = C.to(List.of("a", "b"), Deque.class);
		assertNotNull(a);
		assertInstanceOf(Deque.class, a);
		assertEquals(2, ((Deque<?>) a).size());
	}

	@Test void f15_listToLinkedList() {
		// Hits line 342 C=true branch (LinkedList.class)
		var a = C.to(List.of("a", "b"), LinkedList.class);
		assertNotNull(a);
		assertInstanceOf(LinkedList.class, a);
		assertEquals(2, ((LinkedList<?>) a).size());
	}

	//====================================================================================================
	// g - Map conversions
	//====================================================================================================

	@Test void g01_mapToMap() {
		var a = C.to(Map.of("a", 1, "b", 2), Map.class);
		assertNotNull(a);
		assertInstanceOf(Map.class, a);
		assertEquals(2, ((Map<?, ?>) a).size());
	}

	@Test void g02_mapToMapTyped() {
		var a = C.to(Map.of(1, "100", 2, "200"), (Type) Map.class, new Type[]{String.class, Integer.class});
		assertNotNull(a);
		var map = (Map<?, ?>) a;
		assertTrue(map.containsKey("1") || map.containsKey("2"));
		for (var v : map.values())
			assertInstanceOf(Integer.class, v);
	}

	@Test void g03_mapToSortedMap() {
		var a = C.to(Map.of("c", 3, "a", 1, "b", 2), SortedMap.class);
		assertNotNull(a);
		assertInstanceOf(SortedMap.class, a);
		assertEquals("a", ((SortedMap<?, ?>) a).firstKey());
	}

	@Test void g04_mapToHashMap() {
		var a = C.to(Map.of("a", 1), HashMap.class);
		assertNotNull(a);
		assertInstanceOf(HashMap.class, a);
		assertEquals(1, ((HashMap<?, ?>) a).size());
	}

	@Test void g05_mapToConcreteMap() {
		var a = C.to(Map.of("a", 1), ConcurrentHashMap.class);
		assertNotNull(a);
		assertInstanceOf(ConcurrentHashMap.class, a);
		assertEquals(1, ((ConcurrentHashMap<?, ?>) a).size());
	}

	public static class G06_NoDefaultCtorMap extends HashMap<Object,Object> {
		private static final long serialVersionUID = 1L;
		public G06_NoDefaultCtorMap(int initialCapacity) { super(initialCapacity); }
	}

	@Test void g06_mapToMapWithNoDefaultCtor() {
		var a = (Map<?, ?>) C.to(Map.of("a", 1), G06_NoDefaultCtorMap.class);
		assertNotNull(a);
		assertInstanceOf(LinkedHashMap.class, a);
		assertEquals(1, a.size());
	}

	@Test void g07_mapToLinkedHashMap() {
		// Hits line 370 B=true branch (LinkedHashMap.class)
		var a = (Map<?, ?>) C.to(Map.of("a", 1), LinkedHashMap.class);
		assertNotNull(a);
		assertInstanceOf(LinkedHashMap.class, a);
		assertEquals(1, a.size());
	}

	@Test void g08_mapToAbstractMap() {
		// Hits line 370 C=true branch (AbstractMap.class); use typed args to bypass short-circuit
		var a = (Map<?, ?>) C.to(Map.of("a", 1), (Type) AbstractMap.class, new Type[]{String.class, Integer.class});
		assertNotNull(a);
		assertInstanceOf(AbstractMap.class, a);
		assertEquals(1, a.size());
	}

	@Test void g09_mapToNavigableMap() {
		// Hits line 372 B=true branch (NavigableMap.class)
		var a = (Map<?, ?>) C.to(Map.of("a", 1), NavigableMap.class);
		assertNotNull(a);
		assertInstanceOf(NavigableMap.class, a);
		assertEquals(1, a.size());
	}

	@Test void g10_mapToTreeMap() {
		// Hits line 372 C=true branch (TreeMap.class)
		var a = (Map<?, ?>) C.to(Map.of("a", 1), TreeMap.class);
		assertNotNull(a);
		assertInstanceOf(TreeMap.class, a);
		assertEquals(1, a.size());
	}

	@Test void g11_unconvertibleToMap() {
		assertThrows(InvalidConversionException.class, () -> C.to("hello", Map.class));
	}

	//====================================================================================================
	// h - Array conversions
	//====================================================================================================

	@Test void h01_listToArray() {
		var a = C.to(List.of("a", "b", "c"), String[].class);
		assertNotNull(a);
		assertArrayEquals(new String[]{"a", "b", "c"}, a);
	}

	@Test void h02_arrayToTypedArray() {
		var a = C.to(new Object[]{"1", "2", "3"}, Integer[].class);
		assertNotNull(a);
		assertArrayEquals(new Integer[]{1, 2, 3}, a);
	}

	@Test void h03_listToIntArray() {
		var a = C.to(List.of(1, 2, 3), int[].class);
		assertNotNull(a);
		assertArrayEquals(new int[]{1, 2, 3}, (int[]) a);
	}

	@Test void h04_unconvertibleToArray() {
		assertThrows(InvalidConversionException.class, () -> C.to(42, String[].class));
	}

	//====================================================================================================
	// i - Reflection: static factory methods
	//====================================================================================================

	public static class I01_Target {
		public final String value;
		private I01_Target(String v) { value = v; }
		public static I01_Target valueOf(String s) { return new I01_Target(s); }
	}

	@Test void i01_staticValueOf() {
		var a = C.to("hello", I01_Target.class);
		assertNotNull(a);
		assertEquals("hello", a.value);
	}

	public static class I02_Target {
		public final String value;
		private I02_Target(String v) { value = v; }
		public static I02_Target fromString(String s) { return new I02_Target(s); }
	}

	@Test void i02_staticFromString() {
		var a = C.to("world", I02_Target.class);
		assertNotNull(a);
		assertEquals("world", a.value);
	}

	public static class I03_Target {
		public final String value;
		private I03_Target(String v) { value = v; }
		public static I03_Target of(String s) { return new I03_Target(s); }
	}

	@Test void i03_staticOf() {
		var a = C.to("test", I03_Target.class);
		assertNotNull(a);
		assertEquals("test", a.value);
	}

	public static class I04_Target {
		public final String value;
		private I04_Target(String v) { value = v; }
		public static I04_Target create(String s) { return new I04_Target(s); }
	}

	@Test void i04_staticCreate() {
		var a = C.to("create-test", I04_Target.class);
		assertNotNull(a);
		assertEquals("create-test", a.value);
	}

	public static class I05_Target {
		public final String value;
		private I05_Target(String v) { value = v; }
		public static I05_Target parse(String s) { return new I05_Target(s); }
	}

	@Test void i05_staticParse() {
		var a = C.to("parse-test", I05_Target.class);
		assertNotNull(a);
		assertEquals("parse-test", a.value);
	}

	public static class I06_Target {
		public final String value;
		private I06_Target(String v) { value = v; }
		public static I06_Target from(String s) { return new I06_Target(s); }
	}

	@Test void i06_staticFrom() {
		var a = C.to("from-test", I06_Target.class);
		assertNotNull(a);
		assertEquals("from-test", a.value);
	}

	public static class I07_Target {
		public final String value;
		private I07_Target(String v) { value = v; }
		public static I07_Target forName(String s) { return new I07_Target(s); }
	}

	@Test void i07_staticForName() {
		var a = C.to("forName-test", I07_Target.class);
		assertNotNull(a);
		assertEquals("forName-test", a.value);
	}

	public static class I08_Target {
		public final String value;
		private I08_Target(String v) { value = v; }
		public static I08_Target fromValue(String s) { return new I08_Target(s); }
	}

	@Test void i08_staticFromValue() {
		var a = C.to("fromValue-test", I08_Target.class);
		assertNotNull(a);
		assertEquals("fromValue-test", a.value);
	}

	public static class I09_Target {
		public final String value;
		private I09_Target(String v) { value = v; }
		public static I09_Target builder(String s) { return new I09_Target(s); }
	}

	@Test void i09_staticBuilder() {
		var a = C.to("builder-test", I09_Target.class);
		assertNotNull(a);
		assertEquals("builder-test", a.value);
	}

	public static class I10_Target {
		public final String value;
		private I10_Target(String v) { value = v; }
		public static I10_Target fromString(String s) { return new I10_Target(s); }
	}

	@Test void i10_dynamicFromX() {
		var a = C.to("fromString-test", I10_Target.class);
		assertNotNull(a);
		assertEquals("fromString-test", a.value);
	}

	public static class I11_Input {
		public final String value;
		public I11_Input(String v) { value = v; }
	}

	public static class I11_Target {
		public final String value;
		private I11_Target(String v) { value = v; }
		public static I11_Target fromI11_Input(I11_Input s) { return new I11_Target(s.value); }
	}

	@Test void i11_dynamicFromInputClassName() {
		var a = C.to(new I11_Input("dynamic-from"), I11_Target.class);
		assertNotNull(a);
		assertEquals("dynamic-from", a.value);
	}

	public static class I12_Input {
		public final String value;
		public I12_Input(String v) { value = v; }
	}

	public static class I12_Target {
		public final String value;
		private I12_Target(String v) { value = v; }
		public static I12_Target forI12_Input(I12_Input s) { return new I12_Target(s.value); }
	}

	@Test void i12_dynamicForInputClassName() {
		var a = C.to(new I12_Input("dynamic-for"), I12_Target.class);
		assertNotNull(a);
		assertEquals("dynamic-for", a.value);
	}

	public static class I13_Input {
		public final String value;
		public I13_Input(String v) { value = v; }
	}

	public static class I13_Target {
		public final String value;
		private I13_Target(String v) { value = v; }
		public static I13_Target parseI13_Input(I13_Input s) { return new I13_Target(s.value); }
	}

	@Test void i13_dynamicParseInputClassName() {
		var a = C.to(new I13_Input("dynamic-parse"), I13_Target.class);
		assertNotNull(a);
		assertEquals("dynamic-parse", a.value);
	}

	public static class I14_DecoyStaticMethod {
		public static String fromString(String s) { return s; }  // returns String, not I14_DecoyStaticMethod
	}

	@Test void i14_staticMethodWrongReturnType() {
		assertThrows(InvalidConversionException.class, () -> C.to("hello", I14_DecoyStaticMethod.class));
	}

	//====================================================================================================
	// j - Reflection: public constructors
	//====================================================================================================

	public static class J01_Target {
		public final String value;
		public J01_Target(String v) { value = v; }
	}

	@Test void j01_constructorFromString() {
		var a = C.to("ctor-test", J01_Target.class);
		assertNotNull(a);
		assertEquals("ctor-test", a.value);
	}

	public static class J02_Target {
		public final int value;
		public J02_Target(Integer v) { value = v; }
	}

	@Test void j02_constructorFromInteger() {
		var a = C.to(99, J02_Target.class);
		assertNotNull(a);
		assertEquals(99, a.value);
	}

	//====================================================================================================
	// k - Reflection: toX() instance methods
	//====================================================================================================

	public static class K01_Source {
		private final int value;
		public K01_Source(int v) { value = v; }
		public Integer toInteger() { return value; }
	}

	@Test void k01_toXMethod() {
		var a = C.to(new K01_Source(42), Integer.class);
		assertNotNull(a);
		assertEquals(42, a);
	}

	public static class K02_Source {
		private final String value;
		public K02_Source(String v) { value = v; }
		public List<String> toList() { return List.of(value.split(",")); }
	}

	@Test void k02_toXMethodReturningCollection() {
		var a = C.to(new K02_Source("a,b,c"), List.class);
		assertNotNull(a);
		assertEquals(List.of("a", "b", "c"), a);
	}

	public static class K03_WrongReturnType {
		public String toInteger() { return "not-an-integer"; }  // returns String, not Integer
	}

	@Test void k03_toXMethodWrongReturnType() {
		// toInteger() exists but returns String — hits line 488 false branch (hasReturnTypeParent fails)
		assertThrows(InvalidConversionException.class, () -> C.to(new K03_WrongReturnType(), Integer.class));
	}

	//====================================================================================================
	// l - Special case conversions (TimeZone, Locale, String→Boolean)
	//====================================================================================================

	@Test void l01_stringToTimeZone() {
		var a = C.to("GMT", TimeZone.class);
		assertNotNull(a);
		assertEquals("GMT", a.getID());
	}

	@Test void l02_stringToTimeZoneWithOffset() {
		var a = C.to("America/New_York", TimeZone.class);
		assertNotNull(a);
		assertEquals("America/New_York", a.getID());
	}

	@Test void l03_timeZoneToString() {
		var a = C.to(TimeZone.getTimeZone("PST"), String.class);
		assertEquals("PST", a);
	}

	@Test void l04_timeZoneToNonString() {
		assertThrows(InvalidConversionException.class, () -> C.to(TimeZone.getTimeZone("PST"), Integer.class));
	}

	@Test void l05_stringToLocale() {
		var a = C.to("en-US", Locale.class);
		assertNotNull(a);
		assertEquals("en", a.getLanguage());
		assertEquals("US", a.getCountry());
	}

	@Test void l06_stringToLocaleWithUnderscore() {
		var a = C.to("en_US", Locale.class);
		assertNotNull(a);
		assertEquals("en", a.getLanguage());
		assertEquals("US", a.getCountry());
	}

	@Test void l07_stringToBooleanEmpty() {
		assertNull(C.to("", Boolean.class));
	}

	@Test void l08_stringToBooleanNull() {
		assertNull(C.to("null", Boolean.class));
	}

	@Test void l09_stringToBooleanTrue() {
		assertEquals(true, C.to("true", Boolean.class));
	}

	@Test void l10_stringToBooleanFalse() {
		assertEquals(false, C.to("false", Boolean.class));
	}

	//====================================================================================================
	// m - Identity / assignability
	//====================================================================================================

	@Test void m01_identity() {
		var a = "hello";
		assertSame(a, C.to(a, String.class));
	}

	@Test void m02_widening() {
		var a = new ArrayList<>(List.of("a", "b"));
		var b = C.to(a, List.class);
		assertSame(a, b);
	}

	@Test void m03_numberWidening() {
		var a = Integer.valueOf(42);
		var b = C.to(a, Number.class);
		assertSame(a, b);
	}

	//====================================================================================================
	// n - Null handling
	//====================================================================================================

	@Test void n01_nullInput() {
		assertNull(C.to(null, String.class));
		assertNull(C.to(null, Integer.class));
		assertNull(C.to(null, List.class));
		assertNull(C.to(null, Map.class));
		assertNull(C.to(null, int.class));
	}

	//====================================================================================================
	// o - No conversion available
	//====================================================================================================

	@Test void o01_noConversion() {
		assertThrows(InvalidConversionException.class, () -> C.to(new Object(), BasicConverter_Test.class));
	}

	//====================================================================================================
	// p - Non-static inner class constructor (memberOf path)
	//====================================================================================================

	// Non-static inner class with a String constructor; the JVM synthesizes Inner(BasicConverter_Test, String).
	public class P01_Inner {
		public final String value;
		public P01_Inner(String s) { this.value = s; }
	}

	// Non-static inner class whose only constructor takes a different input type (no match for String).
	public class P02_InnerNoMatch {
		public final int value;
		public P02_InnerNoMatch(int n) { this.value = n; }
	}

	@Test void p01_innerClassConstructorWithMemberOf() {
		// to(o, memberOf, Class) routes through the inner-class path in findConstructorConversion;
		// memberOf (this) is passed as the synthetic outer-instance parameter.
		var x = C.to("hello", this, P01_Inner.class);
		assertEquals("hello", x.value);
	}

	@Test void p02_innerClassNoCtorMatchFallsThrough() {
		// The inner-class branch is entered (P02 has an enclosing class) but the 2-param ctor
		// (BasicConverter_Test, String) does not exist — falls through to the 1-param check which also
		// fails → InvalidConversionException.
		assertThrows(InvalidConversionException.class, () -> C.to("hello", this, P02_InnerNoMatch.class));
	}

	//====================================================================================================
	// q - newCollection AbstractList branch
	//====================================================================================================

	@Test void q01_collectionToAbstractList() {
		// Hits the AbstractList.class branch in newCollection, which returns a new ArrayList.
		var result = C.to(List.of("a", "b"), (Type) AbstractList.class);
		assertNotNull(result);
		assertInstanceOf(List.class, result);
		assertEquals(2, ((List<?>) result).size());
	}
}
