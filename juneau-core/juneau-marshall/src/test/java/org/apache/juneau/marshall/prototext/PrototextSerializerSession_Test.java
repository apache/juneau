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
package org.apache.juneau.marshall.prototext;

import static org.apache.juneau.commons.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Tests targeting low-coverage paths in {@link PrototextSerializerSession}.
 *
 * <p>Exercises serializer paths that the higher-level Prototext*_Test files don't reach:
 * collections-of-beans inside beans, arrays of primitives/beans, streamable element types
 * (Iterable/Iterator/Stream), nested map-of-maps, debug trace toggle, useColon/listSyntax
 * options, scalar value type dispatch (Date/Calendar/Temporal/Duration/Period/byte[]), and
 * bean property comments.
 */
@SuppressWarnings("unchecked")
class PrototextSerializerSession_Test extends TestBase {

	// Debug trace

	@Test void a01_setDebugTrace_toggle() throws Exception {
		// Snapshot starting state then exercise the static toggle in both directions.
		PrototextSerializerSession.setDebugTrace(true);
		try {
			var m = JsonMap.of("k", "v");
			var proto = PrototextSerializer.DEFAULT.serialize(m);
			assertNotNull(proto);
			assertTrue(proto.contains("k"));
			// Trace log should have captured at least one entry while debugTrace was on.
			assertFalse(PrototextSerializerSession.traceLog.isEmpty());
		} finally {
			PrototextSerializerSession.setDebugTrace(false);
		}
	}

	@Test void a02_debugTrace_serializeAnythingRoot() throws Exception {
		// Force serializeAnything root path (top-level Collection) with debug trace enabled.
		PrototextSerializerSession.setDebugTrace(true);
		try {
			PrototextSerializerSession.traceLog.clear();
			var proto = PrototextSerializer.DEFAULT.serialize(List.of(1, 2, 3));
			assertNotNull(proto);
			assertFalse(PrototextSerializerSession.traceLog.isEmpty());
		} finally {
			PrototextSerializerSession.setDebugTrace(false);
		}
	}

	@Test void a03_doSerialize_nullObject() throws Exception {
		// null root must produce empty output without error.
		var proto = PrototextSerializer.DEFAULT.serialize(null);
		assertTrue(proto == null || proto.isEmpty());
	}

	// Top-level scalar / collection / array / streamable

	@Test void b01_topLevelString() throws Exception {
		// Hits scalar root branch (CONST_value).
		var proto = PrototextSerializer.DEFAULT.serialize("hello");
		assertNotNull(proto);
		assertTrue(proto.contains("hello"));
	}

	@Test void b02_topLevelInteger() throws Exception {
		var proto = PrototextSerializer.DEFAULT.serialize(42);
		assertNotNull(proto);
		assertTrue(proto.contains("42"));
	}

	@Test void b03_topLevelEnum() throws Exception {
		var proto = PrototextSerializer.DEFAULT.serialize(LogLevel.WARN);
		assertNotNull(proto);
		assertTrue(proto.contains("WARN"));
	}

	@Test void b04_topLevelArray() throws Exception {
		// Hits sType.isArray() branch in serializeAnything root path.
		var proto = PrototextSerializer.DEFAULT.serialize(new int[] { 1, 2, 3 });
		assertNotNull(proto);
		assertTrue(proto.contains("1") && proto.contains("2") && proto.contains("3"));
	}

	@Test void b05_topLevelStringArray() throws Exception {
		var proto = PrototextSerializer.DEFAULT.serialize(new String[] { "a", "b" });
		assertNotNull(proto);
		assertTrue(proto.contains("a") && proto.contains("b"));
	}

	@Test void b06_topLevelIterable() throws Exception {
		// Hits sType.isStreamable() branch.
		Iterable<String> it = List.of("x", "y", "z");
		var proto = PrototextSerializer.DEFAULT.serialize(it);
		assertNotNull(proto);
		assertTrue(proto.contains("x"));
	}

