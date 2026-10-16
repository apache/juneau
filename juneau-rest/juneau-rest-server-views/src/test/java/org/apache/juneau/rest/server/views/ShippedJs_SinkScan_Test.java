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

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Runs {@link RawContentSinkScanner#scanShippedJs(Path)} over every shipped {@code juneau-*.js} asset: no raw
 * HTML-sink assignment ({@code innerHTML}, {@code outerHTML}, {@code insertAdjacentHTML}, {@code document.write},
 * jQuery {@code .html(}) may appear unless it is on the audited allowlist (first-party icon-registry markup and
 * author-trusted page-contract markup).  Also fails if a shipped script exists on disk but is missing from the
 * scanner's list, so the list cannot silently go stale.
 *
 * <p>
 * Deliberately outside the scan ({@link #EXCLUDED_SCRIPTS}): the petstore example app's {@code petstore.js} and
 * {@code dev/secure.js}, the legacy {@code juneau-rest-server} {@code htdocs/scripts/MenuItemWidget.js}, and the vendor
 * ECharts WebJar asset {@code echarts.min.js} ({@code org.webjars.npm:echarts}, served by the {@code echarts} toolkit
 * pack).  They are example-app, legacy and third-party scripts, not shipped console JS, and the discovery in a03 only
 * walks {@code juneau-*.js}.
 */
class ShippedJs_SinkScan_Test extends TestBase {

	/** Files that must stay completely sink-free: no hit at all, and never an allowlist entry. */
	private static final List<String> ZERO_SINK_FILES = List.of(
		"src/main/resources/org/apache/juneau/views/juneau-run-view.js",
		"../juneau-rest-server-console-ui/src/main/resources/org/apache/juneau/console/juneau-charts.js",
		"../juneau-rest-server-console-ui/src/main/resources/org/apache/juneau/console/juneau-joblog.js",
		"../juneau-rest-server-console-ui/src/main/resources/org/apache/juneau/console/juneau-badges.js",
		"../juneau-rest-server-console-ui/src/main/resources/org/apache/juneau/console/juneau-dashboard.js",
		"../juneau-rest-server-terminal/src/main/resources/org/apache/juneau/terminal/juneau-terminal.js",
		"src/main/resources/org/apache/juneau/views/juneau-regions.js",
		"src/main/resources/org/apache/juneau/views/juneau-renders.js",
		"src/main/resources/org/apache/juneau/views/juneau-data.js");

	/** Example-app and legacy scripts that are intentionally not scanned (see the class javadoc). */
	private static final List<String> EXCLUDED_SCRIPTS = List.of("petstore.js", "secure.js", "MenuItemWidget.js", "echarts.min.js");

	private static final String VIEWS_JS = "src/main/resources/org/apache/juneau/views/juneau-views.js";

	private static Path root() {
		var root = RawContentSinkScanner.locateModuleRoot();
		assertNotNull(root, "could not locate the juneau-rest-server-views module root");
		return root;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a: Live scan of the shipped files
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_shippedJs_onlyAllowlistedHits() throws Exception {
		var r = RawContentSinkScanner.scanShippedJs(root());
		assertEquals(List.of(), r.violations(),
			() -> "shipped JS HTML-sink violations:\n  " + String.join("\n  ", r.violations()));
		var expected = RawContentSinkScanner.shippedJsAllowlist().stream().mapToInt(a -> a.occurrences()).sum();
		assertEquals(expected, r.sinks().size(),
			() -> "every allowlisted snippet must be hit, and nothing else: " + r.sinks());
	}

	@Test void a02_allowlistEntries_areScannedAndMatchTheirPinnedCount() throws Exception {
		var r = RawContentSinkScanner.scanShippedJs(root());
		var listed = RawContentSinkScanner.shippedJsFiles();
		for (var a : RawContentSinkScanner.shippedJsAllowlist()) {
			assertTrue(listed.contains(a.relativePath()), () -> "allowlisted file not scanned: " + a.relativePath());
			var hits = r.sinks().stream()
				.filter(x -> x.file().equals(a.relativePath()) && x.detail().equals(a.snippet()))
				.count();
			assertEquals(a.occurrences(), hits, () -> "allowlisted snippet must match its pinned occurrence count in " + a.relativePath() + ": " + a.snippet());
		}
	}

	@Test void a03_everyShippedJsFile_isOnTheScanList() throws Exception {
		var onDisk = new TreeSet<>(RawContentSinkScanner.discoverShippedJs(root()));
		var listed = new TreeSet<>(RawContentSinkScanner.shippedJsFiles());
		assertFalse(onDisk.isEmpty(), "discovered no shipped JS files; the walk is vacuous");
		var missing = new TreeSet<>(onDisk);
		missing.removeAll(listed);
		assertEquals(Set.of(), missing, () -> "shipped JS files not on RawContentSinkScanner.SHIPPED_JS_FILES: " + missing);
		var stale = new TreeSet<>(listed);
		stale.removeAll(onDisk);
		assertEquals(Set.of(), stale, () -> "SHIPPED_JS_FILES entries with no file on disk: " + stale);
	}

	@Test void a04_zeroSinkFiles_haveNoHitsAndNoAllowlistEntry() throws Exception {
		var root = root();
		for (var rel : ZERO_SINK_FILES) {
			assertTrue(RawContentSinkScanner.shippedJsFiles().contains(rel), () -> "not on the scan list: " + rel);
			var hits = RawContentSinkScanner.scanJsHtmlSinks(rel, Files.readString(root.resolve(rel))).sinks();
			assertEquals(List.of(), hits, () -> rel + " must have zero HTML-sink hits: " + hits);
			assertTrue(RawContentSinkScanner.shippedJsAllowlist().stream().noneMatch(a -> a.relativePath().equals(rel)),
				() -> rel + " must have no allowlist entry");
		}
	}

	@Test void a05_excludedScripts_areNotOnTheScanList() {
		for (var f : RawContentSinkScanner.shippedJsFiles())
			for (var excluded : EXCLUDED_SCRIPTS)
				assertFalse(f.endsWith("/" + excluded), () -> excluded + " is documented as excluded but is on the scan list: " + f);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b: Scanner mechanism (RED/GREEN on fixture strings)
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@ValueSource(strings = {
		"el.innerHTML = x;",
		"el.outerHTML = x;",
		"el.insertAdjacentHTML('beforeend', x);",
		"document.write(x);",
		"$(el).html(x);"
	})
	void b01_eachSinkPattern_isFlagged(String src) {
		var r = RawContentSinkScanner.scanJsHtmlSinks("x.js", src);
		assertEquals(1, r.violations().size(), () -> "violations: " + r.violations());
		assertEquals(1, r.sinks().size());
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"el.textContent = x;",
		"input.value = x;",
		"// el.innerHTML = x;",
		"/* el.insertAdjacentHTML('a', x); document.write(x); */"
	})
	void b02_safeWritesAndCommentMentions_areNotFlagged(String src) {
		var r = RawContentSinkScanner.scanJsHtmlSinks("x.js", src);
		assertEquals(List.of(), r.sinks());
		assertEquals(List.of(), r.violations());
	}

	@Test void b03_allowlistedSnippet_passesOnlyOnExactMatch() {
		var allowed = Set.of("el.innerHTML = markup;");
		var ok = RawContentSinkScanner.scanJsHtmlSinks("x.js", "el.innerHTML = markup;", allowed);
		assertEquals(List.of(), ok.violations());
		assertEquals(1, ok.sinks().size());
		var other = RawContentSinkScanner.scanJsHtmlSinks("x.js", "el.innerHTML = rowText;", allowed);
		assertEquals(1, other.violations().size(), () -> "violations: " + other.violations());
		var otherTarget = RawContentSinkScanner.scanJsHtmlSinks("x.js", "other.innerHTML = markup;", allowed);
		assertEquals(1, otherTarget.violations().size(), () -> "violations: " + otherTarget.violations());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c: Structural pins on the sanitized-HTML painter in juneau-views.js
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * The attribute copier is an ALLOWLIST: it reaches {@code setAttribute} only for names it spells out literally, so an
	 * {@code on*} handler cannot ride along on an otherwise-allowed tag.  A refactor to a deny-list (copy everything,
	 * then remove {@code on*}) would be materially weaker even if the vector suite still passed.
	 */
	@Test void c01_sanitizedHtmlAttrCopier_isAnAllowlist_notADenylist() throws Exception {
		var source = Files.readString(root().resolve(VIEWS_JS));
		var attrs = extractFunction(source, "function copyAllowedSanitizedHtmlAttrs(");
		assertTrue(attrs.contains("isSafeMarkdownHref"), "href must stay scheme-checked: " + attrs);
		assertTrue(attrs.contains("isSafeImageSrc"), "img src must stay scheme-checked: " + attrs);
		assertFalse(attrs.contains("attributes"), "must not enumerate the source node's attributes: " + attrs);
		assertFalse(attrs.contains("removeAttribute"), "a deny-list shape (copy-then-remove) is not allowed: " + attrs);
		assertFalse(attrs.contains("setAttribute(name"), "setAttribute must take a literal name: " + attrs);
	}

	@Test void c02_isSafeImageSrc_rejectsScriptAndDataSchemes() throws Exception {
		var source = Files.readString(root().resolve(VIEWS_JS));
		var fn = extractFunction(source, "function isSafeImageSrc(");
		assertTrue(fn.contains("javascript:") && fn.contains("data:") && fn.contains("vbscript:"), fn);
	}

	private static String extractFunction(String body, String signature) {
		var start = body.indexOf(signature);
		assertTrue(start >= 0, () -> "'" + signature + "' not found");
		var j = body.indexOf('{', start);
		var depth = 0;
		for (; j < body.length(); j++) {
			var c = body.charAt(j);
			if (c == '{')
				depth++;
			else if (c == '}') {
				depth--;
				if (depth == 0) { j++; break; }
			} else if (c == '"' || c == '\'' || c == '`') {
				var quote = c;
				j++;
				while (j < body.length() && body.charAt(j) != quote) {
					if (body.charAt(j) == '\\')
						j++;
					j++;
				}
			}
		}
		return body.substring(start, j);
	}
}
