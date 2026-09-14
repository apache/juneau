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
package org.apache.juneau.releng;

import org.apache.juneau.rest.server.console.Theme;

/**
 * The Release Manager app's console-ui theme.
 *
 * <p>
 * Seeded from {@link Theme#LIGHT_RED} (light chrome, red accents). The {@code --jc-tag-red-*} triad is the
 * one deliberate divergence: it overrides {@link Theme#OPEN}'s Bootstrap-maroon default with this app's own
 * pre-existing danger palette (the same colors {@code .rm-mode-banner.live}/{@code .tag.armed}/{@code .pill.invalid}
 * already used), so the FAILED tag/stage pill now resolves through the token system without changing how it
 * looks. Do not pass {@link Theme#LIGHT_RED} directly to {@code ConsoleChromeMixin.theme} — that would carry
 * OPEN's maroon tag triad and regress FAILED pills.
 *
 * <p>
 * The logo and page-background image are deliberately <b>not</b> part of this theme &mdash; those are
 * {@code ConsoleChromeMixin} builder inputs (see {@link ConsoleAssetsRest}), kept out of the token model entirely.
 */
public final class ReleaseManagerTheme {

	/** The Release Manager's console-ui theme. */
	public static final Theme INSTANCE = build();

	private ReleaseManagerTheme() {}

	private static Theme build() {
		return Theme.deriveFrom("release-manager", Theme.LIGHT_RED)
			// Overrides Theme.OPEN/LIGHT_RED's Bootstrap-maroon red default with this app's existing danger
			// palette, so the FAILED tag/stage pill stays visually identical to .rm-mode-banner.live /
			// .rm-mode-chip.live / .tag.armed / .pill.invalid, which all key off --jc-danger.
			// --jc-tag-red-text is #c23934 rather than var(--jc-danger) because CssValueGrammar's allowlist
			// grammar has no var() production - token values must be literals. Keep this literal in sync with
			// OPEN's --jc-danger; a silent divergence between the two is exactly the bug this comment exists
			// to prevent.
			.token("--jc-tag-red-bg", "#fdeceb")
			.token("--jc-tag-red-text", "#c23934")
			.token("--jc-tag-red-border", "#f3c6c2")
			.build();
	}
}
