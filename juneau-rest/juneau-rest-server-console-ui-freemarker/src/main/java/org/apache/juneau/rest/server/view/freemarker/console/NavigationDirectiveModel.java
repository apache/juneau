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

/**
 * The {@code <@navigation>} shared FreeMarker directive: the wrapper for a recursive {@code <@node>}
 * tree authored <b>once in chrome</b>. Opens a per-render {@link NavContext} (fresh ancestor stack),
 * renders its body, and emits exactly one {@code .juneau-page-nav} landmark whose top-level nodes are
 * a {@code .juneau-page-nav-sections} row.
 *
 * <p>
 * The only attribute is {@code layout} ({@code horizontal} &mdash; the omit default &mdash; or
 * {@code vertical}); {@code format=} and any unknown attribute are rejected. The chrome places this
 * landmark <b>outside</b> {@code <main>} and never inside a {@code .jc-card}.
 *
 * @since 10.0.0
 */
public final class NavigationDirectiveModel implements TemplateDirectiveModel {

	/** The shared-variable name this directive registers under. */
	public static final String NAME = "navigation";

	private static final Set<String> ATTRS = Set.of("layout");

	NavigationDirectiveModel() {}

	@Override
	@SuppressWarnings("unchecked")
	public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		var p = (Map<String, TemplateModel>) params;
		if (p.containsKey("format"))
			throw FtlAttrLists.reject("<@navigation> has no format= attribute.");
		FtlAttrLists.rejectUnknown(p, NAME, ATTRS);

		var layout = FtlAttrLists.scalar(p, "layout");
		if (layout.isEmpty())
			layout = "horizontal";
		if (! ("horizontal".equals(layout) || "vertical".equals(layout)))
			throw FtlAttrLists.reject("<@navigation> layout= must be horizontal|vertical; got '" + layout + "'.");

		env.setCustomState(NavContext.KEY, new NavContext());

		var sw = new StringWriter();
		if (body != null)
			body.render(sw);

		var out = env.getOut();
		out.write("<nav class=\"juneau-page-nav\"");
		if ("vertical".equals(layout))
			out.write(" data-juneau-nav-layout=\"vertical\"");
		out.write("><div class=\"juneau-page-nav-sections\">");
		out.write(sw.toString());
		out.write("</div></nav>");
	}
}
