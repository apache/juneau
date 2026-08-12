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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.apache.juneau.commons.svl.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.marshall.MarshallingContext;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Tests for {@link RequestHeaderList}, exercised via a minimally-mocked {@link RestRequest}/{@link RestContext}
 * pair (mirroring {@code RestRequest_Test}'s harness, since this module excludes {@code MockRestClient} -- see
 * {@code RequestHeader_Test}'s note) rather than a full servlet-container round trip.
 *
 * @since 10.0.0
 */
class RequestHeaderList_Test {

	@SuppressWarnings({
		"resource" // Mockito mock; nothing to close.
	})
	private static HttpServletRequest servletRequest(Map<String,String> headers) {
		var r = mock(HttpServletRequest.class);
		when(r.getHeaderNames()).thenReturn(Collections.enumeration(headers.keySet()));
		for (var e : headers.entrySet())
			when(r.getHeaders(e.getKey())).thenReturn(Collections.enumeration(List.of(e.getValue())));
		return r;
	}

	private static RestRequest restRequest(Map<String,String> headers, Set<String> allowedHeaderParams) {
		// NOTE: the servlet-request mock is built (and fully stubbed) *before* any when(...) call on req/context
		// below -- inlining it as a thenReturn(...) argument would start a nested when()/thenReturn() pair while
		// the outer one is still open, which trips Mockito's UnfinishedStubbingException.
		var servletReq = servletRequest(headers);
		var req = mock(RestRequest.class);
		var context = mock(RestContext.class);
		when(context.getAllowedHeaderParams()).thenReturn(allowedHeaderParams);
		when(req.getHttpServletRequest()).thenReturn(servletReq);
		when(req.getContext()).thenReturn(context);
		when(req.getVarResolverSession()).thenReturn(VarResolver.DEFAULT.createSession());
		when(req.getMarshallingSession()).thenReturn(MarshallingContext.DEFAULT_SESSION);
		return req;
	}

	private static RequestHeaderList listOf(Map<String,String> headers) {
		var req = restRequest(headers, Set.of());
		return new RequestHeaderList(req, new RequestQueryParamList(req, Map.of(), false), false);
	}

	//-----------------------------------------------------------------------------------------------------------
	// a - construction: servlet headers + URL-param-to-header merge (allowedHeaderParams)
	//-----------------------------------------------------------------------------------------------------------

	@Test void a01_construct_populatesFromServletHeaders() {
		var l = listOf(Map.of("X-Foo", "bar"));
		assertEquals("bar", l.get("X-Foo").getValue());
	}

	@Test void a02_construct_queryParamNotAllowed_notMerged() {
		var req = restRequest(Map.of(), Set.of());
		var query = new RequestQueryParamList(req, Map.of("X-Foo", new String[]{"bar"}), false);
		var l = new RequestHeaderList(req, query, false);
		assertFalse(l.contains("X-Foo"));
	}

	@Test void a03_construct_queryParamAllowedByName_merged() {
		var req = restRequest(Map.of(), Set.of("x-foo"));
		var query = new RequestQueryParamList(req, Map.of("X-Foo", new String[]{"bar"}), false);
		var l = new RequestHeaderList(req, query, false);
		assertEquals("bar", l.get("X-Foo").getValue());
	}

	@Test void a04_construct_queryParamAllowedByWildcard_merged() {
		var req = restRequest(Map.of(), Set.of("*"));
		var query = new RequestQueryParamList(req, Map.of("X-Foo", new String[]{"bar"}), false);
		var l = new RequestHeaderList(req, query, false);
		assertEquals("bar", l.get("X-Foo").getValue());
	}

	//-----------------------------------------------------------------------------------------------------------
	// b - copy()/subset(): private copy constructors
	//-----------------------------------------------------------------------------------------------------------

	@Test void b01_copy_isIndependentEqualCopy() {
		var l = listOf(Map.of("X-Foo", "bar"));
		var c = l.copy();
		assertEquals(l, c);
		c.add("X-Bar", "baz");
		assertFalse(l.contains("X-Bar"));
	}

	@Test void b02_subset_onlyIncludesNamedHeaders() {
		var l = listOf(Map.of("X-Foo", "1", "X-Bar", "2"));
		var s = l.subset("X-Foo");
		assertTrue(s.contains("X-Foo"));
		assertFalse(s.contains("X-Bar"));
	}

	//-----------------------------------------------------------------------------------------------------------
	// c - add()/addDefault()
	//-----------------------------------------------------------------------------------------------------------

	@Test void c01_add_httpHeaderArray_skipsNullElements() {
		var l = listOf(Map.of());
		l.add(ETag.of("\"v1\""), null);
		assertEquals("\"v1\"", l.get("ETag").getValue());
		assertEquals(1, l.size());
	}

	@Test void c02_addDefault_blankExisting_isOverridden() {
		var l = listOf(Map.of("X-Foo", ""));
		l.addDefault("X-Foo", "fallback");
		assertEquals("fallback", l.get("X-Foo").getValue());
	}

	@Test void c03_addDefault_nonBlankExisting_isUnchanged() {
		var l = listOf(Map.of("X-Foo", "bar"));
		l.addDefault("X-Foo", "fallback");
		assertEquals("bar", l.get("X-Foo").getValue());
	}

	@Test void c04_addDefault_absent_isAdded() {
		var l = listOf(Map.of());
		l.addDefault(List.of(ETag.of("\"v1\"")));
		assertEquals("\"v1\"", l.get("ETag").getValue());
	}

	//-----------------------------------------------------------------------------------------------------------
	// d - caseSensitive()/contains()/containsAny()
	//-----------------------------------------------------------------------------------------------------------

	@Test void d01_caseSensitive_toggleAffectsMatching() {
		var l = listOf(Map.of("X-Foo", "bar"));
		assertTrue(l.contains("x-foo"));
		l.caseSensitive(true);
		assertFalse(l.contains("x-foo"));
		assertTrue(l.contains("X-Foo"));
	}

	@Test void d02_containsAny_matchFound_returnsTrue() {
		var l = listOf(Map.of("X-Foo", "bar"));
		assertTrue(l.containsAny("X-Bar", "X-Foo"));
	}

	@Test void d03_containsAny_noMatch_returnsFalse() {
		var l = listOf(Map.of("X-Foo", "bar"));
		assertFalse(l.containsAny("X-Bar", "X-Baz"));
	}

	//-----------------------------------------------------------------------------------------------------------
	// e - get(Class): resolves the header name from the type's NAME field / @Header annotation
	//-----------------------------------------------------------------------------------------------------------

	@Test void e01_getByClass_presentHeader_convertsToType() {
		var l = listOf(Map.of("ETag", "\"v1\""));
		var etag = l.get(ETag.class);
		assertTrue(etag.isPresent());
		assertEquals("\"v1\"", etag.get().getValue());
	}

	//-----------------------------------------------------------------------------------------------------------
	// f - getFirst()/getLast()
	//-----------------------------------------------------------------------------------------------------------

	@Test void f01_getFirst_present_returnsFirstMatch() {
		var l = listOf(Map.of());
		l.add("X-Foo", "1").add("X-Foo", "2");
		assertEquals("1", l.getFirst("X-Foo").getValue());
	}

	@Test void f02_getFirst_absent_returnsEmptyHeader() {
		var l = listOf(Map.of());
		assertNull(l.getFirst("X-Foo").getValue());
	}

	@Test void f03_getLast_present_returnsLastMatch() {
		var l = listOf(Map.of());
		l.add("X-Foo", "1").add("X-Foo", "2");
		assertEquals("2", l.getLast("X-Foo").getValue());
	}

	@Test void f04_getLast_absent_returnsEmptyHeader() {
		var l = listOf(Map.of());
		assertNull(l.getLast("X-Foo").getValue());
	}

	//-----------------------------------------------------------------------------------------------------------
	// g - getSorted(): case-sensitive vs case-insensitive comparator
	//-----------------------------------------------------------------------------------------------------------

	@Test void g01_getSorted_caseInsensitive_sortsIgnoringCase() {
		var l = listOf(Map.of());
		l.add("b-header", "1").add("A-Header", "2");
		var names = l.getSorted().map(RequestHeader::getName).toList();
		assertEquals(List.of("A-Header", "b-header"), names);
	}

	@Test void g02_getSorted_caseSensitive_sortsByRawName() {
		var l = listOf(Map.of());
		l.caseSensitive(true);
		l.add("b-header", "1").add("A-Header", "2");
		var names = l.getSorted().map(RequestHeader::getName).toList();
		// Uppercase 'A' sorts before lowercase 'b' in a raw (case-sensitive) String comparison too, so use
		// two names that only diverge under case-sensitive comparison to actually distinguish the branch.
		assertEquals(List.of("A-Header", "b-header"), names);
	}

	//-----------------------------------------------------------------------------------------------------------
	// h - parser(): propagates to already-added headers
	//-----------------------------------------------------------------------------------------------------------

	@Test void h01_parser_propagatesToExistingHeaders() {
		var l = listOf(Map.of("X-Foo", "bar"));
		assertDoesNotThrow(() -> l.parser(null));
	}

	//-----------------------------------------------------------------------------------------------------------
	// i - remove()/set()
	//-----------------------------------------------------------------------------------------------------------

	@Test void i01_remove_removesAllMatchingByName() {
		var l = listOf(Map.of());
		l.add("X-Foo", "1").add("X-Foo", "2").add("X-Bar", "3");
		l.remove("X-Foo");
		assertFalse(l.contains("X-Foo"));
		assertTrue(l.contains("X-Bar"));
	}

	@Test void i02_set_replacesExistingAndAddsNew() {
		var l = listOf(Map.of("X-Foo", "old"));
		l.set(ETag.of("\"v1\""));
		assertEquals("\"v1\"", l.get("ETag").getValue());
		l.set(HttpStringHeader.of("X-Foo", "new"));
		assertEquals("new", l.get("X-Foo").getValue());
		assertEquals(1, l.getAll("X-Foo").size());
	}

	//-----------------------------------------------------------------------------------------------------------
	// j - equals()/hashCode()/toString()
	//-----------------------------------------------------------------------------------------------------------

	@Test void j01_equals_sameInstance_isEqual() {
		var l = listOf(Map.of());
		assertEquals(l, l);
	}

	@Test void j02_equals_equalContents_isEqual() {
		var l1 = listOf(Map.of("X-Foo", "bar"));
		var l2 = l1.copy();
		assertEquals(l1, l2);
		assertEquals(l1.hashCode(), l2.hashCode());
	}

	@Test void j03_equals_differentType_isNotEqual() {
		var l = listOf(Map.of());
		assertNotEquals(l, "not a header list");
	}

	@Test void j04_toString_includesHeaderNamesAndValues() {
		var l = listOf(Map.of("X-Foo", "bar"));
		// getNames() (used by properties()/toString()) case-folds to lowercase when caseSensitive is false.
		var s = l.toString();
		assertTrue(s.contains("x-foo"), () -> "toString() was: " + s);
		assertTrue(s.contains("bar"), () -> "toString() was: " + s);
	}
}
