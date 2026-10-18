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
package org.apache.juneau.rest.server.filter;

import static org.apache.juneau.rest.server.filter.CsrfBoundary_Test.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * End-to-end behaviour of {@link RequestBoundaryFilter} applying a {@link CsrfBoundary}, judged by what reaches the
 * {@link FilterChain}.
 *
 * <p>
 * The chain is a Mockito mock standing in for everything behind the filter, so "never reaches the resource" is
 * observable as "the chain was never touched".  A mock REST client cannot show that: it dispatches straight to the REST
 * context and never runs a servlet filter, so a test built on it passes with no filter installed.
 *
 * <p>
 * Requests are built with {@link CsrfBoundary_Test}'s fixture, which stubs {@code getHeaders(name)} alongside
 * {@code getHeader(name)}; without that every request would look like it had no {@code Host} at all.
 *
 * @since 10.0.0
 */
class CsrfBoundaryChain_Test extends TestBase {

	private static final byte[] KEY = new byte[32];
	static {
		for (var i = 0; i < KEY.length; i++)
			KEY[i] = (byte)(0x30 + i);
	}

	private static final CsrfBoundary B = CsrfBoundary.create().tokens(CsrfTokens.perSession().key(KEY)).build();

	private static final String HOST = "app.example:8080";
	private static final String ORIGIN = "http://app.example:8080";
	private static final String EVIL = "http://evil.example";
	private static final String JSON = "application/json";

	/** Builds the request, keeping the attributes the filter sets on it so the chain can read them back. */
	private static HttpServletRequest build(R r) {
		var req = r.build();
		var attrs = new HashMap<String,Object>();
		doAnswer(x -> attrs.put(x.getArgument(0), x.getArgument(1))).when(req).setAttribute(anyString(), any());
		when(req.getAttribute(anyString())).thenAnswer(x -> attrs.get(x.getArgument(0)));
		return req;
	}

	/** A response that keeps the headers it is given the way a container does, and swallows the body. */
	@SuppressWarnings("resource") // The stub output stream is returned from the mocked response and holds nothing to close.
	private static HttpServletResponse response(Map<String,String> headers) throws IOException {
		var res = mock(HttpServletResponse.class);
		doAnswer(x -> headers.put(x.getArgument(0), x.getArgument(1))).when(res).setHeader(anyString(), anyString());
		doAnswer(x -> { headers.clear(); return null; }).when(res).reset();
		when(res.getOutputStream()).thenReturn(new ServletOutputStream() {
			@Override public void write(int b) { /* The body is not under test here. */ }
			@Override public boolean isReady() { return true; }
			@Override public void setWriteListener(WriteListener l) { /* Not used by the filter. */ }
		});
		return res;
	}

	/** Runs the filter over the request; returns the chain so the caller can see whether it was reached. */
	private static FilterChain run(HttpServletRequest req) throws Exception {
		var chain = mock(FilterChain.class);
		new RequestBoundaryFilter(B).doFilter(req, response(new TreeMap<>(String.CASE_INSENSITIVE_ORDER)), chain);
		return chain;
	}

	/** A masked token for a page served to a request in the given session, as the filter would publish it. */
	private static String pageToken(String session) throws Exception {
		var req = build(new R().method("GET").h("Host", HOST).session(session));
		var chain = run(req);
		verify(chain).doFilter(eq(req), any());
		return (String)req.getAttribute(RequestBoundaryFilter.TOKEN_ATTRIBUTE);
	}

	private static R tokenOf(R r, String token) {
		return r.h(B.csrfHeader(), token);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a) What reaches the chain
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_rejectedRequestNeverReachesChain() throws Exception {
		var token = pageToken("s1");

		var refused = List.of(
			tokenOf(r("POST", "http", HOST, EVIL, "Content-Type", JSON), token),        // another site's page
			r("POST", "http", HOST, ORIGIN, "Content-Type", JSON),                       // the own page, no token
			r("GET", "http", HOST, null).query("method=DELETE")                         // a write disguised as a read
		);
		for (var r : refused)
			verifyNoInteractions(run(build(r)));

		var allowed = List.of(
			tokenOf(r("POST", "http", HOST, ORIGIN, "Content-Type", JSON), token),
			tokenOf(r("POST", "http", HOST, ORIGIN, "Content-Type", JSON), pageToken("s1")),
			tokenOf(r("GET", "http", HOST, ORIGIN).query("method=DELETE"), token)
		);
		for (var r : allowed) {
			var req = build(r);
			verify(run(req), times(1)).doFilter(eq(req), any());
		}
	}

