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

import java.util.*;

import freemarker.template.*;

/**
 * One node in the console navigation tree. Nodes nest to any depth; {@code <@node>} directives append
 * to the node returned by {@link PageCapture#navRoot()}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	NavNode <jv>root</jv> = PageCapture.<jsm>of</jsm>(<jv>env</jv>).navRoot();
 * 	NavNode <jv>home</jv> = <jv>root</jv>.add(<js>"home"</js>, <js>"Home"</js>, <js>"/home"</js>);
 * 	<jv>home</jv>.add(<js>"about"</js>, <js>"About"</js>, <js>"/home/about"</js>);
 * 	<jv>root</jv>.find(List.<jsm>of</jsm>(<js>"home"</js>, <js>"about"</js>)).map(NavNode::label);  <jc>// Optional[About]</jc>
 * </p>
 *
 * @since 10.0.0
 */
public final class NavNode {

	private final NavNode parent;
	private final String id;
	private final String label;
	private final String href;
	private final List<NavNode> children = new ArrayList<>();

	private NavNode(NavNode parent, String id, String label, String href) {
		this.parent = parent;
		this.id = id;
		this.label = label;
		this.href = href;
	}

	static NavNode root() {
		return new NavNode(null, null, null, null);
	}

	/**
	 * Appends a child node.
	 *
	 * @param nodeId The node id, unique among its siblings.
	 * @param label The visible label.
	 * @param hrefOrNull The link target, or <jk>null</jk> for a parent-only node.
	 * @return The new child.
	 * @throws TemplateModelException If a sibling already has this id (E-3).
	 */
	public NavNode add(String nodeId, String label, String hrefOrNull) throws TemplateModelException {
		for (var c : children)
			if (c.id.equals(nodeId))
				throw FtlAttrLists.reject(String.format("<@node id='%s'> duplicates a sibling id under '%s'.", nodeId,
					this.id == null ? "(root)" : String.join("/", idPath())));
		var n = new NavNode(this, nodeId, label, hrefOrNull);
		children.add(n);
		return n;
	}

	/**
	 * Walks a root-first id path from this node.
	 *
	 * @param idPath The ids, root first.
	 * @return The node at the end of the path, or empty.
	 */
	public Optional<NavNode> find(List<String> idPath) {
		var n = this;
		for (var step : idPath) {
			NavNode next = null;
			for (var c : n.children)
				if (c.id.equals(step))
					next = c;
			if (next == null)
				return Optional.empty();
			n = next;
		}
		return Optional.of(n);
	}

	/** @return The id, or <jk>null</jk> for the synthetic root. */
	public String id() { return id; }

	/** @return The label. */
	public String label() { return label; }

	/** @return The href, or <jk>null</jk>. */
	public String href() { return href; }

	/** @return The children, unmodifiable. */
	public List<NavNode> children() { return Collections.unmodifiableList(children); }

	List<String> idPath() {
		var out = new LinkedList<String>();
		for (var n = this; n != null && n.id != null; n = n.parent)
			out.addFirst(n.id);
		return out;
	}

	List<String> paths() {
		var out = new ArrayList<String>();
		for (var c : children) {
			out.add(String.join("/", c.idPath()));
			out.addAll(c.paths());
		}
		return out;
	}

	List<Map<String,Object>> toList() {
		var out = new ArrayList<Map<String,Object>>();
		for (var c : children) {
			var m = new LinkedHashMap<String,Object>();
			m.put("id", c.id);
			m.put("label", c.label);
			if (c.href != null)
				m.put("href", c.href);
			if (! c.children.isEmpty())
				m.put("children", c.toList());
			out.add(m);
		}
		return out;
	}
}
