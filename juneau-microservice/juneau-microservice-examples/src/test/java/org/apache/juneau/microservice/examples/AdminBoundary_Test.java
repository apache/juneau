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
package org.apache.juneau.microservice.examples;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.server.filter.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * {@link AdminBoundary}: the registration's mapping, and the CSRF decisions its filter makes for the admin resources.
 *
 * <p>
 * The decision tests drive the filter itself with mock servlet objects and a counting chain.  A mock REST client would
 * not do: it never runs a servlet filter, so a test built on it passes with no boundary installed.
 */
class AdminBoundary_Test extends TestBase {

	private static final String HOST = "localhost:10000";
	private static final String SELF = "http://localhost:10000";
	private static final String EVIL = "http://evil.example";
	private static final String JSON = "application/json";
	private static final String FORM = "application/x-www-form-urlencoded";
	private static final String REASON = RequestBoundaryFilter.REJECTION_HEADER;

	private static MockServletRequest req(String method, String uri) {
		return new MockServletRequest().method(method).uri(uri).header("Host", HOST).httpSession(null);
	}

	/** A chain that counts its calls. */
	private static final class Chain implements FilterChain {
		int calls;

		@Override /* FilterChain */
		public void doFilter(ServletRequest req, ServletResponse res) {
			calls++;
		}
	}

	private static Filter filter(String...allowedHosts) {
		return AdminBoundary.registration("", allowedHosts).getFilter();
	}

	private static void assertRefused(Filter f, HttpServletRequest req, int status, BoundaryReason reason) throws Exception {
		var res = MockServletResponse.create();
		var chain = new Chain();
		f.doFilter(req, res, chain);
		assertEquals(status, res.getStatus());
		assertEquals(reason.name(), res.getHeader(REASON));
		assertEquals(0, chain.calls);
	}

