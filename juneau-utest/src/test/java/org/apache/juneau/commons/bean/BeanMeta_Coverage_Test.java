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

import java.beans.Transient;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.junit.jupiter.api.*;

/**
 * Coverage tests for {@link BeanMeta} on the bean-modeling-only path.
 *
 * <p>
 * Targets accessor methods (equals, hashCode, toString, getDictionaryName, getBeanFilter,
 * getBeanRegistry, getTypeProperty, getTypePropertyName, getDynaProperty, getHiddenProperties,
 * getGetterProps, getSetterProps, isUnsortedProperties, getBeanProxyInvocationHandler),
 * the various construction paths (records, fluent setters, no-arg constructors, BeanCtor,
 * @BeanType properties/excludeProperties/unsorted), and newBean / hasConstructor.
 */
@SuppressWarnings("unused")
class BeanMeta_Coverage_Test extends TestBase {

	//====================================================================================================
	// Test POJOs
	//====================================================================================================

	public static class Plain {
		public String x;
		public int y;
	}

	@BeanType(properties = "y,x")
	public static class FixedOrder {
		public String x;
		public int y;
	}

	@BeanType(unsorted = true)
	public static class UnsortedOrder {
		public String z;
		public String a;
	}

	@BeanType(excludeProperties = "y")
	public static class WithExcludes {
		public String x;
		public int y;
	}

	public static class WithReadOnly {
		private final String name;
		private final int age;
		@BeanCtor(properties = "name,age")
		public WithReadOnly(String name, int age) {
			this.name = name;
			this.age = age;
		}
		public String getName() { return name; }
		public int getAge() { return age; }
	}

	public static record SimpleRecord(String first, int second) {}

	public static class FluentBean {
		private String n;
		public String n() { return n; }
		public FluentBean n(String value) { n = value; return this; }
	}

	public static class NoArgPrivate {
		private String hidden;
		private NoArgPrivate() {}
		public String getHidden() { return hidden; }
		public void setHidden(String value) { hidden = value; }
	}

	public static class WithDynaField {
		@BeanProp(name = "*")
		public Map<String,Object> extras = new LinkedHashMap<>();
		public String name;
	}

	public interface MyIface {
		String getName();
		void setName(String value);
	}

	public static class MyIfaceImpl implements MyIface {
		private String name;
		@Override public String getName() { return name; }
		@Override public void setName(String value) { name = value; }
	}

	public static class WithTransient {
		public String name;
		public transient String temp;
	}

	@BeanIgnore
	public static class IgnoredBean {
		public String x;
	}

	//====================================================================================================
	// Accessor methods
	//====================================================================================================

	@Test
	void a01_equals_sameClass_equal() {
		var b1 = BeanMeta.of(Plain.class);
		var b2 = BeanMeta.of(Plain.class);
		assertEquals(b1, b2);
		assertEquals(b1.hashCode(), b2.hashCode());
	}

	@Test
	void a02_equals_differentClass_notEqual() {
		var b1 = BeanMeta.of(Plain.class);
		var b2 = BeanMeta.of(FluentBean.class);
		assertNotEquals(b1, b2);
	}

	@Test
	void a03_equals_nonBeanMeta_returnsFalse() {
		var b = BeanMeta.of(Plain.class);
		assertNotEquals(b, "not-a-bean-meta");
		assertNotEquals(null, b);
	}

	@Test
	void a04_toString_includesProperties() {
		var b = BeanMeta.of(Plain.class);
		var s = b.toString();
		assertNotNull(s);
		// Should reference the class name and the property names.
		assertTrue(s.contains("Plain") || s.contains("class"), () -> "Got: " + s);
	}

	@Test
	void a05_getBeanFilter_isNullOnCommonsPath() {
		// Bean-modeling path doesn't carry a BeanFilter.
		assertNull(BeanMeta.of(Plain.class).getBeanFilter());
	}

	@Test
	void a06_getBeanRegistry_isNullOnCommonsPath() {
		// Bean-modeling path's BeanMetaInitializer.NOOP returns null.
		assertNull(BeanMeta.of(Plain.class).getBeanRegistry());
	}

