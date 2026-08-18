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
 * Most token values just restate {@link Theme#OPEN}'s own values &mdash; the app's existing look already matches
 * the shipped default. The {@code --jc-tag-red-*} triad is the one deliberate divergence: it overrides
 * {@link Theme#OPEN}'s Bootstrap-maroon default with this app's own pre-existing danger palette (the same colors
 * {@code .rm-mode-banner.live}/{@code .tag.armed}/{@code .pill.invalid} already used), so the FAILED tag/stage
 * pill now resolves through the token system without changing how it looks. A dedicated class (rather than an
 * inline builder call at the mixin-wiring site) gives the app one obvious, named place to diverge its palette. The
 * logo and page-background image are
 * deliberately <b>not</b> part of this theme &mdash; those are {@code ConsoleChromeMixin} builder inputs (see
 * {@link ConsoleAssetsRest}), kept out of the token model entirely.
 */
public final class ReleaseManagerTheme {

	/** The Release Manager's console-ui theme. */
	public static final Theme INSTANCE = build();

	private ReleaseManagerTheme() {}

	private static Theme build() {
		return Theme.create("release-manager")
			.token("--jc-font", "'Inter', 'Source Sans 3', system-ui, sans-serif")
			.token("--jc-page-bg", "linear-gradient(180deg, #b0c4df 0%, #c7d5e8 22%, #e4eaf2 55%, #f5f6f9 100%)")
			.token("--jc-header-icon-color", "#747474")
			.token("--jc-accent", "#1589EE")
			.token("--jc-accent-wash", "rgba(21,137,238,0.1)")
			.token("--jc-link", "#0174d3")
			.token("--jc-text", "#080707")
			.token("--jc-text-soft", "#080707cc")
			.token("--jc-text-muted", "#706e6b")
			.token("--jc-border", "#dddbda")
			.token("--jc-border-2", "#ced4da")
			.token("--jc-card-bg", "#f5f6f9")
			.token("--jc-chrome-bg", "#f3f2f2")
			.token("--jc-white", "#ffffff")
			.token("--jc-btn-primary", "#1a5297")
			.token("--jc-btn-primary-hover", "#005fb2")
			.token("--jc-danger", "#c23934")
			.token("--jc-success", "#2e844a")
			.token("--jc-avatar-bg", "linear-gradient(135deg, #1589EE, #1a5297)")
			.token("--jc-radius", "0.25rem")
			.token("--jc-tag-green-bg", "#b8e6c4")
			.token("--jc-tag-green-text", "#155724")
			.token("--jc-tag-green-border", "#9fd6ad")
			.token("--jc-tag-blue-bg", "#dceefb")
			.token("--jc-tag-blue-text", "#0c5460")
			.token("--jc-tag-blue-border", "#c3e0f3")
			.token("--jc-tag-amber-bg", "#fff3cd")
			.token("--jc-tag-amber-text", "#856404")
			.token("--jc-tag-amber-border", "#ffe69c")
			.token("--jc-tag-neutral-bg", "#e2e3e5")
			.token("--jc-tag-neutral-text", "#383d41")
			.token("--jc-tag-neutral-border", "#c6c8ca")
			// Overrides Theme.OPEN's Bootstrap-maroon red default with this app's existing danger palette, so the
			// FAILED tag/stage pill (formerly a hardcoded rule in new-release.css) stays visually identical to
			// .rm-mode-banner.live / .rm-mode-chip.live / .tag.armed / .pill.invalid, which all key off --jc-danger.
			// --jc-tag-red-text is #c23934 rather than var(--jc-danger) because CssValueGrammar's allowlist grammar
			// has no var() production - token values must be literals. Keep this literal in sync with --jc-danger
			// above; a silent divergence between the two is exactly the bug this comment exists to prevent.
			.token("--jc-tag-red-bg", "#fdeceb")
			.token("--jc-tag-red-text", "#c23934")
			.token("--jc-tag-red-border", "#f3c6c2")
			.build();
	}
}
