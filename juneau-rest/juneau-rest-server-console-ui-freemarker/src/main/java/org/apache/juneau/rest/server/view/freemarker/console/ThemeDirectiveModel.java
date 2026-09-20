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
 * The {@code <@theme name="…"/>} chrome FreeMarker directive: names the active stock theme once, in the chrome,
 * and points the chrome at that theme's shipped CSS pack.
 *
 * <p>
 * Its only attribute is {@code name}, one of {@link ConsoleChromeMixin#BUILTIN_THEME_NAMES}
 * ({@code open}, {@code light-red}, {@code light-brown}, {@code red}, {@code gray} &mdash; the kebab-case of the
 * {@code Theme} constants). {@code format=}, an unknown attribute, a missing/empty {@code name}, and an unknown
 * theme name are all rejected (fail closed). There is deliberately <b>no</b> {@code default=}: if the tag is
 * present, that <i>is</i> the theme.
 *
 * <h5 class='section'>How the link lands:</h5>
 * <p>
 * The directive does not itself write markup; it sets the {@code pageThemeCss} chrome variable to the resolved,
 * cache-busted URL of the named pack (via {@link ConsoleChromeMixin#themeAssetUrl(org.apache.juneau.rest.server.RestRequest, String)}),
 * and the chrome shell emits {@code <link rel="stylesheet" href="${pageThemeCss}">} at the position it chooses
 * &mdash; after {@code chrome.css} and before any page-local {@code css=}, so the theme pack wins the cascade.
 * {@code <@page>} pre-seeds {@code pageThemeCss} to the {@code open} pack, so omitting {@code <@theme>} yields the
 * default {@code open} theme; because this directive must run <i>before</i> the shell expands
 * {@code ${pageThemeCss}}, author it in the chrome ahead of that {@code <link>}.
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
	@SuppressWarnings("unchecked")
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
		env.setVariable(PAGE_THEME_CSS_VAR, env.getObjectWrapper().wrap(ConsoleChromeMixin.themeAssetUrl(req, name)));
	}
}
