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
 * Tests for {@link ContentRange} and {@link ContentRange.Builder}.
 */
class ContentRange_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// A — ContentRange header bean
	//------------------------------------------------------------------------------------------------------------------

	@Nested class A_bean extends TestBase {

		@Test void a01_name() {
			assertEquals("Content-Range", ContentRange.NAME);
		}

		@Test void a02_ofEager() {
			var x = ContentRange.of("bytes 0-1023/2048");
			assertEquals("Content-Range", x.getName());
			assertEquals("bytes 0-1023/2048", x.getValue());
		}

		@Test void a03_ofLazy() {
			assertEquals("bytes 0-1/2", ContentRange.of(() -> "bytes 0-1/2").getValue());
		}

		@Test void a04_nullValue() {
			assertNull(ContentRange.of((String)null).getValue());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// B — Range + length forms
	//------------------------------------------------------------------------------------------------------------------

	@Nested class B_forms extends TestBase {

		@Test void b01_rangeAndLength() {
			assertEquals("bytes 0-1023/2048", ContentRange.create().range(0, 1023).length(2048).build());
		}

		@Test void b02_rangeUnknownLength() {
			assertEquals("bytes 0-1023/*", ContentRange.create().range(0, 1023).unknownLength().build());
		}

		@Test void b03_unsatisfiedRange() {
			assertEquals("bytes */2048", ContentRange.create().unsatisfiedRange().length(2048).build());
		}

		@Test void b04_customUnit() {
			assertEquals("items 0-9/100", ContentRange.create().unit("items").range(0, 9).length(100).build());
		}

		@Test void b05_unsatisfiedClearsRange() {
			assertEquals("bytes */2048", ContentRange.create().range(0, 1023).unsatisfiedRange().length(2048).build());
		}

		@Test void b06_unknownClearsLength() {
			assertEquals("bytes 0-1023/*", ContentRange.create().range(0, 1023).length(2048).unknownLength().build());
		}

		@Test void b07_defaults() {
			assertEquals("bytes */*", ContentRange.create().build());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// C — Validation
	//------------------------------------------------------------------------------------------------------------------

	@Nested class C_validation extends TestBase {

		@Test void c01_blankUnitThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "range unit must not be blank", () -> ContentRange.create().unit(" "));
		}

		@Test void c02_negativeStartThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "range start must be non-negative", () -> ContentRange.create().range(-1, 10));
		}

		@Test void c03_endBeforeStartThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "must be >= start", () -> ContentRange.create().range(10, 5));
		}

		@Test void c04_negativeLengthThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "complete-length must be non-negative", () -> ContentRange.create().length(-1));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// D — toString / toHeader round-trips
	//------------------------------------------------------------------------------------------------------------------

	@Nested class D_rendering extends TestBase {

		@Test void d01_toStringEqualsBuild() {
			var b = ContentRange.create().range(0, 1023).length(2048);
			assertEquals(b.build(), b.toString());
		}

		@Test void d02_toHeader() {
			var h = ContentRange.create().range(0, 1023).length(2048).toHeader();
			assertEquals("Content-Range", h.getName());
			assertEquals("bytes 0-1023/2048", h.getValue());
			assertEquals("Content-Range: bytes 0-1023/2048", h.toString());
		}
	}
}
