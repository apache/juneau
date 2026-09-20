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
 * The {@code <@page>} shared FreeMarker directive: capture the nested (markup-typed) body, set the
 * chrome variables ({@code pageBody}, {@code pageTab}, {@code pageInit}, {@code pageCss},
 * {@code pageToolkit}, {@code pageToolkitCss}, {@code pageToolkitJs}), then include the consumer
 * chrome template. A child {@code .ftlh} file <i>is</i> the page call &mdash; it needs no leading
 * include.
 *
 * <p>
 * {@code <@page>} does not emit {@code <html>}, header, nav, or {@code <main>}; the consumer chrome
 * owns those and interpolates {@code ${pageBody}} inside exactly one {@code <main class="jc-main">}.
 *
 * @since 10.0.0
 */
public final class PageDirectiveModel implements TemplateDirectiveModel {

	/** The shared-variable name this directive registers under. */
	public static final String NAME = "page";

	private static final Set<String> ATTRS = Set.of("tab", "init", "css", "toolkit");

	private final String chromeTemplate;
	private final ToolkitPackRegistry packs;

	PageDirectiveModel(String chromeTemplate, ToolkitPackRegistry packs) {
		this.chromeTemplate = chromeTemplate;
		this.packs = packs;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		var p = (Map<String, TemplateModel>) params;
		if (p.containsKey("format"))
			throw FtlAttrLists.reject("<@page> has no format= attribute.");
		FtlAttrLists.rejectUnknown(p, NAME, ATTRS);
		if (body == null)
			throw FtlAttrLists.reject("<@page> requires a nested body.");

		var tab = FtlAttrLists.scalar(p, "tab");
		var init = FtlAttrLists.list(p, NAME, "init");
		var css = FtlAttrLists.list(p, NAME, "css");
		var toolkit = FtlAttrLists.list(p, NAME, "toolkit");

		var sw = new StringWriter();
		body.render(sw);

		var ow = env.getObjectWrapper();
		env.setVariable("pageBody", HTMLOutputFormat.INSTANCE.fromMarkup(sw.toString()));
		env.setVariable("pageTab", ow.wrap(tab));
		env.setVariable("pageInit", ow.wrap(init));
		env.setVariable("pageCss", ow.wrap(css));
		env.setVariable("pageToolkit", ow.wrap(toolkit));
		var req = FreemarkerRenderScope.request();
		var resolved = packs.resolve(toolkit, req);
		env.setVariable("pageToolkitCss", ow.wrap(resolved.cssUrls()));
		env.setVariable("pageToolkitJs", ow.wrap(resolved.jsUrls()));
		// Pre-seed the theme-pack link to the default "open" pack; <@theme name="…"/> in the chrome overrides it
		// (Q17 A: omit <@theme> -> open).  Only when a request is in scope - a null-request render (no renderer
		// wrap) leaves the var unset, which is only reachable by a chrome that never references ${pageThemeCss}.
		if (req != null)
			env.setVariable(ThemeDirectiveModel.PAGE_THEME_CSS_VAR,
				ow.wrap(ConsoleChromeMixin.themeAssetUrl(req, ConsoleChromeMixin.BUILTIN_THEME_NAMES.get(0))));
		env.include(env.getConfiguration().getTemplate(chromeTemplate));
	}
}
