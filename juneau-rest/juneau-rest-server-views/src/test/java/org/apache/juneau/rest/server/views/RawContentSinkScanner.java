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
package org.apache.juneau.rest.server.views;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Test-only, <b>private</b> source scanner enforcing the TODO-420 BLK-1 ownership contract &mdash; "template
 * engine, trusted / first-party content only" &mdash; on {@link Tab#content}/{@link Subtab#content}.
 *
 * <p>
 * Not shipped and deliberately not public API, mirroring the discipline of the {@code ScriptJsonSinkScanner}
 * (juneau-commons) and {@code ChromeCssScanner} (juneau-rest-server-console-ui) test-only guards this class is
 * modeled on. It scans this module's own Java sources (both {@code src/main/java} and {@code src/test/java}, since
 * unlike the {@code <script>}-JSON sink this is a brand-new, forward-looking guardrail with no pre-existing
 * "known-good" callers to anchor a main-source-only scan to &mdash; see the accompanying test's anti-vacuous
 * checks) and fails the build if any call to the {@link Tab#content(String)}/{@link Subtab#content(String)}
 * fluent-setter passes an argument that is not a compile-time string literal (or a {@code +}-concatenation of
 * literals).
 *
 * <h5 class='section'>Why "is the argument a literal" is the enforcement mechanism</h5>
 * <p>
 * A build-time scanner cannot do taint analysis, so it cannot directly ask "does this value ever carry
 * live/remote/attacker-influenceable data". A compile-time string literal provably cannot: it is baked into the
 * {@code .class} file, reviewed the same way any other source line is, and by construction carries no
 * request/response/remote-call data. Conversely, <i>any</i> non-literal argument &mdash; an identifier, a field
 * read, a method call, string concatenation involving a non-literal &mdash; is exactly the shape a write-path
 * confirmation/detail emitter would use to build a body from live data (an incident title, a Slack thread, a
 * request parameter). Requiring literal-only arguments is therefore the strongest checkable proxy for the
 * "trusted / first-party content only" contract documented on {@link Tab#content}/{@link Subtab#content}: it lets
 * through exactly the FG-2 docs-prose use case (hand-authored markup baked into the source) and flags everything
 * a live-data confirmation body would look like.
 *
 * <h5 class='section'>What counts as a sink</h5>
 * <p>
 * A sink is any source-level call whose method name is exactly {@code content} (i.e. {@code .content(...)}) &mdash;
 * scoped to this module's own tree, where (verified before this scanner was written) the only such calls are the
 * {@link Tab}/{@link Subtab} fluent setters this item adds; nothing else in this module declares or calls a
 * {@code content(...)} method. This scanner does not attempt receiver-type resolution and does not scan outside
 * this module (other Juneau modules declare unrelated {@code content(...)} methods, e.g.
 * {@code RestRequest.content(Object)}, which are out of scope by construction because they are never on this
 * module's classpath of scanned files).
 *
 * <h5 class='section'>Why it must also assert it is not vacuous</h5>
 * <p>
 * A scanner that silently stops matching reads as a passing test, which is worse than no scanner. Comments are
 * therefore stripped before analysis (a mention of {@code .content(} inside a comment must not be flagged, and
 * must not be mistaken for a real call either), and the accompanying test asserts the live scan of this module's
 * own tree still finds real, known-good literal sinks (the ones this item's own test fixtures add) and that
 * mutating a known-good literal call into a non-literal one turns it into a violation.
 */
final class RawContentSinkScanner {

	private RawContentSinkScanner() {}

	/** One {@code .content(...)} call site found in a source. */
	record Sink(String file, boolean literalOnly, String detail) {}

	/** The outcome of a scan: every sink found, and the subset that are violations. */
	record Result(List<Sink> sinks, List<String> violations) {}

	/** Matches a {@code .content(} call (the {@code Tab}/{@code Subtab} raw-content fluent setter). */
	private static final Pattern CONTENT_CALL = Pattern.compile("\\.content\\s*\\(");

	/** Matches an {@code innerHTML =} assignment in JavaScript (the XSS sink the chooser must never use). */
	private static final Pattern JS_INNERHTML_ASSIGN = Pattern.compile("\\.innerHTML\\s*=");

	/** Matches a jQuery {@code .html(} call in JavaScript (the other XSS sink the chooser must never use). */
	private static final Pattern JS_JQUERY_HTML = Pattern.compile("\\.html\\s*\\(");

	/** Matches an {@code outerHTML =} assignment. */
	private static final Pattern JS_OUTERHTML_ASSIGN = Pattern.compile("\\.outerHTML\\s*=");

	/** Matches {@code insertAdjacentHTML(}. */
	private static final Pattern JS_INSERT_ADJACENT_HTML = Pattern.compile("insertAdjacentHTML\\s*\\(");

	/** Matches {@code document.write(}. */
	private static final Pattern JS_DOCUMENT_WRITE = Pattern.compile("document\\.write\\s*\\(");

	/** First-party icon-registry SVG assignments; never request/app/renderer text. */
	private static final List<AllowedJsSink> SHIPPED_JS_ALLOWLIST = List.of(
		new AllowedJsSink("src/main/resources/org/apache/juneau/views/juneau-views.js", "b.innerHTML = markup;"),
		new AllowedJsSink("src/main/resources/org/apache/juneau/views/juneau-views.js", "caretEl.innerHTML = caretMarkup;"),
		new AllowedJsSink("src/main/resources/org/apache/juneau/views/juneau-ribbon.js", "b.innerHTML = markup;")
	);

	/** Shipped widget JS assets scanned by {@link #scanShippedJs(Path)}. */
	private static final List<String> SHIPPED_JS_FILES = List.of(
		"juneau-config.js", "juneau-pages.js", "juneau-icons.js",
		"juneau-renders.js", "juneau-views.js", "juneau-ribbon.js"
	);

	record AllowedJsSink(String relativePath, String snippet) {}

	/** A single string literal or text-block literal (content kept verbatim by {@link #stripComments}). */
	private static final Pattern LITERAL_TOKEN =
		Pattern.compile("\\s*(?:\"\"\"[\\s\\S]*?\"\"\"|\"(?:[^\"\\\\]|\\\\.)*\"|null)\\s*");

	/**
	 * Excluded, by simple filename, from {@link #scanTree(Path)}: this is the accompanying security-test file,
	 * which deliberately embeds synthetic RED (and GREEN) fixtures as source-text string/text-block literals
	 * (e.g. a fixture literally containing {@code "Tab.create(...).content(someIdentifier)"} as example text).
	 * Because {@link #stripComments} keeps string content verbatim (needed so real call arguments stay
	 * inspectable - see this class's javadoc), tree-scanning that file would find its RED-fixture example text and
	 * flag it as a false-positive violation purely because the file holding the example is itself in the scanned
	 * tree. This is the only file in this module whose job is to hold such text as <i>data</i> rather than as a
	 * real call, so excluding it by name is narrow and safe; every other test file in this tree using
	 * {@code .content(...)} is a real call this scanner is meant to see (and lean on for anti-vacuousness).
	 */
	private static final String EXCLUDED_FIXTURE_FILE = "RawContentSink_SecurityScan_Test.java";

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
		var code = stripComments(source);

		var m = CONTENT_CALL.matcher(code);
		while (m.find()) {
			var openParen = m.end() - 1;
			var arg = balancedArg(code, openParen).trim();
			var literalOnly = isLiteralOnly(arg);
			sinks.add(new Sink(file, literalOnly, "content(" + arg + ")"));
			if (!literalOnly)
				violations.add(file + ": content(...) argument is not a compile-time literal - flagged as a "
					+ "possible live-data write-path confirmation/detail body reaching the raw-markup sink: "
					+ "content(" + arg + ")");
		}
		return new Result(sinks, violations);
	}

	/** Convenience overload for ad-hoc source strings. */
	static Result scan(String source) {
		return scan("(source)", source);
	}

	/**
	 * Scans a JavaScript source string for {@code innerHTML =} / {@code .html(} sinks.  Comments are stripped
	 * first so a mention of the forbidden APIs in a comment is not a hit.
	 *
	 * @param file A label for the source (used in violation messages).
	 * @param source The JavaScript source text.
	 * @return The sinks found and any violations.  Every HTML sink here is a violation (there is no
	 * 	literal-only carve-out: user-controlled strings must never reach these APIs).
	 */
	static Result scanJsHtmlSinks(String file, String source) {
		return scanJsHtmlSinks(file, source, Set.of());
	}

	/**
	 * Scans a JavaScript source string for HTML-injection assignments.  Comments are stripped first.
	 * Hits whose unique assignment snippet is in {@code allowedSnippets} are recorded as sinks but not
	 * violations.
	 *
	 * @param file A label for the source (used in violation messages).
	 * @param source The JavaScript source text.
	 * @param allowedSnippets Unique assignment snippets that are audited first-party sites.
	 * @return The sinks found and any violations.
	 */
	static Result scanJsHtmlSinks(String file, String source, Set<String> allowedSnippets) {
		var sinks = new ArrayList<Sink>();
		var violations = new ArrayList<String>();
		var code = stripComments(source);
		addJsHits(file, code, JS_INNERHTML_ASSIGN, "innerHTML assignment", allowedSnippets, sinks, violations);
		addJsHits(file, code, JS_JQUERY_HTML, ".html( call", allowedSnippets, sinks, violations);
		addJsHits(file, code, JS_OUTERHTML_ASSIGN, "outerHTML assignment", allowedSnippets, sinks, violations);
		addJsHits(file, code, JS_INSERT_ADJACENT_HTML, "insertAdjacentHTML( call", allowedSnippets, sinks, violations);
		addJsHits(file, code, JS_DOCUMENT_WRITE, "document.write( call", allowedSnippets, sinks, violations);
		return new Result(sinks, violations);
	}

	private static void addJsHits(String file, String code, Pattern pattern, String kind,
			Set<String> allowedSnippets, List<Sink> sinks, List<String> violations) {
		var m = pattern.matcher(code);
		while (m.find()) {
			var snippet = jsSinkSnippet(code, m.start(), m.end());
			sinks.add(new Sink(file, false, snippet));
			if (allowedSnippets != null && allowedSnippets.contains(snippet))
				continue;
			violations.add(file + ": " + kind + " (" + snippet + ") — user-controlled strings must be painted with textContent / input.value only");
		}
	}

	/** Unique assignment snippet: identifier chain through the terminating semicolon (or newline). */
	static String jsSinkSnippet(String code, int matchStart, int matchEnd) {
		var from = matchStart;
		while (from > 0) {
			var c = code.charAt(from - 1);
			if (Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '.')
				from--;
			else
				break;
		}
		var to = matchEnd;
		while (to < code.length() && code.charAt(to) != ';' && code.charAt(to) != '\n')
			to++;
		if (to < code.length() && code.charAt(to) == ';')
			to++;
		return code.substring(from, to).trim();
	}

	/**
	 * Walks the six shipped {@code juneau-*.js} assets.  Hits are violations unless the unique snippet is on
	 * the first-party icon-registry allowlist.
	 *
	 * @param moduleRoot The {@code juneau-rest-server-views} module root.
	 * @return Aggregated sinks and violations, plus the allowlisted-hit count in {@link Result#sinks()}.
	 * @throws IOException If an asset cannot be read.
	 */
	static Result scanShippedJs(Path moduleRoot) throws IOException {
		var sinks = new ArrayList<Sink>();
		var violations = new ArrayList<String>();
		var viewsDir = Path.of("src", "main", "resources", "org", "apache", "juneau", "views");
		for (var name : SHIPPED_JS_FILES) {
			var rel = viewsDir.resolve(name);
			var file = moduleRoot.resolve(rel);
			var allowed = new HashSet<String>();
			for (var a : SHIPPED_JS_ALLOWLIST)
				if (a.relativePath().equals(rel.toString().replace('\\', '/')))
					allowed.add(a.snippet());
			var r = scanJsHtmlSinks(rel.toString().replace('\\', '/'), Files.readString(file), allowed);
			sinks.addAll(r.sinks());
			violations.addAll(r.violations());
		}
		return new Result(sinks, violations);
	}

	/** Allowlisted snippets that must still exist in their files (anti-vacuous). */
	static List<AllowedJsSink> shippedJsAllowlist() {
		return SHIPPED_JS_ALLOWLIST;
	}

	/**
	 * Scans the shipped {@code juneau-config.js} asset under {@code src/main/resources} for HTML sinks.
	 *
	 * @param moduleRoot The {@code juneau-rest-server-views} module root.
	 * @return The scan result for that one file.
	 * @throws IOException If the asset cannot be read.
	 */
	static Result scanConfigJs(Path moduleRoot) throws IOException {
		var rel = Path.of("src", "main", "resources", "org", "apache", "juneau", "views", "juneau-config.js");
		var file = moduleRoot.resolve(rel);
		return scanJsHtmlSinks(rel.toString(), Files.readString(file));
	}

	/**
	 * Walks the specified module root and scans every {@code src/main/java} and {@code src/test/java} Java
	 * source under it (both trees, per this class's javadoc: this guardrail has no pre-existing main-source-only
	 * "known-good" anchor to lean on the way {@code ScriptJsonSinkScanner} does).
	 *
	 * @param moduleRoot The module root (e.g. {@code juneau-rest/juneau-rest-server-views}).
	 * @return The aggregated sinks and violations across the tree.
	 * @throws IOException If the tree cannot be walked.
	 */
	static Result scanTree(Path moduleRoot) throws IOException {
		var sinks = new ArrayList<Sink>();
		var violations = new ArrayList<String>();
		var target = File.separator + "target" + File.separator;
		for (var sourceDir : List.of("main", "test")) {
			var srcRoot = moduleRoot.resolve("src").resolve(sourceDir).resolve("java");
			if (!Files.isDirectory(srcRoot))
				continue;
			try (var stream = Files.walk(srcRoot)) {
				var files = stream
					.filter(p -> p.toString().endsWith(".java"))
					.filter(p -> !p.toString().contains(target))
					.filter(p -> !p.getFileName().toString().equals(EXCLUDED_FIXTURE_FILE))
					.sorted()
					.toList();
				for (var f : files) {
					var r = scan(moduleRoot.relativize(f).toString(), Files.readString(f));
					sinks.addAll(r.sinks());
					violations.addAll(r.violations());
				}
			}
		}
		return new Result(sinks, violations);
	}

	/**
	 * Locates the {@code juneau-rest-server-views} module root by walking up from the working directory until a
	 * directory holding both {@code juneau-core} and {@code juneau-rest} is found, then resolving into it.
	 *
	 * @return The module root, or {@code null} if not found.
	 */
	static Path locateModuleRoot() {
		for (var d = Path.of("").toAbsolutePath(); d != null; d = d.getParent()) {
			if (Files.isDirectory(d.resolve("juneau-core")) && Files.isDirectory(d.resolve("juneau-rest"))) {
				var module = d.resolve("juneau-rest").resolve("juneau-rest-server-views");
				return Files.isDirectory(module) ? module : null;
			}
		}
		return null;
	}

	/** True if {@code arg} is composed entirely of string/text-block literals (and/or {@code null}), optionally {@code +}-joined. */
	private static boolean isLiteralOnly(String arg) {
		var s = arg;
		if (s.isBlank())
			return true; // a no-arg / blank call carries nothing - trivially not a data leak
		var i = 0;
		while (i < s.length()) {
			var m = LITERAL_TOKEN.matcher(s.substring(i));
			if (!m.lookingAt())
				return false;
			i += m.end();
			if (i >= s.length())
				return true;
			if (s.charAt(i) != '+')
				return false;
			i++;
		}
		return true;
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
	 * Blanks out comment content (keeping string/char literals fully intact, including their delimiters and
	 * inner text) so that (a) a {@code .content(} mention inside a comment cannot be mistaken for a real call, and
	 * (b) the real literal text of a string argument stays inspectable by {@link #isLiteralOnly(String)}. This is
	 * the one deliberate divergence from {@code ScriptJsonSinkScanner}'s stripper, which blanks string content too
	 * - that scanner never needs to look inside a literal, this one always does.
	 */
	private static String stripComments(String s) {
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
				while (i + 1 < n && !(s.charAt(i) == '*' && s.charAt(i + 1) == '/')) {
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
				while (i + 2 < n && !(s.charAt(i) == '"' && s.charAt(i + 1) == '"' && s.charAt(i + 2) == '"')) {
					out.append(s.charAt(i));
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
						out.append(s.charAt(i)).append(s.charAt(i + 1));
						i += 2;
						continue;
					}
					out.append(s.charAt(i));
					i++;
				}
				if (i < n) {
					out.append('"');
					i++;
				}
				continue;
			}
			if (c == '\'') {
				out.append('\'');
				i++;
				while (i < n && s.charAt(i) != '\'') {
					if (s.charAt(i) == '\\' && i + 1 < n) {
						out.append(s.charAt(i)).append(s.charAt(i + 1));
						i += 2;
						continue;
					}
					out.append(s.charAt(i));
					i++;
				}
				if (i < n) {
					out.append('\'');
					i++;
				}
				continue;
			}
			out.append(c);
			i++;
		}
		return out.toString();
	}
}
