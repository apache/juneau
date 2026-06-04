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
package org.apache.juneau.rest.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * Tests for {@link AuthFilterChain} — pattern matching, first-success principal, role aggregation,
 * all-failure aggregation, pass-through on no match, and pass-through when no filters apply.
 *
 * @since 10.0.0
 */
class AuthFilterChain_Test extends TestBase {

	private static final Principal ALICE = () -> "alice";
	private static final Principal BOB = () -> "bob";

	// -----------------------------------------------------------------------------------------
	// Helpers to build mock HTTP objects.
	// -----------------------------------------------------------------------------------------

	/** Creates a mock request for the given URI path. Context path is empty. */
	private static HttpServletRequest req(String path) {
		var req = mock(HttpServletRequest.class);
		when(req.getRequestURI()).thenReturn(path);
		when(req.getContextPath()).thenReturn("");
		return req;
	}

	/** Captures the request passed to chain.doFilter() so tests can assert on it. */
	private static class CapturingChain implements FilterChain {
		ServletRequest captured;

		@Override
		public void doFilter(ServletRequest req, ServletResponse resp) {
			captured = req;
		}
	}

	/** Writable response that captures status and body. */
	private static HttpServletResponse capturingResponse() throws IOException {
		var resp = mock(HttpServletResponse.class);
		var sw = new StringWriter();
		when(resp.getWriter()).thenReturn(new PrintWriter(sw));
		return resp;
	}

	/** An AuthFilter that always returns the given AuthResult. */
	private static AuthFilter succeeds(Principal principal, String... roles) {
		return new AuthFilter() {
			@Override public Optional<AuthResult> authenticate(HttpServletRequest req) {
				return Optional.of(AuthResult.of(principal, roles));
			}
		};
	}

	/** An AuthFilter that always returns empty (doesn't apply). */
	private static AuthFilter empty() {
		return new AuthFilter() {
			@Override public Optional<AuthResult> authenticate(HttpServletRequest req) {
				return Optional.empty();
			}
		};
	}

	/** An AuthFilter that always throws AuthenticationException with the given message. */
	private static AuthFilter fails(String message) {
		return new AuthFilter() {
			@Override public Optional<AuthResult> authenticate(HttpServletRequest req) throws AuthenticationException {
				throw new AuthenticationException(message).wwwAuthenticate("Bearer realm=\"test\"");
			}
		};
	}

	// -----------------------------------------------------------------------------------------
	// Tests.
	// -----------------------------------------------------------------------------------------

	@Test void a01_noMatchingFilters_passThroughUnchanged() throws Exception {
		var original = req("/public/page");
		var chain = AuthFilterChain.create(null)
			.append(succeeds(ALICE), "/api/*")
			.build();
		var capturing = new CapturingChain();
		chain.doFilter(original, capturingResponse(), capturing);
		// No filter matched /public/page — the original request passed through
		assertSame(original, capturing.captured);
	}

	@Test void a02_singleFilterSuccess_wrapsRequest() throws Exception {
		var chain = AuthFilterChain.create(null)
			.append(succeeds(ALICE, "user"))
			.build();
		var capturing = new CapturingChain();
		chain.doFilter(req("/anything"), capturingResponse(), capturing);
		assertInstanceOf(AuthenticatedRequestWrapper.class, capturing.captured);
		var w = (AuthenticatedRequestWrapper) capturing.captured;
		assertSame(ALICE, w.getUserPrincipal());
		assertTrue(w.isUserInRole("user"));
	}

	@Test void a03_patternMatch_onlyMatchingFilterRuns() throws Exception {
		var called = new AtomicBoolean(false);
		AuthFilter shouldNotRun = new AuthFilter() {
			@Override public Optional<AuthResult> authenticate(HttpServletRequest req) {
				called.set(true);
				return Optional.empty();
			}
		};
		var chain = AuthFilterChain.create(null)
			.append(shouldNotRun, "/api/*")
			.append(succeeds(ALICE, "user"), "/public/*")
			.build();
		var capturing = new CapturingChain();
		chain.doFilter(req("/public/page"), capturingResponse(), capturing);
		assertFalse(called.get(), "Filter for /api/* must not be called for /public/page");
		assertInstanceOf(AuthenticatedRequestWrapper.class, capturing.captured);
	}

	@Test void a04_firstSuccessPrincipalWins() throws Exception {
		var chain = AuthFilterChain.create(null)
			.append(succeeds(ALICE, "user"))
			.append(succeeds(BOB, "admin"))
			.build();
		var capturing = new CapturingChain();
		chain.doFilter(req("/"), capturingResponse(), capturing);
		var w = (AuthenticatedRequestWrapper) capturing.captured;
		// ALICE registered first — her principal must win
		assertSame(ALICE, w.getUserPrincipal());
	}

