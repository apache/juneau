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
package org.apache.juneau.rest.server.httppart;

import static org.apache.juneau.commons.httppart.HttpPartType.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.regex.*;

import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link RequestHttpPart}.
 *
 * <p>
 * Most of this class's logic (name/value access, the {@code as*()} conversion family, fluent assertions)
 * operates purely off the part's own {@code name}/{@code value} state and does not touch the {@code request}
 * field at all -- only {@link RequestHttpPart#as(Class)} and {@link RequestHttpPart#as(java.lang.reflect.Type,
 * java.lang.reflect.Type...)} dereference it (via {@code request.getMarshallingSession()}). This module
 * intentionally excludes {@code MockRestClient} from its test scope (see {@code RestArgResolvers_Test}), so this
 * file constructs {@link RequestHttpPart} directly with a {@code null} request and exercises every method that
 * doesn't require one, leaving the two {@code request}-dependent overloads to the higher-level integration tests.
 */
class RequestHttpPart_Test {

	private static RequestHttpPart part(String name, String value) {
		return new RequestHttpPart(HEADER, null, name, value);
	}

	@Test void a01_getNameAndValue() {
		var p = part("foo", "bar");
		assertEquals("foo", p.getName());
		assertEquals("bar", p.getValue());
	}

	@Test void a02_isPresent() {
		assertTrue(part("foo", "bar").isPresent());
		assertFalse(part("foo", null).isPresent());
	}

	@Test void a03_orElse() {
		assertEquals("bar", part("foo", "bar").orElse("def"));
		assertEquals("def", part("foo", null).orElse("def"));
	}

	@Test void a04_get() {
		assertEquals("bar", part("foo", "bar").get());
		assertThrows(NoSuchElementException.class, () -> part("foo", null).get());
	}

	@Test void a05_def_onlyAppliesWhenValueAbsent() {
		assertEquals("bar", part("foo", "bar").def("def").getValue());
		assertEquals("def", part("foo", null).def("def").getValue());
	}

	@Test void a06_asString() {
		assertEquals("bar", part("foo", "bar").asString().get());
		assertTrue(part("foo", null).asString().isEmpty());
	}

	@Test void a07_asStringPart() {
		var sp = part("foo", "bar").asStringPart();
		assertEquals("foo", sp.getName());
		assertEquals("bar", sp.getValue());
	}

	@Test void a08_asUriPart() {
		assertNotNull(part("foo", "http://example.com").asUriPart());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Boolean
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_asBoolean() {
		assertTrue(part("foo", "true").asBoolean().get());
		assertTrue(part("foo", null).asBoolean().isEmpty());
	}

	@Test void b02_asBooleanPart() {
		assertNotNull(part("foo", "true").asBooleanPart());
	}

	//------------------------------------------------------------------------------------------------------------------
	// CSV array
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_asCsvArray() {
		assertEquals(List.of("a", "b", "c"), part("foo", "a,b,c").asCsvArray().get());
	}

	@Test void c02_asCsvArrayPart() {
		assertNotNull(part("foo", "a,b").asCsvArrayPart());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Date
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_asDate() {
		assertTrue(part("foo", null).asDate().isEmpty());
	}

	@Test void d02_asDatePart() {
		assertNotNull(part("foo", null).asDatePart());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Integer / Long
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_asInteger() {
		assertEquals(123, part("foo", "123").asInteger().get());
	}

	@Test void e02_asIntegerPart() {
		assertNotNull(part("foo", "123").asIntegerPart());
	}

	@Test void e03_asLong() {
		assertEquals(123L, part("foo", "123").asLong().get());
	}

	@Test void e04_asLongPart() {
		assertNotNull(part("foo", "123").asLongPart());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Matcher
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_asMatcher_pattern() {
		var m = part("foo", "bar123").asMatcher(Pattern.compile("[a-z]+(\\d+)"));
		assertTrue(m.matches());
		assertEquals("123", m.group(1));
	}

	@Test void f02_asMatcher_regex() {
		assertTrue(part("foo", "bar").asMatcher("b.r").matches());
	}

	@Test void f03_asMatcher_regexAndFlags() {
		assertTrue(part("foo", "BAR").asMatcher("bar", Pattern.CASE_INSENSITIVE).matches());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Assertions
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_assertCsvArray() {
		part("foo", "a,b").assertCsvArray().isHas("a", "b");
	}

	@Test void g02_assertDate() {
		assertNotNull(part("foo", null).assertDate());
	}

	@Test void g03_assertInteger() {
		part("foo", "5").assertInteger().isGt(1);
	}

	@Test void g04_assertLong() {
		part("foo", "5").assertLong().isLt(100L);
	}

	@Test void g05_assertString() {
		part("foo", "bar").assertString().isContains("ar");
	}

	//------------------------------------------------------------------------------------------------------------------
	// as(ClassMeta) -- doesn't touch request at all
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_asClassMeta() throws Exception {
		var cm = MarshallingContext.DEFAULT.getClassMeta(String.class);
		assertEquals("bar", part("foo", "bar").as(cm).get());
	}

	@Test void h02_asClassMeta_notPresent() throws Exception {
		var cm = MarshallingContext.DEFAULT.getClassMeta(String.class);
		assertTrue(part("foo", null).as(cm).isEmpty());
	}

	//------------------------------------------------------------------------------------------------------------------
	// parser() / schema()
	//------------------------------------------------------------------------------------------------------------------

	@Test void i01_parser_nullResetsToDefault() {
		var p = part("foo", "bar").parser(null);
		assertNotNull(p);
	}

	@Test void i02_schema() {
		assertNotNull(part("foo", "bar").schema(null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// equals() / hashCode() / toString()
	//------------------------------------------------------------------------------------------------------------------

	@Test void j01_equals_httpPart() {
		var p = part("foo", "bar");
		assertEquals(p, org.apache.juneau.http.part.HttpPartBean.of("foo", "bar"));
		assertNotEquals(p, org.apache.juneau.http.part.HttpPartBean.of("foo", "baz"));
		assertNotEquals(p, "not a part");
	}

	@Test void j02_equals_httpHeader() {
		var p = part("foo", "bar");
		assertEquals(p, org.apache.juneau.http.header.HttpStringHeader.of("foo", "bar"));
	}

	@Test void j03_hashCode() {
		assertEquals(part("foo", "bar").hashCode(), part("foo", "bar").hashCode());
	}

	@Test void j04_toString() {
		assertEquals("foo=bar", part("foo", "bar").toString());
	}

	@Test void k01_getRequest_null() {
		assertNull(part("foo", "bar").getRequest());
	}
}
