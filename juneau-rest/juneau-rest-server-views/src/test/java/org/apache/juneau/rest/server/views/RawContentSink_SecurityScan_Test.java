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

/**
 * TODO-420 BLK-1 gate: no source in this module may pour a non-literal (i.e. plausibly live-data-derived) value
 * into {@link Tab#content}/{@link Subtab#content} &mdash; the raw-markup panel-body sink.
 *
 * <p>
 * This is the "guardrail with teeth" the item's cross-family adversarial review required before the raw-markup
 * shape could go READY: {@link Tab#content}/{@link Subtab#content} are a documented "template engine, trusted /
 * first-party content only" sink (caller sanitizes, framework emits verbatim - see their javadoc). A write-path
 * confirmation/detail body built from live data (an incident title, a Slack thread, a request parameter) must
 * never reach it. Because this framework's own sources have no such confirmation emitters yet, this gate is
 * necessarily forward-looking: it is proven correct here by exhaustive RED/GREEN unit checks on the scanner
 * mechanism itself (section a), then pointed at this module's real tree (section b) to lock in that today's
 * sources - main <i>and</i> test - are clean, and to prove the live scan is not accidentally vacuous.
 */
class RawContentSink_SecurityScan_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------
	// a: Unit RED/GREEN on the scanner mechanism itself
	// -----------------------------------------------------------------------------------------------------------

	/** RED: a bare identifier argument - the classic "pour a variable into content" shape - is a violation. */
	@Test void a01_identifierArgument_isFlagged() {
		var src = """
			class X {
			  void f(String incidentTitle) {
			    Tab.create("t", "T").content(incidentTitle);
			  }
			}
			""";
		var r = RawContentSinkScanner.scan(src);
		assertEquals(1, r.sinks().size(), () -> "sinks: " + r.sinks());
		assertEquals(1, r.violations().size(), () -> "violations: " + r.violations());
		assertFalse(r.sinks().get(0).literalOnly());
	}

	/** RED: a method-call argument - the shape a confirmation/detail body renderer would use - is a violation. */
	@Test void a02_methodCallArgument_isFlagged() {
		var src = """
			class X {
			  void f(Object gack) {
			    Subtab.create("s", "S").content(renderConfirmationHtml(gack));
			  }
			}
			""";
		var r = RawContentSinkScanner.scan(src);
		assertEquals(1, r.sinks().size(), () -> "sinks: " + r.sinks());
		assertEquals(1, r.violations().size(), () -> "violations: " + r.violations());
	}

	/** RED: concatenating a literal with a non-literal is still a violation - the literal prefix/suffix doesn't launder it. */
	@Test void a03_concatenationWithNonLiteral_isFlagged() {
		var src = """
			class X {
			  void f(String title) {
			    Tab.create("t", "T").content("<p>" + title + "</p>");
			  }
			}
			""";
		var r = RawContentSinkScanner.scan(src);
		assertEquals(1, r.sinks().size(), () -> "sinks: " + r.sinks());
		assertEquals(1, r.violations().size(), () -> "violations: " + r.violations());
	}

	/** GREEN: a single string literal - the FG-2 docs-prose shape - passes. */
	@Test void a04_singleStringLiteral_passes() {
		var src = """
			class X {
			  void f() {
			    Tab.create("t", "T").content("<p>Hello, world.</p>");
			  }
			}
			""";
		var r = RawContentSinkScanner.scan(src);
		assertEquals(1, r.sinks().size(), () -> "sinks: " + r.sinks());
		assertEquals(List.of(), r.violations());
		assertTrue(r.sinks().get(0).literalOnly());
	}

	/** GREEN: a text-block literal passes (built via string concatenation here to avoid nested triple-quote escaping in this fixture). */
	@Test void a05_textBlockLiteral_passes() {
		var src = "class X {\n"
			+ "  void f() {\n"
			+ "    Tab.create(\"t\", \"T\").content(\"\"\"\n"
			+ "      <p>Hello</p>\n"
			+ "      \"\"\");\n"
			+ "  }\n"
			+ "}\n";
		var r = RawContentSinkScanner.scan(src);
		assertEquals(1, r.sinks().size(), () -> "sinks: " + r.sinks());
		assertEquals(List.of(), r.violations());
		assertTrue(r.sinks().get(0).literalOnly());
	}

	/** GREEN: concatenating two-or-more literals together is still all-literal, so it passes. */
	@Test void a06_concatenationOfOnlyLiterals_passes() {
		var src = """
			class X {
			  void f() {
			    Tab.create("t", "T").content("<p>" + "Hello" + "</p>");
			  }
			}
			""";
		var r = RawContentSinkScanner.scan(src);
		assertEquals(1, r.sinks().size(), () -> "sinks: " + r.sinks());
		assertEquals(List.of(), r.violations());
	}

	/**
	 * A {@code .content(} mention that lives only in a comment must NOT be mistaken for a real call - this is the
	 * exact silent-decay failure mode a scanner exists to prevent, so it is asserted directly (mirrors
	 * {@code ScriptJsonSinkScanner}'s equivalent check).
	 */
	@Test void a07_commentMentionOfContentCall_isNotASink() {
		var src = """
			class X {
			  // old code used to do: Tab.create("t", "T").content(dynamicTitle);
			  void f() {
			    Tab.create("t", "T").content("<p>Hello.</p>");
			  }
			}
			""";
		var r = RawContentSinkScanner.scan(src);
		assertEquals(1, r.sinks().size(), () -> "sinks: " + r.sinks());
		assertEquals(List.of(), r.violations());
	}

	/** {@code null} is treated as a safe (non-data-carrying) argument, not a violation. */
	@Test void a08_nullArgument_isNotFlagged() {
		var src = """
			class X {
			  void f() {
			    Subtab.create("s", "S").content(null);
			  }
			}
			""";
		var r = RawContentSinkScanner.scan(src);
		assertEquals(1, r.sinks().size(), () -> "sinks: " + r.sinks());
		assertEquals(List.of(), r.violations());
	}

	/** Mutation check on a synthetic fixture: a GREEN literal call, once mutated to a non-literal, is flagged. */
	@Test void a09_mutatingAGreenLiteralIntoAnIdentifier_turnsItIntoAViolation() {
		var clean = "class X { void f() { Tab.create(\"t\", \"T\").content(\"<p>Hello.</p>\"); } }";
		var cleanResult = RawContentSinkScanner.scan(clean);
		assertEquals(List.of(), cleanResult.violations());

		var mutated = clean.replace("\"<p>Hello.</p>\"", "buildLiveConfirmationBody(incident)");
		var mutatedResult = RawContentSinkScanner.scan(mutated);
		assertEquals(1, mutatedResult.violations().size(),
			() -> "mutating a literal content(...) argument into a method call must be flagged: " + mutatedResult.violations());
	}

	// -----------------------------------------------------------------------------------------------------------
	// b: Live gate against this module's real tree (main AND test sources)
	// -----------------------------------------------------------------------------------------------------------

	private static Path requireModuleRoot() {
		var root = RawContentSinkScanner.locateModuleRoot();
		assertNotNull(root, "could not locate the juneau-rest-server-views module root from the working "
			+ "directory - the security gate must fail loudly rather than pass vacuously when it cannot find the "
			+ "sources it guards");
		return root;
	}

	/** GREEN: this module's real tree - main and test sources alike - has no content(...) sink fed by a non-literal. */
	@Test void b01_realModuleTree_hasNoViolations() throws Exception {
		var r = RawContentSinkScanner.scanTree(requireModuleRoot());
		assertEquals(List.of(), r.violations(),
			() -> "non-literal content(...) sink(s) found - a write-path confirmation/detail emitter may have "
				+ "reached the raw-markup panel-content sink:\n  " + String.join("\n  ", r.violations()));
	}

	/**
	 * Anti-vacuous: the live scan must actually be finding real content(...) call sites (this item's own
	 * legitimate literal usages in {@code TabSubtabContent_Validation_Test}/{@code PageTableContent_Test}). If
	 * this ever drops to zero, the "no violations" result above is meaningless - the scanner would have quietly
	 * stopped matching, or every real usage would have been (wrongly) excluded from the tree walk.
	 */
	@Test void b02_realModuleTree_findsKnownGoodSinks_notVacuous() throws Exception {
		var r = RawContentSinkScanner.scanTree(requireModuleRoot());
		assertTrue(r.sinks().size() >= 10, () -> "expected >=10 real content(...) call sites, found: " + r.sinks());
		assertTrue(r.sinks().stream().allMatch(RawContentSinkScanner.Sink::literalOnly),
			() -> "expected every real sink found today to be literal-only: " + r.sinks());
	}

	/**
	 * Mutation check against a real file in the tree: a known-good literal {@code content(...)} call in
	 * {@code TabSubtabContent_Validation_Test} passes as-is, but mutating that exact literal into a plausible
	 * live-data expression turns it into a violation - proving the guard genuinely exercises real files rather
	 * than passing them by accident.
	 */
	@Test void b03_realKnownGoodCall_passesButFailsWhenMutatedToNonLiteral() throws Exception {
		var root = requireModuleRoot();
		var file = "src/test/java/org/apache/juneau/rest/server/views/TabSubtabContent_Validation_Test.java";
		var source = Files.readString(root.resolve(file));
		var knownGoodCall = "Tab.create(\"overview\", \"Overview\").content(\"<p>Hello, world.</p>\")";
		assertTrue(source.contains(knownGoodCall), () -> file + " no longer contains the expected known-good call "
			+ "- update this test's fixture string to match the current source");

		var clean = RawContentSinkScanner.scan(file, source);
		assertEquals(List.of(), clean.violations());

		var mutated = source.replace(knownGoodCall,
			"Tab.create(\"overview\", \"Overview\").content(buildLiveConfirmationBody(incident))");
		var mutatedResult = RawContentSinkScanner.scan(file, mutated);
		assertTrue(mutatedResult.violations().size() >= 1,
			() -> "mutating the known-good call into a method-call argument must be flagged: " + mutatedResult.violations());
	}

	/** The scanner's own accompanying fixture file is excluded from the tree walk - sanity-check that exclusion. */
	@Test void b04_scannersOwnFixtureFile_isExcludedFromTheTreeWalk() throws Exception {
		var r = RawContentSinkScanner.scanTree(requireModuleRoot());
		assertTrue(r.sinks().stream().noneMatch(s -> s.file().endsWith("RawContentSink_SecurityScan_Test.java")),
			() -> "the scanner's own RED/GREEN fixture file must be excluded from the tree walk (its example "
				+ "strings would otherwise register as false-positive sinks): " + r.sinks());
	}

	// -----------------------------------------------------------------------------------------------------------
	// c: juneau-config.js HTML-sink gate (innerHTML = / .html() never in the chooser)
	// -----------------------------------------------------------------------------------------------------------

	@Test void c01_innerHtmlAssignment_isFlagged() {
		var r = RawContentSinkScanner.scanJsHtmlSinks("x.js", "el.innerHTML = name;");
		assertEquals(1, r.sinks().size(), () -> "sinks: " + r.sinks());
		assertEquals(1, r.violations().size(), () -> "violations: " + r.violations());
	}

	@Test void c02_jqueryHtmlCall_isFlagged() {
		var r = RawContentSinkScanner.scanJsHtmlSinks("x.js", "$(el).html(name);");
		assertEquals(1, r.sinks().size(), () -> "sinks: " + r.sinks());
		assertEquals(1, r.violations().size(), () -> "violations: " + r.violations());
	}

	@Test void c03_textContentAssignment_isNotFlagged() {
		var r = RawContentSinkScanner.scanJsHtmlSinks("x.js", "el.textContent = name; inp.value = name;");
		assertEquals(List.of(), r.violations());
		assertEquals(List.of(), r.sinks());
	}

	@Test void c04_commentMentionOfInnerHtml_isNotASink() {
		var r = RawContentSinkScanner.scanJsHtmlSinks("x.js",
			"// never innerHTML / .html()\nel.textContent = name;\n");
		assertEquals(List.of(), r.violations());
		assertEquals(List.of(), r.sinks());
	}

	@Test void c05_shippedConfigJs_hasNoHtmlSinks() throws Exception {
		var r = RawContentSinkScanner.scanConfigJs(requireModuleRoot());
		assertEquals(List.of(), r.violations(),
			() -> "juneau-config.js must not assign innerHTML or call .html():\n  "
				+ String.join("\n  ", r.violations()));
	}

	@Test void c06_shippedConfigJs_hasTextContentBaseline_notVacuous() throws Exception {
		var root = requireModuleRoot();
		var rel = Path.of("src", "main", "resources", "org", "apache", "juneau", "views", "juneau-config.js");
		var source = Files.readString(root.resolve(rel));
		assertTrue(source.contains(".textContent"),
			"juneau-config.js must paint user strings with textContent (anti-vacuous baseline)");
		assertTrue(source.contains("paintUserText"), source);
		var r = RawContentSinkScanner.scanConfigJs(root);
		assertEquals(List.of(), r.sinks(),
			() -> "the HTML-sink scan of a known-good file must be empty, not merely 'no violations': " + r.sinks());
	}

	@Test void c07_mutatingTextContentIntoInnerHtml_isFlagged() throws Exception {
		var root = requireModuleRoot();
		var rel = Path.of("src", "main", "resources", "org", "apache", "juneau", "views", "juneau-config.js");
		var source = Files.readString(root.resolve(rel));
		assertTrue(source.contains("el.textContent = value == null ? \"\" : String(value);"),
			"paintUserText body moved - update this fixture");
		var clean = RawContentSinkScanner.scanJsHtmlSinks(rel.toString(), source);
		assertEquals(List.of(), clean.violations());
		var mutated = source.replace("el.textContent = value == null ? \"\" : String(value);",
			"el.innerHTML = value == null ? \"\" : String(value);");
		var mutatedResult = RawContentSinkScanner.scanJsHtmlSinks(rel.toString(), mutated);
		assertTrue(mutatedResult.violations().size() >= 1,
			() -> "mutating paintUserText to innerHTML must be flagged: " + mutatedResult.violations());
	}

	// -----------------------------------------------------------------------------------------------------------
	// d: row-detail template emitter + fillDetailSlots (textContent baseline + innerHTML fixture)
	// -----------------------------------------------------------------------------------------------------------

	@Test void d01_viewTableEmitter_hasNoInnerHtml_andEmitsTemplate() throws Exception {
		var root = requireModuleRoot();
		var rel = Path.of("src", "main", "java", "org", "apache", "juneau", "rest", "server", "views", "ViewTable.java");
		var source = Files.readString(root.resolve(rel));
		assertTrue(source.contains("DETAIL_TEMPLATE_ATTR"), source);
		assertTrue(source.contains("data-juneau-row-detail") || source.contains("DETAIL_TEMPLATE_ATTR"), source);
		assertFalse(source.contains("innerHTML"), source);
		assertTrue(source.contains("emitDetailTemplate"), source);
	}

	@Test void d02_fillDetailSlots_textContentBaseline_noHtmlSinks() throws Exception {
		var root = requireModuleRoot();
		var rel = Path.of("src", "main", "resources", "org", "apache", "juneau", "views", "juneau-views.js");
		var source = Files.readString(root.resolve(rel));
		var fn = extractFunction(source, "function fillDetailSlots(");
		assertTrue(fn.contains(".textContent"), fn);
		assertFalse(fn.contains("innerHTML"), fn);
		var r = RawContentSinkScanner.scanJsHtmlSinks(rel.toString(), fn);
		assertEquals(List.of(), r.violations(), () -> "fillDetailSlots must not assign innerHTML: " + r.violations());
		assertEquals(List.of(), r.sinks());
	}

	@Test void d03_mutatingFillDetailSlotsIntoInnerHtml_isFlagged() throws Exception {
		var root = requireModuleRoot();
		var rel = Path.of("src", "main", "resources", "org", "apache", "juneau", "views", "juneau-views.js");
		var source = Files.readString(root.resolve(rel));
		var needle = "slot.textContent = value;";
		assertTrue(source.contains(needle), "fillDetailSlots assignment moved - update this fixture");
		var fn = extractFunction(source, "function fillDetailSlots(");
		var clean = RawContentSinkScanner.scanJsHtmlSinks(rel.toString(), fn);
		assertEquals(List.of(), clean.violations());
		var mutated = fn.replace("slot.textContent", "slot.innerHTML");
		var mutatedResult = RawContentSinkScanner.scanJsHtmlSinks(rel.toString(), mutated);
		assertTrue(mutatedResult.violations().size() >= 1,
			() -> "mutating fillDetailSlots to innerHTML must be flagged: " + mutatedResult.violations());
	}

	@Test void d04_fillMarkdownSlot_hasNoInnerHtml() throws Exception {
		var root = requireModuleRoot();
		var rel = Path.of("src", "main", "resources", "org", "apache", "juneau", "views", "juneau-views.js");
		var source = Files.readString(root.resolve(rel));
		var fn = extractFunction(source, "function fillMarkdownSlot(");
		assertTrue(fn.contains("DOMParser"), fn);
		assertTrue(fn.contains("createElement"), fn);
		assertFalse(fn.contains("innerHTML"), fn);
		var r = RawContentSinkScanner.scanJsHtmlSinks(rel.toString(), fn);
		assertEquals(List.of(), r.violations(), () -> "fillMarkdownSlot must not assign innerHTML: " + r.violations());
	}

	private static String extractFunction(String body, String signature) {
		var start = body.indexOf(signature);
		assertTrue(start >= 0, () -> "'" + signature + "' not found");
		var i = body.indexOf('{', start);
		var depth = 0;
		var j = i;
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