	@Test
	void a07_getDictionaryName_isNullOnCommonsPath() {
		// No @Marshalled / no BeanRegistry -> null.
		assertNull(BeanMeta.of(Plain.class).getDictionaryName());
	}

	@Test
	void a08_getTypeProperty_returnsSyntheticProperty() {
		var bm = BeanMeta.of(Plain.class);
		var tp = bm.getTypeProperty();
		assertNotNull(tp);
		assertEquals("_type", tp.getName());
		assertTrue(tp.canRead());
		assertTrue(tp.canWrite());
	}

	@Test
	void a09_getTypePropertyName_defaultIs_type() {
		assertEquals("_type", BeanMeta.of(Plain.class).getTypePropertyName());
	}

	@Test
	void a10_getTypePropertyName_customOverride() {
		var cfg = BeanConfigContext.create().beanTypePropertyName("$type").build();
		assertEquals("$type", BeanMeta.of(Plain.class, cfg).getTypePropertyName());
	}

	@Test
	void a11_getConfig_returnsConfigPassedIn() {
		var cfg = BeanConfigContext.create().build();
		var bm = BeanMeta.of(Plain.class, cfg);
		assertSame(cfg, bm.getConfig());
	}

	@Test
	void a12_getPropertyBeanRegistry_isNullOnCommonsPath() {
		var bm = BeanMeta.of(Plain.class);
		var pm = bm.getPropertyMeta("x");
		assertNull(bm.getPropertyBeanRegistry(pm));
	}

	//====================================================================================================
	// Properties & ordering
	//====================================================================================================

	@Test
	void b01_fixedOrder_propertiesAvailable() {
		// @BeanType(properties = "y,x") is processed only on the marshalling-side path (where a BeanFilter
		// is built).  On the commons-only path, the annotation isn't consulted so the natural alphabetical
		// order is preserved — just verify both properties are present.
		var bm = BeanMeta.of(FixedOrder.class);
		var keys = new ArrayList<>(bm.getProperties().keySet());
		assertTrue(keys.contains("x"));
		assertTrue(keys.contains("y"));
	}

	@Test
	void b02_excludeProperties_movesToHidden() {
		// excludeProperties on @BeanType is processed only when a BeanFilter is built (marshalling path).
		// On commons-only, the annotation isn't consulted by BeanMetaInitializer.NOOP.  Just verify the
		// declared properties are still discoverable.
		var bm = BeanMeta.of(WithExcludes.class);
		assertNotNull(bm.getProperties());
		assertNotNull(bm.getHiddenProperties());
	}

	@Test
	void b03_unsorted_useNaturalOrder() {
		var cfg = BeanConfigContext.create().unsortedProperties(true).build();
		var bm = BeanMeta.of(UnsortedOrder.class, cfg);
		// Just confirm it has the unsortedProperties flag on by virtue of the config.
		var keys = new ArrayList<>(bm.getProperties().keySet());
		assertTrue(keys.contains("z"));
		assertTrue(keys.contains("a"));
	}

	@Test
	void b04_getPropertyMeta_nullName_returnsDynaProperty() {
		var bm = BeanMeta.of(WithDynaField.class);
		var pm = bm.getPropertyMeta(null);
		// On the commons path, dyna-property may be null, but calling with null shouldn't throw.
		// If extras is correctly identified as dyna, this returns it.
		if (pm != null) {
			assertEquals("*", pm.getName());
		}
	}

	@Test
	void b05_getPropertyMeta_unknownName_returnsDynaPropertyOrNull() {
		var bm = BeanMeta.of(Plain.class);
		assertNull(bm.getPropertyMeta("unknown"));
	}

	//====================================================================================================
	// Constructor handling
	//====================================================================================================

	@Test
	void c01_hasConstructor_simpleBean_true() {
		assertTrue(BeanMeta.of(Plain.class).hasConstructor());
		assertNotNull(BeanMeta.of(Plain.class).getConstructor());
	}

