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
 * The recursive {@code <@node>} console FreeMarker directive: one entry in a {@code <@navigation>} tree, of any
 * depth.
 *
 * <p>
 * Attributes: {@code id} (required, checked against the id grammar &mdash; E-4), {@code label} (required),
 * {@code href} (required on a leaf &mdash; E-2), {@code visible} (a strict boolean, omit defaults to {@code true}
 * &mdash; E-5), and {@code selected} (a strict boolean, omit defaults to {@code false} &mdash; E-5). {@code format=}
 * and any unknown attribute are rejected. A duplicate sibling id is rejected (E-3). At most one node tree-wide may set
 * {@code selected=true} (E-6).
 *
 * <p>
 * {@code visible=false} omits the node and its whole subtree &mdash; the body is not even rendered. A node not
 * nested inside {@code <@navigation>} is rejected fail-closed.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	&lt;@navigation&gt;
 * 	  &lt;@node id="work" label="Work" href="/work"&gt;
 * 	    &lt;@node id="work-queue" label="Queue" href="/work/queue"/&gt;
 * 	  &lt;/@node&gt;
 * 	  &lt;@node id="setup" label="Setup" href="/setup" visible=isAdmin/&gt;
 * 	&lt;/@navigation&gt;
 * </p>
 *
 * @since 10.0.0
 */
public final class NodeDirectiveModel implements TemplateDirectiveModel {

	/** The shared-variable name this directive registers under. */
	public static final String NAME = "node";

	static final Set<String> ATTRS = Set.of("id", "label", "href", "visible", "selected");

	NodeDirectiveModel() {}

	@Override
	@SuppressWarnings({
		"unchecked" // FreeMarker's raw params Map is String-keyed by contract.
	})
	public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		Map<String, TemplateModel> p = params;
		if (p.containsKey("format"))
			throw FtlAttrLists.reject("<@node> has no format= attribute.");
		FtlAttrLists.rejectUnknown(p, NAME, ATTRS);

		var id = FtlAttrLists.scalar(p, "id");
		if (id.isEmpty())
			throw FtlAttrLists.reject("<@node> requires id=.");
		FtlAttrLists.checkId(NAME, id);
		var label = FtlAttrLists.scalar(p, "label");
		if (label.isEmpty())
			throw FtlAttrLists.reject("<@node> requires label=.");
		var href = FtlAttrLists.scalar(p, "href");
		var visible = FtlAttrLists.strictBoolean(p, NAME, "visible", true);
		var selected = FtlAttrLists.strictBoolean(p, NAME, "selected", false);

		var cap = PageCapture.get(env);
		if (cap == null || ! cap.navOpen)
			throw FtlAttrLists.reject("<@node> must be nested inside <@navigation>.");

		// visible=false omits the node and its whole subtree; the body is not rendered.
		if (! visible)
			return;

		var node = cap.navCursor.peek().add(id, label, href.isEmpty() ? null : href);
		cap.navCursor.push(node);
		try (var sink = new StringWriter()) {
			if (body != null)
				body.render(sink);
		} finally {
			cap.navCursor.pop();
		}

		if (node.children().isEmpty() && node.href() == null)
			throw FtlAttrLists.reject(String.format("<@node id='%s'> leaf requires href=.", id));
		if (selected)
			cap.select(node.idPath());
	}
}
