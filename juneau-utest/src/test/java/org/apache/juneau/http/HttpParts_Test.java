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

import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.httppart.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-focused tests for {@link HttpParts} (the next-gen package
 * {@code org.apache.juneau.http}).  Targets uncovered branches in
 * {@code isHttpPart}, {@code getName}, {@code getConstructor}, and the
 * private {@code readPublicStaticStringField} helper.
 */
class HttpParts_Test extends TestBase {

	// ------------------------------------------------------------------------------------------------------------------
	// part(String, String) / part(String, Supplier<String>) factories
	// ------------------------------------------------------------------------------------------------------------------

	@Test void a01_part_eagerValue() {
		var p = part("k", "v");
		assertEquals("k", p.getName());
		assertEquals("v", p.getValue());
	}

	@Test void a02_part_nullValue() {
		var p = part("k", (String)null);
		assertNull(p.getValue());
	}

	@Test void a03_part_emptyValue() {
		var p = part("k", "");
		assertEquals("", p.getValue());
	}

	@Test void a04_part_blankValue() {
		var p = part("k", "   ");
		assertEquals("   ", p.getValue());
	}

	@Test void a05_part_lazyValue() {
		Supplier<String> s = () -> "lazy";
		var p = part("k", s);
		assertEquals("lazy", p.getValue());
	}

	@Test void a06_part_lazyValue_returnsNull() {
		Supplier<String> s = () -> null;
		var p = part("k", s);
		assertNull(p.getValue());
	}

	// ------------------------------------------------------------------------------------------------------------------
	// partList factories
	// ------------------------------------------------------------------------------------------------------------------

	@Test void b01_partList_varArgs() {
		var pl = partList(part("a", "1"), part("b", "2"));
		assertEquals(2, pl.size());
	}

	@Test void b02_partList_empty() {
		var pl = partList();
		assertEquals(0, pl.size());
	}

	@Test void b03_partListOfPairs() {
		var pl = partListOfPairs("a", "1", "b", "2");
		assertEquals(2, pl.size());
	}

	@Test void b04_partListOfPairs_oddThrows() {
		assertThrows(IllegalArgumentException.class, () -> partListOfPairs("a", "1", "b"));
	}

	@Test void b05_partListOfPairs_empty() {
		var pl = partListOfPairs();
		assertEquals(0, pl.size());
	}

	// ------------------------------------------------------------------------------------------------------------------
	// getConstructor — type with (String) ctor, (String,String) ctor, no matching ctor
	// ------------------------------------------------------------------------------------------------------------------

	public static class C_StringCtor {
		public C_StringCtor(String v) { /* no-op */ }
	}

	public static class C_NameValueCtor {
		public C_NameValueCtor(String n, String v) { /* no-op */ }
	}

	public static class C_NoMatchingCtor {
		public C_NoMatchingCtor(int v) { /* no-op */ }
	}