	@Test void a05_roleAggregation_unionAcrossSuccessfulFilters() throws Exception {
		var chain = AuthFilterChain.create(null)
			.append(succeeds(ALICE, "user"))
			.append(succeeds(BOB, "admin", "billing"))
			.build();
		var capturing = new CapturingChain();
		chain.doFilter(req("/"), capturingResponse(), capturing);
		var w = (AuthenticatedRequestWrapper) capturing.captured;
		// All roles from all successful filters must be present
		assertTrue(w.isUserInRole("user"));
		assertTrue(w.isUserInRole("admin"));
		assertTrue(w.isUserInRole("billing"));
	}

	@Test void a06_allMatchingFiltersFail_returns401() throws Exception {
		var resp = capturingResponse();
		var chain = AuthFilterChain.create(null)
			.append(fails("token bad"))
			.append(fails("key bad"))
			.build();
		var capturing = new CapturingChain();
		chain.doFilter(req("/"), resp, capturing);
		// doFilter must NOT have forwarded the request
		assertNull(capturing.captured);
		verify(resp).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	}

	@Test void a07_allMatchingFiltersReturnEmpty_passThroughUnchanged() throws Exception {
		var original = req("/");
		var chain = AuthFilterChain.create(null)
			.append(empty())
			.append(empty())
			.build();
		var capturing = new CapturingChain();
		chain.doFilter(original, capturingResponse(), capturing);
		// All filters returned empty — no credentials at all; pass through
		assertSame(original, capturing.captured);
	}

	@Test void a08_mixedEmptyAndSuccess_successWins() throws Exception {
		var chain = AuthFilterChain.create(null)
			.append(empty())
			.append(succeeds(ALICE, "user"))
			.build();
		var capturing = new CapturingChain();
		chain.doFilter(req("/"), capturingResponse(), capturing);
		assertInstanceOf(AuthenticatedRequestWrapper.class, capturing.captured);
		assertSame(ALICE, ((AuthenticatedRequestWrapper) capturing.captured).getUserPrincipal());
	}

	@Test void a09_mixedFailureAndEmpty_failureWins_401() throws Exception {
		var resp = capturingResponse();
		var chain = AuthFilterChain.create(null)
			.append(empty())
			.append(fails("bad creds"))
			.build();
		var capturing = new CapturingChain();
		chain.doFilter(req("/"), resp, capturing);
		assertNull(capturing.captured);
		verify(resp).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	}

	@Test void a10_patternMatchWithContextPath_stripsContextPath() throws Exception {
		var req = mock(HttpServletRequest.class);
		when(req.getRequestURI()).thenReturn("/myapp/api/users");
		when(req.getContextPath()).thenReturn("/myapp");

		var chain = AuthFilterChain.create(null)
			.append(succeeds(ALICE, "user"), "/api/*")
			.build();
		var capturing = new CapturingChain();
		chain.doFilter(req, capturingResponse(), capturing);
		assertInstanceOf(AuthenticatedRequestWrapper.class, capturing.captured);
	}

	@Test void a11_successThenFailure_successWins_failureIgnored() throws Exception {
		var chain = AuthFilterChain.create(null)
			.append(succeeds(ALICE, "user"))
			.append(fails("second filter failed"))
			.build();
		var capturing = new CapturingChain();
		chain.doFilter(req("/"), capturingResponse(), capturing);
		// At least one filter succeeded; failure from later filter is ignored
		assertInstanceOf(AuthenticatedRequestWrapper.class, capturing.captured);
		assertSame(ALICE, ((AuthenticatedRequestWrapper) capturing.captured).getUserPrincipal());
	}

	@Test void a12_wwwAuthenticateAggregatedOnAllFailure() throws Exception {
		var resp = mock(HttpServletResponse.class);
		var sw = new StringWriter();
		when(resp.getWriter()).thenReturn(new PrintWriter(sw));

		AuthFilter f1 = new AuthFilter() {
			@Override public Optional<AuthResult> authenticate(HttpServletRequest req) throws AuthenticationException {
				throw new AuthenticationException("msg1").wwwAuthenticate("Bearer realm=\"r1\"");
			}
		};
		AuthFilter f2 = new AuthFilter() {
			@Override public Optional<AuthResult> authenticate(HttpServletRequest req) throws AuthenticationException {
				throw new AuthenticationException("msg2").wwwAuthenticate("ApiKey realm=\"r2\"");
			}
		};

		var chain = AuthFilterChain.create(null).append(f1).append(f2).build();
		var capturing = new CapturingChain();
		chain.doFilter(req("/"), resp, capturing);
		verify(resp).setStatus(401);
		// Both WWW-Authenticate challenges must appear in the aggregated header
		var wwwCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
		verify(resp).setHeader(eq("WWW-Authenticate"), wwwCaptor.capture());
		var headerValue = wwwCaptor.getValue();
		assertTrue(headerValue.contains("Bearer realm=\"r1\""), "Missing Bearer challenge: " + headerValue);
		assertTrue(headerValue.contains("ApiKey realm=\"r2\""), "Missing ApiKey challenge: " + headerValue);
	}
}
