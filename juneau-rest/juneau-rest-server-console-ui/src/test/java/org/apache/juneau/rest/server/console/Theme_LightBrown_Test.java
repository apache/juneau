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
 * {@link Theme#LIGHT_BROWN} &mdash; the first stock token-{@link Theme} beyond {@link Theme#OPEN}.
 *
 * <p>
 * The load-bearing guard is (a01): because {@link Theme#LIGHT_BROWN} is authored via
 * {@link Theme#deriveFrom(String, Theme) deriveFrom}({@link Theme#OPEN}), its token <i>key</i> set must be
 * provably identical to {@link Theme#OPEN}'s, which is what protects the {@code chrome.css} bidirectional
 * cross-check ({@code ConsoleChromeMixin_Test}) for this theme too.
 */
class Theme_LightBrown_Test extends TestBase {

	@Test void a01_tokenKeySet_isIdenticalToOpens() {
		assertEquals(Theme.OPEN.getTokens().keySet(), Theme.LIGHT_BROWN.getTokens().keySet());
	}

	@Test void a02_tokenCount_is48_mirroringOpensPin() {
		assertEquals(48, Theme.LIGHT_BROWN.getTokens().size());
	}

	@Test void a03_name_isLightBrown() {
		assertEquals("light-brown", Theme.LIGHT_BROWN.getName());
	}

	@Test void b01_brownedSignatureValues() {
		var tokens = Theme.LIGHT_BROWN.getTokens();
		assertEquals("#a9772f", tokens.get("--jc-accent"));
		assertEquals("#8a5a1a", tokens.get("--jc-link"));
		assertEquals("#8a6d3b", tokens.get("--jc-btn-primary"));
		assertTrue(tokens.get("--jc-page-bg").contains("#dccaa6"), () -> "expected the brown gradient, got: " + tokens.get("--jc-page-bg"));
	}

	@Test void b02_keptFromOpen_statusAndTagValues_areUnchanged() {
		var tokens = Theme.LIGHT_BROWN.getTokens();
		assertEquals(Theme.OPEN.getTokens().get("--jc-white"), tokens.get("--jc-white"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-danger"), tokens.get("--jc-danger"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-success"), tokens.get("--jc-success"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-tag-green-bg"), tokens.get("--jc-tag-green-bg"));
		assertEquals(Theme.OPEN.getTokens().get("--jc-tag-red-text"), tokens.get("--jc-tag-red-text"));
	}

	@Test void b03_noVarReferencesLeak_everyValueIsAResolvedLiteral() {
		for (var e : Theme.LIGHT_BROWN.getTokens().entrySet())
			assertFalse(e.getValue().contains("var("), () -> "token '" + e.getKey() + "' leaked an unresolved var() reference: " + e.getValue());
	}

	@Test void c01_derivedFromOpen_never_equalsOpensOwnName() {
		assertNotEquals(Theme.OPEN.getName(), Theme.LIGHT_BROWN.getName());
	}
}
