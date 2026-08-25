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
import org.apache.juneau.rest.server.widgets.CalendarDef.*;
import org.apache.juneau.rest.server.widgets.EventCategory.*;
import org.junit.jupiter.api.*;

/**
 * {@link CalendarDef#validate()} matrix and defaults.
 */
class CalendarDef_Test extends TestBase {

	private static CalendarDef good() {
		return CalendarDef.create()
			.id("cal1")
			.endpoint("/events/{year}/{month}")
			.categories(EventCategory.create().id("team").label("Team").color(CategoryColor.BLUE))
			.initial(2026, 8)
			.events(CalendarEvent.create().id("e1").title("Team offsite").start("2026-08-14").categoryId("team"));
	}

	@Test void a01_contractVersion_isStringTwo() {
		// Bumped in lockstep with JUNEAU_CALENDAR_CONTRACT_VERSION when `end` became layout-significant.
		assertEquals("2", CalendarDef.CONTRACT_VERSION);
	}

	@Test void a02_wellFormed_validates() {
		assertDoesNotThrow(() -> good().validate());
	}

	@Test void a03_defaults() {
		var d = CalendarDef.create().id("c");
		assertEquals(CalendarView.MONTH, d.effectiveView());
		assertEquals(WeekStart.SUNDAY, d.effectiveWeekStart());
		assertEquals(3, d.effectiveMaxPerDay());
	}

	@Test void b01_nullId_rejected() {
		assertThrows(IllegalArgumentException.class, () -> CalendarDef.create().validate());
	}

	@Test void b02_blankId_rejected() {
		assertThrows(IllegalArgumentException.class, () -> good().id("  ").validate());
	}

	@Test void b03_badCharsetId_rejected() {
		assertThrows(IllegalArgumentException.class, () -> good().id("1cal").validate());
		assertThrows(IllegalArgumentException.class, () -> good().id("cal.1").validate());
		assertThrows(IllegalArgumentException.class, () -> good().id("cal 1").validate());
	}

	@Test void c01_endpointMissingTokens_rejected() {
		assertThrows(IllegalArgumentException.class, () -> good().endpoint("/events/{year}").validate());
		assertThrows(IllegalArgumentException.class, () -> good().endpoint("/events/{month}").validate());
	}

	@Test void c02_endpointNotSameOrigin_rejected() {
		assertThrows(IllegalArgumentException.class, () -> good().endpoint("http://evil/{year}/{month}").validate());
		assertThrows(IllegalArgumentException.class, () -> good().endpoint("//host/{year}/{month}").validate());
		assertThrows(IllegalArgumentException.class, () -> good().endpoint("a:b/{year}/{month}").validate());
		assertThrows(IllegalArgumentException.class, () -> good().endpoint("../{year}/{month}").validate());
	}

	@Test void c03_blankEndpoint_rejected() {
		assertThrows(IllegalArgumentException.class, () -> good().endpoint("   ").validate());
	}

	@Test void c04_nullEndpoint_ok_seedOnly() {
		// Seed-only: no endpoint; seed events must still be in-window.
		var d = CalendarDef.create()
			.id("c")
			.categories(EventCategory.create().id("team").label("Team"))
			.initial(2026, 8)
			.events(CalendarEvent.create().id("e1").title("x").start("2026-08-01").categoryId("team"));
		d.endpoint = null;
		assertDoesNotThrow(() -> d.validate());
	}

	@Test void d01_maxPerDayBelowOne_rejected() {
		assertThrows(IllegalArgumentException.class, () -> good().maxPerDay(0).validate());
	}

	@Test void d02_initialMonthOutOfRange_rejected() {
		assertThrows(IllegalArgumentException.class, () -> good().initial(2026, 0).validate());
		assertThrows(IllegalArgumentException.class, () -> good().initial(2026, 13).validate());
	}

	@Test void d03_initialSetOneOnly_rejected() {
		var d = good();
		d.initialYear = 2026;
		d.initialMonth = null;
		assertThrows(IllegalArgumentException.class, () -> d.validate());
	}

	@Test void e01_nullCategory_rejected() {
		var d = good().categories((EventCategory) null);
		assertThrows(IllegalArgumentException.class, () -> d.validate());
	}

	@Test void e02_blankCategoryId_rejected() {
		assertThrows(IllegalArgumentException.class,
			() -> good().categories(EventCategory.create().id("").label("x")).validate());
	}

	@Test void e03_badCharsetCategoryId_rejected() {
		assertThrows(IllegalArgumentException.class,
			() -> good().categories(EventCategory.create().id("a b").label("x")).validate());
	}