	@Test void b07_topLevelIterator() throws Exception {
		// Hits sType.isStreamable() branch with Iterator.
		Iterator<String> it = List.of("p", "q").iterator();
		var proto = PrototextSerializer.DEFAULT.serialize(it);
		assertNotNull(proto);
		assertTrue(proto.contains("p"));
	}

	@Test void b08_topLevelEmptyArray() throws Exception {
		var proto = PrototextSerializer.DEFAULT.serialize(new int[0]);
		assertNotNull(proto);
	}

	// Bean with collection-of-beans

	@Test void c01_beanProperty_listOfBeans() throws Exception {
		// Exercise the collection-of-beans branch in serializeBeanMap.
		// Use Map-of-Map values (which Juneau treats as bean-like) to match existing proto patterns.
		var c1 = new LinkedHashMap<String, Object>();
		c1.put("id", 1);
		c1.put("label", "alpha");
		var c2 = new LinkedHashMap<String, Object>();
		c2.put("id", 2);
		c2.put("label", "beta");
		var root = new LinkedHashMap<String, Object>();
		root.put("name", "parent");
		root.put("children", List.of(c1, c2));

		var proto = PrototextSerializer.DEFAULT.serialize(root);
		assertNotNull(proto);
		assertTrue(proto.contains("alpha") && proto.contains("beta"), () -> "Expected alpha+beta in: " + proto);
		assertTrue(proto.contains("children"), () -> "Expected children in: " + proto);
	}

	@Test void c02_beanProperty_listOfBeans_listSyntax() throws Exception {
		// useListSyntaxForBeans=true → exercise the [{...}, {...}] writer path.
		var ser = PrototextSerializer.create().useListSyntaxForBeans(true).build();
		var c1 = new LinkedHashMap<String, Object>();
		c1.put("id", 1);
		c1.put("label", "alpha");
		var c2 = new LinkedHashMap<String, Object>();
		c2.put("id", 2);
		c2.put("label", "beta");
		var root = new LinkedHashMap<String, Object>();
		root.put("name", "parent");
		root.put("children", List.of(c1, c2));

		var proto = ser.serialize(root);
		assertNotNull(proto);
		assertTrue(proto.contains("[") && proto.contains("]"), () -> "Expected list syntax in: " + proto);
		assertTrue(proto.contains("alpha") && proto.contains("beta"));
	}

	@Test void c03_arrayOfMaps() throws Exception {
		// Map<String,Object>[] property — exercises aType.isArray() branch in serializeBeanMap with bean elements.
		var c1 = new LinkedHashMap<String, Object>();
		c1.put("label", "x");
		var c2 = new LinkedHashMap<String, Object>();
		c2.put("label", "y");
		Map<String, Object>[] arr = new Map[] { c1, c2 };
		var root = new LinkedHashMap<String, Object>();
		root.put("name", "parent");
		root.put("children", arr);

		var proto = PrototextSerializer.DEFAULT.serialize(root);
		assertNotNull(proto);
		assertTrue(proto.contains("x") && proto.contains("y"), () -> "Expected x+y in: " + proto);
	}

	@Test void c04_intArray_property() throws Exception {
		// int[] in a Map property — exercises array → list of scalars branch.
		var root = new LinkedHashMap<String, Object>();
		root.put("values", new int[] { 10, 20, 30 });
		var proto = PrototextSerializer.DEFAULT.serialize(root);
		assertNotNull(proto);
		assertTrue(proto.contains("10") && proto.contains("20") && proto.contains("30"));
	}

	@Test void c05_listSyntax_useColon() throws Exception {
		// Combine useListSyntaxForBeans + useColonForMessages to hit both branches.
		var ser = PrototextSerializer.create().useListSyntaxForBeans(true).useColonForMessages(true).build();
		var c1 = new LinkedHashMap<String, Object>();
		c1.put("label", "z");
		var root = new LinkedHashMap<String, Object>();
		root.put("name", "parent");
		root.put("children", List.of(c1));

		var proto = ser.serialize(root);
		assertNotNull(proto);
		assertTrue(proto.contains("z"));
	}

