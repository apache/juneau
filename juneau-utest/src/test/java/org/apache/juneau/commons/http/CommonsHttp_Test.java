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
package org.apache.juneau.commons.http;

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link HeaderValueParser}, {@link HeaderElement}, {@link BasicNameValuePair},
 * and {@link NameValuePair#EMPTY_ARRAY}.
 */
class CommonsHttp_Test extends TestBase {

	@Test
	void a01_parseElements_nullOrEmpty() {
		assertEquals(0, HeaderValueParser.parseElements(null).length);
		assertEquals(0, HeaderValueParser.parseElements("").length);
	}

	@Test
	void a02_parseElements_whitespaceOnly() {
		var a = HeaderValueParser.parseElements("   \t  ");
		assertEquals(0, a.length);
	}

	@Test
	void a03_parseElements_singleTypeNoParams() {
		var a = HeaderValueParser.parseElements("text/html");
		assertEquals(1, a.length);
		assertBean(a[0], "name", "text/html");
		assertEquals(0, a[0].getParameters().length);
	}

	@Test
	void a04_parseElements_typeWithParameters() {
		var a = HeaderValueParser.parseElements("application/json;charset=UTF-8;version=2");
		assertEquals(1, a.length);
		assertEquals("application/json", a[0].getName());
		assertEquals(2, a[0].getParameters().length);
		assertEquals("charset", a[0].getParameters()[0].getName());
		assertEquals("UTF-8", a[0].getParameters()[0].getValue());
		assertEquals("version", a[0].getParameters()[1].getName());
		assertEquals("2", a[0].getParameters()[1].getValue());
	}

	@Test
	void a05_parseElements_twoElementsCommaSeparated() {
		var a = HeaderValueParser.parseElements("text/html, application/json");
		assertEquals(2, a.length);
		assertEquals("text/html", a[0].getName());
		assertEquals("application/json", a[1].getName());
	}

	@Test
	void a06_parseElements_quotedParameterValue() {
		var a = HeaderValueParser.parseElements("foo;bar=\"a,b;c\"");
		assertEquals(1, a.length);
		assertEquals("foo", a[0].getName());
		assertEquals(1, a[0].getParameters().length);
		assertEquals("bar", a[0].getParameters()[0].getName());
		assertEquals("a,b;c", a[0].getParameters()[0].getValue());
	}

	@Test
	void a07_parseElements_escapedBackslashInQuotedString() {
		var a = HeaderValueParser.parseElements("p;q=\"a\\\\b\"");
		assertEquals(1, a.length);
		assertEquals("p", a[0].getName());
		assertEquals(1, a[0].getParameters().length);
		assertEquals("q", a[0].getParameters()[0].getName());
		assertEquals("a\\b", a[0].getParameters()[0].getValue());
	}

	@Test
	void a08_parseElements_mediaRangeStyle() {
		var a = HeaderValueParser.parseElements("text/html;charset=UTF-8;q=0.9");
		assertEquals(1, a.length);
		assertEquals("text/html", a[0].getName());
		assertEquals(2, a[0].getParameters().length);
		assertEquals("q", a[0].getParameters()[1].getName());
		assertEquals("0.9", a[0].getParameters()[1].getValue());
	}

	@Test
	void a09_parseElements_paramWithoutValue() {
		var a = HeaderValueParser.parseElements("multipart/form-data; boundary");
		assertEquals(1, a.length);
		assertEquals(1, a[0].getParameters().length);
		assertEquals("boundary", a[0].getParameters()[0].getName());
		assertNull(a[0].getParameters()[0].getValue());
	}

	@Test
	void a10_parseElements_emptyQuotedParameterValue() {
		var a = HeaderValueParser.parseElements("t;a=\"\"");
		assertEquals(1, a.length);
		assertEquals("", a[0].getParameters()[0].getValue());
	}

	@Test
	void a11_parseElements_leadingTrailingWhitespace() {
		var a = HeaderValueParser.parseElements("  text/plain  ;  charset=utf-8  ");
		assertEquals(1, a.length);
		assertEquals("text/plain", a[0].getName());
		assertEquals("utf-8", a[0].getParameters()[0].getValue());
	}

	@Test
	void a12_parseElements_unterminatedQuotedValue() {
		var a = HeaderValueParser.parseElements("x;y=\"no-close");
		assertEquals(1, a.length);
		assertEquals("no-close", a[0].getParameters()[0].getValue());
	}

	@Test
	void b01_basicNameValuePair_gettersAndToString() {
		var a = new BasicNameValuePair("n", "v");
		assertEquals("n", a.getName());
		assertEquals("v", a.getValue());
		assertEquals("n=v", a.toString());
	}

	@Test
	void b02_basicNameValuePair_nullValue() {
		var a = new BasicNameValuePair("n", null);
		assertNull(a.getValue());
		assertEquals("n=null", a.toString());
	}

	@Test
	void b03_basicNameValuePair_equalsAndHashCode() {
		var a = new BasicNameValuePair("x", "1");
		var b = new BasicNameValuePair("x", "1");
		var c = new BasicNameValuePair("x", "2");
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, c);
	}

	@Test
	void b04_basicNameValuePair_equalsWithNullFields() {
		var a = new BasicNameValuePair(null, null);
		var b = new BasicNameValuePair(null, null);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	void b05_basicNameValuePair_notEqualToOtherType() {
		assertNotEquals(new BasicNameValuePair("a", "b"), "a=b");
	}

	@Test
	void c01_headerElement_noParameters() {
		var a = new HeaderElement("text/plain", NameValuePair.EMPTY_ARRAY);
		assertEquals("text/plain", a.getName());
		assertEquals(0, a.getParameters().length);
	}

	@Test
	void c02_headerElement_withParameters() {
		var a = new HeaderElement("t", new BasicNameValuePair("a", "1"), new BasicNameValuePair("b", "2"));
		assertEquals("t", a.getName());
		assertEquals(2, a.getParameters().length);
		assertBean(a.getParameters()[0], "name,value", "a,1");
	}

	@Test
	void d01_nameValuePair_emptyArray() {
		assertEquals(0, NameValuePair.EMPTY_ARRAY.length);
		assertSame(NameValuePair.EMPTY_ARRAY, NameValuePair.EMPTY_ARRAY);
	}
}
