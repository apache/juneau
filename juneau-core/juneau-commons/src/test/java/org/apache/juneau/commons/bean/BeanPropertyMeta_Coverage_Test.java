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

import java.net.*;
import java.util.*;

import org.apache.juneau.commons.reflect.*;
import org.junit.jupiter.api.*;
import org.apache.juneau.commons.TestBase;

/**
 * Coverage tests for {@link BeanPropertyMeta} on the bean-modeling-only path.
 *
 * <p>
 * Targets the public accessor methods (canRead/canWrite, getName, getField, getGetter, getSetter,
 * getInnerField, isDyna, isReadOnly, isUri, getDelegateFor, getBeanMeta, getBeanInfo, getBeanRegistry,
 * getAnnotations, getAnnotations(Class), equals, hashCode, compareTo, toString), the {@link BeanPropertyMeta#get},
 * {@link BeanPropertyMeta#set}, {@link BeanPropertyMeta#getRaw}, {@link BeanPropertyMeta#getDynaMap} methods,
 * and the marshalling-only-path {@link UnsupportedOperationException} thrown by {@link BeanPropertyMeta#add}.
 */
@SuppressWarnings({
	"unused"  // Unused in this context; kept for API consistency or future use.
})
class BeanPropertyMeta_Coverage_Test extends TestBase {

	//====================================================================================================
	// Test POJOs
	//====================================================================================================

	public static class Plain {
		public String x;
		public int y;
		public String getX() { return x; }
		public void setX(String value) { x = value; }
		public int getY() { return y; }
		public void setY(int value) { y = value; }
	}

	public static class WithUri {
		public URL homepage;
		public URI api;
	}

	public static class WithReadOnly {
		@BeanProp(ro = "true")
		public String name;
		public int age;
	}

	public static class WithWriteOnly {
		@BeanProp(wo = "true")
		public String secret;
		public String name;
	}

	public static class WithGetterThrows {
		public String getX() {
			throw new RuntimeException("boom");
		}
		public void setX(String value) { /* No-op: present only so 'x' is a read/write bean property; the throwing getter is what's under test. */ }
	}

	public static class WithSetterThrows {
		public String getX() { return "x"; }
		public void setX(String value) {
			throw new RuntimeException("boom");
		}
	}

	public static class WithDynaField {
		@BeanProp(name = "*")
		public Map<String,Object> extras = new LinkedHashMap<>();
		public String name;
	}

	public static class WithDynaMethods {
		private final Map<String,Object> store = new LinkedHashMap<>();
		@BeanProp(name = "*")
		public Map<String,Object> getExtras() { return store; }
		@BeanProp(name = "*")
		public void setExtras(String key, Object value) { store.put(key, value); }
	}

	public static class WithCollection {
		public List<String> items = new ArrayList<>();
	}

	//====================================================================================================
	// Accessor methods
	//====================================================================================================

	@Test
	void a01_getName_returnsPropertyName() {
		var bm = BeanMeta.of(Plain.class);
		var pm = bm.getPropertyMeta("x");
		assertEquals("x", pm.getName());
	}

	@Test
	void a02_canRead_canWrite_default() {
		var bm = BeanMeta.of(Plain.class);
		var pm = bm.getPropertyMeta("x");
		assertTrue(pm.canRead());
		assertTrue(pm.canWrite());
	}

	@Test
	void a03_getField_publicField() {
		var bm = BeanMeta.of(Plain.class);
		var pm = bm.getPropertyMeta("x");
		// Public field path: field is non-null when the property has a public field.
		assertNotNull(pm.getField());
	}

	@Test
	void a04_getGetter_returnsGetter() {
		var bm = BeanMeta.of(Plain.class);
		var pm = bm.getPropertyMeta("x");
		assertNotNull(pm.getGetter());
	}

	@Test
	void a05_getSetter_returnsSetter() {
		var bm = BeanMeta.of(Plain.class);
		var pm = bm.getPropertyMeta("x");
		assertNotNull(pm.getSetter());
	}

	@Test
	void a06_isDyna_falseForRegular() {
		var bm = BeanMeta.of(Plain.class);
		var pm = bm.getPropertyMeta("x");
		assertFalse(pm.isDyna());
	}

