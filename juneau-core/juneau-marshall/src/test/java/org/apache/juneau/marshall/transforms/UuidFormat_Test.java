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
package org.apache.juneau.marshall.transforms;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

class UuidFormat_Test {

	private static final UUID U = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
	private static final String STD = "550e8400-e29b-41d4-a716-446655440000";
	private static final String COMPACT = "550e8400e29b41d4a716446655440000";
	private static final String URN = "urn:uuid:550e8400-e29b-41d4-a716-446655440000";

	@Test void a01_format_standard() {
		assertEquals(STD, UuidFormat.format(U, UuidFormat.STANDARD));
	}

	@Test void a02_format_notSetFallsThroughToStandard() {
		assertEquals(STD, UuidFormat.format(U, UuidFormat.NOT_SET));
	}

	@Test void a03_format_nullFormatTreatedAsStandard() {
		assertEquals(STD, UuidFormat.format(U, null));
	}

	@Test void a04_format_noDashes() {
		assertEquals(COMPACT, UuidFormat.format(U, UuidFormat.NO_DASHES));
	}

	@Test void a05_format_urn() {
		assertEquals(URN, UuidFormat.format(U, UuidFormat.URN));
	}

	@Test void a06_format_null() {
		for (var f : UuidFormat.values())
			assertNull(UuidFormat.format(null, f), "format=" + f);
	}

	@Test void a07_isNumeric_alwaysFalse() {
		for (var f : UuidFormat.values())
			assertFalse(f.isNumeric(), "format=" + f);
	}

	@Test void b01_parse_standard() {
		assertEquals(U, UuidFormat.parse(STD, UuidFormat.STANDARD));
	}

	@Test void b02_parse_compact() {
		assertEquals(U, UuidFormat.parse(COMPACT, UuidFormat.NO_DASHES));
	}

	@Test void b03_parse_urn() {
		assertEquals(U, UuidFormat.parse(URN, UuidFormat.URN));
	}

	@Test void b04_parse_lenient_anyHintAcceptsAnyShape() {
		// Format-agnostic lenient parsing: every hint accepts every wire shape.
		for (var hint : UuidFormat.values()) {
			assertEquals(U, UuidFormat.parse(STD, hint), "hint=" + hint);
			assertEquals(U, UuidFormat.parse(COMPACT, hint), "hint=" + hint);
			assertEquals(U, UuidFormat.parse(URN, hint), "hint=" + hint);
		}
	}

	@Test void b05_parse_urnPrefixCaseInsensitive() {
		assertEquals(U, UuidFormat.parse("URN:UUID:" + STD, UuidFormat.URN));
		assertEquals(U, UuidFormat.parse("Urn:Uuid:" + STD, UuidFormat.STANDARD));
	}

	@Test void b06_parse_nullAndBlank() {
		for (var f : UuidFormat.values()) {
			assertNull(UuidFormat.parse(null, f), "format=" + f);
			assertNull(UuidFormat.parse("", f), "format=" + f);
			assertNull(UuidFormat.parse("   ", f), "format=" + f);
		}
	}

	@Test void b07_parse_nullHintTreatedAsLenient() {
		assertEquals(U, UuidFormat.parse(STD, null));
		assertEquals(U, UuidFormat.parse(COMPACT, null));
	}

	@Test void b08_parse_invalidThrows() {
		assertThrows(IllegalArgumentException.class, () -> UuidFormat.parse("not-a-uuid", UuidFormat.STANDARD));
	}

	@Test void b09_parse_trimsWhitespace() {
		assertEquals(U, UuidFormat.parse("  " + STD + "  ", UuidFormat.STANDARD));
	}

	@Test void c01_roundTrip_everyFormat() {
		var fresh = UUID.randomUUID();
		for (var f : UuidFormat.values()) {
			var s = UuidFormat.format(fresh, f);
			assertEquals(fresh, UuidFormat.parse(s, f), "format=" + f);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Branch coverage on parse() — short input and 32-char-with-dash edge cases
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_parse_shortInputBelowUrnPrefixLength() {
		// Input shorter than URN_PREFIX (9 chars) — exercises the s.length() > URN_PREFIX.length() == false
		// arm before the URN prefix-strip check.  UUID.fromString rejects the short string and the helper
		// rethrows as IllegalArgumentException with a wrapped message.
		assertThrows(IllegalArgumentException.class, () -> UuidFormat.parse("abc", UuidFormat.STANDARD));
	}

	@Test void d02_parse_thirtyTwoCharWithDash_skipsCompactExpand() {
		// 32-character input that contains at least one dash — exercises the
		// `s.length() == 32 && indexOf('-') < 0` branch where the second conjunct is false, so the
		// compact-expand block is skipped.  Components after split do not match the standard
		// 8-4-4-4-12 shape, so UUID.fromString rejects and the helper rethrows.
		assertThrows(IllegalArgumentException.class, () -> UuidFormat.parse("abcdefgh-jklmnopqrstuvwxyz01234", UuidFormat.STANDARD));
	}
}
