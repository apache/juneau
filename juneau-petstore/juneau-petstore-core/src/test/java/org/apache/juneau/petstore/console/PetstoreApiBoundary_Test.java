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
package org.apache.juneau.petstore.console;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.settings.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.server.filter.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * The {@code /petstore} API's CSRF boundary, and the console boundary publishing the API's token on console pages.
 *
 * <p>
 * Every test drives the filter itself with mock servlet objects and a counting chain.  A mock REST client would not
 * do: it never runs a servlet filter, so a test built on it passes with no boundary installed.
 */
class PetstoreApiBoundary_Test extends TestBase {

	private static final String HOST = "localhost:8080";
	private static final String SELF = "http://localhost:8080";
	private static final String EVIL = "http://evil.example";
	private static final String JSON = "application/json";
	private static final String FORM = "application/x-www-form-urlencoded";
	private static final String REASON = "X-Request-Boundary";

	/** A request with the given method, path and {@code Host}, and no session. */
	private static CountingRequest req(String method, String uri, String host) {
		var r = new CountingRequest();
		r.method(method).uri(uri).header("Host", host).httpSession(null);
		return r;
	}

	/** Records whether a session was asked to be created. */
	private static final class CountingRequest extends MockServletRequest {
		final AtomicInteger createSession = new AtomicInteger();

		@Override /* HttpServletRequest */
		public HttpSession getSession(boolean create) {
			if (create)
				createSession.incrementAndGet();
			return super.getSession(create);
		}

		CountingRequest h(String name, String value) {
			header(name, value);
			return this;
		}
	}

	/** A chain that counts its calls. */
	private static final class Chain implements FilterChain {
		int calls;

		@Override /* FilterChain */
		public void doFilter(ServletRequest req, ServletResponse res) {
			calls++;
		}
	}

	private static Chain run(Filter f, HttpServletRequest req, MockServletResponse res) throws Exception {
		var chain = new Chain();
		f.doFilter(req, res, chain);
		return chain;
	}

	private static void assertRefused(Filter f, HttpServletRequest req, int status, BoundaryReason reason) throws Exception {
		var res = MockServletResponse.create();
		var chain = run(f, req, res);
		assertEquals(status, res.getStatus());
		assertString(reason.name(), res.getHeader(REASON));
		assertEquals(0, chain.calls);
	}

	private static void assertPasses(Filter f, HttpServletRequest req) throws Exception {
		var res = MockServletResponse.create();
		var chain = run(f, req, res);
		assertNull(res.getHeader(REASON), res.getHeader(REASON));
		assertEquals(1, chain.calls);
	}

