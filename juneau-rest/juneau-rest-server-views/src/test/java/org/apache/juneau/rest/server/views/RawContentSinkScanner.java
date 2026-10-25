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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Test-only, <b>private</b> source scanner with two jobs:
 * <ul>
 * 	<li>Scans the shipped {@code juneau-*.js} console assets for HTML-injection sinks (an {@code innerHTML} or
 * 		{@code outerHTML} assignment, {@code insertAdjacentHTML(}, {@code document.write(}, jQuery {@code .html(}),
 * 		failing on any hit that is not on the audited first-party allowlist.  See {@link #scanShippedJs}.
 * 	<li>Classifies every {@code rawText(...)} call site in the views and widgets main sources, which
 * 		{@code EmitterPurity_ScanTest} uses to prove emitters only hand the verbatim writer JSON-sidecar payloads.
 * </ul>
 *
 * <p>
 * Not shipped and deliberately not public API, mirroring the test-only guards {@code ScriptJsonSinkScanner}
 * (juneau-commons) and {@code ChromeCssScanner} (juneau-rest-server-console-ui).  Comments are stripped before
 * analysis (see {@link #stripComments}) so a mention of a forbidden API in a comment is not a hit, while string
 * literal text stays inspectable.  The JS scan reaches sibling modules (console-ui, widgets, terminal, datatables)
 * through relative paths, so a relocated module fails the read rather than silently dropping out; the accompanying
 * test also compares the list against what is on disk.
 */
final class RawContentSinkScanner {

	private RawContentSinkScanner() {}

	/** One HTML-sink hit found in a shipped JavaScript source; {@code detail} is the unique assignment snippet. */
	record Sink(String file, String detail) {}

	/** The outcome of a scan: every sink found, and the subset that are violations. */
	record Result(List<Sink> sinks, List<String> violations) {}

	/**
	 * Matches a {@code rawText(} call &mdash; the html5 primitive that writes its argument <b>verbatim</b>, bypassing
	 * Juneau's normal XML/HTML text escaping.  This is the sink the emitter-purity scan
	 * ({@code EmitterPurity_ScanTest}) reasons about: the question is "what does the <i>emitter</i> hand to the
	 * verbatim writer".
	 */
	private static final Pattern RAW_TEXT_CALL = Pattern.compile("\\brawText\\s*\\(");

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

	/** This module's shipped-JS resource directory, relative to the module root. */
	private static final String VIEWS_JS_DIR = "src/main/resources/org/apache/juneau/views";

	/** The console-ui module's shipped-JS resource directory (the console shell), relative to THIS module's root. */
	private static final String CONSOLE_JS_DIR =
		"../juneau-rest-server-console-ui/src/main/resources/org/apache/juneau/console";

	/** The terminal module's shipped-JS resource directory, relative to THIS module's root. */
	private static final String TERMINAL_JS_DIR =
		"../juneau-rest-server-terminal/src/main/resources/org/apache/juneau/terminal";

	/** The datatables module's shipped-JS resource directory, relative to THIS module's root. */
	private static final String DATATABLES_JS_DIR =
		"../juneau-rest-server-datatables/src/main/resources/org/apache/juneau/rest/server/datatables";

	/**
	 * The WIDGET module's shipped-JS resource directory, relative to THIS module's root.
	 *
	 * <p>The card / calendar assets ship there now.  A guardrail follows the file it guards rather than the
	 * module the test happens to live in - scanning a views path for a relocated asset would silently scan nothing.
	 */
	private static final String WIDGETS_JS_DIR =
		"../juneau-rest-server-widgets/src/main/resources/org/apache/juneau/widgets";

	/**
	 * The widgets module's root, relative to this module's root &mdash; the Java-source counterpart of
	 * {@link #WIDGETS_JS_DIR}.  The widgets emitters live there, so
	 * {@link #scanRawTextTree} follows this path rather than stopping at the module boundary the test happens to run in.
	 */
	private static final String WIDGETS_MODULE_DIR = "../juneau-rest-server-widgets";

	/** Audited allowlist: icon-registry SVG and author-trusted page-contract markup. */
	private static final List<AllowedJsSink> SHIPPED_JS_ALLOWLIST = List.of(
		new AllowedJsSink(VIEWS_JS_DIR + "/juneau-views.js", "b.innerHTML = markup;", 2),
		new AllowedJsSink(VIEWS_JS_DIR + "/juneau-views.js", "caretEl.innerHTML = caretMarkup;"),
		new AllowedJsSink(VIEWS_JS_DIR + "/juneau-views.js", "slot.innerHTML = markup;"),
		new AllowedJsSink(VIEWS_JS_DIR + "/juneau-ribbon.js", "b.innerHTML = markup;"),
		new AllowedJsSink(VIEWS_JS_DIR + "/juneau-helpers.js", "span.innerHTML = markup;"),
		// First-party icon-registry SVG (resolveIcon), never request/row text: column search and sort glyphs.
		new AllowedJsSink(VIEWS_JS_DIR + "/juneau-views.js", "icon.innerHTML = markup;"),
		new AllowedJsSink(VIEWS_JS_DIR + "/juneau-views.js", "orderSpan.insertAdjacentHTML(\"afterbegin\", markup);"),
		new AllowedJsSink(CONSOLE_JS_DIR + "/juneau-console.js", "iconSpan.innerHTML = glyph;"),
		// Author-trusted markup, NOT inert: a template element keeps scripts from running, but on* handlers fire
		// after importNode.  footer.text is trusted HTML per the page schema, and the html-card src is a same-origin
		// endpoint that must escape its own output.  The page author is the trust boundary.
		new AllowedJsSink(CONSOLE_JS_DIR + "/juneau-console.js", "ftpl.innerHTML = f.text;"),
		new AllowedJsSink(CONSOLE_JS_DIR + "/juneau-console.js", "tpl.innerHTML = text;")
	);

	/** Shipped JS assets scanned by {@link #scanShippedJs(Path)}, each as a reactor-root-relative path. */
	private static final List<String> SHIPPED_JS_FILES = List.of(
		VIEWS_JS_DIR + "/juneau-config.js",
		VIEWS_JS_DIR + "/juneau-icons.js",
		VIEWS_JS_DIR + "/juneau-renders.js",
		// The DataRegistry runtime moves row data between cards, so it must stay sink-free.
		VIEWS_JS_DIR + "/juneau-data.js",
		VIEWS_JS_DIR + "/juneau-views.js",
		VIEWS_JS_DIR + "/juneau-ribbon.js",
		VIEWS_JS_DIR + "/juneau-helpers.js",
		// The REGION RUNTIME.  Omitted until the region emitter landed, which is backwards: this is the one shipped asset that
		// takes an author-supplied populator NAME off a DOM attribute and hands a container to author-registered
		// populate functions, so it is the file where an HTML sink would matter most.  It
		// has zero sinks today - every state message goes out through renderAsyncStatus's textContent - and listing
		// it here is what keeps that true rather than merely currently-so.
		VIEWS_JS_DIR + "/juneau-regions.js",
		// The run-view module renders untrusted report text (test names, messages, traces), so it must stay sink-free.
		VIEWS_JS_DIR + "/juneau-run-view.js",
		WIDGETS_JS_DIR + "/juneau-calendar.js",
		CONSOLE_JS_DIR + "/juneau-console.js",
		// The chart-card runtime renders row data (legend labels, table cells, aria labels), so it must stay sink-free.
		CONSOLE_JS_DIR + "/juneau-charts.js",
		// The job-log runtime renders server-supplied progress lines and outcome messages, so it must stay sink-free.
		CONSOLE_JS_DIR + "/juneau-joblog.js",
		// The badge runtime renders row/status text into badge nodes, so it must stay sink-free.
		CONSOLE_JS_DIR + "/juneau-badges.js",
		// The dashboard runtime renders share-link and saved-state values and data-source dict values, so it must stay sink-free.
		CONSOLE_JS_DIR + "/juneau-dashboard.js",
		VIEWS_JS_DIR + "/juneau-bus.js",
		VIEWS_JS_DIR + "/juneau-pagestate.js",
		VIEWS_JS_DIR + "/juneau-search.js",
		VIEWS_JS_DIR + "/juneau-urlstate.js",
		// The terminal renders untrusted process output, so it must stay sink-free.
		TERMINAL_JS_DIR + "/juneau-terminal.js",
		DATATABLES_JS_DIR + "/juneau-datatables.js"
	);

	/**
	 * An audited first-party sink.
	 *
	 * @param relativePath The scanned file.
	 * @param snippet The unique assignment snippet.
	 * @param occurrences How many times the snippet appears in the file; the test pins this so a copy-pasted extra
	 * 	site cannot hide behind an existing entry.
	 */
	record AllowedJsSink(String relativePath, String snippet, int occurrences) {
		AllowedJsSink(String relativePath, String snippet) {
			this(relativePath, snippet, 1);
		}
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
			sinks.add(new Sink(file, snippet));
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
	 * Walks the shipped {@code juneau-*.js} assets.  Hits are violations unless the unique snippet is on
	 * the first-party icon-registry allowlist.
	 *
	 * @param moduleRoot The {@code juneau-rest-server-views} module root.
	 * @return Aggregated sinks and violations, plus the allowlisted-hit count in {@link Result#sinks()}.
	 * @throws IOException If an asset cannot be read.
	 */
	static Result scanShippedJs(Path moduleRoot) throws IOException {
		var sinks = new ArrayList<Sink>();
		var violations = new ArrayList<String>();
		for (var rel : SHIPPED_JS_FILES) {
			var file = moduleRoot.resolve(rel.replace('/', File.separatorChar));
			var allowed = new HashSet<String>();
			for (var a : SHIPPED_JS_ALLOWLIST)
				if (a.relativePath().equals(rel))
					allowed.add(a.snippet());
			var r = scanJsHtmlSinks(rel, Files.readString(file), allowed);
			sinks.addAll(r.sinks());
			violations.addAll(r.violations());
		}
		return new Result(sinks, violations);
	}

	/** Allowlisted snippets that must still exist in their files (anti-vacuous). */
	static List<AllowedJsSink> shippedJsAllowlist() {
		return SHIPPED_JS_ALLOWLIST;
	}

	/** The shipped JS files {@link #scanShippedJs} covers, relative to this module's root. */
	static List<String> shippedJsFiles() {
		return SHIPPED_JS_FILES;
	}

	/**
	 * Finds every {@code juneau-*.js} asset under the {@code src/main/resources} of this module and its sibling
	 * {@code juneau-rest-server-*} modules, as module-root-relative paths.  Compared against {@link #shippedJsFiles()}
	 * so a newly shipped script cannot silently escape the scan.
	 *
	 * @param moduleRoot The {@code juneau-rest-server-views} module root.
	 * @return The sorted relative paths, using {@code /} separators.
	 * @throws IOException If a directory cannot be walked.
	 */
	static List<String> discoverShippedJs(Path moduleRoot) throws IOException {
		var found = new ArrayList<String>();
		try (var siblings = Files.list(moduleRoot.resolveSibling(""))) {
			for (var module : (Iterable<Path>)siblings.filter(p -> p.getFileName().toString().startsWith("juneau-rest-server"))::iterator) {
				var res = module.resolve("src/main/resources");
				if (! Files.isDirectory(res))
					continue;
				try (var walk = Files.walk(res)) {
					for (var f : (Iterable<Path>)walk.filter(p -> p.getFileName().toString().matches("juneau-.*\\.js"))::iterator)
						found.add(moduleRoot.relativize(f).toString().replace(File.separatorChar, '/'));
				}
			}
		}
		found.sort(null);
		return found;
	}

	//------------------------------------------------------------------------------------------------------------------
	// rawText(...) emitter-purity scanning: what an emitter may hand to the verbatim writer.
	//
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Argument identifiers that are provably a JSON <b>sidecar payload</b> rather than content: the local holding a
	 * serialized descriptor, immediately before it is written into a {@code <script type="application/json">}.
	 *
	 * <p>
	 * The other sidecar shape is recognized structurally instead of by name &mdash; any argument beginning
	 * {@code escapeForScript(} is a sidecar by construction, since that helper exists only to make a JSON payload
	 * safe for a {@code <script>} body.
	 */
	private static final Set<String> SIDECAR_PAYLOAD_ARGS = Set.of("json", "bulkJson");

	/**
	 * One {@code rawText(...)} call site, classified.
	 *
	 * @param file The simple file name (e.g. {@code ViewTable.java}).
	 * @param line The 1-based line number of the call.
	 * @param arg The argument source text, verbatim.
	 */
	record RawTextSite(String file, int line, String arg) {

		/** Stable identity of this sink for allowlisting: file plus argument shape, independent of line number. */
		String key() {
			return file + ":" + arg;
		}

		/** True if the argument is a serialized JSON sidecar descriptor. */
		boolean isSidecarPayload() {
			return SIDECAR_PAYLOAD_ARGS.contains(arg) || arg.startsWith("escapeForScript(");
		}

		/** True if the argument is the empty string literal &mdash; a structural placeholder carrying nothing. */
		boolean isEmptyLiteral() {
			return eq(arg, "\"\"");
		}

		/** True if this site writes something that is neither a sidecar payload nor provably empty. */
		boolean isContentBearing() {
			return !isSidecarPayload() && !isEmptyLiteral();
		}

		@Override
		public String toString() {
			return file + ":" + line + " -> rawText(" + arg + ")";
		}
	}

	/**
	 * Scans a single Java source string for {@code rawText(...)} call sites.
	 *
	 * @param file A label for the source (used in messages).
	 * @param source The Java source text.
	 * @return Every {@code rawText(...)} site found, in source order.
	 */
	static List<RawTextSite> scanRawText(String file, String source) {
		var out = new ArrayList<RawTextSite>();
		var code = stripComments(source);
		var m = RAW_TEXT_CALL.matcher(code);
		while (m.find()) {
			var openParen = m.end() - 1;
			var arg = balancedArg(code, openParen).trim();
			var line = 1 + (int) code.substring(0, m.start()).chars().filter(c -> c == '\n').count();
			out.add(new RawTextSite(file, line, arg));
		}
		return out;
	}

	/**
	 * Walks the specified module root <b>and</b> the sibling widgets module root, scanning every
	 * {@code src/main/java} source under each for {@code rawText(...)} sites.
	 *
	 * <p>
	 * <b>Main sources only</b>, deliberately, and the claim under test is about what the
	 * shipped emitters do.  A test fixture that hands live-looking data to {@code rawText} is exercising the
	 * emitter, not violating the contract.
	 *
	 * @param moduleRoot The views module root.
	 * @return Every site found across both modules' main trees, in stable (per-tree, sorted-by-path) order.
	 * @throws IOException If either tree cannot be walked.
	 */
	static List<RawTextSite> scanRawTextTree(Path moduleRoot) throws IOException {
		var out = new ArrayList<RawTextSite>();
		collectRawText(moduleRoot, out);
		collectRawText(moduleRoot.resolve(WIDGETS_MODULE_DIR), out);
		return out;
	}

	private static void collectRawText(Path moduleRoot, List<RawTextSite> out) throws IOException {
		var target = File.separator + "target" + File.separator;
		var srcRoot = moduleRoot.resolve("src").resolve("main").resolve("java");
		if (!Files.isDirectory(srcRoot))
			return;
		try (var stream = Files.walk(srcRoot)) {
			var files = stream
				.filter(p -> p.toString().endsWith(".java"))
				.filter(p -> !p.toString().contains(target))
				.sorted()
				.toList();
			for (var f : files)
				out.addAll(scanRawText(f.getFileName().toString(), Files.readString(f)));
		}
	}

	/**
	 * Resolves the sibling widgets module root, so a test can assert the cross-module crossing actually happened.
	 *
	 * @param moduleRoot The views module root.
	 * @return The widgets module root (which may not exist &mdash; that is the thing worth asserting).
	 */
	static Path widgetsModuleRoot(Path moduleRoot) {
		return moduleRoot.resolve(WIDGETS_MODULE_DIR);
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
	 * inner text) so that a mention of a forbidden API inside a comment cannot be mistaken for a real call, and the
	 * literal text of a string argument stays inspectable. This is the one deliberate divergence from
	 * {@code ScriptJsonSinkScanner}'s stripper, which blanks string content too.
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
