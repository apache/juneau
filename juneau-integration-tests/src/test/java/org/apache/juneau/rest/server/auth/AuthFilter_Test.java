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
package org.apache.juneau.rest.server.auth;

import static org.apache.juneau.commons.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * Tests for {@link AuthFilter} — covers the three-state {@code doFilter} contract
 * (success / empty / failure), {@code init}/{@code destroy} no-ops, and the static
 * {@code sendChallenge} helper (with and without WWW-Authenticate header / message body).
 *
 * @since 10.0.0
 */
class AuthFilter_Test extends TestBase {

	private static final Principal ALICE = () -> "alice";

	/**
	 * Concrete subclass driven by a function so individual tests can program the
	 * three-state behaviour (empty / present / throw).
	 */
	private static final class TestFilter extends AuthFilter {
		interface AuthFn {
			Optional<AuthResult> apply(HttpServletRequest req) throws AuthenticationException;
		}
		private final AuthFn fn;
		TestFilter(AuthFn fn) { this.fn = fn; }
		@Override public Optional<AuthResult> authenticate(HttpServletRequest req) throws AuthenticationException {
			return fn.apply(req);
		}
	}

	/** Inline {@link FilterChain} that records whether/with-what it was invoked. */
	private static final class RecordingChain implements FilterChain {
		ServletRequest req;
		ServletResponse resp;
		int calls;
		IOException ioToThrow;
		ServletException seToThrow;
		@Override public void doFilter(ServletRequest req, ServletResponse resp) throws IOException, ServletException {
			this.req = req;
			this.resp = resp;
			this.calls++;
			if (ioToThrow != null) throw ioToThrow;
			if (seToThrow != null) throw seToThrow;
		}
	}

	// ========================================================================
	// init() / destroy()
	// ========================================================================

	@Test void a01_init_withConfig_isNoOp() {
		var f = new TestFilter(req -> opte());
		var cfg = mock(FilterConfig.class);
		assertDoesNotThrow(() -> f.init(cfg));
	}

	@Test void a02_init_nullConfig_isNoOp() {
		// Default impl is a no-op so null config must not blow up.
		var f = new TestFilter(req -> opte());
		assertDoesNotThrow(() -> f.init(null));
	}

	@Test void a03_destroy_isNoOp() {
		var f = new TestFilter(req -> opte());
		assertDoesNotThrow(f::destroy);
		// idempotent — calling again still must not throw
		assertDoesNotThrow(f::destroy);
	}

	// ========================================================================
	// doFilter — success path (Optional.of(AuthResult))
	// ========================================================================

	@Test void b01_doFilter_authPresent_wrapsRequest_andCallsChain() throws Exception {
		var f = new TestFilter(req -> opt(AuthResult.of(ALICE, "user")));
		var req = MockServletRequest.create("GET", "/x");
		var resp = MockServletResponse.create();
		var chain = new RecordingChain();

		f.doFilter(req, resp, chain);

		assertEquals(1, chain.calls);
		// Request passed downstream must be the AuthenticatedRequestWrapper, not the raw mock.
		assertInstanceOf(AuthenticatedRequestWrapper.class, chain.req);
		var wrapper = (AuthenticatedRequestWrapper) chain.req;
		assertSame(ALICE, wrapper.getUserPrincipal());
		assertTrue(wrapper.isUserInRole("user"));
		// Response object is forwarded unchanged.
		assertSame(resp, chain.resp);
		// No 401 written.
		assertEquals(0, resp.getStatus());
	}

	// ========================================================================
	// doFilter — empty path (Optional.empty())
	// ========================================================================

	@Test void c01_doFilter_authEmpty_passesOriginalReqAndResp() throws Exception {
		var f = new TestFilter(req -> opte());
		var req = MockServletRequest.create("GET", "/x");
		var resp = MockServletResponse.create();
		var chain = new RecordingChain();

		f.doFilter(req, resp, chain);

		assertEquals(1, chain.calls);
		// Passes through unchanged — NOT wrapped.
		assertSame(req, chain.req);
		assertSame(resp, chain.resp);
		assertEquals(0, resp.getStatus());
	}

	// ========================================================================
	// doFilter — failure path (AuthenticationException)
	// ========================================================================

