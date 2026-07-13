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

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.security.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Tests proving {@link AuthFilter} is an {@link Authenticator} and that the functional-interface contract holds.
 *
 * @since 10.0.0
 */
class Authenticator_Test extends TestBase {

	private static final Principal ALICE = () -> "alice";

	@Test void a01_authFilterIsAuthenticator() throws Exception {
		AuthFilter f = new AuthFilter() {
			@Override public Optional<AuthResult> authenticate(HttpServletRequest req) {
				return o(AuthResult.of(ALICE, "admin"));
			}
		};
		Authenticator a = f;  // must compile
		assertTrue(a.authenticate(null).isPresent());
		assertSame(ALICE, a.authenticate(null).orElseThrow().getPrincipal());
	}

	@Test void a02_lambdaAuthenticator() throws Exception {
		Authenticator a = req -> o(AuthResult.ofRoles("reader"));
		assertEquals(Set.of("reader"), a.authenticate(null).orElseThrow().getRoles());
	}

	@Test void a03_emptyAndThrowContracts() {
		Authenticator empty = req -> oe();
		assertTrue(empty.authenticate(null).isEmpty());
		Authenticator bad = req -> { throw new AuthenticationException("nope"); };
		assertThrows(AuthenticationException.class, () -> bad.authenticate(null));
	}
}
