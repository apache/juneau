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
 * Tests for {@link WwwAuthenticate} and {@link WwwAuthenticate.Builder}.
 */
class WwwAuthenticate_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// A — WwwAuthenticate header bean
	//------------------------------------------------------------------------------------------------------------------

	@Nested class A_bean extends TestBase {

		@Test void a01_name() {
			assertEquals("WWW-Authenticate", WwwAuthenticate.NAME);
		}

		@Test void a02_ofEager() {
			var x = WwwAuthenticate.of("Basic realm=\"x\"");
			assertEquals("WWW-Authenticate", x.getName());
			assertEquals("Basic realm=\"x\"", x.getValue());
		}

		@Test void a03_ofLazy() {
			assertEquals("Basic", WwwAuthenticate.of(() -> "Basic").getValue());
		}

		@Test void a04_constructors() {
			assertEquals("WWW-Authenticate", new WwwAuthenticate("x").getName());
			assertEquals("WWW-Authenticate", new WwwAuthenticate(() -> "x").getName());
		}

		@Test void a05_nullValue() {
			assertNull(WwwAuthenticate.of((String)null).getValue());
		}

		@Test void a06_factoryEntryPoints() {
			assertEquals("Basic realm=\"r\"", WwwAuthenticate.basic("r").build());
			assertEquals("Bearer", WwwAuthenticate.bearer().build());
			assertEquals("Digest", WwwAuthenticate.digest().build());
			assertNotNull(WwwAuthenticate.create());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// B — Scheme presets and typed params
	//------------------------------------------------------------------------------------------------------------------

	@Nested class B_challenge extends TestBase {

		@Test void b01_basic() {
			assertEquals("Basic realm=\"WallyWorld\"", WwwAuthenticate.create().basic("WallyWorld").build());
		}

		@Test void b02_basicWithCharset() {
			assertEquals("Basic realm=\"foo\", charset=\"UTF-8\"", WwwAuthenticate.create().basic("foo").charset("UTF-8").build());
		}

		@Test void b03_bearerEmpty() {
			assertEquals("Bearer", WwwAuthenticate.create().bearer().build());
		}

		@Test void b04_bearerWithError() {
			assertEquals(
				"Bearer realm=\"example\", error=\"invalid_token\", error_description=\"The access token expired\"",
				WwwAuthenticate.create().bearer().realm("example").error("invalid_token").errorDescription("The access token expired").build());
		}

		@Test void b05_digestFull() {
			assertEquals(
				"Digest realm=\"http-auth@example.org\", qop=\"auth,auth-int\", nonce=\"abc\", opaque=\"xyz\", algorithm=SHA-256",
				WwwAuthenticate.create().digest().realm("http-auth@example.org").qop("auth", "auth-int").nonce("abc").opaque("xyz").algorithm("SHA-256").build());
		}

		@Test void b06_stale() {
			assertEquals("Digest stale=true", WwwAuthenticate.create().digest().stale(true).build());
			assertEquals("Digest stale=false", WwwAuthenticate.create().digest().stale(false).build());
		}

		@Test void b07_domain() {
			assertEquals("Digest domain=\"/foo /bar\"", WwwAuthenticate.create().digest().domain("/foo", "/bar").build());
		}

		@Test void b08_schemeArbitrary() {
			assertEquals("Negotiate", WwwAuthenticate.create().scheme("Negotiate").build());
		}

		@Test void b09_paramReplaceKeepsPosition() {
			assertEquals("Basic realm=\"b\"", WwwAuthenticate.create().basic("a").realm("b").build());
		}

		@Test void b10_schemeOverwrites() {
			assertEquals("Bearer", WwwAuthenticate.create().scheme("Basic").bearer().build());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// C — Generic escape hatch + quoting
	//------------------------------------------------------------------------------------------------------------------

	@Nested class C_escapeHatch extends TestBase {

		@Test void c01_genericParamQuoted() {
			assertEquals("Digest custom=\"v\"", WwwAuthenticate.create().digest().param("custom", "v").build());
		}

		@Test void c02_quotingEscapesQuoteAndBackslash() {
			assertEquals("Basic realm=\"a\\\"b\\\\c\"", WwwAuthenticate.create().scheme("Basic").realm("a\"b\\c").build());
		}

		@Test void c03_paramNameTrimmed() {
			assertEquals("Basic x=\"y\"", WwwAuthenticate.create().scheme("Basic").param("  x  ", "y").build());
		}

		@Test void c04_emptyBuild() {
			assertEquals("", WwwAuthenticate.create().build());
		}

		@Test void c05_paramsWithoutScheme() {
			assertEquals("realm=\"r\"", WwwAuthenticate.create().realm("r").build());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// D — Validation
	//------------------------------------------------------------------------------------------------------------------

	@Nested class D_validation extends TestBase {

		@Test void d01_nullSchemeThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "value", () -> WwwAuthenticate.create().scheme(null));
		}

		@Test void d02_blankSchemeThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "scheme must not be blank", () -> WwwAuthenticate.create().scheme(" "));
		}

		@Test void d03_nullParamNameThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "name", () -> WwwAuthenticate.create().param(null, "v"));
		}

		@Test void d04_blankParamNameThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "auth-param name must not be blank", () -> WwwAuthenticate.create().param("  ", "v"));
		}

		@Test void d05_nullParamValueThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "value", () -> WwwAuthenticate.create().realm(null));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// E — toString / toHeader round-trips
	//------------------------------------------------------------------------------------------------------------------

	@Nested class E_rendering extends TestBase {

		@Test void e01_toStringEqualsBuild() {
			var b = WwwAuthenticate.create().basic("x");
			assertEquals(b.build(), b.toString());
		}

		@Test void e02_toHeader() {
			var h = WwwAuthenticate.create().basic("x").toHeader();
			assertEquals("WWW-Authenticate", h.getName());
			assertEquals("Basic realm=\"x\"", h.getValue());
			assertEquals("WWW-Authenticate: Basic realm=\"x\"", h.toString());
		}
	}
}
