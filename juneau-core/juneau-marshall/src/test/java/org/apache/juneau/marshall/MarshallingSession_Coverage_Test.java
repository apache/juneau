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
package org.apache.juneau.marshall;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.http.MediaType;
import org.apache.juneau.commons.reflect.*;
import org.junit.jupiter.api.*;

/**
 * Additional coverage-focused tests for {@link MarshallingSession}, targeting branches not already exercised
 * by {@link MarshallingSession_Test} (which covers {@code toBeanMap(Object, PropertyNamer)} only):
 *  - the {@code Builder#property(String, Object)} switch dispatch (all key aliases plus the null-key and
 *    default arms).
 *  - {@code get(Class)}'s {@code Locale} and {@code MediaType} arms.
 *  - {@code addWarning}'s debug-enabled varargs-vs-no-args branch.
 *  - {@code getBeanMeta(Class)}'s null-class short-circuit.
 *  - {@code getBeanTypePropertyName(ClassMeta)}'s null-cm and non-bean-cm arms.
 *  - {@code getTimeZoneId()}'s both arms.
 *  - {@code newBean(Class)}'s single-arg delegate, the not-a-bean and no-no-arg-ctor failure arms.
 *  - {@code toBeanMap(Object, PropertyNamer)}'s already-a-BeanMap short-circuit and not-a-bean-for-namer arm.
 *  - {@code toArray}'s nested collection-of-collections (array-of-arrays) arm.
 *  - the {@code BeanSession}-bridge {@code convertToType(Object, Object)} and
 *    {@code convertToMemberType(Object, Object, Object)} dispatch (null / ClassMeta / Class / unsupported).
 */
class MarshallingSession_Coverage_Test extends TestBase {

	MarshallingContext ctx = MarshallingContext.DEFAULT;

	//------------------------------------------------------------------------------------------------------------------
	// a. Builder#property(String, Object) dispatch
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_property_nullKeyDelegatesToBase() {
		// Null-key handling is delegated to the base class, which validates and throws (rather than
		// silently no-op'ing), so this documents the null-key arm's actual (throwing) behavior.
		var b = MarshallingSession.create(ctx);
		assertThrows(IllegalArgumentException.class, () -> b.property(null, "x"));
	}

	@Test void a02_property_locale() {
		var s = MarshallingSession.create(ctx).property("locale", "de_DE").build();
		assertEquals(Locale.GERMANY, s.getLocale());
	}

	@Test void a03_property_beanSessionLocale() {
		var s = MarshallingSession.create(ctx).property("MarshallingSession.locale", "fr_FR").build();
		assertEquals(Locale.FRANCE, s.getLocale());
	}

	@Test void a04_property_mediaType() {
		var s = MarshallingSession.create(ctx).property("mediaType", "text/plain").build();
		assertEquals(MediaType.of("text/plain"), s.getMediaType());
	}

	@Test void a05_property_beanSessionMediaType() {
		var s = MarshallingSession.create(ctx).property("MarshallingSession.mediaType", "text/plain").build();
		assertEquals(MediaType.of("text/plain"), s.getMediaType());
	}

	@Test void a06_property_timeZone() {
		var s = MarshallingSession.create(ctx).property("timeZone", "America/New_York").build();
		assertEquals(TimeZone.getTimeZone("America/New_York"), s.getTimeZone());
	}

	@Test void a07_property_beanSessionTimeZone() {
		var s = MarshallingSession.create(ctx).property("MarshallingSession.timeZone", "America/New_York").build();
		assertEquals(TimeZone.getTimeZone("America/New_York"), s.getTimeZone());
	}

	@Test void a08_property_activeView() {
		var s = MarshallingSession.create(ctx).property("activeView", "summary").build();
		assertEquals("summary", s.getActiveView());
	}

	@Test void a09_property_beanSessionActiveView() {
		var s = MarshallingSession.create(ctx).property("MarshallingSession.activeView", "summary").build();
		assertEquals("summary", s.getActiveView());
	}

