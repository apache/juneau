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
package org.apache.juneau.rest.server.view.freemarker.console;

import java.io.*;
import java.util.*;

import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.view.freemarker.*;

import freemarker.core.*;
import freemarker.template.*;

/**
 * The {@code <@theme name="…">} chrome FreeMarker directive: names the active stock theme once, in the chrome,
 * points the chrome at that theme's shipped CSS pack, and &mdash; when a nested {@code <@token>} body is present
 * &mdash; constructs an FTL {@link ThemePack} of custom overrides on top of it.
 *
 * <p>
 * Its only attribute is {@code name}, one of {@link ConsoleChromeMixin#BUILTIN_THEME_NAMES}
 * ({@code open}, {@code light-red}, {@code light-brown}, {@code red}, {@code gray} &mdash; the kebab-case of the
 * {@code Theme} constants). {@code format=}, an unknown attribute, a missing/empty {@code name}, and an unknown
 * theme name are all rejected (fail closed). There is deliberately <b>no</b> {@code default=}: if the tag is
 * present, that <i>is</i> the theme.
 *
 * <h5 class='section'>Custom tokens (the FTL {@link ThemePack} construction path):</h5>
 * <p>
 * The named stock palette is the <b>seed</b>: this directive opens a {@link ThemeBuildContext} holding a
 * {@link org.apache.juneau.rest.server.console.Theme.Builder} copied from the named stock palette (leaf channel) and
 * {@link ThemePack#create(String) ThemePack.create(name)} (alias channel), renders the nested body so each
 * {@code <@token>} folds its override into the matching builder, then assembles the pack. When the body declared at
 * least one token, this directive registers the constructed pack's override block (leaves escaped + aliases
 * verbatim, exactly {@link ConsoleChromeMixin#packRootBlock(ThemePack)}) so {@code <@console>} emits it inline
 * <i>after</i> the stock pack {@code <link>}; a body-less {@code <@theme>} is just the stock pack.
 *
 * <h5 class='section'>How the link lands:</h5>
 * <p>
 * Inside {@code <@console>} (a {@link ConsoleContext} is in scope) the directive writes no markup: it records the
 * resolved stock-pack URL (via {@link ConsoleChromeMixin#themeAssetUrl(org.apache.juneau.rest.server.RestRequest, String)})
 * and the optional override block on the {@link ConsoleContext}, and {@code <@console>} emits both in its head
 * cascade. Absent a {@code <@console>} (the legacy {@code <@page>}-only chrome), it falls back to setting the
 * {@code pageThemeCss} chrome variable to the resolved URL &mdash; the chrome shell then emits
 * {@code <link rel="stylesheet" href="${pageThemeCss}">} itself. {@code <@page>} pre-seeds {@code pageThemeCss} to
 * the {@code open} pack, so omitting {@code <@theme>} yields the default {@code open} theme.
 *
 * @since 10.0.0
 */
public final class ThemeDirectiveModel implements TemplateDirectiveModel {

	/** The shared-variable name this directive registers under. */
	public static final String NAME = "theme";

	/** The chrome variable this directive sets and {@code <@page>} pre-seeds - the resolved theme-pack URL. */
	public static final String PAGE_THEME_CSS_VAR = "pageThemeCss";

	private static final Set<String> ATTRS = Set.of("name");

	ThemeDirectiveModel() {}

	@Override
	@SuppressWarnings({
		"unchecked" // FreeMarker's raw params Map is String-keyed by contract.
	})
	public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		var p = (Map<String, TemplateModel>) params;
		if (p.containsKey("format"))
			throw FtlAttrLists.reject("<@theme> has no format= attribute.");
		FtlAttrLists.rejectUnknown(p, NAME, ATTRS);

		var name = FtlAttrLists.scalar(p, "name");
		if (name.isEmpty())
			throw FtlAttrLists.reject("<@theme> requires name=.");
		if (! ConsoleChromeMixin.BUILTIN_THEME_NAMES.contains(name))
			throw FtlAttrLists.reject("<@theme> unknown theme name '" + name + "'.  Built-in themes: "
				+ String.join(", ", ConsoleChromeMixin.BUILTIN_THEME_NAMES) + ".");

		var req = FreemarkerRenderScope.request();
		if (req == null)
			throw FtlAttrLists.reject("<@theme> needs FreemarkerRenderScope.request() (renderer wrap).");

		// Capture any nested <@token> overrides against the stock-palette seed.  The context is cleared afterward so a
		// stray <@token> outside a <@theme> (e.g. in a sibling directive's body) fails its own "must be nested" guard.
		var seed = ConsoleChromeMixin.stockTheme(name);
		var themeBuilder = Theme.create(name);
		for (var e : seed.getTokens().entrySet())
			themeBuilder.token(e.getKey(), e.getValue());
		var ctx = new ThemeBuildContext(name, themeBuilder, ThemePack.create(name));
		env.setCustomState(ThemeBuildContext.KEY, ctx);
		try {
			if (body != null)
				body.render(new StringWriter());
		} finally {
			env.setCustomState(ThemeBuildContext.KEY, null);
		}

		var url = ConsoleChromeMixin.themeAssetUrl(req, name);
		String overrideBlock = null;
		if (ctx.anyDeclared) {
			var pack = ctx.packBuilder.theme(ctx.themeBuilder.build()).build();
			overrideBlock = ConsoleChromeMixin.packRootBlock(pack);
		}

		var console = (ConsoleContext) env.getCustomState(ConsoleContext.KEY);
		if (console != null) {
			console.themePackUrl = url;
			console.themeOverrideBlock = overrideBlock;
		} else {
			// Legacy <@page>-only chrome: no place for an FTL override block, so only the stock-pack URL flows through.
			env.setVariable(PAGE_THEME_CSS_VAR, env.getObjectWrapper().wrap(url));
		}
	}
}
