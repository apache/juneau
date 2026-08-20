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
import static org.apache.juneau.test.bct.BctAssertions.*;
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
 * Always-on coverage for the {@code juneau-config.js} pure config-application layer (TODO-444, slice 4):
 * {@code computeEffectiveColumns} / {@code validateView} / saved-view (de)serialization / {@code dtIndex}.
 *
 * <p>
 * Two layers, matching the slice-2 split between {@link ViewsJs_ConfigPersistence_Test} and its browser canary:
 * <ul>
 * 	<li><b>Source-shape</b> (always runs, no Node) &mdash; pins that the shipped script defines and exports the
 * 		§4.3 / §4.2-index-model functions.</li>
 * 	<li><b>Behavioral</b> (runs when {@code node} is on {@code PATH}) &mdash; drives
 * 		{@code config-application.cjs} against the real classpath {@code juneau-config.js} so the hardening rules
 * 		and the load-bearing {@code dtIndex} fixture ({@code [sel, A, B(hidden), C, actions] → C is 3}) are
 * 		actually executed, not merely grepped.  Absent Node, the behavioral tests
 * 		{@link Assumptions#assumeTrue(boolean) assumeTrue}-skip so an offline source-tarball build never
 * 		hard-depends on Node (same posture as the {@code js-tests} profile comment in this module's pom).</li>
 * </ul>
 *
 * <p>
 * Deliberately does <b>not</b> require the {@code js-tests} / Playwright profile — the harness is pure Node
 * {@code vm}, no Chromium.
 */
class ViewsJs_ConfigApplication_Test extends TestBase {

	/** Classpath location of the shipped config script (shared with {@link ViewsJs_ConfigPersistence_Test}). */
	static final String CONFIG_JS_RESOURCE = ViewsJs_ConfigPersistence_Test.CONFIG_JS_RESOURCE;

	private static String configJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(CONFIG_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + CONFIG_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	/** Balanced-brace function-body extractor (same algorithm as {@link ViewsJs_ConfigPersistence_Test}). */
	private static String functionBody(String body, String signature) {
		var start = body.indexOf(signature);
		assertTrue(start >= 0, () -> "'" + signature + "' not found:\n" + body);
		var i = body.indexOf('{', start);
		assertTrue(i >= 0, () -> "'" + signature + "' has no opening brace:\n" + body);
		var depth = 0;
		var j = i;
		for (; j < body.length(); j++) {
			var c = body.charAt(j);
			if (c == '{') {
				depth++;
			} else if (c == '}') {
				depth--;
				if (depth == 0) { j++; break; }
			} else if (c == '"' || c == '\'' || c == '`') {
				var quote = c;
				j++;
				while (j < body.length() && body.charAt(j) != quote) { if (body.charAt(j) == '\\') j++; j++; }
			} else if (c == '/' && j + 1 < body.length() && body.charAt(j + 1) == '/') {
				while (j < body.length() && body.charAt(j) != '\n') j++;
			} else if (c == '/' && j + 1 < body.length() && body.charAt(j + 1) == '*') {
				j = body.indexOf("*/", j) + 1;
			}
		}
		return body.substring(start, j);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) Source-shape — functions exist and are exported on NS.config
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_validateView_enforcesPinnedAndAtLeastOneVisible() throws Exception {
		var fn = functionBody(configJs(), "function validateView(");
		assertTrue(fn.contains("c.pinned"), fn);
		assertTrue(fn.contains("visible.length === 0"), fn);
	}

	@Test void a02_validateView_dropsUnknownIds_rejectsDuplicates() throws Exception {
		var fn = functionBody(configJs(), "function validateView(");
		assertTrue(fn.contains("hasDuplicateEntries"), fn);
		assertTrue(fn.contains("byData[id] != null"), fn);
	}

	@Test void a03_validateView_constrainsFormatsToDeclaredList() throws Exception {
		var fn = functionBody(configJs(), "function validateView(");
		assertTrue(fn.contains("allowed.indexOf(fmt)"), fn);
	}

	@Test void a04_computeEffectiveColumns_preservesRenderMetaAndHref_blankLabelReverts() throws Exception {
		var body = configJs();
		var compute = functionBody(body, "function computeEffectiveColumns(");
		assertTrue(compute.contains("swapRenderId("), compute);
		assertTrue(compute.contains("String(override).trim()"), compute);
		var swap = functionBody(body, "function swapRenderId(");
		assertTrue(swap.contains("render.meta"), swap);
		assertTrue(swap.contains("render.popover"), swap);
		assertTrue(body.contains("function copyCatalogColumn("), body);
		var copy = functionBody(body, "function copyCatalogColumn(");
		assertTrue(copy.contains("popover"), copy);
	}

	@Test void a05_dtIndex_isIndexIntoActualOptsColumns_notVisibleOffset() throws Exception {
		var body = configJs();
		var fn = functionBody(body, "function dtIndex(");
		assertTrue(fn.contains("optsColumns[i].data === dataKey"), fn);
		assertTrue(body.contains("function buildOptsColumnSpace("), body);
		assertTrue(body.contains("hasSelection"), body);
		assertTrue(body.contains("hasActions"), body);
	}

	@Test void a06_serializeDeserializeSavedView_schemaVersionAndBlankLabelOmit() throws Exception {
		var body = configJs();
		assertTrue(body.contains("function serializeSavedView("), body);
		assertTrue(body.contains("function deserializeSavedView("), body);
		var ser = functionBody(body, "function serializeSavedView(");
		assertTrue(ser.contains("CURRENT_SCHEMA_VERSION"), ser);
		assertTrue(ser.contains("String(v).trim()"), ser);
	}

	@Test void a07_pureLayerExportedOnNsConfig() throws Exception {
		var body = configJs();
		for (var name : new String[]{
			"NS.config.validateView = validateView",
			"NS.config.computeEffectiveColumns = computeEffectiveColumns",
			"NS.config.deserializeSavedView = deserializeSavedView",
			"NS.config.serializeSavedView = serializeSavedView",
			"NS.config.buildOptsColumnSpace = buildOptsColumnSpace",
			"NS.config.dtIndex = dtIndex"
		})
			assertTrue(body.contains(name), () -> "missing export '" + name + "':\n" + body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b–l) Behavioral — real Node execution of the pure layer (skipped when node is absent)
	//------------------------------------------------------------------------------------------------------------------

	private static Map<?,?> report;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var harness = locateHarness();
		if (harness == null)
			return;
		var configFile = Files.createTempFile("juneau-config-", ".js");
		try {
			Files.writeString(configFile, configJs(), UTF_8);
			report = Json.to(runNode(harness, configFile), Map.class);
		} finally {
			Files.deleteIfExists(configFile);
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
		} catch (Exception e) {
			return false;
		}
	}

	private static Path locateHarness() {
		var basedir = System.getProperty("basedir");
		if (basedir != null) {
			var p = Path.of(basedir, "src/test/js/config-application.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/config-application.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/config-application.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path configJs) throws Exception {
		var stdout = Files.createTempFile("config-application-stdout-", ".json");
		var stderr = Files.createTempFile("config-application-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), configJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("config-application.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("config-application.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or config-application.cjs not found — behavioral layer skipped");
		return report;
	}

	@Test void b01_defaultLayering_catalogOrderAndDefaultVisible() {
		var r = report();
		assertEquals(List.of("A", "B", "C"), r.get("a_defaultOrder"));
		assertEquals(List.of("A", "B", "C"), r.get("a_defaultVisible"));
	}

	@Test void b02_pinnedAlwaysForcedVisible() {
		var r = report();
		assertEquals(true, r.get("b_pinnedOk"));
		assertEquals(true, r.get("b_pinnedForcedVisible"));
	}

	@Test void b03_atLeastOneVisible_repairsAllHiddenBlob() {
		var r = report();
		assertEquals(true, r.get("c_atLeastOneVisible"));
		assertSize(1, r.get("c_repairedVisible"));
	}

	@Test void b04_unknownColumnIdsDropped() {
		var r = report();
		assertEquals(true, r.get("d_ok"));
		assertEquals(List.of("C", "A", "B"), r.get("d_order"));
		assertFalse(((List<?>)r.get("d_visible")).contains("GONE"));
		assertEquals(Map.of("C", "See"), r.get("d_labels"));
		assertEquals(Map.of("C", "ts-zulu"), r.get("d_formats"));
	}

	@Test void b05_duplicateOrderAndVisibleRejected() {
		var r = report();
		assertEquals(false, ((Map<?,?>)r.get("e_dupOrder")).get("ok"));
		assertEquals(false, ((Map<?,?>)r.get("e_dupVisible")).get("ok"));
	}

	@Test void b06_formatOverrideConstrainedToDeclaredList() {
		var r = report();
		assertEquals(Map.of(), r.get("f_formatsAfterConstraint"));
	}

	@Test void b07_formatSwap_preservesRenderMetaAndHref() {
		var r = report();
		assertEquals("ts-zulu", r.get("g_renderId"));
		assertEquals(Map.of("tz", "UTC"), r.get("g_renderMeta"));
		assertEquals("/c/{id}", r.get("g_href"));
	}

	@Test void b08_blankLabelRevertsToCatalogTitle() {
		var r = report();
		assertEquals("Col B", r.get("h_blankReverts"));
		assertEquals("Custom C", r.get("h_customKept"));
	}

	@Test void b09_reorderAndHide() {
		var r = report();
		assertEquals(List.of("C", "A", "B"), r.get("i_order"));
		assertEquals(List.of(true, true, false), r.get("i_visibility"));
	}

	@Test void b10_newCatalogColumnAppearsPerDefaultVisible() {
		var r = report();
		assertEquals(List.of("A", "B", "C", "D"), r.get("j_orderIncludesD"));
		assertEquals(false, r.get("j_D_visible"));
	}

	@Test void b11_serializeDeserializeRoundTrip_blankLabelOmitted() {
		var r = report();
		assertEquals(true, r.get("k_blankLabelOmitted"));
		var ser = (Map<?,?>)r.get("k_serialized");
		assertEquals(1, ((Number)ser.get("schemaVersion")).intValue());
		assertEquals(List.of("C", "A", "B"), ser.get("order"));
		assertEquals(Map.of("C", "See"), ser.get("labels"));
		assertEquals(Map.of("C", "ts-zulu"), ser.get("formats"));
	}

	/**
	 * Load-bearing §4.2 / §6.10 fixture: {@code opts.columns = [sel, A, B(hidden), C, actions]} → C's
	 * {@code dtIndex} is <b>3</b>, not 2 (the "visible+offset" self-contradiction).
	 */
	@Test void b12_dtIndex_selectionPlusHiddenBetweenTarget_Cis3() {
		var r = report();
		// List.of forbids nulls — selection/actions cells legitimately carry data:null.
		assertEquals(Arrays.asList(null, "A", "B", "C", null), r.get("l_optsDataKeys"));
		assertEquals(5, ((Number)r.get("l_optsLength")).intValue());
		assertEquals(1, ((Number)r.get("l_dtIndex_A")).intValue());
		assertEquals(2, ((Number)r.get("l_dtIndex_B")).intValue());
		assertEquals(3, ((Number)r.get("l_dtIndex_C")).intValue());
		assertEquals(-1, ((Number)r.get("l_dtIndex_missing")).intValue());
	}

	@Test void b13_defaultVisibleFalse_hiddenOnFirstVisit() {
		var r = report();
		assertEquals(List.of("A", "C"), r.get("m_firstVisitVisible"));
	}

	@Test void b14_copyAndSwap_preservePopover() {
		var r = report();
		assertEquals(true, r.get("n_hasCopyExport"));
		assertEquals(true, r.get("n_copyPreservesPopover"));
		assertEquals(true, r.get("n_copyIsStructured"));
		assertEquals(true, r.get("n_swapKeepsPopover"));
	}
}
