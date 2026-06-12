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
 * Tests for {@link Forwarded} and {@link Forwarded.Builder}.
 */
class Forwarded_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// A — Forwarded header bean
	//------------------------------------------------------------------------------------------------------------------

	@Nested class A_bean extends TestBase {

		@Test void a01_name() {
			assertEquals("Forwarded", Forwarded.NAME);
		}

		@Test void a02_ofEager() {
			var x = Forwarded.of("for=192.0.2.60");
			assertEquals("Forwarded", x.getName());
			assertEquals("for=192.0.2.60", x.getValue());
		}

		@Test void a03_ofLazy() {
			assertEquals("for=x", Forwarded.of(() -> "for=x").getValue());
		}

		@Test void a04_constructors() {
			assertEquals("Forwarded", new Forwarded("x").getName());
			assertEquals("Forwarded", new Forwarded(() -> "x").getName());
		}

		@Test void a05_nullValue() {
			assertNull(Forwarded.of((String)null).getValue());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// B — Typed parameters + multiple elements
	//------------------------------------------------------------------------------------------------------------------

	@Nested class B_elements extends TestBase {

		@Test void b01_singleElement() {
			assertEquals("for=192.0.2.60;proto=http;by=203.0.113.43",
				Forwarded.create().forValue("192.0.2.60").proto("http").by("203.0.113.43").build());
		}

		@Test void b02_multipleHops() {
			assertEquals("for=192.0.2.60;proto=http;by=203.0.113.43, for=198.51.100.17",
				Forwarded.create().forValue("192.0.2.60").proto("http").by("203.0.113.43").next().forValue("198.51.100.17").build());
		}

		@Test void b03_host() {
			assertEquals("host=example.com", Forwarded.create().host("example.com").build());
		}

		@Test void b04_ipv6Quoted() {
			assertEquals("for=\"[2001:db8:cafe::17]:4711\"", Forwarded.create().forValue("[2001:db8:cafe::17]:4711").build());
		}

		@Test void b05_paramReplaceKeepsPosition() {
			assertEquals("for=b", Forwarded.create().forValue("a").forValue("b").build());
		}

		@Test void b06_trailingEmptyElementOmitted() {
			assertEquals("for=a", Forwarded.create().forValue("a").next().build());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// C — Generic escape hatch + quoting
	//------------------------------------------------------------------------------------------------------------------

	@Nested class C_escapeHatch extends TestBase {

		@Test void c01_genericParam() {
			assertEquals("secret=value", Forwarded.create().param("secret", "value").build());
		}

		@Test void c02_quotingEscapes() {
			assertEquals("for=\"a\\\"b\"", Forwarded.create().forValue("a\"b").build());
		}

		@Test void c03_paramNameTrimmed() {
			assertEquals("for=a", Forwarded.create().param("  for  ", "a").build());
		}

		@Test void c04_emptyBuild() {
			assertEquals("", Forwarded.create().build());
		}

		@Test void c05_uppercaseAndPunctuationTokenRendersBare() {
			assertEquals("for=ABCdef~|*", Forwarded.create().forValue("ABCdef~|*").build());
		}

		@Test void c06_emptyValueQuoted() {
			assertEquals("for=\"\"", Forwarded.create().forValue("").build());
		}

		@Test void c07_backslashQuotedAndEscaped() {
			assertEquals("for=\"a\\\\b\"", Forwarded.create().forValue("a\\b").build());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// D — Validation
	//------------------------------------------------------------------------------------------------------------------

	@Nested class D_validation extends TestBase {

		@Test void d01_nullParamNameThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "name", () -> Forwarded.create().param(null, "v"));
		}

		@Test void d02_blankParamNameThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "forwarded parameter name must not be blank", () -> Forwarded.create().param("  ", "v"));
		}

		@Test void d03_nullValueThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "value", () -> Forwarded.create().forValue(null));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// E — toString / toHeader round-trips
	//------------------------------------------------------------------------------------------------------------------

	@Nested class E_rendering extends TestBase {

		@Test void e01_toStringEqualsBuild() {
			var b = Forwarded.create().forValue("192.0.2.60");
			assertEquals(b.build(), b.toString());
		}

		@Test void e02_toHeader() {
			var h = Forwarded.create().forValue("192.0.2.60").toHeader();
			assertEquals("Forwarded", h.getName());
			assertEquals("for=192.0.2.60", h.getValue());
			assertEquals("Forwarded: for=192.0.2.60", h.toString());
		}
	}
}