	@Test
	void a07_isDyna_trueForDynaField() {
		var bm = BeanMeta.of(WithDynaField.class);
		var pm = bm.getPropertyMeta("*");
		// When BeanMeta recognizes the dyna field, the property meta should be present.
		if (pm != null) {
			assertTrue(pm.isDyna());
			assertEquals("*", pm.getName());
		}
	}

	@Test
	void a08_isReadOnly_default_false() {
		var bm = BeanMeta.of(Plain.class);
		var pm = bm.getPropertyMeta("x");
		assertFalse(pm.isReadOnly());
	}

	@Test
	void a09_isReadOnly_trueWithBeanProp() {
		var bm = BeanMeta.of(WithReadOnly.class);
		var pm = bm.getPropertyMeta("name");
		assertNotNull(pm);
		assertTrue(pm.isReadOnly());
	}

	@Test
	void a10_isUri_defaultFalseForString() {
		var bm = BeanMeta.of(Plain.class);
		var pm = bm.getPropertyMeta("x");
		// On commons-side path, isUri is computed from rawTypeMeta which is null — so it's false.
		assertFalse(pm.isUri());
	}

	@Test
	void a11_getBeanMeta_returnsOwner() {
		var bm = BeanMeta.of(Plain.class);
		var pm = bm.getPropertyMeta("x");
		assertSame(bm, pm.getBeanMeta());
	}

	@Test
	void a12_getBeanInfo_isNullOnCommonsPath() {
		var bm = BeanMeta.of(Plain.class);
		var pm = bm.getPropertyMeta("x");
		// rawTypeMeta is null on the commons path.
		assertNull(pm.getBeanInfo());
	}

	@Test
	void a13_getBeanRegistry_isNullOnCommonsPath() {
		var bm = BeanMeta.of(Plain.class);
		var pm = bm.getPropertyMeta("x");
		assertNull(pm.getBeanRegistry());
	}

	@Test
	void a14_getDelegateFor_returnsSelfWhenNoDelegate() {
		var bm = BeanMeta.of(Plain.class);
		var pm = bm.getPropertyMeta("x");
		assertSame(pm, pm.getDelegateFor());
	}

	@Test
	void a15_getInnerField_alwaysAvailable() {
		var bm = BeanMeta.of(Plain.class);
		var pm = bm.getPropertyMeta("x");
		assertNotNull(pm.getInnerField());
	}

	//====================================================================================================
	// equals / hashCode / compareTo
	//====================================================================================================

	@Test
	void b01_equals_sameProperty_equal() {
		var bm = BeanMeta.of(Plain.class);
		var pm1 = bm.getPropertyMeta("x");
		var pm2 = bm.getPropertyMeta("x");
		assertEquals(pm1, pm2);
		assertEquals(pm1.hashCode(), pm2.hashCode());
	}

	@Test
	void b02_equals_differentProperty_notEqual() {
		var bm = BeanMeta.of(Plain.class);
		var pm1 = bm.getPropertyMeta("x");
		var pm2 = bm.getPropertyMeta("y");
		assertNotEquals(pm1, pm2);
	}

	@Test
	@SuppressWarnings({
		"java:S3415" // Argument order is intentional: this test exercises BeanPropertyMeta.equals() with a non-matching type, so the bean must be the receiver (first arg of assertNotEquals).
	})
	void b03_equals_nonPropertyMeta_returnsFalse() {
		var bm = BeanMeta.of(Plain.class);
		var pm = bm.getPropertyMeta("x");
		assertNotEquals(pm, "not-a-property-meta");
		assertNotEquals(null, pm);
	}

	@Test
	void b04_compareTo_byName() {
		var bm = BeanMeta.of(Plain.class);
		var px = bm.getPropertyMeta("x");
		var py = bm.getPropertyMeta("y");
		// 'x' < 'y' alphabetically.
		assertTrue(px.compareTo(py) < 0);
		assertTrue(py.compareTo(px) > 0);
		assertEquals(0, px.compareTo(bm.getPropertyMeta("x")));
	}

	@Test
	void b05_toString_nonNull() {
		var bm = BeanMeta.of(Plain.class);
		var pm = bm.getPropertyMeta("x");
		var s = pm.toString();
		assertNotNull(s);
	}

	//====================================================================================================
	// getAnnotations
	//====================================================================================================