	private static CountingRequest photoPut() {
		return req("PUT", "/petstore/pets/1/photo", HOST).h("Origin", SELF).h("Content-Type", "image/png");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - browser writes need the API's own token; non-browser writes and reads pass
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_tokenlessSameOriginWriteIsRefused() throws Exception {
		assertRefused(new PetstoreApiBoundary(), photoPut(), 403, BoundaryReason.CSRF_TOKEN_MISSING);
	}

	@Test void a02_writeWithTheMaskedTokenPasses() throws Exception {
		var api = new PetstoreApiBoundary();
		var req = photoPut();
		assertPasses(api, req.h("X-Csrf-Token", api.tokenFor(req)));
	}

	@Test void a03_consoleTokenIsNotAnApiToken() throws Exception {
		assertRefused(new PetstoreApiBoundary(), photoPut().h("X-Csrf-Token", SynchronizerToken.generate().value()), 403, BoundaryReason.CSRF_TOKEN_MISMATCH);
	}

	@Test void a04_crossOriginFormPostIsRefused() throws Exception {
		var req = req("POST", "/petstore/pets", HOST).h("Origin", EVIL).h("Content-Type", FORM);
		assertRefused(new PetstoreApiBoundary(), req, 403, BoundaryReason.ORIGIN_MISMATCH);
	}

	@Test void a05_simpleNonBrowserPostIsRefused() throws Exception {
		var req = req("POST", "/petstore/pets", HOST).h("Content-Type", FORM);
		assertRefused(new PetstoreApiBoundary(), req, 403, BoundaryReason.SIMPLE_REQUEST_WITHOUT_ORIGIN);
	}

	@Test void a06_nonBrowserJsonWriteIsAdmitted() throws Exception {
		assertPasses(new PetstoreApiBoundary(), req("POST", "/petstore/pets", HOST).h("Content-Type", JSON));
	}

	@Test void a07_safeReadIsAdmittedOnAnyHost() throws Exception {
		assertPasses(new PetstoreApiBoundary(), req("GET", "/petstore/pets", "192.0.2.7:1"));
	}

	@Test void a08_nullOriginIsRefused() throws Exception {
		var req = req("POST", "/petstore/pets", HOST).h("Origin", "null").h("Content-Type", JSON);
		assertRefused(new PetstoreApiBoundary(), req, 403, BoundaryReason.ORIGIN_NULL);
	}

	@Test void a09_forwardedHostIsIgnored() throws Exception {
		var api = new PetstoreApiBoundary();
		var req = photoPut().h("X-Forwarded-Host", "evil.example");
		assertPasses(api, req.h("X-Csrf-Token", api.tokenFor(req)));
		var req2 = req("PUT", "/petstore/pets/1/photo", HOST).h("Origin", EVIL).h("X-Forwarded-Host", "evil.example").h("Content-Type", "image/png");
		assertRefused(api, req2.h("X-Csrf-Token", api.tokenFor(req2)), 403, BoundaryReason.ORIGIN_MISMATCH);
	}

	@Test void a10_methodQueryOverrideIsJudgedAsAWrite() throws Exception {
		var req = req("GET", "/petstore/pets/1", HOST).h("Origin", SELF);
		req.queryString("method=DELETE");
		assertRefused(new PetstoreApiBoundary(), req, 403, BoundaryReason.CSRF_TOKEN_MISSING);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - the allowed-hosts setting and the token strategy
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_allowedHostsSettingRefusesOtherHosts() throws Exception {
		PetstoreApiBoundary api;
		Settings.get().setLocal(PetstoreApiBoundary.ALLOWED_HOSTS_PROPERTY, "shop.example");
		try {
			api = new PetstoreApiBoundary();
		} finally {
			Settings.get().unsetLocal(PetstoreApiBoundary.ALLOWED_HOSTS_PROPERTY);
		}
		assertRefused(api, req("GET", "/petstore/pets", "127.0.0.1:8080"), 421, BoundaryReason.HOST_NOT_ALLOWED);
		assertPasses(api, req("GET", "/petstore/pets", "shop.example:8080"));
	}

	@Test void b02_malformedAllowedHostsFailsAtConstruction() {
		Settings.get().setLocal(PetstoreApiBoundary.ALLOWED_HOSTS_PROPERTY, "shop.example:80");
		try {
			assertThrows(IllegalArgumentException.class, PetstoreApiBoundary::new);
		} finally {
			Settings.get().unsetLocal(PetstoreApiBoundary.ALLOWED_HOSTS_PROPERTY);
		}
	}

	@Test void b03_aTokenMintedWithoutASessionVerifiesOnARequestWithOne() throws Exception {
		var api = new PetstoreApiBoundary();
		var req1 = req("GET", "/console/pets/1/photo", HOST);
		var token = api.tokenFor(req1);
		var req2 = photoPut().h("X-Csrf-Token", token);
		req2.httpSession(MockHttpSession.create().id("x"));
		assertPasses(api, req2);
		assertEquals(0, req1.createSession.get());
		assertEquals(0, req2.createSession.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c - the console boundary publishes the API's token on the console requests it allows
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_consoleBoundaryPublishesTheApiTokenOnAllowedRequests() throws Exception {
		var api = new PetstoreApiBoundary();
		var req = req("GET", "/console/pets/1/photo", HOST);
		req.localPort(8080);
		assertPasses(new PetstoreConsoleBoundary(api), req);
		var token = (String)req.getAttribute(PetstoreApiBoundary.TOKEN_ATTRIBUTE);
		assertNotNull(token);
		assertTrue(token.matches("[0-9a-f]{128}"), token);
		var write = photoPut();
		assertPasses(api, write.h("X-Csrf-Token", token));
	}

	@Test void c02_consoleBoundaryPublishesNothingOnARefusedRequest() throws Exception {
		var req = req("GET", "/console/pets/1/photo", "127.0.0.1:8080");
		req.localPort(8080);
		assertRefused(new PetstoreConsoleBoundary(new PetstoreApiBoundary()), req, 421, BoundaryReason.HOST_MISMATCH);
		assertNull(req.getAttribute(PetstoreApiBoundary.TOKEN_ATTRIBUTE));
	}
}