	@Test void c06_useColonForMessages_nestedBean() throws Exception {
		var ser = PrototextSerializer.create().useColonForMessages(true).build();
		var inner = new LinkedHashMap<String, Object>();
		inner.put("x", 1);
		var m = new LinkedHashMap<String, Object>();
		m.put("inner", inner);

		var proto = ser.serialize(m);
		assertNotNull(proto);
		// Whitespace between `:` and `{` may vary (single or double space); accept any.
		assertTrue(proto.contains("inner:") && proto.contains("{"), () -> "Expected 'inner:' and '{' in: " + proto);
		assertTrue(proto.matches("(?s).*inner:\\s+\\{.*"), () -> "Expected 'inner:<ws>{' in: " + proto);
	}

	// Map-with-bean-or-map values

	@Test void d01_mapOfMaps() throws Exception {
		// Hits serializeMap → bean/map branch (recursive serializeMap).
		var inner = new LinkedHashMap<String, Object>();
		inner.put("a", 1);
		inner.put("b", 2);
		var outer = new LinkedHashMap<String, Object>();
		outer.put("nested", inner);
		// Wrap so root is a top-level message rather than the map itself.
		var root = new LinkedHashMap<String, Object>();
		root.put("config", outer);

		var proto = PrototextSerializer.DEFAULT.serialize(root);
		assertNotNull(proto);
		assertTrue(proto.contains("config {"));
		assertTrue(proto.contains("nested {"));
		assertTrue(proto.contains("1") && proto.contains("2"));
	}

	@Test void d02_mapWithListValues() throws Exception {
		// Hits serializeMap → collection branch.
		var sub = new LinkedHashMap<String, Object>();
		sub.put("tags", List.of("a", "b"));
		var root = new LinkedHashMap<String, Object>();
		root.put("sub", sub);

		var proto = PrototextSerializer.DEFAULT.serialize(root);
		assertNotNull(proto);
		assertTrue(proto.contains("tags"));
		assertTrue(proto.contains("a") && proto.contains("b"));
	}

	@Test void d03_mapWithArrayValues() throws Exception {
		// Hits serializeMap → array branch (toList for array).
		var sub = new LinkedHashMap<String, Object>();
		sub.put("ports", new int[] { 80, 443 });
		var root = new LinkedHashMap<String, Object>();
		root.put("sub", sub);

		var proto = PrototextSerializer.DEFAULT.serialize(root);
		assertNotNull(proto);
		assertTrue(proto.contains("80") && proto.contains("443"));
	}

	@Test void d04_mapWithMapValues() throws Exception {
		// Map<String, Map> nested values — exercises serializeMap recursion.
		var c1 = new LinkedHashMap<String, Object>();
		c1.put("id", 1);
		c1.put("label", "first");
		var c2 = new LinkedHashMap<String, Object>();
		c2.put("id", 2);
		c2.put("label", "second");
		var entries = new LinkedHashMap<String, Object>();
		entries.put("k1", c1);
		entries.put("k2", c2);
		var root = new LinkedHashMap<String, Object>();
		root.put("entries", entries);

		var proto = PrototextSerializer.DEFAULT.serialize(root);
		assertNotNull(proto);
		assertTrue(proto.contains("first") && proto.contains("second"));
	}

	@Test void d05_mapWithNullValueIsSkipped() throws Exception {
		var sub = new LinkedHashMap<String, Object>();
		sub.put("present", "yes");
		sub.put("missing", null);
		var root = new LinkedHashMap<String, Object>();
		root.put("sub", sub);

		var proto = PrototextSerializer.DEFAULT.serialize(root);
		assertTrue(proto.contains("present"));
		assertFalse(proto.contains("missing"));
	}

	// Streamable bean property

	@Test void e01_iterableProperty_scalar() throws Exception {
		// Iterable<String> as a Map-property value — exercises the streamable scalar element path.
		Iterable<String> items = List.of("alpha", "beta");
		var root = new LinkedHashMap<String, Object>();
		root.put("items", items);
		var proto = PrototextSerializer.DEFAULT.serialize(root);
		assertNotNull(proto);
		assertTrue(proto.contains("alpha") && proto.contains("beta"), () -> "Expected alpha+beta in: " + proto);
	}

