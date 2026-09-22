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

import freemarker.core.*;
import freemarker.template.*;
import freemarker.template.utility.*;

/**
 * The {@code <@main/>} console FreeMarker directive: the positional placeholder that renders the captured page body
 * inside exactly one {@code <main class="jc-main">}.
 *
 * <p>
 * {@code <@page>} captured the child page body into the {@code pageBody} chrome variable (a
 * {@link TemplateHTMLOutputModel} markup value, so it is not re-escaped). This directive is authored inside
 * {@code <@console>} at the spot the main content belongs; during the console's body pass it writes
 * {@code <main class="jc-main">…</main>} into the console's buffer in author order, next to any
 * {@code <@navigation>} authored alongside it. It is self-closing &mdash; it takes no body of its own (the page body
 * is the one and only main content), and it has no attributes.
 *
 * @since 10.0.0
 */
public final class MainDirectiveModel implements TemplateDirectiveModel {

	/** The shared-variable name this directive registers under. */
	public static final String NAME = "main";

	private static final Set<String> ATTRS = Set.of();

	MainDirectiveModel() {}

	@Override
	@SuppressWarnings({
		"unchecked", // FreeMarker's raw params Map is String-keyed by contract.
		"resource" // FreeMarker owns env.getOut(); closing it would close the HTTP response.
	})
	public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		var p = (Map<String, TemplateModel>) params;
		if (p.containsKey("format"))
			throw FtlAttrLists.reject("<@main> has no format= attribute.");
		FtlAttrLists.rejectUnknown(p, NAME, ATTRS);
		if (body != null)
			throw FtlAttrLists.reject("<@main/> is self-closing; the page body is the main content and takes no nested body.");

		var sb = new StringBuilder("<main class=\"jc-main\">");
		var pageBody = env.getVariable("pageBody");
		if (pageBody instanceof TemplateHTMLOutputModel m)
			// The captured page body is already markup-typed; emit its raw markup so it is not double-escaped.
			sb.append(HTMLOutputFormat.INSTANCE.getMarkupString(m));
		else if (pageBody != null) {
			var u = DeepUnwrap.unwrap(pageBody);
			if (u != null)
				sb.append(u);
		}
		sb.append("</main>");
		var markup = sb.toString();

		// Inside <@console>: stash the main markup and drop a placeholder into the body buffer at this author
		// position so <@console> can split the buffer here (pre-main chrome region vs. below-main content) and
		// substitute the markup back in.  Outside a <@console> (no context in scope) fall back to writing directly.
		var ctx = (ConsoleContext) env.getCustomState(ConsoleContext.KEY);
		var out = env.getOut();
		if (ctx == null) {
			out.write(markup);
		} else {
			ctx.mainMarkup = markup;
			out.write(ConsoleContext.MAIN_PLACEHOLDER);
		}
	}
}
