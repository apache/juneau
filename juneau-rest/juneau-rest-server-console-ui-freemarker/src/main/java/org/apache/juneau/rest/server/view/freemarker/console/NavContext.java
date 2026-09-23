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

/**
 * Per-render navigation context shared between {@code <@navigation>} and the recursive {@code <@node>}
 * directives via {@link freemarker.core.Environment#setCustomState(Object, Object)}.
 *
 * <p>
 * {@code <@navigation>} installs a fresh instance before rendering its body; each {@code <@node>} pushes
 * its own {@code id} onto {@link #ancestors} on enter and pops it on exit, so a node can compute its
 * full slash-joined path (for {@code aria-current} matching against the page's {@code tab=}) without
 * threading state through directive parameters. Nested nodes are collected into {@link #roots} rather
 * than written inline: {@link #write(Writer, String)} then emits the Page Tabs row and one Page Subtabs
 * row per selected ancestor that has children, at arbitrary depth.
 *
 * @since 10.0.0
 */
final class NavContext {

	/** Identity key for {@code Environment} custom-state storage. */
	static final Object KEY = new Object();

	/** The live ancestor-id stack (head..tail = outermost..innermost enclosing node). */
	final Deque<String> ancestors = new ArrayDeque<>();

	/** Top-level {@code <@node>}s, in template order. */
	final List<Entry> roots = new ArrayList<>();

	/** Child-list stack; the tail is where the next {@code <@node>} is appended. */
	private final Deque<List<Entry>> childLists = new ArrayDeque<>();

	NavContext() {
		childLists.addLast(roots);
	}

	/** Registers {@code node} under the current parent (or as a root) and starts collecting its children. */
	void push(Entry node) {
		childLists.peekLast().add(node);
		childLists.addLast(node.children);
	}

	/** Ends collection of the current node's children. */
	void pop() {
		childLists.removeLast();
	}

	/**
	 * Emits {@code <nav class="juneau-page-nav">}: all depth-0 nodes as {@code .juneau-page-nav-sections},
	 * then one {@code .juneau-page-nav-children} row per selected ancestor that has nested nodes.
	 */
	void write(Writer out, String layout) throws IOException {
		out.write("<nav class=\"juneau-page-nav\"");
		if ("vertical".equals(layout))
			out.write(" data-juneau-nav-layout=\"vertical\"");
		out.write("><div class=\"juneau-page-nav-sections\">");
		for (var n : roots)
			writeLink(out, n, "juneau-page-nav-section");
		out.write("</div>");
		var level = roots;
		while (true) {
			Entry selected = null;
			for (var n : level) {
				if (n.current) {
					selected = n;
					break;
				}
			}
			if (selected == null || selected.children.isEmpty())
				break;
			out.write("<div class=\"juneau-page-nav-children\">");
			for (var c : selected.children)
				writeLink(out, c, "juneau-page-nav-child");
			out.write("</div>");
			level = selected.children;
		}
		out.write("</nav>");
	}

	private static void writeLink(Writer out, Entry n, String cls) throws IOException {
		out.write("<a class=\"" + cls + "\"");
		if (! n.href.isEmpty())
			out.write(" href=\"" + n.href + "\"");
		if (n.current)
			out.write(" aria-current=\"page\"");
		out.write(">");
		out.write(n.label);
		out.write("</a>");
	}

	/** One authored {@code <@node>} in the tree. */
	static final class Entry {
		final String label;
		final String href;
		final boolean current;
		final List<Entry> children = new ArrayList<>();

		Entry(String label, String href, boolean current) {
			this.label = label;
			this.href = href;
			this.current = current;
		}
	}
}
