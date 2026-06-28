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
 * Tests for {@link AuthResultAccumulator} fold semantics ({@code ADD} union, {@code REPLACE} reset, roles-only).
 *
 * @since 10.0.0
 */
class AuthResultAccumulator_Test extends TestBase {

	private static final Principal ALICE = () -> "alice";
	private static final Principal BOB = () -> "bob";

	@Test void a01_addUnionsRoles_firstPrincipalWins() {
		var acc = new AuthResultAccumulator();
		acc.add(AuthResult.of(ALICE, "r1"));
		acc.add(AuthResult.of(BOB, "r2"));      // ADD: principal stays alice, roles union
		var r = acc.result().orElseThrow();
		assertSame(ALICE, r.getPrincipal());
		assertEquals(Set.of("r1", "r2"), r.getRoles());
	}

	@Test void a02_addNullPrincipalKeepsExisting() {
		var acc = new AuthResultAccumulator();
		acc.add(AuthResult.of(ALICE, "r1"));
		acc.add(AuthResult.ofRoles("r2"));     // null principal contributes only roles
		var r = acc.result().orElseThrow();
		assertSame(ALICE, r.getPrincipal());
		assertEquals(Set.of("r1", "r2"), r.getRoles());
	}

	@Test void a03_replaceResetsAccumulator() {
		var acc = new AuthResultAccumulator();
		acc.add(AuthResult.of(ALICE, "r1"));
		acc.add(AuthResult.replacing(BOB, "r2"));  // discard alice + r1
		var r = acc.result().orElseThrow();
		assertSame(BOB, r.getPrincipal());
		assertEquals(Set.of("r2"), r.getRoles());
	}

	@Test void a04_emptyWhenNothingAdded() {
		assertTrue(new AuthResultAccumulator().result().isEmpty());
	}

	@Test void a05_rolesOnlyNoPrincipalYieldsEmpty() {
		var acc = new AuthResultAccumulator();
		acc.add(AuthResult.ofRoles("r1"));   // roles but never a principal
		assertTrue(acc.result().isEmpty());  // no identity => not a successful auth
	}

	@Test void a06_nullAddIgnored() {
		var acc = new AuthResultAccumulator();
		acc.add(null);
		assertTrue(acc.result().isEmpty());
	}

	@Test void a07_rolesOnlyThenPrincipalAdopts() {
		var acc = new AuthResultAccumulator();
		acc.add(AuthResult.ofRoles("r1"));        // null principal, role r1
		acc.add(AuthResult.of(ALICE, "r2"));      // adopts alice, unions r2
		var r = acc.result().orElseThrow();
		assertSame(ALICE, r.getPrincipal());
		assertEquals(Set.of("r1", "r2"), r.getRoles());
	}

	@Test void a08_replaceAfterMultipleAdds() {
		var acc = new AuthResultAccumulator();
		acc.add(AuthResult.of(ALICE, "r1"));
		acc.add(AuthResult.ofRoles("r2"));
		acc.add(AuthResult.replacing(BOB));   // discard everything, no roles
		var r = acc.result().orElseThrow();
		assertSame(BOB, r.getPrincipal());
		assertTrue(r.getRoles().isEmpty());
	}
}