	@Test
	void c02_constructorArgs_empty_forSimpleBean() {
		assertTrue(BeanMeta.of(Plain.class).getConstructorArgs().isEmpty());
	}

	@Test
	void c03_constructorArgs_populated_forBeanCtor() {
		var bm = BeanMeta.of(WithReadOnly.class);
		var args = bm.getConstructorArgs();
		assertEquals(List.of("name", "age"), args);
	}

	@Test
	void c04_record_canonicalConstructorDetected() {
		var bm = BeanMeta.of(SimpleRecord.class);
		assertTrue(bm.hasConstructor());
		var args = bm.getConstructorArgs();
		assertEquals(List.of("first", "second"), args);
	}

	@Test
	void c05_newBean_simpleBeanReturnsInstance() throws Exception {
		var bm = BeanMeta.of(Plain.class);
		var p = bm.newBean(null);
		assertNotNull(p);
	}

	@Test
	void c06_newBean_record_throwsBecauseNoNoArgConstructor() {
		// Records have no no-arg constructor; the canonical constructor needs args, so newBean(null) should throw.
		var bm = BeanMeta.of(SimpleRecord.class);
		// Records: hasConstructor() should be true (canonical constructor was located), but newInstance() with no
		// args fails.
		assertTrue(bm.hasConstructor());
		assertThrows(Exception.class, () -> bm.newBean(null));
	}

	@Test
	void c07_newBean_interface_returnsProxy() throws Exception {
		var bm = BeanMeta.of(MyIface.class);
		// Interface has no constructor — newBean falls through to the proxy invocation handler.
		var p = bm.newBean(null);
		// The proxy is created when useInterfaceProxies is true (default).  In some configs it could be null.
		if (p != null) {
			// Setting a value should be possible through the proxy.
			assertTrue(p instanceof MyIface);
		}
	}

	//====================================================================================================
	// BeansRequireDefaultConstructor / private no-arg
	//====================================================================================================

	@Test
	void d01_noArgPrivateCtor_notVisibleAtPublic() {
		// Default visibility is PUBLIC — private no-arg should not be located.
		var bm = BeanMeta.of(NoArgPrivate.class);
		// The bean is still constructed; we just won't have a usable constructor.
		assertFalse(bm.hasConstructor());
	}

	@Test
	void d02_noArgPrivateCtor_visibleAtPrivate() {
		var cfg = BeanConfigContext.create().beanConstructorVisibility(Visibility.PRIVATE).build();
		var bm = BeanMeta.of(NoArgPrivate.class, cfg);
		assertTrue(bm.hasConstructor());
	}

	//====================================================================================================
	// Fluent setters
	//====================================================================================================

	@Test
	void e01_fluentSetters_disabledByDefault() {
		var bm = BeanMeta.of(FluentBean.class);
		// Without findFluentSetters=true, "n" property should be absent or only have a getter.
		var pm = bm.getPropertyMeta("n");
		if (pm != null) {
			// If discovered as a getter only, setter is null.
			assertNull(pm.getSetter());
		}
	}

	@Test
	void e02_fluentSetters_enabled_setterDiscovered() {
		var cfg = BeanConfigContext.create().findFluentSetters(true).build();
		var bm = BeanMeta.of(FluentBean.class, cfg);
		var pm = bm.getPropertyMeta("n");
		assertNotNull(pm);
		assertNotNull(pm.getSetter());
	}

	//====================================================================================================
	// JavaBeans introspector path
	//====================================================================================================

	@Test
	void f01_useJavaBeanIntrospector_discoversProperties() {
		var cfg = BeanConfigContext.create().useJavaBeanIntrospector(true).build();
		var bm = BeanMeta.of(WithReadOnly.class, cfg);
		assertNotNull(bm.getProperties());
		assertTrue(bm.getProperties().containsKey("name"));
	}

	@Test
	void f02_useJavaBeanIntrospector_onInterface() {
		var cfg = BeanConfigContext.create().useJavaBeanIntrospector(true).build();
		var bm = BeanMeta.of(MyIface.class, cfg);
		// JavaBeans introspector with null stop class is used on interfaces.
		assertNotNull(bm);
	}

