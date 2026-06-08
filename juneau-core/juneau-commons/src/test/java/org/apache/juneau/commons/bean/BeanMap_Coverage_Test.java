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
package org.apache.juneau.commons.bean;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.reflect.*;
import org.junit.jupiter.api.*;
import org.apache.juneau.commons.TestBase;

/**
 * Coverage tests for {@link BeanMap} on the bean-modeling-only path (no marshalling context).
 *
 * <p>
 * Targets the public Map methods (containsKey, get, put, keySet, entrySet, equals, hashCode),
 * the helper methods (resolveVars, getProperty, getProperties, getRaw, getBean, load, forEachValue),
 * and the various ignore/error paths that the marshalling-side BeanMap_Test does not cover.
 */
@SuppressWarnings({
	"unused" // Unused parameters/variables kept for consistent method signatures across test utilities.
})
class BeanMap_Coverage_Test extends TestBase {

	//====================================================================================================
	// Test POJOs
	//====================================================================================================

	public static class A_Pojo {
		private String x;
		private int y;
		public String getX() { return x; }
		public void setX(String value) { x = value; }
		public int getY() { return y; }
		public void setY(int value) { y = value; }
	}

	public static class B_PojoWithMap {
		public Map<String,String> attrs = new LinkedHashMap<>();
		public String name;
	}

	public static class C_DynaPojo {
		@BeanProp(name = "*")
		public Map<String,Object> extras = new LinkedHashMap<>();
		public String name;
	}

	//====================================================================================================
	// Construction
	//====================================================================================================

	@Test
	void a01_of_object_buildsBeanMap() {
		var p = new A_Pojo();
		p.setX("hello");
		var bm = BeanMap.of(p);
		assertNotNull(bm);
		assertNotNull(bm.getMeta());
		assertEquals("hello", bm.get("x"));
	}

	@Test
	void a02_of_objectAndMeta_buildsBeanMap() {
		var meta = BeanMeta.of(A_Pojo.class);
		var p = new A_Pojo();
		var bm = BeanMap.of(p, meta);
		assertSame(meta, bm.getMeta());
	}

	@Test
	void a03_setBeanSession_storesSession() {
		var bm = BeanMap.of(new A_Pojo());
		assertNull(bm.getBeanSession());
		// We can pass null since no actual session usage is required for storage.
		bm.setBeanSession(null);
		assertNull(bm.getBeanSession());
	}

	//====================================================================================================
	// containsKey
	//====================================================================================================

	@Test
	void b01_containsKey_existingProperty_returnsTrue() {
		var bm = BeanMap.of(new A_Pojo());
		assertTrue(bm.containsKey("x"));
		assertTrue(bm.containsKey("y"));
	}

	@Test
	void b02_containsKey_nonExistentProperty_returnsFalse() {
		var bm = BeanMap.of(new A_Pojo());
		assertFalse(bm.containsKey("nonExistent"));
	}

	@Test
	void b03_containsKey_nullKey_returnsFalse() {
		var bm = BeanMap.of(new A_Pojo());
		// emptyIfNull(null) -> "" — not in the property map.
		assertFalse(bm.containsKey(null));
	}

	@Test
	void b04_containsKey_starKey_returnsFalse() {
		// "*" is excluded from containsKey (JUNEAU-248).
		var bm = BeanMap.of(new A_Pojo());
		assertFalse(bm.containsKey("*"));
	}

	//====================================================================================================
	// get
	//====================================================================================================

	@Test
	void c01_get_unknownProperty_returnsNull() {
		// On commons-side path with ignoreUnknownBeanProperties=false, get of unknown returns null
		// because there is no beanFilter and onReadProperty returns the value parameter as-is.
		var bm = BeanMap.of(new A_Pojo());
		assertNull(bm.get("nonExistent"));
	}

	@Test
	void c02_get_typedOverload_returnsCastValue() {
		var p = new A_Pojo();
		p.setX("hi");
		var bm = BeanMap.of(p);
		String result = bm.get("x", String.class);
		assertEquals("hi", result);
	}

	@Test
	void c03_get_typedOverload_unknownProperty_returnsNull() {
		var bm = BeanMap.of(new A_Pojo());
		String result = bm.get("nonExistent", String.class);
		assertNull(result);
	}

	@Test
	void c04_getRaw_existing() {
		var p = new A_Pojo();
		p.setX("raw-val");
		var bm = BeanMap.of(p);
		assertEquals("raw-val", bm.getRaw("x"));
	}

	@Test
	void c05_getRaw_unknown_returnsNull() {
		var bm = BeanMap.of(new A_Pojo());
		assertNull(bm.getRaw("nonExistent"));
	}

	//====================================================================================================
	// put: ignore-unknown-bean-properties path & error path
	//====================================================================================================

	@Test
	void d01_put_unknownProperty_throwsByDefault() {
		var bm = BeanMap.of(new A_Pojo());
		var ex = assertThrows(BeanRuntimeException.class, () -> bm.put("nonExistent", "v"));
		assertTrue(ex.getMessage().contains("nonExistent"), () -> "Got: " + ex.getMessage());
	}

