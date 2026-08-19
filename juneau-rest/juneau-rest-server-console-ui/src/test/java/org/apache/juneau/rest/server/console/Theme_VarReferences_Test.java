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

/**
 * {@code var(--jc-name)} Theme-layer reference recognition and composition-time ({@code build()}) resolution.
 *
 * <p>
 * The load-bearing security property here is that {@code var()} is <b>never</b> a {@code CssValueGrammar} value
 * shape: it is recognized one layer above the grammar and resolved to a concrete literal at {@code build()} time,
 * so {@code getTokens()} never carries the substring {@code var(} (see {@code d02}). Resolution is fail-closed:
 * an unknown reference, a cycle, or a chain past the depth cap is a loud {@code build()} failure with its own
 * distinct message, never a silent fall-through (see the {@code c} group).
 */
class Theme_VarReferences_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// a) Recognition + resolution positives
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_ownMapReference_resolvesToTheReferencedLiteral() {
		// The acceptance case (mirrors ReleaseManagerTheme): both tokens on the SAME builder, so this exercises
		// own-map resolution, not the Theme.OPEN fallback.
		var theme = Theme.create("derived")
			.token("--jc-danger", "#c23934")
			.token("--jc-tag-red-text", "var(--jc-danger)")
			.build();
		assertEquals("#c23934", theme.getTokens().get("--jc-tag-red-text"));
		assertEquals("#c23934", theme.getTokens().get("--jc-danger"));
	}

	@Test void a02_forwardReference_resolvesRegardlessOfDeclarationOrder() {
		// Reference declared BEFORE its target - only resolvable at build(), the whole point of deferring.
		var theme = Theme.create("fwd")
			.token("--jc-a", "var(--jc-b)")
			.token("--jc-b", "#abcabc")
			.build();
		assertEquals("#abcabc", theme.getTokens().get("--jc-a"));
	}

	@Test void a03_fallbackToThemeOpen_resolvesAgainstTheFrameworkTokenSet() {
		// Custom theme references a --jc-* name it does not itself define; resolution finds it on Theme.OPEN.
		var theme = Theme.create("fb").token("--jc-a", "var(--jc-danger)").build();
		assertEquals("#c23934", theme.getTokens().get("--jc-a"));
	}

	@Test void a04_multiHopChain_withinCap_resolvesToTheTerminalLiteral() {
		var theme = Theme.create("hop")
			.token("--jc-a", "var(--jc-b)")
			.token("--jc-b", "var(--jc-c)")
			.token("--jc-c", "#0a0b0c")
			.build();
		assertEquals("#0a0b0c", theme.getTokens().get("--jc-a"));
		assertEquals("#0a0b0c", theme.getTokens().get("--jc-b"));
	}

	@Test void a05_recognitionVariants_shareTheSameNormalizationBelt() {
		// Comment-stripping, case-insensitivity, and interior whitespace are handled by the SAME belt every other
		// value runs through - there is no second, reference-specific normalization pass.
		var theme = Theme.create("rec")
			.token("--jc-x", "#123456")
			.token("--jc-comment", "var/**/(--jc-x)")
			.token("--jc-inner-comment", "var(--jc-/**/x)")
			.token("--jc-upper", "VAR(--jc-x)")
			.token("--jc-space", "var( --jc-x )")
			.build();
		assertEquals("#123456", theme.getTokens().get("--jc-comment"));
		assertEquals("#123456", theme.getTokens().get("--jc-inner-comment"));
		assertEquals("#123456", theme.getTokens().get("--jc-upper"));
		assertEquals("#123456", theme.getTokens().get("--jc-space"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b) Deferred timing / atomicity
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_referenceIsAcceptedAtTokenTime_evenWhenTargetIsNotYetDefined() {
		// A well-formed reference must NOT throw at token() time (its target may be a forward reference).
		assertDoesNotThrow(() -> Theme.create("t").token("--jc-a", "var(--jc-later)"));
	}

	@Test void b02_failedBuild_leavesBuilderUnchanged_andRetryable() {
		// Resolution runs on a COPY: a failed build() must not half-resolve the builder's own map.
		var b = Theme.create("atom").token("--jc-a", "var(--jc-missing)");
		assertThrows(IllegalArgumentException.class, b::build);
		// The reference survived intact, so defining its target and rebuilding now succeeds.
		var theme = b.token("--jc-missing", "#123123").build();
		assertEquals("#123123", theme.getTokens().get("--jc-a"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c) Fail-closed resolution: unknown / cycle / depth-cap, each with a DISTINCT message
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_unknownReference_isABuildFailure_namingBothTokens() {
		var ex = assertThrows(IllegalArgumentException.class,
			() -> Theme.create("u").token("--jc-a", "var(--jc-missing)").build());
		assertTrue(ex.getMessage().contains("--jc-a"), () -> "message must name the defining token: " + ex.getMessage());
		assertTrue(ex.getMessage().contains("--jc-missing"), () -> "message must name the missing target: " + ex.getMessage());
		assertTrue(ex.getMessage().contains("unknown"), () -> "message: " + ex.getMessage());
		// Must NOT reuse the pre-existing "not one of the allowed CSS value shapes" message.
		assertFalse(ex.getMessage().contains("allowed CSS value shapes"), () -> "message: " + ex.getMessage());
	}

	@Test void c02_shadowedBrokenOverride_failsInsteadOfSilentlyFallingBackToThemeOpen() {
		// --jc-danger EXISTS on Theme.OPEN (#c23934). Shadowing it here with a broken reference must FAIL the build,
		// NOT silently resolve through to Theme.OPEN's value - the exact bug the reference feature exists to catch.
		var ex = assertThrows(IllegalArgumentException.class,
			() -> Theme.create("shadow").token("--jc-danger", "var(--jc-nonexistent)").build());
		assertTrue(ex.getMessage().contains("--jc-nonexistent"), () -> "message: " + ex.getMessage());
		assertTrue(ex.getMessage().contains("unknown"), () -> "message: " + ex.getMessage());
	}

	@Test void c03_selfCycle_isABuildFailure_withTheCyclePath() {
		var ex = assertThrows(IllegalArgumentException.class,
			() -> Theme.create("cyc").token("--jc-a", "var(--jc-a)").build());
		assertTrue(ex.getMessage().contains("cyclic"), () -> "message: " + ex.getMessage());
		assertTrue(ex.getMessage().contains("--jc-a -> --jc-a"), () -> "message must carry the cycle path: " + ex.getMessage());
	}

	@Test void c04_twoNodeCycle_isABuildFailure_withTheFullCyclePath() {
		var ex = assertThrows(IllegalArgumentException.class,
			() -> Theme.create("cyc2")
				.token("--jc-a", "var(--jc-b)")
				.token("--jc-b", "var(--jc-a)")
				.build());
		assertTrue(ex.getMessage().contains("cyclic"), () -> "message: " + ex.getMessage());
		assertTrue(ex.getMessage().contains("--jc-a -> --jc-b -> --jc-a"), () -> "message must carry the full cycle path: " + ex.getMessage());
	}

	@Test void c05_depthCapBreach_isADistinctFailure_fromCycleAndUnknown() {
		// A long ACYCLIC chain (all names distinct, terminal token defined) must still be bounded by the hop cap -
		// so this fails for depth, not cycle (no repeat) and not unknown (--jc-c10 exists).
		var b = Theme.create("deep");
		for (var i = 0; i < 10; i++)
			b.token("--jc-c" + i, "var(--jc-c" + (i + 1) + ")");
		b.token("--jc-c10", "#000000");
		var ex = assertThrows(IllegalArgumentException.class, b::build);
		assertTrue(ex.getMessage().contains("maximum reference depth"), () -> "message: " + ex.getMessage());
		assertFalse(ex.getMessage().contains("cyclic"), () -> "depth-cap message must be distinct from the cycle message: " + ex.getMessage());
		assertFalse(ex.getMessage().contains("unknown"), () -> "depth-cap message must be distinct from the unknown-name message: " + ex.getMessage());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d) Defense-in-depth + post-build invariant
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_resolvedLiteralThatTheGrammarRejects_wouldFailTheReValidation() {
		// The resolved literal (never an intermediate var()) is re-validated by the unchanged grammar. Through the
		// public API every literal is already grammar-validated at token() time, so this path cannot be driven to a
		// failure here - it is exercised (every resolved value passes back through normalizeAndValidate) rather than
		// tripped. This test pins that a reference to a plain literal round-trips byte-for-byte through that
		// re-validation.
		var theme = Theme.create("dd")
			.token("--jc-a", "linear-gradient(135deg, rgb(21,137,238), rgba(0,0,0,0.2))")
			.token("--jc-b", "var(--jc-a)")
			.build();
		assertEquals("linear-gradient(135deg, rgb(21,137,238), rgba(0,0,0,0.2))", theme.getTokens().get("--jc-b"));
	}

	@Test void d02_afterBuild_noTokenValueContainsTheVarSubstring() {
		var theme = Theme.create("inv")
			.token("--jc-danger", "#c23934")
			.token("--jc-tag-red-text", "var(--jc-danger)")   // own-map reference
			.token("--jc-a", "var(--jc-accent)")               // Theme.OPEN fallback reference
			.token("--jc-b", "var(--jc-danger)")               // chained onto another own token
			.token("--jc-lit", "#1589EE")                      // plain literal, untouched
			.build();
		for (var v : theme.getTokens().values())
			assertFalse(v.contains("var("), () -> "resolved token value still contains 'var(': " + v);
	}
}
