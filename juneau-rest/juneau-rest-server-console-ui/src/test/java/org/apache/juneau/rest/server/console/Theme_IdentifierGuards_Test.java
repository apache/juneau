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
 * Phase 1 gate: {@link Theme#create(String)} / {@link Theme.Builder#token(String, String)} identifier REJECT
 * guards.
 *
 * <p>
 * These tests are written to run against a deliberately-unvalidated {@code Theme} skeleton first (proving the guard
 * is genuinely absent, RED), and again once the two anchored {@code String.matches(...)} guards are added (GREEN).
 * Token <i>values</i> are intentionally NOT covered here &mdash; that is a separate, later gate (the allowlist
 * grammar).
 */
class Theme_IdentifierGuards_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Token name REJECT
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_tokenName_noJcPrefix_rejects() {
		assertThrows(IllegalArgumentException.class, () -> Theme.create("salesforce").token("rm-accent", "red"));
	}

	@Test void a02_tokenName_withSpace_rejects() {
		assertThrows(IllegalArgumentException.class, () -> Theme.create("salesforce").token("--jc-bad name", "red"));
	}

	/**
	 * The anchoring proof: an unanchored {@code find()}-based regex would incorrectly ACCEPT this because
	 * {@code --jc-foo} matches as a substring; only a full-string {@code matches("^--jc-[a-z0-9-]+$")} rejects it.
	 */
	@Test void a03_tokenName_trailingIllegalSuffix_rejects_anchoringProof() {
		assertThrows(IllegalArgumentException.class, () -> Theme.create("salesforce").token("--jc-foo;--bar", "red"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Theme name REJECT
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a04_themeName_uppercase_rejects() {
		assertThrows(IllegalArgumentException.class, () -> Theme.create("Salesforce"));
	}

	/** Path-shaped names are rejected here so a later fast-follow that interpolates the name into an asset path doesn't need to re-gate. */
	@Test void a05_themeName_pathShaped_rejects() {
		assertThrows(IllegalArgumentException.class, () -> Theme.create("../evil"));
	}

	@Test void a06_themeName_empty_rejects() {
		assertThrows(IllegalArgumentException.class, () -> Theme.create(""));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Positive control
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a07_positiveControl_roundTrips() {
		var theme = Theme.create("salesforce").token("--jc-accent", "#1589EE").build();
		assertEquals("salesforce", theme.getName());
		assertEquals("#1589EE", theme.getTokens().get("--jc-accent"));
	}
}
