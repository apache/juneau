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
package org.apache.juneau.marshall.oapi;

import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.marshall.httppart.HttpPartSchema.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.httppart.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.marshall.serializer.*;
import org.junit.jupiter.api.*;

/**
 * Targeted coverage tests for {@link OpenApiSerializerSession} focusing on gaps not exercised by
 * the broader round-trip suite in {@link OpenApi_Test} -- swap dispatch, URI resolution, streamable
 * values, UONC collection-format delegation (toList/toMap/toObject), bean-vs-map OBJECT dispatch,
 * FILE/invalid-object error paths, and toType()'s conversion-failure wrapping.
 */
@SuppressWarnings({
	"java:S5976", // Separate test methods preferred over parameterized for clarity and independent failure reporting.
})
class OpenApiSerializerSession_Test extends TestBase {

	private static final OpenApiSerializer DS = OpenApiSerializer.DEFAULT;

	private static String w(HttpPartSchema schema, Object value) throws Exception {
		return DS.write(null, schema, value);
	}

	// ============================================================
	// a01-a03: swap dispatch (write()'s "Swap if necessary" block)
	// ============================================================

	enum A_Enum { FOO, BAR }

	@Test
	void a01_swapOnNonDateType() throws Exception {
		// nn(swap) && !isDateOrCalendarOrTemporal() -- an enum has a default swap and is not a
		// date/calendar/temporal type, so the swap fires and re-resolves the class meta.
		var s = w(T_STRING, A_Enum.FOO);
		assertEquals("FOO", s);
	}

	public static class A_ObjSwapBean {
		public String x = "hi";
	}

	public static class A_ObjSwap extends org.apache.juneau.marshall.swap.ObjectSwap<A_ObjSwapBean,Object> {
		@Override
		public Object swap(MarshallingSession session, A_ObjSwapBean o) {
			return o.x;
		}
	}

	@Test
	void a02_swapClassMetaResolvesToObject() throws Exception {
		// After swapping, swap.getSwapClassMeta(this).isObject() == true forces a second
		// getClassMetaForObject(value) lookup to discover the swapped value's real runtime type.
		var ctx = OpenApiSerializer.create().swaps(A_ObjSwap.class).build();
		var s = ctx.write(null, T_STRING, new A_ObjSwapBean());
		assertEquals("hi", s);
	}

	// ============================================================
	// b01-b02: URI resolution
	// ============================================================

	@Test
	void b01_uriTypeResolved() throws Exception {
		// type.isUri() branch: value is resolved via getUriResolver() and re-typed as string().
		var s = w(T_STRING, java.net.URI.create("http://foo"));
		assertEquals("http://foo", s);
	}

	// ============================================================
	// c01-c02: ARRAY branch -- streamable values and UONC delegation
	// ============================================================

	@Test
	void c01_arrayStreamableValue() throws Exception {
		// type.isStreamable() branch in the plain (non-UONC) ARRAY handler.
		var s = w(T_ARRAY, Stream.of("a", "b"));
		assertEquals("a,b", s);
	}

	@Test
	void c02_arrayUoncDelegatesToList() throws Exception {
		// cf == UONC branch: delegates to super.write() with a JsonList built by toList().
		var ps = tArray().collectionFormat(HttpPartCollectionFormat.UONC).build();
		var s = w(ps, List.of("a", "b"));
		assertEquals("@(a,b)", s);
	}

	@Test
	void c03_arrayUoncStreamableDelegatesViaToList() throws Exception {
		// toList()'s own type.isStreamable() branch (distinct from write()'s plain-ARRAY streamable
		// check in c01 -- this one is reached only through the UONC dispatch).
		var ps = tArray().collectionFormat(HttpPartCollectionFormat.UONC).build();
		var s = w(ps, Stream.of("a", "b"));
		assertEquals("@(a,b)", s);
	}

	@Test
	void c04_arrayUoncScalarFallsThroughToListElse() throws Exception {
		// toList()'s final else-branch: value is neither array, collection, nor streamable, so it's
		// wrapped as a single-element list via toObject().
		var ps = tArray().collectionFormat(HttpPartCollectionFormat.UONC).build();
		var s = w(ps, "justAString");
		assertEquals("@(justAString)", s);
	}

