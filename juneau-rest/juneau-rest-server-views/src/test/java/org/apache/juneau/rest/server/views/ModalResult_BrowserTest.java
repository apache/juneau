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
 * The declarative-modal + typed-action-result + in-flight-row half of the module's <b>JavaScript-execution
 * harness</b>: runs the REAL served {@code juneau-views.js} in a real headless browser and asserts the TODO-416/417
 * behavior as a user would experience it &mdash; a {@code present=dialog} action opens an overlay whose typed
 * confirmation fields are painted with {@code textContent} (so an HTML-shaped value never becomes an element), every
 * settled outcome (success/failure/refusal/unknown) and a non-2xx transport refusal render a VISIBLE non-optimistic
 * banner, the in-flight marker is set while the sync write is outstanding and CLEARED on every terminal outcome
 * (including UNKNOWN), and polling is no longer frozen once the marker clears.
 *
 * <h5 class='section'>Why this exists (beyond the always-on source-shape test):</h5>
 * <p>
 * {@link ViewsJs_ModalResult_Test} proves the shipped script <i>contains</i> the modal/result/in-flight logic; it
 * cannot prove the overlay renders, the HTML-shaped field value stays inert text, or the marker actually clears on
 * an UNKNOWN outcome at runtime.  This canary drives the real runtime in Chromium against fabricated responses, so
 * those user-visible facts are measured rather than inferred.
 *
 * <h5 class='section'>Off by default:</h5>
 * <p>
 * Disabled unless the {@value #GATE} system property is set, which only the module's opt-in {@code js-tests} Maven
 * profile does.  It reuses that profile's provisioned Node + Playwright browser and derives its own prober
 * ({@code modal-result.cjs}) from the profile's {@code juneau.jsTests.harness} directory, so no pom change is needed.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link RowActionCsrf_BrowserTest} &mdash; the sibling fail-closed-CSRF canary.
 * </ul>
 */
@EnabledIfSystemProperty(named=ModalResult_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class ModalResult_BrowserTest extends TestBase {

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
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent().resolve("modal-result.cjs");

		var fixture = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body>\n<script>\n"
			+ resource(ViewsMixin.VIEWS_JS_RESOURCE)
			+ "\n</script></body></html>";
		var fixtureFile = Files.createDirectories(dir.resolve("fixtures")).resolve("modal-result.html");
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
		var stdout = dir.resolve("modal-result-stdout.json");
		var stderr = dir.resolve("modal-result-stderr.txt");
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

	@SuppressWarnings("unchecked")
	private static Map<String,Object> sub(String key) {
		return (Map<String,Object>) report.get(key);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) The runtime loaded, at the typed-action-result contract version (its OWN version, not the view's)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_runtimeLoadedAtActionResultContractVersion() {
		assertEquals(Boolean.TRUE, report.get("hasInit"), () -> "juneau-views.js did not populate JuneauViews.init: " + report);
		assertEquals(ActionResult.CONTRACT_VERSION, report.get("actionResultContractVersion"), () -> report.toString());
		assertEquals("1", ActionResult.CONTRACT_VERSION);
		assertEquals(List.of(), report.get("jsFailures"), () -> "the runtime logged errors: " + report.get("jsFailures"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) Every 2xx typed outcome renders a VISIBLE banner, and the in-flight marker is cleared (polling resumes)
	//------------------------------------------------------------------------------------------------------------------

	private void assertTerminal(String key, String expectedState, String expectedRole) {
		var o = sub(key);
		assertEquals(Boolean.TRUE, o.get("wasInflightBefore"), () -> key + ": marker should have been set before settle: " + o);
		assertEquals(Boolean.TRUE, o.get("visible"), () -> key + ": outcome banner not visible: " + o);
		assertEquals(expectedState, o.get("state"), () -> key + ": wrong state: " + o);
		assertEquals(expectedRole, o.get("role"), () -> key + ": wrong role: " + o);
		// The 417 hard requirement: the marker is cleared on EVERY terminal outcome, so polling is not frozen.
		assertEquals(Boolean.FALSE, o.get("inflight"), () -> key + ": in-flight marker not cleared: " + o);
		assertEquals(Boolean.FALSE, o.get("pollingFrozen"), () -> key + ": polling still frozen: " + o);
		assertEquals(Boolean.FALSE, o.get("triggerDisabled"), () -> key + ": trigger not re-enabled: " + o);
	}

	@Test void b01_successRendersStatusAndClearsMarker() {
		assertTerminal("success", "success", "status");
	}

	@Test void b02_failureRendersAlertAndClearsMarker() {
		assertTerminal("failure", "failure", "alert");
		assertTrue(String.valueOf(sub("failure").get("text")).contains("nope"), () -> report.toString());
	}

	@Test void b03_refusalRendersNamedCodeAndClearsMarker() {
		assertTerminal("refusal", "refusal", "alert");
		assertTrue(String.valueOf(sub("refusal").get("text")).contains("write-guard:not-armed"), () -> report.toString());
	}

	@Test void b04_unknownRendersVisibleAndClearsMarker() {
		// The load-bearing 417 case: an UNKNOWN outcome MUST clear the marker or the whole table's polling freezes.
		assertTerminal("unknown", "unknown", "alert");
	}

	@Test void b05_contractMismatchRendersUnknownNotOptimisticSuccess() {
		assertTerminal("contractMismatch", "unknown", "alert");
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) A non-2xx is a VISIBLE transport refusal read from the boundary envelope - not "HTTP 200 + schema" (HIGH-3)
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_http403RendersTransportRefusalFromEnvelope() {
		assertTerminal("transport403", "refusal", "alert");
		var text = String.valueOf(sub("transport403").get("text"));
		assertTrue(text.contains("CSRF_TOKEN_MISSING"), () -> "boundary reason not surfaced: " + report);
		assertTrue(text.contains("missing token"), () -> "envelope message not surfaced: " + report);
	}

	@Test void c02_http421WithNoBodyStillRendersVisibleRefusal() {
		// No X-Loopback-Boundary header, empty body -> still a visible, comprehensible non-optimistic refusal.
		assertTerminal("transport421", "refusal", "alert");
		assertTrue(String.valueOf(sub("transport421").get("text")).contains("421"), () -> report.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) Re-render from the authoritative result payload (MERGE_ROW)
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_successMergesAuthoritativeRow() {
		assertNotNull(report.get("mergedRow"), () -> "success did not re-render from the result row: " + report);
		assertEquals("1", report.get("mergeMarkedRow"), () -> report.toString());
		assertNotNull(report.get("mergeCtxRow"), () -> report.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// e) The modal overlay + typed-field confirmation via textContent (XSS-safe), then a bound submit
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_modalOverlayOpensWithTypedFields() {
		var m = sub("modal");
		assertEquals(Boolean.TRUE, m.get("backdropVisible"), () -> "modal overlay not visible: " + report);
		assertEquals("Acknowledge this incident?", m.get("title"), () -> report.toString());
		assertEquals(2L, ((Number) m.get("fieldCount")).longValue(), () -> report.toString());
	}

	@Test void e02_htmlShapedFieldValueStaysInertText() {
		// BLK-1/MED-9: the field value is painted with textContent, so an <img onerror> becomes literal text and
		// NO <img> element is created anywhere in the dialog.
		var m = sub("modal");
		assertEquals("<img src=x onerror=alert(1)>", m.get("evilFieldText"), () -> report.toString());
		assertEquals(0L, ((Number) m.get("injectedImgCount")).longValue(),
			() -> "an HTML-shaped field value was parsed into an element - textContent invariant broken: " + report);
	}

	@Test void e03_confirmSubmitCarriesIdempotencyKeyAndTargetId() {
		var m = sub("modal");
		assertEquals(Boolean.TRUE, m.get("submitIssued"), () -> "confirm did not submit: " + report);
		var body = String.valueOf(m.get("submitBody"));
		assertTrue(body.contains("key-abc"), () -> "idempotency key not carried on submit: " + report);
		assertTrue(body.contains("INC-1"), () -> "target id not carried on submit: " + report);
		assertTrue(body.contains("ack"), () -> "action id not carried on submit: " + report);
		assertEquals(Boolean.TRUE, m.get("backdropClosedAfterConfirm"), () -> "modal did not close on confirm: " + report);
	}

	@Test void e04_formDefInputsPaintTypedControlsAndSubmitFieldValues() {
		var f = sub("form");
		assertEquals(Boolean.TRUE, f.get("formVisible"), () -> "FormDef inputs not visible: " + report);
		assertEquals("<img src=x onerror=alert(1)>", f.get("textareaPrefill"), () -> report.toString());
		assertEquals("ok", f.get("notePrefill"), () -> report.toString());
		assertEquals(Boolean.TRUE, f.get("passwordSkipped"), () -> "non-text type was painted: " + report);
		assertEquals(0L, ((Number) f.get("injectedImgCount")).longValue(),
			() -> "hostile prefill became an element: " + report);
		assertEquals(Boolean.TRUE, f.get("templateNotInFormHtml"), () -> "form.template was used as markup: " + report);
		assertEquals(Boolean.TRUE, f.get("submitIssued"), () -> "confirm did not submit: " + report);
		var body = String.valueOf(f.get("submitBody"));
		assertTrue(body.contains("key-close"), () -> "idempotency key missing: " + report);
		assertTrue(body.contains("QABCDEF"), () -> "target id missing: " + report);
		assertTrue(body.contains("fixed in change"), () -> "edited textarea value missing: " + report);
		assertTrue(body.contains("\"fields\""), () -> "fields object missing: " + report);
		assertTrue(body.contains("resolution"), () -> report.toString());
		assertFalse(body.contains("skipme"), () -> "skipped type leaked into submit: " + report);
		assertFalse(body.contains("secret"), () -> "skipped type value leaked: " + report);
	}
}