	@Test void c01_getConstructor_stringCtor() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(C_StringCtor.class);
		var ctor = getConstructor(cm);
		assertTrue(ctor.isPresent());
	}

	@Test void c02_getConstructor_nameValueCtor() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(C_NameValueCtor.class);
		var ctor = getConstructor(cm);
		// Falls through to the (String,String) branch.
		assertTrue(ctor.isPresent());
	}

	@Test void c03_getConstructor_noMatchingCtor() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(C_NoMatchingCtor.class);
		var ctor = getConstructor(cm);
		assertFalse(ctor.isPresent());
	}

	// ------------------------------------------------------------------------------------------------------------------
	// isHttpPart — exercises every switch arm
	// ------------------------------------------------------------------------------------------------------------------

	public static class D_NotAPart { }

	@Test void d01_isHttpPart_query_assignableToHttpPart() {
		// HttpPartBean implements HttpPart
		var cm = MarshallingContext.DEFAULT.getClassMeta(HttpPartBean.class);
		assertTrue(isHttpPart(HttpPartType.QUERY, cm));
	}

	@Test void d02_isHttpPart_path_assignableToHttpPart() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(HttpPartBean.class);
		assertTrue(isHttpPart(HttpPartType.PATH, cm));
	}

	@Test void d03_isHttpPart_formdata_assignableToHttpPart() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(HttpPartBean.class);
		assertTrue(isHttpPart(HttpPartType.FORMDATA, cm));
	}

	@Test void d04_isHttpPart_query_notAssignable() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(D_NotAPart.class);
		assertFalse(isHttpPart(HttpPartType.QUERY, cm));
	}

	@Test void d05_isHttpPart_header_assignableToHttpHeader() {
		// Accept implements HttpHeader (via HttpMediaRangesHeader).
		var cm = MarshallingContext.DEFAULT.getClassMeta(Accept.class);
		assertTrue(isHttpPart(HttpPartType.HEADER, cm));
	}

	@Test void d06_isHttpPart_header_notAssignable() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(D_NotAPart.class);
		assertFalse(isHttpPart(HttpPartType.HEADER, cm));
	}

	@Test void d07_isHttpPart_default_returnsFalse() {
		// BODY (and any non-PATH/QUERY/FORMDATA/HEADER) hits the default arm.
		var cm = MarshallingContext.DEFAULT.getClassMeta(HttpPartBean.class);
		assertFalse(isHttpPart(HttpPartType.BODY, cm));
		assertFalse(isHttpPart(HttpPartType.RESPONSE_HEADER, cm));
		assertFalse(isHttpPart(HttpPartType.ANY, cm));
	}

	// ------------------------------------------------------------------------------------------------------------------
	// getName — annotation-based and NAME-static-field fallback paths
	// ------------------------------------------------------------------------------------------------------------------

	@org.apache.juneau.http.Header(value = "MyHeader")
	public static class E_HeaderValueBean { }

	@org.apache.juneau.http.Header(name = "MyHeaderName")
	public static class E_HeaderNameBean { }

	@org.apache.juneau.http.Query(value = "MyQuery")
	public static class E_QueryValueBean { }

	@org.apache.juneau.http.Query(name = "MyQueryName")
	public static class E_QueryNameBean { }

	@org.apache.juneau.http.FormData(value = "MyFormData")
	public static class E_FormDataValueBean { }

	@org.apache.juneau.http.FormData(name = "MyFormDataName")
	public static class E_FormDataNameBean { }

	@org.apache.juneau.http.Path(value = "MyPath")
	public static class E_PathValueBean { }

	@org.apache.juneau.http.Path(name = "MyPathName")
	public static class E_PathNameBean { }

	/** No annotation, but a public static String NAME field — exercises readPublicStaticStringField. */
	public static class E_NameField {
		public static final String NAME = "name-from-field";
	}

	/** No annotation, NAME field is non-static — exercises the modifier guard. */
	public static class E_NonStaticName {
		public final String NAME = "instance";
	}

	/** No annotation, NAME field is non-String — exercises the type guard. */
	public static class E_WrongTypeName {
		public static final int NAME = 7;
	}

	/** No annotation and no NAME field — readPublicStaticStringField hits the catch arm. */
	public static class E_NoNameField { }

	/** NAME static String field with null value — exercises the v == null branch. */
	public static class E_NullNameField {
		public static final String NAME = null;
	}

	@Test void e01_getName_header_value() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(E_HeaderValueBean.class);
		assertEquals("MyHeader", getName(HttpPartType.HEADER, cm).orElse(null));
	}

	@Test void e02_getName_header_name() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(E_HeaderNameBean.class);
		assertEquals("MyHeaderName", getName(HttpPartType.HEADER, cm).orElse(null));
	}

	@Test void e03_getName_query_value() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(E_QueryValueBean.class);
		assertEquals("MyQuery", getName(HttpPartType.QUERY, cm).orElse(null));
	}

	@Test void e04_getName_query_name() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(E_QueryNameBean.class);
		assertEquals("MyQueryName", getName(HttpPartType.QUERY, cm).orElse(null));
	}

	@Test void e05_getName_formdata_value() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(E_FormDataValueBean.class);
		assertEquals("MyFormData", getName(HttpPartType.FORMDATA, cm).orElse(null));
	}

	@Test void e06_getName_formdata_name() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(E_FormDataNameBean.class);
		assertEquals("MyFormDataName", getName(HttpPartType.FORMDATA, cm).orElse(null));
	}

	@Test void e07_getName_path_value() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(E_PathValueBean.class);
		assertEquals("MyPath", getName(HttpPartType.PATH, cm).orElse(null));
	}

	@Test void e08_getName_path_name() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(E_PathNameBean.class);
		assertEquals("MyPathName", getName(HttpPartType.PATH, cm).orElse(null));
	}

	@Test void e09_getName_default_arm() {
		// BODY, RESPONSE_HEADER, etc. fall through to opte().
		var cm = MarshallingContext.DEFAULT.getClassMeta(String.class);
		assertFalse(getName(HttpPartType.BODY, cm).isPresent());
		assertFalse(getName(HttpPartType.RESPONSE_HEADER, cm).isPresent());
		assertFalse(getName(HttpPartType.ANY, cm).isPresent());
	}

	@Test void e10_getName_header_fromNameField() {
		// Class with no @Header annotation — falls back to NAME public static String field.
		var cm = MarshallingContext.DEFAULT.getClassMeta(Accept.class);
		assertEquals("Accept", getName(HttpPartType.HEADER, cm).orElse(null));
	}

	@Test void e11_getName_header_noAnnoAndNoField_returnsEmpty() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(E_NoNameField.class);
		assertFalse(getName(HttpPartType.HEADER, cm).isPresent());
	}

	@Test void e12_getName_query_fromNameField() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(E_NameField.class);
		assertEquals("name-from-field", getName(HttpPartType.QUERY, cm).orElse(null));
	}

	@Test void e13_getName_formdata_fromNameField() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(E_NameField.class);
		assertEquals("name-from-field", getName(HttpPartType.FORMDATA, cm).orElse(null));
	}

	@Test void e14_getName_path_fromNameField() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(E_NameField.class);
		assertEquals("name-from-field", getName(HttpPartType.PATH, cm).orElse(null));
	}

	@Test void e15_getName_header_nameFieldNonStatic_returnsEmpty() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(E_NonStaticName.class);
		assertFalse(getName(HttpPartType.HEADER, cm).isPresent());
	}

	@Test void e16_getName_header_nameFieldWrongType_returnsEmpty() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(E_WrongTypeName.class);
		assertFalse(getName(HttpPartType.HEADER, cm).isPresent());
	}

	@Test void e17_getName_header_nullStaticField_returnsEmpty() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(E_NullNameField.class);
		assertFalse(getName(HttpPartType.HEADER, cm).isPresent());
	}
}
