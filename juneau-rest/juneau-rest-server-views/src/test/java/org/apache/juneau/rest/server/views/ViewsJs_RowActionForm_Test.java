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
import static org.junit.jupiter.api.Assumptions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Always-on source-shape coverage for {@code RowAction.form} confirmation-GET substitution, plus a Node
 * behavioral harness ({@code row-action-form.cjs}) when {@code node} is on {@code PATH}.  Mirrors
 * {@link ViewsJs_RowActions_Test}'s two-layer split: source-shape always runs; the GET-time proof is
 * skipped (not failed) when {@code node} is absent.  Does not grow {@link ViewsJs_RowActions_Test}.
 */
class ViewsJs_RowActionForm_Test extends TestBase {

	private static String resource(String name) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(name)) {
			assertNotNull(in, () -> "missing classpath resource: " + name);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String viewsJs() throws IOException { return resource(ViewsMixin.VIEWS_JS_RESOURCE); }

	private static String functionBody(String body, String signature) {
		var start = body.indexOf(signature);
		assertTrue(start >= 0, () -> signature + " not found:\n" + body);
		var end = body.indexOf("\n\t}", start);
		return body.substring(start, end < 0 ? body.length() : end);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) Source-shape pins
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_openActionDialogBlankFormBranchRunsBeforeSubstitution() throws Exception {
		var fn = functionBody(viewsJs(), "function openActionDialog(");
		var blankIdx = fn.indexOf("isBlankToken(action.form)");
		var resolveIdx = fn.indexOf("resolveRowActionFormUrl(");
		assertTrue(blankIdx >= 0, fn);
		assertTrue(resolveIdx >= 0, fn);
		assertTrue(blankIdx < resolveIdx, fn);
	}

	@Test void a02_openActionDialogCallsRowDataThenTheSiblingHelper() throws Exception {
		var body = viewsJs();
		var fn = functionBody(body, "function openActionDialog(");
		assertTrue(fn.contains("rowDataForTr(ctx, tr)"), fn);
		assertTrue(fn.contains("resolveRowActionFormUrl(action.form"), fn);
		assertTrue(body.contains("\n\tfunction resolveRowActionFormUrl("),
			() -> "helper must be a top-level sibling, not nested:\n" + fn);
		assertFalse(fn.contains("\n\tfunction resolveRowActionFormUrl("),
			() -> "helper must not be nested inside openActionDialog:\n" + fn);
	}

	@Test void a03_fetchUsesResolvedUrlNotActionForm() throws Exception {
		var body = viewsJs();
		var fn = functionBody(body, "function openActionDialog(");
		assertTrue(fn.contains("fetch(resolved.url,"), fn);
		assertFalse(fn.contains("fetch(action.form"), fn);
		assertFalse(body.contains("fetch(action.form"), body);
	}

	@Test void a04_helperGuardsMatchWritePathOrderAndReusePredicates() throws Exception {
		var fn = functionBody(viewsJs(), "function resolveRowActionFormUrl(");
		var emptyIdx = fn.indexOf("reason: \"empty-substitution\"");
		var unresolvedIdx = fn.indexOf("reason: \"unresolved-endpoint\"");
		var unsafeIdx = fn.indexOf("reason: \"unsafe-endpoint\"");
		assertTrue(emptyIdx >= 0 && unresolvedIdx >= 0 && unsafeIdx >= 0, fn);
		assertTrue(emptyIdx < unresolvedIdx, fn);
		assertTrue(unresolvedIdx < unsafeIdx, fn);
		assertTrue(fn.contains("hasBlankSubstitution("), fn);
		assertTrue(fn.contains("substituteRowActionEndpoint("), fn);
		assertTrue(fn.contains("hasResidualToken("), fn);
		assertTrue(fn.contains("hasDotDotSegment("), fn);
	}

	@Test void a05_getOptionsUnchanged() throws Exception {
		var fn = functionBody(viewsJs(), "function openActionDialog(");
		assertTrue(fn.contains("method: \"GET\""), fn);
		assertTrue(fn.contains("credentials: \"same-origin\""), fn);
		assertTrue(fn.contains("\"Accept\": \"application/json\""), fn);
	}

	@Test void a06_initDoesNotExportTheFormResolver() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("function resolveRowActionFormUrl("), body);
		assertFalse(body.contains("resolveRowActionFormUrl:"), body);
	}

	@Test void a07_contractVersionStaysFour() throws Exception {
		assertEquals("4", ViewDef.CONTRACT_VERSION);
		assertTrue(viewsJs().contains("JUNEAU_VIEW_CONTRACT_VERSION = \"4\""), viewsJs());
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) Behavioral harness (row-action-form.cjs) — skipped when node is absent
	//------------------------------------------------------------------------------------------------------------------

	private static Map<?,?> report;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var harness = locateHarness();
		if (harness == null)
			return;
		var rendersFile = Files.createTempFile("juneau-renders-", ".js");
		var viewsFile = Files.createTempFile("juneau-views-", ".js");
		try {
			Files.writeString(rendersFile, resource(ViewsMixin.RENDERS_JS_RESOURCE), UTF_8);
			Files.writeString(viewsFile, resource(ViewsMixin.VIEWS_JS_RESOURCE), UTF_8);
			report = Json.to(runNode(harness, rendersFile, viewsFile), Map.class);
		} finally {
			Files.deleteIfExists(rendersFile);
			Files.deleteIfExists(viewsFile);
		}
	}

	private static boolean nodeAvailable() {
		try {
			var p = new ProcessBuilder("node", "--version").redirectErrorStream(true).start();
			if (!p.waitFor(5, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				return false;
			}
			return p.exitValue() == 0;
		} catch (@SuppressWarnings("unused") Exception e) {
			return false;
		}
	}

	private static Path locateHarness() {
		var basedir = System.getProperty("basedir");
		if (basedir != null) {
			var p = Path.of(basedir, "src/test/js/row-action-form.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/row-action-form.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/row-action-form.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path rendersJs, Path viewsJs) throws Exception {
		var stdout = Files.createTempFile("row-action-form-stdout-", ".json");
		var stderr = Files.createTempFile("row-action-form-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), rendersJs.toString(), viewsJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("row-action-form.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("row-action-form.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
					+ "\nstdout:\n" + quietRead(stdout));
			return Files.readString(stdout, UTF_8);
		} finally {
			Files.deleteIfExists(stdout);
			Files.deleteIfExists(stderr);
		}
	}

	private static String quietRead(Path p) {
		try { return Files.readString(p, UTF_8); }
		catch (IOException e) { return "(unreadable: " + e.getMessage() + ")"; }
	}

	private static Map<?,?> report() {
		assumeTrue(report != null, "node not available or row-action-form.cjs not found — behavioral layer skipped");
		return report;
	}

	private static boolean flag(String key) {
		return Boolean.TRUE.equals(report().get(key));
	}

	@Test void b01_exampleShapedFormSubstitutesId() {
		assertEquals("/data/alerts/ALRT-2/ack-form", report().get("b01_url"));
		assertTrue(flag("b01_fetched"));
	}

	@Test void b01b_servletSchemeIsPreserved() {
		assertEquals("servlet:/incidents/a1/form", report().get("b01b_url"));
	}

	@Test void b02_tokenLessFormIsByteIdentical() {
		assertEquals("/data/x/ack-form", report().get("b02_url_withRow"));
		assertEquals("/data/x/ack-form", report().get("b02_url_noRow"));
	}

	@Test void b03_blankTokenValueRefusesEmptySubstitution() {
		assertFalse(flag("b03_missing_fetched"));
		assertEquals("empty-substitution", report().get("b03_missing_reason"));
		assertFalse(flag("b03_null_fetched"));
		assertEquals("empty-substitution", report().get("b03_null_reason"));
		assertFalse(flag("b03_absent_fetched"));
		assertEquals("empty-substitution", report().get("b03_absent_reason"));
		assertFalse(flag("b03_ws_fetched"));
		assertEquals("empty-substitution", report().get("b03_ws_reason"));
	}

	@Test void b04_valueIsEncoded() {
		assertEquals("/data/alerts/a%2F1%20b/ack-form", report().get("b04_url"));
	}

	@Test void b05_omittedRendersJsRefusesTemplatedForm() {
		assertFalse(flag("b05_fetched"));
		assertEquals("unresolved-endpoint", report().get("b05_reason"));
	}

	@Test void b06_omittedRendersJsStillFetchesTokenLessForm() {
		assertTrue(flag("b06_fetched"));
		assertEquals("/data/x/ack-form", report().get("b06_url"));
	}

	@Test void b07_dotDotRowValueRefuses() {
		assertFalse(flag("b07_fetched"));
		assertEquals("unsafe-endpoint", report().get("b07_reason"));
	}

	@Test void b08_authorDeclaredDotDotRefuses() {
		assertFalse(flag("b08_fetched"));
		assertEquals("unsafe-endpoint", report().get("b08_reason"));
	}

	@Test void b09_queryContextIsEncoded() {
		assertEquals("servlet:/x/form?target=a%26b%3Dc%23d", report().get("b09_url"));
	}

	@Test void b10_encodedDotDotIsNotASegment() {
		assertTrue(flag("b10_fetched"));
		assertEquals("/data/alerts/a%2F..%2Fb/ack-form", report().get("b10_url"));
	}

	@Test void b11_secondBlankTokenRefuses() {
		assertFalse(flag("b11_fetched"));
		assertEquals("empty-substitution", report().get("b11_reason"));
	}

	@Test void b12_templatedRibbonFormRefuses() {
		assertFalse(flag("b12_fetched"));
		assertEquals("empty-substitution", report().get("b12_reason"));
	}

	@Test void b13_tokenLessRibbonFormFetches() {
		assertTrue(flag("b13_fetched"));
		assertEquals("/projects/new-form", report().get("b13_url"));
	}

	@Test void b14_postDraftQueryStillSubstitutesPath() {
		assertEquals("servlet:/x/a1/form?juneauDrafts=%7B%22note%22%3A%22hi%22%7D", report().get("b14_url"));
	}

	@Test void b15_blankFormStaysConfirmOnly() {
		assertFalse(flag("b15_fetched"));
		assertEquals(1, ((Number) report().get("b15_dialogCount")).intValue());
		assertNull(report().get("b15_refusalReason"));
	}

	@Test void b16_resolverIsNotOnInit() {
		assertFalse(flag("hasResolverExport"));
	}
}
