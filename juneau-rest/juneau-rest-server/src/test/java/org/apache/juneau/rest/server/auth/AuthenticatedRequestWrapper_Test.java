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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.security.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Tests for {@link AuthenticatedRequestWrapper}.
 *
 * @since 10.0.0
 */
class AuthenticatedRequestWrapper_Test extends TestBase {

	private static final Principal ALICE = () -> "alice";
	private static final Principal BOB = () -> "bob";

	private static HttpServletRequest mockReq() {
		return mock(HttpServletRequest.class);
	}

	@Test void a01_getUserPrincipal() {
		var a = new AuthenticatedRequestWrapper(mockReq(), AuthResult.of(ALICE, "user"));
		assertSame(ALICE, a.getUserPrincipal());
	}

	@Test void a02_isUserInRole_known() {
		var a = new AuthenticatedRequestWrapper(mockReq(), AuthResult.of(ALICE, "user", "admin"));
		assertTrue(a.isUserInRole("user"));
		assertTrue(a.isUserInRole("admin"));
	}

	@Test void a03_isUserInRole_unknown() {
		var a = new AuthenticatedRequestWrapper(mockReq(), AuthResult.of(ALICE, "user"));
		assertFalse(a.isUserInRole("admin"));
		assertFalse(a.isUserInRole(""));
	}

	@Test void a04_getRemoteUser() {
		var a = new AuthenticatedRequestWrapper(mockReq(), AuthResult.of(ALICE));
		assertEquals("alice", a.getRemoteUser());
	}

	@Test void a05_getAttribute_principalAttr() {
		var a = new AuthenticatedRequestWrapper(mockReq(), AuthResult.of(ALICE));
		assertSame(ALICE, a.getAttribute(RestServerConstants.PRINCIPAL_ATTR));
	}

	@Test void a06_getAttribute_otherKey_delegatesToWrapped() {
		var req = mockReq();
		when(req.getAttribute("x-foo")).thenReturn("bar");
		var a = new AuthenticatedRequestWrapper(req, AuthResult.of(ALICE));
		assertEquals("bar", a.getAttribute("x-foo"));
	}

	@Test void a07_withAdditionalRoles() {
		var a = new AuthenticatedRequestWrapper(mockReq(), AuthResult.of(ALICE, "user"));
		a.withAdditionalRoles(Set.of("admin", "billing"));
		assertTrue(a.isUserInRole("user"));
		assertTrue(a.isUserInRole("admin"));
		assertTrue(a.isUserInRole("billing"));
	}

	@Test void a08_withAdditionalRoles_differentPrincipal_principalUnchanged() {
		// withAdditionalRoles only adds roles; principal is always the first-success principal
		var a = new AuthenticatedRequestWrapper(mockReq(), AuthResult.of(ALICE, "user"));
		a.withAdditionalRoles(Set.of("admin"));
		assertSame(ALICE, a.getUserPrincipal());
		assertEquals("alice", a.getRemoteUser());
	}

	@Test void a09_emptyRoles() {
		var a = new AuthenticatedRequestWrapper(mockReq(), AuthResult.of(BOB));
		assertFalse(a.isUserInRole("user"));
		assertEquals("bob", a.getRemoteUser());
	}
}
