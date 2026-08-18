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
package org.apache.juneau.rest.server.filter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link SynchronizerToken}.
 *
 * @since 10.0.0
 */
class SynchronizerToken_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// a) Generation
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_generatedTokenIs256BitsOfHex() {
		var t = SynchronizerToken.generate();
		assertEquals(64, t.value().length(), "32 random bytes, hex-encoded");
		assertTrue(t.value().matches("[0-9a-f]{64}"), t::value);
	}

	@Test void a02_generatedTokensAreDistinct() {
		var seen = new HashSet<String>();
		for (var i = 0; i < 200; i++)
			assertTrue(seen.add(SynchronizerToken.generate().value()), "generate() produced a repeat");
	}

	@Test void a03_ofWrapsACallerSuppliedValue() {
		assertEquals("abc", SynchronizerToken.of("abc").value());
	}

	@Test void a04_ofRejectsNullAndBlank() {
		assertThrows(IllegalArgumentException.class, () -> SynchronizerToken.of(null));
		assertThrows(IllegalArgumentException.class, () -> SynchronizerToken.of(""));
		assertThrows(IllegalArgumentException.class, () -> SynchronizerToken.of("   "));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b) Matching
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_matchesItsOwnValue() {
		var t = SynchronizerToken.generate();
		assertTrue(t.matches(t.value()));
	}

	@Test void b02_doesNotMatchAnotherInstancesValue() {
		assertFalse(SynchronizerToken.generate().matches(SynchronizerToken.generate().value()));
	}

	@Test void b03_doesNotMatchNullOrEmpty() {
		var t = SynchronizerToken.of("abc");
		assertFalse(t.matches(null));
		assertFalse(t.matches(""));
	}

	@Test void b04_doesNotMatchAPrefixOrSuffix() {
		var t = SynchronizerToken.of("abcdef");
		assertFalse(t.matches("abc"));
		assertFalse(t.matches("abcdefg"));
		assertFalse(t.matches(" abcdef"));
		assertFalse(t.matches("abcdef "));
	}

	@Test void b05_matchingIsCaseSensitive() {
		assertFalse(SynchronizerToken.of("abcdef").matches("ABCDEF"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c) The value is a secret and must not leak through stringification
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_toStringDoesNotIncludeTheValue() {
		var t = SynchronizerToken.of("super-secret-value");
		assertFalse(t.toString().contains("super-secret-value"),
			() -> "toString() must not leak the token: " + t);
		assertTrue(t.toString().contains("redacted"), t::toString);
	}
}
