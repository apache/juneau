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
}
