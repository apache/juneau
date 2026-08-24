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

/**
 * {@link CalendarEvent#validate(Set, Integer, Integer)} matrix and civil-date/all-day helpers.
 */
class CalendarEvent_Test extends TestBase {

	private static final Set<String> CATS = Set.of("team", "review");

	private static CalendarEvent good() {
		return CalendarEvent.create().id("e1").title("Team offsite").start("2026-08-14").categoryId("team");
	}

	@Test void a01_wellFormed_validates() {
		assertDoesNotThrow(() -> good().validate(CATS, 2026, 8));
	}

	@Test void a02_blankId_rejected() {
		var e = good().id("  ");
		assertThrows(IllegalArgumentException.class, () -> e.validate(CATS, 2026, 8));
	}

	@Test void a03_blankTitle_rejected() {
		var e = good().title("   ");
		assertThrows(IllegalArgumentException.class, () -> e.validate(CATS, 2026, 8));
	}

	@Test void a04_missingStart_rejected() {
		var e = good().start(null);
		assertThrows(IllegalArgumentException.class, () -> e.validate(CATS, 2026, 8));
	}

	@Test void a05_badStart_rejected() {
		var e = good().start("not-a-date");
		assertThrows(IllegalArgumentException.class, () -> e.validate(CATS, 2026, 8));
	}

	@Test void a06_endBeforeStart_rejected() {
		var e = good().end("2026-08-10");
		assertThrows(IllegalArgumentException.class, () -> e.validate(CATS, 2026, 8));
	}

	@Test void a07_endEqualOrAfter_ok() {
		assertDoesNotThrow(() -> good().end("2026-08-14").validate(CATS, 2026, 8));
		assertDoesNotThrow(() -> good().end("2026-08-20").validate(CATS, 2026, 8));
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
		assertDoesNotThrow(() -> good().href("/events/123?x=1").validate(CATS, 2026, 8));
	}

	@Test void a11_offMonth_rejected() {
		var e = good().start("2026-09-01");
		assertThrows(IllegalArgumentException.class, () -> e.validate(CATS, 2026, 8));
	}

	@Test void a12_offMonthCheckSkippedWhenWindowNull() {
		assertDoesNotThrow(() -> good().start("2026-09-01").validate(CATS, null, null));
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
}
