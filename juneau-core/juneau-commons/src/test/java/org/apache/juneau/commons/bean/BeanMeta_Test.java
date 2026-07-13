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

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Tests the commons-side construction path of {@link BeanMeta} via
 * {@link BeanMeta#of(Class, BeanConfigContext)}.
 *
 * <p>
 * Verifies that property discovery and raw getter/setter invocation work without a
 * {@code MarshallingContext}.  All paths that touch the marshalling-aware fields
 * ({@code classMeta}, {@code marshallingContext}, per-property {@code rawTypeMeta}) must
 * gracefully fall through to bean-modeling-only behavior.
 */
class BeanMeta_Test extends TestBase {

	//====================================================================================================
	// Test POJO
	//====================================================================================================

	public static class A_Pojo {
		private String x;
		private int y;
		public String getX() { return x; }
		public void setX(String value) { x = value; }
		public int getY() { return y; }
		public void setY(int value) { y = value; }
	}

	//====================================================================================================
	// Construction
	//====================================================================================================

	@Test
	void a01_of_class_buildsBeanMeta() {
		var bm = BeanMeta.of(A_Pojo.class);
		assertNotNull(bm);
		assertNotNull(bm.getClassInfo());
		assertNotNull(bm.getConfig());
	}

	@Test
	void a02_of_classWithExplicitConfig_buildsBeanMeta() {
		var bm = BeanMeta.of(A_Pojo.class, BeanConfigContext.DEFAULT);
		assertNotNull(bm);
		assertSame(BeanConfigContext.DEFAULT, bm.getConfig());
	}

	@Test
	void a03_commonsConstructed_marshallingContextIsNull() {
		var bm = BeanMeta.of(A_Pojo.class);
		// The public protected accessor returns null on the commons-side path.
		assertNull(bm.getBeanInfo());
	}

	//====================================================================================================
	// Property discovery
	//====================================================================================================

	@Test
	void b01_properties_discovered() {
		var bm = BeanMeta.of(A_Pojo.class);
		var props = bm.getProperties();
		assertNotNull(props);
		assertTrue(props.containsKey("x"));
		assertTrue(props.containsKey("y"));
		assertEquals(2, props.size());
	}

	@Test
	void b02_propertyMeta_hasGetterAndSetter() {
		var bm = BeanMeta.of(A_Pojo.class);
		var px = bm.getPropertyMeta("x");
		assertNotNull(px);
		assertNotNull(px.getGetter());
		assertNotNull(px.getSetter());
		// rawTypeMeta is left null on the commons-side path — type resolution is a marshalling concern.
		assertNull(px.getBeanInfo());
	}

	//====================================================================================================
	// Raw getter/setter invocation via BeanPropertyMeta / BeanMap
	//====================================================================================================

	@Test
	void c01_rawGet_returnsPropertyValue() {
		var bm = BeanMeta.of(A_Pojo.class);
		var p = new A_Pojo();
		p.setX("hello");
		p.setY(42);

		var map = BeanMap.of(p, bm);
		var px = bm.getPropertyMeta("x");
		var py = bm.getPropertyMeta("y");

		assertEquals("hello", px.get(map, "x"));
		assertEquals(42, py.get(map, "y"));
	}

	@Test
	void c02_rawSet_updatesPropertyValue() {
		var bm = BeanMeta.of(A_Pojo.class);
		var p = new A_Pojo();
		var map = BeanMap.of(p, bm);

		bm.getPropertyMeta("x").set(map, "x", "world");
		bm.getPropertyMeta("y").set(map, "y", 99);

		assertEquals("world", p.getX());
		assertEquals(99, p.getY());
	}

	@Test
	void c03_beanMap_get_put_roundTrip() {
		var bm = BeanMeta.of(A_Pojo.class);
		var p = new A_Pojo();
		var map = BeanMap.of(p, bm);

		map.put("x", "round-trip");
		map.put("y", 7);

		assertEquals("round-trip", map.get("x"));
		assertEquals(7, map.get("y"));
		assertEquals("round-trip", p.getX());
		assertEquals(7, p.getY());
	}

	@Test
	void c04_getRaw_returnsRawPropertyValue() {
		var bm = BeanMeta.of(A_Pojo.class);
		var p = new A_Pojo();
		p.setX("raw");

		var map = BeanMap.of(p, bm);
		var px = bm.getPropertyMeta("x");
		assertEquals("raw", px.getRaw(map, "x"));
	}

	//====================================================================================================
	// Custom BeanConfigContext (e.g. fluent setters, visibility tweaks)
	//====================================================================================================

	public static class D_FluentPojo {
		private String name;
		public String name() { return name; }
		public D_FluentPojo name(String value) { name = value; return this; }
	}

	@Test
	void d01_findFluentSetters_discoversFluentProperties() {
		var cfg = BeanConfigContext.create().findFluentSetters(true).build();
		var bm = BeanMeta.of(D_FluentPojo.class, cfg);
		assertTrue(bm.getProperties().containsKey("name"));
	}

	//====================================================================================================
	// Marshalling-only paths on a commons-built BeanPropertyMeta (Step 7 hardening)
	//====================================================================================================

	public static class E_CollectionPojo {
		private List<String> tags = new ArrayList<>();
		private Map<String,String> attrs = new LinkedHashMap<>();
		public List<String> getTags() { return tags; }
		public void setTags(List<String> value) { tags = value; }
		public Map<String,String> getAttrs() { return attrs; }
		public void setAttrs(Map<String,String> value) { attrs = value; }
	}

	@Test
	void e01_add_collection_throwsOnCommonsBuiltProperty() {
		var bm = BeanMeta.of(E_CollectionPojo.class);
		var p = new E_CollectionPojo();
		var map = BeanMap.of(p, bm);
		var pTags = bm.getPropertyMeta("tags");
		var ex = assertThrows(UnsupportedOperationException.class, () -> pTags.add(map, "tags", "x"));
		assertTrue(ex.getMessage().contains("bean-modeling-only"), () -> "Got: " + ex.getMessage());
		assertTrue(ex.getMessage().contains("tags"), () -> "Got: " + ex.getMessage());
	}

	@Test
	void e02_add_map_throwsOnCommonsBuiltProperty() {
		var bm = BeanMeta.of(E_CollectionPojo.class);
		var p = new E_CollectionPojo();
		var map = BeanMap.of(p, bm);
		var pAttrs = bm.getPropertyMeta("attrs");
		var ex = assertThrows(UnsupportedOperationException.class, () -> pAttrs.add(map, "attrs", "k", "v"));
		assertTrue(ex.getMessage().contains("bean-modeling-only"), () -> "Got: " + ex.getMessage());
		assertTrue(ex.getMessage().contains("attrs"), () -> "Got: " + ex.getMessage());
	}

	//====================================================================================================
	// BeanMap.getBean() on a commons-built BeanMap (Step 7 hardening)
	//====================================================================================================

	public static class F_OptionalPojo {
		private Optional<String> maybe;
		public Optional<String> getMaybe() { return maybe; }
		public void setMaybe(Optional<String> value) { maybe = value; }
	}

	@Test
	void f01_getBean_skipsOptionalInitOnCommonsBuiltProperty() {
		// Optional initialization (cm.isOptional() — seeds null Optional<X> properties with cm.getOptionalDefault())
		// requires a per-property ClassMeta which is unavailable on the bean-modeling-only path.  getBean() must
		// still return the wrapped bean rather than NPE.
		var bm = BeanMeta.of(F_OptionalPojo.class);
		var p = new F_OptionalPojo();
		var map = BeanMap.of(p, bm);
		assertSame(p, map.getBean());
		// Optional field stays at its constructor-assigned value (null) because the marshalling-side
		// Optional-default seeding is skipped for commons-built properties.
		assertNull(p.getMaybe());
	}

	@Test
	void f02_getBean_returnsBeanForSimplePojo() {
		var bm = BeanMeta.of(A_Pojo.class);
		var p = new A_Pojo();
		p.setX("hi");
		var map = BeanMap.of(p, bm);
		assertSame(p, map.getBean());
		assertEquals("hi", p.getX());
	}
}
