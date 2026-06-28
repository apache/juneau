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

import java.security.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Tests for {@link RestAuthenticator} adapter ({@link RestAuthenticator#of(Authenticator)}) and the
 * {@link RestAuthenticator.Null} sentinel.
 *
 * <p>
 * A mock {@link HttpServletRequest} is not a {@link org.apache.juneau.rest.server.RestRequest}, so these tests
 * exercise the package-private {@code authenticateRaw} entry point that the adapter / sentinel override.
 *
 * @since 10.0.0
 */
class RestAuthenticator_Test extends TestBase {

	private static final Principal ALICE = () -> "alice";

	private static HttpServletRequest req() {
		return mock(HttpServletRequest.class);
	}

	@Test void a01_ofAdaptsAuthenticator() throws Exception {
		Authenticator inner = r -> opt(AuthResult.of(ALICE, "admin"));
		var ra = RestAuthenticator.of(inner);
		var result = ra.authenticateRaw(req()).orElseThrow();
		assertSame(ALICE, result.getPrincipal());
		assertTrue(result.getRoles().contains("admin"));
	}

	@Test void a02_nullSentinelReturnsEmpty() throws Exception {
		var ra = new RestAuthenticator.Null();
		assertTrue(ra.authenticateRaw(req()).isEmpty());
		assertTrue(ra.authenticate(null).isEmpty());
	}

	@Test void a03_ofPropagatesAuthenticationException() {
		Authenticator inner = r -> { throw new AuthenticationException("bad"); };
		var ra = RestAuthenticator.of(inner);
		assertThrows(AuthenticationException.class, () -> ra.authenticateRaw(req()));
	}

	@Test void a04_ofEmptyResult() throws Exception {
		Authenticator inner = r -> opte();
		var ra = RestAuthenticator.of(inner);
		assertTrue(ra.authenticateRaw(req()).isEmpty());
	}

	@Test void a05_baseAuthenticateRawDelegatesToRestRequestForm() throws Exception {
		// A subclass that does NOT override authenticateRaw exercises the base delegate (cast → authenticate(RestRequest)).
		var ra = new RestAuthenticator() {
			@Override public Optional<AuthResult> authenticate(RestRequest r) {
				return opt(AuthResult.of(ALICE, "user"));
			}
		};
		var result = ra.authenticateRaw(mock(RestRequest.class)).orElseThrow();
		assertSame(ALICE, result.getPrincipal());
		assertTrue(result.getRoles().contains("user"));
	}
}