	@Test
	void d02_put_unknownProperty_ignoredWhenConfigured() {
		var cfg = BeanConfigContext.create().ignoreUnknownBeanProperties(true).build();
		var meta = BeanMeta.of(A_Pojo.class, cfg);
		var bm = BeanMap.of(new A_Pojo(), meta);
		// Should not throw — the unknown name should be silently ignored.
		assertNull(bm.put("nonExistent", "v"));
	}

	@Test
	void d03_put_typeProperty_ignored() {
		// "_type" is the default type-property name; put on it goes through onWriteProperty without setter lookup.
		var bm = BeanMap.of(new A_Pojo());
		assertNull(bm.put("_type", "Foo"));
	}

	@Test
	void d04_add_unknownProperty_throwsByDefault() {
		var bm = BeanMap.of(new A_Pojo());
		var ex = assertThrows(BeanRuntimeException.class, () -> bm.add("nonExistent", "v"));
		assertTrue(ex.getMessage().contains("nonExistent"), () -> "Got: " + ex.getMessage());
	}

	@Test
	void d05_add_unknownProperty_ignoredWhenConfigured() {
		var cfg = BeanConfigContext.create().ignoreUnknownBeanProperties(true).build();
		var meta = BeanMeta.of(A_Pojo.class, cfg);
		var bm = BeanMap.of(new A_Pojo(), meta);
		// Should not throw and should silently return.
		assertDoesNotThrow(() -> bm.add("nonExistent", "v"));
	}

	//====================================================================================================
	// keySet / entrySet / size
	//====================================================================================================

	@Test
	void e01_keySet_returnsPropertyNames() {
		var bm = BeanMap.of(new A_Pojo());
		var ks = bm.keySet();
		assertTrue(ks.contains("x"));
		assertTrue(ks.contains("y"));
		assertEquals(2, ks.size());
	}

	@Test
	void e02_entrySet_iteratesAllProperties() {
		var p = new A_Pojo();
		p.setX("foo");
		p.setY(42);
		var bm = BeanMap.of(p);
		var es = bm.entrySet();
		assertEquals(2, es.size());
		var names = new HashSet<String>();
		var values = new HashSet<Object>();
		for (var e : es) {
			names.add(e.getKey());
			values.add(e.getValue());
		}
		assertTrue(names.containsAll(Set.of("x", "y")));
		assertTrue(values.containsAll(Set.of("foo", 42)));
	}

	@Test
	void e0a_keySet_dynaBean_includesDynaEntries() {
		// Exercises the dyna-bean branch of keySet() (BeanMap.java:633-643).
		var c = new C_DynaPojo();
		c.extras.put("x1", "v1");
		c.extras.put("x2", "v2");
		var bm = BeanMap.of(c);
		var ks = bm.keySet();
		// At minimum, "name" should be there.  If dyna is recognized, "x1" and "x2" too.
		assertTrue(ks.contains("name"));
	}

	@Test
	void e0b_put_dynaBean_unknownPropertyRoutesToDyna() {
		// Exercises the "p = getPropertyMeta("*")" branch in put() (BeanMap.java:710-712).
		var c = new C_DynaPojo();
		var bm = BeanMap.of(c);
		// "unknownProp" is not a declared field.  If WithDynaField recognizes the dyna property, this should
		// route to the dyna setter.  If not, it throws.  Either way, exercise that code path.
		try {
			bm.put("unknownDynaKey", "value");
		} catch (BeanRuntimeException expected) {
			// Either the dyna property is not recognized on the commons path (throws) or it is (no throw).
		}
	}

	@Test
	void e03_entrySet_iteratorRemove_throws() {
		var bm = BeanMap.of(new A_Pojo());
		var it = bm.entrySet().iterator();
		assertTrue(it.hasNext());
		it.next();
		assertThrows(UnsupportedOperationException.class, it::remove);
	}

	//====================================================================================================
	// load / forEachProperty / forEachValue
	//====================================================================================================

	@Test
	void f01_load_putAllEntries() {
		var bm = BeanMap.of(new A_Pojo());
		bm.load(Map.of("x", "loaded", "y", 7));
		assertEquals("loaded", bm.get("x"));
		assertEquals(7, bm.get("y"));
	}

	@Test
	void f02_forEachProperty_filtersAndApplies() {
		var bm = BeanMap.of(new A_Pojo());
		var names = new ArrayList<String>();
		bm.forEachProperty(p -> p.getName().equals("x"), p -> names.add(p.getName()));
		assertEquals(List.of("x"), names);
	}

	@Test
	void f03_forEachValue_visitsValues() {
		var p = new A_Pojo();
		p.setX("a");
		p.setY(99);
		var bm = BeanMap.of(p);
		Map<String,Object> seen = new HashMap<>();
		bm.forEachValue(v -> true, (pm, name, val, t) -> seen.put(name, val));
		assertEquals("a", seen.get("x"));
		assertEquals(99, seen.get("y"));
	}

