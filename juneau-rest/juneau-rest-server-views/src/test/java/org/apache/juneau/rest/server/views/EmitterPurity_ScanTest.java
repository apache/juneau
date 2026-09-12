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
 * WORK-J0522d, design test <b>33</b>: the <b>emitter-purity scan</b> &mdash; every {@code rawText(...)} call site in
 * the <b>main</b> Java of the views and widgets trees hands the verbatim writer a JSON-sidecar payload (or provably
 * nothing), and the content-bearing sites are a closed, individually-named set that <b>cannot grow</b>.
 *
 * <p>
 * This is the test the design nominates as proving the headline claim of the emitter surgery: emitters stopped being
 * markup authors and became descriptor authors. Its counterpart {@link RawContentSink_SecurityScan_Test} guards the
 * other half &mdash; what an <i>author</i> may pour into a declarative content bean. Both share
 * {@link RawContentSinkScanner}'s stripper and argument extractor rather than keeping two divergent copies.
 *
 * <h5 class='section'>Why this is a pinned floor rather than the universal the design states, and what that still buys</h5>
 * <p>
 * The design states test 33 as a universal &mdash; <i>every</i> {@code rawText} takes a sidecar payload, i.e. the
 * content-bearing count is <b>zero</b>. That universal is not reachable in this item, and the reason is a
 * contradiction between two written requirements rather than a shortfall here. Reaching zero requires deleting
 * {@code Tab.content}, {@code Subtab.content} and {@code CardContent.content}; this item's scope says in as many
 * words that nothing is deprecated and no old path is removed, with two named exceptions, and these are not among
 * them. Those deletions are the deprecation-window close, and they belong to the item that owns it.
 *
 * <p>
 * The detail arm <i>did</i> reach the end state here (see {@link #b04_detailEmitterIsAtZeroContentSinks()}) because
 * it was given a staged mechanism where both shapes emit simultaneously. Cards and tabs were never given the
 * equivalent, so collapsing them now would break every existing consumer the day it landed.
 *
 * <p>
 * So this scan is written as the strongest checkable form available today, in the shape that makes the universal a
 * one-line change later: the content-bearing set is pinned <b>exactly</b>, by name, at the four deprecated
 * declarative-content sites. A <b>new</b> content-bearing {@code rawText} anywhere in either tree fails this test
 * &mdash; which is the property that actually matters between now and the window's close, since it freezes the
 * verbatim-markup surface and lets it only shrink. When the four are deleted,
 * {@link #KNOWN_CONTENT_SINKS} becomes empty and this scan <i>is</i> the design's universal, unrestructured.
 *
 * <h5 class='section'>Why the anti-vacuity checks come first</h5>
 * <p>
 * Every assertion below &mdash; a floor, a universal, an exact-set pin &mdash; is satisfied <i>more</i> easily by a
 * scan that finds nothing than by one that finds the real tree. {@link RawContentSinkScanner}'s own javadoc records
 * this hazard biting it once already: the widgets tree is reached by a relative path and a walk skips a missing
 * directory in silence. So the checks proving the scan still sees both trees are as load-bearing as the violation
 * check itself, and they run first.
 */
class EmitterPurity_ScanTest extends TestBase {

	/**
	 * The content-bearing {@code rawText(...)} sinks that survive the emitter surgery, named individually because an
	 * unnamed allowance is indistinguishable from a hole.
	 *
	 * <p>
	 * Keyed as {@code <SimpleFileName>:<argument source text>}, so the pin survives line-number drift. All are the
	 * deprecated declarative-content path: {@code Tab.content} and {@code Subtab.content} in the page emitter, and
	 * {@code CardContent.content} in the card emitter. <b>This set must only ever shrink.</b>
	 */
	private static final Set<String> KNOWN_CONTENT_SINKS = Set.of(
		"PageTable.java:t.content",
		"PageTable.java:s.content",
		"CardGridTable.java:cc.content");

	/**
	 * Floor on the total number of {@code rawText(...)} sites, well under the 13 present when this was written.
	 *
	 * <p>
	 * Deliberately slack: sidecars legitimately come and go as emitters are refactored, so pinning the total exactly
	 * would make this a change-detector. Its only job is to fail loudly if the scan stops seeing the tree at all.
	 */
	private static final int MINIMUM_EXPECTED_SITES = 10;

	private static Path moduleRoot() {
		var root = RawContentSinkScanner.locateModuleRoot();
		assertNotNull(root, "could not locate the juneau-rest-server-views module root; the scan would see nothing");
		return root;
	}

	private static List<RawContentSinkScanner.RawTextSite> sites() throws Exception {
		return RawContentSinkScanner.scanRawTextTree(moduleRoot());
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) ANTI-VACUITY.  First on purpose - see the class javadoc.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_scanSeesTheRealTree() throws Exception {
		var sites = sites();
		assertTrue(sites.size() >= MINIMUM_EXPECTED_SITES,
			() -> "the scan stopped seeing the tree, and a silently-empty scan reads as a pass.  Found only "
				+ sites.size() + " rawText site(s): " + sites);
	}

	@Test void a02_scanReachesTheSiblingWidgetsTree() {
		// Asserted separately from every other check here because all of those are floors or universals, and both
		// are satisfied MORE easily by a smaller set.  This is the exact hazard RawContentSinkScanner's javadoc
		// records having bitten it before.
		var widgets = RawContentSinkScanner.widgetsModuleRoot(moduleRoot()).resolve("src/main/java");
		assertTrue(Files.isDirectory(widgets),
			() -> "the widgets main tree is not where this scan looks (" + widgets + "), so its emitters would drop "
				+ "out of every scan silently while the detection logic stayed perfectly correct");
	}

	@Test void a03_scanFindsBothKnownSidecarShapes() throws Exception {
		var args = sites().stream().map(RawContentSinkScanner.RawTextSite::arg).toList();
		assertTrue(args.contains("json"),
			() -> "expected the plain per-host sidecar payload shape to still exist: " + args);
		assertTrue(args.contains("bulkJson"),
			() -> "expected the bulk sidecar payload shape to still exist: " + args);
		// The inline rawText(escapeForScript(nestedJson(...))) shape retired with F24 nested-table emit.
		// The classifier still recognizes it (a05 / a07); the live tree no longer has a call site.
	}

	@Test void a04_scanFindsTheKnownContentSinks() throws Exception {
		// If the classifier silently stopped recognizing content, b01/b02 would both pass vacuously.
		var keys = sites().stream().map(RawContentSinkScanner.RawTextSite::key).toList();
		for (var known : KNOWN_CONTENT_SINKS)
			assertTrue(keys.contains(known),
				() -> "the scan no longer finds the known content sink '" + known + "'.  If it was genuinely removed "
					+ "(the deprecation window closing) drop it from KNOWN_CONTENT_SINKS in the same commit; "
					+ "otherwise the scan has gone blind.  Found: " + keys);
	}

	@Test void a05_argumentExtractionIsBalanced() {
		// Must be balanced-paren, or rawText(escapeForScript(nestedJson(v, tokenless))) truncates to
		// "escapeForScript(nestedJson(v" and gets silently RECLASSIFIED as content-bearing.
		var found = RawContentSinkScanner.scanRawText("X.java",
			"class X { void f() { q.text(rawText(escapeForScript(nestedJson(v, tokenless)))); } }");
		assertEquals(1, found.size(), found::toString);
		assertEquals("escapeForScript(nestedJson(v, tokenless))", found.get(0).arg());
		assertTrue(found.get(0).isSidecarPayload());
	}

	@Test void a06_commentMentionsAreNotCounted() {
		// The real tree mentions rawText(...) in javadoc on Tab, Subtab, CardContent, ViewTable, PageTable and
		// CardGridTable.  Counting prose would both inflate the floor and manufacture phantom violations.
		var found = RawContentSinkScanner.scanRawText("X.java", """
			class X {
				/** Writes via {@link HtmlBuilder#rawText(String) rawText} verbatim. */
				// rawText(t.content) mentioned here
				void f() {}
			}
			""");
		assertTrue(found.isEmpty(), found::toString);
	}

	@Test void a07_classifierSeparatesTheThreeShapes() {
		var sidecar = new RawContentSinkScanner.RawTextSite("A.java", 1, "json");
		var inline = new RawContentSinkScanner.RawTextSite("A.java", 2, "escapeForScript(nestedJson(v))");
		var empty = new RawContentSinkScanner.RawTextSite("A.java", 3, "\"\"");
		var content = new RawContentSinkScanner.RawTextSite("A.java", 4, "t.content");

		assertTrue(sidecar.isSidecarPayload() && !sidecar.isContentBearing());
		assertTrue(inline.isSidecarPayload() && !inline.isContentBearing());
		assertTrue(empty.isEmptyLiteral() && !empty.isContentBearing());
		assertTrue(content.isContentBearing(), "a bare field read is the shape a live-data body would take");
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) THE HEADLINE CLAIM.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_everyRawTextIsASidecarPayloadEmptyOrANamedContentSink() throws Exception {
		var violations = sites().stream()
			.filter(RawContentSinkScanner.RawTextSite::isContentBearing)
			.filter(s -> !KNOWN_CONTENT_SINKS.contains(s.key()))
			.map(RawContentSinkScanner.RawTextSite::toString)
			.toList();
		assertTrue(violations.isEmpty(),
			() -> "a NEW content-bearing rawText(...) sink appeared.  Emitters author DESCRIPTORS, not markup: every "
				+ "rawText in main Java must hand the verbatim writer a JSON-sidecar payload.  The only content-bearing "
				+ "exceptions are the deprecated declarative-content sinks named in KNOWN_CONTENT_SINKS, and that set "
				+ "exists to shrink to empty - never to grow:\n  " + String.join("\n  ", violations));
	}

	@Test void b02_theContentBearingSurfaceIsFrozenAtExactlyFourSites() throws Exception {
		var content = sites().stream()
			.filter(RawContentSinkScanner.RawTextSite::isContentBearing)
			.map(RawContentSinkScanner.RawTextSite::toString)
			.toList();
		// FOUR call sites over THREE distinct argument shapes: Tab.content is read twice - once for the leaf
		// content-only panel, once for the content-plus-subtabs preamble.
		assertEquals(4, content.size(),
			() -> "the content-bearing rawText surface must stay exactly the known four.  It may SHRINK (that is the "
				+ "deprecation window closing, and KNOWN_CONTENT_SINKS should shrink with it in the same commit) but "
				+ "it must never grow:\n  " + String.join("\n  ", content));
	}

	@Test void b03_theRegionEmitterIsPureFromDayOne() throws Exception {
		// The emitter this item ADDS must be on the right side of the line from the start.  A region container is an
		// EMPTY div plus a descriptor sidecar; if RegionTable ever writes content verbatim, the whole
		// "configuration rides the sidecar, not the markup" claim is gone.
		var region = sites().stream().filter(s -> "RegionTable.java".equals(s.file())).toList();
		assertFalse(region.isEmpty(),
			"RegionTable writes a descriptor sidecar, so at least one rawText site is expected; finding none means "
				+ "the scan is not reaching it");
		region.forEach(s -> assertTrue(s.isSidecarPayload(),
			() -> "the region emitter must never be a content sink: " + s));
	}

	@Test void b04_detailEmitterIsAtZeroContentSinks() throws Exception {
		// The detail arm is the one that reached the design's end state in this item, because it got the staged
		// mechanism.  So for ViewTable specifically the design's universal holds, and is asserted as a universal.
		sites().stream()
			.filter(s -> "ViewTable.java".equals(s.file()))
			.forEach(s -> assertTrue(s.isSidecarPayload(),
				() -> "the detail emitter is at zero content-bearing rawText sinks and must stay there: " + s));
	}

	@Test void b05_everyContentSinkIsInTheDeprecatedDeclarativePath() throws Exception {
		// Pins WHERE the four live, not just how many.  A content sink migrating into a different emitter would keep
		// the count at four while quietly widening the blast radius.
		var files = sites().stream()
			.filter(RawContentSinkScanner.RawTextSite::isContentBearing)
			.map(RawContentSinkScanner.RawTextSite::file)
			.distinct()
			.sorted()
			.toList();
		assertEquals(List.of("CardGridTable.java", "PageTable.java"), files,
			"content-bearing rawText may only live in the two emitters that still serve the deprecated "
				+ "declarative-content path");
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) Proof the guard BITES.  a01-a07 prove the scan sees the tree; these prove that seeing it is not the same as
	//    a passing test being meaningful.  Both mutate REAL main source text, so they cannot drift from the shapes
	//    the emitters actually use the way a hand-written fixture silently would.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_mutatingARealSidecarIntoAFieldRead_becomesAViolation() throws Exception {
		var viewTable = moduleRoot().resolve("src/main/java/org/apache/juneau/rest/server/views/ViewTable.java");
		var src = Files.readString(viewTable);
		assertTrue(src.contains("text(rawText(json))"), "the real sidecar shape moved; this mutation no longer applies");

		var mutated = src.replace("text(rawText(json))", "text(rawText(row.liveValue))");
		var sites = RawContentSinkScanner.scanRawText("ViewTable.java", mutated);
		var offenders = sites.stream().filter(RawContentSinkScanner.RawTextSite::isContentBearing).toList();

		assertEquals(1, offenders.size(),
			() -> "turning one real sidecar write into a live-data field read must produce exactly one content-bearing "
				+ "site; if it produces none, b01/b02 are passing for free: " + sites);
		assertFalse(KNOWN_CONTENT_SINKS.contains(offenders.get(0).key()),
			"the mutated site must NOT be absorbed by the allowlist - otherwise the allowlist is a hole");
	}

	@Test void c02_aNewContentSinkInTheWidgetsTreeWouldBeCaught() {
		// The widgets tree is the one reached by a relative path, so pair a02's "the directory is there" with proof
		// that a violation IN it is actually classified as one.
		var sites = RawContentSinkScanner.scanRawText("CardContent.java",
			"class CardContent { Object f() { return div(rawText(this.userSuppliedBody)); } }");
		assertEquals(1, sites.size(), sites::toString);
		assertTrue(sites.get(0).isContentBearing(), () -> "not classified as content: " + sites.get(0));
		assertFalse(KNOWN_CONTENT_SINKS.contains(sites.get(0).key()), "must not be pre-absorbed by the allowlist");
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) The allowlist is itself a declaration, so pin what it declares.
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_knownContentSinksAreOnlyTheDeprecatedDeclarativeContentFields() {
		assertEquals(
			Set.of("PageTable.java:t.content", "PageTable.java:s.content", "CardGridTable.java:cc.content"),
			KNOWN_CONTENT_SINKS,
			"widening this set is how the guard dies quietly.  It exists to shrink to empty when the deprecation "
				+ "window closes, at which point this scan becomes the design's universal with no other change");
	}
}
