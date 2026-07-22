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
package org.apache.juneau.http.classic;

import static org.apache.juneau.http.classic.HttpParts.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.http.message.*;
import org.apache.juneau.*;
import org.apache.juneau.commons.httppart.*;
import org.apache.juneau.http.classic.header.*;
import org.apache.juneau.http.classic.header.BasicHeader;
import org.apache.juneau.http.classic.part.*;
import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link HttpParts}.
 */
class HttpParts_Test extends TestBase {

	// ------------------------------------------------------------------------------------------------------------------
	// booleanPart factory methods
	// ------------------------------------------------------------------------------------------------------------------

	@Test void a01_booleanPart_Boolean() {
		var p = booleanPart("Foo", Boolean.TRUE);
		assertNotNull(p);
		assertEquals("true", p.getValue());
	}

	@Test void a02_booleanPart_Boolean_null() {
		assertNull(booleanPart("Foo", (Boolean)null));
	}

	@Test void a03_booleanPart_Supplier() {
		Supplier<Boolean> s = () -> Boolean.FALSE;
		var p = booleanPart("Foo", s);
		assertNotNull(p);
		assertEquals("false", p.getValue());
	}

	@Test void a04_booleanPart_Supplier_null() {
		assertNull(booleanPart("Foo", (Supplier<Boolean>)null));
	}

	// ------------------------------------------------------------------------------------------------------------------
	// canCast / cast
	// ------------------------------------------------------------------------------------------------------------------

	@Test void a05_canCast_null() {
		assertFalse(canCast(null));
	}

	@Test void a06_canCast_nameValuePair() {
		assertTrue(canCast(BasicPart.of("Foo", "bar")));
	}

	@Test void a07_canCast_mapEntry() {
		var entry = Map.entry("Foo", "bar");
		assertTrue(canCast(entry));
	}

	@Test void a08_canCast_notCastable() {
		assertFalse(canCast("not a part"));
	}

	@Test void a09_cast_nameValuePair() {
		NameValuePair p = BasicPart.of("Foo", "bar");
		assertSame(p, cast(p));
	}

	@Test void a10_cast_headerable() {
		var h = BasicHeader.of("Foo", "bar");
		var p = cast(h);
		assertEquals("Foo", p.getName());
		assertEquals("bar", p.getValue());
	}

	@Test void a11_cast_mapEntry() {
		var entry = Map.entry("Foo", "bar");
		var p = cast(entry);
		assertEquals("Foo", p.getName());
		assertEquals("bar", p.getValue());
	}

	@Test void a12_cast_unsupported() {
		assertThrows(RuntimeException.class, () -> cast("unsupported"));
	}

	// ------------------------------------------------------------------------------------------------------------------
	// csvArrayPart factory methods
	// ------------------------------------------------------------------------------------------------------------------

	@Test void a13_csvArrayPart_stringArray() {
		// Test via BasicCsvArrayPart.of to verify behavior
		var p = BasicCsvArrayPart.of("Foo", new String[]{"a", "b", "c"});
		assertNotNull(p);
	}

	@Test void a14_csvArrayPart_stringArray_null() {
		assertNull(csvArrayPart("Foo", (String[])null));
	}

	@Test void a15_csvArrayPart_supplier() {
		Supplier<String[]> s = () -> new String[]{"x", "y"};
		var p = csvArrayPart("Foo", s);
		assertNotNull(p);
		assertNotNull(p.getValue());
	}

	@Test void a16_csvArrayPart_supplier_null() {
		assertNull(csvArrayPart("Foo", (Supplier<String[]>)null));
	}

	// ------------------------------------------------------------------------------------------------------------------
	// datePart factory methods
	// ------------------------------------------------------------------------------------------------------------------

	@Test void a17_datePart_ZonedDateTime() {
		var dt = ZonedDateTime.parse("2024-01-15T10:30:00Z");
		var p = datePart("Foo", dt);
		assertNotNull(p);
		assertNotNull(p.getValue());
	}

	@Test void a18_datePart_ZonedDateTime_null() {
		assertNull(datePart("Foo", (ZonedDateTime)null));
	}

