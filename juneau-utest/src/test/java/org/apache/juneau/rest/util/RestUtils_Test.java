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
package org.apache.juneau.rest.util;

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.apache.juneau.rest.util.RestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.urlencoding.*;
import org.junit.jupiter.api.*;

class RestUtils_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// decode(String)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_testDecode() {
		assertNull(urlDecode(null));
		assertEquals("foo/bar baz  bing", urlDecode("foo%2Fbar+baz++bing"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// encode(String)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_testEncode() {
		assertNull(urlEncode(null));
		assertEquals("foo%2Fbar+baz++bing", urlEncode("foo/bar baz  bing"));
		assertEquals("foobar", urlEncode("foobar"));
		assertEquals("+", urlEncode(" "));
		assertEquals("%2F", urlEncode("/"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// trimSlashes(String)
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_testTrimSlashes() {
		assertNull(trimSlashes(null));
		assertEquals("", trimSlashes(""));
		assertEquals("", trimSlashes("/"));
		assertEquals("", trimSlashes("//"));
		assertEquals("foo/bar", trimSlashes("foo/bar"));
		assertEquals("foo/bar", trimSlashes("foo/bar//"));
		assertEquals("foo/bar", trimSlashes("/foo/bar//"));
		assertEquals("foo/bar", trimSlashes("//foo/bar//"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// trimTrailingSlashes(String)
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_testTrimTrailingSlashes() {
		assertNull(trimTrailingSlashes((String)null));
		assertEquals("", trimTrailingSlashes(""));
		assertEquals("", trimTrailingSlashes("/"));
		assertEquals("", trimTrailingSlashes("//"));
		assertEquals("foo/bar", trimTrailingSlashes("foo/bar"));
		assertEquals("foo/bar", trimTrailingSlashes("foo/bar//"));
		assertEquals("/foo/bar", trimTrailingSlashes("/foo/bar//"));
		assertEquals("//foo/bar", trimTrailingSlashes("//foo/bar//"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// getHttpResponseText(int)
	//------------------------------------------------------------------------------------------------------------------

	@Test void i01_testGetHttpResponseText() {
		assertEquals("OK", getHttpResponseText(200));
		assertEquals("Created", getHttpResponseText(201));
		assertEquals("Accepted", getHttpResponseText(202));
		assertEquals("No Content", getHttpResponseText(204));
		assertEquals("Moved Permanently", getHttpResponseText(301));
		assertEquals("Temporary Redirect", getHttpResponseText(302));
		assertEquals("Not Modified", getHttpResponseText(304));
		assertEquals("Bad Request", getHttpResponseText(400));
		assertEquals("Unauthorized", getHttpResponseText(401));
		assertEquals("Forbidden", getHttpResponseText(403));
		assertEquals("Not Found", getHttpResponseText(404));
		assertEquals("Method Not Allowed", getHttpResponseText(405));
		assertEquals("Internal Server Error", getHttpResponseText(500));
		assertEquals("Not Implemented", getHttpResponseText(501));
		assertEquals("Service Unavailable", getHttpResponseText(503));
		assertEquals("Gateway Timeout", getHttpResponseText(504));
	}

	@Test void i02_testGetHttpResponseTextInvalid() {
		assertNull(getHttpResponseText(0));
		assertNull(getHttpResponseText(99));
		assertNull(getHttpResponseText(600));
		assertNull(getHttpResponseText(-1));
	}

	//------------------------------------------------------------------------------------------------------------------
	// getPathInfoUndecoded(HttpServletRequest)
	//------------------------------------------------------------------------------------------------------------------

	@Test void j01_testGetPathInfoUndecoded_basic() {
		var req = MockServletRequest.create("GET", "/foo/bar");
		assertEquals("/foo/bar", getPathInfoUndecoded(req));
	}

	@Test void j02_testGetPathInfoUndecoded_withContextPath() {
		var req = MockServletRequest.create("GET", "http://localhost:8080/context/foo/bar")
			.contextPath("/context");
		assertEquals("/foo/bar", getPathInfoUndecoded(req));
	}

	@Test void j03_testGetPathInfoUndecoded_withServletPath() {
		var req = MockServletRequest.create("GET", "http://localhost:8080/api/foo/bar")
			.servletPath("/api");
		assertEquals("/foo/bar", getPathInfoUndecoded(req));
	}

	@Test void j04_testGetPathInfoUndecoded_withContextAndServletPath() {
		var req = MockServletRequest.create("GET", "http://localhost:8080/context/api/foo/bar")
			.contextPath("/context")
			.servletPath("/api");
		assertEquals("/foo/bar", getPathInfoUndecoded(req));
	}

	@Test void j05_testGetPathInfoUndecoded_noPathInfo() {
		var req = MockServletRequest.create("GET", "http://localhost:8080/context/api")
			.contextPath("/context")
			.servletPath("/api");
		assertNull(getPathInfoUndecoded(req));
	}

	@Test void j06_testGetPathInfoUndecoded_encodedCharacters() {
		var req = MockServletRequest.create("GET", "/foo%2Fbar%20baz");
		assertEquals("/foo%2Fbar%20baz", getPathInfoUndecoded(req));
	}

	@Test void j07_testGetPathInfoUndecoded_rootPath() {
		var req = MockServletRequest.create("GET", "/");
		assertEquals("/", getPathInfoUndecoded(req));
	}

	@Test void j08_testGetPathInfoUndecoded_emptyContextAndServlet() {
		var req = MockServletRequest.create("GET", "/foo/bar")
			.contextPath("")
			.servletPath("");
		assertEquals("/foo/bar", getPathInfoUndecoded(req));
	}

	//------------------------------------------------------------------------------------------------------------------
	// parseIfJson(String)
	//------------------------------------------------------------------------------------------------------------------

	@Test void k01_testParseIfJson_null() {
		assertNull(parseIfJson(null));
	}

	@Test void k02_testParseIfJson_plainString() {
		assertEquals("hello world", parseIfJson("hello world"));
		assertEquals("foo", parseIfJson("foo"));
		assertEquals("", parseIfJson(""));
		assertEquals("123abc", parseIfJson("123abc"));
	}

	@Test void k03_testParseIfJson_jsonObject() {
		var result = parseIfJson("{\"name\":\"John\",\"age\":30}");
		assertTrue(result instanceof Map);
		var map = (Map<String, Object>) result;
		assertEquals("John", map.get("name"));
		assertEquals(30, map.get("age"));
	}

	@Test void k04_testParseIfJson_jsonObjectEmpty() {
		var result = parseIfJson("{}");
		assertTrue(result instanceof Map);
		var map = (Map<String, Object>) result;
		assertTrue(map.isEmpty());
	}

	@Test void k05_testParseIfJson_jsonArray() {
		var result = parseIfJson("[1,2,3]");
		assertTrue(result instanceof List);
		var list = (List<Object>) result;
		assertEquals(3, list.size());
		assertEquals(1, list.get(0));
		assertEquals(2, list.get(1));
		assertEquals(3, list.get(2));
	}

	@Test void k06_testParseIfJson_jsonArrayEmpty() {
		var result = parseIfJson("[]");
		assertTrue(result instanceof List);
		var list = (List<Object>) result;
		assertTrue(list.isEmpty());
	}

	@Test void k07_testParseIfJson_jsonBoolean() {
		assertEquals(true, parseIfJson("true"));
		assertEquals(false, parseIfJson("false"));
	}

	@Test void k08_testParseIfJson_jsonNull() {
		assertNull(parseIfJson("null"));
	}

	@Test void k09_testParseIfJson_jsonNumber() {
		assertEquals(123, parseIfJson("123"));
		var result = parseIfJson("123.45");
		assertTrue(result instanceof Number);
		assertEquals(123.45, ((Number)result).doubleValue(), 0.001);
		assertEquals(-42, parseIfJson("-42"));
	}

	@Test void k10_testParseIfJson_jsonWithWhitespace() {
		var result = parseIfJson("  {\"key\":\"value\"}  ");
		assertTrue(result instanceof Map);
		var map = (Map<String, Object>) result;
		assertEquals("value", map.get("key"));
	}

	@Test void k11_testParseIfJson_invalidJson() {
		// These strings are detected as JSON by isProbablyJson (start with {/} or [/]) but are invalid
		assertThrows(ParseException.class, () -> parseIfJson("{key:xxx}"));
		assertThrows(ParseException.class, () -> parseIfJson("{key:invalid value}"));
	}

	@Test void k12_testParseIfJson_nestedStructures() {
		var result = parseIfJson("{\"items\":[1,2,3],\"nested\":{\"a\":1,\"b\":2}}");
		assertTrue(result instanceof Map);
		var map = (Map<String, Object>) result;
		assertTrue(map.get("items") instanceof List);
		assertTrue(map.get("nested") instanceof Map);
	}

	@Test void k13_testParseIfJson_singleQuotedString() {
		var result = parseIfJson("'test string'");
		assertEquals("test string", result);
	}

	//------------------------------------------------------------------------------------------------------------------
	// parseQuery(String)
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_testParseQuery_null() {
		var m = parseQuery((String)null);
		assertTrue(m.isEmpty());
	}

	@Test void g02_testParseQuery_emptyString() {
		var m = parseQuery("");
		assertTrue(m.isEmpty());
	}

	@Test void g03_testParseQuery_whitespaceOnly() {
		var m = parseQuery("   ");
		assertTrue(m.isEmpty());
	}

	//------------------------------------------------------------------------------------------------------------------
	// parseQuery(Reader)
	//------------------------------------------------------------------------------------------------------------------

	@Test void g04_testParseQuery_readerNull() {
		var m = parseQuery((Reader)null);
		assertTrue(m.isEmpty());
	}

	@Test void g05_testParseQuery_readerEmpty() {
		var m = parseQuery(new StringReader(""));
		assertTrue(m.isEmpty());
	}

	@Test void g06_testParseQuery_readerWhitespaceOnly() {
		var m = parseQuery(new StringReader("   "));
		assertTrue(m.isEmpty());
	}

	@Test void g07_testParseQuery_readerValid() {
		var m = parseQuery(new StringReader("f1=v1&f2=v2"));
		assertEquals("v1", m.get("f1").get(0));
		assertEquals("v2", m.get("f2").get(0));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test URL-encoded strings parsed into plain-text values using UrlEncodingParser.parseIntoSimpleMap().
	//------------------------------------------------------------------------------------------------------------------

	@Test void g04_testParseIntoSimpleMap() {
		var s = "?f1=,()=&f2a=$b(true)&f2b=true&f3a=$n(123)&f3b=123&f4=$s(foo)";
		var m = parseQuery(s);
		assertEquals(",()=", m.get("f1").get(0));
		assertEquals("$b(true)", m.get("f2a").get(0));
		assertEquals("true", m.get("f2b").get(0));
		assertEquals("$n(123)", m.get("f3a").get(0));
		assertEquals("123", m.get("f3b").get(0));
		assertEquals("$s(foo)", m.get("f4").get(0));

		s = "f1=v1&=";
		m = parseQuery(s);
		assertEquals("v1", m.get("f1").get(0));
		assertEquals("", m.get("").get(0));

		s = "f1=v1&f2&f3";
		m = parseQuery(s);
		assertEquals("v1", m.get("f1").get(0));
		assertTrue(m.containsKey("f2"));
		assertTrue(m.containsKey("f3"));
		assertNull(m.get("f2"));
		assertNull(m.get("f3"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test parsing URL-encoded strings with multiple values.
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_testParseIntoSimpleMapMultiValues() {
		var s = "?f1&f1&f2&f2=abc&f2=def&f2";
		var m = parseQuery(s);
		assertBean(m, "f1,f2", "<null>,[abc,def]");
	}

	@Test void h02_testEmptyString() {
		var p = UrlEncodingParser.DEFAULT;

		var s = "";
		var b = p.parse(s, B.class);
		assertEquals("f1", b.f1);
	}

	public static class B {
		public String f1 = "f1";
	}

	//------------------------------------------------------------------------------------------------------------------
	// toValidContextPath(String)
	//------------------------------------------------------------------------------------------------------------------

	@Test void m01_testToValidContextPath_null() {
		assertEquals("", toValidContextPath(null));
	}

	@Test void m02_testToValidContextPath_empty() {
		assertEquals("", toValidContextPath(""));
	}

	@Test void m03_testToValidContextPath_root() {
		assertEquals("", toValidContextPath("/"));
	}

	@Test void m04_testToValidContextPath_noLeadingSlash() {
		assertEquals("/api", toValidContextPath("api"));
	}

	@Test void m05_testToValidContextPath_withLeadingSlash() {
		assertEquals("/api", toValidContextPath("/api"));
	}

	@Test void m06_testToValidContextPath_trailingSlash() {
		assertEquals("/api", toValidContextPath("api/"));
	}

	@Test void m07_testToValidContextPath_bothSlashes() {
		assertEquals("/api", toValidContextPath("/api/"));
	}

	@Test void m08_testToValidContextPath_multipleTrailingSlashes() {
		assertEquals("/api", toValidContextPath("/api///"));
	}

	@Test void m09_testToValidContextPath_nestedPath() {
		assertEquals("/api/users", toValidContextPath("/api/users"));
	}

	@Test void m10_testToValidContextPath_nestedPathTrailingSlash() {
		assertEquals("/api/users", toValidContextPath("/api/users/"));
	}

	@Test void m11_testToValidContextPath_onlySlashes() {
		assertEquals("", toValidContextPath("///"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// validatePathInfo(String)
	//------------------------------------------------------------------------------------------------------------------

	@Test void n01_testValidatePathInfo_null() {
		assertNull(validatePathInfo(null));
	}

	@Test void n02_testValidatePathInfo_valid() {
		assertEquals("/users", validatePathInfo("/users"));
	}

	@Test void n03_testValidatePathInfo_validNested() {
		assertEquals("/users/123", validatePathInfo("/users/123"));
	}

	@Test void n04_testValidatePathInfo_empty() {
		assertThrows(RuntimeException.class, () -> validatePathInfo(""));
	}

	@Test void n05_testValidatePathInfo_noLeadingSlash() {
		assertThrows(RuntimeException.class, () -> validatePathInfo("users"));
	}

	@Test void n06_testValidatePathInfo_noLeadingSlashNested() {
		assertThrows(RuntimeException.class, () -> validatePathInfo("users/123"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// validateServletPath(String)
	//------------------------------------------------------------------------------------------------------------------

	@Test void o01_testValidateServletPath_empty() {
		assertEquals("", validateServletPath(""));
	}

	@Test void o02_testValidateServletPath_valid() {
		assertEquals("/api", validateServletPath("/api"));
	}

	@Test void o03_testValidateServletPath_validNested() {
		assertEquals("/api/users", validateServletPath("/api/users"));
	}

	@Test void o04_testValidateServletPath_null() {
		assertThrows(RuntimeException.class, () -> validateServletPath(null));
	}

	@Test void o05_testValidateServletPath_root() {
		assertThrows(RuntimeException.class, () -> validateServletPath("/"));
	}

	@Test void o06_testValidateServletPath_noLeadingSlash() {
		assertThrows(RuntimeException.class, () -> validateServletPath("api"));
	}

	@Test void o07_testValidateServletPath_trailingSlash() {
		assertThrows(RuntimeException.class, () -> validateServletPath("/api/"));
	}

	@Test void o08_testValidateServletPath_noLeadingSlashTrailingSlash() {
		assertThrows(RuntimeException.class, () -> validateServletPath("api/"));
	}

	@Test void o09_testValidateServletPath_nestedTrailingSlash() {
		assertThrows(RuntimeException.class, () -> validateServletPath("/api/users/"));
	}
}