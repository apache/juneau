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

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * The alias channel on {@link Theme.Builder}: alias-name shape, the anchored {@code var(--jc-name)} target
 * recognizer, the one-channel-per-name rule, and {@link Theme#getAliases()}'s ordering and immutability.
 */
class Theme_AliasChannel_Test extends TestBase {

	private static Theme.Builder builder() {
		return Theme.create("corporate").token("--jc-accent", "#b45309");
	}

	private static String aliasTargetOf(String target) {
		return builder().alias("--jc-tab-bar-bg", target).build().getAliases().get("--jc-tab-bar-bg");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a) Alias name shape
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {
		"",                // empty
		"--jc-",           // no name part
		"jc-accent",       // missing the -- prefix
		"--JC-accent",     // uppercase prefix
		"--jc-Accent",     // uppercase name
		"--jc-foo;--bar",  // full-string guard: "--jc-foo" matches only as a LEADING substring
		"--jc-foo bar",    // whitespace
		"--other-accent",  // outside the --jc- namespace
	})
	void a01_aliasName_invalidShapes_rejected(String name) {
		var b = builder();
		assertThrows(IllegalArgumentException.class, () -> b.alias(name, "var(--jc-accent)"));
	}

	/** The guard fires on the DECLARED name only: aliasing a theme token <i>to</i> a reserved ladder step is legal. */
	@Test void a02_aliasingToAReservedChromeStep_isLegal() {
		var t = builder().alias("--jc-tab-bar-height", "var(--jc-chrome-control-height)").build();
		assertEquals("var(--jc-chrome-control-height)", t.getAliases().get("--jc-tab-bar-height"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b) One channel per name
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_nameDeclaredByBothChannels_rejectedAtBuild_inEitherDeclarationOrder() {
		var aliasFirst = Theme.create("both").alias("--jc-accent", "var(--jc-link)").token("--jc-accent", "#b45309");
		var e1 = assertThrows(IllegalArgumentException.class, aliasFirst::build);
		assertTrue(e1.getMessage().contains("declares '--jc-accent' as both"), e1::getMessage);
		var tokenFirst = Theme.create("both").token("--jc-accent", "#b45309").alias("--jc-accent", "var(--jc-link)");
		var e2 = assertThrows(IllegalArgumentException.class, tokenFirst::build);
		assertTrue(e2.getMessage().contains("declares '--jc-accent' as both"), e2::getMessage);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c) getAliases(): insertion-ordered, immutable, empty by default
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Insertion order is what makes the served block byte-stable. The names are deliberately neither alphabetical
	 * nor reverse-alphabetical, so a sorted or hash-ordered map fails rather than accidentally passing.
	 */
	@Test void c01_getAliases_isInsertionOrdered_andImmutable() {
		var t = builder()
			.alias("--jc-z-last", "var(--jc-accent)")
			.alias("--jc-a-first", "var(--jc-link)")
			.alias("--jc-m-middle", "var(--jc-text)")
			.build();
		assertEquals(List.of("--jc-z-last", "--jc-a-first", "--jc-m-middle"), new ArrayList<>(t.getAliases().keySet()));
		var aliases = t.getAliases();
		assertThrows(UnsupportedOperationException.class, () -> aliases.put("--jc-x", "var(--jc-accent)"));
	}

	@Test void c02_stockThemes_carryNoAliases() {
		for (var t : List.of(Theme.OPEN, Theme.LIGHT_RED, Theme.LIGHT_BROWN, Theme.RED, Theme.GRAY))
			assertEquals(Map.of(), t.getAliases(), t::getName);
	}

	/** An alias is never resolved: it does not appear in, or alter, the leaf map. */
	@Test void c03_alias_doesNotLeakIntoTheLeafMap() {
		var t = builder().alias("--jc-tab-bar-bg", "var(--jc-card-bg)").build();
		assertFalse(t.getTokens().containsKey("--jc-tab-bar-bg"));
		assertEquals("#b45309", t.getTokens().get("--jc-accent"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d) Alias target shape: the anchored var(--jc-name) accept/reject matrix
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_aliasTargets_acceptedShapes_storeTheNormalizedReference() {
		assertEquals("var(--jc-accent)", aliasTargetOf("var(--jc-accent)"));
		assertEquals("VAR(--jc-accent)", aliasTargetOf("VAR(--jc-accent)"), "the recognizer is case-insensitive on the function name");
		assertEquals("var( --jc-accent )", aliasTargetOf("var( --jc-accent )"), "internal whitespace inside the parens is legal");
		assertEquals("var(--jc-accent)", aliasTargetOf("  var(--jc-accent)  "), "the shared normalization belt trims");
		assertEquals("var(--jc-accent)", aliasTargetOf("var/**/(--jc-accent)"), "the shared normalization belt strips comments");
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {
		"",                                    // not a reference
		"var(--jc-x, #fff)",                   // the fallback form must FAIL, never silently fall back
		"var(--jc-a) var(--jc-b)",             // two references
		"linear-gradient(var(--jc-a), #fff)",  // a reference embedded in a larger value
		"#ffffff",                             // a literal belongs in token(), not here
		"1.2rem",
		"red",
		"url(https://evil)",
		"url (https://evil)",
		"url/**/(https://evil)",
		"var(--jc-a);color:red",               // declaration breakout
		"var(--jc-a)}html{color:red",          // block breakout
		"var(--jc-a)/*",                       // unterminated comment survives the strip and must not match
		"var(--jc-A)",                         // uppercase in the target name
		"var(--other-a)",                      // outside the --jc- namespace
		"var(--jc-a",                          // unbalanced
		"ｖar(--jc-a)",                    // fullwidth 'v' homoglyph
		"var‎(--jc-a)",                   // a bidi/format control between the name and the paren
	})
	void d02_aliasTargets_rejectedShapes(String target) {
		var b = builder();
		assertThrows(IllegalArgumentException.class, () -> b.alias("--jc-tab-bar-bg", target));
	}

	/**
	 * Control characters are rejected by the shared belt on the RAW value, before any trim - which is what kills
	 * the {@code url\t(} / {@code url\n(} CSS-hex reconstruction vector for this channel too.
	 */
	@Test void d03_aliasTargets_withControlCharacters_rejected() {
		var b = builder();
		assertThrows(IllegalArgumentException.class, () -> b.alias("--jc-tab-bar-bg", "var(--jc-a\t)"));
		assertThrows(IllegalArgumentException.class, () -> b.alias("--jc-tab-bar-bg", "var(--jc-a)\n"));
		assertThrows(IllegalArgumentException.class, () -> b.alias("--jc-tab-bar-bg", "\u0000var(--jc-a)"));
		assertThrows(IllegalArgumentException.class, () -> b.alias("--jc-tab-bar-bg", "var(--jc-a)\u007F"));
	}

	/**
	 * The shared-recognizer pin: for every candidate, alias() and token()'s reference resolution agree on whether
	 * it is a {@code var()} reference. A duplicated regex fixed in one copy only would be a silent,
	 * security-relevant divergence. The token side is observed through resolution: a recognized reference is
	 * resolved to {@link Theme#OPEN}'s {@code --jc-accent} literal by {@code build()}.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"var(--jc-accent)",                 // accepted by both
		"VAR(--jc-accent)",
		"var( --jc-accent )",
		"var/**/(--jc-accent)",
		"var(--jc-accent, #fff)",           // rejected by both
		"var(--jc-accent) var(--jc-link)",
		"#ffffff",
		"1.2rem",
		"red",
	})
	void d04_aliasAndTokenChannels_agreeOnWhatAReferenceIs(String value) {
		assertEquals(tokenTreatsAsReference(value), aliasAccepts(value),
			() -> "the two channels disagree about '" + value + "' - the recognizer has been duplicated and has drifted");
	}

	private static boolean aliasAccepts(String value) {
		try {
			Theme.create("probe").alias("--jc-tab-bar-bg", value);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private static boolean tokenTreatsAsReference(String value) {
		try {
			return Theme.OPEN.getTokens().get("--jc-accent")
				.equals(Theme.create("probe").token("--jc-tab-bar-bg", value).build().getTokens().get("--jc-tab-bar-bg"));
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
}
