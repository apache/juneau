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
 * Behavioral coverage for the per-column search popover commit semantics (design §4.5 operator help + §5 evaluation
 * model).  A Node harness ({@code column-search-popover.cjs}) loads {@code juneau-search.js} into the same sandbox
 * as views, drives {@code openColumnSearchPopover(...)} through each commit path, and reports what reached the grid:
 * help rendered from VIEW_META, an incomplete {@code $}-draft leaving the grid untouched, an out-of-set operator
 * rejected, a bare quick-filter previewing live on a client table but deferred on a server table, a {@code $}
 * expression deferred until Enter, and Esc reverting a live preview.  Source-shape pins live in
 * {@link ViewsJs_HeaderSortSearch_Test}.  Gated on {@code node} being on {@code PATH}.
 *
 * <p>
 * Test groups: {@code a01}-{@code a06} the commit/preview/revert semantics above; {@code a07} the per-type bare-value
 * help rendered before the operator help (and absent without search metadata); {@code b01} the per-table live
 * announcer (politely live, outside the wrapper, reused on repeat); {@code c01}-{@code c05} the dismiss announcements
 * (a revert names the column title, a never-previewed {@code $}-draft still announces, an Enter commit or a typed-then-
 * restored value never announces, and two reverts in a row both announce).
 */
class ViewsJs_ColumnSearchPopover_Test extends TestBase {

	private static String resource(String name) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(name)) {
			assertNotNull(in, name);
			return new String(in.readAllBytes(), UTF_8);
		}
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
		var searchFile = Files.createTempFile("juneau-search-", ".js");
		try {
			Files.writeString(viewsFile, resource(ViewsMixin.VIEWS_JS_RESOURCE), UTF_8);
			Files.writeString(rendersFile, resource(ViewsMixin.RENDERS_JS_RESOURCE), UTF_8);
			Files.writeString(searchFile, resource(ViewsMixin.SEARCH_JS_RESOURCE), UTF_8);
			report = Json.to(runNode(harness, rendersFile, viewsFile, searchFile), Map.class);
		} finally {
			Files.deleteIfExists(viewsFile);
			Files.deleteIfExists(rendersFile);
			Files.deleteIfExists(searchFile);
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
			var p = Path.of(basedir, "src/test/js/column-search-popover.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/column-search-popover.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/column-search-popover.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path rendersJs, Path viewsJs, Path searchJs) throws Exception {
		var stdout = Files.createTempFile("column-search-popover-stdout-", ".json");
		var stderr = Files.createTempFile("column-search-popover-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of(
					"node", harness.toString(), rendersJs.toString(), viewsJs.toString(), searchJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("column-search-popover.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("column-search-popover.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or column-search-popover.cjs not found — behavioral layer skipped");
		return report;
	}

	@Test void a01_operatorHelp_renderedFromViewMeta() {
		var r = report();
		// Every effective operator gets a help row: its "$name" then the operator API's help text (design §4.5).
		assertBean(r, "hasOpenColumnSearchPopover,hasSearchEngine,helpOps,helpTexts",
			"true,true,[$eq,$in,$contains],[Exact match.,Any of the listed values.,Case-insensitive substring.]");
	}

	@Test void a02_incompleteDollarDraft_leavesGridUntouched() {
		var r = report();
		// A still-being-typed "$eq(" must not blank/refilter the grid (design §5): zero draws, incomplete flagged.
		assertBean(r, "incompleteDrawCount,incompleteStatusIncomplete", "0,true");
	}

	@Test void a03_outOfSetOperator_isRejectedNotApplied() {
		var r = report();
		// An operator not on the column's effective set is a reject, not a silent filter (design §4.3).
		assertBean(r, "rejectDrawCount,rejectInvalidClass,rejectStatusText", "0,true,Not a valid search for this column.");
	}

	@Test void a04_bareQuickFilter_livesOnClientDefersOnServer() {
		var r = report();
		// Bare input previews live on a client table... but is deferred on a server table until Enter/Apply.
		assertBean(r, "bareClientInputApplied,bareServerInputApplied,bareServerCommitApplied", "abc,,abc");
		assertTrue(((Number) r.get("bareClientInputDraws")).longValue() >= 1L, r::toString);
	}

	@Test void a05_dollarExpression_deferredThenCommitsOnEnter() {
		var r = report();
		// A "$" expression is never previewed (even on a client table); it commits complete+valid on Enter.
		assertBean(r, "dollarInputApplied,dollarCommitApplied", ",$eq(OPEN)");
	}

	@Test void a06_escape_revertsLivePreview() {
		var r = report();
		// Esc reverts the live preview back to the value the popover opened with (design §5).
		assertBean(r, "revertPreApplied,revertPostApplied", "abc,");
	}

	@Test void a07_bareValueHelp_rendersBeforeOperatorHelp_andIsAbsentWithoutSearchMeta() {
		var r = report();
		assertEquals("Matches any value containing this text (case-insensitive). Use $eq(...) for an exact, case-sensitive match.",
			r.get("bareHelpText"), r::toString);
		assertBean(r, "bareHelpAbsentIsNull", "true");
	}

	@Test void b01_announcerIsPolitelyLiveOutsideWrapperAndReusedOnRepeat() {
		var r = report();
		assertBean(r, "announcerHasLiveRegion,announcerOutsideWrapper,announcerFirstTextMatches,announcerSecondTextMatchesAfterRepeat,announcerReused",
			"true,true,true,true,true");
	}

	@Test void c01_revertAnnouncesTheColumnTitle() {
		var r = report();
		assertBean(r, "revertAnnounced", "true");
	}

	@Test void c02_neverPreviewedDollarDraftStillAnnouncesOnDismiss() {
		var r = report();
		assertBean(r, "incompleteDismissDrawCount,incompleteDismissAnnounced", "0,true");
	}

	@Test void c03_committingWithEnterNeverAnnounces() {
		var r = report();
		assertBean(r, "commitAnnouncerAbsentOrEmpty", "true");
	}

	@Test void c04_typingThenRestoringTheOriginalValueNeverAnnounces() {
		var r = report();
		assertBean(r, "restoredAnnouncerAbsentOrEmpty", "true");
	}

	@Test void c05_twoSeparateRevertsOnTheSameTableBothAnnounce() {
		var r = report();
		assertBean(r, "firstRevertAnnounced,secondRevertAnnounced,sameAnnouncerReusedAcrossReverts", "true,true,true");
	}
}
