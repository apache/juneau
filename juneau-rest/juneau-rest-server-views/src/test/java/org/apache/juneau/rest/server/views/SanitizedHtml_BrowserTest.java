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

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * Opt-in Chromium XSS canary for {@link DetailField.Format#SANITIZED_HTML}.
 *
 * <p>
 * {@link ViewsJs_RowDetail_Test}'s {@code b17*} battery already proves the copier's behavior against this
 * module's own regex-fixture {@code DOMParser} shim ({@code row-detail.cjs}) &mdash; a hand-rolled test parser
 * that does not decode entities and does not do implicit tag nesting/foster-parenting the way a real browser
 * HTML parser does (see the fidelity note at the top of that file's SANITIZED_HTML section). This class proves
 * the SAME never-executes guarantee against a REAL browser HTML parser and a real DOM: a {@code <script>} and
 * an {@code <img onerror>} payload must not produce an executable node, and benign markup (a {@code <b>} and a
 * {@code <table>}) must still survive as real elements.
 *
 * <p>
 * Disabled unless the {@value #GATE} system property is set (the module's {@code js-tests} Maven profile).
 */
@EnabledIfSystemProperty(named=SanitizedHtml_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class SanitizedHtml_BrowserTest extends TestBase {

	/** System property the {@code js-tests} profile sets to enable this class. */
	static final String GATE = "juneau.jsTests";

	private static Map<?,?> report;

	private static String resource(String path) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	@BeforeAll
	static void probe() throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent().resolve("sanitized-html-browser.cjs");

		var fixture = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body>\n<script>\n"
			+ resource(ViewsMixin.VIEWS_JS_RESOURCE)
			+ "\n</script></body></html>";
		var fixtureFile = Files.createDirectories(dir.resolve("fixtures")).resolve("sanitized-html.html");
		Files.write(fixtureFile, fixture.getBytes(UTF_8));

		report = Json.to(run(dir, harness, fixtureFile), Map.class);
	}

	private static String requiredProperty(String name) {
		var v = System.getProperty(name);
		assertNotNull(v, () -> "-D" + name + " not set; the js-tests profile is responsible for providing it");
		return v;
	}

	private static String run(Path dir, Path harness, Path fixture) throws Exception {
		var cmd = List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(), fixture.toString());
		var stdout = dir.resolve("sanitized-html-stdout.json");
		var stderr = dir.resolve("sanitized-html-stderr.txt");
		var pb = new ProcessBuilder(cmd).redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
		pb.environment().put("NODE_PATH", dir.resolve("node_modules").toString());
		pb.environment().put("PLAYWRIGHT_BROWSERS_PATH", requiredProperty("juneau.jsTests.browsers"));

		var p = pb.start();
		if (!p.waitFor(3, TimeUnit.MINUTES)) {
			p.destroyForcibly();
			fail("prober did not finish within 3m; stderr:\n" + quietRead(stderr));
		}
		assertEquals(0, p.exitValue(), () -> "prober exited non-zero; stderr:\n" + quietRead(stderr));
		return Files.readString(stdout);
	}

	private static String quietRead(Path p) {
		try {
			return Files.readString(p);
		} catch (IOException e) {
			return "<unreadable: " + e + ">";
		}
	}

	@Test void a01_runtimeLoadedWithFillSanitizedHtmlSlotExported() {
		assertEquals(Boolean.TRUE, report.get("hasFillSanitizedHtmlSlot"), () -> "juneau-views.js did not load: " + report);
		assertEquals(List.of(), report.get("jsFailures"), () -> "the runtime logged errors: " + report.get("jsFailures"));
	}

	@Test void a02_scriptAndOnerrorPayload_doNotExecute_realParser() {
		assertEquals(Boolean.FALSE, report.get("xssFired"), () -> report.toString());
		assertEquals(Boolean.FALSE, report.get("hasScript"), () -> report.toString());
		assertEquals(Boolean.TRUE, report.get("survivorBold"),
			() -> "a benign <b> beside the dropped hostile markup must survive: " + report);
	}

	@Test void a03_benignMarkup_rendersAsRealElements_notEscapedText() {
		assertEquals(Boolean.TRUE, report.get("okHasTable"), () -> report.toString());
		assertEquals(Boolean.TRUE, report.get("okHasBold"), () -> report.toString());
		assertEquals(Boolean.TRUE, report.get("okTextHasWorld"), () -> report.toString());
		assertEquals(Boolean.FALSE, report.get("okTextHasMarkup"),
			() -> "markup must be real elements, not literal angle brackets in the text layer: " + report);
	}

	// ---------------------------------------------------------------------------------------------------
	// WORK-J0517: entity decoding - a regex shim cannot decode "&lt;script&gt;" the way a real HTML
	// parser does, so this proves the decoded text stays literal text and is never re-parsed as markup.
	// ---------------------------------------------------------------------------------------------------

	@Test void a04_entityEncodedScript_decodesToLiteralText_notReparsedToElement() {
		assertEquals(Boolean.TRUE, report.get("entityNoScriptElement"),
			() -> "an entity-encoded <script> must never decode into a real <script> element: " + report);
		assertEquals(Boolean.TRUE, report.get("entityPSurvived"), () -> report.toString());
		assertEquals(Boolean.TRUE, report.get("entityTextIsLiteral"),
			() -> "decoded text must contain the literal '<script>alert(1)</script>' characters: " + report);
	}

	// ---------------------------------------------------------------------------------------------------
	// WORK-J0517: <table> foster-parenting - the HTML5 tree builder relocates a misplaced non-table-
	// structure child to a PRECEDING SIBLING of the table before the copier ever walks the tree. a05
	// proves that is real-parser ground truth (not a harness assumption); a06 proves the copier's output
	// mirrors that already-fostered shape rather than assuming a naive nested read.
	// ---------------------------------------------------------------------------------------------------

	@Test void a05_tableForeignChild_fosterParentedBeforeTable_byRealParser() {
		assertEquals(Boolean.TRUE, report.get("fosterSrcBBeforeTable"),
			() -> "real-parser ground truth: <b> must be foster-parented to precede <table>, order was "
				+ report.get("fosterSrcOrder"));
	}

	@Test void a06_copierMirrorsFosteredTreeShape_notNaiveNestedRead() {
		assertEquals(Boolean.TRUE, report.get("fosterCopyBBeforeTable"),
			() -> "copier output must mirror the fostered order, was " + report.get("fosterCopyOrder"));
		assertEquals(Boolean.TRUE, report.get("fosterCopyBNotNestedInTable"),
			() -> "the foster-parented <b> must not appear nested inside <table> in the copy: " + report);
		assertEquals(Boolean.TRUE, report.get("fosterCopyCellSurvived"), () -> report.toString());
		assertEquals(Boolean.TRUE, report.get("fosterCopySpanSurvived"), () -> report.toString());
	}

	// ---------------------------------------------------------------------------------------------------
	// WORK-J0517: <template> - content lives in an inert DocumentFragment, never the element's own
	// childNodes (real-parser property a regex shim's substring view cannot model). a07 proves that
	// ground truth; a08 proves the copier drops the (DROP_TAGS) tag and its content wholesale.
	// ---------------------------------------------------------------------------------------------------

	@Test void a07_templateContent_livesInInertFragment_neverOwnChildNodes_realParser() {
		assertEquals(Boolean.TRUE, report.get("templateOwnChildNodesEmpty"),
			() -> "a <template>'s own childNodes must be empty; content lives in .content instead: " + report);
		assertEquals(Boolean.TRUE, report.get("templateContentHasChildren"),
			() -> "the inert .content fragment must still hold the parsed children: " + report);
	}

	@Test void a08_templateAndContents_droppedWholesale_noExecutionNoLeak() {
		assertEquals(Boolean.TRUE, report.get("templateNotExecuted"), () -> report.toString());
		assertEquals(Boolean.TRUE, report.get("templateNoTemplateTag"), () -> report.toString());
		assertEquals(Boolean.TRUE, report.get("templateNoScriptTag"), () -> report.toString());
		assertEquals(Boolean.TRUE, report.get("templateHiddenTextNotLeaked"),
			() -> "template content must not leak into the copy's text layer: " + report);
		assertEquals(Boolean.TRUE, report.get("templateSiblingSurvived"), () -> report.toString());
	}

	// ---------------------------------------------------------------------------------------------------
	// WORK-J0517: <noscript> - a DOMParser document has no browsing context, so scripting is DISABLED,
	// which per spec means <noscript> content parses as REAL child elements (opposite of a scripting-
	// enabled page's raw-text treatment). a09 proves that ground truth; a10 proves the copier drops the
	// (DROP_TAGS) tag and its now-real-element children wholesale regardless.
	// ---------------------------------------------------------------------------------------------------

	@Test void a09_noscriptContent_parsedAsRealElements_scriptingDisabled_realParser() {
		assertEquals(Boolean.TRUE, report.get("noscriptParsedAsRealElements"),
			() -> "with scripting disabled (a DOMParser doc), <noscript> content must parse as real "
				+ "elements, not raw text: " + report);
	}

	@Test void a10_noscriptAndContents_droppedWholesale_noExecutionNoLeak() {
		assertEquals(Boolean.TRUE, report.get("noscriptNotExecuted"), () -> report.toString());
		assertEquals(Boolean.TRUE, report.get("noscriptNoScriptTag"), () -> report.toString());
		assertEquals(Boolean.TRUE, report.get("noscriptHiddenTextNotLeaked"),
			() -> "noscript content must not leak into the copy's text layer: " + report);
		assertEquals(Boolean.TRUE, report.get("noscriptSiblingSurvived"), () -> report.toString());
	}

	// ---------------------------------------------------------------------------------------------------
	// WORK-J0517: namespace / foreign content - <svg> switches the HTML5 tree builder into the
	// foreign-content algorithm, producing REAL SVG-namespace nodes. a11 proves that ground truth; a12
	// proves the (DROP_TAGS) tag-name-only allowlist match drops the whole foreign subtree wholesale,
	// never smuggling a same-named allowed HTML tag (e.g. an SVG-namespace <a>) through by namespace-blind
	// matching.
	// ---------------------------------------------------------------------------------------------------

	@Test void a11_svgForeignContent_isRealForeignNamespace_realParser() {
		assertEquals(Boolean.TRUE, report.get("svgIsRealForeignNamespace"),
			() -> "<svg> must parse into a real SVG-namespace element: " + report);
		assertEquals(Boolean.TRUE, report.get("svgScriptIsForeignNamespace"),
			() -> "the nested <script> inside <svg> must be a real SVG-namespace node: " + report);
	}

	@Test void a12_svgSubtree_droppedWholesale_noScriptNoAnchorSmuggled() {
		assertEquals(Boolean.TRUE, report.get("svgNotExecuted"), () -> report.toString());
		assertEquals(Boolean.TRUE, report.get("svgNoScriptTag"), () -> report.toString());
		assertEquals(Boolean.TRUE, report.get("svgNoAnchorSmuggled"),
			() -> "an SVG-namespace <a> must not be smuggled through as an allowed HTML <a>: " + report);
		assertEquals(Boolean.TRUE, report.get("svgClickTextNotLeaked"),
			() -> "the dropped foreign subtree's text must not leak into the copy: " + report);
		assertEquals(Boolean.TRUE, report.get("svgSiblingSurvived"), () -> report.toString());
	}
}
