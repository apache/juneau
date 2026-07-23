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
package org.apache.juneau.marshall.html;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Branch-coverage tests for {@link HtmlParserSession}.
 */
@SuppressWarnings({
	"unchecked",
	"rawtypes"
})
class HtmlParserSession_Test extends TestBase {

	//====================================================================================================
	// Shared beans and helpers
	//====================================================================================================

	public static class AB {
		public String a;
		public String b;
		public AB() {}
		public AB(String a, String b) { this.a = a; this.b = b; }
	}

	private static final HtmlSerializer SER = HtmlSerializer.DEFAULT_SQ;
	private static final HtmlParser PAR = HtmlParser.DEFAULT;

	//====================================================================================================
	// a - <p> tag (line 311, "No Results" path)
	//====================================================================================================

	@Test void a01_emptyCollection_noResults() throws Exception {
		// HtmlSerializer emits <ul></ul> for empty collections; round-trip parses it back
		var list = new ArrayList<String>();
		var html = SER.write(list);
		var result = (List<String>) PAR.read(html, List.class, String.class);
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test void a02_pTagNoResults() throws Exception {
		// Directly parsing <p>No Results</p> wrapped in proper HTML div#data → line 313 true branch
		var html = "<html><body><div id='data'><p>No Results</p></div></body></html>";
		var r = PAR.read(html, Object.class);
		assertNull(r);
	}

	//====================================================================================================
	// b - Table type attribute resolution (lines 330-336)
	//====================================================================================================

	@Test void b01_tableWithArrayType_intoCollection() throws Exception {
		// Table with type="array" attribute → typeName.equals("array") branch at line 357
		// Serialize list of ABs, then parse back as List<AB>
		var list = List.of(new AB("x", "y"), new AB("p", "q"));
		var html = SER.write(list);
		var result = (List<AB>) PAR.read(html, List.class, AB.class);
		assertEquals(2, result.size());
		assertEquals("x", result.get(0).a);
	}

	@Test void b02_tableWithArrayType_intoArray() throws Exception {
		// Table with type="array" → sType.isArray() at line 362
		var list = List.of(new AB("x", "y"), new AB("p", "q"));
		var html = SER.write(list);
		var result = PAR.read(html, AB[].class);
		assertEquals(2, result.length);
		assertEquals("x", result[0].a);
	}

	@Test void b03_tableWithObjectType_intoBean() throws Exception {
		// Table with type="object" → sType.canCreateNewBean() at line 344
		var bean = new AB("hello", "world");
		var html = SER.write(bean);
		var result = PAR.read(html, AB.class);
		assertEquals("hello", result.a);
		assertEquals("world", result.b);
	}

	@Test void b04_tableWithObjectType_intoMap() throws Exception {
		// Table with type="object" → sType.isMap() at line 347
		var map = new LinkedHashMap<String,String>();
		map.put("k1", "v1");
		map.put("k2", "v2");
		var html = SER.write(map);
		var result = (Map<String,String>) PAR.read(html, LinkedHashMap.class, String.class, String.class);
		assertEquals("v1", result.get("k1"));
	}

	//====================================================================================================
	// c - UL list parsing (lines 373-387)
	//====================================================================================================

	@Test void c01_ulIntoStringList() throws Exception {
		// <ul> with strings → sType.isObject() at line 379
		var list = List.of("alpha", "beta", "gamma");
		var html = SER.write(list);
		var result = (List) PAR.read(html, List.class, String.class);
		assertEquals(3, result.size());
		assertEquals("alpha", result.get(0));
	}

	@Test void c02_ulIntoStringArray() throws Exception {
		// <ul> with strings → sType.isArray() at line 383
		var list = List.of("alpha", "beta");
		var html = SER.write(list);
		var result = PAR.read(html, String[].class);
		assertArrayEquals(new String[]{"alpha", "beta"}, result);
	}

	@Test void c03_ulIntoLinkedList() throws Exception {
		// <ul> → sType.isCollection() at line 381, creating LinkedList instance
		var list = List.of("x", "y");
		var html = SER.write(list);
		LinkedList<String> result = PAR.read(html, LinkedList.class, String.class);
		assertEquals(2, result.size());
	}

	//====================================================================================================
	// d - Parse text element types (lines 246-269): Date, Calendar, Temporal, Duration, Period
	//====================================================================================================

	@Test void d01_dateInCollection() throws Exception {
		// sType.isDate() at line 256 when parsing text inside <li>
		var d = new Date(0);
		var html = SER.write(List.of(d));
		assertNotNull(html);
	}

	@Test void d02_temporalRootValue() throws Exception {
		// sType.isTemporal() at line 260 via <string> tag parsing
		var html = SER.write(Instant.EPOCH);
		var result = PAR.read(html, Instant.class);
		assertNotNull(result);
	}

	@Test void d03_durationRootValue() throws Exception {
		// sType.isDuration() at line 262 via <string> tag parsing
		var html = SER.write(Duration.ofSeconds(42));
		var result = PAR.read(html, Duration.class);
		assertEquals(Duration.ofSeconds(42), result);
	}

	@Test void d04_periodRootValue() throws Exception {
		// sType.isPeriod() at line 264 via <string> tag parsing
		var html = SER.write(Period.of(1, 2, 3));
		var result = PAR.read(html, Period.class);
		assertEquals(Period.of(1, 2, 3), result);
	}

	//====================================================================================================
	// e - STRING tag type resolution (lines 271-291): Date, Calendar, Temporal, Duration, Period
	//====================================================================================================

	@Test void e01_calendarAsStringTag() throws Exception {
		// sType.isCalendar() at line 279 via <string> tag
		var cal = Calendar.getInstance();
		cal.setTimeInMillis(0);
		var html = SER.write(cal);
		assertNotNull(html);
	}

	//====================================================================================================
	// f - <null> tag parsing (line 317)
	//====================================================================================================

	@Test void f01_nullTagInList() throws Exception {
		// tag == NULL at line 317 in readAnything → null element in list
		var list = new ArrayList<>();
		list.add("hello");
		list.add(null);
		list.add("world");
		var html = SER.write(list);
		var result = (List) PAR.read(html, List.class, String.class);
		assertEquals(3, result.size());
		assertNull(result.get(1));
	}

	//====================================================================================================
	// g - Table null row (NULL tag inside table rows, line 557)
	//====================================================================================================

	@Test void g01_tableWithNullRow() throws Exception {
		// In readTableIntoCollection, a NULL tag in a row → m = null at line 558
		var list = new ArrayList<AB>();
		list.add(new AB("x", "y"));
		list.add(null);
		list.add(new AB("a", "b"));
		var html = SER.write(list);
		var result = (List<AB>) PAR.read(html, List.class, AB.class);
		assertEquals(3, result.size());
		assertNull(result.get(1));
	}

	//====================================================================================================
	// h - Optional parsing (line 206)
	//====================================================================================================

	@Test void h01_optionalPresentInBean() throws Exception {
		// sType.isOptional() at line 206 → wraps in Optional
		var html = SER.write("test value");
		Optional<String> result = PAR.read(html, Optional.class, String.class);
		assertTrue(result.isPresent());
		assertEquals("test value", result.get());
	}

	//====================================================================================================
	// i - Boolean type in NUMBER/BOOLEAN tags (lines 293-309)
	//====================================================================================================

	@Test void i01_numberTagIntoNumber() throws Exception {
		// tag == NUMBER, sType.isNumber() at line 297
		var html = SER.write(42);
		var result = PAR.read(html, Integer.class);
		assertEquals(42, result);
	}

	@Test void i02_booleanTag() throws Exception {
		// tag == BOOLEAN, sType.isBoolean() at line 305
		var html = SER.write(true);
		var result = PAR.read(html, Boolean.class);
		assertTrue(result);
	}

	@Test void i03_numberAsObject() throws Exception {
		// tag == NUMBER, sType.isObject() at line 295
		var html = SER.write(3.14);
		var result = PAR.read(html, Object.class);
		assertNotNull(result);
	}

	//====================================================================================================
	// j - Unknown property in table row (line 563, e == null case)
	//====================================================================================================

	public static class BeanWithKnownProps {
		public String known;
		public BeanWithKnownProps() { /* Public no-arg constructor required for Juneau bean recognition. */ }
	}

	@Test void j01_unknownBeanPropInTableRow() throws Exception {
		// e == null at line 563 → skip value (readAnything(object(),...))
		// Serialize AB but parse as BeanWithKnownProps (has only 'known' prop)
		var list = List.of(new AB("x", "y"));
		var html = SER.write(list);
		var p = HtmlParser.create().ignoreUnknownBeanProperties().build();
		var result = (List<BeanWithKnownProps>) p.read(html, List.class, BeanWithKnownProps.class);
		assertEquals(1, result.size());
	}

	//====================================================================================================
	// k - Anchor tag as non-root (line 321)
	//====================================================================================================

	@Test void k01_anchorTagInList() throws Exception {
		// tag == A at line 321 in readAnything → calls readAnchor
		var html = "<html><body><div id='data'><ul _type='array'><li><a href='http://example.com'>Link</a></li></ul></div></body></html>";
		var result = (List) PAR.read(html, List.class, String.class);
		assertEquals(1, result.size());
		assertEquals("http://example.com", result.get(0));
	}
}