	@Test void e04_dupCategoryId_rejected() {
		assertThrows(IllegalArgumentException.class, () -> good().categories(
			EventCategory.create().id("team").label("A"),
			EventCategory.create().id("team").label("B")).validate());
	}

	@Test void e05_blankCategoryLabel_rejected() {
		assertThrows(IllegalArgumentException.class,
			() -> good().categories(EventCategory.create().id("team").label("  ")).validate());
	}

	@Test void f01_seedEventBlankTitle_isDropped_notFatal() {
		// A blank title is MALFORMED, and a malformed seed event is dropped so one bad event never costs the whole
		// calendar - the same posture the per-month GET path takes (rec F / rec L / rec S).
		var d = good().events(
			CalendarEvent.create().id("e1").title("  ").start("2026-08-14").categoryId("team"),
			CalendarEvent.create().id("e2").title("Kept").start("2026-08-15").categoryId("team"));
		assertDoesNotThrow(() -> d.validate());
		assertEquals(List.of("e2"), d.wellFormedEvents().stream().map(e -> e.id).toList());
	}

	@Test void f02_seedDupEventId_rejected() {
		assertThrows(IllegalArgumentException.class, () -> good().events(
			CalendarEvent.create().id("e1").title("a").start("2026-08-14").categoryId("team"),
			CalendarEvent.create().id("e1").title("b").start("2026-08-15").categoryId("team")).validate());
	}

	@Test void f03_seedOffMonth_rejected() {
		assertThrows(IllegalArgumentException.class, () -> good().events(
			CalendarEvent.create().id("e1").title("a").start("2026-09-01").categoryId("team")).validate());
	}

	@Test void f04_seedUnknownCategory_rejected() {
		assertThrows(IllegalArgumentException.class, () -> good().events(
			CalendarEvent.create().id("e1").title("a").start("2026-08-14").categoryId("ghost")).validate());
	}

	@Test void f05_seedUnsafeHref_rejected() {
		assertThrows(IllegalArgumentException.class, () -> good().events(
			CalendarEvent.create().id("e1").title("a").start("2026-08-14").href("javascript:alert(1)")).validate());
	}

	@Test void g01_offMonthCheckedAgainstResolvedWindow() {
		// initialYear/initialMonth unset; emitter resolves window and passes it to validate(y,m).
		var d = CalendarDef.create()
			.id("c")
			.categories(EventCategory.create().id("team").label("Team"))
			.events(CalendarEvent.create().id("e1").title("x").start("2026-08-14").categoryId("team"));
		assertThrows(IllegalArgumentException.class, () -> d.validate(2026, 9));
		assertDoesNotThrow(() -> d.validate(2026, 8));
	}

	//------------------------------------------------------------------------------------------------------------------
	// The derived spanning-bar lane budget, and the seed drop path.
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_laneBudget_isDerivedFromMaxPerDay() {
		assertEquals(3, CalendarDef.create().id("c").effectiveLaneBudget());
		assertEquals(5, CalendarDef.create().id("c").maxPerDay(5).effectiveLaneBudget());
	}

	@Test void h02_laneBudget_clampedToHardCap() {
		assertEquals(8, CalendarDef.MAX_LANES_PER_WEEK);
		assertEquals(8, CalendarDef.create().id("c").maxPerDay(50).effectiveLaneBudget());
	}

	@Test void h03_malformedSeedEvent_isDropped_theRestStillValidate() {
		var d = good().events(
			CalendarEvent.create().id("a").title("Kept").start("2026-08-14").categoryId("team"),
			// Declared allDay=true with a date-time end: malformed, and dropped rather than fatal.
			CalendarEvent.create().id("b").title("Bad").start("2026-08-14").allDay(true).end("2026-08-14T10:00"),
			CalendarEvent.create().id("c").title("Also kept").start("2026-08-15").categoryId("team"));
		assertDoesNotThrow(() -> d.validate());
		assertEquals(List.of("a", "c"), d.wellFormedEvents().stream().map(e -> e.id).toList());
	}

	@Test void h04_omittedEndAndOffsetSeedEvents_stillValidate() {
		var d = good().events(
			CalendarEvent.create().id("a").title("Start only").start("2026-08-14").categoryId("team"),
			CalendarEvent.create().id("b").title("Offset").start("2026-08-15T09:00:00Z").end("2026-08-15T10:00:00Z")
				.categoryId("team"));
		assertDoesNotThrow(() -> d.validate());
		assertEquals(2, d.wellFormedEvents().size());
	}
}
