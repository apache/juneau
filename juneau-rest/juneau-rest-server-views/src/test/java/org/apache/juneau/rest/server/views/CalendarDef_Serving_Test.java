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

import java.time.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.widgets.*;
import org.apache.juneau.rest.server.widgets.EventCategory.*;
import org.junit.jupiter.api.*;

/**
 * {@link CalendarTable#of(CalendarDef, Clock)} serving-path coverage: stamped {@code data-juneau-calendar*} hooks,
 * a deterministic server-painted initial month with real seed chips, the two {@code <template>} skeletons, the
 * display-only legend, and the {@code escapeForScript}-encoded seed sidecar (break-out proof).
 */
class CalendarDef_Serving_Test extends TestBase {

	/** A fixed August-2026 clock so the grid and the stamped {@code today} are deterministic. */
	private static final Clock AUG_2026 = Clock.fixed(Instant.parse("2026-08-15T12:00:00Z"), ZoneOffset.UTC);

	private static CalendarDef good() {
		return CalendarDef.create()
			.id("cal1")
			.endpoint("/events/{year}/{month}")
			.categories(
				EventCategory.create().id("team").label("Team").color(CategoryColor.BLUE),
				EventCategory.create().id("review").label("Review").color(CategoryColor.GREEN))
			.initial(2026, 8)
			.events(
				CalendarEvent.create().id("e1").title("Team offsite").start("2026-08-14").categoryId("team")
					.href("/events/123"),
				CalendarEvent.create().id("e2").title("Planning review").start("2026-08-30").categoryId("review"));
	}

	@Test void a01_rootHooksStamped() {
		var html = Html.of(CalendarTable.of(good(), AUG_2026));
		assertTrue(html.contains("data-juneau-calendar=\"cal1\""), html);
		assertTrue(html.contains("data-juneau-calendar-contract=\"1\""), html);
		assertTrue(html.contains("data-juneau-calendar-today=\"2026-08-15\""), html);
		assertTrue(html.contains("data-juneau-calendar-endpoint=\"/events/{year}/{month}\""), html);
		assertTrue(html.contains("data-juneau-calendar-view=\"month\""), html);
		assertTrue(html.contains("data-juneau-calendar-weekstart=\"sunday\""), html);
		assertTrue(html.contains("data-juneau-calendar-maxperday=\"3\""), html);
	}

	@Test void a02_bothTemplatesPresent() {
		var html = Html.of(CalendarTable.of(good(), AUG_2026));
		assertTrue(html.contains("data-juneau-calendar-day"), html);
		assertTrue(html.contains("data-juneau-calendar-event"), html);
	}

	@Test void a03_legendListsCategoriesInOrderWithColorClasses() {
		var html = Html.of(CalendarTable.of(good(), AUG_2026));
		assertTrue(html.contains("data-juneau-calendar-legend"), html);
		var team = html.indexOf("data-juneau-calendar-cat=\"team\"");
		var review = html.indexOf("data-juneau-calendar-cat=\"review\"");
		assertTrue(team >= 0 && review >= 0, html);
		assertTrue(team < review, "legend order must follow declaration: " + html);
		assertTrue(html.contains("jc-cal-cat--blue"), html);
		assertTrue(html.contains("jc-cal-cat--green"), html);
	}

	@Test void a04_initialMonthGridPainted_withSeedChips() {
		var html = Html.of(CalendarTable.of(good(), AUG_2026));
		assertTrue(html.contains("role=\"grid\""), html);
		assertTrue(html.contains("role=\"gridcell\""), html);
		// The seed chips are painted into the cells (true PE), with a same-origin anchor for the linked event.
		assertTrue(html.contains("Team offsite"), html);
		assertTrue(html.contains("Planning review"), html);
		assertTrue(html.contains("href=\"/events/123\""), html);
		// Today is highlighted (Aug 15).
		assertTrue(html.contains("jc-cal-day--today"), html);
	}

	@Test void a05_weekdayHeadersEnglish_monday() {
		var d = good().weekStart(CalendarDef.WeekStart.MONDAY);
		var html = Html.of(CalendarTable.of(d, AUG_2026));
		assertTrue(html.contains("data-juneau-calendar-weekstart=\"monday\""), html);
		assertTrue(html.contains(">Mon<"), html);
		assertTrue(html.contains(">Sun<"), html);
		assertTrue(html.contains("August 2026"), html);
	}

	@Test void a06_seedSidecarPresent_andEscapedAgainstBreakout() {
		var d = good().events(
			CalendarEvent.create().id("x").title("Bad</script><b>oops</b>").start("2026-08-10").categoryId("team"));
		var html = Html.of(CalendarTable.of(d, AUG_2026));
		assertTrue(html.contains("data-juneau-calendar-seed"), html);
		assertTrue(html.contains("\"contractVersion\":\"1\""), html);
		// The raw </script> must be neutralized inside the JSON sidecar so it cannot terminate the element.
		assertFalse(html.contains("Bad</script>"), "escapeForScript must neutralize the </script> break-out: " + html);
	}

	@Test void a07_titlesEscaped_notRawMarkup() {
		var d = good().events(
			CalendarEvent.create().id("x").title("<img src=x onerror=alert(1)>").start("2026-08-10").categoryId("team"));
		var html = Html.of(CalendarTable.of(d, AUG_2026));
		// Painted chip title must be entity-escaped, never live markup.
		assertFalse(html.contains("<img src=x onerror=alert(1)>"), html);
		assertTrue(html.contains("&lt;img") || html.contains("&#60;img"), html);
	}

	@Test void a08_seedOnly_noEndpointAttr() {
		var d = CalendarDef.create()
			.id("c")
			.categories(EventCategory.create().id("team").label("Team"))
			.initial(2026, 8)
			.events(CalendarEvent.create().id("e1").title("x").start("2026-08-01").categoryId("team"));
		d.endpoint = null;
		var html = Html.of(CalendarTable.of(d, AUG_2026));
		assertFalse(html.contains("data-juneau-calendar-endpoint"), html);
	}

	@Test void a09_adjacentMonthCellsCarryNoEvents() {
		// A seed event on Aug 30 is in-month; an off-month event would have failed validate(). Assert the grid's
		// adjacent (July/September) cells exist but the only painted chips are the in-month ones.
		var html = Html.of(CalendarTable.of(good(), AUG_2026));
		assertTrue(html.contains("jc-cal-day--adjacent"), html);
	}

	@Test void a10_defaultMonthFromClockWhenInitialUnset() {
		var d = CalendarDef.create()
			.id("c")
			.categories(EventCategory.create().id("team").label("Team"))
			.events(CalendarEvent.create().id("e1").title("x").start("2026-08-15").categoryId("team"));
		var html = Html.of(CalendarTable.of(d, AUG_2026));
		assertTrue(html.contains("August 2026"), html);
		assertTrue(html.contains("data-juneau-calendar-today=\"2026-08-15\""), html);
	}
}