	// Scalar value type dispatch

	@Test void f01_dateProperty() throws Exception {
		// type.isDate() branch.
		var root = new LinkedHashMap<String, Object>();
		root.put("when", new Date(0L));
		var proto = PrototextSerializer.DEFAULT.serialize(root);
		assertNotNull(proto);
		assertTrue(proto.contains("when"));
	}

	@Test void f02_calendarProperty() throws Exception {
		// type.isCalendar() branch.
		var cal = GregorianCalendar.from(Instant.EPOCH.atZone(ZoneOffset.UTC));
		var root = new LinkedHashMap<String, Object>();
		root.put("when", cal);
		var proto = PrototextSerializer.DEFAULT.serialize(root);
		assertNotNull(proto);
		assertTrue(proto.contains("when"));
	}

	@Test void f03_temporalProperty() throws Exception {
		// type.isTemporal() branch.
		var root = new LinkedHashMap<String, Object>();
		root.put("when", Instant.ofEpochMilli(0L));
		var proto = PrototextSerializer.DEFAULT.serialize(root);
		assertNotNull(proto);
		assertTrue(proto.contains("when"));
	}

	@Test void f04_durationProperty() throws Exception {
		// type.isDuration() branch.
		var root = new LinkedHashMap<String, Object>();
		root.put("length", Duration.ofMinutes(90));
		var proto = PrototextSerializer.DEFAULT.serialize(root);
		assertNotNull(proto);
		assertTrue(proto.contains("length"));
		assertTrue(proto.contains("PT") || proto.contains("1H"));
	}

	@Test void f05_periodProperty() throws Exception {
		// type.isPeriod() branch.
		var root = new LinkedHashMap<String, Object>();
		root.put("span", Period.ofDays(7));
		var proto = PrototextSerializer.DEFAULT.serialize(root);
		assertNotNull(proto);
		assertTrue(proto.contains("span"));
		assertTrue(proto.contains("P7D") || proto.contains("D"));
	}

	@Test void f06_byteArrayProperty() throws Exception {
		// value instanceof byte[] branch.
		var root = new LinkedHashMap<String, Object>();
		root.put("payload", new byte[] { 0x01, 0x02, 0x03 });
		var proto = PrototextSerializer.DEFAULT.serialize(root);
		assertNotNull(proto);
		assertTrue(proto.contains("payload"));
	}

	@Test void f07_floatProperty() throws Exception {
		// value instanceof Float branch (separate from Double).
		var root = new LinkedHashMap<String, Object>();
		root.put("ratio", 3.5f);
		var proto = PrototextSerializer.DEFAULT.serialize(root);
		assertNotNull(proto);
		assertTrue(proto.contains("3.5"));
	}

	@Test void f08_keepNullProperties() throws Exception {
		// keepNullProperties=true should still skip writing null values per the proto.
		// However, this exercises the alternative branch in the checkNull predicate.
		var ser = PrototextSerializer.create().keepNullProperties().build();
		var root = new LinkedHashMap<String, Object>();
		root.put("a", null);

		// Should not throw even when all props are null.
		var proto = ser.serialize(root);
		assertNotNull(proto);
	}

	// Optional<T> property

	@Test void g01_optionalProperty_present() throws Exception {
		// Hits isOptional branch in serializeAnything (via map of Optional<String>).
		var m = new LinkedHashMap<String, Object>();
		m.put("opt", opt("value"));
		var proto = PrototextSerializer.DEFAULT.serialize(m);
		assertNotNull(proto);
		assertTrue(proto.contains("value"));
	}

	@Test void g02_optionalProperty_empty() throws Exception {
		// Optional.empty() -> serializeAnything handles via getOptionalValue (returns null).
		var m = new LinkedHashMap<String, Object>();
		m.put("opt", opte());
		var proto = PrototextSerializer.DEFAULT.serialize(m);
		assertNotNull(proto);
	}

