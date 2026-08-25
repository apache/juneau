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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;

/**
 * {@link QuickStatsTable} emission, the {@link ViewTable} placement of the strip, and the display-only lock.
 *
 * <p>
 * The display-only assertions here are the <b>mechanical</b> half of the lock: rather than trusting that
 * {@link QuickStats} has no action field, they scan the emitted markup for every affordance a runtime could bind to.
 * If a later slice adds a tile action, these fail before the feature ships.
 */
class QuickStatsTable_Emit_Test extends TestBase {

	private static QuickStats stats() {
		return QuickStats.create("overview").items(
			StatTile.of("open", "Open", "42").tone(StatusTone.INFO),
			StatBar.of("seats", "Seats", 180, 500).tone(StatusTone.WARNING),
			SegmentedBadge.of("runs", "Runs").segments(
				SegmentedBadge.Segment.of("failed", 3).tone(StatusTone.ERROR),
				SegmentedBadge.Segment.of("done", 40).tone(StatusTone.SUCCESS)));
	}

	/** Sets an off-palette tone the typed {@code tone(StatusTone)} builder cannot express. */
	private static StatTile offPalette(String tone) {
		var t = StatTile.of("t", "T", "1");
		t.tone = tone;
		return t;
	}

	private static String html(QuickStats q) {
		return Html.of(QuickStatsTable.of(q));
	}

	// -----------------------------------------------------------------------------------------------------------
	// a) strip container
	// -----------------------------------------------------------------------------------------------------------

	@Test void a01_strip_carriesClassIdAndItsOwnContractVersion() {
		var h = html(stats());
		assertTrue(h.contains("class=\"jc-quickstats\""), h);
		assertTrue(h.contains("data-juneau-quickstats=\"overview\""), h);
		// The strip advertises QuickStats.CONTRACT_VERSION, never ViewDef's.
		assertTrue(h.contains("data-juneau-quickstats-contract=\"1\""), h);
		assertEquals("1", QuickStats.CONTRACT_VERSION);
	}

	@Test void a02_nullOrInvalidStats_rejected() {
		assertThrows(IllegalArgumentException.class, () -> QuickStatsTable.of(null));
		// The emitter validates on entry, so an unvalidated bean cannot slip into markup.
		var noItems = QuickStats.create("x");
		assertThrows(IllegalArgumentException.class, () -> QuickStatsTable.of(noItems));
		var offPaletteItems = QuickStats.create("x").items(offPalette("ok"));
		assertThrows(IllegalArgumentException.class, () -> QuickStatsTable.of(offPaletteItems));
	}

	// -----------------------------------------------------------------------------------------------------------
	// b) the three item shapes
	// -----------------------------------------------------------------------------------------------------------

	@Test void b01_statTile_emitsLabelAndValueWithToneModifier() {
		var h = html(QuickStats.create("q").items(StatTile.of("open", "Open", "42").tone(StatusTone.INFO)));
		assertTrue(h.contains("class=\"jc-stat jc-stat-tile\" data-juneau-stat=\"open\""), h);
		assertTrue(h.contains("<span class=\"jc-stat-label\">Open</span>"), h);
		assertTrue(h.contains("<span class=\"jc-stat-value is-info\">42</span>"), h);
	}

	@Test void b02_statBar_emitsAServerComputedWidth() {
		var h = html(QuickStats.create("q").items(StatBar.of("seats", "Seats", 180, 500).tone(StatusTone.WARNING)));
		assertTrue(h.contains("class=\"jc-stat jc-stat-bar\" data-juneau-stat=\"seats\""), h);
		// 180/500 = 36%, computed on the server: no client arithmetic and no animation.
		assertTrue(h.contains("class=\"jc-stat-fill is-warning\" style=\"width:36%\""), h);
		assertTrue(h.contains("<span class=\"jc-stat-value\">180 / 500</span>"), h);
		// The decorative track is hidden from assistive tech; the figure itself is the accessible text.
		assertTrue(h.contains("class=\"jc-stat-track\" aria-hidden=\"true\""), h);
	}

	@Test void b03_statBar_percentIsClampedNotWrapped() {
		assertEquals(0, StatBar.of("b", "B", 0, 500).percent());
		assertEquals(100, StatBar.of("b", "B", 500, 500).percent());
		// Over max clamps to a full track rather than overflowing it.
		assertEquals(100, StatBar.of("b", "B", 900, 500).percent());
		assertEquals(36, StatBar.of("b", "B", 180, 500).percent());
	}

	@Test void b04_segmentedBadge_emitsOneSpanPerSegment() {
		var h = html(QuickStats.create("q").items(SegmentedBadge.of("runs", "Runs").segments(
			SegmentedBadge.Segment.of("failed", 3).tone(StatusTone.ERROR),
			SegmentedBadge.Segment.of("done", 40).tone(StatusTone.SUCCESS))));
		assertTrue(h.contains("class=\"jc-stat jc-stat-segments\" data-juneau-stat=\"runs\""), h);
		assertTrue(h.contains("class=\"jc-stat-segment is-error\""), h);
		assertTrue(h.contains("class=\"jc-stat-segment is-success\""), h);
		assertTrue(h.contains("<span class=\"jc-stat-segment-count\">3</span>"), h);
		assertTrue(h.contains("<span class=\"jc-stat-segment-label\">failed</span>"), h);
	}

	@Test void b05_itemsKeepDeclarationOrder() {
		var h = html(stats());
		assertTrue(h.indexOf("data-juneau-stat=\"open\"") < h.indexOf("data-juneau-stat=\"seats\""), h);
		assertTrue(h.indexOf("data-juneau-stat=\"seats\"") < h.indexOf("data-juneau-stat=\"runs\""), h);
	}