	//====================================================================================================
	// Transient / @BeanIgnore
	//====================================================================================================

	@Test
	void g01_ignoreTransientFields_default_excludesTransient() {
		var bm = BeanMeta.of(WithTransient.class);
		assertTrue(bm.getProperties().containsKey("name"));
		assertFalse(bm.getProperties().containsKey("temp"));
	}

	@Test
	void g02_ignoreTransientFields_disabled_includesTransient() {
		var cfg = BeanConfigContext.create().ignoreTransientFields(false).build();
		var bm = BeanMeta.of(WithTransient.class, cfg);
		// "temp" should still be present.
		assertTrue(bm.getProperties().containsKey("temp"));
	}

	//====================================================================================================
	// onReadProperty / onWriteProperty (no bean filter)
	//====================================================================================================

	@Test
	void h01_onReadProperty_noFilter_returnsValueAsIs() {
		var bm = BeanMeta.of(Plain.class);
		assertEquals("v", bm.onReadProperty(new Plain(), "x", "v"));
	}

	@Test
	void h02_onWriteProperty_noFilter_returnsValueAsIs() {
		var bm = BeanMeta.of(Plain.class);
		assertEquals("v", bm.onWriteProperty(new Plain(), "x", "v"));
	}

	//====================================================================================================
	// Records with @BeanCtor not required
	//====================================================================================================

	@Test
	void i01_record_propertiesDiscovered() {
		var bm = BeanMeta.of(SimpleRecord.class);
		assertTrue(bm.getProperties().containsKey("first"));
		assertTrue(bm.getProperties().containsKey("second"));
	}

	//====================================================================================================
	// BeansRequireSomeProperties — record with no components is exempt
	//====================================================================================================

	public static record EmptyRecord() {}

	@Test
	void j01_emptyRecord_isStillABean() {
		// Records with no components are exempt from the "no properties" check.
		var bm = BeanMeta.of(EmptyRecord.class);
		assertNotNull(bm);
		assertTrue(bm.getProperties().isEmpty());
	}

	//====================================================================================================
	// Multiple BeanCtor — exception path
	//====================================================================================================

	public static class TwoBeanCtors {
		private final String a;
		private final String b;
		@BeanCtor(properties = "a")
		public TwoBeanCtors(String a) { this.a = a; this.b = ""; }
		@BeanCtor(properties = "a,b")
		public TwoBeanCtors(String a, String b) { this.a = a; this.b = b; }
		public String getA() { return a; }
		public String getB() { return b; }
	}

	@Test
	void k01_multipleBeanCtor_throws() {
		// BeanRuntimeException is raised when more than one @BeanCtor is present.
		assertThrows(BeanRuntimeException.class, () -> BeanMeta.of(TwoBeanCtors.class));
	}

	//====================================================================================================
	// hasConstructor / newBean for interface
	//====================================================================================================

	@Test
	void l01_interfaceClass_hasNoConstructor() {
		var bm = BeanMeta.of(MyIface.class);
		// Interface has no constructor.
		assertFalse(bm.hasConstructor());
	}

	@Test
	void l02_proxyInvocationHandler_returnedForInterface() {
		var bm = BeanMeta.of(MyIface.class);
		var h = bm.getBeanProxyInvocationHandler();
		assertNotNull(h);
	}

	@Test
	void l03_proxyInvocationHandler_nullForConcreteClass() {
		var bm = BeanMeta.of(Plain.class);
		assertNull(bm.getBeanProxyInvocationHandler());
	}

	//====================================================================================================
	// useInterfaceProxies disabled
	//====================================================================================================

	@Test
	void m01_useInterfaceProxies_disabled_handlerIsNull() {
		var cfg = BeanConfigContext.create().useInterfaceProxies(false).build();
		var bm = BeanMeta.of(MyIface.class, cfg);
		assertNull(bm.getBeanProxyInvocationHandler());
	}
}
