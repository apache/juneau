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
package org.apache.juneau.rest.server.views;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Guards the shared chrome scale contract on the served {@code juneau-views.css}: the steps are declared once
 * each, and no surface writes a literal that duplicates one.
 *
 * <p>
 * Driven by {@link ChromeScaleScanner}, which carries the contract's own step table and the reasoning behind
 * what it does and does not evaluate.
 */
class ChromeScale_ContractScan_Test extends TestBase {

	private static String viewsCss() throws IOException {
		try (var in = ChromeScale_ContractScan_Test.class.getResourceAsStream("/org/apache/juneau/views/juneau-views.css")) {
			assertNotNull(in, "juneau-views.css not found on the test classpath");
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a) Every step is declared exactly once, at the value the contract states
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_everyStepIsDeclaredExactlyOnce() throws IOException {
		var problems = ChromeScaleScanner.checkDeclaredExactlyOnce(viewsCss());
		assertTrue(problems.isEmpty(), () -> "chrome scale declaration problems:\n  " + String.join("\n  ", problems));
	}

	/**
	 * The ladders are declared whole, so a step with no consumer today is still declared. Asserting the count
	 * pins that: a future edit that prunes an "unused" step has to come through this test.
	 */
	@Test void a02_ladderIsDeclaredWhole_includingStepsWithNoConsumerYet() throws IOException {
		var css = viewsCss();
		assertTrue(css.contains("--jc-space-5: 24px;"), "the spacing ladder is declared whole, unused steps included");
		assertTrue(css.contains("--jc-space-6: 32px;"), "the spacing ladder is declared whole, unused steps included");
		assertEquals(16, ChromeScaleScanner.scale().size());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b) No literal duplicates a declared step
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_noLiteralDuplicatesADeclaredStep() throws IOException {
		var r = ChromeScaleScanner.scan(viewsCss());
		assertTrue(r.violations().isEmpty(), () -> "literals duplicating a declared step:\n  "
			+ String.join("\n  ", r.violations()));
	}

	/**
	 * A scanner that silently stops matching reads as a passing test, which is worse than no scanner. The scan
	 * must actually reach a meaningful number of declarations on the properties the scale covers.
	 */
	@Test void b02_scanIsNotVacuous() throws IOException {
		var r = ChromeScaleScanner.scan(viewsCss());
		assertTrue(r.checked().size() > 20, () -> "scan examined only " + r.checked().size()
			+ " declarations - it has probably stopped matching rather than found a clean file");
	}

	/**
	 * The recorded-exception table is a ratchet, not an allowlist that may rot: an entry matching nothing means
	 * the declaration it excused has moved or been fixed, and the entry must go rather than sit there quietly
	 * widening what the scan will tolerate.
	 */
	@Test void b03_everyRecordedExceptionStillMatchesSomething() throws IOException {
		var r = ChromeScaleScanner.scan(viewsCss());
		var stale = ChromeScaleScanner.recordedLiterals().stream()
			.filter(x -> !r.matchedRecords().contains(x))
			.map(x -> x.selector() + " { " + x.property() + ": " + x.value() + " }")
			.toList();
		assertTrue(stale.isEmpty(), () -> "recorded exceptions that no longer match any declaration - remove them:\n  "
			+ String.join("\n  ", stale));
	}

	/** Every recorded exception states why it is not spending a step; a bare entry is not auditable. */
	@Test void b04_everyRecordedExceptionCarriesAReason() {
		ChromeScaleScanner.recordedLiterals()
			.forEach(x -> assertFalse(x.reason() == null || x.reason().isBlank(),
				() -> "recorded exception with no reason: " + x.selector() + " { " + x.property() + " }"));
	}

	/**
	 * Proves the scan can actually fail. Without this, (b01) passing tells you nothing about whether the check
	 * works or merely never fires.
	 */
	@Test void b05_syntheticDuplicateLiteralIsCaught() {
		var r = ChromeScaleScanner.scan(".synthetic-surface { height: 32px; }");
		assertEquals(1, r.violations().size(), () -> "expected the synthetic duplicate to be caught: " + r.violations());
		assertTrue(r.violations().get(0).contains("--jc-chrome-control-height"), r.violations().get(0));
	}

	/**
	 * An {@code em} value must never be treated as a step match. Its computed value depends on a font-size
	 * context this file does not have one of, and on form controls that context is a UA/system font that differs
	 * by browser - so an engine-specific computation would fail on one platform and pass on another.
	 */
	@Test void b06_emValuesAreNeverTreatedAsAStepMatch() {
		// 0.9em computes to exactly 12px against Chrome's form-control font and to nothing near a step elsewhere.
		var r = ChromeScaleScanner.scan(".synthetic-toggle::after { width: 0.9em; height: 0.9em; }");
		assertTrue(r.violations().isEmpty(), () -> "an em value was evaluated as a step match: " + r.violations());
	}

	/** A provisional step has no rendered consumer yet, so an existing literal must not be bound to its value. */
	@Test void b07_provisionalStepValuesDoNotTriggerAViolation() {
		var r = ChromeScaleScanner.scan(".synthetic-label { font-size: 0.8125rem; }");
		assertTrue(r.violations().isEmpty(), () -> "a provisional step value was enforced: " + r.violations());
	}

	/** A declaration already spending a token is compliant, fallback and all. */
	@Test void b08_tokenFallbackIsNotReadAsABareLiteral() {
		var r = ChromeScaleScanner.scan(".synthetic-grid { gap: var(--jc-card-gap, 1rem); }");
		assertTrue(r.violations().isEmpty(), () -> "a var() fallback was read as a bare literal: " + r.violations());
	}

	/** {@code width} is a scale property on an SVG glyph and a content measure everywhere else. */
	@Test void b09_widthIsOnlyCheckedOnGlyphs() {
		assertTrue(ChromeScaleScanner.scan(".synthetic-cell { width: 16px; }").violations().isEmpty());
		assertEquals(1, ChromeScaleScanner.scan(".synthetic-btn svg { width: 16px; }").violations().size());
	}
}
