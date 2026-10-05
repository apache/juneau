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
 * The reserved {@code --jc-chrome-*} namespace guard on {@link Theme.Builder}. The reserved set is the prefix
 * <i>minus</i> the names {@link Theme#OPEN} already declares; the guard fires on the declared name only, on both
 * the leaf and the alias channel, at declaration time.
 */
class Theme_ReservedNamespace_Test extends TestBase {

	@ParameterizedTest
	@ValueSource(strings = {
		"--jc-chrome-control-height",   // a real ladder step - the whole reason the guard exists
		"--jc-chrome-control-padding-x",
		"--jc-chrome-font-size-1",
		"--jc-chrome-line-height",
		"--jc-chrome-glyph-size-small",
		"--jc-chrome-",                 // the bare prefix: passes the shape guard, must still reject
		"--jc-chrome-anything",         // fails closed on a name no ladder declares today
	})
	void a01_reservedChromeNames_rejectedOnBothChannels_atDeclaration(String name) {
		var b = Theme.create("corporate");
		var leaf = assertThrows(IllegalArgumentException.class, () -> b.token(name, "26px"));
		assertTrue(leaf.getMessage().contains("Cannot declare reserved token '" + name + "'"), leaf::getMessage);
		assertTrue(leaf.getMessage().contains("theme 'corporate' leaf token"), leaf::getMessage);
		var alias = assertThrows(IllegalArgumentException.class, () -> b.alias(name, "var(--jc-accent)"));
		assertTrue(alias.getMessage().contains("theme 'corporate' alias"), alias::getMessage);
	}

	/** Names the guard must <b>not</b> claim; each is a distinct way to over-apply a prefix reservation. */
	@ParameterizedTest
	@ValueSource(strings = {
		"--jc-chromex",        // the prefix minus its trailing hyphen - a different name entirely
		"--jc-chrome",         // the bare word is not the prefix
		"--jc-tab-chrome-bg",  // the prefix appearing mid-string reserves nothing
		"--jc-chrome-bg",      // shipped by Theme.OPEN, consumed by chrome.css, legitimately overridable
		"--jc-chrome-icon",    // idle ribbon / paging glyph; Theme.OPEN leaf, not a ladder step
	})
	void a02_namesOutsideTheReservedSet_acceptedOnBothChannels(String name) {
		assertDoesNotThrow(() -> Theme.create("leaf").token(name, "#b45309").build());
		assertDoesNotThrow(() -> Theme.create("alias").alias(name, "var(--jc-accent)").build());
	}

	/**
	 * The reason the reserved set cannot be the whole prefix: seeding a builder from {@link Theme#OPEN} copies
	 * its shipped {@code --jc-chrome-bg}. An unconditional prefix reservation would reject the normal way to author
	 * a palette, which is what {@code <@theme>} does on every render.
	 */
	@Test void a03_themeSeededFromThemeOpen_builds() {
		var b = Theme.create("corporate");
		for (var e : Theme.OPEN.getTokens().entrySet())
			b.token(e.getKey(), e.getValue());
		var t = b.token("--jc-accent", "#b45309").build();
		assertTrue(t.getTokens().containsKey("--jc-chrome-bg"));
	}

	/** The guard must not break class initialization: every stock constant is built through the guarded builder. */
	@Test void a04_everyStockTheme_buildsThroughTheGuard() {
		for (var t : List.of(Theme.OPEN, Theme.LIGHT_RED, Theme.LIGHT_BROWN, Theme.RED, Theme.GRAY))
			assertNotNull(t.getTokens());
		assertTrue(Theme.OPEN.getTokens().containsKey("--jc-chrome-bg"));
	}
}