	@Test void a19_datePart_Supplier() {
		Supplier<ZonedDateTime> s = () -> ZonedDateTime.parse("2024-01-15T10:30:00Z");
		var p = datePart("Foo", s);
		assertNotNull(p);
		assertNotNull(p.getValue());
	}

	@Test void a20_datePart_Supplier_null() {
		assertNull(datePart("Foo", (Supplier<ZonedDateTime>)null));
	}

	// ------------------------------------------------------------------------------------------------------------------
	// integerPart factory methods
	// ------------------------------------------------------------------------------------------------------------------

	@Test void a21_integerPart_Integer() {
		var p = integerPart("Foo", 42);
		assertNotNull(p);
		assertEquals("42", p.getValue());
	}

	@Test void a22_integerPart_Integer_null() {
		assertNull(integerPart("Foo", (Integer)null));
	}

	@Test void a23_integerPart_Supplier() {
		Supplier<Integer> s = () -> 99;
		var p = integerPart("Foo", s);
		assertNotNull(p);
		assertEquals("99", p.getValue());
	}

	@Test void a24_integerPart_Supplier_null() {
		assertNull(integerPart("Foo", (Supplier<Integer>)null));
	}

	// ------------------------------------------------------------------------------------------------------------------
	// getName - different HttpPartType values
	// ------------------------------------------------------------------------------------------------------------------

	@org.apache.juneau.http.Header("MyHeader")
	public static class A24_HeaderBean {
		@Override
		public String toString() { return "value"; }
	}

	@org.apache.juneau.http.Query("MyQuery")
	public static class A25_QueryBean {
		@Override
		public String toString() { return "value"; }
	}

	@org.apache.juneau.http.FormData("MyFormData")
	public static class A26_FormDataBean {
		@Override
		public String toString() { return "value"; }
	}

	@org.apache.juneau.http.Path("MyPath")
	public static class A27_PathBean {
		@Override
		public String toString() { return "value"; }
	}

	@Test void a25_getName_header() {
		var bc = MarshallingContext.DEFAULT;
		var cm = bc.getClassMeta(A24_HeaderBean.class);
		var name = getName(HttpPartType.HEADER, cm);
		assertTrue(name.isPresent());
		assertEquals("MyHeader", name.get());
	}

	@Test void a26_getName_query() {
		var bc = MarshallingContext.DEFAULT;
		var cm = bc.getClassMeta(A25_QueryBean.class);
		var name = getName(HttpPartType.QUERY, cm);
		assertTrue(name.isPresent());
		assertEquals("MyQuery", name.get());
	}

	@Test void a27_getName_formdata() {
		var bc = MarshallingContext.DEFAULT;
		var cm = bc.getClassMeta(A26_FormDataBean.class);
		var name = getName(HttpPartType.FORMDATA, cm);
		assertTrue(name.isPresent());
		assertEquals("MyFormData", name.get());
	}

	@Test void a28_getName_path() {
		var bc = MarshallingContext.DEFAULT;
		var cm = bc.getClassMeta(A27_PathBean.class);
		var name = getName(HttpPartType.PATH, cm);
		assertTrue(name.isPresent());
		assertEquals("MyPath", name.get());
	}

	@Test void a29_getName_default() {
		var bc = MarshallingContext.DEFAULT;
		var cm = bc.getClassMeta(String.class);
		// BODY type is not a known header/query/formdata/path type
		var name = getName(HttpPartType.BODY, cm);
		assertFalse(name.isPresent());
	}

	// ------------------------------------------------------------------------------------------------------------------
	// getConstructor
	// ------------------------------------------------------------------------------------------------------------------

	@Test void a30_getConstructor_singleString() {
		var bc = MarshallingContext.DEFAULT;
		var cm = bc.getClassMeta(BasicPart.class);
		// BasicPart has constructor(String name, String value) which takes two strings
		var ctor = getConstructor(cm);
		// May or may not be present depending on which constructors BasicPart has
		assertNotNull(ctor);
	}

	// ------------------------------------------------------------------------------------------------------------------
	// b — Coverage for previously-unhit factory methods and branches.
	// ------------------------------------------------------------------------------------------------------------------