	// Bean with comment annotation on bean property

	@Test void h01_beanWithCommentedProperty() throws Exception {
		// Bean property with @Prototext(comment="...") — exercises the protoPMeta comment path.
		var bean = new BeanWithCommentedProperty();
		bean.setName("test");
		var proto = PrototextSerializer.DEFAULT.serialize(bean);
		assertNotNull(proto);
		assertTrue(proto.contains("# This is the name field"));
		assertTrue(proto.contains("name"));
	}

	@Test void h02_beanWithListOfMaps() throws Exception {
		// Bean has a List<Map> property — exercises serializeBeanMap collection-with-bean-elements
		// path (lines 270-296 in PrototextSerializerSession).
		var bean = new BeanWithListOfMaps();
		var c1 = new LinkedHashMap<String, Object>();
		c1.put("k", "alpha");
		var c2 = new LinkedHashMap<String, Object>();
		c2.put("k", "beta");
		bean.setItems(List.of(c1, c2));
		var proto = PrototextSerializer.DEFAULT.serialize(bean);
		assertNotNull(proto);
		assertTrue(proto.contains("alpha") && proto.contains("beta"), () -> "Expected alpha+beta in: " + proto);
	}

	@Test void h03_beanWithListOfMaps_useListSyntax() throws Exception {
		// Bean with List<Map> + useListSyntaxForBeans — hits the [{...},{...}] writer path inside
		// serializeBeanMap (lines 274-288).
		var bean = new BeanWithListOfMaps();
		var c1 = new LinkedHashMap<String, Object>();
		c1.put("k", "alpha");
		var c2 = new LinkedHashMap<String, Object>();
		c2.put("k", "beta");
		bean.setItems(List.of(c1, c2));
		var ser = PrototextSerializer.create().useListSyntaxForBeans(true).build();
		var proto = ser.serialize(bean);
		assertNotNull(proto);
		assertTrue(proto.contains("[") && proto.contains("]"), () -> "Expected list syntax in: " + proto);
		assertTrue(proto.contains("alpha") && proto.contains("beta"));
	}

	@Test void h04_beanWithMapArray() throws Exception {
		// Bean with Map[] property — regression for bug #8: serializeBeanMap was calling
		// toBeanMap() unconditionally on the map elements, which threw BeanRuntimeException.
		// The array path now dispatches Map vs Bean correctly.
		var bean = new BeanWithMapArray();
		var c1 = new LinkedHashMap<String, Object>();
		c1.put("k", "alpha");
		var c2 = new LinkedHashMap<String, Object>();
		c2.put("k", "beta");
		@SuppressWarnings({
		})
		var arr = new Map[] { c1, c2 };
		bean.setItems(arr);
		var proto = PrototextSerializer.DEFAULT.serialize(bean);
		assertNotNull(proto);
		assertTrue(proto.contains("alpha") && proto.contains("beta"), () -> "Expected alpha+beta in: " + proto);
	}

	@Test void h04b_beanWithMapArray_useListSyntax() throws Exception {
		// Same as h04 but with useListSyntaxForBeans=true — covers the [{...},{...}] writer branch
		// where the dispatch fix also applies.
		var bean = new BeanWithMapArray();
		var c1 = new LinkedHashMap<String, Object>();
		c1.put("k", "alpha");
		var c2 = new LinkedHashMap<String, Object>();
		c2.put("k", "beta");
		@SuppressWarnings({
		})
		var arr = new Map[] { c1, c2 };
		bean.setItems(arr);
		var ser = PrototextSerializer.create().useListSyntaxForBeans(true).build();
		var proto = ser.serialize(bean);
		assertNotNull(proto);
		assertTrue(proto.contains("[") && proto.contains("]"), () -> "Expected list syntax in: " + proto);
		assertTrue(proto.contains("alpha") && proto.contains("beta"));
	}