	@Test void a10_property_defaultKeyDelegatesToBase() {
		// Unrecognized key falls through to the default arm (super.property()), which stores it as an
		// opaque session property rather than throwing.
		var s = MarshallingSession.create(ctx).property("someUnknownKey", "v").build();
		assertNotNull(s);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b. get(Class): Locale and MediaType arms (TimeZone already covered elsewhere; Optional-empty default arm too).
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_get_locale() {
		var s = MarshallingSession.create(ctx).locale(Locale.CANADA).build();
		assertEquals(Optional.of(Locale.CANADA), s.get(Locale.class));
	}

	@Test void b02_get_mediaType() {
		var s = MarshallingSession.create(ctx).mediaType(MediaType.of("application/json")).build();
		assertEquals(Optional.of(MediaType.of("application/json")), s.get(MediaType.class));
	}

	@Test void b03_get_unsupportedTypeReturnsEmpty() {
		var s = MarshallingSession.create(ctx).build();
		assertTrue(s.get(String.class).isEmpty());
	}

	//------------------------------------------------------------------------------------------------------------------
	// c. addWarning: debug-enabled, with and without varargs.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_addWarning_debugNoArgs() {
		var s = MarshallingSession.create(ctx).debug(true).build();
		s.addWarning("plain message with no args");
		assertEquals(1, s.getWarnings().size());
	}

