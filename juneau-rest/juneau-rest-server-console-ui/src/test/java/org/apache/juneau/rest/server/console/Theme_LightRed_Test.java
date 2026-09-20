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
 * {@link Theme#LIGHT_RED} &mdash; the light-chrome red stock token-{@link Theme} beyond {@link Theme#OPEN}.
 *
 * <p>
 * The load-bearing guard is (a01): because {@link Theme#LIGHT_RED} is authored via
 * {@link Theme#deriveFrom(String, Theme) deriveFrom}({@link Theme#OPEN}), its token <i>key</i> set must be
 * provably identical to {@link Theme#OPEN}'s, which is what protects the {@code chrome.css} bidirectional
 * cross-check ({@code ConsoleChromeMixin_Test}) for this theme too.
 */
class Theme_LightRed_Test extends TestBase {

	@Test void a01_tokenKeySet_isIdenticalToOpens() {
		assertEquals(Theme.OPEN.getTokens().keySet(), Theme.LIGHT_RED.getTokens().keySet());
	}

	@Test void a02_tokenCount_is70_mirroringOpensPin() {
		assertEquals(70, Theme.LIGHT_RED.getTokens().size());
	}

	@Test void a03_name_isLightRed() {
		assertEquals("light-red", Theme.LIGHT_RED.getName());
	}

	@Test void b01_recoloredSignatureValues() {
		var tokens = Theme.LIGHT_RED.getTokens();
		assertEquals("#B42348", tokens.get("--jc-accent"));
		assertEquals("#8B1538", tokens.get("--jc-link"));
		assertEquals("#6B1D2A", tokens.get("--jc-btn-primary"));
		assertEquals("#f3ecee", tokens.get("--jc-chrome-bg"));
		assertTrue(tokens.get("--jc-page-bg").contains("#e8cfd3"), () -> "expected the light-red gradient, got: " + tokens.get("--jc-page-bg"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-card-bg"), tokens.get("--jc-card-bg"));
		assertEquals("#ffffff", tokens.get("--jc-card-bg"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-main-bg"), tokens.get("--jc-main-bg"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-card-padding"), tokens.get("--jc-card-padding"));
		assertEquals("16px 16px 8px", tokens.get("--jc-card-padding"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-card-shadow"), tokens.get("--jc-card-shadow"));
		assertEquals("0 2px 2px rgba(0, 0, 0, 0.05)", tokens.get("--jc-card-shadow"));
	}

	@Test void b02_keptFromOpen_statusAndTagValues_areUnchanged() {
		var tokens = Theme.LIGHT_RED.getTokens();
		assertEquals(Theme.OPEN.getTokens().get("--jc-white"), tokens.get("--jc-white"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-danger"), tokens.get("--jc-danger"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-success"), tokens.get("--jc-success"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-tag-green-bg"), tokens.get("--jc-tag-green-bg"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-tag-red-text"), tokens.get("--jc-tag-red-text"));
	}

	@Test void b03_chromeStaysLight_andPrimaryIsNotDangerOrMockupRed() {
		var tokens = Theme.LIGHT_RED.getTokens();
		assertNotEquals("#BF2600", tokens.get("--jc-chrome-bg"));
		assertNotEquals(Theme.RED.getTokens().get("--jc-chrome-bg"), tokens.get("--jc-chrome-bg"));
		assertNotEquals("#c23934", tokens.get("--jc-btn-primary"));
		assertNotEquals("#a31f34", tokens.get("--jc-btn-primary"));
		assertEquals("#c23934", tokens.get("--jc-danger"));
	}

	@Test void b04_noVarReferencesLeak_everyValueIsAResolvedLiteral() {
		for (var e : Theme.LIGHT_RED.getTokens().entrySet())
			assertFalse(e.getValue().contains("var("), () -> "token '" + e.getKey() + "' leaked an unresolved var() reference: " + e.getValue());
	}

	@Test void c01_derivedFromOpen_never_equalsOpensOwnName() {
		assertNotEquals(Theme.OPEN.getName(), Theme.LIGHT_RED.getName());
	}
}
