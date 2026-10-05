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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * {@link Theme#GRAY} &mdash; the third stock token-{@link Theme} beyond {@link Theme#OPEN}.
 *
 * <p>
 * The load-bearing guard is (a01): because {@link Theme#GRAY} is seeded from {@link Theme#OPEN},
 * its token <i>key</i> set must be
 * provably identical to {@link Theme#OPEN}'s, which is what protects the {@code chrome.css} bidirectional
 * cross-check ({@code ConsoleChromeMixin_Test}) for this theme too.
 */
class Theme_Gray_Test extends TestBase {

	@Test void a01_tokenKeySet_isIdenticalToOpens() {
		assertEquals(Theme.OPEN.getTokens().keySet(), Theme.GRAY.getTokens().keySet());
	}

	@Test void a02_tokenCount_is82_mirroringOpensPin() {
		assertEquals(82, Theme.GRAY.getTokens().size());
	}

	@Test void a03_name_isGray() {
		assertEquals("gray", Theme.GRAY.getName());
	}

	@Test void b01_grayedSignatureValues() {
		var tokens = Theme.GRAY.getTokens();
		assertBean(tokens, "--jc-chrome-bg,--jc-text,--jc-border", "#f4f4f4,#262626,#e0e0e0");
		assertContains("#e8e8e8", tokens.get("--jc-page-bg"));
	}

	@Test void b02_keptFromOpen_statusAndBlueAccentValues_areUnchanged() {
		var tokens = Theme.GRAY.getTokens();
		assertEquals(Theme.OPEN.getTokens().get("--jc-white"), tokens.get("--jc-white"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-danger"), tokens.get("--jc-danger"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-success"), tokens.get("--jc-success"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-tag-red-text"), tokens.get("--jc-tag-red-text"));
		// Per the accent decision (RECOMMENDED / blue): GRAY keeps OPEN's blue affordances verbatim.
		assertEquals(Theme.OPEN.getTokens().get("--jc-accent"), tokens.get("--jc-accent"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-link"), tokens.get("--jc-link"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-btn-primary"), tokens.get("--jc-btn-primary"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-card-bg"), tokens.get("--jc-card-bg"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-main-bg"), tokens.get("--jc-main-bg"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-card-padding"), tokens.get("--jc-card-padding"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-chrome-icon"), tokens.get("--jc-chrome-icon"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-card-shadow"), tokens.get("--jc-card-shadow"));
		assertBean(tokens, "--jc-accent,--jc-card-bg,--jc-card-padding,--jc-chrome-icon,--jc-card-shadow",
			"#1589EE,#ffffff,16px 16px 8px,#666666,0 2px 2px rgba(0, 0, 0, 0.05)");
	}

	@Test void b03_noVarReferencesLeak_everyValueIsAResolvedLiteral() {
		for (var e : Theme.GRAY.getTokens().entrySet())
			assertFalse(e.getValue().contains("var("), () -> "token '" + e.getKey() + "' leaked an unresolved var() reference: " + e.getValue());
	}

	@Test void c01_derivedFromOpen_never_equalsOpensOwnName() {
		assertNotEquals(Theme.OPEN.getName(), Theme.GRAY.getName());
	}
}