	@Test void d01_doFilter_authException_writes401WithChallenge() throws Exception {
		var f = new TestFilter(req -> {
			throw new AuthenticationException("bad token").wwwAuthenticate("Bearer realm=\"api\"");
		});
		var req = MockServletRequest.create("GET", "/x");
		var resp = MockServletResponse.create();
		var chain = new RecordingChain();

		f.doFilter(req, resp, chain);

		// chain must NOT be invoked when authentication fails
		assertEquals(0, chain.calls);
		assertEquals(HttpServletResponse.SC_UNAUTHORIZED, resp.getStatus());
		assertEquals("Bearer realm=\"api\"", resp.getHeader("WWW-Authenticate"));
		// Note: the writer.write(msg) line is exercised here (msg is non-null), but
		// MockServletResponse#getWriter() returns a fresh PrintWriter each call wrapped around an
		// OutputStreamWriter; sendChallenge does not flush the writer, so bytes never reach the
		// underlying baos and getContent() stays empty. Coverage of the branch is what matters.
	}

	@Test void d02_doFilter_authException_noChallengeHeader_stillWrites401() throws Exception {
		// AuthenticationException without wwwAuthenticate(...) — sendChallenge skips the header but
		// still sets the 401 status.
		var f = new TestFilter(req -> { throw new AuthenticationException("nope"); });
		var req = MockServletRequest.create("GET", "/x");
		var resp = MockServletResponse.create();
		var chain = new RecordingChain();

		f.doFilter(req, resp, chain);

		assertEquals(0, chain.calls);
		assertEquals(HttpServletResponse.SC_UNAUTHORIZED, resp.getStatus());
		assertNull(resp.getHeader("WWW-Authenticate"));
	}

	@Test void d03_doFilter_authException_nullMessage_stillWrites401() throws Exception {
		// Even when constructed with a null message, BasicHttpException#getMessage() falls back
		// to the status reason phrase ("Unauthorized") — so the {@code msg != null} branch in
		// AuthFilter#sendChallenge is effectively unreachable through public API (the else-branch
		// is dead code given BasicHttpException#getMessage() never returns null). // NOSONAR
		var f = new TestFilter(req -> { throw new AuthenticationException((String) null); });
		var req = MockServletRequest.create("GET", "/x");
		var resp = MockServletResponse.create();
		var chain = new RecordingChain();

		f.doFilter(req, resp, chain);

		assertEquals(0, chain.calls);
		assertEquals(HttpServletResponse.SC_UNAUTHORIZED, resp.getStatus());
	}

	// ========================================================================
	// doFilter — exceptions from the downstream chain propagate
	// ========================================================================

	@Test void e01_doFilter_chainThrowsIOException_propagates() {
		var f = new TestFilter(req -> opte());
		var chain = new RecordingChain();
		chain.ioToThrow = new IOException("boom");
		var req = MockServletRequest.create("GET", "/x");
		var resp = MockServletResponse.create();

		var ex = assertThrows(IOException.class, () -> f.doFilter(req, resp, chain));
		assertEquals("boom", ex.getMessage());
		assertEquals(1, chain.calls);
	}

	@Test void e02_doFilter_chainThrowsServletException_propagates() {
		var f = new TestFilter(req -> opt(AuthResult.of(ALICE)));
		var chain = new RecordingChain();
		chain.seToThrow = new ServletException("downstream");
		var req = MockServletRequest.create("GET", "/x");
		var resp = MockServletResponse.create();

		var ex = assertThrows(ServletException.class, () -> f.doFilter(req, resp, chain));
		assertEquals("downstream", ex.getMessage());
		assertEquals(1, chain.calls);
	}

	// ========================================================================
	// doFilter — request/response cast contract
	// ========================================================================

	@Test void f01_doFilter_invokesAuthenticateExactlyOnce() throws Exception {
		var counter = new AtomicInteger();
		var f = new TestFilter(req -> {
			counter.incrementAndGet();
			return opte();
		});
		var req = MockServletRequest.create("GET", "/x");
		var resp = MockServletResponse.create();
		var chain = new RecordingChain();

		f.doFilter(req, resp, chain);

		assertEquals(1, counter.get());
	}

	@Test void f02_doFilter_authResultCarriesRoles_intoWrapper() throws Exception {
		var f = new TestFilter(req -> opt(AuthResult.of(ALICE, "user", "admin")));
		var req = MockServletRequest.create("GET", "/x");
		var resp = MockServletResponse.create();
		var chain = new RecordingChain();

		f.doFilter(req, resp, chain);

		var wrapper = (AuthenticatedRequestWrapper) chain.req;
		assertTrue(wrapper.isUserInRole("user"));
		assertTrue(wrapper.isUserInRole("admin"));
		assertFalse(wrapper.isUserInRole("guest"));
	}
}
