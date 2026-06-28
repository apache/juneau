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
package org.apache.juneau.collections;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json5.*;
import org.junit.jupiter.api.*;

/**
 * Smoke tests for the marshaller-neutral {@link MarshalledMap} base class.
 *
 * <p>
 * These tests exercise the neutral base directly to confirm that:
 * <ul>
 * 	<li>{@link MarshalledMap#toString()} produces the inherited {@link LinkedHashMap} form (not JSON).
 * 	<li>{@link MarshalledMap#getMap} / {@link MarshalledMap#getList} return the neutral types.
 * 	<li>Construction via the parser-required ctors works.
 * 	<li>Bean conversion via {@link MarshalledMap#cast(Class)} still works.
 * </ul>
 */
class MarshalledMap_Test extends TestBase {

	@Test void a01_emptyToString() {
		var m = new MarshalledMap();
		assertEquals("{}", m.toString());
	}

	@Test void a02_toStringIsLinkedHashMapForm() {
		var m = new MarshalledMap("a", 1, "b", "two");
		// LinkedHashMap-style output, NOT JSON5.
		assertEquals("{a=1, b=two}", m.toString());
	}

	@Test void a03_factoryOf() {
		var m = MarshalledMap.of("k1", "v1", "k2", 2);
		assertEquals(2, m.size());
		assertEquals("v1", m.getString("k1"));
		assertEquals(2, m.getInt("k2"));
	}

	@Test void a04_factoryCreateAndAppend() {
		var m = MarshalledMap.create().append("a", 1).append("b", 2);
		assertEquals(2, m.size());
		assertEquals(1, m.getInt("a"));
	}

	@Test void a05_parseViaOfString() {
		var m = MarshalledMap.ofString("{a:1,b:'two'}", Json5Parser.DEFAULT);
		assertNotNull(m);
		assertEquals(1, m.getInt("a"));
		assertEquals("two", m.getString("b"));
	}

	@Test void a06_parseViaOfStringNullInput() {
		var m = MarshalledMap.ofString((CharSequence)null, Json5Parser.DEFAULT);
		assertNotNull(m);
		assertTrue(m.isEmpty());
		assertEquals(MarshalledMap.class, m.getClass());
		m.put("x", 1);
		assertEquals(1, m.size());
	}

	@Test void a06c_parseViaOfStringNullReader() {
		var m = MarshalledMap.ofString((java.io.Reader)null, Json5Parser.DEFAULT);
		assertNotNull(m);
		assertTrue(m.isEmpty());
		assertEquals(MarshalledMap.class, m.getClass());
		m.put("x", 1);
		assertEquals(1, m.size());
	}

	@Test void a06b_ofTextAliasStillWorks() {
		var m = MarshalledMap.ofString("{a:1}", Json5Parser.DEFAULT);
		assertNotNull(m);
		assertEquals(1, m.getInt("a"));
	}

	@Test void a07_getMapReturnsNeutralType() {
		var nested = new MarshalledMap("x", 1);
		var m = new MarshalledMap("nested", nested);
		var got = m.getMap("nested");
		assertTrue(got instanceof MarshalledMap, "Expected MarshalledMap, got " + cn(got));
		assertFalse(got instanceof JsonMap, "Stored MarshalledMap should not be returned as a JsonMap");
		assertEquals(1, got.getInt("x"));
	}

	@Test void a08_getListReturnsNeutralType() {
		var list = MarshalledList.of(1, 2, 3);
		var m = new MarshalledMap("arr", list);
		var got = m.getList("arr");
		assertTrue(got instanceof MarshalledList, "Expected MarshalledList, got " + cn(got));
		assertFalse(got instanceof JsonList, "Stored MarshalledList should not be returned as a JsonList");
		assertEquals(3, got.size());
	}

	@Test void a08b_parseProducesJson5MapInternally() {
		var m = MarshalledMap.ofString("{nested:{x:1}}", Json5Parser.DEFAULT);
		var nested = m.getMap("nested");
		assertTrue(nested instanceof Json5Map, "Json5Parser produces Json5Map for nested objects");
		assertFalse(nested instanceof JsonMap, "Strict-JSON JsonMap is not used by Json5Parser");
	}