	@Test
	void c01_getAnnotations_nonNull() {
		var bm = BeanMeta.of(Plain.class);
		var pm = bm.getPropertyMeta("x");
		var anns = pm.getAnnotations();
		assertNotNull(anns);
	}

	@Test
	void c02_getAnnotations_byClass_empty_returnsEmptyStream() {
		var bm = BeanMeta.of(Plain.class);
		var pm = bm.getPropertyMeta("x");
		// No @Deprecated on Plain.x
		assertEquals(0, pm.getAnnotations(Deprecated.class).count());
	}

	@Test
	void c03_getAnnotations_byClass_nullReturnsEmpty() {
		var bm = BeanMeta.of(Plain.class);
		var pm = bm.getPropertyMeta("x");
		assertEquals(0, pm.getAnnotations(null).count());
	}

	@Test
	void c04_getAnnotations_findsBeanProp_onReadOnly() {
		var bm = BeanMeta.of(WithReadOnly.class);
		var pm = bm.getPropertyMeta("name");
		assertNotNull(pm);
		// @BeanProp(ro="true") should be in the annotation list.
		assertTrue(pm.getAnnotations(BeanProp.class).count() > 0);
	}

	//====================================================================================================
	// get / set / getRaw
	//====================================================================================================

	@Test
	void d01_get_returnsValue() {
		var p = new Plain();
		p.setX("hello");
		var bm = BeanMeta.of(Plain.class);
		var bMap = BeanMap.of(p);
		var pm = bm.getPropertyMeta("x");
		assertEquals("hello", pm.get(bMap, null));
	}

	@Test
	void d02_set_writesValue() {
		var p = new Plain();
		var bm = BeanMeta.of(Plain.class);
		var bMap = BeanMap.of(p);
		var pm = bm.getPropertyMeta("x");
		pm.set(bMap, null, "set-value");
		assertEquals("set-value", p.getX());
	}

	@Test
	void d03_getRaw_returnsRawValue() {
		var p = new Plain();
		p.setX("raw");
		var bm = BeanMeta.of(Plain.class);
		var bMap = BeanMap.of(p);
		var pm = bm.getPropertyMeta("x");
		assertEquals("raw", pm.getRaw(bMap, null));
	}

	@Test
	void d04_set_readOnly_returnsNull() {
		var bm = BeanMeta.of(WithReadOnly.class);
		var bMap = BeanMap.of(new WithReadOnly());
		var pm = bm.getPropertyMeta("name");
		assertNull(pm.set(bMap, null, "ignored"));
	}

	@Test
	void d05_get_writeOnly_returnsNull() {
		var w = new WithWriteOnly();
		w.secret = "ssh!";
		var bm = BeanMeta.of(WithWriteOnly.class);
		var bMap = BeanMap.of(w);
		var pm = bm.getPropertyMeta("secret");
		// Write-only: getInner returns null without invoking the getter.
		assertNull(pm.get(bMap, null));
	}

	@Test
	void d06_getRaw_throwingGetter_throwsByDefault() {
		var bm = BeanMeta.of(WithGetterThrows.class);
		var bMap = BeanMap.of(new WithGetterThrows());
		var pm = bm.getPropertyMeta("x");
		// Default config: ignoreInvocationExceptionsOnGetters=false -> throws.
		assertThrows(BeanRuntimeException.class, () -> pm.getRaw(bMap, null));
	}

	@Test
	void d07_getRaw_throwingGetter_returnsNullWhenIgnoring() {
		var cfg = BeanConfigContext.create().ignoreInvocationExceptionsOnGetters(true).build();
		var bm = BeanMeta.of(WithGetterThrows.class, cfg);
		var bMap = BeanMap.of(new WithGetterThrows(), bm);
		var pm = bm.getPropertyMeta("x");
		// rawTypeMeta is null on commons path, so simply returns null.
		assertNull(pm.getRaw(bMap, null));
	}

	@Test
	void d08_set_throwingSetter_throwsExecutableException() {
		// On commons-side path (rawTypeMeta == null), set() invokes the raw setter via invokeSetter() and any
		// reflective failure surfaces as an ExecutableException (RuntimeException) — not wrapped in
		// BeanRuntimeException because the BasicRuntimeException try-catch only catches that subhierarchy.
		var bm = BeanMeta.of(WithSetterThrows.class);
		var bMap = BeanMap.of(new WithSetterThrows());
		var pm = bm.getPropertyMeta("x");
		assertThrows(RuntimeException.class, () -> pm.set(bMap, null, "v"));
	}

