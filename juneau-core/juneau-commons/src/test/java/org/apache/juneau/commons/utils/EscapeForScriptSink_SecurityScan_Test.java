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

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * TODO-431 Phase 3 gate: the framework's {@code <script>}-JSON sinks must route through
 * {@link StringUtils#escapeForScript(String)}.
 *
 * <p>
 * Pairs a set of unit RED/GREEN checks on {@link ScriptJsonSinkScanner} (proving it flags an unescaped sink, passes
 * an escaped one, ignores non-{@code <script>} raw text, and is not fooled by a comment mention of the escaper) with
 * a live scan of the real source tree that both asserts zero violations <i>and</i> asserts the scanner still finds
 * the known-good {@code ViewTable}/{@code PageTable} sinks &mdash; so a green result can never mean "nothing was
 * examined".
 */
class EscapeForScriptSink_SecurityScan_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------
	// Unit RED/GREEN on the scanner itself
	// -----------------------------------------------------------------------------------------------------------

	/** RED: a serialized payload written into a raw-text {@code <script>} without escapeForScript is a violation. */
	@Test void a01_unescapedScriptRawText_isFlagged() {
		var src = """
			class X {
			  Object of(Object bean) {
			    var json = Json.of(bean);
			    return script().type("application/json").text(rawText(json));
			  }
			}
			""";
		var r = ScriptJsonSinkScanner.scan(src);
		assertEquals(1, r.sinks().size(), () -> "sinks: " + r.sinks());
		assertEquals(1, r.violations().size(), () -> "violations: " + r.violations());
		assertFalse(r.sinks().get(0).escaped());
	}

	/** GREEN: the ViewTable/PageTable shape - escaped via a same-source variable assignment - passes. */
	@Test void a02_escapedViaVariable_passes() {
		var src = """
			class X {
			  Object of(Object bean) {
			    var json = escapeForScript(Json.of(bean));
			    return script().type("application/json").text(rawText(json));
			  }
			}
			""";
		var r = ScriptJsonSinkScanner.scan(src);
		assertEquals(1, r.sinks().size(), () -> "sinks: " + r.sinks());
		assertEquals(java.util.List.of(), r.violations());
		assertTrue(r.sinks().get(0).escaped());
	}

	/** GREEN: inline escapeForScript(...) directly inside rawText(...) passes. */
	@Test void a03_escapedInline_passes() {
		var src = """
			class X {
			  Object of(Object bean) {
			    return script().text(rawText(escapeForScript(Json.of(bean))));
			  }
			}
			""";
		var r = ScriptJsonSinkScanner.scan(src);
		assertEquals(1, r.sinks().size(), () -> "sinks: " + r.sinks());
		assertEquals(java.util.List.of(), r.violations());
	}

	/** A raw-text write that is not into a {@code <script>} element is not a sink (JSON-in/JSON-out scope, not widened). */
	@Test void a04_nonScriptRawText_isNotASink() {
		var src = """
			class X {
			  Object of(Object html) {
			    return div().text(rawText(html));
			  }
			}
			""";
		var r = ScriptJsonSinkScanner.scan(src);
		assertEquals(java.util.List.of(), r.sinks(), () -> "sinks: " + r.sinks());
		assertEquals(java.util.List.of(), r.violations());
	}

	/**
	 * A {@code escapeForScript} mention that lives only in a comment must NOT satisfy the guard - this is the exact
	 * silent-decay failure mode the scanner exists to prevent, so it is asserted directly.
	 */
	@Test void a05_commentMentionOfEscaper_doesNotSatisfyGuard() {
		var src = """
			class X {
			  // We used to call escapeForScript(...) here; the code below no longer does.
			  Object of(Object bean) {
			    var json = Json.of(bean); // escapeForScript would go here
			    return script().type("application/json").text(rawText(json));
			  }
			}
			""";
		var r = ScriptJsonSinkScanner.scan(src);
		assertEquals(1, r.sinks().size(), () -> "sinks: " + r.sinks());
		assertEquals(1, r.violations().size(), () -> "comment mention must not satisfy the guard: " + r.violations());
	}

	/** {@code new RawText(...)} is recognized as the same raw-text primitive as {@code rawText(...)}. */
	@Test void a06_newRawTextConstructor_isAlsoASink() {
		var escaped = ScriptJsonSinkScanner.scan("""
			class X { Object of(Object b) { return script().text(new RawText(escapeForScript(Json.of(b)))); } }
			""");
		assertEquals(1, escaped.sinks().size());
		assertEquals(java.util.List.of(), escaped.violations());

		var unescaped = ScriptJsonSinkScanner.scan("""
			class X { Object of(Object b) { return script().text(new RawText(Json.of(b))); } }
			""");
		assertEquals(1, unescaped.sinks().size());
		assertEquals(1, unescaped.violations().size());
	}

	// -----------------------------------------------------------------------------------------------------------
	// Live gate against the real framework tree
	// -----------------------------------------------------------------------------------------------------------

	private static final String VIEW_TABLE = "juneau-rest/juneau-rest-server-views/src/main/java/org/apache/juneau/rest/server/views/ViewTable.java";
	private static final String PAGE_TABLE = "juneau-rest/juneau-rest-server-views/src/main/java/org/apache/juneau/rest/server/views/PageTable.java";

	/** GREEN: every {@code <script>}-JSON sink in the real framework tree routes through escapeForScript. */
	@Test void a10_realTree_hasNoViolations() throws Exception {
		var root = requireRepoRoot();
		var r = ScriptJsonSinkScanner.scanTree(root);
		assertEquals(java.util.List.of(), r.violations(),
			() -> "framework <script> JSON sinks missing escapeForScript:\n  " + String.join("\n  ", r.violations()));
	}

	/**
	 * Anti-vacuous: the scan must actually be finding the real, known-good sinks. If this ever drops to zero the
	 * "no violations" result above is meaningless - the scanner would have quietly stopped matching.
	 */
	@Test void a11_realTree_findsKnownSinks_notVacuous() throws Exception {
		var root = requireRepoRoot();
		var r = ScriptJsonSinkScanner.scanTree(root);
		assertTrue(r.sinks().size() >= 2, () -> "expected >=2 framework <script> sinks, found: " + r.sinks());
		assertTrue(r.sinks().stream().anyMatch(s -> s.file().replace('\\', '/').equals(VIEW_TABLE)),
			() -> "ViewTable sink not found; scanner may have stopped matching. sinks: " + r.sinks());
		assertTrue(r.sinks().stream().anyMatch(s -> s.file().replace('\\', '/').equals(PAGE_TABLE)),
			() -> "PageTable sink not found; scanner may have stopped matching. sinks: " + r.sinks());
	}

	/**
	 * Mutation check: the real {@code ViewTable} passes as-is, but removing the escaper turns it into a violation.
	 * This proves the guard is genuinely exercising that sink rather than passing it by accident.
	 *
	 * <p>
	 * {@code ViewTable} currently has three escaped {@code <script>}-JSON sinks: the top-level VIEW_META sidecar,
	 * the independently versioned {@code BulkMutateDef} sidecar, and the nested-table VIEW_META sidecar emitted
	 * inside a row-detail {@code <template>}. The blanket {@code escapeForScript}&rarr;{@code noEscape} mutation
	 * (which hits every occurrence in the file) must flag all three.
	 */
	@Test void a12_realViewTable_passesButFailsWhenEscaperRemoved() throws Exception {
		var root = requireRepoRoot();
		var source = Files.readString(root.resolve(VIEW_TABLE));

		var clean = ScriptJsonSinkScanner.scan(VIEW_TABLE, source);
		assertEquals(3, clean.sinks().size(), () -> "sinks: " + clean.sinks());
		assertEquals(java.util.List.of(), clean.violations());

		var mutated = ScriptJsonSinkScanner.scan(VIEW_TABLE, source.replace("escapeForScript", "noEscape"));
		assertEquals(3, mutated.sinks().size(), () -> "sinks: " + mutated.sinks());
		assertEquals(3, mutated.violations().size(),
			() -> "removing escapeForScript from ViewTable should be flagged: " + mutated.violations());
	}

	private static Path requireRepoRoot() {
		var root = ScriptJsonSinkScanner.locateRepoRoot();
		assertNotNull(root, "could not locate the Juneau repo root from the working directory - the security gate "
			+ "must fail loudly rather than pass vacuously when it cannot find the sources it guards");
		return root;
	}
}