	@Test void a09_getMapCreateIfNotExists() {
		var m = MarshalledMap.create();
		var nested = m.getMap("nested", true);
		assertNotNull(nested);
		assertTrue(nested instanceof MarshalledMap);
		assertSame(nested, m.getMap("nested"));
	}

	@Test void a10_getListCreateIfNotExists() {
		var m = MarshalledMap.create();
		var list = m.getList("items", true);
		assertNotNull(list);
		assertTrue(list instanceof MarshalledList);
		assertSame(list, m.getList("items"));
	}

	@Test void a11_typedAccessors() {
		var m = MarshalledMap.of("i", "42", "b", "true", "l", "9999999999", "s", 123);
		assertEquals(42, m.getInt("i"));
		assertTrue(m.getBoolean("b"));
		assertEquals(9999999999L, m.getLong("l"));
		assertEquals("123", m.getString("s"));
	}

	@Test void a12_filterAndExclude() {
		var m = MarshalledMap.of("a", 1, "b", 2, "c", 3);
		var excluded = m.exclude("b");
		assertEquals(2, excluded.size());
		assertTrue(excluded instanceof MarshalledMap);
		assertFalse(excluded.containsKey("b"));
	}

	@Test void a13_includeReturnsNeutralType() {
		var m = MarshalledMap.of("a", 1, "b", 2, "c", 3);
		var included = m.include("a", "c");
		assertTrue(included instanceof MarshalledMap);
		assertEquals(2, included.size());
		assertTrue(included.containsKey("a"));
		assertTrue(included.containsKey("c"));
	}

	@Test void a14_unmodifiable() {
		var m = MarshalledMap.of("a", 1).unmodifiable();
		assertTrue(m.isUnmodifiable());
		var mapB = Map.of("b", 2);
		assertThrows(UnsupportedOperationException.class, () -> m.put("b", 2));
		assertThrows(UnsupportedOperationException.class, () -> m.remove("a"));
		assertThrows(UnsupportedOperationException.class, () -> m.putAll(mapB));
		assertThrows(UnsupportedOperationException.class, m::clear);
		assertThrows(UnsupportedOperationException.class, () -> m.putIfAbsent("b", 2));
		assertThrows(UnsupportedOperationException.class, () -> m.remove("a", 1));
		assertThrows(UnsupportedOperationException.class, () -> m.replace("a", 2));
		assertThrows(UnsupportedOperationException.class, () -> m.replace("a", 1, 2));
		assertThrows(UnsupportedOperationException.class, () -> m.replaceAll((k, v) -> 99));
		assertThrows(UnsupportedOperationException.class, () -> m.compute("a", (k, v) -> 99));
		assertThrows(UnsupportedOperationException.class, () -> m.computeIfAbsent("b", k -> 99));
		assertThrows(UnsupportedOperationException.class, () -> m.computeIfPresent("a", (k, v) -> 99));
		assertThrows(UnsupportedOperationException.class, () -> m.merge("a", 99, (v1, v2) -> 99));
		var entryIt = m.entrySet().iterator();
		entryIt.next();
		assertThrows(UnsupportedOperationException.class, entryIt::remove);
		var keyIt = m.keySet().iterator();
		keyIt.next();
		assertThrows(UnsupportedOperationException.class, keyIt::remove);
		var valIt = m.values().iterator();
		valIt.next();
		assertThrows(UnsupportedOperationException.class, valIt::remove);
		assertEquals(1, m.size());
	}

	@Test void a15_innerMap() {
		var inner = MarshalledMap.of("a", 1);
		var outer = MarshalledMap.of("b", 2).inner(inner);
		assertEquals(1, outer.getInt("a"));
		assertEquals(2, outer.getInt("b"));
	}

	@Test void a16_findAcrossKeys() {
		var m = MarshalledMap.of("alt", "value");
		assertEquals("value", m.findString("primary", "alt"));
	}

	@Test void a17_castToMapInterfaceReturnsNeutralFallback() {
		var m = MarshalledMap.ofString("{a:1,b:2}", Json5Parser.DEFAULT);
		// Casting from a MarshalledMap to the Map interface should fall back to MarshalledMap.
		Map<?,?> casted = m.cast(Map.class);
		assertNotNull(casted);
		assertTrue(casted instanceof MarshalledMap);
		assertFalse(casted instanceof JsonMap);
	}

