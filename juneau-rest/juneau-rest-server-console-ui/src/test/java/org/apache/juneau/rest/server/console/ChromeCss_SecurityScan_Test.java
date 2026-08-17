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
package org.apache.juneau.rest.server.console;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Phase 2 gate: the {@code chrome.css} golden-file security scan (S1/S3).
 */
class ChromeCss_SecurityScan_Test extends TestBase {

	/**
	 * RED: a deliberately-naive fixture copying the dogfood's three original defects verbatim
	 * ({@code background: linear-gradient(...)} with tokens nested inside the gradient function, a
	 * {@code --jc-logo} custom property, and a color token sunk into a {@code background:} shorthand). The
	 * scanner must fail on all three.
	 */
	@Test void a01_naiveFixture_failsAllThreeDefects() {
		var naive = """
			:root {
			  --jc-accent: #1589EE;
			  --jc-btn-primary: #1a5297;
			  --jc-logo: url("data:image/svg+xml;base64,AAAA");
			}
			.jc-avatar {
			  background: linear-gradient(135deg, var(--jc-accent), var(--jc-btn-primary));
			}
			.jc-logo {
			  background-image: var(--jc-logo);
			}
			.jc-nav-tab.active {
			  background: var(--jc-accent-wash);
			}
			""";
		var violations = ChromeCssScanner.scan(naive);
		assertTrue(violations.stream().anyMatch(v -> v.contains("--jc-logo")), () -> "violations: " + violations);
		assertTrue(violations.stream().anyMatch(v -> v.contains("nested")), () -> "violations: " + violations);
		assertTrue(violations.stream().anyMatch(v -> v.contains("url-capable")), () -> "violations: " + violations);
		assertTrue(violations.size() >= 3, () -> "expected at least 3 distinct violations, got: " + violations);
	}

	/** The scanner must NOT flag the static hard-coded logo literal (no var(--jc- there - it's a plain url() literal). */
	@Test void a02_staticLogoUrlLiteral_isNotAFalseRed() {
		var css = ".jc-logo { background-image: url(\"data:image/svg+xml;base64,AAAA\"); }";
		assertEquals(java.util.List.of(), ChromeCssScanner.scan(css));
	}

	/** GREEN: the real, shipped chrome.css passes the scan cleanly. */
	@Test void a03_realChromeCss_passesScan() throws IOException {
		var css = readChromeCss();
		var violations = ChromeCssScanner.scan(css);
		assertEquals(java.util.List.of(), violations, () -> "chrome.css violations: " + violations);
	}

	/** Sanity: the gradient tokens ARE present on background-image (proves the allowlist path is actually exercised, not vacuously green because chrome.css has no gradient tokens at all). */
	@Test void a04_realChromeCss_gradientTokensPresentOnBackgroundImage() throws IOException {
		var css = readChromeCss();
		assertTrue(css.contains("background-image: var(--jc-page-bg)"));
		assertTrue(css.contains("background-image: var(--jc-avatar-bg)"));
	}

	/** Sanity: no --jc-logo custom property DECLARATION or USAGE (the scanner's own denylist check, re-asserted directly here), and the static logo literal is present. */
	@Test void a05_realChromeCss_logoIsHardCoded() throws IOException {
		var css = readChromeCss();
		assertFalse(css.contains("--jc-logo:"), "must not declare a --jc-logo custom property");
		assertFalse(css.contains("var(--jc-logo"), "must not reference a --jc-logo custom property");
		assertTrue(css.contains(".jc-logo"));
		assertTrue(css.contains("url(\"data:image/svg+xml;base64,"));
	}

	private static String readChromeCss() throws IOException {
		try (var in = ChromeCss_SecurityScan_Test.class.getResourceAsStream("/org/apache/juneau/console/chrome.css")) {
			assertNotNull(in, "chrome.css classpath resource not found");
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
