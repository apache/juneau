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
package org.apache.juneau.rest.server.widgets;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * {@link QuickStats} bean contract, the sealed {@link StatItem} permit set, the closed {@link StatusTone} palette, and
 * fail-closed {@link QuickStats#validate()} branches.
 */
class QuickStats_Test extends TestBase {

	private static QuickStats strip(StatItem...items) {
		return QuickStats.create("q").items(items);
	}

	@Test void a01_contractVersion_isOne() {
		assertEquals("1", QuickStats.CONTRACT_VERSION);
	}

	@Test void a02_isAWidget() {
		assertTrue(Widget.class.isAssignableFrom(QuickStats.class));
	}

	@Test void a03_builder_roundTrip() {
		var q = QuickStats.create("fleet").items(
			StatTile.of("open", "Open", "42").tone(StatusTone.INFO),
			StatBar.of("seats", "Seats used", 180, 500).tone(StatusTone.WARNING),
			SegmentedBadge.of("jobs", "Jobs").segments(
				SegmentedBadge.Segment.of("failed", 3).tone(StatusTone.ERROR),
				SegmentedBadge.Segment.of("done", 40).tone(StatusTone.SUCCESS)));
		assertEquals("fleet", q.id);
		assertEquals(3, q.items.size());
		q.validate();
	}

	@Test void a04_sealed_permitsOnlyTheThreeItemTypes() {
		assertTrue(StatItem.class.isSealed());
		var permitted = new HashSet<Class<?>>(Arrays.asList(StatItem.class.getPermittedSubclasses()));
		assertEquals(Set.of(StatTile.class, StatBar.class, SegmentedBadge.class), permitted);
	}

	@Test void a05_emptyItemList_rejected() {
		assertThrows(IllegalArgumentException.class, () -> QuickStats.create("q").validate());
		assertThrows(IllegalArgumentException.class, () -> QuickStats.create("q").items().validate());
	}

	@Test void a06_blankId_rejected() {
		assertThrows(IllegalArgumentException.class,
			() -> QuickStats.create("  ").items(StatTile.of("t", "T", "1")).validate());
	}

	@Test void a07_duplicateItemId_rejected() {
		var q = strip(StatTile.of("dup", "A", "1"), StatTile.of("dup", "B", "2"));
		var e = assertThrows(IllegalArgumentException.class, q::validate);
		assertTrue(e.getMessage().contains("dup"), e::getMessage);
	}

	@Test void a08_badContractVersion_rejected() {
		var q = strip(StatTile.of("t", "T", "1"));
		q.contractVersion = "9";
		assertThrows(IllegalArgumentException.class, q::validate);
	}

	@Test void b01_palette_isExactlyTheFiveTones() {
		assertEquals(List.of("info", "success", "warning", "error", "neutral"), List.copyOf(StatusTone.WIRE_TOKENS));
		for (var t : StatusTone.values())
			strip(StatTile.of("t", "T", "1").tone(t)).validate();
	}

	@Test void b02_offPaletteTone_rejected() {
		// The v1 pill vocabulary, the Badge Tone enum names, and casing variants are all off-palette now.
		for (var tone : new String[]{"ok", "exceeds", "warn", "accent", "danger", "INFO", "Success", ""}) {
			var q = strip(StatTile.of("t", "T", "1"));
			((StatTile) q.items.get(0)).tone = tone;
			var e = assertThrows(IllegalArgumentException.class, q::validate, tone);
			assertTrue(e.getMessage().contains(tone), e::getMessage);
		}
	}

	@Test void b03_offPaletteTone_rejectedOnBarAndSegmentToo() {
		var bar = strip(StatBar.of("b", "B", 1, 2));
		((StatBar) bar.items.get(0)).tone = "ok";
		assertThrows(IllegalArgumentException.class, bar::validate);

		var seg = SegmentedBadge.Segment.of("failed", 1);
		seg.tone = "exceeds";
		var badge = strip(SegmentedBadge.of("s", "S").segments(seg));
		assertThrows(IllegalArgumentException.class, badge::validate);
	}

	@Test void b04_toneIsOptional() {
		strip(StatTile.of("t", "T", "1")).validate();
		strip(StatTile.of("t", "T", "1").tone(null)).validate();
	}

	@Test void c01_statTile_requiresLabelAndValue() {
		assertThrows(IllegalArgumentException.class, () -> strip(StatTile.of("t", "  ", "1")).validate());
		assertThrows(IllegalArgumentException.class, () -> strip(StatTile.of("t", "T", null)).validate());
		assertThrows(IllegalArgumentException.class, () -> strip(StatTile.of("  ", "T", "1")).validate());
	}

	@Test void c02_statBar_requiresNonNegativeValueAndPositiveMax() {
		assertThrows(IllegalArgumentException.class, () -> strip(StatBar.of("b", "B", -1, 10)).validate());
		assertThrows(IllegalArgumentException.class, () -> strip(StatBar.of("b", "B", 1, 0)).validate());
	}

	@Test void c03_statBar_percentIsServerComputedAndClamped() {
		assertEquals(36, StatBar.of("b", "B", 180, 500).percent());
		assertEquals(0, StatBar.of("b", "B", 0, 500).percent());
		assertEquals(100, StatBar.of("b", "B", 900, 500).percent());
	}

	@Test void c04_segmentedBadge_requiresAtLeastOneWellFormedSegment() {
		assertThrows(IllegalArgumentException.class, () -> strip(SegmentedBadge.of("s", "S")).validate());
		assertThrows(IllegalArgumentException.class,
			() -> strip(SegmentedBadge.of("s", "S").segments(SegmentedBadge.Segment.of("  ", 1))).validate());
		assertThrows(IllegalArgumentException.class,
			() -> strip(SegmentedBadge.of("s", "S").segments(SegmentedBadge.Segment.of("x", -1))).validate());
	}

	@Test void d01_displayOnly_noActionEndpointOrRefreshSurfaceExists() {
		// The display-only lock is enforced by the bean shape: no field on QuickStats or any StatItem can carry an
		// action id, an endpoint, a refresh url, or a poll interval.
		var banned = Set.of("action", "actionId", "endpoint", "refreshUrl", "url", "href", "pollIntervalMs", "poll");
		for (var c : List.<Class<?>>of(QuickStats.class, StatTile.class, StatBar.class, SegmentedBadge.class,
				SegmentedBadge.Segment.class))
			for (var f : c.getFields())
				assertFalse(banned.contains(f.getName()),
					() -> c.getSimpleName() + " must not declare a '" + f.getName() + "' field (display-only lock)");
	}

	@Test void d02_statusTone_isDistinctFromTheBadgePalette() {
		// StatusTone deliberately does not replace Tone; the Badge overlay palette is a different surface.
		var statusNames = new HashSet<String>();
		for (var t : StatusTone.values())
			statusNames.add(t.name());
		assertEquals(Set.of("INFO", "SUCCESS", "WARNING", "ERROR", "NEUTRAL"), statusNames);
		assertFalse(StatusTone.isValid("accent"));
		assertFalse(StatusTone.isValid(null));
	}
}
