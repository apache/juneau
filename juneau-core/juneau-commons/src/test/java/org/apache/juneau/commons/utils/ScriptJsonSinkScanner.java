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
package org.apache.juneau.commons.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Test-only, <b>private</b> source scanner enforcing the {@code <script>}-JSON adoption of
 * {@link StringUtils#escapeForScript(String)} across the Juneau framework's own code (TODO-431 Phase 3).
 *
 * <p>
 * Not shipped and deliberately not public API: it guards the framework's own raw-text {@code <script>} sinks and is
 * <b>not</b> a tool consumers are expected to run. It is the durable half of the {@code escapeForScript} adoption
 * sweep &mdash; a one-time migration decays, so this fails the build if a framework source ever writes a serialized
 * JSON payload into a raw-text {@code <script>} element without routing it through {@code escapeForScript} first.
 *
 * <h5 class='section'>What counts as a sink</h5>
 * <p>
 * The framework's single mechanism for inserting <i>verbatim</i> (non-entity-encoded) content into HTML output is the
 * {@code juneau-bean-html5} raw-text primitive &mdash; {@link org.apache.juneau.bean.html5.HtmlBuilder#rawText(String)}
 * and {@code new RawText(...)}. A {@code <script type="application/json">} sidecar's body is exactly such content.
 * A <b>sink</b> is therefore a statement that builds a {@code script(...)} element <i>and</i> inserts a
 * {@code rawText(...)} / {@code new RawText(...)} payload &mdash; which is precisely the shape
 * {@code ViewTable}/{@code PageTable} use. For each sink the payload argument must resolve to an
 * {@code escapeForScript(...)} result (directly, or via a same-file {@code var = escapeForScript(...)} assignment).
 *
 * <h5 class='section'>Why it must also assert it still finds the known-good sites</h5>
 * <p>
 * A scanner that silently stops matching reads as a passing test, which is worse than no scanner. Comments are
 * therefore stripped before analysis (so a {@code escapeForScript} mention in a javadoc cannot vacuously satisfy the
 * guard), and the accompanying test asserts the scan still finds the real {@code ViewTable}/{@code PageTable} sinks
 * and that removing the escaper from one of them turns it into a violation &mdash; so "zero violations" can never mean
 * "zero files examined".
 *
 * <h5 class='section'>Contract boundary</h5>
 * <p>
 * This enforces the JSON-in/JSON-out contract only; it does not widen {@code escapeForScript} to arbitrary raw-text
 * payloads and does not attempt to detect hand-built {@code <script>} JSON assembled by raw string concatenation
 * (the framework does not do that &mdash; it goes through the html5 builder). It scans the framework's Java sources on
 * disk; it does not and cannot see consumer applications.
 */
final class ScriptJsonSinkScanner {

	private ScriptJsonSinkScanner() {}

	/** One raw-text {@code <script>} payload site found in a source. */
	record Sink(String file, boolean escaped, String detail) {}

	/** The outcome of a scan: every sink found, and the subset that are violations. */
	record Result(List<Sink> sinks, List<String> violations) {}

	/** Matches a {@code rawText(} or {@code new RawText(} call (the html5 raw-text primitive). */
	private static final Pattern RAWTEXT_CALL = Pattern.compile("(?:new\\s+RawText|\\brawText)\\s*\\(");

	/** An argument that is (directly, possibly fully-qualified) an {@code escapeForScript(...)} call. */
	private static final Pattern DIRECT_ESCAPE = Pattern.compile("(?s)^[A-Za-z0-9_.]*escapeForScript\\(.*");

	/** A bare Java identifier (the variable-indirection case). */
	private static final Pattern IDENT = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

	/**
	 * Scans a single Java source string.
	 *
	 * @param file A label for the source (used in violation messages).
	 * @param source The Java source text.
	 * @return The sinks found and any violations.
	 */
	static Result scan(String file, String source) {
		var sinks = new ArrayList<Sink>();
		var violations = new ArrayList<String>();
		var code = stripCommentsAndStrings(source);

		var m = RAWTEXT_CALL.matcher(code);
		while (m.find()) {
			var openParen = m.end() - 1; // the '(' of the rawText/RawText call
			var stmtStart = statementStart(code, m.start());
			var stmt = code.substring(stmtStart, m.start());
			if (! stmt.contains("script("))
				continue; // a raw-text write that is not into a <script> element - out of scope (JSON-in/JSON-out only)

			var arg = balancedArg(code, openParen).trim();
			var escaped = isEscaped(arg, code);
			sinks.add(new Sink(file, escaped, "rawText(" + arg + ")"));
			if (! escaped)
				violations.add(file + ": <script> raw-text payload not routed through escapeForScript: rawText(" + arg + ")");
		}
		return new Result(sinks, violations);
	}

	/** Convenience overload for ad-hoc source strings. */
	static Result scan(String source) {
		return scan("(source)", source);
	}

	/**
	 * Walks a repository tree and scans every {@code src/main/java} Java source under it.
	 *
	 * @param repoRoot The repository root.
	 * @return The aggregated sinks and violations across the tree.
	 * @throws IOException If the tree cannot be walked.
	 */
	static Result scanTree(Path repoRoot) throws IOException {
		var sinks = new ArrayList<Sink>();
		var violations = new ArrayList<String>();
		var mainJava = File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator;
		var target = File.separator + "target" + File.separator;
		try (var stream = Files.walk(repoRoot)) {
			var files = stream
				.filter(p -> p.toString().endsWith(".java"))
				.filter(p -> p.toString().contains(mainJava))
				.filter(p -> ! p.toString().contains(target))
				.sorted()
				.toList();
			for (var f : files) {
				var r = scan(repoRoot.relativize(f).toString(), Files.readString(f));
				sinks.addAll(r.sinks());
				violations.addAll(r.violations());
			}
		}
		return new Result(sinks, violations);
	}

	/**
	 * Locates the Juneau repository root by walking up from the working directory until a directory holding both
	 * {@code juneau-core} and {@code juneau-rest} is found.
	 *
	 * @return The repository root, or {@code null} if not found.
	 */
	static Path locateRepoRoot() {
		for (var d = Path.of("").toAbsolutePath(); d != null; d = d.getParent())
			if (Files.isDirectory(d.resolve("juneau-core")) && Files.isDirectory(d.resolve("juneau-rest")))
				return d;
		return null;
	}

	/** True if {@code arg} is an escapeForScript result, directly or via a same-source {@code arg = escapeForScript(...)} assignment. */
	private static boolean isEscaped(String arg, String code) {
		if (DIRECT_ESCAPE.matcher(arg).matches())
			return true;
		if (IDENT.matcher(arg).matches()) {
			var p = Pattern.compile("\\b" + Pattern.quote(arg) + "\\s*=\\s*[A-Za-z0-9_.]*escapeForScript\\(");
			return p.matcher(code).find();
		}
		return false;
	}

	/** Index just past the previous statement/block boundary ({@code ;}, <code>{</code>, or <code>}</code>) before {@code from}. */
	private static int statementStart(String s, int from) {
		for (var i = from - 1; i >= 0; i--) {
			var c = s.charAt(i);
			if (c == ';' || c == '{' || c == '}')
				return i + 1;
		}
		return 0;
	}

	/** Returns the (comma-and-nesting-preserving) argument text between the balanced parens starting at {@code openParen}. */
	private static String balancedArg(String s, int openParen) {
		var depth = 0;
		var sb = new StringBuilder();
		for (var i = openParen; i < s.length(); i++) {
			var c = s.charAt(i);
			if (c == '(') {
				depth++;
				if (depth == 1)
					continue;
			} else if (c == ')') {
				depth--;
				if (depth == 0)
					break;
			}
			sb.append(c);
		}
		return sb.toString();
	}

	/**
	 * Blanks out comment and string/char-literal <i>content</i> (keeping quote delimiters and newlines) so pattern
	 * matching sees only the code skeleton. Critical for correctness: a {@code escapeForScript} or {@code <script}
	 * mention inside a javadoc or comment must not be mistaken for the code doing the right (or wrong) thing.
	 */
	private static String stripCommentsAndStrings(String s) {
		var out = new StringBuilder(s.length());
		var n = s.length();
		var i = 0;
		while (i < n) {
			var c = s.charAt(i);
			if (c == '/' && i + 1 < n && s.charAt(i + 1) == '/') {
				while (i < n && s.charAt(i) != '\n')
					i++;
				continue;
			}
			if (c == '/' && i + 1 < n && s.charAt(i + 1) == '*') {
				i += 2;
				while (i + 1 < n && ! (s.charAt(i) == '*' && s.charAt(i + 1) == '/')) {
					if (s.charAt(i) == '\n')
						out.append('\n');
					i++;
				}
				i = Math.min(n, i + 2);
				continue;
			}
			if (c == '"' && i + 2 < n && s.charAt(i + 1) == '"' && s.charAt(i + 2) == '"') {
				out.append("\"\"\"");
				i += 3;
				while (i + 2 < n && ! (s.charAt(i) == '"' && s.charAt(i + 1) == '"' && s.charAt(i + 2) == '"')) {
					if (s.charAt(i) == '\n')
						out.append('\n');
					i++;
				}
				if (i + 2 < n) {
					out.append("\"\"\"");
					i += 3;
				} else {
					i = n;
				}
				continue;
			}
			if (c == '"') {
				out.append('"');
				i++;
				while (i < n && s.charAt(i) != '"') {
					if (s.charAt(i) == '\\' && i + 1 < n) {
						i += 2;
						continue;
					}
					if (s.charAt(i) == '\n')
						out.append('\n');
					i++;
				}
				out.append('"');
				i++;
				continue;
			}
			if (c == '\'') {
				out.append('\'');
				i++;
				while (i < n && s.charAt(i) != '\'') {
					if (s.charAt(i) == '\\' && i + 1 < n) {
						i += 2;
						continue;
					}
					i++;
				}
				out.append('\'');
				i++;
				continue;
			}
			out.append(c);
			i++;
		}
		return out.toString();
	}
}