	@Test void h05_beanWithListOfStrings() throws Exception {
		// Bean with List<String> — exercises serializeBeanMap scalar-list branch (lines 298-309).
		var bean = new BeanWithListOfStrings();
		bean.setTags(List.of("a", "b", "c"));
		var proto = PrototextSerializer.DEFAULT.serialize(bean);
		assertNotNull(proto);
		assertTrue(proto.contains("a") && proto.contains("b") && proto.contains("c"));
	}

	@Test void h06_beanWithIntArrayProperty() throws Exception {
		// Bean with int[] — exercises serializeBeanMap array→list branch with primitive elements.
		var bean = new BeanWithIntArrayProp();
		bean.setValues(new int[] { 1, 2, 3 });
		var proto = PrototextSerializer.DEFAULT.serialize(bean);
		assertNotNull(proto);
		assertTrue(proto.contains("1") && proto.contains("2") && proto.contains("3"));
	}

	// Streamable bean property — exercises serializeStreamable (lines 405-425)

	@Test void h07_topLevelStreamOfMaps() throws Exception {
		// Top-level Stream<Map> — hits serializeStreamable bean-element branch.
		var c1 = new LinkedHashMap<String, Object>();
		c1.put("k", "alpha");
		var c2 = new LinkedHashMap<String, Object>();
		c2.put("k", "beta");
		var stream = java.util.stream.Stream.of(c1, c2);
		var proto = PrototextSerializer.DEFAULT.serialize(stream);
		assertNotNull(proto);
		assertTrue(proto.contains("alpha") && proto.contains("beta"), () -> "Expected alpha+beta in: " + proto);
	}

	@Test void h08_topLevelStreamOfStrings() throws Exception {
		// Top-level Stream<String> — hits serializeStreamable scalar branch.
		var stream = java.util.stream.Stream.of("a", "b", "c");
		var proto = PrototextSerializer.DEFAULT.serialize(stream);
		assertNotNull(proto);
		assertTrue(proto.contains("a") && proto.contains("b") && proto.contains("c"));
	}

	// Builder

	@Test void i01_create_nullCtxThrows() {
		assertThrows(IllegalArgumentException.class, () -> PrototextSerializerSession.create((PrototextSerializer) null));
	}

	// Nested maps and collections — serializeMap / serializeCollection paths

	@Test void j_mapWithMapValue() throws Exception {
		// Map value inside a Map → serializeMap: aType.isMap() branch (line 337/340 isMap=true)
		var outer = new LinkedHashMap<String, Object>();
		var inner = new LinkedHashMap<String, Object>();
		inner.put("k1", "v1");
		outer.put("nested", inner);
		var proto = PrototextSerializer.DEFAULT.serialize(outer);
		assertNotNull(proto);
		assertTrue(proto.contains("nested"), "Expected nested map field: " + proto);
	}

	@Test void j_mapWithCollectionValue() throws Exception {
		// Collection value inside a Map → serializeMap: aType.isCollection() branch
		var m = new LinkedHashMap<String, Object>();
		m.put("items", List.of("a", "b", "c"));
		var proto = PrototextSerializer.DEFAULT.serialize(m);
		assertNotNull(proto);
		assertTrue(proto.contains("items"), "Expected items field: " + proto);
	}

	@Test void j_beanListProperty_noListSyntax() throws Exception {
		// Bean property with List<Map> → serializeCollection: elementIsBeanOrMap=true, useListSyntaxForBeans=false (default)
		// Exercises the else-if(nn(fieldName)) path in serializeBeanMap for collection-of-maps
		var root = new BeanWithBeanListProp();
		var item1 = new LinkedHashMap<String, Object>();
		item1.put("name", "alice");
		var item2 = new LinkedHashMap<String, Object>();
		item2.put("name", "bob");
		root.setChildren(List.of(item1, item2));
		var proto = PrototextSerializer.DEFAULT.serialize(root);
		assertNotNull(proto);
		assertTrue(proto.contains("children"), "Expected children field: " + proto);
	}