	// ============================================================
	// d01-d04: OBJECT branch -- UONC delegation, bean vs map, invalid type
	// ============================================================

	@Test
	void d01_objectUoncWithPropertiesAndMapValue() throws Exception {
		// cf == UONC branch with schema.hasProperties() && type.isMapOrBean() -- routes the map
		// through toMap() before delegating to super.write().
		var ps = tObject().p("a", tString()).collectionFormat(HttpPartCollectionFormat.UONC).build();
		var s = w(ps, Map.of("a", "b"));
		assertEquals("(a=b)", s);
	}

	public static class D02_Bean {
		public String a = "x";
	}

	@Test
	void d02_objectUoncWithPropertiesAndBeanValue() throws Exception {
		// Same UONC+properties path as d01, but toMap()'s type.isBean() branch fires instead of its
		// plain-Map else-branch.
		var ps = tObject().p("a", tString()).collectionFormat(HttpPartCollectionFormat.UONC).build();
		var s = w(ps, new D02_Bean());
		assertEquals("(a=x)", s);
	}

	public static class D03_ThrowBean {
		public String a = "ok";
		public String getBad() { throw new RuntimeException("boom"); }
	}

	@Test
	void d03_beanPropertyThrowsIsSkipped() throws Exception {
		// type.isBean() branch's forEachValue callback: "if (thrown == null)" false-arm -- a
		// property whose getter throws is silently excluded from the output instead of propagating.
		var s = w(T_OBJECT, new D03_ThrowBean());
		assertEquals("a=ok", s);
	}

	@Test
	void d04_objectTypeNeitherMapNorBeanThrows() throws Exception {
		// Final else-branch: schema forces OBJECT type but the value is a plain String (neither
		// isBean() nor isMap()).
		assertThrowsWithMessage(SerializeException.class, "Input is not a valid object type", () -> w(T_OBJECT, "hello"));
	}

	// ============================================================
	// e01: FILE type
	// ============================================================

	@Test
	void e01_fileTypeThrows() {
		// t == FILE branch: file parts are explicitly unsupported for serialization.
		assertThrowsWithMessage(SerializeException.class, "File part not supported", () -> w(T_FILE, "x"));
	}

	// ============================================================
	// f01-f03: toObject() nested-value branches (reached via UONC array/object items)
	// ============================================================

	@Test
	void f01_nestedArrayItemStringByteFormat() throws Exception {
		// toObject()'s STRING/BYTE branch, reached while converting each element of a UONC array.
		var ps = tArrayUon().items(tString().fByte()).build();
		var s = w(ps, List.of("ab", "cd"));
		assertEquals("@('YWI=','Y2Q=')", s);
	}

	@Test
	void f02_nestedArrayItemArrayCsvJoin() throws Exception {
		// toObject()'s ARRAY branch with cf == CSV, joining the nested list into a single string
		// instead of returning the raw JsonList.
		var ps = tArrayUon().items(tArrayCsv()).build();
		var s = w(ps, List.of(List.of("a", "b")));
		assertEquals("@('a,b')", s);
	}

	@Test
	void f03_nestedArrayItemObject() throws Exception {
		// toObject()'s OBJECT branch, delegating to toMap() for a nested map element.
		var ps = tArrayUon().items(tObject()).build();
		var s = w(ps, List.of(Map.of("k", "v")));
		assertEquals("@((k=v))", s);
	}

	// ============================================================
	// h01-h02: OapiStringBuilder.append(key,val) collection-format dispatch
	// ============================================================

	@Test
	void h01_objectPipesFormat() throws Exception {
		// append(Object,Object)'s "cf == PIPES" branch -- g01a/b (OpenApi_Test) only exercise the
		// default (comma) collection format for OBJECT-typed key=value output.
		var ps = tObject().collectionFormat(HttpPartCollectionFormat.PIPES).build();
		var m = new java.util.LinkedHashMap<String,Object>();
		m.put("a", "1");
		m.put("b", "2");
		var s = w(ps, m);
		assertEquals("a=1|b=2", s);
	}

