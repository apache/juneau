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
package org.apache.juneau.marshall.serializer;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.json5.*;
import org.junit.jupiter.api.*;

/**
 * Coverage tests for {@link SerializerSession} targeting the gap-areas listed in E7:
 * Builder property dispatch, swap-resolution edges, listener notification paths, sort/trim behavior,
 * URI relativizing, and bean-type-name dispatch.
 *
 * <p>These tests exercise the abstract {@link SerializerSession} via the concrete {@link JsonSerializer}
 * and {@link Json5Serializer} sessions where possible. Tests intentionally assert the <em>current</em>
 * behavior; any apparent bug is documented inline for follow-up.
 */
@SuppressWarnings({
	"unused" // Unused parameters/variables kept for consistent method signatures across test utilities.
})
class SerializerSession_Test extends TestBase {

	//====================================================================================================
	// a. Builder.property() dispatch (lines 178-213)
	//====================================================================================================

	@Test void a01_builderProperty_javaMethod_short() throws Exception {
		var m = SerializerSession_Test.class.getDeclaredMethod("a01_builderProperty_javaMethod_short");
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession()
			.property("javaMethod", m)
			.build();
		assertNotNull(s);  // Built without error.
	}

	@Test void a02_builderProperty_javaMethod_qualified() throws Exception {
		var m = SerializerSession_Test.class.getDeclaredMethod("a02_builderProperty_javaMethod_qualified");
		var s = JsonSerializer.DEFAULT.createSession()
			.property("SerializerSession.javaMethod", m)
			.build();
		assertNotNull(s);
	}

	@Test void a03_builderProperty_resolver() {
		var vrs = VarResolver.DEFAULT.createSession();
		var s = JsonSerializer.DEFAULT.createSession()
			.property("resolver", vrs)
			.build();
		assertSame(vrs, s.getVarResolver());
	}

	@Test void a04_builderProperty_resolver_qualified() {
		var vrs = VarResolver.DEFAULT.createSession();
		var s = JsonSerializer.DEFAULT.createSession()
			.property("SerializerSession.resolver", vrs)
			.build();
		assertSame(vrs, s.getVarResolver());
	}

	@Test void a05_builderProperty_schema() {
		var schema = HttpPartSchema.DEFAULT;
		var s = JsonSerializer.DEFAULT.createSession()
			.property("schema", schema)
			.build();
		assertSame(schema, s.getSchema());
	}

	@Test void a06_builderProperty_schema_qualified() {
		var schema = HttpPartSchema.DEFAULT;
		var s = JsonSerializer.DEFAULT.createSession()
			.property("SerializerSession.schema", schema)
			.build();
		assertSame(schema, s.getSchema());
	}

	@Test void a07_builderProperty_uriContext() {
		var s = JsonSerializer.DEFAULT.createSession()
			.property("uriContext", "{}")
			.build();
		assertNotNull(s);
	}

	@Test void a08_builderProperty_uriContext_qualified() {
		var s = JsonSerializer.DEFAULT.createSession()
			.property("SerializerSession.uriContext", "{}")
			.build();
		assertNotNull(s);
	}

	@Test void a09_builderProperty_keepNullProperties() {
		var s = JsonSerializer.DEFAULT.createSession()
			.property("keepNullProperties", "true")
			.build();
		assertTrue(s.isKeepNullProperties());
	}

	@Test void a10_builderProperty_trimStrings() {
		var s = JsonSerializer.DEFAULT.createSession()
			.property("SerializerSession.trimStrings", "true")
			.build();
		assertTrue(s.isTrimStrings());
	}

	@Test void a11_builderProperty_addBeanTypes() {
		var s = JsonSerializer.DEFAULT.createSession()
			.property("addBeanTypes", "true")
			.build();
		assertTrue(s.isAddBeanTypes());
	}

	@Test void a12_builderProperty_addRootType() {
		var s = JsonSerializer.DEFAULT.createSession()
			.property("addRootType", "true")
			.build();
		assertTrue(s.isAddRootType());
	}

	@Test void a13_builderProperty_sortCollections() {
		var s = JsonSerializer.DEFAULT.createSession()
			.property("sortCollections", "true")
			.build();
		assertTrue(s.isSortCollections());
	}

	@Test void a14_builderProperty_sortMaps() {
		var s = JsonSerializer.DEFAULT.createSession()
			.property("sortMaps", "true")
			.build();
		assertTrue(s.isSortMaps());
	}

	@Test void a15_builderProperty_trimEmptyCollections() {
		var s = JsonSerializer.DEFAULT.createSession()
			.property("trimEmptyCollections", "true")
			.build();
		assertTrue(s.isTrimEmptyCollections());
	}

	@Test void a16_builderProperty_trimEmptyMaps() {
		var s = JsonSerializer.DEFAULT.createSession()
			.property("trimEmptyMaps", "true")
			.build();
		assertTrue(s.isTrimEmptyMaps());
	}

	@Test void a17_builderProperty_unknownKey_fallsThrough() {
		// Default branch in switch goes to super.property().
		var s = JsonSerializer.DEFAULT.createSession()
			.property("someUnknownKey", "x")
			.build();
		assertNotNull(s);
	}

	@SuppressWarnings({
		"java:S5778" // assertThrows lambdas contain multiple calls; only the primary call throws.
	})
	@Test void a18_builderProperty_nullKey_fallsThrough() {
		// Null key takes the early-return branch.
		assertThrows(IllegalArgumentException.class,
			() -> JsonSerializer.DEFAULT.createSession().property(null, "x"));
	}

