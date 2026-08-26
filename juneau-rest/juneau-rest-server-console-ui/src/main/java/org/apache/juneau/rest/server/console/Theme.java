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
import java.util.regex.*;

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
 * <p>
 * A token value may also be a {@code var(--jc-name)} <b>reference</b> to another known token. References are
 * <i>not</i> a {@code CssValueGrammar} value shape: {@code Theme.Builder} recognizes them one layer above the
 * grammar and resolves each to a concrete literal at {@link Builder#build() build()} time (own tokens shadowing
 * {@link #OPEN}'s), so the substring {@code var(} never appears in any {@link #getTokens()} value &mdash; only the
 * resolved literal, which is itself re-validated by the grammar, is ever emitted. An unknown reference, a cycle,
 * or a chain longer than the resolution depth cap is a loud {@code build()} failure, never a silent fallback.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	Theme <jv>corporate</jv> = Theme.<jsm>create</jsm>(<js>"corporate"</js>)
 * 		.token(<js>"--jc-font"</js>, <js>"'Source Sans 3', Inter, sans-serif"</js>)
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
	 * Anchored recognizer for a {@code var(--jc-name)} Theme-layer reference, run on the post-belt (control-char
	 * rejected, comment-stripped, trimmed) value.
	 *
	 * <p>
	 * Fully anchored ({@code ^...$}) so a reference can only ever be the <b>entire</b> value, never a prefix or
	 * suffix of some larger value: {@code linear-gradient(var(--jc-a), #fff)} never matches this and is rejected by
	 * the unchanged grammar. There is no fallback branch &mdash; {@code var(--jc-x, #fff)} simply fails to match
	 * (comma inside the parens) and falls through to grammar rejection.
	 */
	private static final Pattern VAR_REFERENCE = Pattern.compile("^[Vv][Aa][Rr]\\(\\s*(--jc-[a-z0-9-]+)\\s*\\)$");

	/**
	 * The maximum number of reference hops resolved before {@code build()} fails, enforced independently of cycle
	 * detection so a long <i>acyclic</i> chain is bounded too.
	 */
	private static final int MAX_REFERENCE_HOPS = 8;

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
		// Additive token gaps.  Appended after the tag palette so --jc-font stays first and the tag
		// triads stay contiguous (see Theme_TokenOrdering_Test.a02).  All are behaviour-preserving: each default
		// equals the literal it replaces in chrome.css, so the shipped chrome renders pixel-identically.
		.token("--jc-header-height", "56px")               // (a) keeps .jc-header height and .jc-nav sticky offset in sync
		.token("--jc-nav-indicator-width", "3px")          // (g) nav underline / active-tab indicator thickness
		.token("--jc-card-shadow", "none")                 // (c) card elevation seam; flat by default (relies on the "none" keyword)
		.token("--jc-danger-wash", "#fdeceb")              // (d) themeable .jc-btn-danger:hover fill (was a hardcoded hex)
		.token("--jc-success-wash", "#eaf6ee")             // (d) themeable .jc-btn-success:hover fill (was a hardcoded hex)
		.token("--jc-space-1", "4px")                      // (e) spacing scale seam (consumed by the .jc-prose rhythm)
		.token("--jc-space-2", "8px")
		.token("--jc-space-3", "12px")
		.token("--jc-space-4", "16px")
		.token("--jc-space-5", "24px")
		.token("--jc-space-6", "32px")
		// Accessibility: a themeable focus-ring colour so the generic :focus-visible rule in chrome.css
		// gives every interactive control a WCAG 2.4.7-compliant keyboard-focus indicator by default.  Defaults to
		// the Lightning-blue accent so the ring reads as "the same brand colour" out of the box.
		.token("--jc-focus", "#1589EE")
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
		 * <p>
		 * The value may be either a <b>literal</b> or a {@code var(--jc-name)} <b>reference</b> to another known
		 * token:
		 * <ul class='spaced-list'>
		 * 	<li>A literal is validated eagerly here by {@code CssValueGrammar}'s accept-known-safe allowlist grammar
		 * 		after normalization (trim &rarr; reject C0/C1/DEL &rarr; strip comments &rarr; reject {@code url(} in
		 * 		any spelling &rarr; grammar).
		 * 	<li>A value that, after that same normalization belt, matches {@code var(--jc-name)} is recognized as a
		 * 		reference and stored <i>unresolved</i> &mdash; it is resolved to a concrete literal at {@link #build()}
		 * 		time (its target may not be defined yet, e.g. a forward reference). {@code var()} is never a
		 * 		{@code CssValueGrammar} value shape; it is Theme-layer syntax recognized one layer above the grammar.
		 * </ul>
		 * Escaping happens later, at emission time, via {@code CssValueEscaper} (see {@code ConsoleChromeMixin}).
		 *
		 * @param name The token name. Must not be <jk>null</jk> and must match {@code ^--jc-[a-z0-9-]+$}.
		 * @param value
		 * 	The CSS value &mdash; a literal, or a {@code var(--jc-name)} reference.
		 * @return This object.
		 * @throws IllegalArgumentException
		 * 	If the name is not in the legal shape (full-string {@code String.matches(...)}, not {@code find(...)}
		 * 	&mdash; {@code "--jc-foo;--bar"} must REJECT even though {@code "--jc-foo"} matches as a leading
		 * 	substring), if the value contains a control character or a {@code url(} production, or if the value is
		 * 	neither a {@code var(--jc-name)} reference nor one of the allowlisted CSS value shapes. A reference whose
		 * 	target is unknown, cyclic, or too deeply chained is instead reported at {@link #build()} time.
		 */
		public Builder token(String name, String value) {
			if (name == null || ! name.matches(TOKEN_NAME_PATTERN))
				throw iaex("Invalid theme token name: '%s'.  Must match %s.", name, TOKEN_NAME_PATTERN);
			// Recognition runs on the SAME post-belt string the grammar would see (one shared belt, no second
			// comment-stripping pass).  A reference is stored unresolved; a literal is validated eagerly, exactly
			// as before this feature existed.
			var normalized = CssValueGrammar.normalize(value);
			if (referencedName(normalized) != null)
				tokens.put(name, normalized);
			else
				tokens.put(name, CssValueGrammar.normalizeAndValidate(value));
			return this;
		}

		/**
		 * Builds the immutable {@link Theme}, resolving every {@code var(--jc-name)} reference to a concrete literal.
		 *
		 * <p>
		 * Resolution scope is this builder's own tokens, shadowing {@link Theme#OPEN}'s tokens (exact shadowing: a
		 * name defined on this builder wins outright, even if resolving that entry then fails &mdash; there is no
		 * silent fall-through to {@code Theme.OPEN}'s value for a shadowed name). Resolution runs on a copy of the
		 * token map, so a failed {@code build()} leaves this builder unchanged and retryable.
		 *
		 * @return A new {@link Theme} whose every token value is a resolved six-shape literal (the substring
		 * 	{@code var(} appears in none of them).
		 * @throws IllegalArgumentException
		 * 	If a reference names an unknown token, forms a cycle (the message carries the cycle path), or exceeds the
		 * 	resolution depth cap.
		 */
		public Theme build() {
			return new Theme(name, resolveReferences());
		}

		/** Resolves every reference in a copy of the token map, preserving declaration order; never mutates {@link #tokens}. */
		private Map<String,String> resolveReferences() {
			var resolved = new LinkedHashMap<String,String>();
			for (var e : tokens.entrySet()) {
				var target = referencedName(e.getValue());
				resolved.put(e.getKey(), target == null ? e.getValue() : resolveReference(e.getKey(), target));
			}
			return resolved;
		}

		/**
		 * Iteratively walks a reference chain from {@code firstTarget} to the literal it names, with exact shadowing
		 * (own tokens win over {@link Theme#OPEN}'s), cycle detection, and an independent hop cap; re-validates the
		 * resolved literal against the grammar as defense-in-depth.
		 */
		private String resolveReference(String definingName, String firstTarget) {
			// Theme.OPEN is null only while Theme.OPEN itself is being built - and it has no references, so this
			// fallback map is never actually consulted during that construction.
			var openTokens = Theme.OPEN == null ? Collections.<String,String>emptyMap() : Theme.OPEN.getTokens();
			var visited = new LinkedHashSet<String>();
			visited.add(definingName);
			var target = firstTarget;
			var hops = 0;
			while (true) {
				if (visited.contains(target))
					throw iaex("Theme token '%s' contains a cyclic reference: %s.", definingName, cyclePath(visited, target));
				if (++hops > MAX_REFERENCE_HOPS)
					throw iaex("Theme token '%s' exceeds the maximum reference depth of %d hops.", definingName, MAX_REFERENCE_HOPS);
				visited.add(target);

				String targetValue;
				if (tokens.containsKey(target))
					targetValue = tokens.get(target);          // own map wins outright (exact shadowing)
				else if (openTokens.containsKey(target))
					targetValue = openTokens.get(target);
				else
					throw iaex("Theme token '%s' references unknown token '%s'.", definingName, target);

				var next = referencedName(targetValue);
				if (next == null)
					// The resolved literal - and only that literal, with no var() present - is re-validated by the
					// unchanged grammar as defense-in-depth against a bad value hiding under a referenceable name.
					return CssValueGrammar.normalizeAndValidate(targetValue);
				target = next;
			}
		}

		/** Returns the {@code --jc-name} a {@code var(--jc-name)} reference points at, or <jk>null</jk> if {@code value} is not a reference. */
		private static String referencedName(String value) {
			var m = VAR_REFERENCE.matcher(value);
			return m.matches() ? m.group(1) : null;
		}

		/** Renders a cycle path like {@code --jc-a -> --jc-b -> --jc-a} for a build-failure message. */
		private static String cyclePath(Set<String> visited, String repeated) {
			return String.join(" -> ", visited) + " -> " + repeated;
		}
	}
}
