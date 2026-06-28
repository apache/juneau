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

import java.security.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link AuthResult} factory methods, getters, and defensive-copy semantics.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S5778"  // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice
})
class AuthResult_Test extends TestBase {

	private static final Principal ALICE = () -> "alice";
	private static final Principal BOB = () -> "bob";

	@Test void a01_ofVarargs_principalAndRoles() {
		var a = AuthResult.of(ALICE, "user", "admin");
		assertSame(ALICE, a.getPrincipal());
		assertEquals(Set.of("user", "admin"), a.getRoles());
	}

	@Test void a02_ofVarargs_noRoles() {
		var a = AuthResult.of(ALICE);
		assertSame(ALICE, a.getPrincipal());
		assertTrue(a.getRoles().isEmpty());
	}

	@Test void a03_ofSet_principalAndRoles() {
		var roles = new HashSet<>(Set.of("r1", "r2"));
		var a = AuthResult.of(ALICE, roles);
		assertSame(ALICE, a.getPrincipal());
		assertEquals(Set.of("r1", "r2"), a.getRoles());
	}

	@Test void a04_ofSet_null() {
		var a = AuthResult.of(ALICE, (Set<String>) null);
		assertTrue(a.getRoles().isEmpty());
	}

	@Test void a05_rolesAreDefensiveCopy() {
		var mutable = new HashSet<>(Set.of("user"));
		var a = AuthResult.of(ALICE, mutable);
		mutable.add("admin");
		// Mutation of source set must not affect the result
		assertEquals(Set.of("user"), a.getRoles());
	}

	@Test void a06_rolesAreUnmodifiable() {
		var a = AuthResult.of(ALICE, "user");
		assertThrows(UnsupportedOperationException.class, () -> a.getRoles().add("admin"));
	}

	@Test void a07_nullPrincipalThrows() {
		assertThrows(Exception.class, () -> AuthResult.of(null, "user"));
	}

	@Test void b01_defaultModeIsAdd() {
		var x = AuthResult.of(ALICE, "admin", "user");
		assertEquals(AuthResult.MergeMode.ADD, x.getMode());
		assertSame(ALICE, x.getPrincipal());
		assertEquals(Set.of("admin", "user"), x.getRoles());
	}

	@Test void b02_ofRoles_nullPrincipalAddMode() {
		var x = AuthResult.ofRoles("reader");
		assertNull(x.getPrincipal());
		assertEquals(AuthResult.MergeMode.ADD, x.getMode());
		assertEquals(Set.of("reader"), x.getRoles());
	}

	@Test void b03_replacing_replaceMode() {
		var x = AuthResult.replacing(BOB, "admin");
		assertEquals(AuthResult.MergeMode.REPLACE, x.getMode());
		assertSame(BOB, x.getPrincipal());
		assertEquals(Set.of("admin"), x.getRoles());
	}

	@Test void b04_replacingRequiresPrincipal() {
		assertThrows(Exception.class, () -> AuthResult.replacing(null, "x"));
	}

	@Test void b05_ofRolesCollection() {
		var x = AuthResult.ofRoles(java.util.List.of("r1", "r2"));
		assertNull(x.getPrincipal());
		assertEquals(AuthResult.MergeMode.ADD, x.getMode());
		assertEquals(Set.of("r1", "r2"), x.getRoles());
	}

	@Test void b06_ofRolesNullArgs() {
		assertTrue(AuthResult.ofRoles((String[]) null).getRoles().isEmpty());
		assertTrue(AuthResult.ofRoles((java.util.Collection<String>) null).getRoles().isEmpty());
	}

	@Test void b07_replacingSetAndNullRoles() {
		var x = AuthResult.replacing(BOB, Set.of("admin"));
		assertEquals(AuthResult.MergeMode.REPLACE, x.getMode());
		assertEquals(Set.of("admin"), x.getRoles());
		assertTrue(AuthResult.replacing(BOB, (Set<String>) null).getRoles().isEmpty());
	}
}
