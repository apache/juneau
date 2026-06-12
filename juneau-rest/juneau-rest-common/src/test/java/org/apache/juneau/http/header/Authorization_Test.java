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
 * Tests for {@link Authorization} and {@link Authorization.Builder}.
 */
class Authorization_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// A — Authorization header bean
	//------------------------------------------------------------------------------------------------------------------

	@Nested class A_bean extends TestBase {

		@Test void a01_name() {
			assertEquals("Authorization", Authorization.NAME);
		}

		@Test void a02_ofEager() {
			var x = Authorization.of("Bearer abc");
			assertEquals("Authorization", x.getName());
			assertEquals("Bearer abc", x.getValue());
		}

		@Test void a03_ofLazy() {
			assertEquals("Bearer abc", Authorization.of(() -> "Bearer abc").getValue());
		}

		@Test void a04_constructors() {
			assertEquals("Authorization", new Authorization("x").getName());
			assertEquals("Authorization", new Authorization(() -> "x").getName());
		}

		@Test void a05_nullValue() {
			assertNull(Authorization.of((String)null).getValue());
		}

		@Test void a06_factoryEntryPoints() {
			assertEquals("Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==", Authorization.basic("Aladdin", "open sesame").build());
			assertEquals("Bearer tok", Authorization.bearer("tok").build());
			assertEquals("Digest", Authorization.digest().build());
			assertNotNull(Authorization.create());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// B — token68 credentials forms
	//------------------------------------------------------------------------------------------------------------------

	@Nested class B_token68 extends TestBase {

		@Test void b01_basicBase64() {
			assertEquals("Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==", Authorization.create().basic("Aladdin", "open sesame").build());
		}

		@Test void b02_bearer() {
			assertEquals("Bearer mF_9.B5f-4.1JqM", Authorization.create().bearer("mF_9.B5f-4.1JqM").build());
		}

		@Test void b03_genericToken() {
			assertEquals("Negotiate YII", Authorization.create().scheme("Negotiate").token("YII").build());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// C — Digest auth-params
	//------------------------------------------------------------------------------------------------------------------

	@Nested class C_digest extends TestBase {

		@Test void c01_digestFull() {
			assertEquals(
				"Digest username=\"Mufasa\", realm=\"http-auth@example.org\", uri=\"/dir/index.html\", qop=auth, nc=00000001, response=\"abc\"",
				Authorization.create().digest().username("Mufasa").realm("http-auth@example.org").uri("/dir/index.html").qop("auth").nc("00000001").response("abc").build());
		}

		@Test void c02_digestOptionalParams() {
			assertEquals(
				"Digest nonce=\"n\", cnonce=\"c\", opaque=\"o\", algorithm=MD5",
				Authorization.create().digest().nonce("n").cnonce("c").opaque("o").algorithm("MD5").build());
		}

		@Test void c03_paramReplaceKeepsPosition() {
			assertEquals("Digest username=\"b\"", Authorization.create().digest().username("a").username("b").build());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// D — Mutual exclusion + generic escape hatch
	//------------------------------------------------------------------------------------------------------------------

	@Nested class D_escapeHatch extends TestBase {

		@Test void d01_genericParamQuoted() {
			assertEquals("Digest custom=\"v\"", Authorization.create().digest().param("custom", "v").build());
		}

		@Test void d02_quotingEscapes() {
			assertEquals("Digest username=\"a\\\"b\"", Authorization.create().digest().username("a\"b").build());
		}

		@Test void d02b_quotingEscapesBackslash() {
			assertEquals("Digest username=\"a\\\\b\"", Authorization.create().digest().username("a\\b").build());
		}

		@Test void d03_tokenClearsParams() {
			assertEquals("Bearer t", Authorization.create().digest().username("a").bearer("t").build());
		}

		@Test void d04_paramClearsToken() {
			assertEquals("Digest username=\"a\"", Authorization.create().bearer("t").scheme("Digest").username("a").build());
		}

		@Test void d05_emptyNoScheme() {
			assertEquals("", Authorization.create().build());
		}

		@Test void d06_schemeOnly() {
			assertEquals("Digest", Authorization.create().digest().build());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// E — Validation
	//------------------------------------------------------------------------------------------------------------------

	@Nested class E_validation extends TestBase {

		@Test void e01_nullSchemeThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "value", () -> Authorization.create().scheme(null));
		}

		@Test void e02_blankSchemeThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "scheme must not be blank", () -> Authorization.create().scheme(" "));
		}

		@Test void e03_nullParamNameThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "name", () -> Authorization.create().param(null, "v"));
		}

		@Test void e04_blankParamNameThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "auth-param name must not be blank", () -> Authorization.create().param("  ", "v"));
		}

		@Test void e05_basicNullUserThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "user", () -> Authorization.create().basic(null, "p"));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// F — toString / toHeader round-trips
	//------------------------------------------------------------------------------------------------------------------

	@Nested class F_rendering extends TestBase {

		@Test void f01_toStringEqualsBuild() {
			var b = Authorization.create().bearer("tok");
			assertEquals(b.build(), b.toString());
		}

		@Test void f02_toHeader() {
			var h = Authorization.create().bearer("tok").toHeader();
			assertEquals("Authorization", h.getName());
			assertEquals("Bearer tok", h.getValue());
			assertEquals("Authorization: Bearer tok", h.toString());
		}
	}
}