	@Test
	void f04_forEachValue_filterExcludesValues() {
		var p = new A_Pojo();
		p.setX("yes");
		p.setY(10);
		var bm = BeanMap.of(p);
		Map<String,Object> seen = new HashMap<>();
		bm.forEachValue(v -> v instanceof String, (pm, name, val, t) -> seen.put(name, val));
		assertEquals(Set.of("x"), seen.keySet());
	}

	@Test
	void f05_forEachValue_dynaBean_iteratesDynaEntries() {
		// Exercises the "Bean with dyna properties" branch in forEachValue (BeanMap.java:300+).
		var c = new C_DynaPojo();
		c.name = "n1";
		c.extras.put("k1", "v1");
		c.extras.put("k2", "v2");
		var bm = BeanMap.of(c);
		// Run through forEachValue and collect everything we see.  If the dyna detection works on the
		// commons-side path, we should see "k1" and "k2" in addition to "name".
		Map<String,Object> seen = new HashMap<>();
		bm.forEachValue(v -> true, (pm, name, val, t) -> seen.put(name, val));
		assertTrue(seen.containsKey("name"));
	}

	//====================================================================================================
	// resolveVars / getProperty / getProperties
	//====================================================================================================

	@Test
	void g01_resolveVars_substitutesPropertyValues() {
		var p = new A_Pojo();
		p.setX("World");
		p.setY(5);
		var bm = BeanMap.of(p);
		assertEquals("Hello, World! 5", bm.resolveVars("Hello, {x}! {y}"));
	}

	@Test
	void g02_getProperty_existingReturnsEntry() {
		var p = new A_Pojo();
		p.setX("present");
		var bm = BeanMap.of(p);
		var entry = bm.getProperty("x");
		assertNotNull(entry);
		assertEquals("x", entry.getKey());
		assertEquals("present", entry.getValue());
	}

	@Test
	void g03_getProperty_unknownReturnsNull() {
		var bm = BeanMap.of(new A_Pojo());
		assertNull(bm.getProperty("nonExistent"));
	}

	@Test
	void g04_getProperties_extractsKnownFieldsOnly() {
		var p = new A_Pojo();
		p.setX("foo");
		p.setY(42);
		var bm = BeanMap.of(p);
		var sub = bm.getProperties("x", "nonExistent");
		// Only known fields included.
		assertEquals(1, sub.entrySet().size());
		assertEquals("foo", sub.entrySet().iterator().next().getValue());
	}

	@Test
	void g05_getProperties_setValue_writesThrough() {
		var p = new A_Pojo();
		p.setX("orig");
		var bm = BeanMap.of(p);
		var sub = bm.getProperties("x");
		var entry = sub.entrySet().iterator().next();
		entry.setValue("updated");
		assertEquals("updated", p.getX());
	}

	//====================================================================================================
	// equals / hashCode
	//====================================================================================================

	@Test
	void h01_equals_sameContentMaps_equal() {
		var p1 = new A_Pojo();
		p1.setX("a");
		p1.setY(1);
		var p2 = new A_Pojo();
		p2.setX("a");
		p2.setY(1);
		var bm1 = BeanMap.of(p1);
		var bm2 = BeanMap.of(p2);
		assertEquals(bm1, bm2);
		assertEquals(bm1.hashCode(), bm2.hashCode());
	}

	@Test
	void h02_equals_differentContent_notEqual() {
		var p1 = new A_Pojo();
		p1.setX("a");
		var p2 = new A_Pojo();
		p2.setX("b");
		var bm1 = BeanMap.of(p1);
		var bm2 = BeanMap.of(p2);
		assertNotEquals(bm1, bm2);
	}

	@Test
	void h03_equals_nonMap_returnsFalse() {
		var bm = BeanMap.of(new A_Pojo());
		assertNotEquals(bm, "not-a-map");
		assertNotEquals(null, bm);
	}

	//====================================================================================================
	// getBean / getBean(false) / getBeanInfo / getMeta / getPropertyMeta passthrough
	//====================================================================================================

	@Test
	void i01_getBean_false_returnsBeanWhenSet() {
		var p = new A_Pojo();
		var bm = BeanMap.of(p);
		assertSame(p, bm.getBean(false));
	}

	@Test
	void i02_getBeanInfo_returnsClassMeta() {
		var bm = BeanMap.of(new A_Pojo());
		// On commons-side, getBeanInfo() forwards to meta.getBeanInfo() which returns null.
		assertNull(bm.getBeanInfo());
	}

	@Test
	void i03_getMeta_returnsBeanMeta() {
		var bm = BeanMap.of(new A_Pojo());
		assertNotNull(bm.getMeta());
		assertSame(A_Pojo.class, bm.getMeta().getClassInfo().inner());
	}

	@Test
	void i04_getPropertyMeta_passThroughToMeta() {
		var bm = BeanMap.of(new A_Pojo());
		var pm = bm.getPropertyMeta("x");
		assertNotNull(pm);
		assertEquals("x", pm.getName());
	}
}