	private static void assertPasses(Filter f, HttpServletRequest req) throws Exception {
		var res = MockServletResponse.create();
		var chain = new Chain();
		f.doFilter(req, res, chain);
		assertNull(res.getHeader(REASON), res.getHeader(REASON));
		assertEquals(1, chain.calls);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - the registration's mapping
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_rootPrefixMapsSixPatterns() {
		var r = AdminBoundary.registration("");
		assertEquals(List.of("/config", "/config/*", "/logs", "/logs/*", "/shutdown", "/shutdown/*"), r.getUrlPatterns());
	}

	@Test void a02_prefixIsPrepended() {
		var r = AdminBoundary.registration("/admin");
		assertEquals(List.of("/admin/config", "/admin/config/*", "/admin/logs", "/admin/logs/*", "/admin/shutdown", "/admin/shutdown/*"), r.getUrlPatterns());
	}

	@Test void a03_requestDispatchOnlyAtItsOrder() {
		var r = AdminBoundary.registration("");
		assertEquals(Set.of(DispatcherType.REQUEST), Set.copyOf(r.getDispatcherTypes()));
		assertEquals(AdminBoundary.ORDER, r.getOrder());
		assertEquals(ServletFilterRegistration.HIGHEST_PRECEDENCE + 20, r.getOrder());
		assertTrue(r.getOrder() < ServletFilterRegistration.AUTH_ORDER, "The boundary must run before authentication");
	}

	@Test void a04_eachCallBuildsItsOwnFilter() {
		var a = AdminBoundary.registration("");
		var b = AdminBoundary.registration("");
		assertInstanceOf(RequestBoundaryFilter.class, a.getFilter());
		assertNotSame(a.getFilter(), b.getFilter());
	}

	@Test void a05_malformedPrefixIsRejected() {
		for (var p : new String[] { null, "/", "admin", "/admin/", "/admin/*", "*", "//admin", "/a;b", "/a?b", "/a b", "/a/../b", "/./a", "/.hidden", "/a%2Fb" }) {
			assertThrows(IllegalArgumentException.class, () -> AdminBoundary.registration(p), () -> "prefix: " + p);
		}
	}

	@Test void a06_nestedPrefixIsAccepted() {
		assertEquals("/ops/admin-1/config", AdminBoundary.registration("/ops/admin-1").getUrlPatterns().get(0));
	}

	@Test void a07_badHostsAreRejected() {
		assertThrows(IllegalArgumentException.class, () -> AdminBoundary.registration("", (String[])null));
		assertThrows(IllegalArgumentException.class, () -> AdminBoundary.registration("", (String)null));
		assertThrows(IllegalArgumentException.class, () -> AdminBoundary.registration("", " "));
		assertThrows(IllegalArgumentException.class, () -> AdminBoundary.registration("", "localhost:10000"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - browser writes are refused; non-browser writes and reads pass
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_crossSiteShutdownIsRefused() throws Exception {
		assertRefused(filter(), req("POST", "/shutdown").header("Origin", EVIL).header("Content-Type", FORM), 403, BoundaryReason.ORIGIN_MISMATCH);
	}

	@Test void b02_sameOriginBrowserWriteHasNoTokenAndIsRefused() throws Exception {
		assertRefused(filter(), req("POST", "/shutdown").header("Origin", SELF).header("Content-Type", JSON), 403, BoundaryReason.CSRF_TOKEN_MISSING);
	}

	@Test void b03_simplePostWithoutOriginIsRefused() throws Exception {
		assertRefused(filter(), req("POST", "/shutdown").header("Content-Type", "text/plain"), 403, BoundaryReason.SIMPLE_REQUEST_WITHOUT_ORIGIN);
	}

	@Test void b04_methodParamDeleteIsRefused() throws Exception {
		assertRefused(filter(), req("GET", "/logs/app.log?method=DELETE").queryString("method=DELETE"), 403, BoundaryReason.SIMPLE_REQUEST_WITHOUT_ORIGIN);
	}

	@Test void b05_methodOverrideHeaderIsRefused() throws Exception {
		assertRefused(filter(), req("GET", "/config").header("X-HTTP-Method-Override", "PUT"), 403, BoundaryReason.SIMPLE_REQUEST_WITHOUT_ORIGIN);
	}

	@Test void b06_nonBrowserJsonShutdownIsAdmitted() throws Exception {
		assertPasses(filter(), req("POST", "/shutdown").header("Content-Type", JSON));
	}

	@Test void b07_nonBrowserConfigPutIsAdmitted() throws Exception {
		assertPasses(filter(), req("PUT", "/config/S/k").header("Content-Type", "text/plain"));
	}

	@Test void b08_nonBrowserLogDeleteIsAdmitted() throws Exception {
		assertPasses(filter(), req("DELETE", "/logs/old.log"));
	}

	@Test void b09_readsPass() throws Exception {
		assertPasses(filter(), req("GET", "/logs/view/app.log").header("Origin", EVIL));
		assertPasses(filter(), req("GET", "/config"));
	}

	@Test void b10_pageLoadDoesNotCreateASession() throws Exception {
		var req = req("GET", "/config");
		assertPasses(filter(), req);
		assertNull(req.getSession(false), "A read must not mint a session");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c - allowed hosts
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_unlistedHostIsRefusedEvenForReads() throws Exception {
		var f = filter("app.example.com");
		assertRefused(f, req("GET", "/config"), 421, BoundaryReason.HOST_NOT_ALLOWED);
		assertRefused(f, req("PUT", "/config/S/k").header("Content-Type", "text/plain"), 421, BoundaryReason.HOST_NOT_ALLOWED);
	}

	@Test void c02_listedHostPasses() throws Exception {
		assertPasses(filter("localhost"), req("PUT", "/config/S/k").header("Content-Type", "text/plain"));
	}

	private static MockServletRequest at(String method, String host) {
		return new MockServletRequest().method(method).uri("/config").header("Host", host).httpSession(null);
	}

	@Test void c03_noHostsRefusesARebindingHost() throws Exception {
		assertRefused(filter(), at("GET", "rebound.example"), 421, BoundaryReason.HOST_NOT_ALLOWED);
		assertRefused(filter(), at("GET", "rebound.example:10000"), 421, BoundaryReason.HOST_NOT_ALLOWED);
		assertRefused(filter(), at("PUT", "rebound.example:10000").header("Content-Type", "text/plain"), 421, BoundaryReason.HOST_NOT_ALLOWED);
	}

	@Test void c04_noHostsAdmitsLoopback() throws Exception {
		for (var h : new String[] { "localhost", "localhost:10000", "LOCALHOST:10000", "127.0.0.1", "127.0.0.1:10000", "[::1]", "[::1]:10000" }) {
			assertPasses(filter(), at("GET", h));
			assertPasses(filter(), at("PUT", h).header("Content-Type", "text/plain"));
		}
	}

	@Test void c05_givenHostsReplaceLoopback() throws Exception {
		var f = filter("app.example.com");
		assertRefused(f, at("GET", "127.0.0.1:10000"), 421, BoundaryReason.HOST_NOT_ALLOWED);
		assertPasses(f, at("GET", "app.example.com:10000"));
	}

	@Test void c06_loopbackHosts() {
		assertEquals(List.of("localhost", "127.0.0.1", "[::1]"), AdminBoundary.LOOPBACK_HOSTS);
		var b = (CsrfBoundary)((RequestBoundaryFilter)filter()).boundary();
		assertEquals(Set.of("localhost", "127.0.0.1", "[::1]"), b.allowedHosts());
	}
}