	@Test void c02_addWarning_debugWithArgs() {
		var s = MarshallingSession.create(ctx).debug(true).build();
		s.addWarning("formatted %s message", "warning");
		assertEquals(1, s.getWarnings().size());
		assertTrue(s.getWarnings().get(0).contains("formatted warning message"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// d. getBeanMeta(Class): null-class short-circuit.
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_getBeanMeta_nullClass() {
		var s = MarshallingSession.create(ctx).build();
		assertNull(s.getBeanMeta(null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// e. getBeanTypePropertyName(ClassMeta): null-cm and non-bean-cm arms.
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_getBeanTypePropertyName_nullCm() {
		var s = MarshallingSession.create(ctx).build();
		assertEquals(s.getBeanTypePropertyName(), s.getBeanTypePropertyName((ClassMeta<?>)null));
	}

	@Test void e02_getBeanTypePropertyName_nonBeanCmFallsBack() {
		var s = MarshallingSession.create(ctx).build();
		var cm = s.getClassMeta(String.class);
		assertEquals(s.getBeanTypePropertyName(), s.getBeanTypePropertyName(cm));
	}

	@Test void e03_getBeanTypePropertyName_beanCmUsesDefault_type() {
		var s = MarshallingSession.create(ctx).build();
		var cm = s.getClassMeta(E03_Bean.class);
		// BeanMeta.getTypePropertyName() is never null (defaults to "_type"), so this always resolves
		// to the bean's own (non-null) type-property name rather than falling back to the session default.
		assertEquals("_type", s.getBeanTypePropertyName(cm));
	}

	public static class E03_Bean {
		public String name;
	}

	//------------------------------------------------------------------------------------------------------------------
	// f. getTimeZoneId(): both arms.
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_getTimeZoneId_noTimeZoneUsesSystemDefault() {
		var s = MarshallingSession.create(ctx).timeZone(null).build();
		if (ctx.getTimeZone() == null)
			assertEquals(ZoneId.systemDefault(), s.getTimeZoneId());
	}

	@Test void f02_getTimeZoneId_explicitTimeZone() {
		var s = MarshallingSession.create(ctx).timeZone(TimeZone.getTimeZone("UTC")).build();
		assertEquals(ZoneId.of("UTC"), s.getTimeZoneId());
	}

	//------------------------------------------------------------------------------------------------------------------
	// g. newBean(Class): single-arg delegate, not-a-bean, and no-no-arg-ctor arms.
	//------------------------------------------------------------------------------------------------------------------

	public static class G01_Bean {
		public String name = "x";
	}

	@Test void g01_newBean_singleArgDelegate() {
		var s = MarshallingSession.create(ctx).build();
		var b = s.newBean(G01_Bean.class);
		assertNotNull(b);
	}

	private static final class G02_NotABean {
		private int x = 1;
		private G02_NotABean() {}
		@SuppressWarnings({
			"unused" // Getter shape is needed for newBean()'s bean-detection check even though nothing calls it directly.
		})
		int getX() { return x; }
	}

	@Test void g02_newBean_notABeanReturnsNull() {
		var s = MarshallingSession.create(ctx).build();
		assertNull(s.newBean(G02_NotABean.class));
	}

	public static class G03_NoNoArgCtor {
		public String name;
		public G03_NoNoArgCtor(String name) { this.name = name; }
	}

	@Test void g03_newBean_noNoArgCtorThrows() {
		var s = MarshallingSession.create(ctx).build();
		assertThrows(BeanRuntimeException.class, () -> s.newBean(G03_NoNoArgCtor.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// h. toBeanMap(Object, PropertyNamer): already-a-BeanMap and not-a-bean-for-namer arms.
	//------------------------------------------------------------------------------------------------------------------

	public static class H01_Bean {
		public String name = "x";
	}

	@Test void h01_toBeanMap_withNamer_alreadyABeanMap() {
		var s = MarshallingSession.create(ctx).build();
		var bm = s.toBeanMap(new H01_Bean());
		var bm2 = s.toBeanMap(bm, PropertyNamerDLC.INSTANCE);
		assertSame(bm, bm2);
	}

	@Test void h02_toBeanMap_withNamer_notABeanThrows() {
		var s = MarshallingSession.create(ctx).build();
		assertThrows(BeanRuntimeException.class, () -> s.toBeanMap("not-a-bean", PropertyNamerDLC.INSTANCE));
	}

	//------------------------------------------------------------------------------------------------------------------
	// i. BeanSession-bridge convertToType(Object, Object) and convertToMemberType(Object, Object, Object) dispatch.
	//------------------------------------------------------------------------------------------------------------------

	@Test void i01_convertToType_bridge_nullTargetType() {
		var s = MarshallingSession.create(ctx).build();
		BeanSession bs = s;
		assertEquals("5", bs.convertToType("5", (Object)null));
	}

	@Test void i02_convertToType_bridge_classMeta() {
		var s = MarshallingSession.create(ctx).build();
		BeanSession bs = s;
		var cm = s.getClassMeta(Integer.class);
		assertEquals(5, bs.convertToType("5", cm));
	}

	@Test void i03_convertToType_bridge_class() {
		var s = MarshallingSession.create(ctx).build();
		BeanSession bs = s;
		assertEquals(5, bs.convertToType("5", Integer.class));
	}

	@Test void i04_convertToType_bridge_unsupportedThrows() {
		var s = MarshallingSession.create(ctx).build();
		BeanSession bs = s;
		assertThrows(IllegalArgumentException.class, () -> bs.convertToType("5", "not-a-type-descriptor"));
	}

	@Test void i05_convertToMemberType_bridge_nullTargetType() {
		var s = MarshallingSession.create(ctx).build();
		BeanSession bs = s;
		assertEquals("5", bs.convertToMemberType(null, "5", (Object)null));
	}

	@Test void i06_convertToMemberType_bridge_classMeta() {
		var s = MarshallingSession.create(ctx).build();
		BeanSession bs = s;
		var cm = s.getClassMeta(Integer.class);
		assertEquals(5, bs.convertToMemberType(null, "5", cm));
	}

	@Test void i07_convertToMemberType_bridge_class() {
		var s = MarshallingSession.create(ctx).build();
		BeanSession bs = s;
		assertEquals(5, bs.convertToMemberType(null, "5", Integer.class));
	}

	@Test void i08_convertToMemberType_bridge_unsupportedThrows() {
		var s = MarshallingSession.create(ctx).build();
		BeanSession bs = s;
		assertThrows(IllegalArgumentException.class, () -> bs.convertToMemberType(null, "5", "not-a-type-descriptor"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// j. convertToMemberType(Object, Object, Class): single delegate overload.
	//------------------------------------------------------------------------------------------------------------------

	@Test void j01_convertToMemberType_classOverloadDelegate() {
		var s = MarshallingSession.create(ctx).build();
		assertEquals(5, (Integer)s.convertToMemberType(null, "5", Integer.class));
	}
}