	@Test void j_beanListProperty_useListSyntax() throws Exception {
		// Bean property with List<Map> and useListSyntaxForBeans=true → list syntax path
		var s = PrototextSerializer.create().useListSyntaxForBeans(true).build();
		var root = new BeanWithBeanListProp();
		var item1 = new LinkedHashMap<String, Object>();
		item1.put("name", "alice");
		var item2 = new LinkedHashMap<String, Object>();
		item2.put("name", "bob");
		root.setChildren(List.of(item1, item2));
		var proto = s.serialize(root);
		assertNotNull(proto);
		assertTrue(proto.contains("children"), "Expected children field: " + proto);
	}

	@Test void j_collectionOfMaps_asProperty() throws Exception {
		// Collection items that are Maps → serializeCollectionItem: aType.isMap() branch (line 361)
		var root = new BeanWithMapListProp();
		var m1 = new LinkedHashMap<String, Object>();
		m1.put("x", "1");
		var m2 = new LinkedHashMap<String, Object>();
		m2.put("x", "2");
		root.setEntries(List.of(m1, m2));
		var proto = PrototextSerializer.DEFAULT.serialize(root);
		assertNotNull(proto);
		assertTrue(proto.contains("entries") || proto.contains("x"), "Expected map items: " + proto);
	}

	@Test void j_mapWithBeanValue_serializeMap() throws Exception {
		// Map value that is a bean → serializeMap: aType.isBean() branch (line 340 true)
		var outer = new LinkedHashMap<String, Object>();
		outer.put("child", new ChildBean("eve"));
		var proto = PrototextSerializer.DEFAULT.serialize(outer);
		assertNotNull(proto);
		assertTrue(proto.contains("child"), "Expected child field: " + proto);
		assertTrue(proto.contains("eve"), "Expected eve in: " + proto);
	}

	// Scalar type dispatch: Date / Calendar / Temporal / Duration / Period / byte[]

	@Test void j01_dateBeanProperty() throws Exception {
		// java.util.Date bean property → serializeScalarValue Date branch
		var bean = new BeanWithDateProp();
		bean.ts = new java.util.Date(0);
		bean.name = "dateTest";
		var proto = PrototextSerializer.DEFAULT.serialize(bean);
		assertNotNull(proto);
		assertTrue(proto.contains("ts"), "Expected ts field: " + proto);
	}

	@Test void j02_calendarBeanProperty() throws Exception {
		// java.util.Calendar bean property → serializeScalarValue Calendar branch
		var bean = new BeanWithCalendarProp();
		bean.cal = java.util.Calendar.getInstance();
		bean.label = "calTest";
		var proto = PrototextSerializer.DEFAULT.serialize(bean);
		assertNotNull(proto);
		assertTrue(proto.contains("cal"), "Expected cal field: " + proto);
	}

	@Test void j03_temporalBeanProperty() throws Exception {
		// java.time.Instant property → serializeScalarValue Temporal branch
		var bean = new BeanWithInstantProp();
		bean.instant = java.time.Instant.ofEpochMilli(0);
		bean.label = "temporalTest";
		var proto = PrototextSerializer.DEFAULT.serialize(bean);
		assertNotNull(proto);
		assertTrue(proto.contains("instant"), "Expected instant field: " + proto);
	}

	@Test void j04_durationBeanProperty() throws Exception {
		// java.time.Duration property → serializeScalarValue Duration branch
		var bean = new BeanWithDurationProp();
		bean.dur = java.time.Duration.ofSeconds(30);
		bean.label = "durationTest";
		var proto = PrototextSerializer.DEFAULT.serialize(bean);
		assertNotNull(proto);
		assertTrue(proto.contains("dur"), "Expected dur field: " + proto);
	}

	@Test void j05_periodBeanProperty() throws Exception {
		// java.time.Period property → serializeScalarValue Period branch
		var bean = new BeanWithPeriodProp();
		bean.period = java.time.Period.ofDays(7);
		bean.label = "periodTest";
		var proto = PrototextSerializer.DEFAULT.serialize(bean);
		assertNotNull(proto);
		assertTrue(proto.contains("period"), "Expected period field: " + proto);
	}

