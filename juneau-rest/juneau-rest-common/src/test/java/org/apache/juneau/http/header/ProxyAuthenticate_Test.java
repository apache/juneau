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
 * Tests for {@link ProxyAuthenticate} and {@link ProxyAuthenticate.Builder} (twin of {@link WwwAuthenticate}).
 */
class ProxyAuthenticate_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// A — ProxyAuthenticate header bean
	//------------------------------------------------------------------------------------------------------------------

	@Nested class A_bean extends TestBase {

		@Test void a01_name() {
			assertEquals("Proxy-Authenticate", ProxyAuthenticate.NAME);
		}

		@Test void a02_ofEager() {
			var x = ProxyAuthenticate.of("Basic realm=\"x\"");
			assertEquals("Proxy-Authenticate", x.getName());
			assertEquals("Basic realm=\"x\"", x.getValue());
		}

		@Test void a03_ofLazy() {
			assertEquals("Basic", ProxyAuthenticate.of(() -> "Basic").getValue());
		}

		@Test void a04_constructors() {
			assertEquals("Proxy-Authenticate", new ProxyAuthenticate("x").getName());
			assertEquals("Proxy-Authenticate", new ProxyAuthenticate(() -> "x").getName());
		}

		@Test void a05_nullValue() {
			assertNull(ProxyAuthenticate.of((String)null).getValue());
		}

		@Test void a06_factoryEntryPoints() {
			assertEquals("Basic realm=\"r\"", ProxyAuthenticate.basic("r").build());
			assertEquals("Bearer", ProxyAuthenticate.bearer().build());
			assertEquals("Digest", ProxyAuthenticate.digest().build());
			assertNotNull(ProxyAuthenticate.create());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// B — Scheme presets and typed params (must render identically to WwwAuthenticate)
	//------------------------------------------------------------------------------------------------------------------

	@Nested class B_challenge extends TestBase {

		@Test void b01_basic() {
			assertEquals("Basic realm=\"WallyWorld\"", ProxyAuthenticate.create().basic("WallyWorld").build());
		}

		@Test void b02_basicWithCharset() {
			assertEquals("Basic realm=\"foo\", charset=\"UTF-8\"", ProxyAuthenticate.create().basic("foo").charset("UTF-8").build());
		}

		@Test void b03_bearerEmpty() {
			assertEquals("Bearer", ProxyAuthenticate.create().bearer().build());
		}

		@Test void b04_bearerWithError() {
			assertEquals(
				"Bearer realm=\"example\", error=\"invalid_token\", error_description=\"The access token expired\"",
				ProxyAuthenticate.create().bearer().realm("example").error("invalid_token").errorDescription("The access token expired").build());
		}

		@Test void b05_digestFull() {
			assertEquals(
				"Digest realm=\"http-auth@example.org\", qop=\"auth,auth-int\", nonce=\"abc\", opaque=\"xyz\", algorithm=SHA-256",
				ProxyAuthenticate.create().digest().realm("http-auth@example.org").qop("auth", "auth-int").nonce("abc").opaque("xyz").algorithm("SHA-256").build());
		}

		@Test void b06_stale() {
			assertEquals("Digest stale=true", ProxyAuthenticate.create().digest().stale(true).build());
			assertEquals("Digest stale=false", ProxyAuthenticate.create().digest().stale(false).build());
		}

		@Test void b07_domain() {
			assertEquals("Digest domain=\"/foo /bar\"", ProxyAuthenticate.create().digest().domain("/foo", "/bar").build());
		}

		@Test void b08_schemeArbitrary() {
			assertEquals("Negotiate", ProxyAuthenticate.create().scheme("Negotiate").build());
		}

		@Test void b09_paramReplaceKeepsPosition() {
			assertEquals("Basic realm=\"b\"", ProxyAuthenticate.create().basic("a").realm("b").build());
		}

		@Test void b10_schemeOverwrites() {
			assertEquals("Bearer", ProxyAuthenticate.create().scheme("Basic").bearer().build());
		}

		@Test void b11_twinRendersIdenticallyToWww() {
			var proxy = ProxyAuthenticate.create().digest().realm("r").qop("auth").nonce("n").algorithm("MD5").stale(true).build();
			var www = WwwAuthenticate.create().digest().realm("r").qop("auth").nonce("n").algorithm("MD5").stale(true).build();
			assertEquals(www, proxy);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// C — Generic escape hatch + quoting
	//------------------------------------------------------------------------------------------------------------------

	@Nested class C_escapeHatch extends TestBase {

		@Test void c01_genericParamQuoted() {
			assertEquals("Digest custom=\"v\"", ProxyAuthenticate.create().digest().param("custom", "v").build());
		}

		@Test void c02_quotingEscapesQuoteAndBackslash() {
			assertEquals("Basic realm=\"a\\\"b\\\\c\"", ProxyAuthenticate.create().scheme("Basic").realm("a\"b\\c").build());
		}

		@Test void c03_paramNameTrimmed() {
			assertEquals("Basic x=\"y\"", ProxyAuthenticate.create().scheme("Basic").param("  x  ", "y").build());
		}

		@Test void c04_emptyBuild() {
			assertEquals("", ProxyAuthenticate.create().build());
		}

		@Test void c05_paramsWithoutScheme() {
			assertEquals("realm=\"r\"", ProxyAuthenticate.create().realm("r").build());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// D — Validation
	//------------------------------------------------------------------------------------------------------------------

	@Nested class D_validation extends TestBase {

		@Test void d01_nullSchemeThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "value", () -> ProxyAuthenticate.create().scheme(null));
		}

		@Test void d02_blankSchemeThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "scheme must not be blank", () -> ProxyAuthenticate.create().scheme(" "));
		}

		@Test void d03_nullParamNameThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "name", () -> ProxyAuthenticate.create().param(null, "v"));
		}

		@Test void d04_blankParamNameThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "auth-param name must not be blank", () -> ProxyAuthenticate.create().param("  ", "v"));
		}

		@Test void d05_nullParamValueThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "value", () -> ProxyAuthenticate.create().realm(null));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// E — toString / toHeader round-trips
	//------------------------------------------------------------------------------------------------------------------

	@Nested class E_rendering extends TestBase {

		@Test void e01_toStringEqualsBuild() {
			var b = ProxyAuthenticate.create().basic("x");
			assertEquals(b.build(), b.toString());
		}

		@Test void e02_toHeader() {
			var h = ProxyAuthenticate.create().basic("x").toHeader();
			assertEquals("Proxy-Authenticate", h.getName());
			assertEquals("Basic realm=\"x\"", h.getValue());
			assertEquals("Proxy-Authenticate: Basic realm=\"x\"", h.toString());
		}
	}
}
