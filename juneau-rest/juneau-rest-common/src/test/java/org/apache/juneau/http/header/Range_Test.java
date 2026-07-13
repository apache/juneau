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

import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link Range} and {@link Range.Builder}.
 */
class Range_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// A — Range header bean
	//------------------------------------------------------------------------------------------------------------------

	@Nested class A_bean extends TestBase {

		@Test void a01_name() {
			assertEquals("Range", Range.NAME);
		}

		@Test void a02_ofEager() {
			var x = Range.of("bytes=0-499");
			assertEquals("Range", x.getName());
			assertEquals("bytes=0-499", x.getValue());
		}

		@Test void a03_ofLazy() {
			assertEquals("bytes=0-1", Range.of(() -> "bytes=0-1").getValue());
		}

		@Test void a04_nullValue() {
			assertNull(Range.of((String)null).getValue());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// B — Range spec forms
	//------------------------------------------------------------------------------------------------------------------

	@Nested class B_specs extends TestBase {

		@Test void b01_closedRange() {
			assertEquals("bytes=0-499", Range.create().range(0, 499).build());
		}

		@Test void b02_openEnded() {
			assertEquals("bytes=9500-", Range.create().from(9500).build());
		}

		@Test void b03_suffix() {
			assertEquals("bytes=-500", Range.create().suffix(500).build());
		}

		@Test void b04_multipleSpecs() {
			assertEquals("bytes=0-499,9500-,-200", Range.create().range(0, 499).from(9500).suffix(200).build());
		}

		@Test void b05_customUnit() {
			assertEquals("items=0-9", Range.create().unit("items").range(0, 9).build());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// C — Generic escape hatch
	//------------------------------------------------------------------------------------------------------------------

	@Nested class C_escapeHatch extends TestBase {

		@Test void c01_spec() {
			assertEquals("bytes=0-0,-1", Range.create().spec("0-0").spec("-1").build());
		}

		@Test void c02_specTrimmed() {
			assertEquals("bytes=0-499", Range.create().spec("  0-499  ").build());
		}

		@Test void c03_emptyBuild() {
			assertEquals("", Range.create().build());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// D — Validation
	//------------------------------------------------------------------------------------------------------------------

	@Nested class D_validation extends TestBase {

		@Test void d01_blankUnitThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "range unit must not be blank", () -> Range.create().unit(" "));
		}

		@Test void d02_negativeStartThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "range start must be non-negative", () -> Range.create().range(-1, 10));
		}

		@Test void d03_endBeforeStartThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "must be >= start", () -> Range.create().range(10, 5));
		}

		@Test void d04_negativeSuffixThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "suffix length must be non-negative", () -> Range.create().suffix(-1));
		}

		@Test void d04b_negativeFromThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "range start must be non-negative", () -> Range.create().from(-1));
		}

		@Test void d05_blankSpecThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "range spec must not be blank", () -> Range.create().spec("  "));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// E — toString / toHeader round-trips
	//------------------------------------------------------------------------------------------------------------------

	@Nested class E_rendering extends TestBase {

		@Test void e01_toStringEqualsBuild() {
			var b = Range.create().range(0, 499);
			assertEquals(b.build(), b.toString());
		}

		@Test void e02_toHeader() {
			var h = Range.create().range(0, 499).toHeader();
			assertEquals("Range", h.getName());
			assertEquals("bytes=0-499", h.getValue());
			assertEquals("Range: bytes=0-499", h.toString());
		}
	}
}
