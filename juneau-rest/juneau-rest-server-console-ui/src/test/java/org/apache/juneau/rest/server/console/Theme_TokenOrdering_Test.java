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

/**
 * {@link Theme#getTokens()} must iterate in the order the tokens were declared, because
 * {@code ConsoleChromeMixin} writes that iteration straight out as the served stylesheet's {@code :root{}}
 * declarations - so a hash-ordered map makes the response body differ, byte for byte, between two processes
 * serving an identical token set.
 *
 * <p>
 * The load-bearing assertion is (a01)'s <i>insertion</i>-order check, not (a03)'s repeat-build check. A
 * {@code java.util.ImmutableCollections} map perturbs its probe sequence with a per-JVM salt, so its order is
 * arbitrary but <i>fixed</i> within one process: a test that only builds the same theme twice and compares
 * cannot see the defect at all.
 */
class Theme_TokenOrdering_Test extends TestBase {

	/** Declared in reverse-alphabetical order so a map that happened to sort its keys would not pass by accident. */
	private static final List<String> NAMES = List.of(
		"--jc-zulu", "--jc-yankee", "--jc-xray", "--jc-whiskey", "--jc-victor", "--jc-uniform",
		"--jc-tango", "--jc-sierra", "--jc-romeo", "--jc-quebec", "--jc-papa", "--jc-oscar");

	private static Theme themeWithNames() {
		var b = Theme.create("ordering");
		for (var i = 0; i < NAMES.size(); i++)
			b.token(NAMES.get(i), String.format("#%06x", i));
		return b.build();
	}

	@Test void a01_getTokens_iteratesInDeclarationOrder() {
		assertEquals(NAMES, new ArrayList<>(themeWithNames().getTokens().keySet()));
	}

	@Test void a02_themeOpen_iteratesInDeclarationOrder_soTheTagPaletteStaysGrouped() {
		var names = new ArrayList<>(Theme.OPEN.getTokens().keySet());
		assertEquals("--jc-font", names.get(0), () -> "Theme.OPEN's first declared token is not first, order: " + names);
		var tags = names.stream().filter(x -> x.startsWith("--jc-tag-")).toList();
		assertEquals(
			List.of(
				"--jc-tag-green-bg", "--jc-tag-green-text", "--jc-tag-green-border",
				"--jc-tag-blue-bg", "--jc-tag-blue-text", "--jc-tag-blue-border",
				"--jc-tag-amber-bg", "--jc-tag-amber-text", "--jc-tag-amber-border",
				"--jc-tag-neutral-bg", "--jc-tag-neutral-text", "--jc-tag-neutral-border",
				"--jc-tag-red-bg", "--jc-tag-red-text", "--jc-tag-red-border"),
			tags);
	}

	@Test void a03_repeatedBuildsOfTheSameTokenSet_produceTheSameOrder() {
		assertEquals(new ArrayList<>(themeWithNames().getTokens().keySet()), new ArrayList<>(themeWithNames().getTokens().keySet()));
	}

	@Test void a04_reDeclaringAToken_updatesInPlace_withoutMovingIt() {
		var theme = Theme.create("ordering")
			.token("--jc-accent", "#111111")
			.token("--jc-link", "#222222")
			.token("--jc-accent", "#333333")
			.build();
		assertEquals(List.of("--jc-accent", "--jc-link"), new ArrayList<>(theme.getTokens().keySet()));
		assertEquals("#333333", theme.getTokens().get("--jc-accent"));
	}

	@Test void a05_getTokens_isStillUnmodifiable() {
		var tokens = Theme.OPEN.getTokens();
		assertThrows(UnsupportedOperationException.class, () -> tokens.put("--jc-injected", "#000000"));
	}

	/** Mutating the builder after {@code build()} must not reach through into the built theme's copy. */
	@Test void a06_builtTheme_isDecoupledFromLaterBuilderMutation() {
		var b = Theme.create("ordering").token("--jc-accent", "#111111");
		var theme = b.build();
		b.token("--jc-link", "#222222");
		assertEquals(List.of("--jc-accent"), new ArrayList<>(theme.getTokens().keySet()));
	}
}
