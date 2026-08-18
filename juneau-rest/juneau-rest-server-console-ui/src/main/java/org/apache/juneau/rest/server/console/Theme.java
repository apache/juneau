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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

/**
 * An immutable, named set of CSS custom-property ("theme token") overrides for the admin-console chrome.
 *
 * <p>
 * A {@link Theme} is pure data &mdash; it carries no classpath assets (fonts/logos/background images; that is a
 * fast-follow, see the design's P3) &mdash; just a name and a map of {@code --jc-*} token names to CSS values.
 * {@code ConsoleChromeMixin} appends the active theme's tokens to the served {@code chrome.css} response as a
 * {@code :root{}} block, on top of {@link #OPEN}'s own block.
 *
 * <h5 class='section'>Security:</h5>
 * <p>
 * Identifiers (theme name, token name) are REJECTed fail-closed by anchored {@code String.matches(...)} guards.
 * Token values are validated by {@code CssValueGrammar}'s accept-known-safe allowlist grammar (not a
 * {@code url(}-blocklist) and are escaped at emission time by {@code CssValueEscaper} (see
 * {@code ConsoleChromeMixin}). {@link #OPEN} is a placeholder empty theme in this revision; its real token set
 * lands together with {@code chrome.css}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	Theme <jv>salesforce</jv> = Theme.<jsm>create</jsm>(<js>"salesforce"</js>)
 * 		.token(<js>"--jc-font"</js>, <js>"'Salesforce Sans', Inter, sans-serif"</js>)
 * 		.token(<js>"--jc-accent"</js>, <js>"#1589EE"</js>)
 * 		.build();
 * </p>
 *
 * @since 10.0.0
 */
public final class Theme {

	/** Anchored (full-string) guard for a theme name. Anchoring matters: an unanchored {@code find()} would accept {@code "--jc-foo;--bar"}-shaped garbage as a substring match. */
	private static final String NAME_PATTERN = "^[a-z][a-z0-9-]*$";

	/** Anchored (full-string) guard for a CSS custom-property token name. */
	private static final String TOKEN_NAME_PATTERN = "^--jc-[a-z0-9-]+$";

	/**
	 * The default, "open" theme.
	 *
	 * <p>
	 * Ships with the framework so out-of-the-box adoption needs zero theme configuration: an Inter/system-ui font
	 * stack, a neutral blue-gray page gradient, and Lightning-blue accents.  Its token set is exactly the set
	 * {@code chrome.css} references &mdash; see {@code ConsoleChromeMixin_Test}'s bidirectional cross-check (every
	 * token here is a {@code var(--jc-*)} in {@code chrome.css}, and vice versa).
	 */
	public static final Theme OPEN = create("open")
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
		.token("--jc-tag-red-bg", "#f8d7da")
		.token("--jc-tag-red-text", "#721c24")
		.token("--jc-tag-red-border", "#f5c6cb")
		.build();

	private final String name;
	private final Map<String,String> tokens;

	private Theme(String name, Map<String,String> tokens) {
		this.name = name;
		// Insertion-ordered rather than Map.copyOf: ConsoleChromeMixin emits this map's iteration order directly as
		// the served stylesheet's :root{} declarations, and Map.copyOf's order is perturbed by a per-JVM salt - which
		// would make the response body differ between two processes serving an identical token set.  cp(Map) is
		// contractually a LinkedHashMap copy, so it preserves that order; do not swap it for an unordered copy.
		this.tokens = u(cp(tokens));
	}

	/**
	 * Starts building a new named theme.
	 *
	 * @param name The theme name. Must not be <jk>null</jk> and must match {@code ^[a-z][a-z0-9-]*$}.
	 * @return A new builder.
	 * @throws IllegalArgumentException If the name is <jk>null</jk>, empty, or not in the legal shape (e.g. contains
	 * 	uppercase, whitespace, or a path-traversal-shaped segment like {@code "../evil"} &mdash; gated here, not
	 * 	just at classpath-asset-resolution time, so a later fast-follow that interpolates the name into an asset
	 * 	path doesn't need to re-gate).
	 */
	public static Builder create(String name) {
		if (name == null || ! name.matches(NAME_PATTERN))
			throw iaex("Invalid theme name: '%s'.  Must match %s.", name, NAME_PATTERN);
		return new Builder(name);
	}

	/**
	 * Returns this theme's name.
	 *
	 * @return This theme's name.
	 */
	public String getName() { return name; }

	/**
	 * Returns this theme's token overrides.
	 *
	 * @return
	 * 	An immutable map of token name (e.g. {@code "--jc-accent"}) to CSS value, iterating in the order the tokens
	 * 	were declared on the builder. Never <jk>null</jk>.
	 */
	public Map<String,String> getTokens() { return tokens; }

	/**
	 * Builder for {@link Theme}.
	 */
	public static final class Builder {
		private final String name;
		private final Map<String,String> tokens = new LinkedHashMap<>();

		private Builder(String name) { this.name = name; }

		/**
		 * Adds (or overrides) a single CSS custom-property token.
		 *
		 * @param name The token name. Must not be <jk>null</jk> and must match {@code ^--jc-[a-z0-9-]+$}.
		 * @param value
		 * 	The CSS value.  Validated (not escaped) by {@code CssValueGrammar}'s accept-known-safe allowlist
		 * 	grammar after normalization (trim &rarr; reject C0/C1/DEL &rarr; strip comments &rarr; reject
		 * 	{@code url(} in any spelling &rarr; grammar).  Escaping happens later, at emission time, via
		 * 	{@code CssValueEscaper} (see {@code ConsoleChromeMixin}).
		 * @return This object.
		 * @throws IllegalArgumentException
		 * 	If the name is not in the legal shape (full-string {@code String.matches(...)}, not {@code find(...)}
		 * 	&mdash; {@code "--jc-foo;--bar"} must REJECT even though {@code "--jc-foo"} matches as a leading
		 * 	substring), or if the value is not one of the allowlisted CSS value shapes.
		 */
		public Builder token(String name, String value) {
			if (name == null || ! name.matches(TOKEN_NAME_PATTERN))
				throw iaex("Invalid theme token name: '%s'.  Must match %s.", name, TOKEN_NAME_PATTERN);
			tokens.put(name, CssValueGrammar.normalizeAndValidate(value));
			return this;
		}

		/**
		 * Builds the immutable {@link Theme}.
		 *
		 * @return A new {@link Theme}.
		 */
		public Theme build() {
			return new Theme(name, tokens);
		}
	}
}
