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
 * The recursive {@code <@node>} console FreeMarker directive: one entry in a {@code <@navigation>} tree, of any
 * depth, or (with {@code under=}) a page-scoped addition appended under an existing chrome nav node.
 *
 * <p>
 * Attributes: {@code id} (required, checked against the id grammar &mdash; E-4), {@code label} (required),
 * {@code href} (required on a leaf &mdash; E-2), {@code visible} (a strict boolean, omit defaults to {@code true}
 * &mdash; E-5), {@code selected} (a strict boolean, omit defaults to {@code false} &mdash; E-5), and {@code under}
 * (a chrome nav id path; see below). {@code format=} and any unknown attribute are rejected. A duplicate sibling id is rejected (E-3). At most one node tree-wide may set
 * {@code selected=true} (E-6).
 *
 * <p>
 * {@code visible=false} omits the node and its whole subtree &mdash; the body is not even rendered. A node not
 * nested inside {@code <@navigation>} and without {@code under=} is rejected fail-closed.
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
 * <h5 class='section'>{@code under=}: page-scoped nav additions:</h5>
 * <p>
 * {@code under=} is allowed only on a {@code <@node>} that is a direct child of {@code <@page>}, or of
 * {@code <@console>} outside {@code <@navigation>}; anywhere else it is E-B14.  The node (and any ordinary nested
 * {@code <@node>} children, built exactly as inside {@code <@navigation>}) is recorded as a pending addition and
 * applied at {@code </@console>}, before {@code activeNav} resolves.  An unknown {@code under=} path is E-B8; a
 * duplicate sibling under the resolved parent is E-3.  {@link PageSpec#navUnder(String, PageCapture.NavAdder)} is
 * the Java twin; both funnel into the same resolution pass.  Pending additions resolve in order &mdash; Java
 * {@code navUnder} calls first, then {@code <@node under>} in document order &mdash; so an addition can target only
 * a node created by an earlier one; a forward reference is E-B8.
 * <p class='bcode'>
 * 	&lt;@page tab="fleet/instances/${instance.id}/logs"&gt;
 * 	  &lt;@node under="fleet/instances" id=instance.id label=instance.name href="/fleet/instances/${instance.id}"&gt;
 * 	    &lt;@node id="logs" label="Logs" href="/fleet/instances/${instance.id}/logs" selected=true/&gt;
 * 	  &lt;/@node&gt;
 * 	&lt;/@page&gt;
 * </p>
 *
 * @since 10.0.0
 */
public final class NodeDirectiveModel implements TemplateDirectiveModel {

	/** The shared-variable name this directive registers under. */
	public static final String NAME = "node";

	static final Set<String> ATTRS = Set.of("id", "label", "href", "visible", "selected", "under");

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
		var under = FtlAttrLists.scalar(p, "under");

		if (p.containsKey("under"))
			for (var segment : under.split("/", -1))
				if (segment.isBlank())
					throw FtlAttrLists.reject(f(
						"<@node id='%s'> under must name a '/'-separated nav id path; got '%s'.", id, under));

		var cap = PageCapture.get(env);
		if (! under.isEmpty()) {
			if (n(cap) || cap.navOpen || cap.cardOpen || cap.slotOpen || ! (cap.pageOpen || cap.consoleOpen))
				throw FtlAttrLists.reject(f(
					"<@node id='%s' under='%s'> must be a direct child of <@page> or <@console>, outside <@navigation>.", id, under));
		} else if (n(cap) || ! cap.navOpen) {
			throw FtlAttrLists.reject("<@node> must be nested inside <@navigation>.");
		}

		// visible=false omits the node and its whole subtree; the body is not rendered.
		if (! visible)
			return;

		if (! under.isEmpty()) {
			buildUnder(cap, under, id, label, href, selected, body);
			return;
		}

		var node = cap.navCursor.peek().add(id, label, href.isEmpty() ? null : href);
		cap.navCursor.push(node);
		try (var sink = new StringWriter()) {
			if (nn(body))
				body.render(sink);
		} finally {
			cap.navCursor.pop();
		}

		if (node.children().isEmpty() && n(node.href()))
			throw FtlAttrLists.reject(f("<@node id='%s'> leaf requires href=.", id));
		if (selected) {
			if (cap.underRootOpen) {
				if (nn(cap.underRootSelected))
					throw FtlAttrLists.reject(f("<@node id='%s'> selected=true but '%s' is already selected.",
						node.id(), underFullPath(cap, cap.underRootSelected)));
				cap.underRootSelected = node;
			} else {
				cap.select(node.idPath());
			}
		}
	}

	// under=: builds the node (and its ordinary nested children) against a throwaway detached root, then queues
	// a NavAdder that copies the finished subtree onto the real, resolved ancestor at </@console>.
	private static void buildUnder(PageCapture cap, String under, String id, String label, String href,
			boolean selected, TemplateDirectiveBody body) throws TemplateException, IOException {
		var top = NavNode.root().add(id, label, href.isEmpty() ? null : href);
		cap.navCursor.push(top);
		cap.navOpen = true;
		cap.underRootOpen = true;
		cap.underRootParent = under;
		try (var sink = new StringWriter()) {
			if (nn(body))
				body.render(sink);
		} finally {
			cap.navCursor.pop();
			cap.navOpen = false;
			cap.underRootOpen = false;
		}
		if (top.children().isEmpty() && n(top.href()))
			throw FtlAttrLists.reject(f("<@node id='%s'> leaf requires href=.", id));

		var selectedMarker = cap.underRootSelected;
		cap.underRootSelected = null;
		if (selected) {
			if (nn(selectedMarker))
				throw FtlAttrLists.reject(f("<@node id='%s'> selected=true but '%s' is already selected.",
					id, underFullPath(cap, selectedMarker)));
			selectedMarker = top;
		}

		var marker = selectedMarker;
		cap.addPendingNavUnder(under, parent -> {
			var found = new NavNode[1];
			copySubtree(top, parent, marker, found);
			if (nn(found[0]))
				cap.select(found[0].idPath());
		});
	}

	// The selected node's full path, as it will be once attached: the under= path followed by its detached path.
	private static String underFullPath(PageCapture cap, NavNode detached) {
		return cap.underRootParent + "/" + String.join("/", detached.idPath());
	}

	// Recursively copies a detached subtree onto a real, resolved ancestor; NavNode has no re-parenting operation,
	// so every node is rebuilt via its public getters.
	private static NavNode copySubtree(NavNode src, NavNode dstParent, NavNode selectedMarker, NavNode[] found)
			throws TemplateModelException {
		var copy = dstParent.add(src.id(), src.label(), src.href());
		if (src == selectedMarker)
			found[0] = copy;
		for (var child : src.children())
			copySubtree(child, copy, selectedMarker, found);
		return copy;
	}
}
