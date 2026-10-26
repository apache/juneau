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
package org.apache.juneau.console.freemarker;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.console.*;
import org.apache.juneau.rest.server.view.freemarker.*;

import freemarker.core.*;
import freemarker.template.*;

/**
 * The {@code <@theme name="…">} chrome FreeMarker directive: names the active stock theme once, in the chrome,
 * points the chrome at that theme's shipped stylesheet, and &mdash; when a nested {@code <@token>} body is present
 * &mdash; builds a {@link Theme} of custom overrides on top of it.
 *
 * <p>
 * Its only attribute is {@code name} (optional), one of {@link ConsoleChromeMixin#BUILTIN_THEME_NAMES}
 * ({@code open}, {@code light-red}, {@code light-brown}, {@code red}, {@code gray} &mdash; the kebab-case of the
 * {@code Theme} constants). {@code format=}, an unknown attribute, an empty {@code name=""}, and an unknown
 * theme name are all rejected (fail closed). There is deliberately <b>no</b> {@code default=}: if the tag is
 * present, that <i>is</i> the theme.
 *
 * <h5 class='section'>Nameless form:</h5>
 * <p>
 * Without {@code name=}, the directive inherits the page theme as its seed: {@code <@console theme="…">} if given,
 * else the {@link PageSpec#theme(String)} value, else {@code open}. It never conflicts with a page-spec theme, so
 * {@code <@theme><@token …/></@theme>} customises whatever theme the page already selected.
 *
 * <h5 class='section'>Custom tokens:</h5>
 * <p>
 * The named stock palette is the <b>seed</b>: this directive opens a {@link ThemeBuildContext} holding a
 * {@code Theme.Builder} copied from the named stock palette, renders the nested body so each {@code <@token>} folds
 * its leaf or alias into that builder, then builds the theme. When the body declared at least one token, the
 * theme's override block ({@link ConsoleChromeMixin#overrideBlock(Theme)}: leaves escaped, aliases verbatim) is
 * emitted inline <i>after</i> the stock-theme {@code <link>}. A body-less {@code <@theme>} is just the stock theme.
 * A {@code build()} failure (unknown {@code var()} target, cycle, a name on both channels) is a template error
 * carrying {@code Theme.Builder}'s sentence.
 *
 * <p>
 * A {@code <@token scheme="dark">} adds to a separate dark rule, emitted after the light block in the same inline
 * {@code <style>}; see {@link TokenDirectiveModel}. A body with only dark tokens emits no light block. A dark alias
 * whose target is not a light-theme token or alias, a {@link Theme#OPEN} token, a chrome role alias or another dark
 * declaration is rejected.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bftl'>
 * 	&lt;#-- Stock palette only: links juneau-theme-light-brown.css, no inline block. --&gt;
 * 	&lt;@theme name="light-brown"/&gt;
 *
 * 	&lt;#-- Stock palette plus one leaf override: link, then &lt;style&gt;html:root{...}&lt;/style&gt;. --&gt;
 * 	&lt;@theme name="light-brown"&gt;&lt;@token name="--jc-accent-selected" value="#8a5a1a"/&gt;&lt;/@theme&gt;
 * </p>
 *
 * <h5 class='section'>How the link lands:</h5>
 * <p>
 * {@code <@theme>} must be nested inside {@code <@console>}; it records the resolved stock-theme URL (via
 * {@link ConsoleChromeMixin#themeAssetUrl(org.apache.juneau.rest.server.RestRequest, String)}) and the optional
 * override block on the {@link PageCapture}, and {@code <@console>} emits both in its head cascade. A {@code <@theme>}
 * authored outside {@code <@console>} is rejected fail-closed (E-18).
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S3776" // Validation and rendering chains many independent guards; splitting them adds no clarity.
})
public final class ThemeDirectiveModel implements TemplateDirectiveModel {

	/** The shared-variable name this directive registers under. */
	public static final String NAME = "theme";

	static final Set<String> ATTRS = Set.of("name");

	/** A CSS comment, stripped before matching the alias shape (the same belt the alias validation applies). */
	private static final Pattern COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

	/** The {@code var(--jc-name)} alias shape, capturing the target name. */
	private static final Pattern DARK_ALIAS_TARGET = Pattern.compile("^[Vv][Aa][Rr]\\(\\s*(--jc-[a-z0-9-]+)\\s*\\)$");

	ThemeDirectiveModel() {}

	@Override
	@SuppressWarnings({
		"unchecked" // FreeMarker's raw params Map is String-keyed by contract.
	})
	public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		Map<String, TemplateModel> p = params;
		if (p.containsKey("format"))
			throw FtlAttrLists.reject("<@theme> has no format= attribute.");
		FtlAttrLists.rejectUnknown(p, NAME, ATTRS);

		// A nameless <@theme> inherits the page theme; an explicit name="" is a mistake, not a request to inherit.
		var named = p.containsKey("name");
		var name = FtlAttrLists.scalar(p, "name");
		if (named && name.isEmpty())
			throw FtlAttrLists.reject("<@theme> name= must not be empty; omit name= to inherit the page theme.");
		if (named && ! ConsoleChromeMixin.BUILTIN_THEME_NAMES.contains(name))
			throw FtlAttrLists.reject("<@theme> unknown theme name '" + name + "'.  Built-in themes: "
				+ String.join(", ", ConsoleChromeMixin.BUILTIN_THEME_NAMES) + ".");

		var cap = PageCapture.get(env);
		if (n(cap) || ! cap.consoleOpen)
			throw FtlAttrLists.reject(f(
				"<@theme%s> must be nested inside <@console>; the legacy pageThemeCss path was removed in 10.0.0.", named ? " name='" + name + "'" : ""));

		var req = FreemarkerRenderScope.request();
		if (n(req))
			throw FtlAttrLists.reject("<@theme> needs FreemarkerRenderScope.request() (renderer wrap).");

		// Capture any nested <@token> overrides against the stock-palette seed.  The context is cleared afterward so a
		// stray <@token> outside a <@theme> (e.g. in a sibling directive's body) fails its own "must be nested" guard.
		// A nameless directive seeds from the page theme: console theme=, else the PageSpec theme, else the default.
		if (! named) {
			var pageTheme = nn(cap.themeOrNull()) ? cap.themeOrNull() : ConsoleChromeMixin.BUILTIN_THEME_NAMES.get(0);
			name = nn(cap.consoleTheme) ? cap.consoleTheme : pageTheme;
		}
		var seed = ConsoleChromeMixin.stockTheme(name);
		var themeBuilder = Theme.create(name);
		for (var e : seed.getTokens().entrySet())
			themeBuilder.token(e.getKey(), e.getValue());
		var ctx = new ThemeBuildContext(name, themeBuilder);
		cap.themeBuild = ctx;
		try (var sink = new StringWriter()) {
			if (nn(body))
				body.render(sink);
		} finally {
			cap.themeBuild = null;
		}

		String overrideBlock = null;
		if (ctx.anyDeclared) {
			try {
				var light = ctx.themeBuilder.build();
				checkDarkAliasTargets(ctx, light);
				// The light block is emitted only when a light token was declared; a dark-only body carries just its
				// dark rule, so the stock stylesheet keeps supplying every light value.
				var block = (ctx.lightDeclared ? ConsoleChromeMixin.overrideBlock(light) : "")
					+ ConsoleChromeMixin.schemeOverrideBlock(ctx.darkTokens, ctx.darkAliases);
				overrideBlock = block.isEmpty() ? null : block;
			} catch (IllegalArgumentException e) {
				// Theme.Builder.build()'s frozen sentence (unknown var() target, cycle, a name on both channels) is the
				// diagnostic; re-surface it undecorated (see FtlAttrLists.reject).
				throw FtlAttrLists.reject(e.getMessage());
			}
		}

		// Merge-checked before any field write, so a conflict leaves nothing half-applied.
		// A nameless directive adopts the page theme rather than naming one, so there is nothing to conflict with.
		if (named)
			cap.mergeTheme(name);
		else
			cap.theme(name);
		cap.themeCssUrl = ConsoleChromeMixin.themeAssetUrl(req, name);
		cap.themeOverrideBlock = overrideBlock;
	}

	/**
	 * Rejects a dark alias whose {@code var()} target is not one of: a token or alias of the built light theme,
	 * a {@link Theme#OPEN} token, a role alias the chrome declares ({@link ConsoleChromeMixin#roleAliasNames()}),
	 * or another dark declaration.
	 */
	private static void checkDarkAliasTargets(ThemeBuildContext ctx, Theme light) {
		for (var e : ctx.darkAliases.entrySet()) {
			var m = DARK_ALIAS_TARGET.matcher(COMMENT.matcher(e.getValue()).replaceAll("").trim());
			if (! m.matches())
				continue;  // HTT: the alias shape was already validated when the <@token> ran.
			var target = m.group(1);
			var known = light.getTokens().containsKey(target) || light.getAliases().containsKey(target)
				|| Theme.OPEN.getTokens().containsKey(target) || ConsoleChromeMixin.roleAliasNames().contains(target)
				|| ctx.darkTokens.containsKey(target) || ctx.darkAliases.containsKey(target);
			if (! known)
				throw new IllegalArgumentException(f("Dark alias '%s' references unknown token '%s'.", e.getKey(), target));
		}
	}
}
