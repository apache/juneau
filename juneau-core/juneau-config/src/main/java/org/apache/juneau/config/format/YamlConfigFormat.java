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
package org.apache.juneau.config.format;

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.emptyIfNull;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.config.internal.*;

/**
 * YAML config format.
 */
public class YamlConfigFormat implements ConfigFormat {

	/** Singleton instance. */
	public static final YamlConfigFormat INSTANCE = new YamlConfigFormat();

	/**
	 * Constructor.
	 */
	protected YamlConfigFormat() {}

	@Override /* ConfigFormat */
	public String id() {
		return "yaml";
	}

	@Override /* ConfigFormat */
	@SuppressWarnings({
		"java:S3776", // Cognitive complexity acceptable for YAML-to-INI format conversion logic
		"java:S135" // Multiple continue statements are intentional in this line-by-line YAML parsing state machine.
	})
	public String toInternal(String contents) throws IOException {
		if (contents == null)
			return "";

		var lines = splitLines(contents);
		var sb = new StringBuilder();
		var stack = new ArrayDeque<Node>();
		var preLines = new ArrayList<String>();
		String lastSection = null;

		for (var i = 0; i < lines.size(); i++) {
			var line = lines.get(i);
			var trim = line.trim();
			if (trim.isEmpty() || trim.startsWith("#") || trim.equals("---")) {
				preLines.add(line);
				continue;
			}

			var indent = leadingSpaces(line);
			while (! stack.isEmpty() && indent <= stack.peekLast().indent)
				stack.removeLast();

			var colonIndex = trim.indexOf(':');
			if (colonIndex <= 0)
				throw new IOException("Invalid YAML config line: " + line);

			var key = trim.substring(0, colonIndex).trim();
			var rest = trim.substring(colonIndex + 1).trim();
			if ("_imports".equals(key))
				continue;

			if (rest.isEmpty()) {
				stack.addLast(new Node(key, indent));
				continue;
			}

			String comment = null;
			var commentIndex = rest.indexOf(" #");
			if (commentIndex != -1) {
				comment = rest.substring(commentIndex + 2).trim();
				rest = rest.substring(0, commentIndex).trim();
			}

			var value = unquote(rest);
			var section = section(stack);
			if (! eq(section, lastSection)) {
				appendPreLines(sb, preLines);
				if (ne(section))
					sb.append('[').append(section).append(']').append('\n');
				lastSection = section;
			}
			appendPreLines(sb, preLines);
			sb.append(key).append(" = ").append(value);
			if (ne(comment))
				sb.append(" # ").append(comment);
			sb.append('\n');
		}
		appendPreLines(sb, preLines);

		return sb.toString();
	}

	@Override /* ConfigFormat */
	public String fromInternal(ConfigMap map) {
		var sb = new StringBuilder();
		List<String> opened = new ArrayList<>();
		for (var section : map.getSections()) {
			var segments = splitPath(section);
			var common = commonPrefix(opened, segments);
			for (var i = common; i < segments.size(); i++) {
				indent(sb, i).append(segments.get(i)).append(':').append('\n');
			}
			opened = segments;
			var keys = map.getKeys(section);
			for (var key : keys) {
				var entry = map.getEntry(section, key);
				var value = entry == null ? "" : emptyIfNull(entry.getValue());
				indent(sb, segments.size()).append(key).append(": ").append(quoteIfNeeded(value));
				if (entry != null && ne(entry.getComment()))
					sb.append(" # ").append(entry.getComment());
				sb.append('\n');
			}
		}
		return sb.toString();
	}

	private int commonPrefix(List<String> a, List<String> b) {
		var i = 0;
		while (i < a.size() && i < b.size() && eq(a.get(i), b.get(i)))
			i++;
		return i;
	}

	private List<String> splitLines(String contents) throws IOException {
		try (var r = new BufferedReader(new StringReader(contents))) {
			var out = new ArrayList<String>();
			String line;
			while ((line = r.readLine()) != null)
				out.add(line);
			return out;
		}
	}

	private List<String> splitPath(String section) {
		if (isEmpty(section))
			return Collections.emptyList();
		return Arrays.asList(section.split("/"));
	}

	private String section(Deque<Node> stack) {
		if (stack.isEmpty())
			return "";
		var sb = new StringBuilder();
		for (var node : stack) {
			if (! sb.isEmpty())
				sb.append('/');
			sb.append(node.name);
		}
		return sb.toString();
	}

	private StringBuilder indent(StringBuilder sb, int level) {
		for (var i = 0; i < level; i++)
			sb.append("  ");
		return sb;
	}

	private int leadingSpaces(String line) {
		var i = 0;
		while (i < line.length() && line.charAt(i) == ' ')
			i++;
		return i;
	}

	private void appendPreLines(StringBuilder sb, List<String> preLines) {
		for (var line : preLines)
			sb.append(line).append('\n');
		preLines.clear();
	}

	private String unquote(String value) {
		if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))))
			return value.substring(1, value.length() - 1);
		return value;
	}

	private String quoteIfNeeded(String value) {
		if (value.isEmpty())
			return "\"\"";
		if (value.indexOf(':') != -1 || value.indexOf('#') != -1 || value.startsWith(" ") || value.endsWith(" "))
			return '"' + value.replace("\"", "\\\"") + '"';
		return value;
	}

	private static class Node {
		final String name;
		final int indent;

		Node(String name, int indent) {
			this.name = name;
			this.indent = indent;
		}
	}
}
