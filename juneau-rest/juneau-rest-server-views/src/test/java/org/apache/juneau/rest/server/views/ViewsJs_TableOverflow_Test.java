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
 * Always-on coverage for the TODO-445n table-overflow-discipline JS helpers ({@code ensureTableScroll} /
 * {@code unwrapTableScroll} - the DT1 "Approach B" wrap).  Source-shape always runs; the behavioral Node harness
 * runs when {@code node} is on {@code PATH} (skipped otherwise - no {@code -Pjs-tests} required).
 *
 * <p>
 * The jsdom-style harness has no DataTables and cannot measure {@code scrollWidth}, so it pins only the DOM
 * structure invariants (INV-1, INV-2, INV-5, N5) and the DT2 flex skip-guard (N-P5-B1).  The table-vs-page scroll
 * contract and the overflow-detected {@code tabindex} (L12 A) are pinned in {@code TableOverflow_BrowserTest}.
 */
class ViewsJs_TableOverflow_Test extends TestBase {

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

	//------------------------------------------------------------------------------------------------------------------
	// Source-shape (always-on) - the DT2 skip-guard MUST precede the .juneau-view-table-scroll skip, and the helper
	// must be wired from constructTable + paired with an unwrap on teardown (N-P5-B1 / B3).
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_helpersExportedAndWired() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("ensureTableScroll: ensureTableScroll"), "missing NS export ensureTableScroll");
		assertTrue(body.contains("unwrapTableScroll: unwrapTableScroll"), "missing NS export unwrapTableScroll");
		// Wired from constructTable (the wrap is applied after DataTable boot + toolbar).
		assertTrue(body.contains("ensureTableScroll(table"), "ensureTableScroll must be called from constructTable");
		// Paired unwrap in teardownTable so a destroy()+reconstruct cannot nest the toolbar inside the box (INV-5).
		assertTrue(body.contains("unwrapTableScroll(table"), "teardownTable must unwrap the DT1 scroll box");
	}

	@Test void a02_dt2SkipGuardPrecedesWrapClassSkip() throws Exception {
		var body = viewsJs();
		var dt2Guard = body.indexOf("closest(\".dt-layout-cell\")");
		// The wrap-class skip uses the TABLE_SCROLL_CLASS constant ("." + TABLE_SCROLL_CLASS), not a literal.
		var wrapGuard = body.indexOf("closest(\".\" + TABLE_SCROLL_CLASS)");
		assertTrue(dt2Guard >= 0, "ensureTableScroll must no-op on DT2 via table.closest('.dt-layout-cell')");
		assertTrue(wrapGuard >= 0, "ensureTableScroll must skip an already-wrapped table (closest('.'+TABLE_SCROLL_CLASS))");
		assertTrue(dt2Guard < wrapGuard,
			"the DT2 (.dt-layout-cell) skip-guard must fire BEFORE the .juneau-view-table-scroll skip (N-P5-B1)");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Behavioral Node harness (skips cleanly when node is absent).
	//------------------------------------------------------------------------------------------------------------------

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
			report = Json.to(runNode(harness, rendersFile, viewsFile), Map.class);
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
			var p = Path.of(basedir, "src/test/js/table-overflow.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/table-overflow.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/table-overflow.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path rendersJs, Path viewsJs) throws Exception {
		var stdout = Files.createTempFile("table-overflow-stdout-", ".json");
		var stderr = Files.createTempFile("table-overflow-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), rendersJs.toString(), viewsJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("table-overflow.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("table-overflow.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or table-overflow.cjs not found - behavioral layer skipped");
		return report;
	}

	@Test void b01_helpersExist() {
		var r = report();
		assertEquals(true, r.get("hasEnsureTableScroll"));
		assertEquals(true, r.get("hasUnwrapTableScroll"));
	}

	/** T-JS-1: after the wrap, the table is the sole element child of the scroll box, sibling-lineage of the toolbar. */
	@Test void b02_wrapStructure_inv1() {
		var r = report();
		assertEquals(true, r.get("t1_boxIsScrollClass"));
		assertEquals(true, r.get("t1_boxParentIsWrapper"));
		assertEquals(true, r.get("t1_tableIsSoleElementChild"));
		assertEquals(true, r.get("t1_toolbarNotAncestorOfTable"));
		assertEquals(true, r.get("t1_boxDoesNotContainToolbar"));
		assertEquals(true, r.get("t1_columnsAdjusted"));   // S5: columns.adjust() after the move
	}

	/** T-JS-3: the paging-pill menu is not a descendant of the scroll box (INV-2). */
	@Test void b03_pagingPillMenuOutsideBox_inv2() {
		assertEquals(true, report().get("t3_pillMenuOutsideBox"));
	}

	/** T-JS-2: idempotency across teardown - exactly one box after wrap/unwrap/re-wrap, toolbar never inside (INV-5). */
	@Test void b04_idempotencyAcrossTeardown_inv5() {
		var r = report();
		assertEquals(true, r.get("t2_unwrapRestoresTableToWrapper"));
		assertEquals(true, r.get("t2_noBoxAfterUnwrap"));
		assertEquals(true, r.get("t2_exactlyOneBoxAfterRewrap"));
		assertEquals(true, r.get("t2_toolbarNotDescendantOfBox"));
		assertEquals(true, r.get("t2_tableNotDuplicated"));
	}

	/** T-JS-5: a table already inside a scroll box does not get a second box (nested tables, N5). */
	@Test void b05_nestedTableNotDoubleWrapped_n5() {
		assertEquals(true, report().get("t5_noSecondBox"));
	}

	/** N-P5-B1: a DT2 table (parent is a flex {@code .dt-layout-cell}) is not wrapped - Approach D CSS owns overflow. */
	@Test void b06_dt2SkipGuardFires() {
		var r = report();
		assertEquals(true, r.get("dt2_notWrapped"));
		assertEquals(true, r.get("dt2_noScrollBoxCreated"));
	}

	/**
	 * DT1 is <b>not</b> dropped by the 10.0 overflow contract: a DT1-shaped DOM still resolves its scroll region
	 * through the existing wrap path, so there is no "explicit failure on DT1" anywhere in the contract.
	 */
	@Test void b07_dt1StillResolvesThroughTheWrapPath() {
		assertEquals(true, report().get("l12_dt1RegionResolves"),
			"a DT1-shaped DOM must still resolve .juneau-view-table-scroll as its scroll region");
	}

	/**
	 * L12 A preserved verbatim: the overflow-detected {@code tabindex="0"} + generic label are applied ONLY while the
	 * region actually overflows, and BOTH are removed once it does not.  OQ4/OQ5 stay out.
	 */
	@Test void b08_l12a_tabindexOnlyWhileOverflowing() {
		var r = report();
		assertEquals(true, r.get("l12_overflowingHasTabindex"), "an overflowing region must be keyboard-reachable");
		assertEquals(true, r.get("l12_overflowingHasLabel"), "an overflowing region must carry the generic label");
		assertEquals(true, r.get("l12_notOverflowingNoTabindex"),
			"a non-overflowing region must NOT keep a tab stop (a false 'scrollable' announcement)");
		assertEquals(true, r.get("l12_notOverflowingNoLabel"), "a non-overflowing region must NOT keep the label");
	}

	/**
	 * The clip/ellipsis opt-out has to reach the cell to mean anything: the named emitters (progress / pill / tag /
	 * linked) put {@code juneau-cell-wrap} on their column's className, which is what stamps it on the {@code <td>}.
	 * Prose renderers stay on the clip default, and an author's own column class is preserved rather than replaced.
	 */
	@Test void b09_namedEmittersPutTheOptOutOnTheCell() {
		var r = report();
		assertEquals(true, r.get("wrap_pill"), "the `pill` column must carry juneau-cell-wrap");
		assertEquals(true, r.get("wrap_progress"), "the `progress` column must carry juneau-cell-wrap");
		assertEquals(true, r.get("wrap_tag"), "the `tag` column must carry juneau-cell-wrap");
		assertEquals(true, r.get("wrap_linked"), "the `linked` column must carry juneau-cell-wrap");
		assertEquals(true, r.get("wrap_truncateStaysClipped"),
			"`truncate` is unchanged and stays on the clip default - it must not opt out");
		assertEquals(true, r.get("wrap_dateStaysClipped"), "a prose renderer must stay on the clip default");
		assertEquals(true, r.get("wrap_authorClassPreserved"),
			"a renderer class must be APPENDED to the author's column class, never replace it");
	}
}
