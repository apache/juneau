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
 * The recursive {@code <@node>} shared FreeMarker directive: one entry in a {@code <@navigation>} tree.
 * Attributes are {@code id} (required), {@code label} (required), {@code href} (required on a leaf), and
 * {@code visible} (a model boolean; omit = shown). {@code format=} and any unknown attribute are rejected.
 *
 * <p>
 * A node pushes its {@code id} onto the {@link NavContext} ancestor stack, renders its nested
 * {@code <@node>}s, then pops &mdash; so it knows its full slash-joined path and stamps
 * {@code aria-current="page"} when that path equals, or is an ancestor prefix of, the page's {@code tab=}
 * (read from the {@code pageTab} environment variable {@code <@page>} set). Nodes register themselves on
 * {@link NavContext}; {@link NavigationDirectiveModel} emits depth-0 nodes as
 * {@code .juneau-page-nav-section} and nested nodes as {@code .juneau-page-nav-child} rows along the
 * selected ancestor chain. {@code visible=false} (and any unrecognized value, which fails closed) omits
 * the node and its whole subtree from the HTML.
 *
 * @since 10.0.0
 */
public final class NodeDirectiveModel implements TemplateDirectiveModel {

	/** The shared-variable name this directive registers under. */
	public static final String NAME = "node";

	private static final Set<String> ATTRS = Set.of("id", "label", "href", "visible");

	NodeDirectiveModel() {}

	@Override
	@SuppressWarnings({
		"unchecked" // FreeMarker's raw params Map is String-keyed by contract.
	})
	public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		var p = (Map<String, TemplateModel>) params;
		if (p.containsKey("format"))
			throw FtlAttrLists.reject("<@node> has no format= attribute.");
		FtlAttrLists.rejectUnknown(p, NAME, ATTRS);

		var id = FtlAttrLists.scalar(p, "id");
		if (id.isEmpty())
			throw FtlAttrLists.reject("<@node> requires id=.");
		var label = FtlAttrLists.scalar(p, "label");
		if (label.isEmpty())
			throw FtlAttrLists.reject("<@node> requires label=.");
		var href = FtlAttrLists.scalar(p, "href");

		// visible=false (or any unrecognized value: fail closed) omits the node and its subtree entirely.
		if (! isVisible(p))
			return;

		var ctx = (NavContext) env.getCustomState(NavContext.KEY);
		if (ctx == null)
			throw FtlAttrLists.reject("<@node> must be nested inside <@navigation>.");

		var prefix = String.join("/", ctx.ancestors);
		var path = prefix.isEmpty() ? id : prefix + "/" + id;
		var current = pathMatches(env, path);
		var node = new NavContext.Entry(label, href, current);

		ctx.ancestors.addLast(id);
		ctx.push(node);
		if (body != null)
			body.render(new StringWriter());
		ctx.pop();
		ctx.ancestors.removeLast();

		if (node.children.isEmpty() && href.isEmpty())
			throw FtlAttrLists.reject("<@node id=\"" + id + "\"> leaf requires href=.");
	}

	/**
	 * Resolves {@code visible=} as a model boolean (Q18 A). Omit = shown; a {@link Boolean} or the scalar
	 * {@code "true"} shows the node; {@code false}, {@code null}, or any other value fails closed (hidden).
	 */
	private static boolean isVisible(Map<String, TemplateModel> p) throws TemplateModelException {
		var raw = p.get("visible");
		if (raw == null)
			return true;
		var u = DeepUnwrap.unwrap(raw);
		if (u instanceof Boolean b)
			return b;
		return "true".equals(String.valueOf(u));
	}

	/** True when this node's path equals, or is an ancestor prefix of, the page's {@code tab=} (pageTab). */
	private static boolean pathMatches(Environment env, String path) throws TemplateModelException {
		var raw = env.getVariable("pageTab");
		if (raw == null)
			return false;
		var u = DeepUnwrap.unwrap(raw);
		if (u == null)
			return false;
		var tab = String.valueOf(u).trim();
		if (tab.isEmpty())
			return false;
		return tab.equals(path) || tab.startsWith(path + "/");
	}
}