	@Test
	void h02_objectSsvFormat() throws Exception {
		// append(Object,Object)'s "cf == SSV || cf == TSV" branch.
		var ps = tObject().collectionFormat(HttpPartCollectionFormat.SSV).build();
		var m = new java.util.LinkedHashMap<String,Object>();
		m.put("a", "1");
		m.put("b", "2");
		var s = w(ps, m);
		assertEquals("a=1 b=2", s);
	}

	// ============================================================
	// g01: toType()'s InvalidDataConversionException wrapping
	// ============================================================

	@Test
	void g01_unconvertibleValueWrapsAsSerializeException() {
		// toType()'s catch (InvalidDataConversionException e) -> throw new SerializeException(e).
		assertThrowsWithMessage(SerializeException.class, "Invalid data conversion", () -> w(T_BYTE, new Object()));
	}

	// ============================================================
	// i01-i06: additional gap-targeting tests
	// ============================================================

	@Test
	void i01_objectTsvFormat() throws Exception {
		// append(Object,Object)'s "cf == SSV || cf == TSV" branch, TSV arm specifically -- h02 only
		// exercises SSV for this two-arg overload.
		var ps = tObject().collectionFormat(HttpPartCollectionFormat.TSV).build();
		var m = new java.util.LinkedHashMap<String,Object>();
		m.put("a", "1");
		m.put("b", "2");
		var s = w(ps, m);
		assertEquals("a=1\tb=2", s);
	}

	@Test
	void i02_swapSkippedForDateOrCalendarType() throws Exception {
		// nn(swap) && !isDateOrCalendarOrTemporal()'s false-arm: java.util.Date has a registered
		// default swap, but isDateOrCalendarOrTemporal() is true for it, so the swap is
		// deliberately skipped and the DATE_TIME formatting path handles it directly instead.
		var ps = tString().format(org.apache.juneau.commons.httppart.HttpPartFormat.DATE_TIME).build();
		var s = w(ps, new java.util.Date(0));
		assertNotNull(s);
	}

	@Test
	void i04_uriTypeResolvedWithoutParsedTypeMutation() throws Exception {
		// type.isUri() true-arm reached without the parsed-type mutater (line 217) first converting
		// `type` away from URI: DEFAULT_SCHEMA's parsed type doesn't mutate a URI value, so it's
		// still classified isUri() when reaching the dedicated URI-resolution branch (distinct from
		// b01, whose T_STRING schema's parsed type -- String -- causes the mutater branch to fire
		// first and pre-convert the value, bypassing this branch entirely).
		var s = w(null, java.net.URI.create("http://foo"));
		assertEquals("http://foo", s);
	}

	@Test
	void i05_uoncObjectWithoutPropertiesSkipsToMapConversion() throws Exception {
		// cf == UONC branch's "schema.hasProperties() && type.isMapOrBean()" false-arm: a UONC
		// schema with no declared properties passes the raw map straight to super.write() instead
		// of routing it through toMap() first (d01/d02 both declare properties).
		var ps = tObject().collectionFormat(HttpPartCollectionFormat.UONC).build();
		var s = w(ps, Map.of("a", "b"));
		assertEquals("(a=b)", s);
	}

	public static class I06_Bean {
		public String a = "x";
		public String b;
	}

	@Test
	void i06_keepNullPropertiesIncludesNullBeanValue() throws Exception {
		// The OBJECT/isBean() branch's checkNull predicate: "isKeepNullProperties()" true-arm --
		// with the option enabled, a null-valued property is still included in the output (d03/the
		// default session only exercise the "nn(x)" true-arm via non-null values).
		var ctx = OpenApiSerializer.create().keepNullProperties().build();
		var s = ctx.write(null, T_OBJECT, new I06_Bean());
		assertTrue(s.contains("a=x"));
		assertTrue(s.contains("b="));
	}

