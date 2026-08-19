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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Phase 2 gate: {@code CssValueGrammar}'s allowlist-grammar accept/reject sweep, tested through
 * {@link Theme.Builder#token(String, String)} (the only call site).
 *
 * <p>
 * The GREEN bypass-vector sweep below is the B1 close-out: it must beat every named vector the plan-review
 * identified, not just a literal {@code url(}.
 */
class CssValueGrammar_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// GREEN: the B1 bypass-vector sweep - each MUST throw.
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@ValueSource(strings = {
		"url(https://evil/x)",
		"URL(https://evil)",
		"URl(x)",
		"url (https://evil/x)",       // space
		"url\t(https://evil)",        // tab (C0)
		"url\n(https://evil)",        // newline (C0)
		"url/**/(https://evil)",      // comment
		"  url(x)",                   // leading whitespace
		"linear-gradient(url(evil))", // nested url() inside an otherwise-shaped gradient - grammar-layer reject
		// The "none"-interaction vectors below (folded in from the former a12, merged to close out S4144 - these
		// share a01's exact shape and target, so splitting them into a second method was pure duplication):
		"url(none)",                       // "none" as a url() argument must not smuggle the url() production through
		"URL(none.png)",
		"url\t(none)",                     // tab (C0) between url and paren
		"none url(evil)",                  // "none" prefix must not whitelist a trailing url()
		"linear-gradient(none, url(evil))",
	})
	void a01_bypassVectorSweep_throws(String payload) {
		var b = Theme.create("x");
		assertThrows(IllegalArgumentException.class, () -> b.token("--jc-page-bg", payload));
	}

	@Test void a02_cssHexEscapedUrl_throws() {
		// CSS-hex-escaped letters spelling "url(" - the grammar has no production admitting a literal backslash,
		// so these die in the allowlist grammar (not the normalization url-regex, which does not match "\75rl(").
		var b = Theme.create("x");
		assertThrows(IllegalArgumentException.class, () -> b.token("--jc-page-bg", "\\75rl(x)"));
		assertThrows(IllegalArgumentException.class, () -> b.token("--jc-page-bg", "url\\3C(x)"));
	}

	@Test void a03_rawC0ControlChar_anywhere_throws() {
		var b = Theme.create("x");
		assertThrows(IllegalArgumentException.class, () -> b.token("--jc-accent", "#1589EE\u0000"));
		assertThrows(IllegalArgumentException.class, () -> b.token("--jc-accent", "\u0007red"));
	}

	@Test void a04_rawC1DelControlChar_throws() {
		var b = Theme.create("x");
		assertThrows(IllegalArgumentException.class, () -> b.token("--jc-accent", "red\u007F"));
		assertThrows(IllegalArgumentException.class, () -> b.token("--jc-accent", "red\u0085"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// S5: prove the C0/newline cases throw at NORMALIZATION (before the grammar), not merely because the belt
	// regex (?i)url\s*\( also happens to match whitespace. A payload the grammar would ACCEPT if the C0 reject
	// were removed, but that still must throw because of the control character alone.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a05_controlCharacterInOtherwiseValidGrammar_throwsAtNormalization_notGrammar() {
		// rgb(...)'s functional-color charset includes whitespace; String.trim() does not strip an INTERIOR
		// newline. This value contains no "url(" anywhere, so if the C0/C1 normalization reject were deleted,
		// the (?i)url\s*\( belt would NOT catch it either, and the grammar would ACCEPT it (whitespace is in the
		// rgb() charset). It must still throw - and for a DIFFERENT reason (control character) than an
		// out-of-grammar value.
		var b = Theme.create("x");
		var ex = assertThrows(IllegalArgumentException.class,
			() -> b.token("--jc-accent", "rgb(21,\n137,238)"));
		assertTrue(ex.getMessage().contains("control character"), () -> "unexpected message: " + ex.getMessage());
	}

	@Test void a06_outOfGrammarValue_throwsWithDistinctMessage_notControlCharacter() {
		var b = Theme.create("x");
		var ex = assertThrows(IllegalArgumentException.class,
			() -> b.token("--jc-accent", "not-a-css-value!!!"));
		assertFalse(ex.getMessage().contains("control character"), () -> "unexpected message: " + ex.getMessage());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// GREEN: allowlist positives survive byte-for-byte.
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@ValueSource(strings = {
		"#1589EE",
		"#0f0",
		"rgb(21,137,238)",
		"rgba(21,137,238,0.1)",
		"hsl(210,86%,51%)",
		"transparent",
		"currentColor",
		"26px 26px",
		"0.25rem",
		"'Salesforce Sans', Inter, system-ui, sans-serif",
		"linear-gradient(180deg, #b0c4df 0%, #f5f6f9 100%)",
	})
	void a07_positives_acceptedVerbatim(String value) {
		var theme = Theme.create("x").token("--jc-accent", value).build();
		assertEquals(value, theme.getTokens().get("--jc-accent"));
	}

	@Test void a08_gradientWithNestedFunctionalColor_accepted() {
		var value = "linear-gradient(135deg, rgb(21,137,238), rgba(0,0,0,0.2))";
		var theme = Theme.create("x").token("--jc-avatar-bg", value).build();
		assertEquals(value, theme.getTokens().get("--jc-avatar-bg"));
	}

	@Test void a09_gradientWithDisallowedNestedFunction_rejected() {
		var b = Theme.create("x");
		assertThrows(IllegalArgumentException.class,
			() -> b.token("--jc-avatar-bg", "linear-gradient(135deg, image(evil))"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// The bare keyword "none" is admitted so a theme can express a flat page background
	// (background-image: none), but admitting it must NOT reopen the url() ban - "none" is an orthogonal branch of
	// isAllowedShape, not a loosening of URL_REJECT or the gradient nested-function allowlist. The "must not reopen
	// the url() ban" vectors are covered by the "none"-interaction entries folded into (a01) above.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a10_bareNoneKeyword_acceptedVerbatim() {
		var theme = Theme.create("x").token("--jc-page-bg", "none").build();
		assertEquals("none", theme.getTokens().get("--jc-page-bg"));
	}

	@Test void a11_noneKeyword_isCaseInsensitive() {
		assertEquals("NONE", Theme.create("x").token("--jc-page-bg", "NONE").build().getTokens().get("--jc-page-bg"));
		assertEquals("None", Theme.create("x").token("--jc-page-bg", "None").build().getTokens().get("--jc-page-bg"));
	}
}