	@Test void a02_rejectionCarriesNoStoreAndRequestBoundaryHeader() throws Exception {
		var headers = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
		var res = response(headers);
		var chain = mock(FilterChain.class);
		new RequestBoundaryFilter(B).doFilter(build(r("POST", "http", HOST, EVIL, "Content-Type", JSON)), res, chain);
		verifyNoInteractions(chain);
		verify(res).setStatus(403);
		assertEquals("ORIGIN_MISMATCH", headers.get(RequestBoundaryFilter.REJECTION_HEADER));
		assertEquals("no-store", headers.get("Cache-Control"));
	}

	@Test void a03_tokenPublishedOnAllowedSafeRequest() throws Exception {
		var req = build(r("GET", "http", HOST, null));
		var seen = new HashMap<String,Object>();
		var chain = mock(FilterChain.class);
		doAnswer(x -> {
			var q = (HttpServletRequest)x.getArgument(0);
			for (var a : List.of(RequestBoundaryFilter.TOKEN_ATTRIBUTE, RequestBoundaryFilter.TOKEN_SUPPLIER_ATTRIBUTE,
					RequestBoundaryFilter.HEADER_ATTRIBUTE))
				seen.put(a, q.getAttribute(a));
			return null;
		}).when(chain).doFilter(any(), any());
		new RequestBoundaryFilter(B).doFilter(req, response(new HashMap<>()), chain);

		var token = assertInstanceOf(String.class, seen.get(RequestBoundaryFilter.TOKEN_ATTRIBUTE));
		assertTrue(token.matches("[0-9a-f]{128}"), token);
		var supplier = assertInstanceOf(Supplier.class, seen.get(RequestBoundaryFilter.TOKEN_SUPPLIER_ATTRIBUTE));
		var supplied = assertInstanceOf(String.class, supplier.get());
		assertTrue(supplied.matches("[0-9a-f]{128}"), supplied);
		assertEquals(B.csrfHeader(), seen.get(RequestBoundaryFilter.HEADER_ATTRIBUTE));
	}

	@Test void a05_sessionMintingObserved() throws Exception {
		// Only getSession(true) is observable here; the cookie it leads to is written by a real container.
		var navigate = build(r("GET", "http", HOST, null, "Sec-Fetch-Mode", "navigate").noSession());
		run(navigate);
		verify(navigate).getSession(true);

		var html = build(r("GET", "http", HOST, null, "Accept", "text/html,application/xhtml+xml").noSession());
		run(html);
		verify(html).getSession(true);

		var any = build(r("GET", "http", HOST, null, "Accept", "*/*").noSession());
		verify(run(any)).doFilter(eq(any), any());
		verify(any, never()).getSession(true);
	}

	@Test void a06_perSessionTokenWorksEndToEnd() throws Exception {
		var mine = pageToken("s1");
		var theirs = pageToken("s2");

		var ok = build(tokenOf(r("POST", "http", HOST, ORIGIN, "Content-Type", JSON).session("s1"), mine));
		verify(run(ok)).doFilter(eq(ok), any());

		var headers = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
		var chain = mock(FilterChain.class);
		new RequestBoundaryFilter(B).doFilter(
			build(tokenOf(r("POST", "http", HOST, ORIGIN, "Content-Type", JSON).session("s1"), theirs)), response(headers), chain);
		verifyNoInteractions(chain);
		assertEquals("CSRF_TOKEN_MISMATCH", headers.get(RequestBoundaryFilter.REJECTION_HEADER));
	}

	@Test void a07_mutatedTokenOnSameSessionFails() throws Exception {
		var token = pageToken("s1");
		var last = token.charAt(token.length() - 1);
		var mutated = token.substring(0, token.length() - 1) + (last == '0' ? '1' : '0');

		var headers = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
		var chain = mock(FilterChain.class);
		new RequestBoundaryFilter(B).doFilter(
			build(tokenOf(r("POST", "http", HOST, ORIGIN, "Content-Type", JSON).session("s1"), mutated)), response(headers), chain);
		verifyNoInteractions(chain);
		assertEquals("CSRF_TOKEN_MISMATCH", headers.get(RequestBoundaryFilter.REJECTION_HEADER));

		var ok = build(tokenOf(r("POST", "http", HOST, ORIGIN, "Content-Type", JSON).session("s1"), token));
		verify(run(ok)).doFilter(eq(ok), any());
	}
}