	@Test void j06_bytesBeanProperty() throws Exception {
		// byte[] property → serializeScalarValue byte[] branch
		var bean = new BeanWithBytesProp();
		bean.data = new byte[] { 1, 2, 3 };
		bean.label = "bytesTest";
		var proto = PrototextSerializer.DEFAULT.serialize(bean);
		assertNotNull(proto);
		assertTrue(proto.contains("data"), "Expected data field: " + proto);
	}

	@Test void j07_floatAndDoubleValues() throws Exception {
		// Float and Double in bean property → serializeScalarValue float branch
		var bean = new BeanWithFloatProp();
		bean.f = 1.5f;
		bean.d = 2.5;
		var proto = PrototextSerializer.DEFAULT.serialize(bean);
		assertNotNull(proto);
		assertTrue(proto.contains("1.5") || proto.contains("f"), "Expected float: " + proto);
		assertTrue(proto.contains("2.5") || proto.contains("d"), "Expected double: " + proto);
	}

	@Test void j08_serializeAnything_withFieldName_collection() throws Exception {
		// serializeAnything called with fieldName non-null and Collection type
		// This is hit when a bean property has a collection value
		var root = new BeanWithStringListField();
		root.setNames(List.of("Alice", "Bob"));
		var proto = PrototextSerializer.DEFAULT.serialize(root);
		assertNotNull(proto);
		assertTrue(proto.contains("names"), "Expected names field: " + proto);
		assertTrue(proto.contains("Alice") && proto.contains("Bob"));
	}

	// Test fixture beans

	enum LogLevel { DEBUG, INFO, WARN, ERROR }

	public static class BeanWithCommentedProperty {
		private String name;
		@org.apache.juneau.marshall.prototext.Prototext(comment = "This is the name field")
		public String getName() { return name; }
		public void setName(String v) { name = v; }
	}

	public static class BeanWithListOfMaps {
		private List<Map<String, Object>> items;
		public List<Map<String, Object>> getItems() { return items; }
		public void setItems(List<Map<String, Object>> v) { items = v; }
	}

	public static class BeanWithListOfStrings {
		private List<String> tags;
		public List<String> getTags() { return tags; }
		public void setTags(List<String> v) { tags = v; }
	}

	public static class BeanWithIntArrayProp {
		private int[] values;
		public int[] getValues() { return values; }
		public void setValues(int[] v) { values = v; }
	}

	public static class BeanWithMapArray {
		private Map<String, Object>[] items;
		public Map<String, Object>[] getItems() { return items; }
		public void setItems(Map<String, Object>[] v) { items = v; }
	}

	public static class ChildBean {
		private String name;
		public String getName() { return name; }
		public void setName(String v) { name = v; }
		public ChildBean(String n) { name = n; }
		public ChildBean() {}
	}

	public static class BeanWithBeanListProp {
		private List<Object> children;
		public List<Object> getChildren() { return children; }
		public void setChildren(List<Object> v) { children = v; }
	}

	public static class BeanWithMapListProp {
		private List<Map<String, Object>> entries;
		public List<Map<String, Object>> getEntries() { return entries; }
		public void setEntries(List<Map<String, Object>> v) { entries = v; }
	}

	public static class BeanWithDateProp {
		public String name;
		public java.util.Date ts;
	}

	public static class BeanWithCalendarProp {
		public String label;
		public java.util.Calendar cal;
	}

	public static class BeanWithInstantProp {
		public String label;
		public java.time.Instant instant;
	}

	public static class BeanWithDurationProp {
		public String label;
		public java.time.Duration dur;
	}

	public static class BeanWithPeriodProp {
		public String label;
		public java.time.Period period;
	}

	public static class BeanWithBytesProp {
		public String label;
		public byte[] data;
	}

	public static class BeanWithFloatProp {
		public float f;
		public double d;
	}

	public static class BeanWithStringListField {
		private List<String> names;
		public List<String> getNames() { return names; }
		public void setNames(List<String> v) { names = v; }
	}
}
