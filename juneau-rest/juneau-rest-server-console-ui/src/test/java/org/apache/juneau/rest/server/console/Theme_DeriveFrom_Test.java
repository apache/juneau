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
 * {@link Theme#deriveFrom(String, Theme)} &mdash; the builder-seed factory.
 *
 * <p>
 * The load-bearing property here is the <b>two</b>-argument signature: a derived theme's name is always the
 * caller's, never the seed's. A one-argument {@code deriveFrom(Theme)} shape would, for a theme derived from
 * {@link Theme#OPEN}, inherit the name {@code "open"} &mdash; and {@code ConsoleChromeMixin.buildBody} suppresses
 * the emitted theme block whenever the active theme's name equals {@code Theme.OPEN}'s, silently dropping every
 * override the derived theme declares. {@code a01} pins that this state is unreachable.
 */
class Theme_DeriveFrom_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// a) The derived theme's name is the caller's, never the seed's
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_derivedThemeName_isCallersName_neverTheSeeds() {
		var theme = Theme.deriveFrom("release-manager", Theme.OPEN).build();
		assertEquals("release-manager", theme.getName());
		assertNotEquals(Theme.OPEN.getName(), theme.getName());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b) Seeded token count, order, and override semantics
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_noAdditionalTokens_seededCountAndOrder_matchTheSeeds() {
		var theme = Theme.deriveFrom("x", Theme.OPEN).build();
		var seeded = new ArrayList<>(theme.getTokens().keySet());
		var seed = new ArrayList<>(Theme.OPEN.getTokens().keySet());
		assertEquals(seed, seeded);
		assertEquals(Theme.OPEN.getTokens(), theme.getTokens());
	}

	@Test void b02_consumerOverride_replacesExactlyOneEntry_addsNone() {
		var theme = Theme.deriveFrom("x", Theme.OPEN).token("--jc-accent", "#123456").build();
		assertEquals(Theme.OPEN.getTokens().size(), theme.getTokens().size());
		for (var e : Theme.OPEN.getTokens().entrySet()) {
			if (e.getKey().equals("--jc-accent"))
				assertEquals("#123456", theme.getTokens().get(e.getKey()));
			else
				assertEquals(e.getValue(), theme.getTokens().get(e.getKey()), () -> "unexpected drift on " + e.getKey());
		}
	}

	@Test void b03_derivedTheme_carriesAdditionalTokensTheSeedDidNotHave() {
		// A token name the seed doesn't define is simply added, appended after the seeded entries. Key order is a
		// served-stylesheet contract, so pin it rather than only the count: the seeded entries in the seed's own
		// order, then the appended ones.
		var theme = Theme.deriveFrom("x", Theme.OPEN).token("--jc-extra", "#abcdef").build();
		assertEquals(Theme.OPEN.getTokens().size() + 1, theme.getTokens().size());
		assertEquals("#abcdef", theme.getTokens().get("--jc-extra"));
		var expected = new ArrayList<>(Theme.OPEN.getTokens().keySet());
		expected.add("--jc-extra");
		assertEquals(expected, new ArrayList<>(theme.getTokens().keySet()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c) Name guard: same anchored matches(...) guard create(String) already applies
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@ValueSource(strings = {
		"Salesforce",   // uppercase
		"has space",    // interior whitespace
		"../evil",      // path-shaped
		"",             // empty
	})
	void c01_invalidName_rejects_sameShapesAsCreate(String name) {
		assertThrows(IllegalArgumentException.class, () -> Theme.deriveFrom(name, Theme.OPEN));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d) Null-seed rejection (fail-closed, not silently treated as create(name))
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_nullSeed_rejects() {
		assertThrows(IllegalArgumentException.class, () -> Theme.deriveFrom("x", null));
	}
}