	@Test void a18_emptyMapConstant() {
		assertTrue(MarshalledMap.EMPTY_MAP.isEmpty());
		assertThrows(UnsupportedOperationException.class, () -> MarshalledMap.EMPTY_MAP.put("x", 1));
	}

	@Test void a19_keyValuePairCtor() {
		var m = new MarshalledMap("a", 1, "b", 2);
		assertEquals(1, m.getInt("a"));
		assertEquals(2, m.getInt("b"));
	}

	@Test void a20_oddKeyValuePairsThrows() {
		assertThrows(IllegalArgumentException.class, () -> new MarshalledMap("a", 1, "b"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Additional coverage for previously uncovered methods.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_factoryFilteredMap_empty() {
		var m = MarshalledMap.filteredMap();
		assertNotNull(m);
		// Filtered map skips null/false/-1/empty values.
		m.put("a", null);
		m.put("b", false);
		m.put("c", -1);
		m.put("d", "kept");
		assertEquals(1, m.size());
		assertTrue(m.containsKey("d"));
	}

	@Test void b02_factoryFilteredMap_withPairs() {
		// filteredMap(pairs) loads pairs first, then enables filtering for further puts.
		var m = MarshalledMap.filteredMap("a", "v1", "b", "v2");
		assertEquals(2, m.size());
		// Subsequent puts respect the filter.
		m.put("c", null);
		assertFalse(m.containsKey("c"));
	}

	@Test void b03_factoryOfMap() {
		var src = new LinkedHashMap<String,Object>();
		src.put("a", 1);
		src.put("b", 2);
		var m = MarshalledMap.of(src);
		assertEquals(2, m.size());
		assertEquals(1, m.getInt("a"));
	}

	@Test void b04_factoryOfMapNull() {
		assertNull(MarshalledMap.of((Map<?,?>)null));
	}

	@Test void b05_constructorMap() {
		var src = new LinkedHashMap<Object,Object>();
		src.put(1, "v1");
		src.put("k2", "v2");
		var m = new MarshalledMap(src);
		// Keys converted via toString().
		assertEquals("v1", m.getString("1"));
		assertEquals("v2", m.getString("k2"));
	}

	@Test void b06_constructorMapNull() {
		var m = new MarshalledMap((Map<?,?>)null);
		assertTrue(m.isEmpty());
	}

	@Test void b07_appendMap() {
		var m = MarshalledMap.create();
		var src = new LinkedHashMap<String,Object>();
		src.put("x", 1);
		src.put("y", 2);
		m.append(src);
		assertEquals(2, m.size());
	}

	@Test void b08_appendMap_null() {
		var m = MarshalledMap.create().append((Map<String,Object>)null);
		assertTrue(m.isEmpty());
	}

	@Test void b09_appendFirstMatchingPredicate() {
		var m = MarshalledMap.create();
		// String predicate: first non-blank.
		m.appendFirst(s -> ((String)s).length() > 1, "key", "x", "ab", "cd");
		assertEquals("ab", m.getString("key"));
	}

	@Test void b10_appendFirstNoMatch() {
		var m = MarshalledMap.create();
		m.appendFirst(s -> false, "key", "x", "y");
		assertFalse(m.containsKey("key"));
	}

	@Test void b11_appendIfFlagTrue() {
		var m = MarshalledMap.create().appendIf(true, "k", "v");
		assertEquals("v", m.getString("k"));
	}

	@Test void b12_appendIfFlagFalse() {
		var m = MarshalledMap.create().appendIf(false, "k", "v");
		assertFalse(m.containsKey("k"));
	}

	@Test void b13_appendIfPredicateTrue() {
		var m = MarshalledMap.create().appendIf(s -> !((String)s).isEmpty(), "k", "value");
		assertEquals("value", m.getString("k"));
	}

	@Test void b14_appendIfPredicateFalse() {
		var m = MarshalledMap.create().appendIf(s -> ((String)s).length() > 100, "k", "value");
		assertFalse(m.containsKey("k"));
	}

	@Test void b15_appendIfAbsent_addsWhenAbsent() {
		var m = MarshalledMap.create();
		m.appendIfAbsent("k", "v1");
		assertEquals("v1", m.getString("k"));
	}

	@Test void b16_appendIfAbsent_skipWhenPresent() {
		var m = MarshalledMap.of("k", "existing");
		m.appendIfAbsent("k", "new");
		assertEquals("existing", m.getString("k"));
	}

	@Test void b17_appendIfAbsentIf() {
		var m = MarshalledMap.create();
		m.appendIfAbsentIf(s -> ((String)s).startsWith("v"), "k", "value");
		assertEquals("value", m.getString("k"));
		// Try again with a value that does not match the predicate.
		m.appendIfAbsentIf(s -> ((String)s).startsWith("z"), "k2", "value");
		assertFalse(m.containsKey("k2"));
	}

	@Test void b18_toStringWithSerializer() {
		var m = MarshalledMap.of("a", 1);
		var s = m.toString(Json5Serializer.DEFAULT);
		assertNotNull(s);
		assertTrue(s.contains("a"));
	}

	@Test void b19_containsKeyNotEmpty() {
		var m = MarshalledMap.of("a", "value", "b", "", "c", null, "d", 42);
		assertTrue(m.containsKeyNotEmpty("a"));
		assertFalse(m.containsKeyNotEmpty("b"));   // empty string
		assertFalse(m.containsKeyNotEmpty("c"));   // null
		assertFalse(m.containsKeyNotEmpty("d"));   // not a CharSequence
		assertFalse(m.containsKeyNotEmpty("none")); // missing
	}

	@Test void b20_containsOuterKey() {
		var inner = MarshalledMap.of("x", 1);
		var outer = MarshalledMap.of("y", 2).inner(inner);
		assertTrue(outer.containsOuterKey("y"));
		assertFalse(outer.containsOuterKey("x")); // lives in inner only
	}

	@Test void b21_containsKeyTraversesInner() {
		var inner = MarshalledMap.of("x", 1);
		var outer = MarshalledMap.of("y", 2).inner(inner);
		assertTrue(outer.containsKey("x"));
		assertTrue(outer.containsKey("y"));
	}

	@Test void b22_entrySetWithInner_iterate() {
		var inner = MarshalledMap.of("x", 1);
		var outer = MarshalledMap.of("y", 2).inner(inner);
		var es = outer.entrySet();
		// Should see both keys.
		var keys = new HashSet<String>();
		es.forEach(e -> keys.add(e.getKey()));
		assertTrue(keys.contains("x"));
		assertTrue(keys.contains("y"));
		assertEquals(2, es.size());
	}

	@Test void b23_entrySetWithInner_setValue() {
		var inner = MarshalledMap.of("x", 1);
		var outer = MarshalledMap.of("y", 2).inner(inner);
		var it = outer.entrySet().iterator();
		while (it.hasNext()) {
			var e = it.next();
			if (e.getKey().equals("y"))
				e.setValue(99);
		}
		assertEquals(99, outer.getInt("y"));
	}

	@Test void b24_entrySetWithInner_removeThrows() {
		var inner = MarshalledMap.of("x", 1);
		var outer = MarshalledMap.of("y", 2).inner(inner);
		var it = outer.entrySet().iterator();
		it.next();
		assertThrows(UnsupportedOperationException.class, it::remove);
	}

	@Test void b25_findFirstKey() {
		var m = MarshalledMap.of("a", 1, "b", 2);
		assertEquals(1, m.find("a", "b"));
		assertEquals(1, m.find("missing", "a"));
		assertNull(m.find("none1", "none2"));
	}

	@Test void b26_findClassType() {
		var m = MarshalledMap.of("k", "42");
		assertEquals(Integer.valueOf(42), m.find(Integer.class, "k"));
	}

	@Test void b27_findBoolean() {
		var m = MarshalledMap.of("k", "true");
		assertTrue(m.findBoolean("missing", "k"));
	}

	@Test void b28_findInt() {
		var m = MarshalledMap.of("k", "42");
		assertEquals(Integer.valueOf(42), m.findInt("missing", "k"));
	}

	@Test void b29_findLong() {
		var m = MarshalledMap.of("k", "42");
		assertEquals(Long.valueOf(42L), m.findLong("missing", "k"));
	}

	@Test void b30_findKeyIgnoreCase() {
		var m = MarshalledMap.of("FOO", 1);
		assertEquals("FOO", m.findKeyIgnoreCase("foo"));
		assertNull(m.findKeyIgnoreCase("bar"));
	}

	@Test void b31_findList() {
		var inner = MarshalledList.of("a", "b");
		var m = MarshalledMap.of("k", inner);
		var got = m.findList("k");
		assertNotNull(got);
		assertEquals(2, got.size());
	}

	@Test void b32_findMap() {
		var nested = MarshalledMap.of("x", 1);
		var m = MarshalledMap.of("k", nested);
		var got = m.findMap("k");
		assertNotNull(got);
		assertEquals(1, got.getInt("x"));
	}

	@Test void b33_getWithDefault() {
		var m = MarshalledMap.of("k", "v");
		assertEquals("v", m.getWithDefault("k", "def"));
		assertEquals("def", m.getWithDefault("missing", "def"));
	}

	@Test void b34_getWithDefaultClassType() {
		var m = MarshalledMap.of("k", "42");
		assertEquals(Integer.valueOf(42), m.getWithDefault("k", null, Integer.class));
		assertEquals(Integer.valueOf(99), m.getWithDefault("missing", 99, Integer.class));
	}

	@Test void b35_getStringArray_fromCollection() {
		var m = MarshalledMap.of("k", List.of("a", "b"));
		var arr = m.getStringArray("k");
		assertNotNull(arr);
		assertEquals(2, arr.length);
	}

	@Test void b36_getStringArray_fromObjectArray() {
		var m = MarshalledMap.of("k", new Object[] { "a", "b" });
		var arr = m.getStringArray("k");
		assertNotNull(arr);
		assertEquals(2, arr.length);
	}

	@Test void b37_getStringArray_fromStringArray() {
		var m = MarshalledMap.of("k", new String[] { "a", "b" });
		var arr = m.getStringArray("k");
		assertNotNull(arr);
		assertEquals(2, arr.length);
	}

	@Test void b38_getStringArray_default() {
		var m = MarshalledMap.create();
		var def = new String[] { "z" };
		assertSame(def, m.getStringArray("missing", def));
	}

	@Test void b39_getStringArray_fromString() {
		var m = MarshalledMap.of("k", "a,b,c");
		var arr = m.getStringArray("k");
		assertNotNull(arr);
		assertTrue(arr.length >= 1);
	}

	@Test void b40_getClassMeta() {
		var m = MarshalledMap.of("k", "v");
		assertNotNull(m.getClassMeta("k"));
	}

	@Test void b41_getFirstKey() {
		var m = MarshalledMap.of("first", 1, "second", 2);
		assertEquals("first", m.getFirstKey());
	}

	@Test void b42_getFirstKey_empty() {
		assertNull(MarshalledMap.create().getFirstKey());
	}

	@Test void b43_getBoolean() {
		var m = MarshalledMap.of("k", "true");
		assertTrue(m.getBoolean("k"));
		assertNull(m.getBoolean("missing"));
		assertEquals(Boolean.FALSE, m.getBoolean("missing", false));
	}

	@Test void b44_getInt() {
		var m = MarshalledMap.of("k", "42");
		assertEquals(Integer.valueOf(42), m.getInt("k"));
		assertNull(m.getInt("missing"));
		assertEquals(Integer.valueOf(99), m.getInt("missing", 99));
	}

	@Test void b45_getLong() {
		var m = MarshalledMap.of("k", "42");
		assertEquals(Long.valueOf(42L), m.getLong("k"));
		assertNull(m.getLong("missing"));
		assertEquals(Long.valueOf(99L), m.getLong("missing", 99L));
	}

	@Test void b46_getString_default() {
		var m = MarshalledMap.create();
		assertEquals("def", m.getString("missing", "def"));
	}

	@Test void b47_getList_default() {
		var m = MarshalledMap.create();
		var def = MarshalledList.of(1, 2);
		assertSame(def, m.getList("missing", def));
	}

	@Test void b48_getList_typed() {
		var m = MarshalledMap.of("k", List.of("1", "2", "3"));
		var def = new ArrayList<Integer>();
		var got = m.getList("k", Integer.class, def);
		assertEquals(3, got.size());
		assertEquals(Integer.valueOf(1), got.get(0));
	}

	@Test void b49_getList_typed_default() {
		var m = MarshalledMap.create();
		var def = new ArrayList<Integer>();
		assertSame(def, m.getList("missing", Integer.class, def));
	}

	@Test void b50_getMap_default() {
		var m = MarshalledMap.create();
		var def = MarshalledMap.of("x", 1);
		assertSame(def, m.getMap("missing", def));
	}

	@Test void b51_getMap_typed() {
		var inner = new LinkedHashMap<String,Object>();
		inner.put("a", "1");
		var m = MarshalledMap.of("k", inner);
		var def = new LinkedHashMap<String,Integer>();
		var got = m.getMap("k", String.class, Integer.class, def);
		assertEquals(1, got.get("a"));
	}

	@Test void b52_getMap_typed_default() {
		var m = MarshalledMap.create();
		var def = new LinkedHashMap<String,Integer>();
		assertSame(def, m.getMap("missing", String.class, Integer.class, def));
	}

	@Test void b53_getMapCreateIfNotExists_returnsExisting() {
		var existing = MarshalledMap.of("x", 1);
		var m = MarshalledMap.of("k", existing);
		var got = m.getMap("k", true);
		assertEquals(1, got.getInt("x"));
	}

	@Test void b54_getListCreateIfNotExists_returnsExisting() {
		var existing = MarshalledList.of(1, 2, 3);
		var m = MarshalledMap.of("k", existing);
		var got = m.getList("k", true);
		assertEquals(3, got.size());
	}

	@Test void b55_get_typeArgs() {
		var m = MarshalledMap.of("k", "42");
		Integer val = m.get("k", Integer.class);
		assertEquals(Integer.valueOf(42), val);
	}

	@Test void b56_get_parameterizedType() {
		var m = MarshalledMap.of("k", List.of("1", "2"));
		List<Integer> val = m.get("k", List.class, Integer.class);
		assertNotNull(val);
		assertEquals(2, val.size());
	}

	@Test void b57_isShortcut() {
		var m = MarshalledMap.of("flag", "true");
		assertTrue(m.is("flag", false));
		assertFalse(m.is("missing", false));
		assertTrue(m.is("missing", true));
	}

	@Test void b58_isUnmodifiable_default() {
		assertFalse(MarshalledMap.create().isUnmodifiable());
	}

	@Test void b59_keepAll() {
		var m = MarshalledMap.of("a", 1, "b", 2, "c", 3);
		m.keepAll("a", "c");
		assertEquals(2, m.size());
		assertTrue(m.containsKey("a"));
		assertTrue(m.containsKey("c"));
		assertFalse(m.containsKey("b"));
	}

	@Test void b60_keySetWithInner() {
		var inner = MarshalledMap.of("x", 1);
		var outer = MarshalledMap.of("y", 2).inner(inner);
		var keys = outer.keySet();
		assertTrue(keys.contains("x"));
		assertTrue(keys.contains("y"));
	}

	@Test void b61_modifiable_alreadyModifiable() {
		var m = MarshalledMap.of("a", 1);
		assertSame(m, m.modifiable());
	}

	@Test void b62_modifiable_fromUnmodifiable() {
		var m = MarshalledMap.of("a", 1).unmodifiable();
		var mod = m.modifiable();
		assertNotSame(m, mod);
		mod.put("b", 2); // should succeed
		assertEquals(2, mod.size());
	}

	@Test void b63_unmodifiable_idempotent() {
		var m = MarshalledMap.of("a", 1).unmodifiable();
		assertSame(m, m.unmodifiable());
	}

	@Test void b64_removeAllCollection() {
		var m = MarshalledMap.of("a", 1, "b", 2, "c", 3);
		m.removeAll(List.of("a", "c"));
		assertEquals(1, m.size());
		assertTrue(m.containsKey("b"));
	}

	@Test void b65_removeAllVarargs() {
		var m = MarshalledMap.of("a", 1, "b", 2, "c", 3);
		m.removeAll("a", "c");
		assertEquals(1, m.size());
	}

	@Test void b66_removeBoolean() {
		var m = MarshalledMap.of("k", "true");
		assertTrue(m.removeBoolean("k"));
		assertFalse(m.containsKey("k"));
	}

	@Test void b67_removeBoolean_default() {
		var m = MarshalledMap.create();
		assertEquals(Boolean.FALSE, m.removeBoolean("missing", false));
	}

	@Test void b68_removeInt() {
		var m = MarshalledMap.of("k", "42");
		assertEquals(Integer.valueOf(42), m.removeInt("k"));
		assertFalse(m.containsKey("k"));
	}

	@Test void b69_removeInt_default() {
		var m = MarshalledMap.create();
		assertEquals(Integer.valueOf(7), m.removeInt("missing", 7));
	}

	@Test void b70_removeString() {
		var m = MarshalledMap.of("k", "v");
		assertEquals("v", m.removeString("k"));
		assertFalse(m.containsKey("k"));
	}

	@Test void b71_removeString_default() {
		var m = MarshalledMap.create();
		assertEquals("def", m.removeString("missing", "def"));
	}

	@Test void b72_removeWithDefault() {
		var m = MarshalledMap.of("k", "42");
		assertEquals(Integer.valueOf(42), m.removeWithDefault("k", 0, Integer.class));
		assertFalse(m.containsKey("k"));
	}

	@Test void b73_session_setterChain() {
		var m = MarshalledMap.create();
		var ms = MarshallingContext.DEFAULT_SESSION;
		m.session(ms);
		assertSame(ms, m.getMarshallingSession());
	}

	@Test void b74_setBeanSession() {
		var m = MarshalledMap.create();
		var ms = MarshallingContext.DEFAULT_SESSION;
		m.setBeanSession(ms);
		assertSame(ms, m.getMarshallingSession());
	}

	@Test void b75_filteredCustom() {
		// Filter only retains values that start with 'k'.
		var m = MarshalledMap.create().filtered(v -> v instanceof String s && s.startsWith("k"));
		m.put("a", "kept");
		m.put("b", "skipped");
		assertEquals(1, m.size());
	}

	@Test void b76_castToBean() {
		var m = MarshalledMap.of("name", "alice", "age", 30);
		var bean = m.cast(B76_Bean.class);
		assertEquals("alice", bean.name);
		assertEquals(30, bean.age);
	}

	public static class B76_Bean {
		public String name;
		public int age;
	}

	@Test void b77_putAtAndGetAt() {
		var m = MarshalledMap.create();
		m.putAt("a", "value");
		assertEquals("value", m.getAt("a", String.class));
	}

	@Test void b78_postAt() {
		var m = MarshalledMap.create();
		m.put("list", new ArrayList<String>());
		m.postAt("list", "x");
		var list = (List<?>) m.get("list");
		assertEquals(1, list.size());
		assertEquals("x", list.get(0));
	}

	@Test void b79_deleteAt() {
		var m = MarshalledMap.of("a", "v");
		m.deleteAt("a");
		assertFalse(m.containsKey("a"));
	}

	@Test void b80_emptyMapEntrySetAndKeySetAndValues() {
		assertTrue(MarshalledMap.EMPTY_MAP.entrySet().isEmpty());
		assertTrue(MarshalledMap.EMPTY_MAP.keySet().isEmpty());
		assertTrue(MarshalledMap.EMPTY_MAP.values().isEmpty());
		assertThrows(UnsupportedOperationException.class, () -> MarshalledMap.EMPTY_MAP.remove("x"));
	}

	@Test void b81_unmodifiableConstructorWithNull() {
		// UnmodifiableMarshalledMap is private but reachable via .unmodifiable().
		// Construct from a null source by going through the public API.
		var m = MarshalledMap.create().unmodifiable();
		assertTrue(m.isUnmodifiable());
		assertEquals(0, m.size());
	}
}
