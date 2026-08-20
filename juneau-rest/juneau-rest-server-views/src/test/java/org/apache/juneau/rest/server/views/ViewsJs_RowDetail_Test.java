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
 * Always-on coverage for the row-detail JS helpers.  Source-shape always runs; behavioral Node harness runs
 * when {@code node} is on {@code PATH} (skipped otherwise — no {@code -Pjs-tests} required).
 */
class ViewsJs_RowDetail_Test extends TestBase {

	private static String viewsJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.VIEWS_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.VIEWS_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String rendersJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.RENDERS_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.RENDERS_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	@Test void a01_helpersExportedOnNsInit() throws Exception {
		var body = viewsJs();
		for (var name : new String[]{
			"isSafeDetailUrl: isSafeDetailUrl",
			"substituteDetailUrl: substituteDetailUrl",
			"scalarFieldValue: scalarFieldValue",
			"isSafeMarkdownHref: isSafeMarkdownHref",
			"fillMarkdownSlot: fillMarkdownSlot",
			"fillRenderSlot: fillRenderSlot",
			"fillDetailSlots: fillDetailSlots",
			"findRowDetailTemplate: findRowDetailTemplate",
			"JUNEAU_ROW_DETAIL_CONTRACT_VERSION: JUNEAU_ROW_DETAIL_CONTRACT_VERSION"
		})
			assertTrue(body.contains(name), () -> "missing export '" + name + "'");
		assertFalse(body.contains("function buildDetailFields("), body);
		assertFalse(body.contains("function buildDetailPanel("), body);
		assertTrue(body.contains("submitRowAction(action, table, parentTr"),
			"write path must target the expanded DataTables row, not expand JSON");
	}

	private static Map<?,?> report;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var harness = locateHarness();
		if (harness == null)
			return;
		var viewsFile = Files.createTempFile("juneau-views-", ".js");
		var rendersFile = Files.createTempFile("juneau-renders-", ".js");
		try {
			Files.writeString(viewsFile, viewsJs(), UTF_8);
			Files.writeString(rendersFile, rendersJs(), UTF_8);
			report = Json.to(runNode(harness, viewsFile, rendersFile), Map.class);
		} finally {
			Files.deleteIfExists(viewsFile);
			Files.deleteIfExists(rendersFile);
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
			var p = Path.of(basedir, "src/test/js/row-detail.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/row-detail.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/row-detail.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path viewsJs, Path rendersJs) throws Exception {
		var stdout = Files.createTempFile("row-detail-stdout-", ".json");
		var stderr = Files.createTempFile("row-detail-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), viewsJs.toString(), rendersJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("row-detail.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("row-detail.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or row-detail.cjs not found — behavioral layer skipped");
		return report;
	}

	@Test void b01_urlSafety() {
		var r = report();
		assertEquals(true, r.get("url_pathOk"));
		assertEquals(true, r.get("url_relativeOk"));
		assertEquals(false, r.get("url_absolute"));
		assertEquals(false, r.get("url_protoRel"));
		assertEquals(false, r.get("url_scheme"));
		assertEquals(false, r.get("url_dotdot"));
	}

	@Test void b02_hostileId_isEncoded_notPathTraversal() {
		var r = report();
		assertEquals(true, r.get("sub_hostileEncoded"));
		assertNull(r.get("sub_absoluteTpl"));
		assertEquals("/data/alerts/a1", r.get("sub_plain"));
		var hostileUrl = String.valueOf(r.get("sub_hostile"));
		assertFalse(hostileUrl.contains("../"), hostileUrl);
		assertTrue(hostileUrl.startsWith("/data/alerts/"), hostileUrl);
	}

	@Test void b03_scalarAndXssFill_usesTextContent() {
		var r = report();
		assertEquals("hi", r.get("scalar_str"));
		assertEquals("7", r.get("scalar_num"));
		assertEquals("true", r.get("scalar_bool"));
		assertEquals("", r.get("scalar_null"));
		assertEquals("", r.get("scalar_obj"));
		assertEquals("", r.get("scalar_arr"));
		assertEquals(true, r.get("fill_xssNotInterpreted"));
		assertTrue(String.valueOf(r.get("fill_xss")).contains("<img"));
		assertEquals("42", r.get("fill_num"));
		assertEquals("", r.get("fill_missing"));
		assertEquals(true, r.get("hasFillMarkdown"));
		assertEquals(false, r.get("md_hasScript"));
		assertEquals(false, r.get("md_hasImg"));
		assertEquals(false, r.get("md_jsHref"));
		assertEquals(true, r.get("md_httpsHref"));
		assertEquals(true, r.get("md_textHasOk"));
		assertEquals(true, r.get("md_textHasX"));
		assertEquals(true, r.get("md_textHasY"));
		assertEquals(false, r.get("md_textHasAlert"));
		assertEquals(false, r.get("href_js"));
		assertEquals(true, r.get("href_https"));
		assertEquals(false, r.get("href_data"));
	}

	@Test void b07_fillRenderSlot_tagProgressLinkedAndCanary() {
		var r = report();
		assertEquals(true, r.get("hasFillRender"));
		assertEquals(true, r.get("rr_tagHasClass"));
		assertEquals(true, r.get("rr_progressWidth"));
		assertEquals(true, r.get("rr_linkedHref"));
		assertEquals(false, r.get("rr_jsHref"));
		assertEquals(false, r.get("rr_hasScript"));
		assertEquals(false, r.get("rr_hostileStyle"));
		assertEquals(true, r.get("rr_truncateTitle"));
		assertEquals(true, r.get("rr_jsonCode"));
		assertEquals(true, r.get("rr_malformedMetaOk"));
		assertEquals("", r.get("rr_missing"));
		assertEquals(true, r.get("rr_dispatchRenderFirst"));
	}

	@Test void b04_404500_actionRefButtonless_collapseRemains() {
		var r = report();
		assertEquals(true, r.get("fail_ackDisabled"));
		assertEquals(true, r.get("fail_ackHidden"));
		assertEquals(true, r.get("fail_escHidden"));
		assertEquals(true, r.get("fail_collapseEnabled"));
	}

	@Test void b05_coalesceKeyAndDropIfOrphaned() {
		var r = report();
		assertEquals("a1:3", r.get("key_a1"));
		assertEquals(true, r.get("drop_gone"));
		assertEquals(true, r.get("drop_gen"));
		assertEquals(false, r.get("drop_keep"));
	}

	@Test void b06_loudContractMismatch() {
		var r = report();
		assertEquals("1", r.get("contractVersion"));
		assertEquals(true, r.get("contract_ok"));
		assertEquals(false, r.get("contract_bad"));
		assertEquals(false, r.get("contract_missing"));
	}
}
