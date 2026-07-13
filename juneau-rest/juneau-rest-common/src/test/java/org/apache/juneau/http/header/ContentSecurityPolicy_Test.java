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
import static org.apache.juneau.http.header.ContentSecurityPolicy.Builder.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link ContentSecurityPolicy} and {@link ContentSecurityPolicy.Builder}.
 */
class ContentSecurityPolicy_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// A — ContentSecurityPolicy header bean
	//------------------------------------------------------------------------------------------------------------------

	@Nested class A_bean extends TestBase {

		@Test void a01_names() {
			assertEquals("Content-Security-Policy", ContentSecurityPolicy.NAME);
			assertEquals("Content-Security-Policy-Report-Only", ContentSecurityPolicy.REPORT_ONLY_NAME);
		}

		@Test void a02_ofEager() {
			var x = ContentSecurityPolicy.of("default-src 'self'");
			assertEquals("Content-Security-Policy", x.getName());
			assertEquals("default-src 'self'", x.getValue());
		}

		@Test void a03_ofLazy() {
			var x = ContentSecurityPolicy.of(() -> "default-src 'self'");
			assertEquals("Content-Security-Policy", x.getName());
			assertEquals("default-src 'self'", x.getValue());
		}

		@Test void a04_ofReportOnlyEager() {
			var x = ContentSecurityPolicy.ofReportOnly("default-src 'self'");
			assertEquals("Content-Security-Policy-Report-Only", x.getName());
			assertEquals("default-src 'self'", x.getValue());
		}

		@Test void a05_ofReportOnlyLazy() {
			var x = ContentSecurityPolicy.ofReportOnly(() -> "default-src 'self'");
			assertEquals("Content-Security-Policy-Report-Only", x.getName());
			assertEquals("default-src 'self'", x.getValue());
		}

		@Test void a06_constructors() {
			assertEquals("Content-Security-Policy", new ContentSecurityPolicy("x").getName());
			assertEquals("Content-Security-Policy", new ContentSecurityPolicy(() -> "x").getName());
		}

		@Test void a07_nullValue() {
			assertNull(ContentSecurityPolicy.of((String)null).getValue());
		}

		@Test void a08_factoryEntryPoints() {
			assertNotNull(ContentSecurityPolicy.create());
			assertNotNull(ContentSecurityPolicy.strictStarter());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// B — Typed directive setters
	//------------------------------------------------------------------------------------------------------------------

	@Nested class B_directives extends TestBase {

		@Test void b01_allTypedDirectives() {
			var x = create()
				.defaultSrc(SELF)
				.scriptSrc(SELF, "https:")
				.styleSrc(SELF)
				.imgSrc(SELF, "data:")
				.connectSrc(SELF)
				.fontSrc(SELF)
				.objectSrc(NONE)
				.baseUri(SELF)
				.frameAncestors(SELF)
				.reportUri("/csp-report")
				.reportTo("csp-endpoint")
				.build();
			assertEquals(
				"default-src 'self'; script-src 'self' https:; style-src 'self'; img-src 'self' data:; "
				+ "connect-src 'self'; font-src 'self'; object-src 'none'; base-uri 'self'; "
				+ "frame-ancestors 'self'; report-uri /csp-report; report-to csp-endpoint",
				x);
		}

		@Test void b02_insertionOrderPreserved() {
			var x = create().objectSrc(NONE).defaultSrc(SELF).build();
			assertEquals("object-src 'none'; default-src 'self'", x);
		}

		@Test void b03_resetReplacesSources() {
			var x = create().scriptSrc(SELF).scriptSrc(NONE).build();
			assertEquals("script-src 'none'", x);
		}

		@Test void b04_resetPreservesPosition() {
			var x = create().scriptSrc(SELF).styleSrc(SELF).scriptSrc(NONE).build();
			assertEquals("script-src 'none'; style-src 'self'", x);
		}

		@Test void b05_genericDirective() {
			var x = create().directive("worker-src", SELF, "blob:").build();
			assertEquals("worker-src 'self' blob:", x);
		}

		@Test void b06_bareDirectiveNoSources() {
			var x = create().directive("upgrade-insecure-requests").build();
			assertEquals("upgrade-insecure-requests", x);
		}

		@Test void b07_emptyBuild() {
			assertEquals("", create().build());
		}

		@Test void b08_directiveNameTrimmed() {
			var x = create().directive("  default-src  ", SELF).build();
			assertEquals("default-src 'self'", x);
		}

		@Test void b09_nullDirectiveNameThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "name", () -> create().directive(null, SELF));
		}

		@Test void b10_blankDirectiveNameThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "directive name must not be blank", () -> create().directive("  ", SELF));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// C — Source-expression helpers
	//------------------------------------------------------------------------------------------------------------------

	@Nested class C_sourceHelpers extends TestBase {

		@Test void c01_keywordConstants() {
			assertEquals("'self'", SELF);
			assertEquals("'none'", NONE);
			assertEquals("'unsafe-inline'", UNSAFE_INLINE);
			assertEquals("'unsafe-eval'", UNSAFE_EVAL);
			assertEquals("'strict-dynamic'", STRICT_DYNAMIC);
		}

		@Test void c02_nonce() {
			assertEquals("'nonce-abc123'", nonce("abc123"));
		}

		@Test void c03_noncePlaceholder() {
			assertEquals("'nonce-{nonce}'", nonce(NONCE_PLACEHOLDER));
		}

		@Test void c04_nonceNullThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "token", () -> nonce(null));
		}

		@Test void c05_nonceBlankThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "nonce token must not be blank", () -> nonce("  "));
		}

		@Test void c06_schemeAddsColon() {
			assertEquals("https:", scheme("https"));
		}

		@Test void c07_schemeKeepsColon() {
			assertEquals("data:", scheme("data:"));
		}

		@Test void c08_schemeNullThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "scheme", () -> scheme(null));
		}

		@Test void c09_schemeBlankThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "scheme must not be blank", () -> scheme(" "));
		}

		@Test void c10_hash() {
			assertEquals("'sha256-abc'", hash("sha256", "abc"));
		}

		@Test void c11_hashNullThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "algorithm", () -> hash(null, "abc"));
			assertThrowsWithMessage(IllegalArgumentException.class, "base64Hash", () -> hash("sha256", null));
		}

		@Test void c12_hashBlankThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "hash algorithm must not be blank", () -> hash(" ", "abc"));
			assertThrowsWithMessage(IllegalArgumentException.class, "hash value must not be blank", () -> hash("sha256", " "));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// D — Report-only mode
	//------------------------------------------------------------------------------------------------------------------

	@Nested class D_reportOnly extends TestBase {

		@Test void d01_defaultIsEnforcing() {
			var b = create().defaultSrc(SELF);
			assertFalse(b.isReportOnly());
			assertEquals("Content-Security-Policy", b.toHeader().getName());
		}

		@Test void d02_reportOnlyFlag() {
			var b = create().defaultSrc(SELF).reportOnly();
			assertTrue(b.isReportOnly());
			assertEquals("Content-Security-Policy-Report-Only", b.toHeader().getName());
		}

		@Test void d03_reportOnlyToggleBack() {
			var b = create().defaultSrc(SELF).reportOnly(true).reportOnly(false);
			assertFalse(b.isReportOnly());
			assertEquals("Content-Security-Policy", b.toHeader().getName());
		}

		@Test void d04_toHeaderCarriesValue() {
			assertEquals("default-src 'self'", create().defaultSrc(SELF).toHeader().getValue());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// E — Strict starter preset
	//------------------------------------------------------------------------------------------------------------------

	@Nested class E_strictStarter extends TestBase {

		@Test void e01_presetValue() {
			assertEquals(
				"default-src 'self'; script-src 'self' 'nonce-{nonce}'; style-src 'self' 'nonce-{nonce}'; "
				+ "img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'self'",
				strictStarter().build());
		}

		@Test void e02_presetIsEnforcingByDefault() {
			assertEquals("Content-Security-Policy", strictStarter().toHeader().getName());
		}

		@Test void e03_presetReportOnly() {
			assertEquals("Content-Security-Policy-Report-Only", strictStarter().reportOnly().toHeader().getName());
		}

		@Test void e04_resolveNonce() {
			var resolved = resolveNonce(strictStarter().build(), "R4nd0mT0k3n");
			assertEquals(
				"default-src 'self'; script-src 'self' 'nonce-R4nd0mT0k3n'; style-src 'self' 'nonce-R4nd0mT0k3n'; "
				+ "img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'self'",
				resolved);
		}

		@Test void e05_resolveNonceNullPolicy() {
			assertNull(resolveNonce(null, "x"));
		}

		@Test void e06_resolveNonceNullNonceThrows() {
			assertThrowsWithMessage(IllegalArgumentException.class, "nonce", () -> resolveNonce("x", null));
		}

		@Test void e07_generateNonceFormat() {
			var n = generateNonce();
			assertNotNull(n);
			// 16 bytes base64url, no padding => 22 chars, url-safe alphabet only.
			assertEquals(22, n.length());
			assertTrue(n.matches("[A-Za-z0-9_-]+"), () -> "Not base64url: " + n);
		}

		@Test void e08_generateNonceUnique() {
			assertNotEquals(generateNonce(), generateNonce());
		}

		@Test void e09_generateAndResolveRoundTrip() {
			var nonce = generateNonce();
			var resolved = resolveNonce(strictStarter().build(), nonce);
			assertTrue(resolved.contains("'nonce-" + nonce + "'"), () -> resolved);
			assertFalse(resolved.contains(NONCE_PLACEHOLDER), () -> resolved);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// F — toString / toHeader round-trips
	//------------------------------------------------------------------------------------------------------------------

	@Nested class F_rendering extends TestBase {

		@Test void f01_toStringEqualsBuild() {
			var b = create().defaultSrc(SELF).objectSrc(NONE);
			assertEquals(b.build(), b.toString());
		}

		@Test void f02_toHeaderToString() {
			var h = create().defaultSrc(SELF).toHeader();
			assertEquals("Content-Security-Policy: default-src 'self'", h.toString());
		}
	}
}