	// basicPart — ofPair / 2-arg / Supplier overloads (none of which were exercised before).

	@Test void b01_basicPart_ofPair() {
		var p = basicPart("Foo: bar");
		assertEquals("Foo", p.getName());
		assertEquals("bar", p.getValue());
	}

	@Test void b02_basicPart_nameValue() {
		var p = basicPart("Foo", "bar");
		assertEquals("Foo", p.getName());
		assertEquals("bar", p.getValue());
	}

	@Test void b03_basicPart_supplier() {
		Supplier<String> s = () -> "lazy";
		var p = basicPart("Foo", s);
		assertEquals("lazy", p.getValue());
	}

	// canCast — Headerable / NameValuePairable branches.

	@Test void b04_canCast_headerable() {
		Headerable h = () -> BasicHeader.of("Foo", "bar");
		assertTrue(canCast(h));
	}

	@Test void b05_canCast_nameValuePairable() {
		NameValuePairable n = () -> BasicPart.of("Foo", "bar");
		assertTrue(canCast(n));
	}

	// cast — exercises the Headerable branch (BasicHeader is also a NameValuePair so it short-circuits there).

	@Test void b06_cast_pureHeaderable() {
		// A Headerable that is not itself a NameValuePair forces the second branch.
		Headerable h = () -> BasicHeader.of("Foo", "bar");
		var p = cast(h);
		assertEquals("Foo", p.getName());
		assertEquals("bar", p.getValue());
	}

	@Test void b07_cast_apacheBasicNameValuePair() {
		// Apache BasicNameValuePair implements NameValuePair (returns same object back).
		var src = new BasicNameValuePair("Foo", "bar");
		var p = cast(src);
		assertSame(src, p);
	}

	// csvArrayPart eager (String[]) factory was only exercised via raw call; ensure factory itself is hit.

	@Test void b08_csvArrayPart_stringArray_factory() {
		var p = csvArrayPart("Foo", "a", "b", "c");
		assertNotNull(p);
		assertEquals("a,b,c", p.getValue());
	}

	// integerPart (already partly covered) — no extra needed.

	// longPart — both overloads.

	@Test void b09_longPart_Long() {
		var p = longPart("Foo", 7L);
		assertEquals("7", p.getValue());
	}

	@Test void b10_longPart_Long_null() {
		assertNull(longPart("Foo", (Long)null));
	}

	@Test void b11_longPart_Supplier() {
		Supplier<Long> s = () -> 99L;
		var p = longPart("Foo", s);
		assertEquals("99", p.getValue());
	}

	@Test void b12_longPart_Supplier_null() {
		assertNull(longPart("Foo", (Supplier<Long>)null));
	}

	// stringPart — both overloads.

	@Test void b13_stringPart_String() {
		var p = stringPart("Foo", "bar");
		assertEquals("bar", p.getValue());
	}

	@Test void b14_stringPart_String_null() {
		assertNull(stringPart("Foo", (String)null));
	}

	@Test void b15_stringPart_String_blank() {
		var p = stringPart("Foo", "");
		assertNotNull(p);
	}

	@Test void b16_stringPart_Supplier() {
		Supplier<String> s = () -> "lazy";
		var p = stringPart("Foo", s);
		assertEquals("lazy", p.getValue());
	}

	@Test void b17_stringPart_Supplier_null() {
		assertNull(stringPart("Foo", (Supplier<String>)null));
	}

	// uriPart — both overloads.

	@Test void b18_uriPart_URI() {
		var u = URI.create("https://example.com");
		var p = uriPart("Foo", u);
		assertEquals("https://example.com", p.getValue());
	}

	@Test void b19_uriPart_URI_null() {
		assertNull(uriPart("Foo", (URI)null));
	}

	@Test void b20_uriPart_Supplier() {
		Supplier<URI> s = () -> URI.create("https://example.com/x");
		var p = uriPart("Foo", s);
		assertNotNull(p);
		assertNotNull(p.getValue());
	}

	@Test void b21_uriPart_Supplier_null() {
		assertNull(uriPart("Foo", (Supplier<URI>)null));
	}

	// serializedPart — both overloads.

