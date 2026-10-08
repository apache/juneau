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

import freemarker.core.*;
import freemarker.template.*;

/**
 * The {@code <@navigation>} console FreeMarker directive: opens the nav tree on the {@link PageCapture}.
 *
 * <p>
 * Nested {@code <@node>}s add {@code nav[]} entries of any depth; the shell renders the nav and sets
 * {@code aria-current}. An omitted {@code layout=} leaves {@code navLayout} out of the contract (the shell defaults
 * to horizontal). {@code format=}, any unknown attribute, and nesting inside another {@code <@navigation>} or outside
 * {@code <@console>} are all rejected (fail closed).
 *
 * @since 10.0.0
 */
public final class NavigationDirectiveModel implements TemplateDirectiveModel {

	/** The shared-variable name this directive registers under. */
	public static final String NAME = "navigation";

	static final Set<String> ATTRS = Set.of("layout");

	NavigationDirectiveModel() {}

	@Override
	@SuppressWarnings({
		"unchecked" // FreeMarker's raw params Map is String-keyed by contract.
	})
	public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		Map<String, TemplateModel> p = params;
		if (p.containsKey("format"))
			throw FtlAttrLists.reject("<@navigation> has no format= attribute.");
		FtlAttrLists.rejectUnknown(p, NAME, ATTRS);

		var cap = PageCapture.get(env);
		if (n(cap) || ! cap.consoleOpen)
			throw FtlAttrLists.reject(String.format("<@%s> must be nested inside <@console>.", NAME));
		if (cap.navOpen)
			throw FtlAttrLists.reject("<@navigation> cannot be nested inside another <@navigation>.");

		var layout = FtlAttrLists.scalar(p, "layout");
		if (! layout.isEmpty()) {
			if (! (eqa(layout, "horizontal", "vertical")))
				throw FtlAttrLists.reject(String.format(
					"<@navigation> layout= must be horizontal or vertical; got '%s'.", layout));
			cap.navLayout(layout);
		}

		cap.navOpen = true;
		cap.navCursor.push(cap.navRoot());
		try (var sink = new StringWriter()) {
			if (nn(body))
				body.render(sink);
		} finally {
			cap.navCursor.pop();
			cap.navOpen = false;
		}
	}
}