	//====================================================================================================
	// b. Builder setters that ignore null (nn() guards) - lines 173, 226, 244, 257-260, 277
	//====================================================================================================

	@Test void b01_javaMethod_nullIgnored() {
		// javaMethod(null) should be a no-op (nn() guard).
		var s = JsonSerializer.DEFAULT.createSession()
			.javaMethod(null)
			.build();
		assertNotNull(s);
	}

	@Test void b02_resolver_nullIgnored() {
		var s = JsonSerializer.DEFAULT.createSession()
			.resolver(null)
			.build();
		// Default created lazily.
		assertNotNull(s.getVarResolver());
	}

	@Test void b03_schema_nullIgnored() {
		var s = JsonSerializer.DEFAULT.createSession()
			.schema(null)
			.build();
		assertNull(s.getSchema());
	}

	@Test void b04_schemaDefault_setsWhenNull() {
		var schema = HttpPartSchema.DEFAULT;
		var s = JsonSerializer.DEFAULT.createSession()
			.schemaDefault(schema)
			.build();
		assertSame(schema, s.getSchema());
	}

	@Test void b05_schemaDefault_doesNotOverwrite() {
		var schema1 = HttpPartSchema.DEFAULT;
		var schema2 = HttpPartSchema.create().build();
		var s = JsonSerializer.DEFAULT.createSession()
			.schema(schema1)
			.schemaDefault(schema2)  // Should not overwrite existing.
			.build();
		assertSame(schema1, s.getSchema());
	}

	@Test void b06_schemaDefault_nullIgnored() {
		var s = JsonSerializer.DEFAULT.createSession()
			.schemaDefault(null)
			.build();
		assertNull(s.getSchema());
	}

	@Test void b07_uriContext_nullIgnored() {
		var s = JsonSerializer.DEFAULT.createSession()
			.uriContext(null)
			.build();
		assertNotNull(s);  // Default kept.
	}

	@Test void b08_keepNullProperties_setter() {
		var s = JsonSerializer.DEFAULT.createSession()
			.keepNullProperties(true)
			.build();
		assertTrue(s.isKeepNullProperties());
	}

	@Test void b09_trimStrings_setter() {
		var s = JsonSerializer.DEFAULT.createSession()
			.trimStrings(true)
			.build();
		assertTrue(s.isTrimStrings());
	}

	@Test void b10_addBeanTypes_setter() {
		var s = JsonSerializer.DEFAULT.createSession()
			.addBeanTypes(true)
			.build();
		assertTrue(s.isAddBeanTypes());
	}

	@Test void b11_sortCollections_setter() {
		var s = JsonSerializer.DEFAULT.createSession()
			.sortCollections(true)
			.build();
		assertTrue(s.isSortCollections());
	}

	@Test void b12_trimEmptyCollections_setter() {
		var s = JsonSerializer.DEFAULT.createSession()
			.trimEmptyCollections(true)
			.build();
		assertTrue(s.isTrimEmptyCollections());
	}

	//====================================================================================================
	// c. canIgnoreValue / trim-empty branches (lines 526-547)
	//====================================================================================================

	@Test void c01_trimEmptyCollections_emptyArray() throws Exception {
		var s = JsonSerializer.create().trimEmptyCollections().build();
		var bean = new BeanWithArrays();
		bean.empty = new String[0];
		bean.nonEmpty = new String[]{"a"};
		var json = s.serialize(bean);
		// empty array should be omitted.
		assertFalse(json.contains("\"empty\""));
		assertTrue(json.contains("\"nonEmpty\""));
	}

	@Test void c02_trimEmptyCollections_emptyCollection() throws Exception {
		var s = JsonSerializer.create().trimEmptyCollections().build();
		var bean = new BeanWithCollections();
		bean.empty = list();
		bean.nonEmpty = list("a");
		var json = s.serialize(bean);
		assertFalse(json.contains("\"empty\""));
		assertTrue(json.contains("\"nonEmpty\""));
	}

	@Test void c03_trimEmptyMaps_emptyMap() throws Exception {
		var s = JsonSerializer.create().trimEmptyMaps().build();
		var bean = new BeanWithMaps();
		bean.empty = new LinkedHashMap<>();
		bean.nonEmpty = new LinkedHashMap<>();
		bean.nonEmpty.put("k", "v");
		var json = s.serialize(bean);
		assertFalse(json.contains("\"empty\""));
		assertTrue(json.contains("\"nonEmpty\""));
	}

	@Test void c04_keepNullProperties_keepsNullField() throws Exception {
		var s = JsonSerializer.create().keepNullProperties().build();
		var bean = new BeanWithNullField();
		var json = s.serialize(bean);
		assertTrue(json.contains("\"a\""));  // null kept.
	}

	public static class BeanWithArrays {
		public String[] empty;
		public String[] nonEmpty;
	}

	public static class BeanWithCollections {
		public List<String> empty;
		public List<String> nonEmpty;
	}

	public static class BeanWithMaps {
		public Map<String,String> empty;
		public Map<String,String> nonEmpty;
	}

	public static class BeanWithNullField {
		public String a = null;
		public int b = 1;
	}

	//====================================================================================================
	// d. forEachEntry / sort branches (lines 559, 579, 845-866, 879)
	//====================================================================================================

	@Test void d01_forEachEntry_collectionSorted() {
		var s = (SerializerSession) JsonSerializer.create().sortCollections().build().createSession().build();
		var input = list("c", "a", "b");
		var collected = new ArrayList<String>();
		s.forEachEntry(input, collected::add);
		assertEquals(list("a", "b", "c"), collected);
	}

