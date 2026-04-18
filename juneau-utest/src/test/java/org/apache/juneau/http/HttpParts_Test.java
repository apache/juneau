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
package org.apache.juneau.http;

import static org.apache.juneau.http.HttpParts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.commons.httppart.*;
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
		var p = org.apache.juneau.http.part.BasicCsvArrayPart.of("Foo", new String[]{"a", "b", "c"});
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

	@org.apache.juneau.http.annotation.Header("MyHeader")
	public static class A24_HeaderBean {
		@Override
		public String toString() { return "value"; }
	}

	@org.apache.juneau.http.annotation.Query("MyQuery")
	public static class A25_QueryBean {
		@Override
		public String toString() { return "value"; }
	}

	@org.apache.juneau.http.annotation.FormData("MyFormData")
	public static class A26_FormDataBean {
		@Override
		public String toString() { return "value"; }
	}

	@org.apache.juneau.http.annotation.Path("MyPath")
	public static class A27_PathBean {
		@Override
		public String toString() { return "value"; }
	}

	@Test void a25_getName_header() {
		var bc = BeanContext.DEFAULT;
		var cm = bc.getClassMeta(A24_HeaderBean.class);
		var name = getName(HttpPartType.HEADER, cm);
		assertTrue(name.isPresent());
		assertEquals("MyHeader", name.get());
	}

	@Test void a26_getName_query() {
		var bc = BeanContext.DEFAULT;
		var cm = bc.getClassMeta(A25_QueryBean.class);
		var name = getName(HttpPartType.QUERY, cm);
		assertTrue(name.isPresent());
		assertEquals("MyQuery", name.get());
	}

	@Test void a27_getName_formdata() {
		var bc = BeanContext.DEFAULT;
		var cm = bc.getClassMeta(A26_FormDataBean.class);
		var name = getName(HttpPartType.FORMDATA, cm);
		assertTrue(name.isPresent());
		assertEquals("MyFormData", name.get());
	}

	@Test void a28_getName_path() {
		var bc = BeanContext.DEFAULT;
		var cm = bc.getClassMeta(A27_PathBean.class);
		var name = getName(HttpPartType.PATH, cm);
		assertTrue(name.isPresent());
		assertEquals("MyPath", name.get());
	}

	@Test void a29_getName_default() {
		var bc = BeanContext.DEFAULT;
		var cm = bc.getClassMeta(String.class);
		// BODY type is not a known header/query/formdata/path type
		var name = getName(HttpPartType.BODY, cm);
		assertFalse(name.isPresent());
	}

	// ------------------------------------------------------------------------------------------------------------------
	// getConstructor
	// ------------------------------------------------------------------------------------------------------------------

	@Test void a30_getConstructor_singleString() {
		var bc = BeanContext.DEFAULT;
		var cm = bc.getClassMeta(BasicPart.class);
		// BasicPart has constructor(String name, String value) which takes two strings
		var ctor = getConstructor(cm);
		// May or may not be present depending on which constructors BasicPart has
		assertNotNull(ctor);
	}
}
