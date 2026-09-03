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
 * {@link Theme#RED} &mdash; the second stock token-{@link Theme} beyond {@link Theme#OPEN}.
 *
 * <p>
 * The load-bearing guard is (a01): because {@link Theme#RED} is authored via
 * {@link Theme#deriveFrom(String, Theme) deriveFrom}({@link Theme#OPEN}), its token <i>key</i> set must be
 * provably identical to {@link Theme#OPEN}'s, which is what protects the {@code chrome.css} bidirectional
 * cross-check ({@code ConsoleChromeMixin_Test}) for this theme too.
 */
class Theme_Red_Test extends TestBase {

	@Test void a01_tokenKeySet_isIdenticalToOpens() {
		assertEquals(Theme.OPEN.getTokens().keySet(), Theme.RED.getTokens().keySet());
	}

	@Test void a02_tokenCount_is48_mirroringOpensPin() {
		assertEquals(48, Theme.RED.getTokens().size());
	}

	@Test void a03_name_isRed() {
		assertEquals("red", Theme.RED.getName());
	}

	@Test void b01_recoloredSignatureValues() {
		var tokens = Theme.RED.getTokens();
		assertEquals("#BF2600", tokens.get("--jc-chrome-bg"));
		assertEquals("#FF5630", tokens.get("--jc-accent"));
		assertEquals("#BF2600", tokens.get("--jc-link"));
		assertEquals("#BF2600", tokens.get("--jc-btn-primary"));
	}

	@Test void b02_keptFromOpen_semanticStatusAndTagValues_areUnchanged() {
		var tokens = Theme.RED.getTokens();
		assertEquals(Theme.OPEN.getTokens().get("--jc-white"), tokens.get("--jc-white"));
		// Kept deliberately: RED's chrome/brand red must not be confused with, or replace, this semantic color.
		assertEquals(Theme.OPEN.getTokens().get("--jc-danger"), tokens.get("--jc-danger"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-success"), tokens.get("--jc-success"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-tag-blue-bg"), tokens.get("--jc-tag-blue-bg"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-tag-amber-bg"), tokens.get("--jc-tag-amber-bg"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-tag-neutral-bg"), tokens.get("--jc-tag-neutral-bg"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-tag-red-bg"), tokens.get("--jc-tag-red-bg"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-tag-red-text"), tokens.get("--jc-tag-red-text"));
	}

	@Test void b03_noVarReferencesLeak_everyValueIsAResolvedLiteral() {
		for (var e : Theme.RED.getTokens().entrySet())
			assertFalse(e.getValue().contains("var("), () -> "token '" + e.getKey() + "' leaked an unresolved var() reference: " + e.getValue());
	}

	@Test void c01_derivedFromOpen_never_equalsOpensOwnName() {
		assertNotEquals(Theme.OPEN.getName(), Theme.RED.getName());
	}
}
