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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.view.freemarker.*;

import freemarker.core.*;
import freemarker.template.*;

/**
 * The {@code <@theme name="…">} chrome FreeMarker directive: names the active stock theme once, in the chrome,
 * points the chrome at that theme's shipped stylesheet, and &mdash; when a nested {@code <@token>} body is present
 * &mdash; builds a {@link Theme} of custom overrides on top of it.
 *
 * <p>
 * Its only attribute is {@code name}, one of {@link ConsoleChromeMixin#BUILTIN_THEME_NAMES}
 * ({@code open}, {@code light-red}, {@code light-brown}, {@code red}, {@code gray} &mdash; the kebab-case of the
 * {@code Theme} constants). {@code format=}, an unknown attribute, a missing/empty {@code name}, and an unknown
 * theme name are all rejected (fail closed). There is deliberately <b>no</b> {@code default=}: if the tag is
 * present, that <i>is</i> the theme.
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
public final class ThemeDirectiveModel implements TemplateDirectiveModel {

	/** The shared-variable name this directive registers under. */
	public static final String NAME = "theme";

	static final Set<String> ATTRS = Set.of("name");

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

		var name = FtlAttrLists.scalar(p, "name");
		if (name.isEmpty())
			throw FtlAttrLists.reject("<@theme> requires name=.");
		if (! ConsoleChromeMixin.BUILTIN_THEME_NAMES.contains(name))
			throw FtlAttrLists.reject("<@theme> unknown theme name '" + name + "'.  Built-in themes: "
				+ String.join(", ", ConsoleChromeMixin.BUILTIN_THEME_NAMES) + ".");

		var cap = PageCapture.get(env);
		if (n(cap) || ! cap.consoleOpen)
			throw FtlAttrLists.reject(String.format(
				"<@theme name='%s'> must be nested inside <@console>; the legacy pageThemeCss path was removed in 10.0.0.", name));

		var req = FreemarkerRenderScope.request();
		if (n(req))
			throw FtlAttrLists.reject("<@theme> needs FreemarkerRenderScope.request() (renderer wrap).");

		// Capture any nested <@token> overrides against the stock-palette seed.  The context is cleared afterward so a
		// stray <@token> outside a <@theme> (e.g. in a sibling directive's body) fails its own "must be nested" guard.
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
				overrideBlock = ConsoleChromeMixin.overrideBlock(ctx.themeBuilder.build());
			} catch (IllegalArgumentException e) {
				// Theme.Builder.build()'s frozen sentence (unknown var() target, cycle, a name on both channels) is the
				// diagnostic; re-surface it undecorated (see FtlAttrLists.reject).
				throw FtlAttrLists.reject(e.getMessage());
			}
		}

		cap.themeCssUrl = ConsoleChromeMixin.themeAssetUrl(req, name);
		cap.themeOverrideBlock = overrideBlock;
		cap.theme(name);
	}
}
