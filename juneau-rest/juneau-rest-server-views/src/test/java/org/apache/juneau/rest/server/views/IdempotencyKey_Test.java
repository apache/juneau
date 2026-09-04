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
package org.apache.juneau.rest.server.views;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Test for the server-minted, {@code (action, targetId)}-bound {@link IdempotencyKey} (design doc §6.2; the
 * idempotency half of the row-action contract, HIGH-8).
 *
 * <p>
 * The security property is the <b>binding</b>, not the width: a key submitted against a different action or target
 * than it was minted for must be a refusal, never a replayed success.
 */
class IdempotencyKey_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Minting: unforgeable width + binding
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_mint_isBoundToActionAndTarget() {
		var k = IdempotencyKey.mint("ack", "INC-1");
		assertEquals("ack", k.action());
		assertEquals("INC-1", k.targetId());
		assertNotNull(k.value());
		assertFalse(k.value().isBlank());
	}

	@Test void a02_mint_valueIs256BitsHex() {
		// 32 bytes -> 64 lowercase hex chars.
		var v = IdempotencyKey.mint("ack", "INC-1").value();
		assertEquals(64, v.length(), v);
		assertTrue(v.matches("[0-9a-f]{64}"), v);
	}

	@Test void a03_mint_valuesAreUnique() {
		var seen = Stream.generate(() -> IdempotencyKey.mint("ack", "INC-1").value()).limit(50).collect(Collectors.toSet());
		assertEquals(50, seen.size(), "minted key values must be unique (SecureRandom)");
	}

	@Test void a04_mint_blankArgsThrow() {
		assertThrows(IllegalArgumentException.class, () -> IdempotencyKey.mint(null, "x"));
		assertThrows(IllegalArgumentException.class, () -> IdempotencyKey.mint("a", "  "));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Self-targeted minting: a key for a submit that has no artifact to bind to yet
	//------------------------------------------------------------------------------------------------------------------

	@Test void a05_mintSelfTargeted_isBoundToItsOwnValue() {
		// The whole point: at mint time there is no artifact id (the submit CREATES the thing), so the key binds to
		// itself.  targetId == value BY CONSTRUCTION - not by a convention a caller has to remember to honour.
		var k = IdempotencyKey.mintSelfTargeted("create-project");
		assertEquals("create-project", k.action());
		assertEquals(k.value(), k.targetId());
		assertTrue(k.matches("create-project", k.value()));
	}

	@Test void a06_mintSelfTargeted_hasTheSameUnforgeableWidthAsMint() {
		// Same 256-bit width and same lowercase-hex alphabet as mint(...): the fixed-width property is what makes a
		// key unguessable, and a self-bound key is no less security-relevant for being self-bound.
		var v = IdempotencyKey.mintSelfTargeted("create-project").value();
		assertEquals(64, v.length(), v);
		assertTrue(v.matches("[0-9a-f]{64}"), v);
	}

	@Test void a07_mintSelfTargeted_valuesAreUnique() {
		// Freshness matters more here than for mint(...): the key IS the target, so a repeated value would make two
		// distinct creates look like one replayed create.
		var seen = Stream.generate(() -> IdempotencyKey.mintSelfTargeted("create-project").value()).limit(50)
			.collect(Collectors.toSet());
		assertEquals(50, seen.size(), "self-targeted key values must be unique (SecureRandom)");
	}

	@Test void a08_mintSelfTargeted_stillRefusesAForeignTarget() {
		// Self-bound does NOT mean unbound: the binding check is exactly as strict, so a key replayed against a real
		// artifact id (or another key's value) is still a refusal.
		var k = IdempotencyKey.mintSelfTargeted("create-project");
		assertFalse(k.matches("create-project", "INC-1"));
		assertFalse(k.matches("create-project", IdempotencyKey.mintSelfTargeted("create-project").value()));
		assertFalse(k.matches("delete-project", k.value()));
	}

	@Test void a09_mintSelfTargeted_blankActionThrows() {
		assertThrows(IllegalArgumentException.class, () -> IdempotencyKey.mintSelfTargeted(null));
		assertThrows(IllegalArgumentException.class, () -> IdempotencyKey.mintSelfTargeted("  "));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Binding check (HIGH-8): mismatch is a refusal, never a replayed success
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_matches_onlyForSameActionAndTarget() {
		var k = IdempotencyKey.of("deadbeef", "ack", "INC-1");
		assertTrue(k.matches("ack", "INC-1"));
	}

	@Test void b02_matches_falseForDifferentTarget() {
		// The A-replayed-under-B attack: same key/action, different target -> must NOT match.
		var k = IdempotencyKey.of("deadbeef", "ack", "INC-1");
		assertFalse(k.matches("ack", "INC-2"));
	}

	@Test void b03_matches_falseForDifferentAction() {
		var k = IdempotencyKey.of("deadbeef", "ack", "INC-1");
		assertFalse(k.matches("resolve", "INC-1"));
	}

	@Test void b04_matches_falseForNulls() {
		var k = IdempotencyKey.of("deadbeef", "ack", "INC-1");
		assertFalse(k.matches(null, "INC-1"));
		assertFalse(k.matches("ack", null));
	}

	@Test void b05_of_rehydratesRecordedBinding() {
		var k = IdempotencyKey.of("v", "ack", "INC-1");
		assertEquals("v", k.value());
		assertEquals("ack", k.action());
		assertEquals("INC-1", k.targetId());
	}

	@Test void b06_of_blankArgsThrow() {
		assertThrows(IllegalArgumentException.class, () -> IdempotencyKey.of("  ", "a", "t"));
		assertThrows(IllegalArgumentException.class, () -> IdempotencyKey.of("v", null, "t"));
		assertThrows(IllegalArgumentException.class, () -> IdempotencyKey.of("v", "a", "  "));
	}

	//------------------------------------------------------------------------------------------------------------------
	// The value is a secret - toString must not leak it
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_toString_redactsValue() {
		var k = IdempotencyKey.of("super-secret-value", "ack", "INC-1");
		var s = k.toString();
		assertFalse(s.contains("super-secret-value"), s);
		assertTrue(s.contains("<redacted>"), s);
		assertTrue(s.contains("ack"), s);
		assertTrue(s.contains("INC-1"), s);
	}
}