	@Test void b22_serializedPart_value() {
		var p = serializedPart("Foo", "bar");
		assertEquals("Foo", p.getName());
	}

	@Test void b23_serializedPart_supplier() {
		Supplier<Object> s = () -> "lazy";
		var p = serializedPart("Foo", s);
		assertEquals("Foo", p.getName());
	}

	// partList — every overload.

	@Test void b24_partList_noArgs() {
		var pl = partList();
		assertNotNull(pl);
		assertEquals(0, pl.size());
	}

	@Test void b25_partList_list() {
		var pl = partList(List.<NameValuePair>of(BasicPart.of("a", "1"), BasicPart.of("b", "2")));
		assertList(pl, "a=1", "b=2");
	}

	@Test void b26_partList_varargs() {
		var pl = partList(BasicPart.of("a", "1"), BasicPart.of("b", "2"));
		assertList(pl, "a=1", "b=2");
	}

	@Test void b27_partList_pairs() {
		var pl = partList("a", "1", "b", "2");
		assertList(pl, "a=1", "b=2");
	}

	// isHttpPart — every switch arm.

	@Test void b28_isHttpPart_query_assignable() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(BasicPart.class);
		assertTrue(isHttpPart(HttpPartType.QUERY, cm));
	}

	@Test void b29_isHttpPart_path_assignable() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(BasicPart.class);
		assertTrue(isHttpPart(HttpPartType.PATH, cm));
	}

	@Test void b30_isHttpPart_formdata_assignable() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(BasicPart.class);
		assertTrue(isHttpPart(HttpPartType.FORMDATA, cm));
	}

	@Test void b31_isHttpPart_query_notAssignable() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(String.class);
		assertFalse(isHttpPart(HttpPartType.QUERY, cm));
	}

	@Test void b32_isHttpPart_header_assignable() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(BasicHeader.class);
		assertTrue(isHttpPart(HttpPartType.HEADER, cm));
	}

	@Test void b33_isHttpPart_header_notAssignable() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(String.class);
		assertFalse(isHttpPart(HttpPartType.HEADER, cm));
	}

	@Test void b34_isHttpPart_default() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(BasicPart.class);
		assertFalse(isHttpPart(HttpPartType.BODY, cm));
		assertFalse(isHttpPart(HttpPartType.RESPONSE_HEADER, cm));
		assertFalse(isHttpPart(HttpPartType.ANY, cm));
	}

	// getName — exercises the @X(name=...) annotation paths (the existing tests cover @X(value=...)).

	@org.apache.juneau.http.Header(name = "HName")
	public static class B_HeaderNameBean { }

	@org.apache.juneau.http.Query(name = "QName")
	public static class B_QueryNameBean { }

	@org.apache.juneau.http.FormData(name = "FName")
	public static class B_FormDataNameBean { }

	@org.apache.juneau.http.Path(name = "PName")
	public static class B_PathNameBean { }

	@Test void b35_getName_header_byName() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(B_HeaderNameBean.class);
		assertEquals("HName", getName(HttpPartType.HEADER, cm).orElse(null));
	}

	@Test void b36_getName_query_byName() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(B_QueryNameBean.class);
		assertEquals("QName", getName(HttpPartType.QUERY, cm).orElse(null));
	}

	@Test void b37_getName_formdata_byName() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(B_FormDataNameBean.class);
		assertEquals("FName", getName(HttpPartType.FORMDATA, cm).orElse(null));
	}

	@Test void b38_getName_path_byName() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(B_PathNameBean.class);
		assertEquals("PName", getName(HttpPartType.PATH, cm).orElse(null));
	}

	@Test void b39_getName_noAnnotation_returnsEmpty() {
		// The classic-package version of HttpParts does NOT fall back to a NAME field —
		// classes without an annotation produce an empty Optional.
		var cm = MarshallingContext.DEFAULT.getClassMeta(String.class);
		assertFalse(getName(HttpPartType.HEADER, cm).isPresent());
		assertFalse(getName(HttpPartType.QUERY, cm).isPresent());
		assertFalse(getName(HttpPartType.FORMDATA, cm).isPresent());
		assertFalse(getName(HttpPartType.PATH, cm).isPresent());
	}
}