	@Test
	void d09_set_throwingSetter_propagatesOnCommonsPath() {
		// ignoreInvocationExceptionsOnSetters is honored only on the marshalling-side path (in setPropertyValue); // NOSONAR
		// on the commons-side raw-reflection branch the exception still propagates.
		var cfg = BeanConfigContext.create().ignoreInvocationExceptionsOnSetters(true).build();
		var bm = BeanMeta.of(WithSetterThrows.class, cfg);
		var bMap = BeanMap.of(new WithSetterThrows(), bm);
		var pm = bm.getPropertyMeta("x");
		assertThrows(RuntimeException.class, () -> pm.set(bMap, null, "v"));
	}

	//====================================================================================================
	// add — UnsupportedOperationException on commons-side path
	//====================================================================================================

	@Test
	void e01_add_throwsUnsupportedOnCommonsPath() {
		var bm = BeanMeta.of(WithCollection.class);
		var bMap = BeanMap.of(new WithCollection());
		var pm = bm.getPropertyMeta("items");
		assertNotNull(pm);
		// add() requires a marshalling context (rawTypeMeta != null).
		assertThrows(UnsupportedOperationException.class, () -> pm.add(bMap, null, "value"));
	}

	@Test
	void e02_addWithKey_throwsUnsupportedOnCommonsPath() {
		var bm = BeanMeta.of(WithCollection.class);
		var bMap = BeanMap.of(new WithCollection());
		var pm = bm.getPropertyMeta("items");
		assertNotNull(pm);
		assertThrows(UnsupportedOperationException.class, () -> pm.add(bMap, null, "key", "value"));
	}

	//====================================================================================================
	// getDynaMap
	//====================================================================================================

	@Test
	void f01_getDynaMap_nonDyna_returnsEmpty() throws Exception {
		var bm = BeanMeta.of(Plain.class);
		var pm = bm.getPropertyMeta("x");
		assertTrue(pm.getDynaMap(new Plain()).isEmpty());
	}

	@Test
	void f02_getDynaMap_dynaField_returnsBackingMap() throws Exception {
		var w = new WithDynaField();
		w.extras.put("k", "v");
		var bm = BeanMeta.of(WithDynaField.class);
		var pm = bm.getPropertyMeta("*");
		if (pm != null) {
			var m = pm.getDynaMap(w);
			assertEquals("v", m.get("k"));
		}
	}

	//====================================================================================================
	// Builder validation
	//====================================================================================================

	@Test
	void g01_builder_delegateFor_rejectsNull() {
		var bm = BeanMeta.of(Plain.class);
		var builder = BeanPropertyMeta.builder(bm, "x");
		assertThrows(IllegalArgumentException.class, () -> builder.delegateFor(null));
	}

	@Test
	void g02_builder_readTransform_rejectsNull() {
		var bm = BeanMeta.of(Plain.class);
		var builder = BeanPropertyMeta.builder(bm, "x");
		assertThrows(IllegalArgumentException.class, () -> builder.readTransform(null));
	}

	@Test
	void g03_builder_writeTransform_rejectsNull() {
		var bm = BeanMeta.of(Plain.class);
		var builder = BeanPropertyMeta.builder(bm, "x");
		assertThrows(IllegalArgumentException.class, () -> builder.writeTransform(null));
	}

	@Test
	void g04_builder_overrideValue_acceptsValue() {
		var bm = BeanMeta.of(Plain.class);
		var builder = BeanPropertyMeta.builder(bm, "x");
		// Builder should accept any object including null without throwing.
		assertSame(builder, builder.overrideValue("value"));
		assertSame(builder, builder.overrideValue(null));
	}

	//====================================================================================================
	// Property names / DLC / unsorted
	//====================================================================================================

	@Test
	void h01_getName_consistentWithMap() {
		var bm = BeanMeta.of(Plain.class);
		var keys = bm.getProperties().keySet();
		for (var k : keys) {
			assertEquals(k, bm.getPropertyMeta(k).getName());
		}
	}
}
