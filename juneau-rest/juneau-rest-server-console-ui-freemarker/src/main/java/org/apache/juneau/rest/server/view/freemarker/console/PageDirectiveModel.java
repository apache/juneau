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

import org.apache.juneau.rest.server.view.freemarker.*;

import freemarker.core.*;
import freemarker.template.*;

/**
 * The {@code <@page>} shared FreeMarker directive: capture the nested body into {@link PageCapture}, resolve the
 * {@code toolkit=} packs, then include the consumer chrome template. A child {@code .ftlh} file <i>is</i> the page
 * call &mdash; it needs no leading include.
 *
 * <p>
 * {@code <@page>} is the outermost console directive; it must not be nested or repeated. The chrome template it
 * includes is expected to render exactly one {@code <@console>}, which in turn renders the page contract.
 *
 * @since 10.0.0
 */
public final class PageDirectiveModel implements TemplateDirectiveModel {

	/** The shared-variable name this directive registers under. */
	public static final String NAME = "page";

	static final Set<String> ATTRS = Set.of("tab", "init", "css", "toolkit");

	private final String chromeTemplate;
	private final ToolkitPackRegistry packs;

	PageDirectiveModel(String chromeTemplate, ToolkitPackRegistry packs) {
		this.chromeTemplate = chromeTemplate;
		this.packs = packs;
	}

	@Override
	@SuppressWarnings({
		"unchecked" // FreeMarker passes the directive params as a raw Map; it is assigned to Map<String,TemplateModel>, the type FreeMarker documents for them
	})
	public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		Map<String, TemplateModel> p = params;
		if (p.containsKey("format"))
			throw FtlAttrLists.reject("<@page> has no format= attribute.");
		FtlAttrLists.rejectUnknown(p, NAME, ATTRS);
		if (body == null)
			throw FtlAttrLists.reject("<@page> requires a nested body.");

		var cap = PageCapture.of(env);
		if (cap.pageOpen || cap.consoleOpen || cap.consoleDone)
			throw FtlAttrLists.reject("<@page> must be the outermost console directive; it cannot be nested or repeated.");

		var toolkit = FtlAttrLists.list(p, NAME, "toolkit");
		packs.resolve(toolkit, FreemarkerRenderScope.request());  // Fail fast on an unknown pack, before the body renders.
		cap.tab(FtlAttrLists.scalar(p, "tab")).toolkit(toolkit);
		cap.init(FtlAttrLists.list(p, NAME, "init"));
		cap.css(FtlAttrLists.list(p, NAME, "css"));

		cap.pageOpen = true;
		try {
			body.render(cap.pageBuffer());
		} finally {
			cap.pageOpen = false;
		}
		cap.flushSegment();

		// Resolved after the body so the card set is known: the DataTables glue rides only with a server-mode table card.
		var resolved = packs.resolve(toolkit, FreemarkerRenderScope.request(), cap.hasServerModeTable());
		cap.toolkitAssets(resolved.cssUrls(), resolved.jsUrls());

		env.include(env.getConfiguration().getTemplate(chromeTemplate));

		if (! cap.consoleDone)
			throw FtlAttrLists.reject(String.format(
				"<@page> chrome template '%s' rendered no <@console>; the legacy chrome was removed in 10.0.0.", chromeTemplate));
	}
}
