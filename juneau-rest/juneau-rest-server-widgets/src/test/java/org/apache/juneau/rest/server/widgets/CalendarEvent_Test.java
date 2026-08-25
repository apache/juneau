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

import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * {@link CalendarEvent#validate(Set, Integer, Integer)} matrix and civil-date/all-day helpers.
 */
class CalendarEvent_Test extends TestBase {

	private static final Set<String> CATS = Set.of("team", "review");

	private static CalendarEvent good() {
		return CalendarEvent.create().id("e1").title("Team offsite").start("2026-08-14").categoryId("team");
	}

	@Test void a01_wellFormed_validates() {
		var e = good();
		assertDoesNotThrow(() -> e.validate(CATS, 2026, 8));
	}

	@Test void a02_blankId_rejected() {
		var e = good().id("  ");
		assertThrows(IllegalArgumentException.class, () -> e.validate(CATS, 2026, 8));
	}

	@Test void a03_blankTitle_rejected() {
		var e = good().title("   ");
		assertThrows(IllegalArgumentException.class, () -> e.validate(CATS, 2026, 8));
	}

	/** Missing, unparseable, and off-month starts are all rejected the same way: as an invalid {@code start}. */
	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"not-a-date", "2026-09-01"})
	void a04_invalidStart_rejected(String start) {
		var e = good().start(start);
		assertThrows(IllegalArgumentException.class, () -> e.validate(CATS, 2026, 8));
	}

	@Test void a06_endBeforeStart_rejected() {
		var e = good().end("2026-08-10");
		assertThrows(IllegalArgumentException.class, () -> e.validate(CATS, 2026, 8));
	}

	@Test void a07_endEqualOrAfter_ok() {
		var e1 = good().end("2026-08-14");
		assertDoesNotThrow(() -> e1.validate(CATS, 2026, 8));
		var e2 = good().end("2026-08-20");
		assertDoesNotThrow(() -> e2.validate(CATS, 2026, 8));
	}

	@Test void a08_unknownCategory_rejected() {
		var e = good().categoryId("nope");
		assertThrows(IllegalArgumentException.class, () -> e.validate(CATS, 2026, 8));
	}

	@Test void a09_unsafeHref_rejected() {
		var e = good().href("https://evil/x");
		assertThrows(IllegalArgumentException.class, () -> e.validate(CATS, 2026, 8));
	}

	@Test void a10_safeHref_ok() {
		var e = good().href("/events/123?x=1");
		assertDoesNotThrow(() -> e.validate(CATS, 2026, 8));
	}

	@Test void a12_offMonthCheckSkippedWhenWindowNull() {
		var e = good().start("2026-09-01");
		assertDoesNotThrow(() -> e.validate(CATS, null, null));
	}

	@Test void b01_civilStart_dateOnly() {
		assertEquals(LocalDate.of(2026, 8, 14), good().civilStart());
	}

	@Test void b02_civilStart_dateTimeCoercedToLeadingDate() {
		var e = good().start("2026-08-14T23:30:00Z");
		assertEquals(LocalDate.of(2026, 8, 14), e.civilStart());
	}

	@Test void b03_allDayDefaulting() {
		assertTrue(CalendarEvent.create().id("e").title("t").start("2026-08-14").effectiveAllDay());
		assertFalse(CalendarEvent.create().id("e").title("t").start("2026-08-14T04:00:00Z").effectiveAllDay());
		assertFalse(CalendarEvent.create().id("e").title("t").start("2026-08-14").allDay(false).effectiveAllDay());
		assertTrue(CalendarEvent.create().id("e").title("t").start("2026-08-14T04:00:00Z").allDay(true).effectiveAllDay());
	}

	//------------------------------------------------------------------------------------------------------------------
	// `end` is layout-significant, with SPLIT inclusivity.
	//------------------------------------------------------------------------------------------------------------------

	private static CalendarEvent ev(String start, String end) {
		var e = CalendarEvent.create().id("e1").title("t").start(start);
		return end == null ? e : e.end(end);
	}

	@Test void c01_allDayEnd_isInclusive_threeDaySpanOccupiesThreeCells() {
		var e = ev("2026-03-02", "2026-03-04");
		assertEquals(LocalDate.of(2026, 3, 4), e.lastDay());
		assertTrue(e.spanning());
		assertEquals(3, e.lastDay().toEpochDay() - e.civilStart().toEpochDay() + 1);
	}

	@Test void c02_timedEnd_isExclusive_startHourOnly() {
		var e = ev("2026-03-02T09:00", "2026-03-02T10:00");
		assertEquals(LocalDate.of(2026, 3, 2), e.lastDay());
		assertFalse(e.spanning());
	}

	@Test void c03_allDayEndEqualsStart_isSingleDay() {
		var e = ev("2026-03-02", "2026-03-02");
		assertEquals(LocalDate.of(2026, 3, 2), e.lastDay());
		assertFalse(e.spanning());
		assertNull(e.malformedReason());
	}

	@Test void c04_timedEndEqualsStart_isZeroDuration_andRejected() {
		var e = ev("2026-03-02T09:00", "2026-03-02T09:00");
		assertNotNull(e.malformedReason());
		assertThrows(IllegalArgumentException.class, () -> e.validate(CATS, null, null));
	}

	@Test void c05_timedCrossingMidnight_isSpanningBar_usingExclusiveEnd() {
		var e = ev("2026-03-02T15:00", "2026-03-03T09:00");
		assertEquals(LocalDate.of(2026, 3, 3), e.lastDay());
		assertTrue(e.spanning());
		// An end at exactly midnight is EXCLUSIVE, so the last occupied day is the day before.
		assertEquals(LocalDate.of(2026, 3, 2), ev("2026-03-02T15:00", "2026-03-03T00:00").lastDay());
		assertFalse(ev("2026-03-02T15:00", "2026-03-03T00:00").spanning());
	}

	@Test void c06_omittedEnd_isStartOnly_andValid() {
		var allDay = ev("2026-03-02", null);
		assertNull(allDay.malformedReason());
		assertEquals(LocalDate.of(2026, 3, 2), allDay.lastDay());
		assertFalse(allDay.spanning());
		var timed = ev("2026-03-02T15:00", null);
		assertNull(timed.malformedReason());
		assertEquals(LocalDate.of(2026, 3, 2), timed.lastDay());
		assertFalse(timed.spanning());   // a start-only timed event is a zero-width chip, never a bar
		assertNull(allDay.civilEnd());
	}

	@Test void c07_endBeforeStart_stillRejected() {
		assertNotNull(ev("2026-03-04", "2026-03-02").malformedReason());
		assertNotNull(ev("2026-03-04T09:00", "2026-03-02T09:00").malformedReason());
	}

	@Test void c08_offsetOrZ_isIgnored_neverRejected() {
		// Fail-soft: v1 already ignored the offset, and rejecting it would break shipped seeds.
		var e = ev("2026-03-02T09:00:00Z", "2026-03-02T10:00:00+02:00");
		assertNull(e.malformedReason());
		assertDoesNotThrow(() -> e.validate(CATS, 2026, 3));
		assertEquals(LocalDate.of(2026, 3, 2), e.civilStart());
		assertEquals(LocalDate.of(2026, 3, 2), e.lastDay());
		assertEquals("09:00", e.startTimeLabel());
	}

	@Test void c09_startTimeLabel_onlyForTimedEvents() {
		assertEquals("09:05", ev("2026-03-02T09:05", null).startTimeLabel());
		assertNull(ev("2026-03-02", null).startTimeLabel());
	}

	//------------------------------------------------------------------------------------------------------------------
	// The CLOSED malformed set - no other kind exists.
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_malformed_missingRequiredFields() {
		assertNotNull(CalendarEvent.create().title("t").start("2026-03-02").malformedReason());
		assertNotNull(CalendarEvent.create().id("e").start("2026-03-02").malformedReason());
		assertNotNull(CalendarEvent.create().id("e").title("t").malformedReason());
	}

	@Test void d02_malformed_unparseableDates() {
		assertNotNull(ev("nope", null).malformedReason());
		assertNotNull(ev("2026-03-02", "nope").malformedReason());
	}

	@Test void d03_malformed_declaredAllDayDisagreesWithEndShape() {
		assertNotNull(ev("2026-03-02", "2026-03-02T10:00").allDay(true).malformedReason());
		assertNotNull(ev("2026-03-02T09:00", "2026-03-04").allDay(false).malformedReason());
	}

	@Test void d04_malformed_mixedShapesWithNullAllDay() {
		assertNotNull(ev("2026-03-02", "2026-03-04T10:00").malformedReason());
		assertNotNull(ev("2026-03-02T09:00", "2026-03-04").malformedReason());
	}

	@Test void d05_nullAllDay_staysValid_viaEffectiveAllDay() {
		// rec F / rec L: agreement is decided by effectiveAllDay(), so a null allDay with matching shapes is fine.
		var dateOnly = ev("2026-03-02", "2026-03-04");
		assertNull(dateOnly.allDay);
		assertNull(dateOnly.malformedReason());
		var timed = ev("2026-03-02T09:00", "2026-03-02T10:00");
		assertNull(timed.allDay);
		assertNull(timed.malformedReason());
	}

	@Test void d06_retainWellFormed_dropsMalformedKeepsTheRest() {
		var ok1 = CalendarEvent.create().id("a").title("A").start("2026-03-02");
		var bad = CalendarEvent.create().id("b").title("B").start("2026-03-02").allDay(true).end("2026-03-02T10:00");
		var ok2 = CalendarEvent.create().id("c").title("C").start("2026-03-03T09:00").end("2026-03-03T10:00");
		var kept = CalendarEvent.retainWellFormed(Arrays.asList(ok1, bad, null, ok2));
		assertEquals(List.of("a", "c"), kept.stream().map(x -> x.id).toList());
	}

	@Test void d07_authorErrorsAreNotMalformed() {
		// An unknown category / unsafe href / off-month start are AUTHOR errors that throw; they are not drop reasons.
		assertNull(good().categoryId("nope").malformedReason());
		assertNull(good().href("https://evil/x").malformedReason());
		assertNull(good().start("2026-09-01").malformedReason());
	}

	@Test void d08_spanIntoTheRenderedMonth_isNotOffMonth() {
		// `end` is layout-significant, so an event that STARTS earlier but is visible in the month is in-window.
		var inWindow = ev("2026-07-28", "2026-08-03").id("e1").title("t");
		assertDoesNotThrow(() -> inWindow.validate(Set.of(), 2026, 8));
		var e = ev("2026-06-01", "2026-06-05").id("e1").title("t");
		assertThrows(IllegalArgumentException.class, () -> e.validate(Set.of(), 2026, 8));
	}
}