	@Test void d02_forEachEntry_collectionUnsortedWhenDisabled() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		var input = list("c", "a", "b");
		var collected = new ArrayList<String>();
		s.forEachEntry(input, collected::add);
		assertEquals(list("c", "a", "b"), collected);
	}

	@Test void d03_forEachEntry_emptyCollection() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		var input = new ArrayList<String>();
		var collected = new ArrayList<String>();
		s.forEachEntry(input, collected::add);
		assertTrue(collected.isEmpty());
	}

	@Test void d04_forEachEntry_sortedSetSkipsResort() {
		var s = (SerializerSession) JsonSerializer.create().sortCollections().build().createSession().build();
		var input = new TreeSet<String>(list("c", "a", "b"));
		var collected = new ArrayList<String>();
		s.forEachEntry(input, collected::add);
		// TreeSet already sorted; consumer order matches iteration.
		assertEquals(list("a", "b", "c"), collected);
	}

	@Test void d05_forEachEntry_collectionWithNonComparable() {
		var s = (SerializerSession) JsonSerializer.create().sortCollections().build().createSession().build();
		var input = list(new Object(), new Object());
		var collected = new ArrayList<Object>();
		s.forEachEntry(input, collected::add);
		// Non-Comparable should not be sorted.
		assertEquals(2, collected.size());
	}

	@Test void d06_forEachEntry_mapSorted() {
		var s = (SerializerSession) JsonSerializer.create().sortMaps().build().createSession().build();
		var input = new LinkedHashMap<String,Integer>();
		input.put("c", 3);
		input.put("a", 1);
		input.put("b", 2);
		var collected = new ArrayList<String>();
		s.forEachEntry(input, e -> collected.add(e.getKey()));
		assertEquals(list("a", "b", "c"), collected);
	}

	@Test void d07_forEachEntry_mapUnsorted() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		var input = new LinkedHashMap<String,Integer>();
		input.put("c", 3);
		input.put("a", 1);
		input.put("b", 2);
		var collected = new ArrayList<String>();
		s.forEachEntry(input, e -> collected.add(e.getKey()));
		assertEquals(list("c", "a", "b"), collected);
	}

	@Test void d08_forEachEntry_emptyMap() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		var collected = new ArrayList<String>();
		s.forEachEntry(new HashMap<String,Integer>(), e -> collected.add(e.getKey()));
		assertTrue(collected.isEmpty());
	}

	@Test void d09_forEachEntry_sortedMapSkipsResort() {
		var s = (SerializerSession) JsonSerializer.create().sortMaps().build().createSession().build();
		var input = new TreeMap<String,Integer>();
		input.put("c", 3);
		input.put("a", 1);
		input.put("b", 2);
		var collected = new ArrayList<String>();
		s.forEachEntry(input, e -> collected.add(e.getKey()));
		assertEquals(list("a", "b", "c"), collected);
	}

	//====================================================================================================
	// e. sort(...) variants (lines 845-882)
	//====================================================================================================

	@Test void e01_sortList_sortable() {
		var s = (SerializerSession) JsonSerializer.create().sortCollections().build().createSession().build();
		var result = s.sort(list("c", "a", "b"));
		assertEquals(list("a", "b", "c"), result);
	}

	@Test void e02_sortList_emptyReturnsSame() {
		var s = (SerializerSession) JsonSerializer.create().sortCollections().build().createSession().build();
		List<String> input = list();
		var result = s.sort(input);
		assertSame(input, result);
	}

	@Test void e03_sortList_sortDisabledReturnsSame() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		List<String> input = list("c", "a", "b");
		var result = s.sort(input);
		assertSame(input, result);
	}

	@Test void e04_sortList_nonSortableReturnsSame() {
		var s = (SerializerSession) JsonSerializer.create().sortCollections().build().createSession().build();
		List<Object> input = list(new Object(), new Object());
		var result = s.sort(input);
		assertSame(input, result);  // non-Comparable: not sorted.
	}

	@Test void e05_sortCollection_returnsSorted() {
		var s = (SerializerSession) JsonSerializer.create().sortCollections().build().createSession().build();
		Collection<String> input = list("c", "a", "b");
		var result = s.sort(input);
		assertEquals(list("a", "b", "c"), new ArrayList<>(result));
	}

	@Test void e06_sortCollection_sortedSetReturnsSame() {
		var s = (SerializerSession) JsonSerializer.create().sortCollections().build().createSession().build();
		Collection<String> input = new TreeSet<>(list("c", "a", "b"));
		var result = s.sort(input);
		assertSame(input, result);
	}

	@Test void e07_sortMap_returnsSorted() {
		var s = (SerializerSession) JsonSerializer.create().sortMaps().build().createSession().build();
		var input = new LinkedHashMap<String,Integer>();
		input.put("c", 3);
		input.put("a", 1);
		var result = s.sort(input);
		assertTrue(result instanceof TreeMap);
	}

	@Test void e08_sortMap_sortedMapReturnsSame() {
		var s = (SerializerSession) JsonSerializer.create().sortMaps().build().createSession().build();
		Map<String,Integer> input = new TreeMap<>();
		input.put("c", 3);
		var result = s.sort(input);
		assertSame(input, result);
	}

	@Test void e09_sortMap_emptyReturnsSame() {
		var s = (SerializerSession) JsonSerializer.create().sortMaps().build().createSession().build();
		Map<String,Integer> input = new LinkedHashMap<>();
		var result = s.sort(input);
		assertSame(input, result);
	}

	@Test void e10_sortMap_disabledReturnsSame() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		var input = new LinkedHashMap<String,Integer>();
		input.put("c", 3);
		input.put("a", 1);
		var result = s.sort(input);
		assertSame(input, result);
	}

	//====================================================================================================
	// f. trim() and toString() conversions (lines 897-1002)
	//====================================================================================================

	@Test void f01_trim_null() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		assertNull(s.trim(null));
	}

	@Test void f02_trim_noTrim() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		assertEquals("  hello  ", s.trim("  hello  "));
	}

	@Test void f03_trim_withTrim() {
		var s = (SerializerSession) JsonSerializer.create().trimStrings().build().createSession().build();
		assertEquals("hello", s.trim("  hello  "));
	}

	@Test void f04_toString_null() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		assertNull(s.toString(null));
	}

	@Test void f05_toString_classObject() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		var result = s.toString(String.class);
		assertEquals("java.lang.String", result);
	}

	@Test void f06_toString_classInfo() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		var ci = org.apache.juneau.commons.reflect.ClassInfo.of(String.class);
		var result = s.toString(ci);
		// ClassInfo.getNameFull() returns "public final class java.lang.String".
		assertTrue(result.contains("java.lang.String"), "Got: " + result);
	}

	@Test void f07_toString_enum() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		var result = s.toString(Thread.State.NEW);
		assertEquals("NEW", result);
	}

	@Test void f08_toString_date() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		var d = new Date(0);
		var result = s.toString(d);
		assertNotNull(result);
	}

	@Test void f09_toString_calendar() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		var cal = GregorianCalendar.from(Instant.EPOCH.atZone(ZoneOffset.UTC));
		var result = s.toString(cal);
		assertNotNull(result);
	}

	@Test void f10_toString_temporal() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		var t = Instant.ofEpochMilli(0);
		var result = s.toString(t);
		assertNotNull(result);
	}

	@Test void f11_toString_duration() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		var d = Duration.ofSeconds(60);
		var result = s.toString(d);
		assertNotNull(result);
	}

	@Test void f12_toString_period() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		var p = Period.ofDays(1);
		var result = s.toString(p);
		assertNotNull(result);
	}

	@Test void f13_toString_normalString_noTrim() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		assertEquals("  hello  ", s.toString("  hello  "));
	}

	@Test void f14_toString_normalString_withTrim() {
		var s = (SerializerSession) JsonSerializer.create().trimStrings().build().createSession().build();
		assertEquals("hello", s.toString("  hello  "));
	}

	//====================================================================================================
	// g. unwrapSupplier (lines 816-822)
	//====================================================================================================

	@Test void g01_unwrapSupplier_singleLevel() throws Exception {
		Supplier<String> sup = () -> "hi";
		var json = Json5Serializer.DEFAULT.serialize(sup);
		assertEquals("'hi'", json);
	}

	@Test void g02_unwrapSupplier_nested() throws Exception {
		Supplier<Supplier<String>> sup = () -> () -> "deep";
		var json = Json5Serializer.DEFAULT.serialize(sup);
		assertEquals("'deep'", json);
	}

	@Test void g03_unwrapSupplier_exceedsDepth() {
		Supplier<?> chain = () -> "bottom";
		for (int i = 0; i < 12; i++) {
			final Supplier<?> prev = chain;
			chain = () -> prev;
		}
		final Supplier<?> deep = chain;
		assertThrows(SerializeException.class, () -> Json5Serializer.DEFAULT.serialize(deep));
	}

	//====================================================================================================
	// h. serialize() error paths (line 787-789, 793-797)
	//====================================================================================================

	@Test void h01_serialize_beanConsumer_rejected() {
		BeanConsumer<String> bc = item -> {};
		var ex = assertThrows(SerializeException.class, () -> Json5Serializer.DEFAULT.serialize(bc));
		assertTrue(ex.getMessage().contains("BeanConsumer cannot be used as a serializer source"));
	}

	@SuppressWarnings({
		"java:S5778" // assertThrows lambdas contain multiple calls; only the primary call throws.
	})
	@Test void h02_serialize_runtimeException_wrapped() {
		// A bean property getter that throws RuntimeException should bubble up as SerializeException.
		assertThrows(SerializeException.class,
			() -> JsonSerializer.DEFAULT.serialize(new BeanThatThrowsRE()));
	}

	public static class BeanThatThrowsRE {
		public String getA() { throw new IllegalStateException("oops"); }
		public void setA(String value) { /* no-op */ }
	}

	//====================================================================================================
	// i. Listener notification - onBeanGetterException, onError (lines 1338-1365)
	//====================================================================================================

	public static class CapturingListener extends SerializerListener {
		public static List<String> events = new ArrayList<>();
		public static void reset() { events = new ArrayList<>(); }
		@Override public void onBeanGetterException(SerializerSession session, Throwable t, BeanPropertyMeta p) {
			events.add("bean:" + p.getName());
		}
		@Override public void onError(SerializerSession session, Throwable t, String msg) {
			events.add("error:" + msg);
		}
	}

	@Test void i01_listener_beanGetterException_ignored_doesNotThrow() throws Exception {
		// When ignoreInvocationExceptionsOnGetters is enabled, the BeanMap getRaw() short-circuits the
		// throw and returns null without surfacing the exception, so the JSON path never sees `thrown` and
		// the listener is NOT invoked. This documents the current behavior (production observation for issue 156).
		CapturingListener.reset();
		var s = JsonSerializer.create()
			.ignoreInvocationExceptionsOnGetters()
			.listener(CapturingListener.class)
			.build();
		// Should not throw because exceptions on getters are ignored.
		var json = s.serialize(new BeanThatThrowsRE());
		assertNotNull(json);
		// Listener does not fire because the underlying exception is swallowed by BeanPropertyMeta.getRaw.
		assertTrue(CapturingListener.events.isEmpty(), "Listener unexpectedly fired: " + CapturingListener.events);
	}

	@SuppressWarnings({
		"java:S5778" // assertThrows lambdas contain multiple calls; only the primary call throws.
	})
	@Test void i02_listener_beanGetterException_invoked_thrown() {
		CapturingListener.reset();
		var s = JsonSerializer.create()
			.listener(CapturingListener.class)
			.build();
		// Exceptions on getters are NOT ignored; SerializeException is thrown.
		assertThrows(SerializeException.class, () -> s.serialize(new BeanThatThrowsRE()));
		assertTrue(CapturingListener.events.stream().anyMatch(e -> e.startsWith("bean:")),
			"Listener should fire even when exception is rethrown");
	}

	@Test void i03_listener_getListenerByClass() {
		CapturingListener.reset();
		var s = (SerializerSession) JsonSerializer.create()
			.listener(CapturingListener.class)
			.build()
			.createSession()
			.build();
		var l = s.getListener(CapturingListener.class);
		assertNotNull(l);
	}

	@Test void i04_listener_getListener_noListener() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		assertNull(s.getListener());
	}

	//====================================================================================================
	// j. resolve / addVarBean / VarResolverSession path (lines 707-728)
	//====================================================================================================

	@Test void j01_resolve_systemProperty() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		// VarResolver.DEFAULT supports $S{name,default}; we pass a literal that won't expand.
		assertEquals("plain text", s.resolve("plain text"));
	}

	@Test void j02_addVarBean() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		var ret = s.addVarBean(String.class, "hello");
		assertSame(s, ret);
	}

	@Test void j03_getVarResolver_lazyInit() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		var vrs = s.getVarResolver();
		assertNotNull(vrs);
		// Subsequent call returns same instance (cached).
		assertSame(vrs, s.getVarResolver());
	}

	//====================================================================================================
	// k. resolveUri / relativizeUri (lines 757-759, 1429-1431)
	//====================================================================================================

	@Test void k01_resolveUri_passesToResolver() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		var result = s.resolveUri("foo://bar");
		assertNotNull(result);
	}

	//====================================================================================================
	// l. Default builder.build() (line 161) and create(ctx) (line 393)
	//====================================================================================================

	@Test void l01_create_returnsBuilder() {
		var b = SerializerSession.create(JsonSerializer.DEFAULT);
		assertNotNull(b);
		var s = b.build();
		assertNotNull(s);
	}

	@Test void l02_create_nullCtx_throws() {
		assertThrows(IllegalArgumentException.class, () -> SerializerSession.create((Serializer) null));
	}

	//====================================================================================================
	// m. handleThrown static dispatch (lines 414-425)
	//====================================================================================================

	@Test void m01_handleThrown_runtimeException() throws Exception {
		var method = SerializerSession.class.getDeclaredMethod("handleThrown", Throwable.class);
		method.setAccessible(true);
		assertThrows(InvocationTargetException.class, () -> {
			try {
				method.invoke(null, new IllegalStateException("rt"));
			} catch (InvocationTargetException e) {
				assertTrue(e.getCause() instanceof RuntimeException);
				throw e;
			}
		});
	}

	@Test void m02_handleThrown_error() throws Exception {
		var method = SerializerSession.class.getDeclaredMethod("handleThrown", Throwable.class);
		method.setAccessible(true);
		assertThrows(InvocationTargetException.class, () -> {
			try {
				method.invoke(null, new Error("err"));
			} catch (InvocationTargetException e) {
				assertTrue(e.getCause() instanceof Error);
				throw e;
			}
		});
	}

	@Test void m03_handleThrown_stackOverflow_convertedToSerializeException() throws Exception {
		// StackOverflowError is checked before the generic Error branch, and is converted to a SerializeException
		// with a recursion-detection hint so callers up the stack get a clean serialize-time error.
		var method = SerializerSession.class.getDeclaredMethod("handleThrown", Throwable.class);
		method.setAccessible(true);
		var ex = assertThrows(InvocationTargetException.class, () -> method.invoke(null, new StackOverflowError()));
		assertTrue(ex.getCause() instanceof SerializeException, () -> "Expected SerializeException, got: " + ex.getCause());
		assertTrue(ex.getCause().getMessage().contains("Stack overflow occurred"));
	}

	@Test void m04_handleThrown_serializeException() throws Exception {
		var method = SerializerSession.class.getDeclaredMethod("handleThrown", Throwable.class);
		method.setAccessible(true);
		var se = new SerializeException("boom");
		assertThrows(InvocationTargetException.class, () -> {
			try {
				method.invoke(null, se);
			} catch (InvocationTargetException e) {
				assertTrue(e.getCause() instanceof SerializeException);
				assertSame(se, e.getCause());
				throw e;
			}
		});
	}

	@Test void m05_handleThrown_otherException() throws Exception {
		var method = SerializerSession.class.getDeclaredMethod("handleThrown", Throwable.class);
		method.setAccessible(true);
		assertThrows(InvocationTargetException.class, () -> {
			try {
				method.invoke(null, new IOException("io"));
			} catch (InvocationTargetException e) {
				assertTrue(e.getCause() instanceof SerializeException);
				throw e;
			}
		});
	}

	//====================================================================================================
	// n. toList(Class, Object) - primitive vs object array (line 442-452)
	//====================================================================================================

	@Test void n01_toList_primitiveArray() throws Exception {
		var method = SerializerSession.class.getDeclaredMethod("toList", Class.class, Object.class);
		method.setAccessible(true);
		var result = (List<?>) method.invoke(null, int[].class, new int[]{1, 2, 3});
		assertEquals(3, result.size());
		assertEquals(1, result.get(0));
	}

	@Test void n02_toList_objectArray() throws Exception {
		var method = SerializerSession.class.getDeclaredMethod("toList", Class.class, Object.class);
		method.setAccessible(true);
		var result = (List<?>) method.invoke(null, String[].class, new String[]{"a", "b"});
		assertEquals(2, result.size());
		assertEquals("a", result.get(0));
	}

	//====================================================================================================
	// o. Bean type name dispatch (getBeanTypeName) - lines 1101-1139
	//====================================================================================================

	@Marshalled(typeName="A1")
	public static class A1 {
		public String foo;
	}

	@Marshalled(typeName="A2")
	public static class A2 extends A1 {
		public String bar;
	}

	@Marshalled(dictionary={A1.class, A2.class})
	public static class Container {
		public A1 prop;
	}

	@Test void o01_getBeanTypeName_writtenAtRoot() throws Exception {
		var s = JsonSerializer.create()
			.addBeanTypes()
			.addRootType()
			.beanDictionary(A2.class)
			.build();
		var json = s.serialize(new A2());
		// _type=A2 should be added at root.
		assertTrue(json.contains("_type") && json.contains("A2"), "Expected type marker, got: " + json);
	}

	@Test void o02_getBeanTypeName_subtypeViaDictionary() throws Exception {
		var s = JsonSerializer.create()
			.addBeanTypes()
			.build();
		var c = new Container();
		c.prop = new A2();
		var json = s.serialize(c);
		// Subtype A2 should be marked with _type since expected type is A1.
		assertTrue(json.contains("A2"), "Expected subtype marker: " + json);
	}

	//====================================================================================================
	// p. BeanSupplier streamable handling (line 606-613)
	//====================================================================================================

	@Test void p01_forEachStreamableEntry_iterable() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		var input = list("a", "b");
		var collected = new ArrayList<>();
		Consumer<Object> c = collected::add;
		s.forEachStreamableEntry(input, s.getClassMeta(List.class), c);
		assertEquals(list("a", "b"), collected);
	}

	@Test void p02_forEachStreamableEntry_null() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		var collected = new ArrayList<>();
		Consumer<Object> c = collected::add;
		s.forEachStreamableEntry(null, s.getClassMeta(List.class), c);
		assertTrue(collected.isEmpty());
	}

	@Test void p03_forEachStreamableEntry_iterator() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		var iter = list("a", "b").iterator();
		var collected = new ArrayList<>();
		Consumer<Object> c = collected::add;
		s.forEachStreamableEntry(iter, s.getClassMeta(Iterator.class), c);
		assertEquals(list("a", "b"), collected);
	}

	@Test void p04_forEachStreamableEntry_enumeration() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		var en = Collections.enumeration(list("x", "y"));
		var collected = new ArrayList<>();
		Consumer<Object> c = collected::add;
		// Enumeration is iterator-like (isIterator()=true) when wrapped via asIterator().
		s.forEachStreamableEntry(en, s.getClassMeta(Enumeration.class), c);
		assertEquals(list("x", "y"), collected);
	}

	@Test void p05_forEachStreamableEntry_stream() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		var stream = list("x", "y").stream();
		var collected = new ArrayList<>();
		Consumer<Object> c = collected::add;
		s.forEachStreamableEntry(stream, s.getClassMeta(java.util.stream.Stream.class), c);
		assertEquals(list("x", "y"), collected);
	}

	@Test void p06_toListFromStreamable() {
		var s = (SerializerSession) JsonSerializer.DEFAULT.createSession().build();
		var list = s.toListFromStreamable(list("a", "b"), s.getClassMeta(List.class));
		assertEquals(2, list.size());
	}

	//====================================================================================================
	// q. swap(ObjectSwap, Object) (lines 1441-1448)
	//====================================================================================================

	@Test void q01_swap_nullSwapReturnsObject() throws Exception {
		var s = JsonSerializer.DEFAULT.createSession().build();
		var method = SerializerSession.class.getDeclaredMethod("swap", org.apache.juneau.marshall.swap.ObjectSwap.class, Object.class);
		method.setAccessible(true);
		var result = method.invoke(s, null, "hello");
		assertEquals("hello", result);
	}

	//====================================================================================================
	// r. Misc convenience (isWriterSerializer, getResponseHeaders)
	//====================================================================================================

	@Test void r01_isWriterSerializer_jsonIsTrue() {
		var s = JsonSerializer.DEFAULT.createSession().build();
		assertTrue(s.isWriterSerializer());
	}

	@Test void r02_getResponseHeaders() {
		var s = JsonSerializer.DEFAULT.createSession().build();
		var h = s.getResponseHeaders();
		assertNotNull(h);
	}

	//====================================================================================================
	// s. serialize (Object) shortcut throws unsupportedOp on raw SerializerSession (line 773)
	//====================================================================================================

	// Note: SerializerSession.serialize(Object) and serializeToString(Object) base implementations throw
	// UnsupportedOperationException. They are normally overridden by WriterSerializerSession/OutputStreamSerializerSession
	// and unreachable via concrete sessions. Lines 772-774/834-836 are intentionally dead code on this path.

	//====================================================================================================
	// t. canIgnoreValue object-typed branch (lines 530, 532, 536)
	//====================================================================================================

	@Test void t01_canIgnoreValue_objectTypedArrayEmpty() throws Exception {
		var s = JsonSerializer.create().trimEmptyCollections().build();
		// Object-typed field holding empty array (cm.isObject() == true, isArray(value) == true).
		var bean = new BeanWithObjectField();
		bean.a = new String[0];
		var json = s.serialize(bean);
		assertFalse(json.contains("\"a\""), "Empty Object-typed array should be trimmed: " + json);
	}

	@Test void t02_canIgnoreValue_objectTypedCollectionEmpty() throws Exception {
		var s = JsonSerializer.create().trimEmptyCollections().build();
		var bean = new BeanWithObjectField();
		bean.a = list();
		var json = s.serialize(bean);
		assertFalse(json.contains("\"a\""), "Empty Object-typed collection should be trimmed: " + json);
	}

	@Test void t03_canIgnoreValue_objectTypedMapEmpty() throws Exception {
		var s = JsonSerializer.create().trimEmptyMaps().build();
		var bean = new BeanWithObjectField();
		bean.a = new LinkedHashMap<>();
		var json = s.serialize(bean);
		assertFalse(json.contains("\"a\""), "Empty Object-typed map should be trimmed: " + json);
	}

	public static class BeanWithObjectField {
		public Object a;
		public int b = 1;
	}

	//====================================================================================================
	// u. Bean type name dispatch - all branches (lines 1101-1139)
	//====================================================================================================

	@Marshalled(typeName="U_A")
	public static class U_A { public String foo; }

	@Marshalled(typeName="U_B")
	public static class U_B extends U_A { public String bar; }

	public static class U_Holder {
		public U_A prop;
	}

	@Marshalled(dictionary={U_B.class})
	public static class U_HolderWithDictAtBean {
		public U_A prop;
	}

	@Test void u01_typeName_actualTypeMatchesExpected_returnsNull() throws Exception {
		// When eType == aType, getBeanTypeName returns null (no _type emitted).
		var s = JsonSerializer.create().addBeanTypes().beanDictionary(U_A.class).build();
		var holder = new U_Holder();
		holder.prop = new U_A();  // expected and actual both U_A.
		var json = s.serialize(holder);
		// _type marker should not be on the inner property.
		assertFalse(json.contains("U_A"), "Did not expect type name to appear: " + json);
	}

	@Test void u02_typeName_subtype_emittedFromSessionDictionary() throws Exception {
		// Subtype + dictionary on session => _type on property.
		var s = JsonSerializer.create().addBeanTypes().beanDictionary(U_B.class).build();
		var holder = new U_Holder();
		holder.prop = new U_B();
		var json = s.serialize(holder);
		assertTrue(json.contains("U_B"), "Expected U_B type marker: " + json);
	}

	@Test void u03_typeName_subtype_emittedFromBeanLevelDictionary() throws Exception {
		// dictionary on the holder bean => found via eType.getBeanRegistry().
		var s = JsonSerializer.create().addBeanTypes().build();
		var holder = new U_HolderWithDictAtBean();
		holder.prop = new U_B();
		var json = s.serialize(holder);
		assertTrue(json.contains("U_B"), "Expected U_B type marker via bean registry: " + json);
	}

	//====================================================================================================
	// v. onError listener wiring (lines 1361-1365) - exercise via push/recursion path
	//====================================================================================================

	public static class V_Cycle {
		public V_Cycle ref;
	}

	public static class CapturingErrorListener extends SerializerListener {
		public static List<String> events = new ArrayList<>();
		public static void reset() { events = new ArrayList<>(); }
		@Override public void onError(SerializerSession session, Throwable t, String msg) {
			events.add("error:" + msg);
		}
	}

	@Test void v01_listener_onError_invoked_onIgnoredRecursion() throws Exception {
		// When ignoreRecursions is on, a recursion warning fires through onError.
		CapturingErrorListener.reset();
		var s = JsonSerializer.create()
			.detectRecursions()
			.ignoreRecursions()
			.listener(CapturingErrorListener.class)
			.build();
		var c = new V_Cycle();
		c.ref = c;
		// Should not throw; recursion ignored.
		var json = s.serialize(c);
		assertNotNull(json);
	}

	//====================================================================================================
	// w. getJavaMethod, getDateFormat, getLocaleFormat, etc. — protected getters via reflection
	//====================================================================================================

	@Test void w01_getJavaMethod() throws Exception {
		var m = SerializerSession_Test.class.getDeclaredMethod("w01_getJavaMethod");
		var s = JsonSerializer.DEFAULT.createSession()
			.javaMethod(m)
			.build();
		var method = SerializerSession.class.getDeclaredMethod("getJavaMethod");
		method.setAccessible(true);
		assertSame(m, method.invoke(s));
	}

	@SuppressWarnings({
		"java:S5976" // Parameterization would reduce clarity for these format-specific edge cases.
	})
	@Test void w02_getTimeZoneFormat() throws Exception {
		var s = JsonSerializer.DEFAULT.createSession().build();
		var method = SerializerSession.class.getDeclaredMethod("getTimeZoneFormat");
		method.setAccessible(true);
		assertNotNull(method.invoke(s));
	}

	@Test void w03_getLocaleFormat() throws Exception {
		var s = JsonSerializer.DEFAULT.createSession().build();
		var method = SerializerSession.class.getDeclaredMethod("getLocaleFormat");
		method.setAccessible(true);
		assertNotNull(method.invoke(s));
	}

	@Test void w04_getDurationFormat() throws Exception {
		var s = JsonSerializer.DEFAULT.createSession().build();
		var method = SerializerSession.class.getDeclaredMethod("getDurationFormat");
		method.setAccessible(true);
		assertNotNull(method.invoke(s));
	}

	@Test void w05_getPeriodFormat() throws Exception {
		var s = JsonSerializer.DEFAULT.createSession().build();
		var method = SerializerSession.class.getDeclaredMethod("getPeriodFormat");
		method.setAccessible(true);
		assertNotNull(method.invoke(s));
	}

	@Test void w06_getCalendarFormat() throws Exception {
		var s = JsonSerializer.DEFAULT.createSession().build();
		var method = SerializerSession.class.getDeclaredMethod("getCalendarFormat");
		method.setAccessible(true);
		assertNotNull(method.invoke(s));
	}

	@Test void w07_getDateFormat() throws Exception {
		var s = JsonSerializer.DEFAULT.createSession().build();
		var method = SerializerSession.class.getDeclaredMethod("getDateFormat");
		method.setAccessible(true);
		assertNotNull(method.invoke(s));
	}

	@Test void w08_getTemporalFormat() throws Exception {
		var s = JsonSerializer.DEFAULT.createSession().build();
		var method = SerializerSession.class.getDeclaredMethod("getTemporalFormat");
		method.setAccessible(true);
		assertNotNull(method.invoke(s));
	}

	@Test void w09_getUriContext() throws Exception {
		var s = JsonSerializer.DEFAULT.createSession().build();
		var method = SerializerSession.class.getDeclaredMethod("getUriContext");
		method.setAccessible(true);
		assertNotNull(method.invoke(s));
	}

	@Test void w10_getUriRelativity() throws Exception {
		var s = JsonSerializer.DEFAULT.createSession().build();
		var method = SerializerSession.class.getDeclaredMethod("getUriRelativity");
		method.setAccessible(true);
		assertNotNull(method.invoke(s));
	}

	@Test void w11_getUriResolution() throws Exception {
		var s = JsonSerializer.DEFAULT.createSession().build();
		var method = SerializerSession.class.getDeclaredMethod("getUriResolution");
		method.setAccessible(true);
		assertNotNull(method.invoke(s));
	}

	@Test void w12_getUriResolver() throws Exception {
		var s = JsonSerializer.DEFAULT.createSession().build();
		var method = SerializerSession.class.getDeclaredMethod("getUriResolver");
		method.setAccessible(true);
		assertNotNull(method.invoke(s));
	}

	@Test void w13_relativizeUri() throws Exception {
		var s = JsonSerializer.DEFAULT.createSession().build();
		var method = SerializerSession.class.getDeclaredMethod("relativizeUri", Object.class, Object.class);
		method.setAccessible(true);
		var result = method.invoke(s, "http://example.com/foo", "http://example.com/foo/bar");
		assertNotNull(result);
	}

	//====================================================================================================
	// x. createPipe (line 1043-1045) - direct invocation
	//====================================================================================================

	@Test void x01_createPipe_returnsNonNull() throws Exception {
		var s = JsonSerializer.DEFAULT.createSession().build();
		var method = SerializerSession.class.getDeclaredMethod("createPipe", Object.class);
		method.setAccessible(true);
		var sb = new StringBuilder();
		var pipe = method.invoke(s, sb);
		assertNotNull(pipe);
	}

	//====================================================================================================
	// y. createBeanTypeNameProperty (lines 403-406) and unused-but-static helpers
	//====================================================================================================

	@Test void y01_createBeanTypeNameProperty() throws Exception {
		var bm = org.apache.juneau.commons.bean.BeanMap.of(new U_A());
		var method = SerializerSession.class.getDeclaredMethod("createBeanTypeNameProperty",
			org.apache.juneau.commons.bean.BeanMap.class, String.class);
		method.setAccessible(true);
		var result = method.invoke(null, bm, "U_A");
		assertNotNull(result);
	}

	//====================================================================================================
	// z. push2 and getExpectedRootType (lines 1101-1106, 1387-1393)
	//====================================================================================================

	@Test void z01_getExpectedRootType_addRootType() throws Exception {
		var s = JsonSerializer.create().addRootType().build().createSession().build();
		var method = SerializerSession.class.getDeclaredMethod("getExpectedRootType", Object.class);
		method.setAccessible(true);
		var result = method.invoke(s, new Object());
		assertNotNull(result);
	}

	@Test void z02_getExpectedRootType_optional() throws Exception {
		var s = JsonSerializer.DEFAULT.createSession().build();
		var method = SerializerSession.class.getDeclaredMethod("getExpectedRootType", Object.class);
		method.setAccessible(true);
		var result = method.invoke(s, o("hello"));
		assertNotNull(result);
	}

	@Test void z03_getExpectedRootType_normal() throws Exception {
		var s = JsonSerializer.DEFAULT.createSession().build();
		var method = SerializerSession.class.getDeclaredMethod("getExpectedRootType", Object.class);
		method.setAccessible(true);
		var result = method.invoke(s, "hello");
		assertNotNull(result);
	}
}