	/** {@code neutral} is in-palette but emits no modifier, so it is indistinguishable from an unset tone. */
	@Test void b06_neutralAndUnsetToneBothEmitNoModifier() {
		var neutral = html(QuickStats.create("q").items(StatTile.of("t", "T", "1").tone(StatusTone.NEUTRAL)));
		var unset = html(QuickStats.create("q").items(StatTile.of("t", "T", "1")));
		assertEquals(unset, neutral);
		assertFalse(neutral.contains("is-neutral"), neutral);
	}

	// -----------------------------------------------------------------------------------------------------------
	// c) display-only, asserted mechanically against the emitted markup
	// -----------------------------------------------------------------------------------------------------------

	/**
	 * There is nothing in the strip for a runtime to bind a click, a key press, a navigation, or a timer to.  This is
	 * the assertion that makes "display-only" a lock rather than a note: it scans the actual markup for every
	 * affordance the toolkit's runtime dispatches on.
	 */
	@Test void c01_emittedMarkupHasNoClickOrKeyboardAffordance() {
		var h = html(stats());
		for (var forbidden : List.of(
				"role=",                    // no button/link semantics
				"tabindex",                 // not focusable
				"href",                     // no navigation
				"data-juneau-action",       // not a row action
				"data-juneau-pill",         // not a pill either
				"onclick", "onkeydown", "onmouseover", "onload",   // no inline handlers
				"<a ", "<button", "<input", "<form"))               // no interactive elements
			assertFalse(h.contains(forbidden), () -> "Emitted quick-stats markup must not contain '" + forbidden + "': " + h);
	}

	/**
	 * No sidecar, no endpoint, no interval: unlike a bar slot, a quick-stats strip publishes nothing for a client to
	 * refresh from, so there is no polling surface even if a runtime wanted one.
	 */
	@Test void c02_emittedMarkupPublishesNoRefreshSurface() {
		var h = html(stats());
		assertFalse(h.contains("<script"), h);
		assertFalse(h.contains("endpoint"), h);
		assertFalse(h.contains("refresh"), h);
		assertFalse(h.contains("interval"), h);
		assertFalse(h.contains("poll"), h);
	}

	/** Only {@code div}/{@code span} elements are emitted, so there is no inert-by-accident element to regress. */
	@Test void c03_onlyDivAndSpanElementsAreEmitted() {
		var tags = new TreeSet<String>();
		var h = html(stats());
		for (var i = h.indexOf('<'); i >= 0; i = h.indexOf('<', i + 1)) {
			var j = i + 1;
			while (j < h.length() && (Character.isLetterOrDigit(h.charAt(j)) || h.charAt(j) == '/'))
				j++;
			tags.add(h.substring(i + 1, j).replace("/", ""));
		}
		assertEquals(Set.of("div", "span"), tags, h);
	}

	@Test void c04_hostileLabelsAndValuesAreEscaped() {
		var h = html(QuickStats.create("q").items(
			StatTile.of("t", "<img src=x onerror=alert(1)>", "<script>alert(1)</script>")));
		assertFalse(h.toLowerCase(Locale.ROOT).contains("<script"), h);
		assertFalse(h.contains("<img"), h);
		assertTrue(h.contains("&lt;img"), h);
	}

	// -----------------------------------------------------------------------------------------------------------
	// d) ViewTable placement + the wire-promotion guard
	// -----------------------------------------------------------------------------------------------------------

	private static ViewDef view(QuickStats q) {
		return ViewDef.create("releases").dataMode(ViewDef.DataMode.CLIENT).dataUrl("/u")
			.columns(Column.of("name").title("Name"))
			.quickStats(q)
			.build();
	}

	@Test void d01_viewTable_emitsTheStripAboveTheTable() {
		var h = Html.of(ViewTable.of(view(stats())));
		var strip = h.indexOf("jc-quickstats");
		var table = h.indexOf("<table");
		assertTrue(strip >= 0, h);
		// Above the <table>, so it lands above the control row DataTables grows at init rather than inside it.
		assertTrue(strip < table, () -> "quick-stats strip must precede the table: " + h);
	}

	@Test void d02_viewTable_withoutQuickStats_emitsNoStrip() {
		var h = Html.of(ViewTable.of(view(null)));
		assertFalse(h.contains("jc-quickstats"), h);
		assertFalse(h.contains("data-juneau-stat"), h);
	}

	@Test void d03_viewDefValidate_cascadesIntoQuickStats() {
		var v = view(QuickStats.create("q").items(offPalette("exceeds")));
		var e = assertThrows(IllegalArgumentException.class, v::validate);
		assertTrue(e.getMessage().contains("exceeds"), e::getMessage);
		// And the same bad strip is refused at emit time, not just by an explicit validate() call.
		assertThrows(IllegalArgumentException.class, () -> ViewTable.of(v));
	}

	/**
	 * {@code quickStats} is a <b>Java-only</b> builder field, so attaching a strip cannot bump the wire contract.
	 * {@link ViewDef#CONTRACT_VERSION} stays {@code "4"} and the serialized {@code VIEW_META} gains no key.
	 */
	@Test void d04_quickStatsIsNotPromotedToTheViewMetaWire() {
		assertEquals("4", ViewDef.CONTRACT_VERSION);
		var withStrip = Json.of(view(stats()));
		var without = Json.of(view(null));
		assertEquals(without, withStrip);
		assertFalse(withStrip.contains("quickStats"), withStrip);
		assertFalse(withStrip.contains("jc-quickstats"), withStrip);
		assertTrue(withStrip.contains("\"contractVersion\":\"4\""), withStrip);
	}
}
