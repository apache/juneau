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
 * An immutable, named bundle of a {@link Theme} of <b>resolved-literal</b> leaf tokens plus a
 * <b>reference-only</b> map of derived <i>alias</i> tokens, with two optional classpath image assets.
 *
 * <p>
 * The contract in one sentence: <b>a leaf is always a literal; an alias is always a reference; and no token name
 * is declared by both.</b> The two channels are disjoint by <i>value shape</i> rather than by convention, and each
 * carries its own validation:
 *
 * <table class='styled'>
 * 	<tr><th>&nbsp;</th><th>leaf ({@link #getTheme()})</th><th>alias ({@link #getAliases()})</th></tr>
 * 	<tr><td>example</td><td><c>--jc-accent: #b45309</c></td><td><c>--jc-tab-bar-bg: var(--jc-card-bg)</c></td></tr>
 * 	<tr><td>value shape</td><td>literal</td><td>reference</td></tr>
 * 	<tr><td>validated by</td><td>{@code CssValueGrammar}'s accept-known-safe allowlist</td><td>the anchored
 * 		{@code var(--jc-name)} recognizer ({@link Builder#alias(String, String)})</td></tr>
 * 	<tr><td>emitted</td><td>escaped, via {@code CssValueEscaper}</td><td>verbatim &mdash; see
 * 		<i>Injection safety</i> below</td></tr>
 * </table>
 *
 * <p>
 * The rule a pack author needs to remember: <b>a literal belongs in the pack's {@link Theme}; a reference belongs
 * in the alias map.</b> To make {@code --jc-tab-bar-bg} be {@code #fff}, declare it as a {@link Theme} token. To
 * make it <i>follow</i> {@code --jc-card-bg} through the live cascade, {@link Builder#alias(String, String) alias}
 * it.
 *
 * <h5 class='section'>Why the alias channel exists at all:</h5>
 * <p>
 * A {@link Theme} cannot carry a derived token. {@link Theme.Builder#build()} recognizes a
 * {@code var(--jc-name)} value as a reference and <b>resolves it to a frozen literal</b>, which snapshots the live
 * CSS cascade at composition time &mdash; so a token that must <i>keep following</i> its target can never be a
 * {@link Theme} leaf. {@code ConsoleChromeMixin.OPEN_ROLE_ALIASES} is the framework's own answer to the same
 * problem, but those are unconditional framework defaults rather than per-pack derivations. This map is the third
 * channel: <b>consumer-authored derivations that survive to the wire as references.</b>
 *
 * <h5 class='section'>The var() asymmetry &mdash; read this before "unifying" the two paths:</h5>
 * <p>
 * These two adjacent channels have <b>opposite</b> {@code var()} semantics, deliberately:
 * <ul class='spaced-list'>
 * 	<li>A {@code var()} in a <b>{@link Theme} token</b> is <b>resolved away</b> before the wire
 * 		({@link Theme.Builder#build()}), so the served declaration carries the literal. That is asserted by
 * 		{@code ConsoleChromeMixin_Test.p01}, which pins that {@code --jc-tag-red-text:var(} never appears in the
 * 		body.
 * 	<li>A {@code var()} in an <b>alias</b> must <b>survive as a reference</b> in the served declaration. Freezing
 * 		it to a literal would defeat the entire purpose of the channel.
 * </ul>
 * <p>
 * So a change that "unifies the two {@code var()} paths" silently destroys this type's reason to exist. The same
 * warning is repeated at the emission site ({@code ConsoleChromeMixin.packRootBlock}), because that is the other
 * place the mistake is reachable from.
 *
 * <h5 class='section'>Injection safety:</h5>
 * <p>
 * Every alias value must match the fully-anchored {@code var(--jc-name)} shape ({@link Theme#VAR_REFERENCE}), which
 * admits <b>exactly</b> this alphabet and nothing else: the three letters of {@code var} in either case, an opening
 * paren, optional ASCII spaces, the literal {@code --jc-}, one or more of {@code [a-z0-9-]}, optional ASCII spaces,
 * and a closing paren. (The recognizer's whitespace class can only ever match a plain space here, because every
 * other character it would accept &mdash; tab, newline, carriage return, form feed, vertical tab &mdash; is a C0
 * control character that {@code CssValueGrammar}'s belt has already rejected on the raw value, before this
 * recognizer runs.)
 *
 * <p>
 * So not one of {@code ;}, <code>}</code>, {@code "}, {@code '}, {@code \}, <code>/&#42;</code>, {@code url(} or a
 * control character is <i>representable</i> in an accepted value &mdash; the emitted text is provably safe with
 * <b>no escaper at all</b>. That is a strictly stronger claim than the leaf channel's "safe because we escape it",
 * and it is the reason the channel is restricted to references rather than given an escaper of its own (escaping a
 * {@code var()} reference would corrupt its parens).
 *
 * <p>
 * A corollary worth stating: because an alias cannot carry a literal, the alias channel is <b>structurally</b>
 * incapable of smuggling in a colour value, a font stack, or any other palette content. It can only ever point one
 * name at another.
 *
 * <h5 class='section'>Assets:</h5>
 * <p>
 * {@link Builder#logo(String)} / {@link Builder#pageBackgroundImage(String)} carry classpath image paths through
 * the same fail-closed validation {@code ConsoleChromeMixin.Builder.logo(String)} applies. They are honoured
 * <b>only</b> when the pack is supplied through {@code ConsoleChromeMixin.Builder.pack(ThemePack)} &mdash; see
 * that method's javadoc for the documented limitation, and {@code ConsoleChromeMixin.Builder.logo(String)} for
 * which one wins.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	ThemePack <jv>corporate</jv> = ThemePack.<jsm>create</jsm>(<js>"corporate"</js>)
 * 		.theme(
 * 			Theme.<jsm>deriveFrom</jsm>(<js>"corporate"</js>, Theme.<jsf>OPEN</jsf>)
 * 				.token(<js>"--jc-accent"</js>, <js>"#b45309"</js>)
 * 				.token(<js>"--jc-accent-wash"</js>, <js>"rgba(180,83,9,0.1)"</js>)
 * 				.build()
 * 		)
 * 		.alias(<js>"--jc-tab-bar-bg"</js>, <js>"var(--jc-card-bg)"</js>)
 * 		.build();
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Theme}
 * 	<li class='jc'>{@link ThemePackSettings}
 * 	<li class='jc'>{@link ConsoleChromeMixin}
 * </ul>
 *
 * @since 10.0.0
 */
public final class ThemePack {

	/**
	 * Anchored (full-string) guard for a pack id &mdash; deliberately the same shape {@link Theme#create(String)}
	 * applies to a theme name.
	 *
	 * <p>
	 * Held as its own copy rather than shared with {@code Theme}'s constant: only the security-relevant
	 * {@code var(--jc-name)} recognizer is shared (a duplicated <i>value</i> recognizer that drifts out of sync
	 * opens a silent hole). An identifier-shape guard is not in that category &mdash; a divergence between these
	 * two copies can only make one type accept an id the other rejects, which fails closed at the stricter of the
	 * two and can never widen the accepted <i>value</i> alphabet.
	 */
	private static final String ID_PATTERN = "^[a-z][a-z0-9-]*$";

	/** Anchored (full-string) guard for an alias token name &mdash; the same shape a {@link Theme} token name uses. */
	private static final String ALIAS_NAME_PATTERN = "^--jc-[a-z0-9-]+$";

	private final String id;
	private final Theme theme;
	private final Map<String,String> aliases;
	private final String logoResource;
	private final String pageBackgroundResource;

	private ThemePack(Builder builder) {
		this.id = builder.id;
		this.theme = builder.theme;
		// Insertion-ordered rather than Map.copyOf, for the reason Theme's own constructor documents:
		// ConsoleChromeMixin emits this map's iteration order directly as the served stylesheet's declarations, and
		// Map.copyOf's order is perturbed by a per-JVM salt - which would make the response body differ between two
		// processes serving an identical alias set.  cp(Map) is contractually a LinkedHashMap copy, so it preserves
		// that order; do not swap it for an unordered copy.
		this.aliases = u(cp(builder.aliases));
		this.logoResource = builder.logoResource;
		this.pageBackgroundResource = builder.pageBackgroundResource;
	}

	/**
	 * Starts building a new pack under the specified id.
	 *
	 * <p>
	 * The returned builder seeds <b>nothing</b>: no theme, and an empty alias map. A pack's content is entirely the
	 * caller's.
	 *
	 * @param id
	 * 	The pack id. Must not be <jk>null</jk> and must match {@code ^[a-z][a-z0-9-]*$}.
	 * @return A new builder.
	 * @throws IllegalArgumentException
	 * 	If the id is <jk>null</jk>, empty, or not in the legal shape (e.g. contains uppercase, whitespace, or a
	 * 	path-traversal-shaped segment like {@code "../evil"}) &mdash; gated here rather than at asset-resolution
	 * 	time so a later feature that interpolates the id into a path or an attribute value needs no second gate.
	 */
	public static Builder create(String id) {
		if (id == null || ! id.matches(ID_PATTERN))
			throw iaex("Invalid theme pack id: '%s'.  Must match %s.", id, ID_PATTERN);
		return new Builder(id);
	}

	/**
	 * Returns this pack's id.
	 *
	 * @return This pack's id. Never <jk>null</jk>.
	 */
	public String getId() { return id; }

	/**
	 * Returns this pack's leaf tokens, as a {@link Theme}.
	 *
	 * <p>
	 * Every value is a resolved literal (the substring {@code var(} appears in none of them &mdash; see
	 * {@link Theme#getTokens()}).
	 *
	 * @return This pack's theme. Never <jk>null</jk>.
	 */
	public Theme getTheme() { return theme; }

	/**
	 * Returns this pack's derived alias tokens.
	 *
	 * <p>
	 * Every value is a {@code var(--jc-name)} reference; no value is a literal. The map iterates in the order the
	 * aliases were declared on the builder, which is the order they are emitted in &mdash; so the served response
	 * stays byte-stable for a given pack.
	 *
	 * <p>
	 * Deliberately a {@link Map} rather than pre-rendered CSS text: returning rendered text would make the
	 * <i>emission format</i> part of this public API, and it would leave a cross-stylesheet
	 * declared-versus-consumed scan (which needs both the declared names and the referenced names) to recover them
	 * with a regex. Rendering to CSS text is a private concern of {@code ConsoleChromeMixin}. <b>Do not "simplify"
	 * this accessor into a rendered String.</b>
	 *
	 * @return An immutable, insertion-ordered map of alias name to {@code var(--jc-name)} target. Never
	 * 	<jk>null</jk>; empty if the pack declares no aliases.
	 */
	public Map<String,String> getAliases() { return aliases; }

	/**
	 * Returns the classpath resource path of this pack's logo image, if it carries one.
	 *
	 * @return The logo resource path, or <jk>null</jk> if this pack carries no logo.
	 */
	public String getLogoResource() { return logoResource; }

	/**
	 * Returns the classpath resource path of this pack's page-background image, if it carries one.
	 *
	 * @return The page-background resource path, or <jk>null</jk> if this pack carries no page background.
	 */
	public String getPageBackgroundResource() { return pageBackgroundResource; }

	/**
	 * Builder for {@link ThemePack}.
	 *
	 * <p>
	 * Every guard below is fail-closed and throws {@link IllegalArgumentException}. Guards fire at the
	 * <b>setter</b> wherever the offending input arrives through one, so the stack trace names the offending call;
	 * the two that depend on both channels being populated ({@link #theme(Theme)} may be called either side of
	 * {@link #alias(String, String)}) fire at {@link #build()}.
	 */
	public static final class Builder {
		private final String id;
		private final Map<String,String> aliases = new LinkedHashMap<>();
		private Theme theme;
		private String logoResource;
		private String pageBackgroundResource;

		private Builder(String id) { this.id = id; }

		/**
		 * Sets this pack's leaf tokens. Required.
		 *
		 * <p>
		 * Every token value is already a resolved literal by the time it gets here &mdash; {@link Theme} validates
		 * and resolves at its own {@code build()} &mdash; so this method adds only the reserved-namespace check
		 * below.
		 *
		 * @param value The pack's theme. Must not be <jk>null</jk>.
		 * @return This object.
		 * @throws IllegalArgumentException
		 * 	If {@code value} is <jk>null</jk>, or if any of its token names is in the reserved chrome-scale
		 * 	namespace (see {@code ConsoleChromeMixin.Builder.theme(Theme)}). {@link Theme} legitimately permits
		 * 	those names; the narrower rule lives at this layer and at the emission boundary, not in {@link Theme}.
		 */
		public Builder theme(Theme value) {
			if (value == null)
				throw iaex("Invalid theme pack theme: null.");
			for (var name : value.getTokens().keySet())
				ConsoleChromeMixin.rejectReservedChromeDeclaration(name, "theme pack '" + id + "' leaf token");
			this.theme = value;
			return this;
		}

		/**
		 * Adds (or overrides) a single derived alias: a token name pointing at another token name.
		 *
		 * <p>
		 * {@code target} must be a {@code var(--jc-name)} <b>reference</b> and nothing else. A literal is not
		 * accepted here at all &mdash; put it in the pack's {@link Theme}. The reference is recognized by the same
		 * anchored recognizer {@link Theme.Builder#token(String, String)} uses, run on the same normalization belt
		 * (trim, reject control characters, strip comments, reject any {@code url(} spelling), so
		 * {@code var(--jc-x, #fff)}, {@code linear-gradient(var(--jc-a), #fff)} and
		 * {@code var(--jc-a);color:red} all REJECT rather than partially matching.
		 *
		 * <p>
		 * The reference is stored <b>unresolved</b> and reaches the served stylesheet <b>as a reference</b> &mdash;
		 * the opposite of what happens to a {@code var()} written on a {@link Theme} token. See the class
		 * javadoc's <i>var() asymmetry</i> section.
		 *
		 * <p>
		 * Note what is deliberately <b>not</b> checked: the alias's <i>target</i>. Pointing an alias <i>at</i> a
		 * reserved chrome-scale step is legal &mdash; <i>reading</i> the shared control ladder is exactly what a
		 * pack should be able to do. Only the <b>declared name</b> is namespace-checked.
		 *
		 * @param name The alias token name. Must not be <jk>null</jk> and must match {@code ^--jc-[a-z0-9-]+$}.
		 * @param target
		 * 	The token this alias points at. Must not be <jk>null</jk> and must be a {@code var(--jc-name)}
		 * 	reference &mdash; never a literal.
		 * @return This object.
		 * @throws IllegalArgumentException
		 * 	If {@code name} is not in the legal shape (full-string {@code String.matches(...)}, not
		 * 	{@code find(...)} &mdash; {@code "--jc-foo;--bar"} must REJECT even though {@code "--jc-foo"} matches as
		 * 	a leading substring), if {@code name} is in the reserved chrome-scale namespace, or if {@code target} is
		 * 	<jk>null</jk>, contains a control character or a {@code url(} production, or is not a
		 * 	{@code var(--jc-name)} reference.
		 */
		public Builder alias(String name, String target) {
			if (name == null || ! name.matches(ALIAS_NAME_PATTERN))
				throw iaex("Invalid theme pack alias name: '%s'.  Must match %s.", name, ALIAS_NAME_PATTERN);
			ConsoleChromeMixin.rejectReservedChromeDeclaration(name, "theme pack '" + id + "' alias");
			// Rejected here rather than left to the shared belt, whose null message is phrased for the Theme-token
			// caller it was written for and would name the wrong concept on this path.
			if (target == null)
				throw iaex("Invalid theme pack alias target for '%s': null.", name);
			// Recognition runs on the SAME post-belt string the Theme layer would see, so there is no second,
			// alias-specific normalization pass to keep in sync with CssValueGrammar's belt.
			var normalized = CssValueGrammar.normalize(target);
			if (! Theme.VAR_REFERENCE.matcher(normalized).matches())
				throw iaex("Invalid theme pack alias target for '%s': '%s'.  Must be a var(--jc-name) reference.", name, target);
			aliases.put(name, normalized);
			return this;
		}

		/**
		 * Adds (or overrides) every alias in the specified map, in the map's own iteration order.
		 *
		 * <p>
		 * Each entry goes through {@link #alias(String, String)}, so every guard applies per-entry. Pass an
		 * insertion-ordered map if the emitted declaration order matters to you.
		 *
		 * @param values The aliases to add. Must not be <jk>null</jk>.
		 * @return This object.
		 * @throws IllegalArgumentException
		 * 	If {@code values} is <jk>null</jk>, or if any entry fails {@link #alias(String, String)}'s guards.
		 */
		public Builder aliases(Map<String,String> values) {
			if (values == null)
				throw iaex("Invalid theme pack aliases: null.");
			values.forEach(this::alias);
			return this;
		}

		/**
		 * Configures a themeable logo image this pack carries.
		 *
		 * @param value
		 * 	An app-owned, classpath-root-absolute resource path (e.g. {@code "/static/img/oakleaf.svg"}). Same
		 * 	fail-closed validation as {@code ConsoleChromeMixin.Builder.logo(String)}: must exist on the classpath,
		 * 	contain no {@code ..} path segment or {@code %} character, and end in one of {@code .svg}/{@code .png}/
		 * 	{@code .jpg}/{@code .jpeg}/{@code .webp}/{@code .gif}.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code value} is <jk>null</jk>, empty, traversal-shaped, has an
		 * 	unrecognized extension, or does not resolve to an existing classpath resource.
		 */
		public Builder logo(String value) {
			this.logoResource = ConsoleChromeMixin.validateAssetResource(value, "logo");
			return this;
		}

		/**
		 * Configures a themeable page-background image this pack carries.
		 *
		 * @param value
		 * 	An app-owned, classpath-root-absolute resource path (e.g. {@code "/static/img/topo-bg.png"}). Same
		 * 	validation as {@link #logo(String)}.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code value} is <jk>null</jk>, empty, traversal-shaped, has an
		 * 	unrecognized extension, or does not resolve to an existing classpath resource.
		 */
		public Builder pageBackgroundImage(String value) {
			this.pageBackgroundResource = ConsoleChromeMixin.validateAssetResource(value, "pageBackgroundImage");
			return this;
		}

		/**
		 * Builds the immutable {@link ThemePack}.
		 *
		 * @return A new {@link ThemePack}.
		 * @throws IllegalArgumentException
		 * 	If no theme was set, or if any token name is declared by <b>both</b> channels. The latter is checked
		 * 	here rather than at a setter because either channel may be populated second, and it is the invariant
		 * 	the framework already keeps for its own two blocks: leaves own literal values, aliases own derived
		 * 	values, and no name is declared by both.
		 */
		public ThemePack build() {
			if (theme == null)
				throw iaex("Theme pack '%s' has no theme.  A pack must carry a theme.", id);
			for (var name : aliases.keySet())
				if (theme.getTokens().containsKey(name))
					throw iaex("Theme pack '%s' declares '%s' as both a leaf token and an alias.  A name may be declared by only one channel.", id, name);
			return new ThemePack(this);
		}
	}
}
