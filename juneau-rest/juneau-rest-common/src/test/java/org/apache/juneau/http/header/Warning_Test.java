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
package org.apache.juneau.http.header;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link Warning} and {@link Warning.Builder}.
 */
class Warning_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// A — Warning header bean
	//------------------------------------------------------------------------------------------------------------------

	@Nested class A_bean extends TestBase {

		@Test void a01_name() {
			assertEquals("Warning", Warning.NAME);
		}

		@Test void a02_ofEager() {
			var x = Warning.of("110 - \"stale\"");
			assertEquals("Warning", x.getName());
			assertEquals("110 - \"stale\"", x.getValue());
		}

		@Test void a03_ofLazy() {
			assertEquals("199 - \"x\"", Warning.of(() -> "199 - \"x\"").getValue());
		}

		@Test void a04_nullValue() {
			assertNull(Warning.of((String)null).getValue());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// B — Warning values
	//------------------------------------------------------------------------------------------------------------------

	@Nested class B_warnings extends TestBase {

		@Test void b01_single() {
			assertEquals("110 anderson/1.3.37 \"Response is stale\"",
				Warning.create().warning(110, "anderson/1.3.37", "Response is stale").build());
		}

		@Test void b02_withDate() {
			assertEquals("112 - \"cache down\" \"Wed, 21 Oct 2015 07:28:00 GMT\"",
				Warning.create().warning(112, "-", "cache down", "Wed, 21 Oct 2015 07:28:00 GMT").build());
		}

		@Test void b03_multiple() {
			assertEquals("110 - \"stale\", 214 proxy.example \"Transformation applied\"",
				Warning.create().warning(110, "-", "stale").warning(214, "proxy.example", "Transformation applied").build());
		}

		@Test void b04_codeZeroPadded() {
			assertEquals("099 - \"x\"", Warning.create().warning(99, "-", "x").build());
		}

		@Test void b05_textQuotingEscapes() {
			assertEquals("110 - \"a\\\"b\"", Warning.create().warning(110, "-", "a\"b").build());
		}

		@Test void b06_textQuotingEscapesBackslash() {
			assertEquals("110 - \"a\\\\b\"", Warning.create().warning(110, "-", "a\\b").build());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// C — Generic escape hatch
	//------------------------------------------------------------------------------------------------------------------

	@Nested class C_escapeHatch extends TestBase {

		@Test void c01_addRaw() {
			assertEquals("110 - \"stale\"", Warning.create().add("110 - \"stale\"").build());
		}

		@Test void c02_addTrimmed() {
			assertEquals("110 - \"stale\"", Warning.create().add("  110 - \"stale\"  ").build());
		}

		@Test void c03_emptyBuild() {
			assertEquals("", Warning.create().build());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// D — Validation
	//------------------------------------------------------------------------------------------------------------------

	@Nested class D_validation extends TestBase {

		@Test void d01_codeOutOfRangeThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "warn-code must be in the range 0-999", () -> Warning.create().warning(1000, "-", "x"));
		}

		@Test void d01b_negativeCodeThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "warn-code must be in the range 0-999", () -> Warning.create().warning(-1, "-", "x"));
		}

		@Test void d02_blankAgentThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "warn-agent must not be blank", () -> Warning.create().warning(110, " ", "x"));
		}

		@Test void d03_nullTextThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "text", () -> Warning.create().warning(110, "-", null));
		}

		@Test void d04_blankAddThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "warning value must not be blank", () -> Warning.create().add("  "));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// E — toString / toHeader round-trips
	//------------------------------------------------------------------------------------------------------------------

	@Nested class E_rendering extends TestBase {

		@Test void e01_toStringEqualsBuild() {
			var b = Warning.create().warning(110, "-", "stale");
			assertEquals(b.build(), b.toString());
		}

		@Test void e02_toHeader() {
			var h = Warning.create().warning(110, "-", "stale").toHeader();
			assertEquals("Warning", h.getName());
			assertEquals("110 - \"stale\"", h.getValue());
			assertEquals("Warning: 110 - \"stale\"", h.toString());
		}
	}
}