	@Test
	void i07_keepNullPropertiesIncludesNullMapValueViaUonc() throws Exception {
		// toMap()'s isBean() branch's checkNull predicate: "isKeepNullProperties()" true-arm,
		// reached via the UONC+properties dispatch (m17/d02 only exercise non-null bean values).
		var ctx = OpenApiSerializer.create().keepNullProperties().build();
		var ps = tObject().p("a", tString()).p("b", tString()).collectionFormat(HttpPartCollectionFormat.UONC).build();
		var s = ctx.write(null, ps, new I06_Bean());
		assertEquals("(a=x,b=null)", s);
	}

	@Test
	void i08_toMapBeanPropertyThrowsIsSkipped() throws Exception {
		// toMap()'s isBean() branch forEachValue callback: "if (thrown == null)" false-arm -- a
		// property whose getter throws is silently excluded, reached via the UONC+properties
		// dispatch (d03 exercises the equivalent branch in write()'s own OBJECT/isBean() handler,
		// not toMap()'s).
		var ps = tObject().p("a", tString()).collectionFormat(HttpPartCollectionFormat.UONC).build();
		var s = w(ps, new D03_ThrowBean());
		assertEquals("(a=ok)", s);
	}

	@Test
	void i09_sortedCollectionUoncArray() throws Exception {
		// toList()'s "isSortCollections()" true-arm -- unsorted input is re-sorted before being
		// wrapped for the UONC array delegation.
		var ctx = OpenApiSerializer.create().sortCollections().build();
		var ps = tArray().collectionFormat(HttpPartCollectionFormat.UONC).build();
		var s = ctx.write(null, ps, List.of("b", "a", "c"));
		assertEquals("@(a,b,c)", s);
	}

	@Test
	void i10_sortedMapUoncObject() throws Exception {
		// toMap()'s "isSortMaps()" true-arm -- unsorted input is re-sorted before being wrapped
		// for the UONC object delegation.
		var ctx = OpenApiSerializer.create().sortMaps().build();
		var ps = tObject().p("b", tString()).p("a", tString()).collectionFormat(HttpPartCollectionFormat.UONC).build();
		var m = new java.util.LinkedHashMap<String,Object>();
		m.put("b", "2");
		m.put("a", "1");
		var s = ctx.write(null, ps, m);
		assertEquals("(a='1',b='2')", s);
	}

	@Test
	void i11a_nestedArrayItemStringDateFormat() throws Exception {
		// toObject()'s STRING/DATE branch (as opposed to i11's DATE_TIME), reached while converting
		// each element of a UONC array.
		var ps = tArrayUon().items(tString().format(org.apache.juneau.commons.httppart.HttpPartFormat.DATE)).build();
		var s = w(ps, List.of(new java.util.Date(0)));
		assertNotNull(s);
	}

	@Test
	void i11_nestedArrayItemStringDateTimeFormat() throws Exception {
		// toObject()'s STRING/DATE_TIME branch, reached while converting each element of a UONC
		// array (f01 only exercises the BYTE format for this same dispatch point).
		var ps = tArrayUon().items(tString().format(org.apache.juneau.commons.httppart.HttpPartFormat.DATE_TIME)).build();
		var s = w(ps, List.of(new java.util.Date(0)));
		assertNotNull(s);
	}

	@Test
	void i12_nestedArrayItemArrayPipesJoin() throws Exception {
		// toObject()'s ARRAY branch with cf == PIPES, joining the nested list into a single
		// pipe-delimited string (f02 only exercises CSV for this same dispatch point).
		var ps = tArrayUon().items(tArrayPipes()).build();
		var s = w(ps, List.of(List.of("a", "b")));
		assertEquals("@(a|b)", s);
	}

	@Test
	void i13_nestedArrayItemArraySsvJoin() throws Exception {
		// toObject()'s ARRAY branch with cf == SSV, joining the nested list with spaces.
		var ps = tArrayUon().items(tArraySsv()).build();
		var s = w(ps, List.of(List.of("a", "b")));
		assertEquals("@('a b')", s);
	}

	@Test
	void i14_nestedArrayItemArrayTsvJoin() throws Exception {
		// toObject()'s ARRAY branch with cf == TSV, joining the nested list with tabs.
		var ps = tArrayUon().items(tArrayTsv()).build();
		var s = w(ps, List.of(List.of("a", "b")));
		assertEquals("@('a\tb')", s);
	}
}
